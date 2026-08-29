# LockManager.ps1

function Get-PrLockPath {
    param (
        [string]$BaseDir,
        [int]$PrNumber
    )
    $lockDir = Join-Path $BaseDir ".agent-work\locks"
    if (-not (Test-Path $lockDir)) {
        New-Item -ItemType Directory -Force -Path $lockDir | Out-Null
    }
    return Join-Path $lockDir "pr-$PrNumber.lock"
}

function Acquire-PrLock {
    param (
        [string]$BaseDir,
        [int]$PrNumber,
        [int]$CurrentPid = $PID
    )

    $lockPath = Get-PrLockPath -BaseDir $BaseDir -PrNumber $PrNumber

    if (Test-Path $lockPath) {
        try {
            $content = Get-Content -Path $lockPath -Raw -ErrorAction Stop | ConvertFrom-Json
            $lockedPid = $content.pid
            
            # Check OS process liveness
            $process = Get-Process -Id $lockedPid -ErrorAction SilentlyContinue
            if ($null -eq $process) {
                # Stale lock: PID is dead, safe to reclaim
                Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
            } else {
                # Active process holds the lock
                return $false
            }
        } catch {
            # Corrupted lock file: remove and reclaim
            Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
        }
    }

    $lockData = @{
        pid = $CurrentPid
        pr = $PrNumber
        lease = (Get-Date).ToString("o")
    } | ConvertTo-Json

    try {
        Set-Content -Path $lockPath -Value $lockData -Force -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Update-PrLockLease {
    param (
        [string]$BaseDir,
        [int]$PrNumber,
        [int]$CurrentPid = $PID
    )

    $lockPath = Get-PrLockPath -BaseDir $BaseDir -PrNumber $PrNumber
    if (Test-Path $lockPath) {
        try {
            $lockData = @{
                pid = $CurrentPid
                pr = $PrNumber
                lease = (Get-Date).ToString("o")
            } | ConvertTo-Json
            Set-Content -Path $lockPath -Value $lockData -Force -ErrorAction SilentlyContinue
        } catch {}
    }
}

function Release-PrLock {
    param (
        [string]$BaseDir,
        [int]$PrNumber
    )

    $lockPath = Get-PrLockPath -BaseDir $BaseDir -PrNumber $PrNumber
    if (Test-Path $lockPath) {
        Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
    }
}
