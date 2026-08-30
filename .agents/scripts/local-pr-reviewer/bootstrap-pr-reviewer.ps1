<#
.SYNOPSIS
    Bootstrap launcher for Scan Pilot Gemini PR Pre-Reviewer (Task Scheduler compatible).
.DESCRIPTION
    Resolves repository root from $PSScriptRoot, sets the process working directory,
    and invokes run-pr-review.ps1 safely regardless of caller working directory.
#>
[CmdletBinding()]
param()

$RepoRoot = (Resolve-Path "$PSScriptRoot\..\..\..").Path
Set-Location $RepoRoot
& "$PSScriptRoot\run-pr-review.ps1" -BaseDir $RepoRoot
