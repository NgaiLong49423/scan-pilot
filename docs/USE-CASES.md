> **Document:** Scan Pilot Use Cases Specification
> **File:** `docs/USE-CASES.md`
> **Version:** v1.0.0
> **Created:** 2026-08-19
> **Last Updated:** 2026-08-19
> **Status:** Active

# Scan Pilot Use Cases Specification

This document defines the canonical Use Cases (`UC-001` through `UC-006`) for the Scan Pilot MVP, detailing the interaction flows between the user, the React frontend, the Spring Boot backend, the PostgreSQL database, GitHub APIs, and Google Gemini AI.

---

## Use Case Summary Map

| UC ID | Use Case Title | Primary Actor | Target Module | Traceability |
|---|---|---|---|---|
| **UC-001** | GitHub Authentication & App Installation Linking | Developer / User | Auth & GitHub App | `FR-001`, `FR-046`, `FR-047`, `DEC-045`, `Issue #19`, `Issue #14` |
| **UC-002** | Monitored Repository Selection & Branch Slot Configuration | Developer / User | Project & Repo Config | `FR-001`, `FR-020`, `FR-022`, `FR-023`, `DEC-046`, `Issue #14` |
| **UC-003** | On-Demand & Continuous Security Scan Execution | Developer / User | Scan Pipeline | `FR-002`, `FR-003`, `FR-016`, `FR-025`, `DEC-015`, `DEC-040`, `Issue #23` |
| **UC-004** | Finding Inspection & AI-Assisted Remediation | Developer / User | Finding & AI Engine | `FR-004`, `FR-005`, `FR-014`, `FR-048`, `DEC-007`, `DEC-048`, `Issue #16`, `Issue #21` |
| **UC-005** | Re-scan Lifecycle Tracking & Resolution Verification | Developer / User | Lifecycle Engine | `FR-007`, `FR-018`, `FR-019`, `FR-051`, `DEC-012`, `Issue #23` |
| **UC-006** | Transparent Scan Coverage & Skipped Content Audit | Developer / User | Classifier & Coverage | `FR-028`, `FR-029`, `FR-031`, `FR-034`, `FR-035`, `FR-037`, `Issue #15` |

---

## Detailed Use Cases

### UC-001: GitHub Authentication & App Installation Linking

* **Primary Actor:** Developer / Security Engineer.
* **Preconditions:** User has an active personal GitHub account with internet access.
* **Trigger:** User clicks "Sign in with GitHub" on the Scan Pilot web application.

#### Main Success Scenario (Happy Path):
1. User navigates to the Scan Pilot landing page and clicks **"Sign in with GitHub"**.
2. Frontend redirects the browser to `GET /api/v1/auth/github/login`.
3. Backend generates a cryptographically secure random `state` token, stores it with a 10-minute TTL, and redirects the user to GitHub OAuth authorize URL (`https://github.com/login/oauth/authorize`).
4. User authenticates on GitHub and grants permission (`read:user,user:email`).
5. GitHub redirects the browser back to `GET /api/v1/auth/github/callback?code=...&state=...`.
6. Backend validates the `state` parameter, exchanges the authorization code for a server-side access token, fetches the user's profile (`id`, `login`, `name`, `avatar_url`, `email`), creates or updates `UserEntity` and `UserSessionEntity`, and sets a secure `SCANPILOT_SESSION` HttpOnly cookie.
7. Backend redirects the user to the Frontend dashboard (`/`).
8. Frontend calls `GET /api/v1/auth/me`, receives `UserProfileDto`, and renders the authenticated header with avatar and username.

#### Extensions (Alternative & Error Flows):
* **3a. State mismatch / CSRF attack:**
  * Backend detects invalid or expired `state`.
  * Backend redirects to frontend with query parameter `?error=invalid_state`.
  * Frontend renders an error toast: "Authentication session expired. Please try signing in again."
