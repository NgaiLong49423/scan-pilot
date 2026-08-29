<#
.SYNOPSIS
    Scan Pilot — Secret-Safe Local Gemini PR Pre-review Runner
.DESCRIPTION
    Scans open pull requests targeting `dev`, extracts static diffs, applies best-effort secret redaction,
    evaluates findings using Google Gemini (gemini-3.7-flash), locally validates hunk lines, and publishes
    an advisory pre-review comment on GitHub.
#>
[CmdletBinding()]
param (
    [string]$BaseDir = (Resolve-Path "$PSScriptRoot\..\..\..").Path,
    [int]$PrNumber = 0,
    [switch]$DryRun,
    [switch]$BypassCooldown
)

$ErrorActionPreference = "Stop"

# Load library modules
. "$PSScriptRoot\lib\RedactionEngine.ps1"
. "$PSScriptRoot\lib\DiffParser.ps1"
. "$PSScriptRoot\lib\LockManager.ps1"
. "$PSScriptRoot\lib\CacheManager.ps1"
. "$PSScriptRoot\lib\GeminiClient.ps1"
. "$PSScriptRoot\lib\OutputValidator.ps1"
. "$PSScriptRoot\lib\GitHubClient.ps1"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Scan Pilot — Secret-Safe Local Gemini PR Pre-Review Runner" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Repository base dir: $BaseDir"

$apiKey = $Env:GEMINI_PR_REVIEW_API_KEY
$model = if ([string]::IsNullOrWhiteSpace($Env:GEMINI_PR_REVIEW_MODEL)) { "gemini-3.7-flash" } else { $Env:GEMINI_PR_REVIEW_MODEL }
$thinkingLevel = if ([string]::IsNullOrWhiteSpace($Env:GEMINI_PR_REVIEW_THINKING_LEVEL)) { "medium" } else { $Env:GEMINI_PR_REVIEW_THINKING_LEVEL }

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Warning "GEMINI_PR_REVIEW_API_KEY is not set. Pre-review runner cannot invoke Gemini API."
    return @{ Status = "UNAVAILABLE"; ErrorCode = "MODEL_UNAVAILABLE" }
}

# 1. Model Preflight Check
Write-Host "Performing model preflight check for [$model]..." -NoNewline
$preflight = Test-GeminiModelAvailability -ApiKey $apiKey -Model $model
if (-not $preflight.Success) {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Warning "Model preflight failed with code: $($preflight.ErrorCode). Reason: $($preflight.Message)"
    return @{ Status = "UNAVAILABLE"; ErrorCode = $preflight.ErrorCode }
}
Write-Host " [OK]" -ForegroundColor Green

# 2. Fetch Eligible Pull Requests
Write-Host "Querying eligible same-repo PRs targeting [dev]..."
$prs = Get-EligibleDevPullRequests
if ($PrNumber -gt 0) {
    $prs = $prs | Where-Object { $_.number -eq $PrNumber }
}

if (-not $prs -or $prs.Count -eq 0) {
    Write-Host "No eligible open pull requests found." -ForegroundColor Yellow
    return @{ Status = "NO_PRS"; ReviewedCount = 0 }
}

Write-Host "Found $($prs.Count) eligible PR(s) to inspect." -ForegroundColor Cyan
$reviewedCount = 0

