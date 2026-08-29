# OutputValidator.ps1
. "$PSScriptRoot\RedactionEngine.ps1"

function Validate-GeminiResponse {
    param (
        [string]$RawJson,
        [System.Collections.Generic.Dictionary[string, System.Collections.Generic.HashSet[int]]]$HunkLines
    )

    $neutralSummary = "No AI findings were validated; human/Codex review and CI are still required."

    if ([string]::IsNullOrWhiteSpace($RawJson)) {
        return @{
            Status = "UNAVAILABLE"
            Summary = $neutralSummary
            Findings = @()
            ErrorCode = "INVALID_SCHEMA"
        }
    }

    try {
        # Strip potential markdown code fences ```json ... ```
        $cleanJson = $RawJson.Trim()
        if ($cleanJson -match '^```(?:json)?\s*([\s\S]*?)\s*```$') {
            $cleanJson = $matches[1].Trim()
        }

        $parsed = $cleanJson | ConvertFrom-Json
    } catch {
        return @{
            Status = "UNAVAILABLE"
            Summary = $neutralSummary
            Findings = @()
            ErrorCode = "INVALID_SCHEMA"
        }
    }

    if ($null -eq $parsed) {
        return @{
            Status = "UNAVAILABLE"
            Summary = $neutralSummary
            Findings = @()
            ErrorCode = "INVALID_SCHEMA"
        }
    }

    $allowedStatuses = @("NO_BLOCKER", "CHANGES_NEEDED", "UNAVAILABLE")
    $status = if ($allowedStatuses -contains $parsed.status) { $parsed.status } else { "UNAVAILABLE" }

    if ($status -eq "UNAVAILABLE") {
        return @{
            Status = "UNAVAILABLE"
            Summary = $neutralSummary
            Findings = @()
            ErrorCode = "INVALID_SCHEMA"
        }
    }

    $validFindings = [System.Collections.Generic.List[PSCustomObject]]::new()
    $allowedSeverities = @("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO")

    if ($null -ne $parsed.findings -and $parsed.findings.Count -gt 0) {
        foreach ($finding in $parsed.findings) {
            $file = [string]$finding.file
            $line = [int]$finding.line
            $severity = [string]$finding.severity
            $message = [string]$finding.message

            # Validate file existence in diff hunks
            if (-not $HunkLines.ContainsKey($file)) {
                # Hallucinated file -> drop finding
                continue
            }

            # Validate line existence in changed hunk lines
            if (-not $HunkLines[$file].Contains($line)) {
                # Hallucinated line outside diff hunk -> drop finding
                continue
            }

            # Validate severity
            if ($allowedSeverities -notcontains $severity.ToUpper()) {
                $severity = "MEDIUM"
            } else {
                $severity = $severity.ToUpper()
            }

            # Sanitize and cap message length (max 500 chars)
            if ($message.Length -gt 500) {
                $message = $message.Substring(0, 497) + "..."
            }
            $message = Redact-Secrets -Content $message

            $validFindings.Add([PSCustomObject]@{
                file = $file
                line = $line
                severity = $severity
                message = $message
            })
        }
    }

    $summary = [string]$parsed.summary
    if ([string]::IsNullOrWhiteSpace($summary) -or $validFindings.Count -eq 0) {
        $summary = $neutralSummary
    } else {
        if ($summary.Length -gt 300) {
            $summary = $summary.Substring(0, 297) + "..."
        }
        $summary = Redact-Secrets -Content $summary
    }

    # Ensure no "approved" or "pass" language appears in summary
    $summary = [regex]::Replace($summary, '(?i)\b(approved|passes|passed|clean)\b', 'reviewed')

    $finalStatus = if ($validFindings.Count -gt 0) { "CHANGES_NEEDED" } else { "NO_BLOCKER" }

    return @{
        Status = $finalStatus
        Summary = $summary
        Findings = $validFindings.ToArray()
        ErrorCode = $null
    }
}
