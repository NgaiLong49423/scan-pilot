# CacheManager.ps1

function Get-CacheFilePath {
    param (
        [string]$BaseDir
    )
    $workDir = Join-Path $BaseDir ".agent-work"
    if (-not (Test-Path $workDir)) {
        New-Item -ItemType Directory -Force -Path $workDir | Out-Null
    }
    return Join-Path $workDir "gemini-pr-review-cache.json"
}

function Get-AllCachedReviews {
    param (
        [string]$BaseDir
    )
    $cacheFile = Get-CacheFilePath -BaseDir $BaseDir
    if (Test-Path $cacheFile) {
        try {
            $content = Get-Content -Path $cacheFile -Raw -ErrorAction Stop
            if ([string]::IsNullOrWhiteSpace($content)) {
                return @{}
            }
            $json = ConvertFrom-Json -InputObject $content -AsHashtable
            if ($null -ne $json) {
                return $json
            }
        } catch {
            return @{}
        }
    }
    return @{}
}

function Get-CachedPrReview {
    param (
        [string]$BaseDir,
        [int]$PrNumber,
        [string]$HeadSha
    )

    $allCache = Get-AllCachedReviews -BaseDir $BaseDir
    $key = "$PrNumber"
    if ($allCache.ContainsKey($key)) {
        $entry = $allCache[$key]
        if ($entry.headSha -eq $HeadSha) {
            # Check if this was UNAVAILABLE with active cooldown
            if ($entry.outcome -eq "UNAVAILABLE" -and $null -ne $entry.errorCooldownUntil) {
                $cooldownDate = [DateTimeOffset]$entry.errorCooldownUntil
                if ([DateTimeOffset]::UtcNow -lt $cooldownDate.ToUniversalTime()) {
                    return @{
                        IsCached = $true
                        InCooldown = $true
                        Entry = $entry
                    }
                } else {
                    # Cooldown expired: allow retry
                    return @{
                        IsCached = $false
                        InCooldown = $false
                        Entry = $entry
                    }
                }
            }

            # Successfully reviewed previously
            return @{
                IsCached = $true
                InCooldown = $false
                Entry = $entry
            }
        }
    }

    return @{
        IsCached = $false
        InCooldown = $false
        Entry = $null
    }
}

function Save-CachedPrReview {
    param (
        [string]$BaseDir,
        [int]$PrNumber,
        [string]$HeadSha,
        [string]$Outcome,
        [string]$CommentId = $null,
        [int]$CooldownMinutes = 15
    )

    $cacheFile = Get-CacheFilePath -BaseDir $BaseDir
    $tmpFile = "$cacheFile.tmp"
    $allCache = Get-AllCachedReviews -BaseDir $BaseDir

    $entry = @{
        pr = $PrNumber
        headSha = $HeadSha
        outcome = $Outcome
        commentId = $CommentId
        timestamp = ([DateTime]::UtcNow).ToString("o")
        errorCooldownUntil = $null
    }

    if ($Outcome -eq "UNAVAILABLE") {
        $entry.errorCooldownUntil = ([DateTime]::UtcNow.AddMinutes($CooldownMinutes)).ToString("o")
    }

    $allCache["$PrNumber"] = $entry

    $jsonContent = $allCache | ConvertTo-Json -Depth 5
    
    # Atomic write
    Set-Content -Path $tmpFile -Value $jsonContent -Force -Encoding utf8 -ErrorAction Stop
    Move-Item -Path $tmpFile -Destination $cacheFile -Force -ErrorAction Stop
}