foreach ($pr in $prs) {
    $num = $pr.number
    $headSha = $pr.headRefOid
    Write-Host "`n--------------------------------------------------"
    Write-Host "Inspecting PR #$num (HEAD: $headSha)..." -ForegroundColor White

    # 3. Acquire Inter-Process Lock
    $locked = Acquire-PrLock -BaseDir $BaseDir -PrNumber $num
    if (-not $locked) {
        Write-Warning "PR #$num is currently locked by another active process. Skipping."
        continue
    }

    try {
        # 4. Check Local Cooldown (if any)
        $cacheCheck = Get-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha
        if ($cacheCheck.IsCached -and $cacheCheck.InCooldown -and -not $BypassCooldown) {
            Write-Host "PR #$num HEAD $headSha is in UNAVAILABLE cooldown until $($cacheCheck.Entry.errorCooldownUntil). Skipping." -ForegroundColor Yellow
            continue
        }

        # 5. Remote PR Comment Marker Check (Mandatory Gate — Cannot be Bypassed)
        Update-PrLockLease -BaseDir $BaseDir -PrNumber $num
        $remoteCheck = Check-RemotePrMarker -PrNumber $num -HeadSha $headSha
        if (-not $remoteCheck.Success) {
            Write-Warning "Failed to query remote PR comments for PR #$num (fail-closed). Marking UNAVAILABLE with cooldown."
            Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome "UNAVAILABLE" -CooldownMinutes 15
            continue
        }

        if ($remoteCheck.HasMarker) {
            Write-Host "PR #$num already has review comment on GitHub (CommentId: $($remoteCheck.CommentId)). Synchronizing cache." -ForegroundColor Green
            Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome "NO_BLOCKER" -CommentId $remoteCheck.CommentId
            continue
        }

        # 6. Extract and Parse PR Diff
        Update-PrLockLease -BaseDir $BaseDir -PrNumber $num
        Write-Host "Fetching diff for PR #$num..."
        $rawDiff = Get-PrDiffContent -PrNumber $num
        if ([string]::IsNullOrWhiteSpace($rawDiff)) {
            Write-Warning "PR #$num has empty diff or diff fetch failed."
            Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome "UNAVAILABLE" -CooldownMinutes 15
            continue
        }

        $parsedDiff = Parse-UnifiedDiff -RawDiff $rawDiff
        Write-Host "Diff parsed: $($parsedDiff.Files.Count) changed file(s), $($parsedDiff.TotalChars) chars (Truncated: $($parsedDiff.IsTruncated))."

        # 7. Call Gemini API
        Update-PrLockLease -BaseDir $BaseDir -PrNumber $num
        Write-Host "Calling Gemini API ($model, thinking: $thinkingLevel)..."
        $geminiResult = Invoke-GeminiPrReview `
            -ApiKey $apiKey `
            -Model $model `
            -ThinkingLevel $thinkingLevel `
            -SanitizedDiff $parsedDiff.SanitizedDiff `
            -PrNumber $num `
            -HeadSha $headSha

        if ($geminiResult.Status -eq "UNAVAILABLE") {
            Write-Warning "Gemini review returned UNAVAILABLE (Code: $($geminiResult.ErrorCode))."
            Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome "UNAVAILABLE" -CooldownMinutes 15
            continue
        }

        # 8. Local Output Validation (Changed Hunk Lines Match & Sanitization)
        $validated = Validate-GeminiResponse -RawJson $geminiResult.RawJson -HunkLines $parsedDiff.HunkLines
        Write-Host "Validation completed. Status: $($validated.Status), Valid Findings: $($validated.Findings.Count)" -ForegroundColor Cyan

        # 9. Format Comment Body
        $commentBody = Format-PrReviewCommentBody `
            -PrNumber $num `
            -HeadSha $headSha `
            -Model $model `
            -ThinkingLevel $thinkingLevel `
            -Status $validated.Status `
            -Summary $validated.Summary `
            -Findings $validated.Findings

        # 10. Publish Comment
        if ($DryRun) {
            Write-Host "[DRY-RUN] Comment would be posted to PR #$num:`n$commentBody" -ForegroundColor Gray
            Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome $validated.Status -CommentId "dry-run"
            $reviewedCount++
        } else {
            Update-PrLockLease -BaseDir $BaseDir -PrNumber $num
            Write-Host "Posting pre-review comment to PR #$num..."
            $postResult = Publish-PrReviewComment -PrNumber $num -Body $commentBody
            if ($postResult.Success) {
                Write-Host "Successfully posted pre-review comment on PR #$num!" -ForegroundColor Green
                Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome $validated.Status
                $reviewedCount++
            } else {
                Write-Warning "Failed to post comment to PR #$num (Error code: PR_COMMENT_FAILED)."
                Save-CachedPrReview -BaseDir $BaseDir -PrNumber $num -HeadSha $headSha -Outcome "UNAVAILABLE" -CooldownMinutes 15
            }
        }
    } finally {
        Release-PrLock -BaseDir $BaseDir -PrNumber $num
    }
}

Write-Host "`nPre-review runner finished. Total PRs reviewed: $reviewedCount" -ForegroundColor Cyan
return @{ Status = "SUCCESS"; ReviewedCount = $reviewedCount }
