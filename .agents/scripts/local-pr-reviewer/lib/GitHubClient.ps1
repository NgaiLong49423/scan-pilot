# GitHubClient.ps1

function Get-EligibleDevPullRequests {
    param (
        [string]$GhCommand = "gh"
    )

    try {
        $json = & $GhCommand pr list --state open --base dev --json number,title,isDraft,headRefOid,headRepository,baseRepository,baseRefName,url 2>$null
        if ([string]::IsNullOrWhiteSpace($json)) {
            return @()
        }

        $prs = $json | ConvertFrom-Json
        $eligible = [System.Collections.Generic.List[PSCustomObject]]::new()

        foreach ($pr in $prs) {
            # 1. State must be OPEN
            # (already queried with --state open, but double-check if present)
            
            # 2. Must not be draft
            if ($pr.isDraft -eq $true) {
                continue
            }

            # 3. Base branch must be dev
            if ($pr.baseRefName -ne "dev") {
                continue
            }

            # 4. Immutable repository identity check (same-repo, exclude forks)
            if ($null -eq $pr.headRepository -or $null -eq $pr.baseRepository -or [string]::IsNullOrEmpty($pr.headRepository.id) -or [string]::IsNullOrEmpty($pr.baseRepository.id)) {
                continue
            }

            if ($pr.headRepository.id -ne $pr.baseRepository.id) {
                continue
            }

            $eligible.Add($pr)
        }

        return $eligible.ToArray()
    } catch {
        return @()
    }
}

function Check-RemotePrMarker {
    param (
        [int]$PrNumber,
        [string]$HeadSha,
        [string]$GhCommand = "gh"
    )

    $marker = "<!-- scanpilot-gemini-pr-review: $PrNumber $HeadSha -->"

    try {
        $json = & $GhCommand pr view $PrNumber --json comments 2>$null
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($json)) {
            return @{
                Success = $false
                HasMarker = $false
                ErrorCode = "REMOTE_QUERY_FAILED"
            }
        }

        $data = $json | ConvertFrom-Json
        if ($null -ne $data.comments) {
            foreach ($comment in $data.comments) {
                if ($comment.body -and $comment.body.Contains($marker)) {
                    return @{
                        Success = $true
                        HasMarker = $true
                        CommentId = $comment.id
                        ErrorCode = $null
                    }
                }
            }
        }

        return @{
            Success = $true
            HasMarker = $false
            CommentId = $null
            ErrorCode = $null
        }
    } catch {
        return @{
            Success = $false
            HasMarker = $false
            ErrorCode = "REMOTE_QUERY_FAILED"
        }
    }
}

function Get-PrDiffContent {
    param (
        [int]$PrNumber,
        [string]$GhCommand = "gh"
    )

    try {
        $diff = & $GhCommand pr diff $PrNumber 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }
        return $diff
    } catch {
        return $null
    }
}

function Format-PrReviewCommentBody {
    param (
        [int]$PrNumber,
        [string]$HeadSha,
        [string]$Model,
        [string]$ThinkingLevel,
        [string]$Status,
        [string]$Summary,
        [array]$Findings
    )

    $marker = "<!-- scanpilot-gemini-pr-review: $PrNumber $HeadSha -->"

    $statusBadge = switch ($Status) {
        "NO_BLOCKER"     { "🟢 **NO_BLOCKER**" }
        "CHANGES_NEEDED" { "🟡 **CHANGES_NEEDED**" }
        default          { "⚪ **UNAVAILABLE**" }
    }

    $body = @"
$marker
## 🤖 Scan Pilot — AI Heuristic Pre-Review

> **Model:** `$Model` (Thinking: `$ThinkingLevel`)  
> **Target PR HEAD:** `$HeadSha`  
> **Pre-Review Status:** $statusBadge  
> **Notice:** *This is an advisory AI pre-review based on static diff heuristics. It does not replace automated CI checks or technical sign-off by Technical Manager (Codex).*

### Summary

$Summary

"@

    if ($Findings -and $Findings.Count -gt 0) {
        $body += @"
### Findings ($($Findings.Count))

| Severity | File | Line | Issue Description |
|---|---|---|---|
"@

        foreach ($f in $Findings) {
            $sevBadge = switch ($f.severity) {
                "CRITICAL" { "🔴 CRITICAL" }
                "HIGH"     { "🟠 HIGH" }
                "MEDIUM"   { "🟡 MEDIUM" }
                "LOW"      { "🔵 LOW" }
                default    { "⚪ INFO" }
            }
            $body += "`n| $sevBadge | `$($f.file)` | $($f.line) | $($f.message) |"
        }
        $body += "`n"
    } else {
        $body += @"

*No AI findings were validated; human/Codex review and CI are still required.*
"@
    }

    return $body
}

function Publish-PrReviewComment {
    param (
        [int]$PrNumber,
        [string]$Body,
        [string]$GhCommand = "gh"
    )

    try {
        $output = & $GhCommand pr comment $PrNumber --body $Body 2>&1
        if ($LASTEXITCODE -eq 0) {
            return @{
                Success = $true
                Output = [string]$output
            }
        } else {
            return @{
                Success = $false
                Output = [string]$output
            }
        }
    } catch {
        return @{
            Success = $false
            Output = $_.Exception.Message
        }
    }
}
