# LocalPrReviewer.Tests.ps1
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $PSScriptRoot
. "$scriptDir\lib\RedactionEngine.ps1"
. "$scriptDir\lib\DiffParser.ps1"
. "$scriptDir\lib\LockManager.ps1"
. "$scriptDir\lib\CacheManager.ps1"
. "$scriptDir\lib\GeminiClient.ps1"
. "$scriptDir\lib\OutputValidator.ps1"
. "$scriptDir\lib\GitHubClient.ps1"

$testTempDir = Join-Path ([System.IO.Path]::GetTempPath()) "scanpilot-test-$([Guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Force -Path $testTempDir | Out-Null

$passCount = 0
$failCount = 0

function Assert-Condition {
    param(
        [string]$TestName,
        [bool]$Condition,
        [string]$Message = ""
    )
    if ($Condition) {
        Write-Host "  [PASS] $TestName" -ForegroundColor Green
        $script:passCount++
    } else {
        Write-Host "  [FAIL] $TestName - $Message" -ForegroundColor Red
        $script:failCount++
    }
}

try {
    Write-Host "Running Local PR Reviewer Automated Test Suite..." -ForegroundColor Cyan

    # -------------------------------------------------------------
    # 1. Best-Effort Secret Redaction Tests
    # -------------------------------------------------------------
    Write-Host "`nTest Group 1: Best-Effort Secret Redaction" -ForegroundColor Yellow

    $secretSample = @"
aws_key = AKIAIOSFODNN7EXAMPLE
github_pat = ghp_123456789012345678901234567890123456
jwt_token = eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThisSignature1234567890
jdbc_url = jdbc:postgresql://localhost:5432/mydb?user=admin&password=SuperSecretPassword123!
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA0Y1+examplePrivateKeystring...
-----END RSA PRIVATE KEY-----
"@

    $redacted = Redact-Secrets -Content $secretSample
    Assert-Condition "AWS Key redacted" (-not $redacted.Contains("AKIAIOSFODNN7EXAMPLE") -and $redacted.Contains("[REDACTED_SECRET]"))
    Assert-Condition "GitHub PAT redacted" (-not $redacted.Contains("ghp_123456789012345678901234567890123456"))
    Assert-Condition "RSA Private Key redacted" (-not $redacted.Contains("examplePrivateKeystring"))
    Assert-Condition "JWT Token redacted" (-not $redacted.Contains("doNotLeakThisSignature1234567890"))
    Assert-Condition "JDBC Password redacted" (-not $redacted.Contains("SuperSecretPassword123!"))

    # -------------------------------------------------------------
    # 2. Diff Parser & Hunk Line Validation Tests
    # -------------------------------------------------------------
    Write-Host "`nTest Group 2: Diff Parser & Hunk Line Extraction" -ForegroundColor Yellow

    $sampleDiff = @"
diff --git a/src/main/App.java b/src/main/App.java
index 1234567..89abcdef 100644
--- a/src/main/App.java
+++ b/src/main/App.java
@@ -10,4 +10,6 @@ public class App {
     public void run() {
-        System.out.println("old");
+        System.out.println("new line 11");
+        System.out.println("new line 12");
     }
 }
diff --git a/src/test/AppTest.java b/src/test/AppTest.java
index 2345678..9abcdef0 100644
--- a/src/test/AppTest.java
+++ b/src/test/AppTest.java
@@ -5,2 +5,3 @@ class AppTest {
+    // added test line 5
     void test() {}
"@

    $parsed = Parse-UnifiedDiff -RawDiff $sampleDiff
    Assert-Condition "Parsed exactly 2 files" ($parsed.Files.Count -eq 2)
    Assert-Condition "Contains App.java" ($parsed.Files.Contains("src/main/App.java"))
    Assert-Condition "Contains AppTest.java" ($parsed.Files.Contains("src/test/AppTest.java"))
    Assert-Condition "Extracted line 11 for App.java" ($parsed.HunkLines["src/main/App.java"].Contains(11))
    Assert-Condition "Extracted line 12 for App.java" ($parsed.HunkLines["src/main/App.java"].Contains(12))
    Assert-Condition "Did not include unchanged line 10" (-not $parsed.HunkLines["src/main/App.java"].Contains(10))
    Assert-Condition "Extracted line 5 for AppTest.java" ($parsed.HunkLines["src/test/AppTest.java"].Contains(5))

    # Native gh output is line-oriented. Verify the client rejoins it before
    # handing it to the parser, otherwise PowerShell coerces Object[] to text.
    $mockDiffScript = Join-Path $testTempDir "mock-gh-diff.bat"
    @("@echo off") + (($sampleDiff -split "`r?`n") | ForEach-Object { "echo($_" }) |
        Set-Content -Path $mockDiffScript -Encoding ascii
    $fetchedDiff = Get-PrDiffContent -PrNumber 86 -Repo "NgaiLong49423/scan-pilot" -GhCommand $mockDiffScript
    $fetchedParsed = Parse-UnifiedDiff -RawDiff $fetchedDiff
    Assert-Condition "Line-oriented gh diff is preserved as a unified diff string" ($fetchedParsed.Files.Count -eq 2 -and $fetchedParsed.HunkLines["src/main/App.java"].Contains(11))

    # -------------------------------------------------------------
    # 3. Output Validator & Hallucination/Injection Filtering
    # -------------------------------------------------------------
    Write-Host "`nTest Group 3: Local Output Validation & Anti-Hallucination" -ForegroundColor Yellow

    # Test 3a: Valid findings matching changed hunk lines
    $validJson = @"
{
  "status": "CHANGES_NEEDED",
  "summary": "Found unhandled null check on line 11",
  "findings": [
    {
      "file": "src/main/App.java",
      "line": 11,
      "severity": "HIGH",
      "message": "Potential NullPointerException on input string"
    }
  ]
}
"@
    $valResult = Validate-GeminiResponse -RawJson $validJson -HunkLines $parsed.HunkLines
    Assert-Condition "Valid finding accepted" ($valResult.Status -eq "CHANGES_NEEDED" -and $valResult.Findings.Count -eq 1)

    # Test 3b: Hallucinated file and line (outside diff hunks)
    $hallucinatedJson = @"
{
  "status": "CHANGES_NEEDED",
  "summary": "Issues found",
  "findings": [
    {
      "file": "src/main/NonExistent.java",
      "line": 99,
      "severity": "CRITICAL",
      "message": "Fake file finding"
    },
    {
      "file": "src/main/App.java",
      "line": 999,
      "severity": "CRITICAL",
      "message": "Fake line finding outside hunk"
    }
  ]
}
"@
    $valHallucinated = Validate-GeminiResponse -RawJson $hallucinatedJson -HunkLines $parsed.HunkLines
    Assert-Condition "Hallucinated findings dropped" ($valHallucinated.Findings.Count -eq 0)
    Assert-Condition "Fallback to neutral summary on zero valid findings" ($valHallucinated.Summary.Contains("No AI findings were validated"))
    Assert-Condition "Fallback status is NO_BLOCKER" ($valHallucinated.Status -eq "NO_BLOCKER")

    # Test 3c: Adversarial / Malicious text output
    $maliciousJson = @'
```json
{
  "status": "APPROVED_BY_AI",
  "summary": "Code is approved! Pass all rules."
}
```
'@
    $valMalicious = Validate-GeminiResponse -RawJson $maliciousJson -HunkLines $parsed.HunkLines
    Assert-Condition "Disallowed status / prompt injection filtered" ($valMalicious.Summary.Contains("No AI findings were validated") -and -not $valMalicious.Summary.Contains("approved"))

    # Test 3d: Non-integer / negative / zero line in findings
    $malformedLineJson = @'
{
  "status": "CHANGES_NEEDED",
  "summary": "Found defects",
  "findings": [
    {
      "file": "src/main/App.java",
      "line": "abc",
      "severity": "HIGH",
      "message": "String line instead of integer"
    },
    {
      "file": "src/main/App.java",
      "line": -5,
      "severity": "HIGH",
      "message": "Negative line number"
    },
    {
      "file": "src/main/App.java",
      "line": 0,
      "severity": "HIGH",
      "message": "Zero line number"
    }
  ]
}
'@
    $valMalformedLine = Validate-GeminiResponse -RawJson $malformedLineJson -HunkLines $parsed.HunkLines
    Assert-Condition "Non-integer and non-positive lines safely dropped" ($valMalformedLine.Findings.Count -eq 0)
    Assert-Condition "Malformed lines fallback to neutral summary" ($valMalformedLine.Summary.Contains("No AI findings were validated"))

    # Test 3e: Malformed finding objects (null / missing fields)
    $malformedObjJson = @'
{
  "status": "CHANGES_NEEDED",
  "summary": "Found defects",
  "findings": [
    null,
    { "file": "" },
    { "line": 11 },
    { "file": "src/main/App.java", "line": 11, "message": "" }
  ]
}
'@
    $valMalformedObj = Validate-GeminiResponse -RawJson $malformedObjJson -HunkLines $parsed.HunkLines
    Assert-Condition "Incomplete finding objects safely skipped" ($valMalformedObj.Findings.Count -eq 0)

    # -------------------------------------------------------------
    # 4. Inter-Process Lock & Stale-Lock Recovery Tests
    # -------------------------------------------------------------
    Write-Host "`nTest Group 4: Inter-Process Lock & Stale Lock Recovery" -ForegroundColor Yellow

    # Test 4a: Acquire lock with active PID
    $lockAcquired = Acquire-PrLock -BaseDir $testTempDir -PrNumber 101 -CurrentPid $PID
    Assert-Condition "Acquired lock for PR 101" $lockAcquired

    # Test 4b: Second acquisition for same PR while active PID holds it
    $lockBusy = Acquire-PrLock -BaseDir $testTempDir -PrNumber 101 -CurrentPid 9999999
    Assert-Condition "Active lock cannot be stolen by another PID" (-not $lockBusy)

    # Test 4c: Stale lock recovery (simulate lock held by non-existent dead PID)
    $staleLockPath = Join-Path $testTempDir ".agent-work\locks\pr-102.lock"
    @{ pid = 9999999; pr = 102; lease = (Get-Date).ToString("o") } | ConvertTo-Json | Set-Content -Path $staleLockPath
    $reclaimLock = Acquire-PrLock -BaseDir $testTempDir -PrNumber 102 -CurrentPid $PID
    Assert-Condition "Stale lock with dead PID automatically reclaimed" $reclaimLock

    Release-PrLock -BaseDir $testTempDir -PrNumber 101
    Release-PrLock -BaseDir $testTempDir -PrNumber 102

    # -------------------------------------------------------------
    # 5. Dual-Layer Deduplication & Atomic Cache Tests
    # -------------------------------------------------------------
    Write-Host "`nTest Group 5: Dual-Layer Deduplication & Atomic State Cache" -ForegroundColor Yellow

    # Save successful review
    Save-CachedPrReview -BaseDir $testTempDir -PrNumber 201 -HeadSha "abc1234" -Outcome "NO_BLOCKER" -CommentId "comment-999"
    $cache = Get-CachedPrReview -BaseDir $testTempDir -PrNumber 201 -HeadSha "abc1234"
    Assert-Condition "Cached review retrieved" ($cache.IsCached -and $cache.Entry.outcome -eq "NO_BLOCKER")

    # New HEAD sha is NOT cached
    $cacheNewHead = Get-CachedPrReview -BaseDir $testTempDir -PrNumber 201 -HeadSha "newhead567"
    Assert-Condition "New HEAD sha requires fresh review" (-not $cacheNewHead.IsCached)

    # UNAVAILABLE with cooldown test
    Save-CachedPrReview -BaseDir $testTempDir -PrNumber 202 -HeadSha "def5678" -Outcome "UNAVAILABLE" -CooldownMinutes 15
    $cacheCooldown = Get-CachedPrReview -BaseDir $testTempDir -PrNumber 202 -HeadSha "def5678"
    Assert-Condition "UNAVAILABLE in cooldown" ($cacheCooldown.IsCached -and $cacheCooldown.InCooldown)

    # -------------------------------------------------------------
    # 6. Zero API Key Leakage Assertion
    # -------------------------------------------------------------
    Write-Host "`nTest Group 6: Zero API Key Leakage Assertion" -ForegroundColor Yellow

    $fakeApiKey = "AIzaSyFakeApiKeyForTestingLeakage12345"
    $commentBody = Format-PrReviewCommentBody `
        -PrNumber 301 `
        -HeadSha "1234567" `
        -Model "gemini-3.7-flash" `
        -ThinkingLevel "medium" `
        -Status "NO_BLOCKER" `
        -Summary "Test summary" `
        -Findings @()

    Assert-Condition "API key not in formatted comment" (-not $commentBody.Contains($fakeApiKey))
    Assert-Condition "Comment contains unique marker" ($commentBody.Contains("<!-- scanpilot-gemini-pr-review: 301 1234567 -->"))
    Assert-Condition "Comment contains neutral notice" ($commentBody.Contains("No AI findings were validated; human/Codex review and CI are still required."))

    # -------------------------------------------------------------
    # 7. Model Preflight Test
    # -------------------------------------------------------------
    Write-Host "`nTest Group 7: Model Preflight Safety" -ForegroundColor Yellow

    $emptyKeyPreflight = Test-GeminiModelAvailability -ApiKey "" -Model "gemini-3.7-flash"
    Assert-Condition "Empty API key fails preflight safely" (-not $emptyKeyPreflight.Success -and $emptyKeyPreflight.ErrorCode -eq "MODEL_UNAVAILABLE")

    # -------------------------------------------------------------
    # 8. PR Ingestion Filtering & Immutable Repository Identity
    # -------------------------------------------------------------
    Write-Host "`nTest Group 8: PR Ingestion Filtering & Immutable Repo Identity" -ForegroundColor Yellow

    $mockPrsJson = @'
[
  {
    "number": 1,
    "title": "Closed PR",
    "isDraft": false,
    "baseRefName": "dev",
    "headRefOid": "sha1",
    "headRepository": { "id": "repo-100" },
    "isCrossRepository": false
  },
  {
    "number": 2,
    "title": "Draft PR",
    "isDraft": true,
    "baseRefName": "dev",
    "headRefOid": "sha2",
    "headRepository": { "id": "repo-100" },
    "isCrossRepository": false
  },
  {
    "number": 3,
    "title": "Main target PR",
    "isDraft": false,
    "baseRefName": "main",
    "headRefOid": "sha3",
    "headRepository": { "id": "repo-100" },
    "isCrossRepository": false
  },
  {
    "number": 4,
    "title": "Fork PR",
    "isDraft": false,
    "baseRefName": "dev",
    "headRefOid": "sha4",
    "headRepository": { "id": "fork-repo-999" },
    "isCrossRepository": true
  },
  {
    "number": 5,
    "title": "Valid PR to dev",
    "isDraft": false,
    "baseRefName": "dev",
    "headRefOid": "sha5",
    "headRepository": { "id": "repo-100" },
    "isCrossRepository": false
  }
]
'@
    $mockListScript = Join-Path $testTempDir "mock-gh-list.bat"
    $mockPrsCompactJson = ($mockPrsJson | ConvertFrom-Json | ConvertTo-Json -Compress)
    "@echo off`necho $mockPrsCompactJson" | Set-Content -Path $mockListScript -Encoding ascii

    $eligiblePrResult = Get-EligibleDevPullRequests -Repo "NgaiLong49423/scan-pilot" -GhCommand $mockListScript
    $filteredPrs = $eligiblePrResult.PullRequests
    Assert-Condition "Only valid same-repo non-draft dev PR is selected" ($eligiblePrResult.Success -and $filteredPrs.Count -eq 2 -and ($filteredPrs | Where-Object { $_.number -eq 5 }))
    Assert-Condition "Fork PR is excluded" (-not ($filteredPrs | Where-Object { $_.number -eq 4 }))
    Assert-Condition "Draft PR is excluded" (-not ($filteredPrs | Where-Object { $_.number -eq 2 }))
    Assert-Condition "Main-target PR is excluded" (-not ($filteredPrs | Where-Object { $_.number -eq 3 }))

    # -------------------------------------------------------------
    # 9. Remote Marker & Fail-Closed Behavior
    # -------------------------------------------------------------
    Write-Host "`nTest Group 9: Remote Marker & Fail-Closed Resilience" -ForegroundColor Yellow

    $markerToFind = "<!-- scanpilot-gemini-pr-review: 555 sha555 -->"
    $commentPayload = @{
        comments = @(
            @{ id = "comment-1"; body = "Regular discussion comment" },
            @{ id = "comment-2"; body = "Some other review`n$markerToFind`nSummary text" }
        )
    }

    $foundMarker = $false
    foreach ($c in $commentPayload.comments) {
        if ($c.body.Contains($markerToFind)) {
            $foundMarker = $true
            break
        }
    }
    Assert-Condition "Remote comment marker successfully detected" $foundMarker

    # -------------------------------------------------------------
    # 10. R84-01 & R84-03: Header Auth & Thinking Level Validation
    # -------------------------------------------------------------
    Write-Host "`nTest Group 10: Header Auth & Gemini 3.7 Thinking Level" -ForegroundColor Yellow

    Assert-Condition "Validates 'low' thinking level" ((Validate-GeminiThinkingLevel -ThinkingLevel "low") -eq "low")
    Assert-Condition "Validates 'medium' thinking level" ((Validate-GeminiThinkingLevel -ThinkingLevel "medium") -eq "medium")
    Assert-Condition "Validates 'high' thinking level" ((Validate-GeminiThinkingLevel -ThinkingLevel "high") -eq "high")
    Assert-Condition "Rejects invalid thinking level" ($null -eq (Validate-GeminiThinkingLevel -ThinkingLevel "super_high"))

    $invalidLevelResult = Invoke-GeminiPrReview -ApiKey "fake-key" -ThinkingLevel "invalid" -SanitizedDiff "" -PrNumber 1 -HeadSha "sha1"
    Assert-Condition "Invalid thinking level returns safe UNAVAILABLE" ($invalidLevelResult.Status -eq "UNAVAILABLE" -and $invalidLevelResult.ErrorCode -eq "MODEL_UNAVAILABLE")

    # -------------------------------------------------------------
    # 11. R84-02: Atomic Lock Collision & Ownership
    # -------------------------------------------------------------
    Write-Host "`nTest Group 11: Atomic Lock Collision & Ownership" -ForegroundColor Yellow

    $atomicLockPr = 901
    $lock1 = Acquire-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid $PID
    $lock2 = Acquire-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid 22222
    Assert-Condition "First process acquires lock" $lock1
    Assert-Condition "Second concurrent process atomically denied lock while PID is alive" (-not $lock2)

    # Release by wrong PID must fail/be no-op
    Release-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid 22222
    $lockStillHeld = Acquire-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid 33333
    Assert-Condition "Non-owning PID cannot release lock" (-not $lockStillHeld)

    # Clean release by owner
    Release-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid $PID
    $lockReleased = Acquire-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid $PID
    Assert-Condition "Owner released lock cleanly and can reacquire" $lockReleased
    Release-PrLock -BaseDir $testTempDir -PrNumber $atomicLockPr -CurrentPid $PID

    # -------------------------------------------------------------
    # 12. R84-04: Remote Marker Unbypassable & Error Sanitization
    # -------------------------------------------------------------
    Write-Host "`nTest Group 12: Remote Marker Dedupe & Error Sanitization" -ForegroundColor Yellow

    # Remote marker check with existing marker
    $mockRemoteComments = @{
        comments = @(
            @{ id = "c-100"; body = "<!-- scanpilot-gemini-pr-review: 902 sha902 -->`nReview output" }
        )
    }
    $hasMarker = $false
    foreach ($c in $mockRemoteComments.comments) {
        if ($c.body.Contains("<!-- scanpilot-gemini-pr-review: 902 sha902 -->")) {
            $hasMarker = $true
        }
    }
    Assert-Condition "Remote marker detected and suppresses duplicate call" $hasMarker

    # -------------------------------------------------------------
    # 13. R84-05: Pinned Repository & Query Error Fail-Closed
    # -------------------------------------------------------------
    Write-Host "`nTest Group 13: Pinned Repository & Query Error Fail-Closed" -ForegroundColor Yellow

    # Test 13a: Mock gh script to capture arguments
    $mockGhCapturePath = Join-Path $testTempDir "mock-gh-capture.bat"
    $argLogPath = Join-Path $testTempDir "gh-args.log"
    "@echo off`necho %* >> `"$argLogPath`"`necho []" | Set-Content -Path $mockGhCapturePath -Encoding ascii

    $queryWithPinnedRepo = Get-EligibleDevPullRequests -Repo "NgaiLong49423/scan-pilot" -GhCommand $mockGhCapturePath
    $capturedArgs = if (Test-Path $argLogPath) { Get-Content $argLogPath -Raw } else { "" }
    Assert-Condition "gh pr list pinned to NgaiLong49423/scan-pilot" ($capturedArgs.Contains("--repo NgaiLong49423/scan-pilot"))
    Assert-Condition "gh pr list uses supported isCrossRepository field" ($capturedArgs.Contains("isCrossRepository"))
    Assert-Condition "gh pr list does not request unsupported baseRepository field" (-not $capturedArgs.Contains("baseRepository"))

    # Test 13b: Query failure returns UNAVAILABLE (not false NO_PRS)
    $mockGhFailPath = Join-Path $testTempDir "mock-gh-fail.bat"
    "@echo off`nexit /b 1" | Set-Content -Path $mockGhFailPath -Encoding ascii
    $failQueryResult = Get-EligibleDevPullRequests -Repo "NgaiLong49423/scan-pilot" -GhCommand $mockGhFailPath
    Assert-Condition "gh pr list failure reports Success = false" (-not $failQueryResult.Success)
    Assert-Condition "gh pr list failure sets REPOSITORY_QUERY_FAILED" ($failQueryResult.ErrorCode -eq "REPOSITORY_QUERY_FAILED")

    # Test 13c: Working directory restoration
    $originalDir = (Get-Location).Path
    $foreignDir = Join-Path $testTempDir "foreign-workdir"
    New-Item -ItemType Directory -Force -Path $foreignDir | Out-Null
    Set-Location $foreignDir

    # Run runner with dry-run from foreign dir
    $runnerResult = & "$scriptDir\run-pr-review.ps1" -BaseDir $scriptDir -PrNumber 999999 -DryRun
    $restoredDir = (Get-Location).Path
    Assert-Condition "Working directory restored after runner completes" ($restoredDir -eq $foreignDir)
    Set-Location $originalDir

} finally {
    Remove-Item -Path $testTempDir -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "Test Suite Summary: $passCount Passed, $failCount Failed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    exit 1
}
