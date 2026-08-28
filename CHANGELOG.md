> **Document:** Scan Pilot Changelog
> **File:** `CHANGELOG.md`
> **Version:** v2.37.0
> **Created:** 2026-08-11
> **Last Updated:** 2026-08-28
> **Status:** Active

# Scan Pilot Changelog

This file records notable Scan Pilot changes as a chronological, human-readable history. Git remains the exact file-level source of truth.

## 2026-08-28 — Authenticated Shallow Git Clone & History Traversal (Issue #49)

**Status:** Working tree (pre-commit)

**Scope:** Implemented authenticated shallow Git clone (`git clone --depth 50 --single-branch`) and Git history traversal across the reachable shallow clone (up to 50 commits) for secret detection in Git commit diffs (FR-025, DEC-012, DEC-015). Token transport is strictly isolated to environment variables (`GIT_CONFIG_COUNT`, `GIT_CONFIG_KEY_0`, `GIT_CONFIG_VALUE_0`, `GIT_TERMINAL_PROMPT=0`) with zero tokens or credentials in command line arguments (argv), `.git/config`, or log outputs. Enforced untrusted repository execution hardening using `-c core.hooksPath` pointing to an isolated controlled `.empty-hooks` directory, `-c core.fsmonitor=false`, `--no-recurse-submodules`, and `--no-tags`. Implemented active background watchdog monitoring workspace size against a 120 MiB operational stop threshold (80% watermark) and cumulative job deadlines with fail-closed tree process termination. Hardened backend Dockerfile runner stage with pinned Git and Gitleaks v8.24.0 verified with official SHA-256 integrity checksums. Added CI container smoke checks verifying runtime binaries.

### Added

- Added `GitCloneProperties` configuration with default depth 50, max depth 100, 60s timeout, 120 MiB watchdog threshold, and 250ms polling interval.
- Added `GitCloneService` implementing shallow Git clone with environment-only credential transport, `.empty-hooks` hook suppression, active directory size watchdog, and process-tree termination.
- Added `GitCloneServiceTest` verifying command construction (zero token in argv), environment credential isolation, empty hooks isolation, and active watchdog size/timeout aborts.
- Added `ScanPipelineGitHistoryTest` verifying end-to-end Git history secret detection, commit SHA recording, and lifecycle resolution (`RESOLVED` / `RISK_CONTAINED`).

### Changed

- Updated `ScanPipelineService` to utilize `GitCloneService` for repository acquisition, enabling Stage 2 Git history traversal on reachable commits.
- Updated `GitleaksDetectorAdapter` to support both `commitRange` and `logOpts` for Git history scans.
- Updated `backend/Dockerfile` runner stage to install pinned Git and Gitleaks v8.24.0 with official SHA-256 checksum verification.
- Updated `.github/workflows/ci.yml` backend job with container runtime binary smoke check.

## 2026-08-27 — Truthful Security Action Summary (Issue #71)

**Status:** Committed (`613a6a5`)

**Scope:** Replaced synthetic health score formulas (e.g. `/100`), letter grades (`Grade A/B/C`), fabricated 30-day sparklines, and unverified AI Fix Ready counts with an evidence-based `SecurityActionSummary` (DEC-060). Implemented typed `ApiResult<T>` discriminated unions in the API client to prevent swallowing HTTP 5xx errors and network drops. Created pure, deterministic `postureResolver` following a strict 7-rule precedence hierarchy with zero diagnostic leakage (`ScanJobDto.errorMessage` is never displayed in user-facing copy). Created `SecurityActionSummaryCard.tsx` with high contrast, WCAG AA compliance, and operable fresh retry capabilities. Updated `FleetDashboard.tsx` and `App.tsx` to display verified evidence states, authentic fleet severity breakdown (Crit/High/Med/Low), and explicit separation of unavailable evidence.

### Added

- Added `ApiResult<T>`, `RepositoryPostureStatus`, `FindingSeverityCounts`, and `SecurityActionSummary` interfaces to `frontend/src/types/index.ts`.
- Added pure `resolveRepositoryPosture` function in `frontend/src/services/postureResolver.ts` enforcing 7-rule precedence and safe error messages.
- Added comprehensive unit tests in `frontend/src/services/postureResolver.test.ts`.
- Added `frontend/src/services/api.test.ts` testing `ApiResult<T>` mapping on HTTP 200, 404, 500, network errors, and malformed responses.
- Added `frontend/src/components/SecurityActionSummaryCard.tsx` providing truthful visual representations across all six evidence-based posture states: `ACTION_REQUIRED`, `NO_OPEN_FINDINGS`, `COVERAGE_INCOMPLETE`, `AWAITING_INITIAL_SCAN`, `SCAN_IN_PROGRESS`, and `SCAN_UNAVAILABLE`.
- Added `frontend/src/components/SecurityActionSummaryCard.test.tsx` verifying component markup via `renderToStaticMarkup`.
- Added `frontend/src/components/FleetDashboard.test.tsx` verifying fleet aggregation, severity distributions, and posture badges.

### Changed

- Updated `frontend/src/services/api.ts` so `fetchFindingsForRepo` and `fetchCoverageForRepo` return typed `ApiResult<T>` without swallowing failures.
- Updated `frontend/src/components/FleetDashboard.tsx` to remove synthetic `/100` scores and letter grades, replacing them with authentic posture badges, severity distribution pills, and explicit evidence availability counts.
- Updated `frontend/src/App.tsx` to remove deduction math and top 3-card analytics grid, wiring `SecurityActionSummaryCard` to verified PostgreSQL evidence and propagating `severityCounts` across all repository lifecycle states.

### Removed

- Removed synthetic health score deduction formulas and `HealthMetrics` type.
- Removed obsolete `HealthGauge.tsx`, `TrendSparkline.tsx`, and `MetricsGrid.tsx` components.

## 2026-08-26 — Real Scan Event Telemetry & Truthful Terminal Progress (Issue #69)

**Status:** Committed (`06c01df`)

**Scope:** Implemented real-time, monotonic scan event telemetry and truthful UI progress streaming (FR-002, NFR-001). Replaced artificial progress bar and polling mocks with real backend scan events persisted durably in PostgreSQL (`scan_events` table via Flyway V4), allocated with an atomic single-statement CTE counter (`next_event_sequence`) bounded by a 100-event cap. Exposed `GET /api/v1/scans/jobs/{jobId}/events` with fail-closed repository authorization and sequence-based pagination. Added truthful client-side draining logic, safe message translation without raw secret leakage, and Vitest test runner to frontend CI workflow.

### Added

- Added Flyway migration `V4__add_scan_events_telemetry.sql` adding `next_event_sequence` column to `scan_jobs` and creating `scan_events` table with unique sequence indexing.
- Added `ScanEventEntity` and `ScanEventRepository` supporting native PostgreSQL CTE atomic event sequence allocation with cap protection.
- Added `ScanEventDto` and `ScanEventsResponse` DTOs.
- Added `SnapshotTransferMetrics` record returning archive size, extracted workspace size, and entry count from `StreamedSnapshotFetcher`.
- Added `GET /api/v1/scans/jobs/{jobId}/events` endpoint in `ScanController` with fail-closed authorization and pagination.
- Added `ScanEventRepositoryPostgresTest` verifying atomic CTE sequence allocation, cap clamping, and rollback consistency with Testcontainers PostgreSQL.
- Added `frontend/src/services/telemetryPolling.ts` and `telemetryPolling.test.ts` for truthful event sequence draining.
- Added `Vitest` test runner and updated frontend CI in `.github/workflows/ci.yml`.

### Changed

- Updated `ScanJobDispatcher` and `ScanPipelineService` to emit milestone telemetry events (`STAGE_STARTED`, `SNAPSHOT_FETCHED`, `FILES_CLASSIFIED`, `SCANNER_ACTIVE`, `FINDING_ALERT`, `FINDINGS_TRUNCATED`, `RECORDING_EVIDENCE`, `JOB_COMPLETED`, `JOB_FAILED`, `GUARDRAIL_LIMIT_HIT`).
- Updated `LiveScanTerminal.tsx` to safely map backend telemetry event codes to human-readable logs and render authentic stage states and timers.
- Updated `App.tsx` rescan handler to stream live events using sequence cursors and drain terminal states completely before fetching finalized findings.

## 2026-08-25 — GitHub Snapshot Resource Limits and Guardrails (Issue #67)

**Status:** Committed (`2b1e753`)

**Scope:** Enforced robust resource guardrails on GitHub snapshot downloads and workspace extractions to prevent container OOM, disk exhaustion, zip-bombs, and hangs on Cloud Run (NFR-001, FR-028, FR-031, DEC-036). Implemented streaming download with a 20 MiB archive ceiling, streaming extraction with a 150 MiB uncompressed workspace ceiling and 10,000 archive entries ceiling, canonical multi-platform Zip-Slip path traversal defense, and an overarching 180-second whole scan-job cumulative deadline across all pipeline stages coupled with a Gitleaks process-tree watchdog. Enforced fail-closed coverage reporting where guardrail limits record `ScanJobEntity.status=COMPLETED` and `CoverageImpact=INCOMPLETE`, persist `reason_code` and `limit_hit_value` in PostgreSQL via Flyway V3, strictly block scan checkpoint advancement, and render truthful neutral metrics (`—`) and warning banners without claiming Clean or 100% Safe.

### Added

- Added Flyway migration `V3__add_coverage_guardrail_telemetry.sql` adding `reason_code` and `limit_hit_value` columns to `coverage_records`.
- Added `SnapshotGuardrailProperties` configuring maximum archive download bytes (20 MiB), maximum uncompressed workspace bytes (150 MiB), maximum entry count (10,000), and cumulative whole scan-job timeout (180 seconds).
- Added `overrideTimeoutSeconds` parameter and factory overloads to `GitleaksScanRequest` for dynamically allocating remaining job deadline to detector stages.
- Added `ResourceGuardrailExceededException` carrying structured reason code, observed bytes/files, and limit hit value.
- Added `StreamedSnapshotFetcher` for bounded streaming archive download, multi-platform Zip-Slip traversal rejection, streaming extraction limits, and overarching job deadline enforcement across HTTP transfer, entry iterations, and buffer writes.
- Added `CoverageWarningBanner.tsx` in frontend for truthful warning disclosure when scan coverage is incomplete due to safety limits.
- Added unit and integration tests: `StreamedSnapshotFetcherTest`, `ScanPipelineGuardrailTest`, and `GitleaksConfigPropertiesTest`.

### Changed

