# DiffParser.ps1
. "$PSScriptRoot\RedactionEngine.ps1"

function Parse-UnifiedDiff {
    param (
        [string]$RawDiff,
        [int]$MaxFiles = 50,
        [int]$MaxTotalChars = 60000
    )

    $result = [PSCustomObject]@{
        Files = [System.Collections.Generic.List[string]]::new()
        HunkLines = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.HashSet[int]]]::new()
        TotalChars = 0
        IsTruncated = $false
        SanitizedDiff = ""
    }

    if ([string]::IsNullOrEmpty($RawDiff)) {
        return $result
    }

    $lines = $RawDiff -split "`r?`n"
    $currentFile = $null
    $currentNewLine = 0
    $fileCount = 0
    $builder = [System.Text.StringBuilder]::new()
    $totalCharsCount = 0

    foreach ($line in $lines) {
        # Check diff file headers (e.g. diff --git a/path/to/file b/path/to/file or +++ b/path/to/file)
        if ($line -match '^diff --git a/.* b/(.*)$') {
            $fileCount++
            if ($fileCount -gt $MaxFiles) {
                $result.IsTruncated = $true
                break
            }
            $currentFile = $matches[1]
            if (-not $result.Files.Contains($currentFile)) {
                $result.Files.Add($currentFile)
                $result.HunkLines[$currentFile] = [System.Collections.Generic.HashSet[int]]::new()
            }
        }
        elseif ($line -match '^\+\+\+ b/(.*)$') {
            $path = $matches[1]
            if ($path -ne "/dev/null") {
                $currentFile = $path
                if (-not $result.Files.Contains($currentFile)) {
                    $result.Files.Add($currentFile)
                    $result.HunkLines[$currentFile] = [System.Collections.Generic.HashSet[int]]::new()
                }
            }
        }
        elseif ($line -match '^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@') {
            # Hunk header: @@ -oldStart,oldLen +newStart,newLen @@
            $currentNewLine = [int]$matches[1]
        }
        elseif ($line.StartsWith('+') -and -not $line.StartsWith('+++')) {
            # Added or modified line in new file
            if ($null -ne $currentFile -and $result.HunkLines.ContainsKey($currentFile)) {
                [void]$result.HunkLines[$currentFile].Add($currentNewLine)
            }
            $currentNewLine++
        }
        elseif ($line.StartsWith('-') -and -not $line.StartsWith('---')) {
            # Deleted line in old file (does not increment new file line index)
        }
        else {
            # Context line
            $currentNewLine++
        }

        # Check total characters length
        if ($totalCharsCount + $line.Length + 1 -gt $MaxTotalChars) {
            $result.IsTruncated = $true
            break
        }

        [void]$builder.AppendLine($line)
        $totalCharsCount += $line.Length + 1
    }

    $rawTruncatedDiff = $builder.ToString()
    $result.TotalChars = $totalCharsCount

    if ($result.IsTruncated) {
        $rawTruncatedDiff += "`n`n[TRUNCATED: Max limit reached - Diff truncated safely]`n"
    }

    # Best-effort secret redaction
    $result.SanitizedDiff = Redact-Secrets -Content $rawTruncatedDiff

    return $result
}
