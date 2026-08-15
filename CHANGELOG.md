> **Document:** Scan Pilot Changelog  
> **File:** `CHANGELOG.md`  
> **Version:** v2.14.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-15  
> **Status:** Active  

# Scan Pilot Changelog

This file records notable Scan Pilot changes as a chronological, human-readable history. Git remains the exact file-level source of truth.

Each entry states whether it is already committed or still in the working tree. A working-tree entry is replaced with its commit hash when the coherent checkpoint is committed; it is not copied into a second entry. File paths in older entries may be normalized to a later canonical directory after an explicit structural migration; Git history remains the exact source for the path used by each historical commit.

## 2026-08-15 — AI Riser Submission Architecture and Validation

**Status:** Working tree — not committed

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

**Status:** Working tree — not committed

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

**Status:** Working tree — not committed

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

**Status:** Working tree — not committed

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

**Status:** Working tree — not committed

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

**Status:** Working tree — not committed

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