- Updated `GitleaksConfigProperties` default production watchdog timeout to 180 seconds and synchronized `application.yml`.
- Updated `GitleaksDetectorAdapter` to calculate effective timeout per request (`overrideTimeoutSeconds`), forcibly terminate process trees (`ProcessHandle.descendants()`), await exit on timeout, and propagate typed `ResourceGuardrailExceededException("SCAN_TIMEOUT")` without modifying singleton configuration properties or falling back to embedded scan.
- Updated `ScanPipelineService` across both asynchronous `executeScanJob` and synchronous `executeScan` entry points to establish an immutable whole scan-job deadline (180s), check deadline bounds prior to each pipeline stage, propagate `jobDeadline` into snapshot download and extraction stages (`StreamedSnapshotFetcher`) as well as detector stages (`GitleaksDetectorAdapter`), catch guardrail exceptions, record early `CoverageRecordEntity(coverageImpact="INCOMPLETE")`, and strictly block `ScanCheckpointEntity` advancement while guaranteeing workspace cleanup in `finally`.
- Updated `CoverageSummaryDto`, `frontend/src/types/index.ts`, and `frontend/src/types/api.ts` with reason code and limit telemetry.
- Updated `App.tsx`, `HealthGauge.tsx`, and `FleetDashboard.tsx` to handle incomplete coverage by rendering neutral health score (`—`) rather than claiming a false 100/100 or Grade A.

### Fixed

