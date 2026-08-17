> **Document:** Scan Pilot Changelog
> **File:** `CHANGELOG.md`
> **Version:** v2.25.0
> **Created:** 2026-08-11
> **Last Updated:** 2026-08-16
> **Status:** Active

# Scan Pilot Changelog

This file records notable Scan Pilot changes as a chronological, human-readable history. Git remains the exact file-level source of truth.

Each entry states whether it is already committed or still in the working tree. A working-tree entry is replaced with its commit hash when the coherent checkpoint is committed; it is not copied into a second entry. File paths in older entries may be normalized to a later canonical directory after an explicit structural migration; Git history remains the exact source for the path used by each historical commit.

## 2026-08-17 — Production Foundation Established (Issue #9)

**Status:** Working tree — not committed

**Scope:** Established the production workspace layout for React/Vite frontend and Spring Boot 3 / Java 21 / Maven backend under Issue `#9`.

### Added

- Added `frontend/` containing React 19, TypeScript, Vite, and Tailwind CSS production workspace with the approved UI/UX prototype transferred cleanly.
- Added `backend/` containing Spring Boot 3.4.3, Java 21, and Maven modular monolith skeleton with `SystemStatusController` and unit/context tests.

### Changed

- Updated `.gitignore` with node/frontend build patterns.
- Updated `docs/CURRENT-STATUS.md` to reflect implementation foundation phase.

## 2026-08-16 — Hybrid Agent Delivery Governance Installed

**Status:** Working tree — not committed

**Scope:** Installed the reusable `agent-delivery-governance` v1.0.0 skill and recorded Scan Pilot's accepted hybrid local/GitHub handoff contract under `DEC-055`.

### Added

- Added `.agents/skill/agent-delivery-governance/` with activation, brief, implementation-report, PR-handoff, technical-review, and Product Owner decision templates.
- Added `.agent-work/` to `.gitignore` for local coordination artifacts.

### Changed

- Updated agent instructions and delivery workflow to require a PR for Git-tracked implementation only after the `FULL_TRACKED` Integration Check passes.
- Recorded that the observed Antigravity export workspace is not a Git checkout, so strict activation remains pending rather than being claimed prematurely.

## 2026-08-16 — Conditional Implementation Start Accepted

**Status:** Working tree — not committed

**Scope:** Recorded Product Owner acceptance of the Eligibility Spike `CONDITIONAL GO` and transitioned the project from research/specification into Issue-driven implementation under `DEC-054`.

### Added

- Added `DEC-054`, preserving Completion Form verification, Cloud Billing alert, production authentication/private-source lifecycle, and Issue-delivery conditions.

### Changed

- Updated agent instructions, project context, architecture direction, status, and Eligibility Spike record to distinguish authorized implementation from unconditional production readiness.

## 2026-08-16 — Eligibility Spike Conditional-Go Recommendation

**Status:** Working tree — not committed

**Scope:** Consolidated Issues `#3` through `#7` into an evidence-backed `CONDITIONAL GO` recommendation for Issue `#8`; no implementation-phase change was made.

### Added

- Added the Eligibility Spike result, evidence summary, carried risks, explicit preconditions, owners, and verification limits.

### Changed

- Updated the submission context, current status, and documentation index to distinguish a technical eligibility recommendation from Product Owner authorization to begin product implementation.

## 2026-08-16 — Production GitHub OAuth and Session Verification

**Status:** Working tree — not committed

**Scope:** Recorded the successful Issue `#7` production-origin OAuth/session Eligibility Spike without starting the Spring Boot product implementation.

### Added

- Added the source and evidence record for a same-origin Cloud Run GitHub authorization-code flow with PKCE, a short-lived HttpOnly cookie, and server-side-only token exchange.
- Added defined production outcomes for logout, denied authorization, expired or revoked user authorization, and lost selected-repository installation access.

### Changed

- Recorded that the private GitHub App is scoped to the selected `scan-pilot` repository with read-only contents access and no webhooks.
- Recorded the narrow request-log exclusion that prevents OAuth callback query parameters from being retained by the temporary auth spike.
- Advanced the Eligibility Spike from browser-authentication verification to Product Owner review of Issue `#7` and final go/no-go Issue `#8`.

## 2026-08-16 — Submission Runtime Boundary Revision

**Status:** Working tree — not committed

**Scope:** Revised the accepted AI Riser submission topology after confirming the distinction between an AI Studio project link and a public deployed application.

### Changed

- Reclassified the Google AI Studio project as frozen submission evidence rather than the production authentication origin.
- Established GitHub-managed source deployed to Cloud Run as the real Scan Pilot frontend and backend runtime.
- Redirected the remaining browser-authentication spike toward the production origin; the completed AI Studio-to-Cloud-Run CORS result remains limited connectivity evidence.

## 2026-08-16 — AI Studio to Cloud Run CORS Verification

**Status:** Working tree — not committed

**Scope:** Completed and accepted the credential-free Cloud Run connectivity spike for Issue `#6` without starting the production backend or browser authentication.

### Added

- Added evidence for actual AI Studio browser success and third-party-origin CORS failure against a temporary Cloud Run endpoint.
- Added the isolated temporary spike source under `spikes/issue-006-ai-studio-cors/`.

### Changed

- Recorded use of the Product Owner's existing shared MVP Google Cloud project while preserving strict separation from IoT credentials and resources.
- Advanced the Eligibility Spike's next task to browser authentication and session handoff.

## 2026-08-16 — AI Studio Export and Frozen Evidence Verification

**Status:** Working tree — not committed

**Scope:** Completed the Issue `#5` Eligibility Spike verification without starting implementation or copying sensitive Antigravity workspace state into the repository.

### Added

- Added a secret-safe export evidence record for the standard AI Studio ZIP, including its project identity, capture timestamp, and SHA-256 integrity digest.

### Changed

- Distinguished the standard ZIP snapshot from the sensitive Antigravity workspace transfer, which includes a configured secret and local workspace state.
- Recorded GitHub production source as the only post-handoff production source of truth and rejected continuous manual copy-paste synchronization.

## 2026-08-16 — AI Studio Signed-Out Access Verification

**Status:** Working tree — not committed

**Scope:** Completed and accepted the user-run Incognito access test for Issue `#4` without changing AI Studio sharing settings or beginning implementation.

### Added

- Added a structured AI Studio access evidence record: Google authentication is required; the tested separate account could view Preview and Code, while prompt/project information and original creation conversation were not observed.

### Changed

- Updated the submission context and current status to distinguish authenticated link access from anonymous public access and to record the accepted `PASS` result.

## 2026-08-16 — AI Riser Live Submission Contract Verification

**Status:** Working tree — not committed

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

**Status:** Working tree — not committed

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
