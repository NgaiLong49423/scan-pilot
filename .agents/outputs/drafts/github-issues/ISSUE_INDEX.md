> **Document:** Scan Pilot GitHub Issue Index
> **File:** `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md`
> **Version:** v3.2.1
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot GitHub Issue Index

## 2026-08-20 Reconciliation

This section supersedes older live-status and next-step statements later in the historical index. The older decomposition is retained for traceability.

- **Implementation evidence:** `docs/IMPLEMENTATION-BASELINE.md`
- **Live GitHub state checked:** 2026-08-20
- **Previously created implementation Issues:** `#14` through `#24` are closed.
- **Other completed delivery Issues:** `#9`, `#36`, `#37`, and `#40` are closed.
- **Open work:** `#49` Git history, `#50` localization, parent spike `#2`, Issues `#51` through `#58` below.
- **Mode for entries 021–027:** Created as live GitHub Issues on 2026-08-20. GitHub Project fields and milestone were deliberately not changed.

### Open Live Issues Requiring Review

| Issue | Scope | Planning note |
|---|---|---|
| `#49` | Authenticated Git acquisition and history traversal | Must resolve the contradiction between `--depth 50` and the acceptance criterion claiming all reachable commits. |
| `#50` | English/Vietnamese localization | Schedule after the frontend baseline is buildable and primary UI strings stabilize. |

Historical foundation draft: [`009-production-foundation-and-antigravity-handoff.md`](009-production-foundation-and-antigravity-handoff.md).

### Created Next-Feature Issues