* **4a. User denies GitHub authorization:**
  * GitHub redirects with `?error=access_denied`.
  * Backend handles error and redirects to frontend with `?error=access_denied`.
  * Frontend displays a friendly cancel state.
* **8a. Session Expired:**
  * If the session cookie is missing or invalid, `GET /api/v1/auth/me` returns `401 Unauthorized`.
  * Frontend clears user state and transitions to the signed-out landing view.

#### Postconditions:
* Authenticated user session persisted in PostgreSQL `user_sessions`.
* Access token stored server-side only; never leaked to client or browser JavaScript.

---

### UC-002: Monitored Repository Selection & Branch Slot Configuration

* **Primary Actor:** Authenticated Developer.
* **Preconditions:** User is signed in (`UC-001`) and has installed the Scan Pilot GitHub App on at least one personal repository.
* **Trigger:** User opens the Repository Selection screen or clicks "Change Monitored Repository".

#### Main Success Scenario:
1. Frontend calls `GET /api/v1/github/repositories` with active session.
2. Backend queries GitHub API using the user's ephemeral token / installation token and returns accessible personal repositories with their default branches.
3. Frontend renders the repository list with search and selection indicators.
4. User selects a repository (e.g., `user/web-app`) and clicks **"Monitor This Repository"**.
5. Frontend sends `POST /api/v1/projects/select-repository` with payload `{ "githubRepoId": 123456, "fullName": "user/web-app" }`.
6. Backend enforces the 1-monitored-repository policy (`DEC-046`), registers `RepositoryEntity`, derives the `PRIMARY` branch from GitHub's default branch (`FR-020`, `FR-022`), and initializes `MonitoredBranchEntity`.
7. User navigates to Branch Settings and selects up to 2 additional secondary branches (e.g., `staging`, `develop`).
8. Frontend sends `PUT /api/v1/projects/branches` with `{ "secondaryBranches": ["staging", "develop"] }`.
9. Backend validates that secondary branch count does not exceed 2 slots (`FR-020`, `FR-023`) and updates `MonitoredBranchEntity` records.
10. Frontend displays the active monitored project badge and branch selector.

#### Extensions:
* **2a. GitHub App not yet installed:**
  * If no repositories are found, Frontend displays a prompt: "Install Scan Pilot GitHub App on your repositories" with a direct link to `GET /api/v1/github/install-url`.
* **8a. User attempts to configure > 2 secondary branches:**
  * Backend rejects the request with `400 Bad Request` (`"Maximum 2 secondary branches allowed"`).
  * Frontend highlights the branch slot limit and disables adding further branches.

#### Postconditions:
* Active repository profile and branch configurations persisted in `repositories` and `monitored_branches`.

---

### UC-003: On-Demand & Continuous Security Scan Execution

* **Primary Actor:** Authenticated Developer.
* **Preconditions:** A repository is selected for monitoring (`UC-002`).
* **Trigger:** User clicks **"Trigger Scan"** on the Dashboard or a GitHub webhook is received.

#### Main Success Scenario:
1. User clicks **"Trigger Scan"** on the Dashboard.
2. Frontend sends `POST /api/v1/scans/trigger` with `{ "branch": "main", "scanMode": "CONTINUOUS_MONITORING" }`.
3. Backend creates a `ScanJobEntity` with status `PENDING` and returns `{ "jobId": "...", "status": "PENDING" }`.
4. Frontend enters real-time polling mode (`GET /api/v1/scans/jobs/{jobId}` every 1.5s) displaying a visual progress indicator.
5. Backend scan pipeline worker picks up the job:
   * Transitions job status to `RUNNING`.
   * Provisions an isolated temporary workspace directory (`scanpilot-ws-<repoId>-...`) via `GitWorkspaceManager`.
   * Fetches the current commit snapshot and determines current HEAD SHA.
   * Runs `FileEligibilityEngine` to classify all files, logging `CoverageRecordEntity` and `CoverageItemEntity`.
   * **Stage 1 (Snapshot Scan):** Executes `GitleaksDetectorAdapter.forSnapshot()` against current HEAD text files (`FR-025`).
   * **Stage 2 (History Scan):** Executes `GitleaksDetectorAdapter.forGitHistory()` traversing reachable Git commits.
   * Normalizes findings via `SecretRedactionService` generating `SP_SECRET_FP_V1` fingerprints and masked evidence.
   * Applies `FindingLifecycleEngine` updating `findings`, `finding_locations`, and `evidence_items`.
   * Validates coverage; advances `ScanCheckpointEntity` if valid (`FR-028`, `FR-029`).
   * Disposes of the temporary workspace directory completely (100% recursive cleanup).
   * Marks `ScanJobEntity` status as `COMPLETED`.
