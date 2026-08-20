> **Document:** Scan Pilot Implementation Baseline and Gap Register
> **File:** `docs/IMPLEMENTATION-BASELINE.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot Implementation Baseline and Gap Register

## Purpose

This document records what the checked-out application demonstrably implements. It does not replace accepted product decisions or requirements. It separates verified implementation from partial integration, presentation-only behavior, and future scope so planning does not treat an existing UI or database table as proof of an end-to-end capability.

Baseline reviewed on 2026-08-20:

- source commit: `9b368a6`, tree corresponding to merged PR `#48` on `origin/main` (`c8ddbc5`);
- backend verification: `mvn test` passed, 210 tests, 0 failures, 0 errors, 0 skipped;
- frontend verification: `npm run lint` failed at `frontend/src/App.tsx` because `HealthGauge` does not accept the supplied `isScanned` prop;
- frontend production build: not verified in the restricted local environment because Vite/esbuild process creation was denied after the lint failure was identified;
- public backend status endpoint: HTTP 200 with application status `HEALTHY`;
- GitHub state: Issues `#49` and `#50` are open; PR `#48` is merged.

## Status Definitions

| Status | Meaning |
|---|---|
| `VERIFIED` | Present in source and supported by a passing relevant check or current external observation. |
| `PARTIAL` | Some layers exist, but the documented end-to-end contract is incomplete or has an important fallback/limitation. |
| `UI_ONLY` | The interface presents or mutates local state without a matching durable backend operation. |
| `SPECIFIED` | Accepted or documented target behavior with no complete implementation evidence in this baseline. |
| `BROKEN` | Present in the current tree but fails a required build, type, or behavior check. |

## Capability Matrix

| Capability | Status | Current evidence | Important limit |
|---|---|---|---|
| Spring Boot REST API and health endpoint | `VERIFIED` | Controllers exist; backend test suite passes; public status endpoint returned HTTP 200. | Health proves service reachability, not every integration. |
| GitHub OAuth and server-side session endpoints | `PARTIAL` | Login, callback, current-user, logout, persistence tests, and HttpOnly cookie flow exist. | Production revocation, installation authorization, and private-source lifecycle were not revalidated in this audit. |
| GitHub repository discovery | `PARTIAL` | GitHub App token and OAuth-token paths exist. | GitHub API failure can return development repositories, violating the accepted zero-mock production policy. |
| Repository selection | `PARTIAL` | Selected repositories are persisted and exposed through `/projects/monitored`. | The service also keeps an in-memory current-project map; one-repository enforcement and restart behavior are not consistently database-owned. |
| Secondary branch configuration | `PARTIAL` | API validates at most two secondary branches. | Branch configuration is updated in memory and is not shown to persist `MonitoredBranchEntity` changes. Default-branch synchronization is not connected to a webhook flow. |
| Manual repository scan | `VERIFIED` | Controller invokes the real pipeline; tests cover pipeline behavior and remote snapshot acquisition. | The HTTP request executes the scan synchronously instead of dispatching an asynchronous job. |
| Remote repository acquisition | `PARTIAL` | GitHub zipball snapshot download and safe extraction are implemented. | Zipballs contain no `.git`; complete reachable-history scanning is unavailable. Issue `#49` tracks this gap. |
| Content classification and coverage persistence | `VERIFIED` | Classifier, eligibility engine, coverage entities/repositories, endpoints, and tests exist. | Real-production scale and every accepted edge policy were not benchmarked here. |
| `SP-CONFIG-001` detector, redaction, and fingerprinting | `VERIFIED` | Trusted policy, adapter/fallback, redaction, `SP_SECRET_FP_V1`, tests, and 60-case synthetic benchmark exist. | The local run used the embedded detector because a Gitleaks binary was unavailable; the synthetic suite does not prove broad real-world accuracy. |
| Finding lifecycle engine | `VERIFIED` at component/integration level | Tests cover open, resolved, risk-contained, verified-complete, and regression transitions. | Current remote zipball acquisition cannot prove complete Git-history coverage. |
| Gemini explanation service | `PARTIAL` | Redacted request service, persisted AI evidence, cache/fallback, endpoints, and tests exist. | The current test run used deterministic fallback after an invalid API key; live Gemini success was not verified. |
| Multi-repository fleet UI | `PARTIAL` | Fleet view loads monitored repositories from PostgreSQL and caches them locally. | Product decisions originally bounded submission onboarding to one repository; enforcement and current fleet behavior are inconsistent. Frontend lint is broken. |
| Finding and coverage UI | `PARTIAL` | Real REST reads populate findings and coverage. | Some snippets and remediation diffs are synthesized client-side rather than returned as evidence-backed backend data. |
| Scan progress and live terminal | `UI_ONLY` | Visual stepper and terminal components exist. | Timed file logs, counts `120/240/350`, and progress messages are simulated and do not come from backend telemetry. |
| Health score | `PARTIAL` | Severity-weighted score is computed deterministically from fetched findings. | Grade wording includes `100% Safe`; trend, MTTR, and AI success metrics use static values and are still marked as real data. |
| Apply remediation | `UI_ONLY` | The button changes finding state and score in React state. | It does not edit a repository, persist a transition, validate credential invalidation, or trigger a re-scan. |
| GitHub Issue creation from a finding | `SPECIFIED` | `FR-006` defines the direction. | No matching backend endpoint or complete frontend workflow was found. |
| Push, pull-request, merge, and scheduled scans | `SPECIFIED` | `FR-003` defines these triggers. | No webhook controller or scheduler was found. |
| Repository Profile / Project Discovery | `SPECIFIED` with schema foundation | Repository and related persistence foundations exist. | No complete discovery extractor and attributed Repository Profile workflow was found. |
| Review Requests | `SPECIFIED` with schema foundation | Review-request persistence entity/repository exists. | No create/respond lifecycle API or dashboard workflow was found. |
| Configuration Awareness | `SPECIFIED` | Accepted requirements and research exist. | No Configuration Map or family-specific analyzer is implemented. |
| CI | `VERIFIED` in repository configuration | `.github/workflows/ci.yml` checks frontend lint/build and backend verification. | The current frontend tree fails its local equivalent lint check. |
| Backend CD to Cloud Run | `VERIFIED` in workflow and public endpoint | Backend deployment workflow exists; public endpoint is healthy. | Frontend deployment and database/service cost behavior were not revalidated in this audit. |

