> **Document:** Scan Pilot Documentation Index
> **File:** `docs/README.md`
> **Version:** v1.18.0
> **Created:** 2026-08-11
> **Last Updated:** 2026-08-16
> **Status:** Active

# Scan Pilot Documentation

This directory is the canonical source of product, research, and specification context.

## Start Here

| Order | Document | Purpose | Status |
|---:|---|---|---|
| 1 | [Project Context](PROJECT-CONTEXT.md) | Product identity, users, direction, and open questions | Active |
| 2 | [Accepted Decisions](DECISIONS.md) | Decisions an agent must not silently override | Active |
| 3 | [Current Status](CURRENT-STATUS.md) | Current phase, checkpoint, and next logical task | Active |
| 4 | [Delivery Workflow](DELIVERY-WORKFLOW.md) | GitHub Issue execution, review, acceptance, and authorization boundaries | Active |
| 5 | [Cloud Budget and Cost Guardrails](CLOUD-BUDGET.md) | Accepted two-month funding envelope and cost constraints for design and operation | Active |
| 6 | [AI Riser Vietnam 2026 Submission Context](research/submission/AI-RISER-VIETNAM-2026.md) | External submission constraints and evaluation signals for direction-setting work | Under Review |
| 7 | [Product Definition](PRODUCT.md) | Product scope and MVP outcome | Under Review |
| 8 | [Requirements](REQUIREMENTS.md) | High-level functional and quality requirements | Under Review |
| 9 | [Inspection Specification](INSPECTION-SPEC.md) | Official rule contracts and rule candidates | Under Review |
| 10 | [Evidence Model](EVIDENCE-MODEL.md) | Provenance, evidence types, scoped claims, and verification status | Active |
| 11 | [Finding Tracking Model](FINDING-TRACKING.md) | Hybrid identity, repository versions, diff, workspaces, and re-scan tracking | Active |

## Design and Lifecycle

- [Software Requirements Specification (SRS)](requirements/SRS.md)
- [Use Cases Specification (UC-001 - UC-006)](USE-CASES.md)
- [Non-Functional Requirements (NFR-001 - NFR-010)](NON-FUNCTIONAL-REQUIREMENTS.md)
- [Local Development and Run Guide](LOCAL-DEVELOPMENT-GUIDE.md)
- [Architecture Direction](ARCHITECTURE.md)
- [Cloud Budget and Cost Guardrails](CLOUD-BUDGET.md)
- [Scan Lifecycle](SCAN-LIFECYCLE.md)
- [Evidence Model](EVIDENCE-MODEL.md)
- [Finding Tracking Model](FINDING-TRACKING.md)

## Research

- [AI Riser Vietnam 2026 Submission Context](research/submission/AI-RISER-VIETNAM-2026.md) — review before choosing or materially changing product direction, MVP scope, Google integration, deployment, demo, or submission material.
- [AI Riser Submission Contract Verification](research/submission/AI-RISER-SUBMISSION-CONTRACT-VERIFICATION.md) — live official deadline, deliverables, access limitation, and Issue `#3` evidence.
- [AI Studio Signed-Out Access Verification](research/submission/AI-STUDIO-SIGNED-OUT-ACCESS-VERIFICATION.md) — Issue `#4` evidence for required Google sign-in and judge-visible surfaces.
- [AI Studio Export and Frozen Evidence Verification](research/submission/AI-STUDIO-EXPORT-VERIFICATION.md) — Issue `#5` evidence for the one-way handoff and safe ZIP snapshot boundary.
- [AI Studio to Cloud Run CORS Verification](research/submission/AI-STUDIO-CLOUD-RUN-CORS-VERIFICATION.md) — Issue `#6` evidence for an actual browser CORS pass/fail path.
- [Production GitHub OAuth and Session Verification](research/submission/PRODUCTION-GITHUB-OAUTH-SESSION-VERIFICATION.md) — Issue `#7` evidence for the bounded same-origin OAuth callback and server-side token boundary.
- [AI Riser Eligibility Spike Result](research/submission/ELIGIBILITY-SPIKE-RESULT.md) — Issue `#8` accepted `CONDITIONAL GO` and mandatory implementation conditions.
- [Research Sources](RESEARCH-SOURCES.md)
- [Secret Scanning Content Scope Benchmark](research/benchmarks/SECRET-SCANNING-CONTENT-SCOPE.md)
- [Gitleaks Adapter Trust and Verification Benchmark](research/benchmarks/GITLEAKS-ADAPTER.md)
- [Secret Detection Validation Research](research/benchmarks/SECRET-DETECTION-VALIDATION.md) — separates independent detector evidence, Scan Pilot integration verification, and the controlled end-to-end security-lab journey.
- [Configuration Awareness Research](research/benchmarks/CONFIGURATION-AWARENESS.md)
- [Deferred Document Extraction Adapter Benchmark Plan](research/benchmarks/DOCUMENT-EXTRACTION-ADAPTER.md) — retained for optional Phase 2; do not execute for the MVP.
- [A01 Broken Access Control](research/security/A01-BROKEN-ACCESS-CONTROL.md)
- [A02 Security Misconfiguration](research/security/A02-SECURITY-MISCONFIGURATION.md)

Research notes and submission-context documents may contain unresolved or time-sensitive information. Only user-accepted content recorded in `DECISIONS.md` and promoted into a specification is authoritative for implementation.

## Legacy Template Documents

- `requirements/PRD.md` and `requirements/SRS.md` are deprecated template documents retained only to prevent broken historical references.
- `diagrams/` and `reports/` contain reusable template guidance; they are not current Scan Pilot specifications.

## Documentation Metadata

Important Markdown files must begin with document name, repository-relative file path, document version, created date, last updated date, and status. See `../.agents/skill/document-metadata-standardizer/SKILL.md`.

The canonical repository directory for agent skills, contracts, and generated agent outputs is `../.agents/`. Project documentation and tooling must use this path consistently.