6. Frontend poll receives `status: "COMPLETED"`, stops polling, and seamlessly refreshes the Dashboard metrics and Finding lists without a full page reload.

#### Extensions:
* **5a. Scan failure or unexpected exception:**
  * Workspace is guaranteed to be deleted in `try-finally` block.
  * Backend marks `ScanJobEntity` as `FAILED` with detailed `errorMessage`.
  * Frontend polling detects `FAILED` status and renders an error banner with a "Retry Scan" action.

#### Postconditions:
* Full scan telemetry recorded in `scan_jobs`.
* Verified coverage recorded in `coverage_records`.
* Temporary workspace completely purged from disk.

---

### UC-004: Finding Inspection & AI-Assisted Remediation

* **Primary Actor:** Developer reviewing security alerts.
* **Preconditions:** A scan has completed with at least one detected secret finding (`UC-003`).
* **Trigger:** User clicks on a Finding card in the Dashboard.

#### Main Success Scenario:
1. User clicks on a Finding card (e.g., `SP-CONFIG-001 — Google API Key exposed`).
2. Frontend expands the Finding detail drawer/view showing:
   * File path and line numbers (e.g., `src/main/resources/application.yml:L12`).
   * Masked secret preview (e.g., `AIzaSyD...9xZ4`) with click-to-copy masked token.
   * Redacted code snippet with syntax highlighting and `[REDACTED_SECRET]` badge.
   * Immutable finding fingerprint `SP_SECRET_FP_V1` (64-character hex digest).
   * Current Lifecycle badge (`OPEN`) and Remediation Quality badge (`ACTION_REQUIRED`).
3. Frontend requests AI explanation via `POST /api/v1/ai/findings/{findingId}/explain`.
4. Backend `GeminiExplanationService` validates request (zero raw secret guarantee), checks local fingerprint cache:
   * If cached: returns `GeminiExplanationResponse` immediately (< 50ms).
   * If not cached: constructs bounded prompt with redacted evidence, calls Google Gemini API (`gemini-1.5-flash`) with structured JSON schema enforcement, parses response, attaches `EvidenceItemEntity` (`AI_INFERENCE`), and caches result.
5. Frontend renders the AI Remediation card displaying:
   * **Plain-Language Summary:** What happened in accessible language.
   * **Risk Impact:** Real-world consequences if exploited.
   * **Evidence Limits:** What this scan proves and does not prove.
   * **Actionable Steps:** Step-by-step resolution checklist.
   * **Before / After Code Diff:** Syntax-highlighted diff showing how to replace hardcoded secret with environment variable `System.getenv("API_KEY")`.
   * **Key Revocation Command:** Guided command to revoke the exposed key.

#### Extensions:
* **4a. Gemini API Key missing, quota exceeded, or network timeout (> 15s):**
  * Backend automatically returns deterministic fallback template guidance for the specific rule family (`google-api-key`, `github-pat`, `aws-access-key`, `private-key`, `generic`).
  * Response is attributed as `AI Inferred Guidance (Deterministic Fallback)`.
  * Frontend renders the remediation guidance smoothly without throwing any UI errors.

#### Postconditions:
* `EvidenceItemEntity` of type `AI_INFERENCE` recorded in PostgreSQL.
* Zero raw secrets transmitted to Gemini or displayed on UI.