| Draft | Title | Priority | Size | Points | Primary trace | Status |
|---|---|---|---|---:|---|---|
| [`021`](021-frontend-integrity-honest-telemetry.md) | Restore Build Integrity and Evidence-Backed Telemetry | Critical | M | 5 | `DEC-060`, `FR-004`, `FR-007`, `FR-028` | Created [#51](https://github.com/NgaiLong49423/scan-pilot/issues/51) |
| [`022`](022-async-scan-job-progress.md) | Dispatch Asynchronous Scan Jobs and Expose Real Progress | High | L | 8 | `FR-002`, `NFR-001`, `UC-003` | Created [#52](https://github.com/NgaiLong49423/scan-pilot/issues/52) |
| [`023`](023-production-github-fail-closed-persistence.md) | Fail Closed and Persist Repository Configuration | Critical | L | 8 | `FR-020`, `FR-047`, `DEC-054` | Created [#53](https://github.com/NgaiLong49423/scan-pilot/issues/53) |
| [`024`](024-event-driven-scan-triggers.md) | Add Authorized Event-Driven Scan Triggers | High | L | 8 | `FR-003`, `DEC-003`, `DEC-004` | Created [#54](https://github.com/NgaiLong49423/scan-pilot/issues/54) |
| [`025`](025-finding-github-issue-workflow.md) | Create a Secret-Safe GitHub Issue from a Finding | Medium | M | 5 | `FR-006`, `FR-014` | Created [#55](https://github.com/NgaiLong49423/scan-pilot/issues/55) |
| [`026`](026-repository-profile-configuration-map.md) | Build Repository Profile and Configuration Inventory | Medium | XL | 13 | `FR-010`, `FR-039`–`FR-044` | Created [#56](https://github.com/NgaiLong49423/scan-pilot/issues/56); PO family decision needed |
| [`027`](027-review-request-workflow.md) | Implement Asynchronous Review Requests | Medium | L | 8 | `FR-012`–`FR-014` | Created [#57](https://github.com/NgaiLong49423/scan-pilot/issues/57) |

Recommended order: `021` → refine/execute live `#49` → `022` and `023` → `024` → `025` → `026`/`027` → live `#50`.

### Gated Stretch-Rule Issue

| Draft | Title | Priority | Size | Points | Primary trace | Status |
|---|---|---|---|---:|---|---|
| [`028`](028-ci-mutable-actions-reference.md) | Detect Mutable Remote GitHub Actions References | Medium | M | 5 | `DEC-061`, `SP-CI-001`, OWASP A03:2025 | Created [#58](https://github.com/NgaiLong49423/scan-pilot/issues/58); blocked by #51, #49, #53 |

## Summary

- **Source documents:**
  - `AGENTS.md` (v2.3.0)
  - `docs/REQUIREMENTS.md` (v0.23.0 — FR-001 through FR-051, NFRs, Security Requirements)
  - `docs/DECISIONS.md` (v2.2.0 — DEC-001 through DEC-055)
  - `docs/CURRENT-STATUS.md` (v2.2.0)
  - `docs/ARCHITECTURE.md` (v0.17.0)
  - `docs/INSPECTION-SPEC.md` (v0.12.0)
  - `docs/CLOUD-BUDGET.md`
  - `.agents/skill/srs-to-github-issues/SKILL.md`
- **Draft output directory:** `.agents/outputs/drafts/github-issues/`
- **Mode:** Created (Live GitHub Issues created on repository `NgaiLong49423/scan-pilot`)
- **Real GitHub issues created:** Yes (Issues #14 to #24 created, linked to Project #13 and Milestone 1)
- **GitHub Project synced:** Yes
- **Target Project:** [Scan Pilot Project #13](https://github.com/users/NgaiLong49423/projects/13)
- **Milestone:** `AI Riser 2026 — Stable Submission`

## Source Hierarchy

1. `docs/DECISIONS.md`: Canonical accepted architecture, security, scope, and governance decisions.
2. `docs/REQUIREMENTS.md`: Functional Requirements (FR-001 to FR-051), Non-Functional Requirements, and Inspection & Security rules.
3. `AGENTS.md`: Repository instructions, agent delivery governance (`FULL_TRACKED`), and delivery automation policies.
4. `docs/INSPECTION-SPEC.md` & `docs/ARCHITECTURE.md`: Detailed inspection rule contracts (`SP-CONFIG-001`) and submission topology boundaries.
5. `docs/CURRENT-STATUS.md`: Phase status, completed milestones, and immediate execution priorities.

## Traceability Table (Requirements Matrix)

| Source ID | Source Scope / Requirement | Draft Issue(s) | Notes & Justification |
|---|---|---|---|
| **FR-001** | Connect & select GitHub repositories for monitoring | `011`, `012` | Split into Auth/Session (011) and App Linking / Repo Selection (012). |
| **FR-002** | Create and process scan job for selected repository | `017` | Orchestrated in async Scan Pipeline. |
| **FR-003** | Manual scan trigger & event-driven trigger readiness | `017`, `019` | Backend trigger in 017; Frontend UI button in 019. |
| **FR-004** | Normalized findings (rule, location, severity, evidence, remediation) | `015`, `017`, `019` | Fingerprint/Redaction in 015, Lifecycle in 017, Display in 019. |
| **FR-005** | Gemini contextual explanation and remediation analysis | `018` | Dedicated Gemini AI service with redacted prompt. |
| **FR-006** | Create GitHub Issue from finding | *Deferred* | Explicitly deferred beyond Submission MVP per `DEC-046`. |
| **FR-007** | Re-scan lifecycle: `OPEN`, `RESOLVED`, `REGRESSED` | `017` | Implemented in Finding Lifecycle Engine. |
| **FR-008** | Multi-project dashboard showing latest scan state | `019` | Implemented in React frontend UI integration. |
| **FR-009** | Implement `SP-CONFIG-001` against real repository content | `014` | Gitleaks detector adapter with pinned TOML policy. |
| **FR-010** | Project discovery and persistent Repository Profile | `012`, `016` | Profile schema in PostgreSQL (016) and repo metadata (012). |
| **FR-011** | Persist scan checkpoints and finding history | `016`, `017` | Checkpoint schema in 016; advancement logic in 017. |
| **FR-012**, **FR-013** | Non-blocking Review Requests with structured answers | `016`, `019` | Entity schema in 016; non-blocking UI display in 019. |
| **FR-014** | Persist typed, scoped, attributable Evidence Items | `016` | Implemented in PostgreSQL schema (`evidence_items`). |
| **FR-015** | Hybrid finding tracking (identity, history, diff, locations) | `015`, `016`, `017` | Fingerprint in 015, schema in 016, matching engine in 017. |
| **FR-016** | Immutable source snapshot in disposable mutable workspace | `017` | Temporary isolated workspace management in Scan Pipeline. |
| **FR-017** | `SP_SECRET_FP_V1` HMAC-SHA-256 fingerprinting without persisting secret | `015` | Dedicated cryptographic fingerprinting & redaction module. |
| **FR-018** | Finding lifecycle separated from remediation quality (`ACTION_REQUIRED`, `RISK_CONTAINED`, `VERIFIED_COMPLETE`) | `017` | Dual-dimension state tracking in Finding Lifecycle Engine. |
| **FR-019** | Re-scan resolution and history containment progression | `017`, `020` | Core logic in 017; verified end-to-end in 020. |
| **FR-020** | 1 `PRIMARY` branch (GitHub default) + up to 2 `SECONDARY` slots | `012` | Handled in GitHub App Linking & Repository Selection. |
| **FR-021** | Secondary branch configuration unlocked after baseline | `012`, `017` | Baseline completion gate before secondary slot activation. |
| **FR-022**, **FR-023** | GitHub default branch auto-sync and capacity retention | `012` | Automatic primary synchronization without deleting old evidence. |
| **FR-024** | Independent branch scanning | `017` | Branch-isolated scan job execution. |
| **FR-025** | Immutable HEAD snapshot first, then reachable history baseline | `017` | Two-phase scan strategy in Scan Pipeline. |
| **FR-026** | Gitleaks detector behind Scan Pilot adapter | `014` | Gitleaks Adapter with isolated raw report cleanup. |
| **FR-027** | Incremental scan from ancestor checkpoint; re-baseline on rewrite | `017` | Graph-aware checkpoint ancestry validation. |
| **FR-028**, **FR-029** | Validated coverage records required before advancing checkpoints | `013`, `017` | Coverage validation engine & checkpoint persistence. |
| **FR-030** | No automated live credential verification against providers | `014`, `017` | Strict policy constraint respected across all scanner modules. |
| **FR-031** | Consider all Git-tracked items; scan eligible; record structured skips | `013` | Layered classifier & skip record generation. |
| **FR-032**, **FR-033** | Binary document semantic extraction adapter | *Deferred* | Deferred to optional Phase 2 per `DEC-033` / `DEC-034`. |
| **FR-034** | Binary document inventory (PDF/Office) skipped with `UNSUPPORTED_BINARY_DOCUMENT` | `013` | Implemented in Content Classifier & File Eligibility. |
| **FR-035** | Layered classification (`TEXT`, `BINARY`, `UNDETERMINED`); persistent coverage records | `013` | Magic bytes, sampling, extension hints in 013. |
| **FR-036** | Java 21 / Spring Boot 3 uses Apache Maven | `010` | Enforced in CI pipeline build scripts. |
| **FR-037** | 10 MiB monitoring limit, 50 MiB release ceiling | `013` | Two-tier file size policy in File Eligibility. |
| **FR-038** | Pinned trusted Gitleaks policy; repository configs cannot override | `014` | Explicit `--config` flag and bypass of local `.gitleaksignore`. |
| **FR-039**–**FR-044** | Configuration Awareness foundation & UX separation | `013`, `019` | Classification in 013; UX attention/coverage/change separation in 019. |
| **FR-045** | AI Studio frozen evidence; Cloud Run hosts real production product | `010`, `019` | Architecture boundary respected across CI and Frontend. |
| **FR-046** | No judge-only anonymous bypass; standard GitHub onboarding | `011`, `012` | Unified user authentication & repo selection flow. |
| **FR-047** | Sign in before linking GitHub App; personal accounts supported | `011`, `012` | Enforced onboarding sequence. |
| **FR-048** | Gemini bounded secret-redacted explanation; no repo mutation | `018` | Structured JSON prompt with zero raw secret exposure. |
| **FR-049** | Independent safe secret-detection benchmark evidence | `020` | Safe benchmark test suite execution and metrics report. |
| **FR-050** | Controlled Security-Lab repository with synthetic secrets & Git history | `020` | Repeatable E2E verification test target. |
| **FR-051** | Demonstrate 3-stage lifecycle (`ACTION_REQUIRED` -> `RISK_CONTAINED` -> `VERIFIED_COMPLETE`) | `017`, `020` | Orchestration in 017; verification execution in 020. |
| **Automation** | GitHub Actions CI workflow for PRs and `main` | `010` | CI automation for frontend and backend. |

---

## Submission MVP Work Items (Drafts 010 – 020)

| No | Draft File | Title | Type | Size | Story Points | Priority | Source Trace | Dependencies | Relationships | Labels | Suggested Branch | Status | GitHub Issue |
|---|---|---|---|---|---:|---|---|---|---|---|---|---|---|
| **010** | [`010-ci-delivery-automation.md`](010-ci-delivery-automation.md) | `[CI][Automation] Implement Continuous Integration Workflow for Frontend and Backend` | Task | S | 3 | High | `AGENTS.md`, `DEC-055`, `FR-036` | Issue #9 (merged) | Blocking: 011–020 | `📋 Task`, `🛠️ Backend`, `🎨 Frontend`, `🔴 priority-high` | `codex/18-ci-delivery-automation` | Created | [#18](https://github.com/NgaiLong49423/scan-pilot/issues/18) |
| **011** | [`011-github-auth-session.md`](011-github-auth-session.md) | `[Auth][FR-001][FR-047] Implement GitHub OAuth Sign-In and Server-Side Session Management` | Feature | M | 5 | High | `FR-001`, `FR-046`, `FR-047`, `DEC-047`, `DEC-054` | 010 | Blocked by: #18; Blocking: #14, #17 | `🚀 Feature`, `🔒 Security`, `🛠️ Backend`, `🔴 priority-high` | `codex/19-github-auth-session` | Created | [#19](https://github.com/NgaiLong49423/scan-pilot/issues/19) |
| **012** | [`012-github-app-linking-repo-selection.md`](012-github-app-linking-repo-selection.md) | `[GitHub][FR-001][FR-020][FR-047] Support GitHub App Installation Linking and Repository Selection` | Feature | M | 5 | High | `FR-001`, `FR-020`, `FR-022`, `FR-023`, `FR-047`, `DEC-047` | 011 | Blocked by: #19; Blocking: #22, #23, #17 | `🚀 Feature`, `🛠️ Backend`, `🔴 priority-high` | `codex/14-github-app-linking-repo-selection` | Created | [#14](https://github.com/NgaiLong49423/scan-pilot/issues/14) |
| **013** | [`013-content-classifier-file-eligibility.md`](013-content-classifier-file-eligibility.md) | `[Scanner][FR-031][FR-035][FR-037] Implement Layered Content Classifier and File Eligibility Policy` | Feature | M | 5 | High | `FR-031`, `FR-034`, `FR-035`, `FR-037`, `DEC-035`, `DEC-036` | Issue #9 | Blocked by: None; Blocking: #20, #23 | `🚀 Feature`, `🛠️ Backend`, `🔴 priority-high` | `codex/15-content-classifier-file-eligibility` | Created | [#15](https://github.com/NgaiLong49423/scan-pilot/issues/15) |
| **014** | [`014-gitleaks-detector-adapter.md`](014-gitleaks-detector-adapter.md) | `[Detector][FR-009][FR-026][FR-038] Implement Gitleaks Detector Adapter with Trusted SP-CONFIG-001 Policy` | Feature | M | 5 | High | `FR-009`, `FR-026`, `FR-038`, `DEC-037`, `DEC-053` | 013 | Blocked by: #15; Blocking: #21, #23 | `🚀 Feature`, `🔒 Security`, `🛠️ Backend`, `🔴 priority-high` | `codex/20-gitleaks-detector-adapter` | Created | [#20](https://github.com/NgaiLong49423/scan-pilot/issues/20) |
| **015** | [`015-secret-fingerprinting-redaction.md`](015-secret-fingerprinting-redaction.md) | `[Security][FR-017] Implement SP_SECRET_FP_V1 HMAC-SHA-256 Fingerprinting and Redaction Engine` | Feature | S | 3 | High | `FR-017`, `FR-004`, `DEC-038`, `DEC-048` | Issue #9 | Blocked by: None; Blocking: #22, #23, #16 | `🚀 Feature`, `🔒 Security`, `🛠️ Backend`, `🔴 priority-high` | `codex/21-secret-fingerprinting-redaction` | Created | [#21](https://github.com/NgaiLong49423/scan-pilot/issues/21) |
| **016** | [`016-postgresql-core-persistence.md`](016-postgresql-core-persistence.md) | `[Database][FR-010][FR-011][FR-014] Implement PostgreSQL Schema and Repositories for Scan Pilot Core Entities` | Feature | M | 5 | High | `FR-010`, `FR-011`, `FR-014`, `FR-015`, `DEC-006`, `DEC-039` | 011, 012, 015 | Blocked by: #19, #14, #21; Blocking: #23, #17 | `🚀 Feature`, `🗄️ Database`, `🛠️ Backend`, `🔴 priority-high` | `codex/22-postgresql-core-persistence` | Created | [#22](https://github.com/NgaiLong49423/scan-pilot/issues/22) |
| **017** | [`017-scan-pipeline-finding-lifecycle.md`](017-scan-pipeline-finding-lifecycle.md) | `[Scan][FR-002][FR-007][FR-018][FR-025][FR-051] Implement Snapshot and Git History Scan Pipeline with Finding Lifecycle` | Feature | L | 8 | Critical | `FR-002`, `FR-007`, `FR-018`, `FR-019`, `FR-025`, `FR-051`, `DEC-040` | 012, 013, 014, 015, 016 | Blocked by: #14, #15, #20, #21, #22; Blocking: #16, #17, #24 | `🚀 Feature`, `🛠️ Backend`, `🚨 Critical` | `codex/23-scan-pipeline-finding-lifecycle` | Created | [#23](https://github.com/NgaiLong49423/scan-pilot/issues/23) |
| **018** | [`018-gemini-ai-explanation-service.md`](018-gemini-ai-explanation-service.md) | `[AI][FR-005][FR-048] Implement Gemini Explanation and Remediation Guidance Service` | Feature | M | 5 | High | `FR-005`, `FR-048`, `DEC-007`, `DEC-048` | 015, 017 | Blocked by: #21, #23; Blocking: #17, #24 | `🚀 Feature`, `🛠️ Backend`, `🔴 priority-high` | `codex/16-gemini-ai-explanation-service` | Created | [#16](https://github.com/NgaiLong49423/scan-pilot/issues/16) |
| **019** | [`019-frontend-real-api-integration.md`](019-frontend-real-api-integration.md) | `[Frontend][FR-004][FR-008][FR-044] Connect React Dashboard to Real Scan Pilot Backend REST APIs` | Feature | M | 5 | High | `FR-004`, `FR-008`, `FR-044`, `DEC-002`, `DEC-005`, `DEC-043` | 011, 012, 017, 018 | Blocked by: #19, #14, #23, #16; Blocking: #24 | `🚀 Feature`, `🎨 Frontend`, `🔴 priority-high` | `codex/17-frontend-real-api-integration` | Created | [#17](https://github.com/NgaiLong49423/scan-pilot/issues/17) |
| **020** | [`020-security-lab-e2e-benchmark.md`](020-security-lab-e2e-benchmark.md) | `[Verification][FR-049][FR-050][FR-051] Execute Security-Lab E2E Lifecycle Verification and Independent Secret Benchmark` | Testing | M | 5 | Critical | `FR-049`, `FR-050`, `FR-051`, `DEC-049`, `DEC-050` | 017, 018, 019 | Blocked by: #23, #16, #17; Blocking: Final Submission | `🧪 Testing`, `🔒 Security`, `🚨 Critical` | `codex/24-security-lab-e2e-benchmark` | Created | [#24](https://github.com/NgaiLong49423/scan-pilot/issues/24) |

**Total Estimated Story Points for Submission MVP:** 49 Story Points (11 work items).

---

## Historical & Baseline Issues (#2 – #9)

| Issue | Relationship | Source Trace | Points | Priority | Status | Description / Outcome |
|---|---|---|---:|---|---|---|
| [#2](https://github.com/NgaiLong49423/scan-pilot/issues/2) | Parent Epic | `DEC-044`–`DEC-051` | — | Critical | Open / Evidence complete | Validate AI Riser submission architecture; closure remains a Product Owner action. |
| [#3](https://github.com/NgaiLong49423/scan-pilot/issues/3) | Child of #2 | `DEC-051` | 1 | Critical | Closed / Completed | Confirm submission requirements and deadline (PASS: 2026-08-30) |
| [#4](https://github.com/NgaiLong49423/scan-pilot/issues/4) | Child of #2 | `DEC-045` | 2 | Critical | Closed / Completed | Verify signed-out AI Studio access (PASS) |
| [#5](https://github.com/NgaiLong49423/scan-pilot/issues/5) | Child of #2 | `DEC-044` | 2 | High | Closed / Completed | Verify export and frozen evidence (PASS) |
| [#6](https://github.com/NgaiLong49423/scan-pilot/issues/6) | Child of #2 | `DEC-045` | 3 | Critical | Closed / Completed | Prove AI Studio to Cloud Run call (PASS) |
| [#7](https://github.com/NgaiLong49423/scan-pilot/issues/7) | Child of #2 | `DEC-047`, `FR-046` | 5 | Critical | Closed / Completed | Verify GitHub authentication handoff (PASS) |
| [#8](https://github.com/NgaiLong49423/scan-pilot/issues/8) | Child of #2 | `DEC-054` | 2 | Critical | Closed / Completed | Record go/no-go result (CONDITIONAL GO accepted) |
| [#9](https://github.com/NgaiLong49423/scan-pilot/issues/9) | Baseline Foundation | `DEC-054`, `DEC-055`, `FR-036` | 5 | High | In Review / Merged PR #12 | Establish production foundation and Antigravity handoff |

---

## Grouping & Splitting Decisions

| Requirement Source | Decision | Technical & Delivery Rationale |
|---|---|---|
| `FR-001`, `FR-047` | Split into `011` (Auth/Session) and `012` (GitHub App Linking / Repo Selection) | User identity authentication and GitHub App installation authorization represent distinct security scopes and lifecycle boundaries. |
| `FR-031`, `FR-034`, `FR-035`, `FR-037` | Grouped into `013` (Content Classifier & File Eligibility) | Content classification, size limits, binary document skip policy, and coverage record generation are tightly coupled in the pre-scan stage. |
| `FR-009`, `FR-026`, `FR-038` | Grouped into `014` (Gitleaks Detector Adapter) | Gitleaks execution, custom TOML policy pinning, and raw report sanitization form a cohesive detector adapter unit. |
| `FR-017` | Separated into `015` (Secret Fingerprinting & Redaction) | Zero-secret exposure and cryptographic HMAC-SHA-256 fingerprinting require an isolated trusted module usable across database, logs, and AI prompts. |
| `FR-010`, `FR-011`, `FR-014`, `FR-015` | Grouped into `016` (PostgreSQL Core Persistence) | Centralized schema migration (Flyway) and JPA repository definitions maintain referential integrity across entities. |
| `FR-002`, `FR-007`, `FR-018`, `FR-025`, `FR-051` | Formulated as `017` (Scan Pipeline & Finding Lifecycle) | Combines workspace isolation, two-stage scan execution, and Finding Lifecycle state transitions into one testable vertical slice (Size L, 8 pts). |
| `FR-005`, `FR-048` | Formulated as `018` (Gemini AI Explanation Service) | Keeps AI reasoning strictly separated from deterministic scanning truth, with structured JSON schema outputs and fallback handling. |
| `FR-004`, `FR-008`, `FR-044` | Grouped into `019` (Frontend Real API Integration) | Replaces mock prototype data with live REST API communication across all core views without altering approved UI/UX. |
| `FR-049`, `FR-050`, `FR-051` | Grouped into `020` (Security-Lab E2E Verification & Benchmark) | Combines end-to-end multi-commit lifecycle testing on a synthetic target with independent secret detection benchmark evidence. |
| `FR-006` | Deferred beyond Submission MVP | GitHub Issue creation from findings is a secondary workflow deferred under `DEC-046` to keep submission focus tight. |
| `FR-032`, `FR-033` | Deferred beyond Submission MVP | Binary document semantic extraction (Apache Tika) is deferred under `DEC-033`/`DEC-034`; inventory-only model adopted. |

---

## Review Notes

1. **Historical note:** Work items 010 through 020 were used to create live Issues `#18`–`#24`, `#14`–`#17`, and related delivery work; those Issues are now closed. Treat their older draft-state wording as historical only.
2. **Next Product Owner review:** approve, reject, regroup, or reprioritize drafts 021 through 027. Authorization to review drafts is not authorization to create Issues, change Project #13, implement code, commit, or push.