## Documentation Corrections Required

The following statements must not be used as current implementation claims until their gaps are closed:

- `zero mock telemetry` — the policy is accepted, but the current frontend still simulates terminal events and metrics;
- `asynchronous scan worker` — the target architecture says asynchronous, while the controller currently calls the pipeline synchronously;
- `full Git history` for remote scans — current zipball acquisition is a working-tree snapshot only;
- `100% safe` — detector absence does not prove repository or application safety, especially with incomplete coverage;
- `all core user journeys complete` — GitHub Issue creation, event triggers, Review Requests, Project Discovery, and Configuration Awareness remain incomplete;
- `frontend checks pass` — the current tree has a TypeScript error.

## Planning Order

### Gate 0 — Restore truthful, buildable baseline

1. Fix the frontend type error and add a repeatable component/integration test floor.
2. Replace simulated telemetry and static metrics with backend-derived values or explicit `Not available` states.
3. Remove client-only finding resolution and rename the action to guidance-only until a verified re-scan transition exists.
4. Prevent GitHub API failures from silently returning demo repositories outside an explicit development profile.

### Gate 1 — Complete the core continuous scan contract

1. Refine and implement Issue `#49` for authenticated Git acquisition and history coverage.
2. Make scan dispatch asynchronous and let the frontend poll or subscribe to persisted job status.
3. Persist branch configuration and implement default-branch synchronization.
4. Add push, pull-request, merge, and scheduled triggers with authorization and deduplication.

Issue `#49` needs refinement before execution: `git clone --depth 50` is bounded history and cannot satisfy an acceptance criterion claiming all reachable commits. The Issue must choose either a bounded shallow-history contract with honest coverage or a full reachable-history clone with corresponding cost and timeout controls.

### Gate 2 — Finish product workflows already accepted

1. Create or draft a GitHub Issue from a Finding (`FR-006`).
2. Implement Repository Profile and Project Discovery (`FR-010`, `FR-039`–`FR-044`).
3. Implement asynchronous Review Requests (`FR-012`–`FR-014`).
4. Add Vietnamese localization through Issue `#50` only after the baseline UI is buildable and its strings are stable.

## Decomposition Boundary

The local issue-draft index under `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md` contains planning candidates derived from this gap register. They are proposals, not accepted scope and not live GitHub Issues. Creation, Project synchronization, assignment, and implementation each require their own authorization.

## Verification Limits

- No private repository was scanned during this audit.
- Live GitHub OAuth, GitHub App installation-token rotation/revocation, live Gemini response, frontend Cloud Run origin, Cloud SQL persistence, billing alerts, and webhook delivery were not exercised.
- Passing tests show behavior covered by those tests only.
- The public health response confirms backend availability at one point in time, not production readiness or continuous uptime.
