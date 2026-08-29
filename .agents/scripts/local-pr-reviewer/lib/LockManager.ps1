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

function Try-WriteLockAtomic {
    param (
        [string]$LockPath,
        [int]$PrNumber,
        [int]$CurrentPid
    )

    try {
        $stream = [System.IO.File]::Open($LockPath, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        $writer = [System.IO.StreamWriter]::new($stream, [System.Text.Encoding]::UTF8)
        $data = @{
            pid = $CurrentPid
            pr = $PrNumber
            lease = ([DateTime]::UtcNow).ToString("o")
        } | ConvertTo-Json
        $writer.Write($data)
        $writer.Flush()
        $writer.Close()
        $stream.Close()
        return $true
    } catch [System.IO.IOException] {
        return $false
    } catch {
        return $false
    }
}

function Acquire-PrLock {
    param (
        [string]$BaseDir,
        [int]$PrNumber,
        [int]$CurrentPid = $PID
    )

    $lockPath = Get-PrLockPath -BaseDir $BaseDir -PrNumber $PrNumber

    # 1. Attempt atomic creation
    if (Try-WriteLockAtomic -LockPath $lockPath -PrNumber $PrNumber -CurrentPid $CurrentPid) {
        return $true
    }

    # 2. Lock file exists: inspect owner PID for stale lock
    try {
        if (Test-Path $lockPath) {
            $raw = Get-Content -Path $lockPath -Raw -ErrorAction Stop
            $content = $raw | ConvertFrom-Json
            $lockedPid = $content.pid

            # Check OS process liveness
            $process = Get-Process -Id $lockedPid -ErrorAction SilentlyContinue
            if ($null -eq $process) {
                # Stale lock: PID is dead, remove file and reattempt atomic creation
                Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
                return (Try-WriteLockAtomic -LockPath $lockPath -PrNumber $PrNumber -CurrentPid $CurrentPid)
            } else {
                # Active process holds the lock
                return $false
            }
        }
    } catch {
        # Corrupted lock: try remove and reacquire atomically
        Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
        return (Try-WriteLockAtomic -LockPath $lockPath -PrNumber $PrNumber -CurrentPid $CurrentPid)
    }

    return $false
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
            # Only the owning PID may update lease
            $raw = Get-Content -Path $lockPath -Raw -ErrorAction Stop
            $content = $raw | ConvertFrom-Json
            if ($content.pid -eq $CurrentPid) {
                $lockData = @{
                    pid = $CurrentPid
                    pr = $PrNumber
                    lease = ([DateTime]::UtcNow).ToString("o")
                } | ConvertTo-Json
                Set-Content -Path $lockPath -Value $lockData -Force -ErrorAction SilentlyContinue
            }
        } catch {}
    }
}

function Release-PrLock {
    param (
        [string]$BaseDir,
        [int]$PrNumber,
        [int]$CurrentPid = $PID
    )

    $lockPath = Get-PrLockPath -BaseDir $BaseDir -PrNumber $PrNumber
    if (Test-Path $lockPath) {
        try {
            $raw = Get-Content -Path $lockPath -Raw -ErrorAction Stop
            $content = $raw | ConvertFrom-Json
            if ($content.pid -eq $CurrentPid) {
                Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
            }
        } catch {
            Remove-Item -Path $lockPath -Force -ErrorAction SilentlyContinue
        }
    }
}