---

### UC-005: Re-scan Lifecycle Tracking & Resolution Verification

* **Primary Actor:** Developer verifying security fixes.
* **Preconditions:** A finding was previously recorded with status `OPEN / ACTION_REQUIRED` (`UC-004`).
* **Trigger:** Developer modifies the repository and triggers a re-scan.

#### Scenario A: Current Code Fixed, Secret Still in Git History (`RISK_CONTAINED`)
1. Developer removes the hardcoded secret from current source code, commits and pushes to GitHub.
2. Developer triggers a re-scan in Scan Pilot (`UC-003`).
3. Scan pipeline detects:
   * Secret is **ABSENT** at current HEAD snapshot.
   * Secret is **PRESENT** in reachable Git commit history.
4. `FindingLifecycleEngine` transitions finding:
   * Lifecycle: `RESOLVED`.
   * Remediation Quality: `RISK_CONTAINED` (Yellow warning: "Current code is safe, but secret exists in historical commits").
5. Dashboard updates Health Score, moves finding to "Resolved" tab with reassurance notice.

#### Scenario B: Git History Cleaned via History Rewrite (`VERIFIED_COMPLETE`)
1. Developer runs `git filter-repo` or BFG Repo-Cleaner to purge the secret from all commits, and force-pushes.
2. Developer triggers a re-scan in Scan Pilot.
3. Scan pipeline detects:
   * Secret is **ABSENT** at HEAD snapshot.
   * Secret is **ABSENT** from all reachable Git history.
4. `FindingLifecycleEngine` transitions finding:
   * Lifecycle: `RESOLVED`.
   * Remediation Quality: `VERIFIED_COMPLETE` (Green success: "Verified completely clean across all commits").
5. Dashboard awards full verified status.

#### Scenario C: Accidental Re-introduction / Regression (`REGRESSED`)
1. Developer accidentally restores or re-pastes the exposed secret in a new commit.
2. Scan pipeline detects secret present at current HEAD.
3. `FindingLifecycleEngine` transitions finding:
   * Lifecycle: `REGRESSED`.
   * Remediation Quality: `ACTION_REQUIRED` (Red critical alert: "Previously resolved finding has regressed!").
4. Dashboard raises high-priority notification to developer.

---

### UC-006: Transparent Scan Coverage & Skipped Content Audit

* **Primary Actor:** Auditor / Security Lead / Developer.
* **Preconditions:** A scan has completed (`UC-003`).
* **Trigger:** User navigates to the "Coverage & Audit" tab.

#### Main Success Scenario:
1. User clicks the **"Coverage"** tab on the Dashboard.
2. Frontend calls `GET /api/v1/scans/repositories/{repositoryId}/coverage`.
3. Backend returns `CoverageSummaryDto` containing:
   * Total files inventoried, scanned text files count, skipped binary files count.
   * Total bytes evaluated and overall `CoverageImpact` (`COMPLETE`, `PARTIAL`, `INCOMPLETE`).
   * List of `CoverageItemDto` for all skipped items with path, size, classification, and stable `reasonCode`.
4. Frontend renders:
   * Coverage Health Donut Chart (% scanned text vs % skipped binary/unsupported).
   * Scanned Files Table with size and classification.
   * **Skipped Files Table** with reason badges:
     - `UNSUPPORTED_BINARY_DOCUMENT`: PDF, DOCX, XLSX files (`FR-034`, `DEC-034`).
     - `UNSUPPORTED_BINARY_FILE`: PNG, JPG, EXE, DLL, ZIP binaries.
     - `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`: Files > 10 MiB in Continuous mode (`FR-037`).
     - `RELEASE_FILE_SIZE_CEILING_EXCEEDED`: Files > 50 MiB in Release mode (`FR-037`).
5. User filters by skip reason to audit repository surface.

#### Postconditions:
* Transparent audit record available for inspection without relying on volatile application logs.
