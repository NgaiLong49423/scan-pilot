> **Document:** Scan Pilot Changelog  
> **File:** `CHANGELOG.md`  
> **Version:** v1.9.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-13  
> **Status:** Active  

# Changelog

This file records meaningful product, documentation, database, feature, and structural changes. The metadata version above is the version of this document; release versions below are project versions when releases begin.

## [Unreleased]

### Added

- Established Scan Pilot as the official product name.
- Added canonical project context, accepted decisions, current status, product, requirements, architecture, scan lifecycle, inspection specification, and research-source documents.
- Transferred the A01 Broken Access Control research checkpoint into the main repository.
- Added the A02 Security Misconfiguration research checkpoint.
- Recorded `SP-CONFIG-001 — Source Code Secret Exposure` as the first accepted MUST MVP rule.
- Added standard document metadata for maintained project documentation.
- Added accepted Project Discovery and persistent Repository Profile direction.
- Added asynchronous human-in-the-loop Review Requests with structured and free-form responses.
- Added the canonical Evidence Model for typed evidence, scoped claims, provenance, and verification status.
- Added the canonical Finding Tracking Model for hybrid identity, repository versions, diff, workspaces, and re-scan verification.
- Added the accepted `SP_SECRET_FP_V1` HMAC-SHA-256 identity contract for `SP-CONFIG-001`.
- Added the accepted Git checkpoint policy for coherent, user-authorized commit and push proposals.
- Added the accepted lightweight branch workflow for Scan Pilot as a solo project.
- Added separate Finding lifecycle and remediation-quality semantics, including `ACTION_REQUIRED`, `RISK_CONTAINED`, `VERIFIED_COMPLETE`, and neutral `NOT_ASSESSED` handling.
- Added GitHub-derived PRIMARY branch monitoring, initialization gating, independent secondary scanning, and automatic default-branch synchronization.
- Added the accepted Gitleaks-backed snapshot/history scan pipeline, validated coverage and checkpoint contract, and onboarding option B.

### Changed

- Converted the generic Java web-app template documentation into an agent-readable Scan Pilot documentation system.
- Replaced the old template source-of-truth contract with Scan Pilot canonical documents.
- Updated legacy `VibeGuard` naming and `VG-` research identifiers to `Scan Pilot` and `SP-` in canonical documentation.
- Deprecated template PRD/SRS files in favor of `docs/PRODUCT.md` and `docs/REQUIREMENTS.md`.
- Updated product, requirements, architecture, lifecycle, and current-status documents for persistent project context and human review.
- Linked inspection, architecture, lifecycle, requirements, and current status to the accepted Evidence Model.
- Linked rule contracts and lifecycle requirements to the accepted hybrid Finding Tracking architecture.
- Applied scoped secret identity and key-version tracking to the inspection, architecture, lifecycle, and requirements documents.
- Replaced per-edit commit expectations with reviewable checkpoints and explicit, separate commit and push authorization.
- Corrected project-planning and architecture text that incorrectly described Scan Pilot as a two-person project.
- Defined `SP-CONFIG-001` resolution as clean current source plus credential invalidation while using Git-history cleanup to determine remediation quality.
- Defined the full-capacity default-branch policy: retain both user-selected secondary branches, remove the former primary from current scope, and preserve all historical security records.
- Replaced snapshot-only initialization with a validated full-history baseline gate and defined ancestor-only incremental scanning with re-baseline after history rewrites or incompatible scan contracts.

### Security

- Defined that detected secrets must be redacted from findings, logs, prompts, screenshots, and reports.
- Preserved the rule that untrusted repository code must not execute in the main API process.
- Defined repository documents and Review Request content as untrusted input and required secret redaction before storage or Gemini analysis.
- Defined that User Assertions and AI Inferences cannot silently replace or resolve observed Technical Evidence.
- Separated trusted repository acquisition from optional untrusted execution and prohibited backend credentials in execution workspaces.
- Required raw secret candidates and fingerprint key material to remain outside persistence, asynchronous results, logs, metrics, AI prompts, and untrusted execution.
- Required Gitleaks raw reports to remain temporary inside the trusted adapter boundary and prohibited automatic provider verification with discovered credentials.

## [Template Baseline] - 2026-08-11

### Added

- Initialized the generic Java web-app repository template.