- Fixed Zip-Slip path traversal handling to reject Unix (`/`), Windows (`\`), drive-qualified (`:`), UNC, and `..` segments before stripping GitHub wrapper prefixes.
- Fixed HTTP response stream handling in `StreamedSnapshotFetcher` to ensure `response.body()` is closed via try-with-resources on non-2xx status codes without exposing raw error bodies.
- Fixed benchmark test harness in `IndependentSecretBenchmarkTest` to isolate report generation to temporary directories unless explicitly configured.

### Affected files

- `backend/src/main/java/com/scanpilot/persistence/entity/CoverageRecordEntity.java`
- `backend/src/main/java/com/scanpilot/scanner/config/SnapshotGuardrailProperties.java`
- `backend/src/main/java/com/scanpilot/scanner/detector/gitleaks/GitleaksConfigProperties.java`
- `backend/src/main/java/com/scanpilot/scanner/detector/gitleaks/GitleaksDetectorAdapter.java`
- `backend/src/main/java/com/scanpilot/scanner/detector/gitleaks/GitleaksScanRequest.java`
- `backend/src/main/java/com/scanpilot/scanner/dto/CoverageSummaryDto.java`
- `backend/src/main/java/com/scanpilot/scanner/exception/ResourceGuardrailExceededException.java`
- `backend/src/main/java/com/scanpilot/scanner/pipeline/ScanPipelineService.java`
- `backend/src/main/java/com/scanpilot/scanner/pipeline/StreamedSnapshotFetcher.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/V3__add_coverage_guardrail_telemetry.sql`
- `backend/src/test/java/com/scanpilot/benchmark/IndependentSecretBenchmarkTest.java`
- `backend/src/test/java/com/scanpilot/persistence/CoverageRecordAndItemPersistenceTest.java`
- `backend/src/test/java/com/scanpilot/persistence/FlywaySchemaMigrationTest.java`
- `backend/src/test/java/com/scanpilot/scanner/detector/gitleaks/GitleaksConfigPropertiesTest.java`
- `backend/src/test/java/com/scanpilot/scanner/detector/gitleaks/GitleaksDetectorAdapterTest.java`
- `backend/src/test/java/com/scanpilot/scanner/pipeline/ScanPipelineGuardrailTest.java`
- `backend/src/test/java/com/scanpilot/scanner/pipeline/ScanPipelineServiceTest.java`
- `backend/src/test/java/com/scanpilot/scanner/pipeline/StreamedSnapshotFetcherTest.java`
- `frontend/src/App.tsx`
- `frontend/src/components/CoverageWarningBanner.tsx`
- `frontend/src/components/FleetDashboard.tsx`
- `frontend/src/components/HealthGauge.tsx`
- `frontend/src/types/api.ts`
- `frontend/src/types/index.ts`

## 2026-08-24 — Asynchronous Scan Job Dispatch and Real Progress (Issue #52)

**Status:** Committed (`86d6637`)

**Scope:** Implemented non-blocking scan execution and real stage tracking (FR-002, NFR-001). Scan trigger endpoint returns HTTP 202 Accepted immediately with queued status, a bounded in-process worker queue handles asynchronous execution, and monotonic stages (QUEUED, FETCHING_SNAPSHOT, CLASSIFYING_FILES, SCANNING_SECRETS, RECORDING_EVIDENCE, COMPLETED/FAILED) are persisted in PostgreSQL. Implemented database-level pessimistic locking for cross-instance duplicate prevention, task-scoped heartbeat liveness, and atomic fail-closed restart/stale recovery (~2.5m SLA). Added bounded frontend interval polling with terminal error handling.

### Added

- Added Flyway migration `V2__add_scan_job_stage_and_created_at.sql` adding stage, timestamps, worker instance ID, and heartbeat columns to `scan_jobs`.
- Added `ScanExecutorConfig` with bounded `ThreadPoolTaskExecutor` (1 worker, queue capacity 10, AbortPolicy) and `ScanCapacityExceededException` (HTTP 429).
- Added `ScanJobDispatcher` with pessimistic row locking (`findByIdForUpdate`) for atomic cross-instance duplicate scan prevention and post-commit worker execution.
- Added `ScanWorkerHeartbeatScheduler` for active queued jobs and task-scoped heartbeat execution in `ScanPipelineService` for running scans.
- Added atomic conditional stale job recovery in `ScanJobRestartReconciler` (`reconcileStaleJobsAtomic`) running at startup and periodically every 30 seconds.
- Added `fetchScanJob` in `frontend/src/services/api.ts` distinguishing terminal (401, 403, 404) vs transient errors.

### Changed

- Updated `POST /api/v1/scans/trigger` to dispatch asynchronously and return HTTP 202 Accepted prompt response with repository UUID identity.
- Updated `GET /api/v1/scans/jobs/{jobId}` to enforce user ownership check (HTTP 404 for non-owners).
- Updated `ScanProgressStepper.tsx` to dynamically display persisted backend scan stages and sanitized failure messages.
- Updated `App.tsx` with bounded interval polling (1.5s), 5-error retry limit, and cleanup on unmount/repo change.
- Refactored all loggers in scan dispatcher and pipeline to structured sanitized format, scrubbing sensitive tokens and credentials.

### Affected files

- `backend/src/main/java/com/scanpilot/persistence/entity/ScanJobEntity.java`
- `backend/src/main/java/com/scanpilot/persistence/repository/RepositoryRepository.java`
- `backend/src/main/java/com/scanpilot/persistence/repository/ScanJobRepository.java`
- `backend/src/main/java/com/scanpilot/scanner/config/ScanExecutorConfig.java`
- `backend/src/main/java/com/scanpilot/scanner/config/ScanWorkerInstance.java`
- `backend/src/main/java/com/scanpilot/scanner/controller/ScanController.java`
- `backend/src/main/java/com/scanpilot/scanner/dispatcher/ScanJobDispatcher.java`
- `backend/src/main/java/com/scanpilot/scanner/dto/ScanJobDto.java`
- `backend/src/main/java/com/scanpilot/scanner/dto/ScanTriggerResponse.java`
- `backend/src/main/java/com/scanpilot/scanner/exception/ScanCapacityExceededException.java`
- `backend/src/main/java/com/scanpilot/scanner/lifecycle/ScanJobRestartReconciler.java`
- `backend/src/main/java/com/scanpilot/scanner/lifecycle/ScanWorkerHeartbeatScheduler.java`
- `backend/src/main/java/com/scanpilot/scanner/pipeline/ScanPipelineService.java`
- `backend/src/main/java/com/scanpilot/system/GlobalExceptionHandler.java`
- `backend/src/main/resources/db/migration/V2__add_scan_job_stage_and_created_at.sql`
- `backend/src/test/java/com/scanpilot/persistence/FlywaySchemaMigrationTest.java`
- `backend/src/test/java/com/scanpilot/scanner/controller/ScanControllerTest.java`
- `backend/src/test/java/com/scanpilot/scanner/dispatcher/ScanJobDispatcherTest.java`
- `backend/src/test/java/com/scanpilot/scanner/lifecycle/ScanJobRestartReconcilerTest.java`
- `backend/src/test/java/com/scanpilot/scanner/pipeline/ScanPipelineServiceTest.java`
- `frontend/src/App.tsx`
- `frontend/src/components/ScanProgressStepper.tsx`
- `frontend/src/services/api.ts`
- `CHANGELOG.md`

## 2026-08-22 — Production PostgreSQL Fail-Closed Persistence (Issue #53)

**Status:** Working tree — traceability correction for implementation commit `833ffe3`

**Scope:** Hardened the production persistence boundary so Scan Pilot requires PostgreSQL and production security configuration rather than silently falling back to ephemeral local defaults. The production deployment path now uses Cloud SQL, Secret Manager, and a dedicated least-privilege runtime identity. The Fleet also distinguishes a verified empty repository list from backend unavailability.

### Fixed

- Removed the production H2 fallback and required PostgreSQL datasource configuration, Cloud SQL Socket Factory support, and a production HMAC secret.
- Added fail-closed startup validation with secret-safe diagnostics.
- Replaced deployment plaintext secret configuration with Secret Manager references and a dedicated Cloud Run runtime service account.
- Added a neutral retry state when the backend cannot return monitored repositories, preventing a false `Fleet 0` display.

### Changed

- Updated the Cloud Run deployment specification and manual `frontend/src` to Google AI Studio transfer guidance.
- Added focused production datasource, HMAC, and Socket Factory regression tests.

### Affected files

- `.github/workflows/deploy-cloud-run.yml`
- `backend/pom.xml`
- `backend/src/main/java/com/scanpilot/config/ProductionDatasourceStartupValidator.java`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/test/java/com/scanpilot/config/ProductionDatasourceStartupValidatorTest.java`
- `docs/DEPLOYMENT-SPEC.md`
- `frontend/src/App.tsx`
- `CHANGELOG.md`

## 2026-08-22 — Efficient Local-Review Delivery Controls

**Status:** Working tree

### Changed

- Added a pre-BUILD target manifest to prevent stale-base, wrong-worktree, frozen-prototype, and non-deployed frontend artifact changes.
- Added a high-risk acceptance test matrix that distinguishes deterministic tests from live integration evidence.
- Added directed remediation cards so Codex returns exact correction/proof instructions rather than asking the implementation team to rediscover known defects.
- Formalized risk-based verification: narrow checks during implementation and one full applicable suite on the frozen handoff diff.

### Affected files

- `AGENTS.md`
- `docs/DELIVERY-WORKFLOW.md`
- `CHANGELOG.md`

## 2026-08-20 — Restore Build Integrity and Evidence-Backed Telemetry (Issue #51)

**Status:** Committed — 3b9c0d6

**Scope:** Restored build integrity and evidence-backed telemetry across the frontend. Removed all client-side fake progressive timer telemetry, fake file paths, fake counts, fallback numbers (375/352), static MTTR/AI success rates, and static 30-day trend arrays. Rendered neutral evidence states (`Awaiting scan`, `Not available`, `Scan request in progress — live progress is not available yet`) when backend data is missing or scan is in progress. Converted `Apply AI Fix` into copy/guidance-only presentation without client-side state mutation.

### Changed

- Updated `frontend/src/App.tsx`, `HealthGauge.tsx`, `MetricsGrid.tsx`, `TrendSparkline.tsx`, `ScanProgressStepper.tsx`, `LiveScanTerminal.tsx`, `RemediationDiff.tsx`, `CoverageAuditView.tsx`, `Navbar.tsx`, `FleetDashboard.tsx`, and `types/index.ts`.
- Repaired `HealthGauge` prop interface (`isScanned?: boolean`) and caller props.
- Removed client-side timer `setInterval` and `scanDurationSeconds` property.
- Handled `triggerRealScan()` failure by transitioning UI to error alert state without executing pipeline.
- Replaced absolute safety claims (`100% Safe`, `Grade A`) with bounded snapshot messaging (`No open findings in this completed scan`).

## 2026-08-20 — Review-Before-Commit and Technical Lead RCA Correction (Issue #60)

**Status:** Committed — 70f6769

**Scope:** Corrected the Issue #60 delivery workflow after Product Owner feedback. All Agent 1/2/3/4 and Codex review occurs on one frozen uncommitted local worktree diff. Product Owner acceptance is required before a local commit; push, pull-request creation, merge, Issue closure, and deployment remain separate authorizations. Added Codex Technical Lead root-cause analysis and prevention/re-dispatch responsibilities for workflow failures.

### Changed

- Updated `AGENTS.md`, `docs/DELIVERY-WORKFLOW.md`, and `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md` to prohibit autonomous pre-acceptance commit, push, and PR creation.
- Replaced pre-commit reviewed-head-SHA requirements with a frozen local-review-target contract: worktree path, base commit for context only, and changed-file list.
- Added the Codex RCA report template and the requirement to correct accepted delivery artifacts when the root cause lies in a workflow, contract, brief, or template.
- Classified the active governance-document version changes as MAJOR because the delivery contract and source-of-truth behavior changed; updated the changelog document version as a MINOR record addition.

### Affected files

- `AGENTS.md`
- `docs/DELIVERY-WORKFLOW.md`
- `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`
- `CHANGELOG.md`

## 2026-08-20 — Executable Multi-Agent Governance Gates & Coordinator Role Refinement (Issue #60)

**Status:** Committed — 4af79f4

**Scope:** Refined Scan Pilot's active `FULL_TRACKED` delivery contract and multi-agent governance model (Issue #60). Replaced five-tier sequence terminology with the Nested Coordination Model (Agent 4 coordinating Agent 1 Coder, Agent 2 QA, Agent 3 AppSec -> Codex Tech Lead -> Product Owner). Added explicit pre-BUILD assignment rules naming Agent 4 in the Issue contract, and naming Agent 1, Agent 2, and Agent 3 in Agent 4's execution plan, while recording Codex separately as Technical Lead.

### Added

- Added change-proportional QA and AppSec evaluation checklists (Frontend/UI, Backend/API, Auth/GitHub Integration, Database/Migration, CI/Workflow) in `docs/DELIVERY-WORKFLOW.md` and `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`.
- Added standard reporting templates for Codex Tech Lead Review (`APPROVED_FOR_PO_ACCEPTANCE`) in `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`.

### Changed

- Updated `AGENTS.md`, `docs/DELIVERY-WORKFLOW.md`, and `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md` to define the Nested Coordination Model (Mô hình Phối hợp Lồng nhau) and explicit pre-BUILD assignment naming rules (Agent 4 in Issue contract; Agent 1, 2, 3 in Agent 4 execution plan; Codex recorded separately as Technical Lead).
- Reconciled `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md` to enforce exact output states (`APPROVED`/`REQUEST_CHANGES` for QA, `APPROVED`/`BLOCKED` for AppSec, `READY_FOR_TECH_LEAD_REVIEW` for Delivery Gatekeeper, `APPROVED_FOR_PO_ACCEPTANCE` for Tech Lead), remediation loops, and local `.agent-work/` vs compact secret-safe PR summary boundaries.

### Fixed

- Removed stale delivery tables and "five-tier sequence" terminology that conflicted with the Nested Coordination Model.
- Explicitly prohibited Coder self-approval as QA/AppSec, clarified that Codex Tech Lead is separate from Agent 4, and affirmed that Delivery Gatekeeper and Tech Lead sign-offs do not replace Product Owner merge authority.

### Affected files

- `AGENTS.md`
- `docs/DELIVERY-WORKFLOW.md`
- `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`
- `CHANGELOG.md`

## 2026-08-20 — Implementation Baseline, Documentation Reconciliation, and Next-Feature Drafts

**Status:** Working tree

**Scope:** Reconciled current documentation with verified source behavior, local checks, live GitHub delivery state, and the public backend status endpoint. Added an explicit capability/gap baseline and created the reviewed GitHub work contracts for stabilization and subsequent product slices.

### Added

- Added `docs/IMPLEMENTATION-BASELINE.md` with `VERIFIED`, `PARTIAL`, `UI_ONLY`, `SPECIFIED`, and `BROKEN` capability states, evidence, limitations, and planning gates.
- Added local drafts `021` through `027` and created live Issues `#51` through `#57` for frontend truthfulness, asynchronous scan jobs, production GitHub fail-closed behavior, event triggers, Finding-to-Issue workflow, Repository Profile/Configuration Map, and Review Requests.
- Added `SP-CI-001` comparative research, an accepted gated stretch-rule direction, the proposed inspection contract and test matrix, plus Issue draft `028` and live Issue `#58` for the gated implementation work contract.

### Changed

- Replaced stale current-status content with the verified implementation phase, current blockers, live open Issues, and recommended execution order.
- Corrected the README, active SRS, Use Cases, documentation index, local development guide, repository contract, and benchmark metadata.
- Reconciled the local Issue index with closed Issues `#14`–`#24`, open Issues `#49`–`#57`, and the corresponding local issue drafts.
- Synchronized product, project context, current status, inspection specification, and research sources with the accepted `SP-CI-001` direction without claiming it is implemented.
- Refined Issue `#51` and its local source record to separate frontend truthfulness from asynchronous progress work owned by `#52`, with an explicit data-presentation contract and review-ready verification boundary.

### Fixed

- Removed documentation claims that CI/CD was both implemented and not implemented, or that Issue `#9` still awaited closeout.
- Stopped presenting simulated frontend telemetry, client-only resolution, complete Git history, and unimplemented workflows as verified application behavior.
- Corrected React/Spring versions, backend test count, active SRS classification, repository-relative draft links, and the benchmark metadata block.

### Affected files

- `README.md`
- `CHANGELOG.md`
- `.agents/repo-contract.yml`
- `.agents/outputs/drafts/github-issues/**`
- `docs/CURRENT-STATUS.md`
- `docs/IMPLEMENTATION-BASELINE.md`
- `docs/LOCAL-DEVELOPMENT-GUIDE.md`
- `docs/README.md`
- `docs/USE-CASES.md`
- `docs/requirements/SRS.md`
- `docs/research/benchmarks/BENCHMARK-RESULTS-SP-CONFIG-001.md`

## 2026-08-20 — Multi-Repository Fleet Hub and Live Scan Radar Presentation (PR #48)

**Status:** Committed — `c8ddbc5` (PR #48)

**Scope:** Added the multi-repository Fleet Overview, repository drill-down, local/PostgreSQL synchronization, Live Scan Radar presentation, deterministic severity scoring, skipped-file disclosures, and monitored-repository filtering. The terminal's intermediate events/counts and several dashboard metrics remain client-generated; `DEC-060` records the required policy rather than verified compliance.

### Added

- Added `frontend/src/components/FleetDashboard.tsx` implementing organization-level portfolio view with horizontal monitored repository rows, real-time status badges, health score gauge, and one-click deep posture navigation.
- Added `frontend/src/components/LiveScanTerminal.tsx` with terminal-style progress presentation, file target display, counters, copy behavior, and auto-scroll. At this checkpoint, intermediate events and counts are simulated in the client.
- Added `@GetMapping("/monitored")` endpoint in `ProjectController.java` and `getAllMonitoredProjects` in `ProjectService.java` for database-backed repository synchronization.
- Added `DEC-060` in `docs/DECISIONS.md` establishing zero mock telemetry and honest pipeline verification principles.

### Changed

- Updated `frontend/src/components/ScanProgressStepper.tsx` to honestly map the 4 real pipeline stages (`Workspace Setup`, `Working Tree Scan`, `Evidence & Sync`, `Checkpoint Verified`).
- Updated `frontend/src/components/CoverageAuditView.tsx` with dedicated "Excluded Artifacts & Eligibility Disclosure" table reporting skipped binaries and assets with exact reason codes (`UNSUPPORTED_BINARY_FILE`, `UNSUPPORTED_BINARY_DOCUMENT`).
- Updated `frontend/src/components/MetricsGrid.tsx` with exact mathematical code coverage percentage calculation and interactive Skipped Files audit link.
- Updated `frontend/src/components/RepoSelectModal.tsx` and `frontend/src/App.tsx` to strictly filter out already-monitored repositories and only display unmonitored repositories.

## 2026-08-20 — GitHub Dark Theme, Profile Popover & Uniform Bento Grid (PR #47)

**Status:** Committed — `573998a` (PR #47)

**Scope:** Migrated entire frontend to authentic GitHub Dark Theme (`#0d1117` canvas, `#161b22` cards, `#30363d` borders, `#238636` green accents), implemented Google-style account profile popover with live GitHub avatar, and restructured visual analytics bento banner to equal-height 3-column layout with 2x2 metric matrix.

### Added

- Added Google-style Profile Popover in `frontend/src/components/Navbar.tsx` displaying live GitHub avatar, account handle, status badge, GitHub profile link, and integrated sign-out action.
- Added `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md` formalizing the 4-agent peer review and gatekeeping delivery protocol.

### Changed

- Updated `frontend/src/index.css` and all component styles to authentic GitHub dark theme palette and custom dark scrollbars.
- Updated `frontend/src/components/MetricsGrid.tsx` to balanced 2x2 grid layout aligning height with `HealthGauge` and `TrendSparkline`.
- Updated `frontend/src/services/api.ts` with dynamic API base URL resolution and cross-origin OAuth redirection parameters.

## 2026-08-19 — Frontend Modular Architecture & Dual-Stage Stitch Design System (PR #46)

**Status:** Committed — `fef4daa` (PR #46)

**Scope:** Established official `frontend/` source layout (React 19 + TypeScript + Vite + Tailwind CSS v4) with modular component architecture, 3D perspective hero landing page, dark slate design system, dual-stage scan progression stepper (working tree & git history), interactive health score gauge, and side-by-side Gemini AI remediation diff viewer.

### Added

- Added `frontend/src/types/index.ts` defining strict TypeScript interfaces for repositories, findings, diff snippets, and health metrics.
- Added `frontend/src/services/api.ts` providing typed service abstractions with interactive mock data and backend endpoint connectors.
- Added `frontend/src/components/Navbar.tsx` featuring repository & branch dropdown and 2 main navigation tabs (`Findings & Remediation` vs `Coverage & Audit`).
- Added `frontend/src/components/HeroLanding.tsx` featuring ambient neon aura glow, 3D perspective tilt mockup card, and enterprise analysis engine bento grid.
- Added `frontend/src/components/HealthGauge.tsx` and `TrendSparkline.tsx` rendering visual health scores (`92/100 Safe - Grade A`) and 30-day leak reduction curves.
- Added `frontend/src/components/ScanProgressStepper.tsx` displaying 4-step dual-stage scan progression (Snapshot HEAD vs Git History tree).
- Added `frontend/src/components/CoverageAuditView.tsx` rendering deterministic audit trails and multi-stage coverage metrics.
- Added `frontend/src/components/FindingCard.tsx` and `RemediationDiff.tsx` providing zero-raw-secret masking and 1-click Gemini AI remediation diffs.
- Added `frontend/src/components/RepoSelectModal.tsx` providing repository search with `Ctrl+K` shortcut support.
## 2026-08-19 — Remote GitHub Snapshot Auto-Redirect & Default Branch Fallback (PR #45)

**Status:** Committed — `528ce1a` (PR #45)

**Scope:** Resolved remote GitHub archive download issue by replacing HttpURLConnection with Java 21 `HttpClient` configured with `Redirect.ALWAYS` (handling cross-domain 302 redirects from `api.github.com` to `codeload.github.com`), and added automatic fallback to repository default branch when branch-specific URL returns 404.

### Changed

- Updated `backend/src/main/java/com/scanpilot/scanner/pipeline/ScanPipelineService.java` using Java 21 `HttpClient` with auto-redirects and branch fallback to download complete repository snapshots.

## 2026-08-19 — Scan Pipeline PostgreSQL Persistence & Remote Snapshot Scanning (PR #44)

**Status:** Committed — `c84aff4` (PR #44)

**Scope:** Resolved scan execution 500 error by persisting user and repository entities to PostgreSQL on repository selection, added automated remote GitHub repository snapshot downloading/unpacking via pure Java ZipStream, configured default production frontend URL, and added root API welcome endpoint.

### Added

- Added `backend/src/main/java/com/scanpilot/system/RootController.java` serving welcome API metadata at `GET /` to prevent 404 Whitelabel errors.
- Added pure Java `fetchRemoteRepositorySnapshot` and `extractZipArchive` with zip-slip security protection in `ScanPipelineService.java`.

### Changed

- Updated `backend/src/main/java/com/scanpilot/project/service/ProjectService.java` synchronizing `UserEntity` and `RepositoryEntity` to PostgreSQL on repository selection to guarantee valid Foreign Keys in `scan_jobs`.
- Updated `backend/src/main/java/com/scanpilot/scanner/controller/ScanController.java` validating database repository existence before triggering scan pipeline.
- Updated `backend/src/main/resources/application-prod.yml` setting `frontend-url` default to `https://scan-pilot.ai.studio`.
- Updated `UserAndSessionPersistenceTest.java` adding `@BeforeEach` database cleanup for deterministic test isolation.

## 2026-08-19 — GitHub Deployments Production Environment Widget (PR #43)

**Status:** Committed — `56f0fdf` (PR #43)

**Scope:** Configured GitHub Actions CD workflow to register production deployment environment with live Cloud Run URL on repository sidebar.

### Changed

- Updated `.github/workflows/deploy-cloud-run.yml` declaring `environment.name: production` and `environment.url: https://scan-pilot-api-drbjfwrlxq-as.a.run.app`.

## 2026-08-19 — Production GitHub OAuth App Secrets & Dynamic Redirect (PR #42)

**Status:** Committed — `eb46e81` (PR #42)

**Scope:** Integrated production GitHub OAuth App credentials, enabled dynamic return redirect via `redirect_uri` / `Referer`, and configured cross-origin `SameSite=None; Secure` session cookies.

### Changed

- Updated `backend/src/main/java/com/scanpilot/auth/controller/AuthController.java` supporting dynamic `redirect_uri` parameter resolution.
- Updated `backend/src/main/java/com/scanpilot/auth/service/SessionService.java` setting `SameSite=None` when `cookieSecure` is enabled.
- Updated `frontend/src/api/authApi.ts` transmitting `window.location.origin` in login request.
- Updated `.github/workflows/deploy-cloud-run.yml` passing `GH_APP_CLIENT_ID` and `GH_APP_CLIENT_SECRET` secrets to Cloud Run deployment.

## 2026-08-19 — GitHub Actions CD Workflow for Cloud Run Deployment (Issue #40)

**Status:** Committed — `2278cb1`

**Scope:** Implemented automated Continuous Deployment (CD) pipeline via GitHub Actions to build Docker images and deploy Backend Spring Boot service to Google Cloud Run upon merges to `main`.

### Added

- Added `.github/workflows/deploy-cloud-run.yml` executing pre-deployment tests, Google Cloud authentication, Cloud Build image packaging, and automated Cloud Run deployment (`scan-pilot-api`).
- Configured Google Cloud Service Account `scan-pilot-deployer` with least-privilege IAM roles (`run.admin`, `artifactregistry.writer`, `cloudbuild.builds.editor`, `storage.admin`, `iam.serviceAccountUser`).
- Configured encrypted GitHub Repository Secrets `GCP_SA_KEY` and `GCP_PROJECT_ID`.

## 2026-08-19 — Frontend AI Studio Integration & Cloud Run Dual-Origin (Issue #37)

**Status:** Committed — `806bada`

**Scope:** Implemented dynamic `VITE_API_BASE_URL` resolution, TypeScript environment typings, and step-by-step Google AI Studio synchronization and deployment runbook under Issue `#37`.

### Added

- Updated `frontend/src/api/client.ts` and `authApi.ts` with dynamic `VITE_API_BASE_URL` prefixing and `getBaseUrl` accessor.
- Added `frontend/src/vite-env.d.ts` declaring typed Vite environment variables.
- Added `docs/AI-STUDIO-DEPLOYMENT-GUIDE.md` detailing step-by-step sync, environment variable configuration, and native Cloud Run publishing from Google AI Studio.

## 2026-08-19 — Backend Cloud Run Dockerfile & Multi-Origin CORS (Issue #36)

**Status:** Committed — `4725a3b`

**Scope:** Implemented multi-stage lean Dockerfile, non-root user execution, dynamic Cloud Run port binding, and multi-origin CORS configuration supporting Google AI Studio and Frontend Cloud Run origins under Issue `#36`.

### Added

- Added `backend/Dockerfile` using multi-stage build (`maven:3.9.9-eclipse-temurin-21-alpine` builder and `eclipse-temurin:21-jre-alpine` runner) with unprivileged user `scanpilot` (UID 10001) and container JVM flags.
- Added `backend/.dockerignore` filtering build artifacts, VCS, and sensitive local files.
- Added `backend/src/main/java/com/scanpilot/config/CorsProperties.java` and `CorsConfig.java` supporting dynamic multi-origin CORS and credentials.
- Added `backend/src/main/resources/application-prod.yml` configured for Cloud SQL PostgreSQL, HikariCP pool, and Cloud Run port binding.
- Added `backend/src/test/java/com/scanpilot/config/CorsConfigTest.java` verifying multi-origin CORS requests.

## 2026-08-19 — Decoupled Cloud Run Deployment Architecture Specification (DEC-056)

**Status:** Committed — `11f07fb`

**Scope:** Defined canonical decoupled deployment architecture (`DEC-056`) and deployment specification (`docs/DEPLOYMENT-SPEC.md`) for deploying Backend Spring Boot API to Google Cloud Run and Frontend React Dashboard via Google AI Studio native deployment.

### Added

- Added `docs/DEPLOYMENT-SPEC.md` defining multi-stage Dockerfile standards, Scale-to-Zero resource limits, environment variables, multi-origin CORS, and step-by-step Cloud Run deployment runbooks.
- Recorded `DEC-056` in `docs/DECISIONS.md`.
- Created executable GitHub Issues `#36` (Backend Cloud Run Deployment) and `#37` (Frontend AI Studio Dual-Origin Integration).

## 2026-08-19 — Security-Lab E2E Lifecycle Verification & Independent Secret Benchmark (Issue #24)

**Status:** Committed — `1a95f32`

**Scope:** Implemented full 4-stage Security-Lab E2E integration test suite and independent ground-truth synthetic secret detection benchmark suite (`SP-CONFIG-001`), publishing verification evidence under Issue `#24`.

### Added

- Added `backend/src/test/java/com/scanpilot/e2e/SecurityLabE2ELifecycleTest.java` verifying the complete 4-stage lifecycle (`OPEN/ACTION_REQUIRED` -> `RESOLVED/RISK_CONTAINED` -> `RESOLVED/VERIFIED_COMPLETE` -> `REGRESSED/ACTION_REQUIRED`) with synthetic Git histories, 100% workspace disposal, and zero secret leakage.
- Added `backend/src/test/java/com/scanpilot/benchmark/SafeSecretBenchmarkSuite.java` defining 60 synthetic ground-truth test cases across 12 rule families.
- Added `backend/src/test/java/com/scanpilot/benchmark/IndependentSecretBenchmarkTest.java` achieving 100% Precision, 100% Recall, 100% F1-Score, 100% Specificity, and 100% Accuracy on `SP-CONFIG-001`.
- Added `docs/research/benchmarks/BENCHMARK-RESULTS-SP-CONFIG-001.md` documenting formal benchmark results, metrics breakdown, and verification evidence.

## 2026-08-19 — React Dashboard Integration with Real Backend REST APIs (Issue #17)

**Status:** Committed — `fa96335`

**Scope:** Connected the React + TypeScript + Vite frontend with real Spring Boot backend REST APIs, replacing mock state with live GitHub OAuth authentication, repository selection, real-time scan job polling, finding details with Gemini AI remediation guides, before/after code diffs, finding lifecycle tracking, and coverage audit reporting under Issue `#17`.

### Added

- Added `frontend/src/types/api.ts` defining TypeScript interfaces matching all backend DTOs (`UserProfile`, `GitHubRepository`, `MonitoredProject`, `ScanJob`, `Finding`, `FindingLocation`, `EvidenceItem`, `AiExplanation`, `CoverageSummary`, `CoverageItem`, `ScanTriggerResponse`).
- Added `frontend/src/api/` package containing:
  - `client.ts`: Native `fetch` HTTP client with `credentials: 'include'` and status-aware error throwing.
  - `authApi.ts`: Endpoints for `/api/v1/auth/*` (`getMe`, `getLoginUrl`, `logout`).
  - `githubApi.ts`: Endpoints for `/api/v1/github/*` (`getAccessibleRepositories`, `getInstallUrl`).
  - `projectsApi.ts`: Endpoints for `/api/v1/projects/*` (`getCurrentProject`, `selectRepository`, `updateBranches`).
  - `scansApi.ts`: Endpoints for `/api/v1/scans/*` (`triggerScan`, `getScanJob`, `getFindings`, `getCoverage`).
  - `aiApi.ts`: Endpoints for `/api/v1/ai/*` (`explainFinding`, `getFindingExplanation`).
- Added `frontend/src/components/` package containing:
  - `Header.tsx`: Brand header with user profile avatar, active repository badge, view navigation, and sign-in/out controls.
  - `RepoSelectorModal.tsx`: Modal for browsing and selecting GitHub repositories and configuring branch slots.
  - `ScanProgressBar.tsx`: Real-time polling indicator for `PENDING` -> `RUNNING` -> `COMPLETED`/`FAILED` scan jobs.
  - `FindingCard.tsx`: Finding card with masked secret preview, redacted snippet, severity badges, lifecycle badges, and remediation quality badges.
  - `AiRemediationGuide.tsx`: Gemini AI guidance displaying plain-language summary, risk impact, evidence limits, interactive checklist, before/after diff preview, and copyable key revocation command.
  - `CoverageTab.tsx`: File classification audit tab with metric cards, segmented ratio bar, and filterable skipped files table.
  - `LoadingSkeleton.tsx`, `EmptyState.tsx`, `ErrorBanner.tsx`: UI state completeness components (WCAG AA compliant, `tabular-nums` for numeric stability).

### Changed

- Updated `frontend/src/App.tsx` with complete end-to-end state management, live data fetching, polling, and view routing.
- Updated `frontend/vite.config.ts` configuring API proxy (`/api` -> `http://localhost:8080`) on dev server port 3000.

## 2026-08-19 — Canonical SRS, Use Cases (UC-001 - UC-006) & Non-Functional Requirements (NFR-001 - NFR-010)

**Status:** Committed — `2be23d2`

**Scope:** Decomposed and standardized formal Use Cases (`UC-001` to `UC-006`), quantitative Non-Functional Requirements (`NFR-001` to `NFR-010`), and revitalized the canonical Software Requirements Specification (`docs/requirements/SRS.md` v2.0.0).

### Added

- Added `docs/USE-CASES.md` v1.0.0 defining detailed actor flows, preconditions, triggers, main success scenarios, alternate/error extensions, and postconditions for 6 core user interactions (`UC-001` through `UC-006`).
- Added `docs/NON-FUNCTIONAL-REQUIREMENTS.md` v1.0.0 defining exact quantitative metrics, verification methods, and acceptance thresholds across 10 non-functional categories (`NFR-001` through `NFR-010`).

### Changed

- Updated `docs/requirements/SRS.md` to `v2.0.0` (Active) unifying product introduction, architecture diagram, functional summary, use cases, and non-functional specifications.
- Updated `docs/README.md` to link all canonical requirement specifications.

## 2026-08-19 — Lean Code Crafting, Frontend Design Taste Skills & Agent Trigger Map

**Status:** Committed — `4972a4c`

**Scope:** Audited, integrated, and registered lean code crafting (Ponytail method) and beginner-friendly, anti-slop frontend UI/UX design standards into Scan Pilot canonical instructions and agent skills.

### Added

- Added `.agents/skill/ponytail/SKILL.md` enforcing the 7-rung priority ladder (YAGNI, codebase reuse, stdlib first, native platform features, installed dependencies, one-liner, minimum code) with non-negotiable security/validation boundaries.
- Added `.agents/skill/ponytail-review/SKILL.md` for reviewing pull requests and diffs specifically to eliminate over-engineering, single-implementation interfaces, and redundant dependencies.
- Added `.agents/skill/ponytail-debt/SKILL.md` for scanning and managing deliberate MVP shortcuts tagged with `// ponytail:` comment markers into a structured technical debt ledger.
- Added `.agents/docs/platform-native-cheatsheet.md` providing a comprehensive reference for Java 21, Spring Boot 3, PostgreSQL, HTML5, CSS3, and modern Web APIs native capabilities.
- Added `.agents/skill/design-taste-frontend/SKILL.md` establishing high-taste, beginner-friendly UI/UX directives ("Security for Humans", plain-language finding summaries, one-click before/after remediation diffs, secret masking, reassuring empty states, and WCAG AA contrast).
- Added `.agents/skill/full-output-enforcement/SKILL.md` preventing AI code truncation and banning lazy placeholders (`// TODO`, `// rest of code`).
- Added `.agents/skill/ui-design-audit/SKILL.md` providing a comprehensive UI/UX audit checklist for Frontend pull requests and views.

### Changed

- Updated `AGENTS.md` to `v2.4.0` adding a canonical `Installed Agent Skills & Trigger Map` table defining mandatory triggers and usage scopes for all installed skills.
- Standardized document metadata headers across all agent skills (`changelog-automatic`, `document-metadata-standardizer`, `repo-template-doc-sync-auditor`, `srs-to-github-issues`) and spike documents (`spikes/issue-006-ai-studio-cors/README.md`, `spikes/issue-007-github-oauth/README.md`).
- Updated `database/README.md` to `v1.0.0` (Active) reflecting the implemented PostgreSQL core schema (`V1__init_core_schema.sql`), JPA entities, and repositories.

## 2026-08-19 — Gemini AI Explanation and Remediation Guidance Service (Issue #16)

**Status:** Committed — `b8caeec`

**Scope:** Implemented Gemini AI Explanation and Remediation Guidance Service (`GeminiExplanationService`, `GeminiApiClient`) with structured JSON outputs, rule family fallback templates, fingerprint caching, and `AI_INFERENCE` evidence persistence under Issue `#16`.

### Added

- Added `com.scanpilot.ai.gemini` package containing:
  - Configuration: `GeminiConfigProperties` for `scanpilot.ai.gemini`.
  - Models: `GeminiExplanationRequest` and `GeminiExplanationResponse` records carrying plain-language summaries, risk impacts, evidence limits, remediation steps, remediation diffs, and revocation commands.
  - Services: `GeminiApiClient` (Spring 6 `RestClient` calling Google GenAI REST API with `response_mime_type: "application/json"`) and `GeminiExplanationService` (defensive zero-secret validation, `ConcurrentHashMap` TTL caching, deterministic fallback guidance, and `AI_INFERENCE` persistence).
  - Controller: `AiExplanationController` (`/api/v1/ai`) with endpoints `POST /findings/{findingId}/explain` and `GET /findings/{findingId}/explanation`.
- Added 16 comprehensive unit and integration tests in `GeminiExplanationServiceTest` and `AiExplanationControllerTest` (195/195 tests passing in total).

### Changed

- Updated `backend/src/main/resources/application.yml` with `scanpilot.ai.gemini` configuration properties.

## 2026-08-19 — Lean Code Crafting & Human-Centric Frontend Design Taste Skills

**Status:** Working tree

**Scope:** Audited and integrated lean development philosophy (Ponytail method) and beginner-friendly, anti-slop frontend UI/UX design standards into Scan Pilot agent skills without external scripts or supply-chain attack surface.

### Added

- Added `.agents/skill/ponytail/SKILL.md` enforcing the 7-rung priority ladder (YAGNI, codebase reuse, stdlib first, native platform features, installed dependencies, one-liner, minimum code) with non-negotiable security/validation boundaries.
- Added `.agents/skill/ponytail-review/SKILL.md` for reviewing pull requests and diffs specifically to eliminate over-engineering, single-implementation interfaces, and redundant dependencies.
- Added `.agents/skill/ponytail-debt/SKILL.md` for scanning and managing deliberate MVP shortcuts tagged with `// ponytail:` comment markers into a structured technical debt ledger.
- Added `.agents/docs/platform-native-cheatsheet.md` providing a comprehensive reference for Java 21, Spring Boot 3, PostgreSQL, HTML5, CSS3, and modern Web APIs native capabilities.
- Added `.agents/skill/design-taste-frontend/SKILL.md` establishing high-taste, beginner-friendly UI/UX directives ("Security for Humans", plain-language finding summaries, one-click before/after remediation diffs, secret masking, reassuring empty states, and WCAG AA contrast).
- Added `.agents/skill/full-output-enforcement/SKILL.md` preventing AI code truncation and banning lazy placeholders (`// TODO`, `// rest of code`).
- Added `.agents/skill/ui-design-audit/SKILL.md` providing a comprehensive UI/UX audit checklist for Frontend pull requests and views.

## 2026-08-18 — Snapshot and Git History Scan Pipeline with Finding Lifecycle (Issue #23)

**Status:** Committed — `8f8c331`

**Scope:** Implemented end-to-end Scan Pipeline (`ScanPipelineService`), Finding Lifecycle Engine (`FindingLifecycleEngine` for `OPEN/ACTION_REQUIRED` -> `RESOLVED/RISK_CONTAINED` -> `RESOLVED/VERIFIED_COMPLETE` -> `REGRESSED/ACTION_REQUIRED`), disposable workspace manager with guaranteed recursive cleanup (`GitWorkspaceManager`), and REST scan endpoints under Issue `#23`.

### Added

- Added `com.scanpilot.scanner.workspace` package containing `GitWorkspace` and `GitWorkspaceManager` with isolated directory creation and guaranteed recursive deletion.
- Added `com.scanpilot.scanner.lifecycle` package containing `FindingLifecycleEngine` mapping finding states and remediation qualities across sequential scans.
- Added `com.scanpilot.scanner.pipeline` package containing `ScanPipelineService` orchestrating Stage 1 snapshot scan, Stage 2 Git history scan, coverage recording, finding normalization, and checkpoint advancement.
- Added `com.scanpilot.scanner.controller` package containing `ScanController` (`/api/v1/scans`) for trigger scan, job status, findings list, and coverage reports.
- Added 18 unit and integration tests across lifecycle, workspace, pipeline, and controller test suites (179/179 tests passing in total).

## 2026-08-18 — PostgreSQL Core Persistence and Repositories (Issue #22)

**Status:** Committed — `5f6017a`

**Scope:** Implemented complete PostgreSQL database schema via Flyway (`V1__init_core_schema.sql`), JPA entities without `@Data`, Spring Data JPA repositories, indexes, constraints (`UNIQUE(repository_id, fingerprint)`), and integration tests under Issue `#22`.

### Added

- Added Flyway migration `backend/src/main/resources/db/migration/V1__init_core_schema.sql` initializing 12 core tables: `users`, `user_sessions`, `repositories`, `monitored_branches`, `scan_jobs`, `scan_checkpoints`, `findings`, `finding_locations`, `evidence_items`, `coverage_records`, `coverage_items`, and `review_requests`.
- Added `com.scanpilot.persistence` package containing:
  - Entities: `UserEntity`, `UserSessionEntity`, `RepositoryEntity`, `MonitoredBranchEntity`, `ScanJobEntity`, `ScanCheckpointEntity`, `FindingEntity`, `FindingLocationEntity`, `EvidenceItemEntity`, `CoverageRecordEntity`, `CoverageItemEntity`, `ReviewRequestEntity`.
  - Repositories: `UserRepository`, `UserSessionRepository`, `RepositoryProfileRepository`, `MonitoredBranchRepository`, `ScanJobRepository`, `ScanCheckpointRepository`, `FindingRepository`, `FindingLocationRepository`, `EvidenceItemRepository`, `CoverageRecordRepository`, `CoverageItemRepository`, `ReviewRequestRepository`.
- Added 20 comprehensive persistence tests in `backend/src/test/java/com/scanpilot/persistence/` (161/161 tests passing in total).

### Changed

- Updated `backend/pom.xml` with dependencies for Spring Data JPA, PostgreSQL driver, Flyway, and H2 test database.
- Updated `backend/src/main/resources/application.yml` with datasource and Flyway configuration.

## 2026-08-18 — Gitleaks Detector Adapter with Trusted SP-CONFIG-001 Policy (Issue #20)

**Status:** Committed — `8c0d34d`

**Scope:** Implemented Gitleaks detector adapter with pinned trusted `SP-CONFIG-001` policy (`sp-config-001-gitleaks.toml`), target repository anti-tamper safeguards (`FR-038`, `DEC-037`), secure temporary JSON report deletion, portable embedded regex engine, and full normalization pipeline with `RedactedEvidence` under Issue `#20`.

### Added

- Added `backend/src/main/resources/policies/sp-config-001-gitleaks.toml` with canonical trusted rules for Google API Keys, GitHub Tokens, AWS Keys, RSA Private Keys, and Generic API Keys.
- Added `com.scanpilot.scanner.detector.gitleaks` package containing:
  - Models: `GitleaksRawFinding`, `GitleaksScanRequest`, `GitleaksScanResult`, `DetectedSecretFinding`.
  - Services: `GitleaksDetectorAdapter` (CLI process builder, trusted policy injection, secure `try-finally` cleanup, embedded fallback scanner, and `scanAndNormalize`).
  - Configuration: `GitleaksConfigProperties` for `scanpilot.gitleaks`.
- Added 15 comprehensive unit and integration tests in `GitleaksDetectorAdapterTest` (141/141 tests passing in total).

### Changed

- Updated `backend/src/main/resources/application.yml` with `scanpilot.gitleaks` configuration properties.

## 2026-08-18 — SP_SECRET_FP_V1 HMAC-SHA-256 Fingerprinting and Redaction Engine (Issue #21)

**Status:** Committed — `b7d58a8`

**Scope:** Implemented repository-scoped HMAC-SHA-256 `SP_SECRET_FP_V1` secret fingerprinting with length prefixing (**REC-03**) and comprehensive secret redaction/masking engine under Issue `#21`.

### Added

- Added `com.scanpilot.security.secret` package containing:
  - Models: `SecretMatch` (transient raw match record within trusted boundary) and `RedactedEvidence` (safe, public immutable record).
  - Services: `SecretFingerprintService` (canonical `v1|repoId|ruleId|len:secret` format with HMAC-SHA-256) and `SecretRedactionService` (token masking for Google, GitHub, AWS, generic secrets, snippet/text sanitization, and `buildRedactedEvidence`).
  - Configuration: `SecurityConfigProperties` for `scanpilot.security.hmac-secret-key`.
- Added 25 unit tests in `SecretFingerprintServiceTest` and `SecretRedactionServiceTest` (126/126 tests passing in total).

### Changed

- Updated `backend/src/main/resources/application.yml` with `scanpilot.security` configuration properties.

## 2026-08-18 — Layered Content Classifier and File Eligibility Policy (Issue #15)

**Status:** Committed — `3bd1038`

**Scope:** Implemented layered content classifier, memory-safe byte sampling (8KB buffer), binary document recognition (PDF/Office per `FR-034`), two-tier size ceilings (10 MiB monitoring, 50 MiB release per `FR-037`), and structured Coverage Record generation under Issue `#15`.

### Added

- Added `com.scanpilot.scanner.classifier` package containing:
  - Enums: `ContentClassification`, `ScanMode`, `CoverageStatus`, `SkipReasonCode`, `CoverageImpact`.
  - DTOs & Models: `ClassificationResult`, `CoverageItem`, `CoverageSummary`.
  - Services: `ContentClassifierService` (magic bytes signature detection, byte sampling analysis, disguised file detection) and `FileEligibilityEngine` (size limit enforcement, binary document skipping, coverage summary aggregation).
- Added 39 comprehensive unit tests in `ContentClassifierServiceTest` and `FileEligibilityEngineTest` (101/101 tests passing in total).

## 2026-08-18 — GitHub App Linking and Repository Selection (Issue #14)

**Status:** Committed — `e71b0ec`

**Scope:** Implemented GitHub App installation linking, accessible personal repository querying, single repository onboarding with automatic PRIMARY branch derivation from GitHub default branch, and secondary branch slot management under Issue `#14`.

### Added

- Added `com.scanpilot.github` package for GitHub App RSA JWT authentication, installation token generation, install URL generation, and accessible repository fetching.
- Added `com.scanpilot.project` package for single repository onboarding (`DEC-046`), PRIMARY branch derivation (`FR-020`, `FR-022`), max 2 secondary branches management (`FR-020`, `FR-023`), and default branch sync (`FR-022`).
- Added `GlobalExceptionHandler` in `com.scanpilot.system` for structured REST API error handling.
- Added 31 unit and integration tests across GitHub App and Project services and controllers (59/59 tests passing in total).

### Changed

- Updated `UserSession` and `SessionService` to thread-safely retain `installationId`.
- Updated `backend/src/main/resources/application.yml` with `scanpilot.github` configuration properties.

## 2026-08-18 — GitHub OAuth Sign-In and Server-Side Session Management (Issue #19)

**Status:** Committed — `51e4508`

**Scope:** Implemented complete GitHub OAuth 2.0 authorization flow and server-side session management in Spring Boot backend under Issue `#19`.

### Added

- Added `com.scanpilot.auth` package containing models (`UserSession`), DTOs (`UserProfileDto`, `GitHubTokenResponse`, `GitHubUserDto`), services (`SessionService`, `GitHubOAuthService`), controller (`AuthController`), interceptor (`AuthInterceptor` with `@RequireAuth`), and resolver (`AuthenticatedUserArgumentResolver` with `@CurrentUser`).
- Added comprehensive unit and integration tests with 28/28 tests passing (`AuthControllerTest`, `SessionServiceTest`, `GitHubOAuthServiceTest`, `AuthInterceptorTest`).

### Changed

- Updated `backend/src/main/resources/application.yml` with `scanpilot.auth` configuration properties.

## 2026-08-18 — CI Delivery Automation and Submission MVP Issue Decomposition (Issue #18)

**Status:** Committed — `feab6fa`

**Scope:** Decomposed Submission MVP requirements into GitHub Issues #14 through #24, synchronized with GitHub Project #13, and implemented GitHub Actions CI workflow for frontend and backend under Issue `#18`.

### Added

- Added `.github/workflows/ci.yml` with dual parallel jobs: frontend lint/build (Node.js 20, npm cache) and backend verify (Java 21 Temurin, Maven cache) with least-privilege `permissions: { contents: read }`.
- Added numbered draft issues and updated `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md` with live issue links for Issues #14 through #24.

### Changed

- Updated `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md` status to Created.

## 2026-08-17 — Delivery Automation Staging Policy

**Status:** Committed — `299b257`

**Scope:** Recorded the accepted CI-first, CD-deferred delivery-automation policy and reconciled workflow/status documents after the merged production-foundation pull requests.

### Changed

- Updated the active `FULL_TRACKED` declaration in the delivery workflow after the passed Integration Check.
- Defined CI as the next separately authorized automation work item, with required `main` checks only after successful workflow evidence.
- Kept Cloud Run deployment and public release as explicit Product Owner-controlled work, not an automatic result of a green CI run.
- Updated current status to record PR `#12` merged into `main` while retaining Issue `#9` closure as a separate Product Owner action.

## 2026-08-17 — Production Foundation Established (Issue #9)

**Status:** Committed — `eb43426`

**Scope:** Established the production workspace layout for React/Vite frontend and Spring Boot 3 / Java 21 / Maven backend under Issue `#9`.

### Added

- Added `frontend/` containing React 19, TypeScript, Vite, and Tailwind CSS production workspace with the approved UI/UX prototype transferred cleanly.
- Added `backend/` containing Spring Boot 3.4.3, Java 21, and Maven modular monolith skeleton with `SystemStatusController` and unit/context tests.

### Changed

- Updated `.gitignore` with node/frontend build patterns.
- Updated `docs/CURRENT-STATUS.md` to reflect implementation foundation phase under review.

## 2026-08-17 — Agent Delivery Governance Integration Check Passed

**Status:** Committed — `494c167`

**Scope:** Verified that Antigravity branch and pull request handoffs are accessible to Codex, `.agent-work/` is Git-ignored, and activated `FULL_TRACKED` mode for implementation work under `DEC-055`.

### Changed

- Updated `AGENTS.md` and `docs/CURRENT-STATUS.md` to record Integration Check PASS and activate `FULL_TRACKED`.

## 2026-08-16 — Hybrid Agent Delivery Governance Installed

**Status:** Committed — `52011ad`

**Scope:** Installed the reusable `agent-delivery-governance` v1.0.0 skill and recorded Scan Pilot's accepted hybrid local/GitHub handoff contract under `DEC-055`.

### Added

- Added `.agents/skill/agent-delivery-governance/` with activation, brief, implementation-report, PR-handoff, technical-review, and Product Owner decision templates.
- Added `.agent-work/` to `.gitignore` for local coordination artifacts.

### Changed

- Updated agent instructions and delivery workflow to require a PR for Git-tracked implementation only after the `FULL_TRACKED` Integration Check passes.
- Recorded that the observed Antigravity export workspace is not a Git checkout, so strict activation remains pending rather than being claimed prematurely.

## 2026-08-16 — Conditional Implementation Start Accepted

**Status:** Committed — `52011ad`

**Scope:** Recorded Product Owner acceptance of the Eligibility Spike `CONDITIONAL GO` and transitioned the project from research/specification into Issue-driven implementation under `DEC-054`.

### Added

- Added `DEC-054`, preserving Completion Form verification, Cloud Billing alert, production authentication/private-source lifecycle, and Issue-delivery conditions.

### Changed

- Updated agent instructions, project context, architecture direction, status, and Eligibility Spike record to distinguish authorized implementation from unconditional production readiness.

## 2026-08-16 — Eligibility Spike Conditional-Go Recommendation

**Status:** Committed — `52011ad`

**Scope:** Consolidated Issues `#3` through `#7` into an evidence-backed `CONDITIONAL GO` recommendation for Issue `#8`; no implementation-phase change was made.

### Added

- Added the Eligibility Spike result, evidence summary, carried risks, explicit preconditions, owners, and verification limits.

### Changed

- Updated the submission context, current status, and documentation index to distinguish a technical eligibility recommendation from Product Owner authorization to begin product implementation.

## 2026-08-16 — Production GitHub OAuth and Session Verification

**Status:** Committed — `52011ad`

**Scope:** Recorded the successful Issue `#7` production-origin OAuth/session Eligibility Spike without starting the Spring Boot product implementation.

### Added

- Added the source and evidence record for a same-origin Cloud Run GitHub authorization-code flow with PKCE, a short-lived HttpOnly cookie, and server-side-only token exchange.
- Added defined production outcomes for logout, denied authorization, expired or revoked user authorization, and lost selected-repository installation access.

### Changed

- Recorded that the private GitHub App is scoped to the selected `scan-pilot` repository with read-only contents access and no webhooks.
- Recorded the narrow request-log exclusion that prevents OAuth callback query parameters from being retained by the temporary auth spike.
- Advanced the Eligibility Spike from browser-authentication verification to Product Owner review of Issue `#7` and final go/no-go Issue `#8`.

## 2026-08-16 — Submission Runtime Boundary Revision

**Status:** Committed — `52011ad`

**Scope:** Revised the accepted AI Riser submission topology after confirming the distinction between an AI Studio project link and a public deployed application.

### Changed

- Reclassified the Google AI Studio project as frozen submission evidence rather than the production authentication origin.
- Established GitHub-managed source deployed to Cloud Run as the real Scan Pilot frontend and backend runtime.
- Redirected the remaining browser-authentication spike toward the production origin; the completed AI Studio-to-Cloud-Run CORS result remains limited connectivity evidence.

## 2026-08-16 — AI Studio to Cloud Run CORS Verification

**Status:** Committed — `52011ad`

**Scope:** Completed and accepted the credential-free Cloud Run connectivity spike for Issue `#6` without starting the production backend or browser authentication.

### Added

- Added evidence for actual AI Studio browser success and third-party-origin CORS failure against a temporary Cloud Run endpoint.
- Added the isolated temporary spike source under `spikes/issue-006-ai-studio-cors/`.

### Changed

- Recorded use of the Product Owner's existing shared MVP Google Cloud project while preserving strict separation from IoT credentials and resources.
- Advanced the Eligibility Spike's next task to browser authentication and session handoff.

## 2026-08-16 — AI Studio Export and Frozen Evidence Verification

**Status:** Committed — `b2047f3`

**Scope:** Completed the Issue `#5` Eligibility Spike verification without starting implementation or copying sensitive Antigravity workspace state into the repository.

### Added

- Added a secret-safe export evidence record for the standard AI Studio ZIP, including its project identity, capture timestamp, and SHA-256 integrity digest.

### Changed

- Distinguished the standard ZIP snapshot from the sensitive Antigravity workspace transfer, which includes a configured secret and local workspace state.
- Recorded GitHub production source as the only post-handoff production source of truth and rejected continuous manual copy-paste synchronization.

## 2026-08-16 — AI Studio Signed-Out Access Verification

**Status:** Committed — `b2047f3`

**Scope:** Completed and accepted the user-run Incognito access test for Issue `#4` without changing AI Studio sharing settings or beginning implementation.

### Added

- Added a structured AI Studio access evidence record: Google authentication is required; the tested separate account could view Preview and Code, while prompt/project information and original creation conversation were not observed.

### Changed

- Updated the submission context and current status to distinguish authenticated link access from anonymous public access and to record the accepted `PASS` result.

## 2026-08-16 — AI Riser Live Submission Contract Verification

**Status:** Committed — `80fef25`

**Scope:** Completed and accepted Issue `#3` live-source research without beginning product implementation; corrected the official deadline and retained the same-day internal gate by explicit Product Owner decision.

### Added

- Added an evidence-backed submission-contract verification record with a `PASS` result and explicit Completion Form access limitation.

### Changed

- Corrected the factual official deadline to `2026-08-30 23:59 GMT+7` across research and current context.
- Marked the previous requirements-level safety-gate wording as unresolved rather than preserving the invalid August 31 contingency assumption.
- Distinguished required AI Studio, YouTube, and LinkedIn deliverables from the optional base-submission deployment link and the Cloud Run deployment bonus condition.
- Recorded that no separate source-code field is verified from the public page and that the exact emailed Completion Form remains unavailable.
- Revised `DEC-051` after Product Owner acceptance: the official and internal dates both remain August 30, with no separate contingency day.

## 2026-08-16 — Issue-Driven Delivery Governance

**Status:** Committed — `6d64327`

**Scope:** Established the professional GitHub Issue and Project workflow for a solo Product Owner, project manager, and developer without beginning product implementation.

### Added

- Added `docs/DELIVERY-WORKFLOW.md` as the canonical state, authorization, traceability, review, and acceptance contract.
- Added `.agents/skill/github-issue-delivery/` for executing an authorized Issue through planning, verification, and Product Owner review.
- Added retrospective traceability for Eligibility Spike Issues `#2` through `#8` in `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md`.
- Added `DEC-052` for Issue-driven delivery and `DEC-053` for the one-core-rule plus bounded-stretch policy.

### Changed

- Synchronized agent instructions, contribution guidance, repository contract, project context, current status, and documentation index with GitHub Project #13.
- Recorded the current Eligibility Spike schedule risk and made Issue `#4` the next executable task after Issue `#3` acceptance.
- Reconciled earlier changelog working-tree entries with their verified commits.

## 2026-08-15 — AI Riser Submission Architecture and Validation

**Status:** Committed — `80fef25` (`docs: define AI Riser submission architecture`)

**Scope:** Converted the accepted AI Riser submission workflow, focused vertical slice, GitHub onboarding, Gemini authority, independent validation, controlled demonstration, Product Owner decision boundary, and internal deadline into canonical documentation without starting implementation.

### Added

- Added `DEC-044` through `DEC-051` and `FR-045` through `FR-051` for the accepted submission contract.
- Added a source-attributed validation research note separating independent detector evidence, Scan Pilot integration verification, and the deployed security-lab journey.
- Added an Eligibility Spike gate for AI Studio public access, Cloud Run REST/CORS, authentication handoff, export fidelity, and final submission-source verification.

### Changed

- Defined a one-way AI Studio-to-production handoff and kept GitHub production source as the post-handoff source of truth.
- Narrowed the submission MVP to a real `SP-CONFIG-001` flow using a personal GitHub repository, Gitleaks, redacted evidence, Gemini explanation, re-scan, and remediation-quality transitions.
- Recorded 2026-08-30 as the internal complete-and-stable gate and 2026-08-31 at 23:59 as the user-reported external deadline pending live-form and timezone verification.
- Routed the next task to the submission Eligibility Spike before implementation or further Configuration Awareness expansion.

### Affected files

- `AGENTS.md`
- `CHANGELOG.md`
- `docs/PROJECT-CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/CURRENT-STATUS.md`
- `docs/PRODUCT.md`
- `docs/REQUIREMENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/RESEARCH-SOURCES.md`
- `docs/README.md`
- `docs/research/submission/AI-RISER-VIETNAM-2026.md`
- `docs/research/benchmarks/SECRET-DETECTION-VALIDATION.md`
- `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`

## 2026-08-14 — Configuration Awareness Direction

**Status:** Committed — `63849fc` (`docs: consolidate configuration awareness decisions`)

**Scope:** Completed the general Configuration Awareness checkpoint: multi-dimensional artifacts, deterministic classification and coverage separation, scenario-bounded environment and override semantics, change tracking and evidence invalidation, and action-first UX without starting implementation.

### Added

- Added `DEC-038` through `DEC-043` and `FR-039` through `FR-044` for the complete general Configuration Awareness contract.
- Expanded comparative research across OWASP, Trivy, GitHub, Spring Boot, Docker Compose, Git, Terraform, GitLab IaC, and Snyk IaC.

### Changed

- Updated product, architecture, lifecycle, A02 research, current checkpoint, and source registry.
- Moved the next decomposition task to selecting the first deep family among Spring Boot, GitHub Actions, and Docker.

### Affected files

- `CHANGELOG.md`
- `docs/DECISIONS.md`
- `docs/REQUIREMENTS.md`
- `docs/PRODUCT.md`
- `docs/ARCHITECTURE.md`
- `docs/SCAN-LIFECYCLE.md`
- `docs/CURRENT-STATUS.md`
- `docs/RESEARCH-SOURCES.md`
- `docs/README.md`
- `docs/research/security/A02-SECURITY-MISCONFIGURATION.md`
- `docs/research/benchmarks/CONFIGURATION-AWARENESS.md`
- `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`

## 2026-08-14 — Trusted Gitleaks Detector Policy

**Status:** Committed — `63849fc` (`docs: consolidate configuration awareness decisions`)

**Scope:** Accepted Scan Pilot ownership of the Gitleaks baseline policy so an untrusted repository cannot silently redefine rules or suppress findings.

### Added

- Added `DEC-037` and `FR-038` for trusted configuration, suppression isolation, version pinning, explicit redaction, and exact-byte size enforcement.
- Added a source-attributed Gitleaks adapter benchmark plan covering configuration attacks, command outcomes, coverage, resource boundaries, safe parsing, and cleanup.

### Changed

- Updated the inspection specification, current checkpoint, research registry, and documentation index to route the next work through the accepted adapter trust boundary.

### Affected files

- `CHANGELOG.md`
- `docs/DECISIONS.md`
- `docs/REQUIREMENTS.md`
- `docs/INSPECTION-SPEC.md`
- `docs/CURRENT-STATUS.md`
- `docs/RESEARCH-SOURCES.md`
- `docs/README.md`
- `docs/research/benchmarks/GITLEAKS-ADAPTER.md`
- `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`

## 2026-08-14 — Two-Tier Full-File Size Policy

**Status:** Committed — `63849fc` (`docs: consolidate configuration awareness decisions`)

**Scope:** Accepted `10 MiB` Continuous Monitoring and `50 MiB` release-oriented full-file limits for eligible text without introducing chunk checkpoints or starting implementation.

### Added

- Added `DEC-036` and `FR-037` with stable monitoring and release ceiling reason codes.
- Added the required Gitleaks benchmark matrix for `1`, `10`, `25`, `50`, and `100 MiB` files.

### Changed

- Updated product, inspection, lifecycle, status, and comparative-research documents to distinguish transparent monitoring skips from incomplete required release coverage.
- Preserved the broader Release Assessment MVP scope, triggers, build or artifact checks, and completion contract as open decisions.

### Affected files

- `CHANGELOG.md`
- `docs/DECISIONS.md`
- `docs/REQUIREMENTS.md`
- `docs/PRODUCT.md`
- `docs/SCAN-LIFECYCLE.md`
- `docs/INSPECTION-SPEC.md`
- `docs/CURRENT-STATUS.md`
- `docs/RESEARCH-SOURCES.md`
- `docs/research/benchmarks/SECRET-SCANNING-CONTENT-SCOPE.md`
- `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`

## 2026-08-14 — Maven Backend Build Direction

**Status:** Committed — `63849fc` (`docs: consolidate configuration awareness decisions`)

**Scope:** Accepted Apache Maven as the canonical build and dependency-management tool for the Java 21 and Spring Boot 3 backend without starting implementation.

### Added

- Added `DEC-035` and `FR-036` to establish one Maven-based Java backend build contract.

### Changed

- Updated the project, architecture, agent, and status documents to identify Maven as accepted while keeping exact Wrapper, module, plugin, dependency, profile, and CI details unresolved.

### Affected files

- `AGENTS.md`
- `README.md`
- `CHANGELOG.md`
- `docs/PROJECT-CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/REQUIREMENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/CURRENT-STATUS.md`
- `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`

## 2026-08-13 — Submission Context and Documentation Traceability

**Status:** Committed — `63849fc` (`docs: consolidate configuration awareness decisions`)

**Scope:** Recorded AI Riser Vietnam 2026 material, added comparative product-research and attribution rules, accepted content eligibility, deferred binary document extraction beyond the MVP, established the canonical two-month Google Cloud budget, and reworked this changelog into dated, traceable entries.

### Added

- Added the AI Riser Vietnam 2026 submission context, including the completion-form text supplied by the user, partner challenge deck, Participant Handbook, and Google AI & Vibe Coding Handbook reference. The latter remains pending detailed review.
- Added a source-attributed benchmark of GitHub Secret Scanning, GitLab Secret Detection, and Gitleaks for repository content scope.
- Added `DEC-029` and `FR-031`: all Git-tracked content in the selected scope is considered for eligibility, while every scan or skip outcome remains explicit in coverage.
- Added the document-extraction benchmark plan and recorded `DEC-030` and `FR-032`: Project Discovery uses an isolated extraction adapter, with Apache Tika as the first benchmark candidate rather than a selected production dependency.
- Added `DEC-031` and `FR-033`: Project Discovery inventories all in-scope content, targets DOCX and text-native PDF, marks scanned PDF as `NEEDS_OCR`, and keeps unsupported binary office documents inventory-only for the MVP.
- Added `docs/CLOUD-BUDGET.md` and `DEC-032`: USD 250 two-month planning envelope, USD 180 operating target, USD 70 reserve, Google Cloud promotional credit as the only recorded funding source, and explicit cost controls and alerts.
- Added `DEC-033` and `FR-034`: PDF and common Office binary documents remain inventoried but are not semantically extracted in the MVP, and `SP-CONFIG-001` skips their internal content with explicit coverage reason `UNSUPPORTED_BINARY_DOCUMENT`.
- Added `DEC-034` and `FR-035`: layered content classification produces `TEXT`, `BINARY`, or `UNDETERMINED`, while every skipped item remains a persistent structured coverage record rather than existing only in application logs.
- Established `.agents/` as the canonical repository directory for agent skills, contracts, and generated outputs.

### Changed

- Required review of submission-context documents before proposing changes to product direction, MVP scope, Google integration, deployment, demo, or submission evidence.
- Added the submission-context document to the documentation index.
- Required agents to benchmark relevant professional products and primary sources before material decisions, explain transferable and non-transferable lessons, and preserve the boundary between research and accepted requirements.
- Added a research attribution policy that prohibits copying proprietary material and requires license review before open-source implementation reuse.
- Replaced source-folder-only or silent-skip behavior with explicit `CONSIDERED`, `SCANNED`, and `SKIPPED` content outcomes.
- Added a parser-independent Project Discovery boundary, sensitive-output handling, and benchmark gate before accepting document formats or a production parser.
- Converted the document benchmark from an open format-selection question into a measurable accepted MVP target while preserving the production-parser decision gate.
- Required agents to review the canonical budget before cost-bearing design and to separate nominal promotional credit from verified eligible funding.
- Superseded the MVP portions of `DEC-030`, `DEC-031`, `FR-032`, and `FR-033`; stopped the Apache Tika benchmark for the current phase and retained it only as an optional Phase 2 research plan.
- Removed document-parser capacity from the current cloud budget while preserving one-worker and scale-to-zero cost controls.
- Replaced the previous aggregated `Unreleased` list with chronological entries backed by Git commits or the current working tree.
- Extended the content-scope benchmark with Git and Sourcegraph evidence and moved the next eligibility decision from general binary detection to oversized-file policy.
- Migrated the repository agent-support structure and all current internal references to the canonical `.agents/` directory.

### Affected files

- `AGENTS.md`
- `CHANGELOG.md`
- `docs/README.md`
- `docs/CLOUD-BUDGET.md`
- `docs/research/submission/AI-RISER-VIETNAM-2026.md`
- `docs/RESEARCH-SOURCES.md`
- `docs/research/benchmarks/SECRET-SCANNING-CONTENT-SCOPE.md`
- `docs/research/benchmarks/DOCUMENT-EXTRACTION-ADAPTER.md`
- `docs/DECISIONS.md`
- `docs/PROJECT-CONTEXT.md`
- `docs/REQUIREMENTS.md`
- `docs/PRODUCT.md`
- `docs/ARCHITECTURE.md`
- `docs/SCAN-LIFECYCLE.md`
- `docs/INSPECTION-SPEC.md`
- `docs/CURRENT-STATUS.md`
- `.agents/repo-contract.yml`
- `.agents/skill/**`
- `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`

## 2026-08-13 — Scan Pilot Research and Specification Baseline

**Status:** Committed — `456cda7` (`docs: establish Scan Pilot research specification baseline`)

**Scope:** Converted the generic repository template into the first Scan Pilot research and specification baseline.

### Added

- Added the canonical product, architecture, requirements, inspection, scan-lifecycle, evidence, finding-tracking, project-context, current-status, decision, and research-source documents.
- Added the A01 Broken Access Control and A02 Security Misconfiguration research checkpoints.
- Added a metadata-audit report for the documentation migration.

### Changed

- Replaced legacy VibeGuard naming with Scan Pilot and updated agent, contributor, repository, database, and documentation guidance for the research/specification phase.
- Deprecated template PRD and SRS documents in favor of the new canonical product and requirements documents.

### Security

- Recorded the initial security-model direction, including evidence provenance, finding tracking, secret redaction, isolated scanning, and `SP-CONFIG-001` as the first accepted secret-exposure rule.

### Affected files

- Canonical additions: `docs/ARCHITECTURE.md`, `docs/CURRENT-STATUS.md`, `docs/DECISIONS.md`, `docs/EVIDENCE-MODEL.md`, `docs/FINDING-TRACKING.md`, `docs/INSPECTION-SPEC.md`, `docs/PRODUCT.md`, `docs/PROJECT-CONTEXT.md`, `docs/REQUIREMENTS.md`, `docs/RESEARCH-SOURCES.md`, `docs/SCAN-LIFECYCLE.md`
- Research additions: `docs/research/security/A01-BROKEN-ACCESS-CONTROL.md`, `docs/research/security/A02-SECURITY-MISCONFIGURATION.md`
- Updated project guidance: `AGENTS.md`, `README.md`, `CONTRIBUTING.md`, `App/README.md`, `database/README.md`, `docs/README.md`, `docs/requirements/PRD.md`, `docs/requirements/SRS.md`
- Supporting updates: `.agents/repo-contract.yml`, `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`, `docs/diagrams/**/README.md`, `docs/reports/README.md`

## 2026-08-11 — Generic Repository Template Baseline

**Status:** Committed — `616f984` (`Initial commit`)

**Scope:** Initialized the repository as a generic Java web-application template with baseline project, database, GitHub, diagram, and agent-support documentation.

### Added

- Added the initial repository structure, contribution guidance, project README, application and database placeholders, GitHub issue and pull-request templates, and documentation templates.
- Added initial agent skills for changelog handling, document metadata, repository-template synchronization, and SRS-to-GitHub-Issue workflows.

### Affected files

- Repository and GitHub setup: `.gitignore`, `.github/**`, `LICENSE`, `README.md`, `CONTRIBUTING.md`
- Template project files: `App/README.md`, `database/**`, `docs/**`, `AGENTS.md`
- Agent configuration and skills: `.agents/**`
