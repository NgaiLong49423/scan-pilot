> **Document:** Scan Pilot Document Metadata Report  
> **File:** `.agents/outputs/reports/DOCUMENT_METADATA_REPORT.md`  
> **Version:** v1.22.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-15  
> **Status:** Active  

# Document Metadata Report

## Summary

- Repository: Scan Pilot
- Audit date: 2026-08-15
- Files reviewed: 34 maintained project Markdown documents
- Files with complete metadata: 34
- Files missing metadata: 0
- Files with invalid metadata: 0
- Version updates still needed: 0
- Date issues: 0
- Overall status: Ready

The 34-file count excludes repository skill packages and `.github/pull_request_template.md`. Skill packages use YAML frontmatter required by the skill system; the pull-request template is copied into PR bodies and should not display document metadata.

This report was refreshed after the AI Riser submission-architecture checkpoint synchronized the accepted tool roles, submission MVP, onboarding, Gemini boundary, validation evidence, deadline, and Eligibility Spike across canonical documentation.

## Overall Status

**Ready** — maintained project documentation has sufficient metadata for normal repository and agent work.

## Files Reviewed

| File group | Count | Metadata status | Notes |
|---|---:|---|---|
| Root project documents | 4 | Complete | `AGENTS.md`, `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md` |
| Canonical `docs/` documents | 13 | Complete | Includes the active Evidence, Finding Tracking, and Cloud Budget models; active context is separated from draft/under-review specifications |
| Research and submission context | 8 | Complete | Security research, product benchmarks, validation research, and submission context are correctly marked `Under Review` |
| Legacy/template documentation | 6 | Complete | Correctly marked `Deprecated` or `Template` |
| Source/database folder notices | 2 | Complete | Legacy app folder deprecated; database status remains draft |
| Metadata audit report | 1 | Complete | Automatically created because more than five files were modified |

## Findings Resolved

| ID | Severity | Problem | Resolution |
|---|---|---|---|
| META-001 | Critical | Generic template documents could be mistaken for Scan Pilot source of truth. | Replaced canonical entry points and deprecated PRD/SRS template files. |
| META-002 | Major | Maintained project Markdown lacked required metadata. | Added document name, relative path, version, creation date, last-updated date, and status. |
| META-003 | Major | Draft research and specifications had no stability signal. | Applied `v0.1.0` with `Draft` or `Under Review`. |
| META-004 | Minor | Template documentation contained local file URIs. | Replaced project-facing local links with repository-relative links. |
| META-005 | Major | Agent-support files and documentation used conflicting singular and plural directory paths. | Established `.agents/` as the canonical directory and synchronized current links, templates, contracts, and report metadata. |

## Migration Baseline Version Decisions

The following table records the versions assigned during the original documentation migration. The latest compatible version updates are listed separately below.

| Files | Version | Decision | Reason |
|---|---|---|---|
| `AGENTS.md`, `README.md`, `CHANGELOG.md` | v1.0.0 | Template to Active | User approved conversion from a generic template to the stable Scan Pilot repository entry points. |
| `CONTRIBUTING.md` | v1.0.0 | Active baseline | Existing contribution rules remain valid and now participate in the metadata standard. |
| `docs/PROJECT-CONTEXT.md`, `docs/DECISIONS.md`, `docs/CURRENT-STATUS.md`, `docs/RESEARCH-SOURCES.md` | v1.0.0 | Active baseline | These contain user-accepted context and are stable enough to guide agents. |
| `docs/PRODUCT.md`, `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`, `docs/INSPECTION-SPEC.md` | v0.1.0 | Under Review | Important boundaries and implementation details remain unresolved. |
| `docs/SCAN-LIFECYCLE.md`, `database/README.md` | v0.1.0 | Draft | Lifecycle and database contracts are not finalized. |
| A01 and A02 research files | v0.1.0 | Under Review | Research checkpoints can guide discussion but are not complete implementation contracts. |
| Legacy PRD/SRS and `App/README.md` | v1.0.0 | Deprecated | Retained only to preserve historical paths and prevent agents from using old template content. |
| Diagram and report guides | v1.0.0 | Template | Reusable guidance, not current project truth. |
| `docs/EVIDENCE-MODEL.md` | v1.0.0 | Active baseline | The user accepted the shared evidence provenance and verification contract. |
| `docs/FINDING-TRACKING.md` | v1.0.0 | Active baseline | The user accepted the hybrid tracking, snapshot, workspace, and execution boundaries. |

## Latest Version Updates

| File | Previous | Current | Reason |
|---|---:|---:|---|
| `docs/PROJECT-CONTEXT.md` | v1.0.0 | v1.9.0 | Added persistent context, solo delivery and budget constraints, submission tool roles, deadline, and Product Owner decision altitude. |
| `AGENTS.md` | v1.0.0 | v1.8.0 | Added Git checkpoints, research and cost boundaries, canonical `.agents/`, Maven, and the Product Owner versus technical decision boundary. |
| `CONTRIBUTING.md` | v1.0.0 | v1.2.0 | Documented coherent checkpoints and practical branch and PR behavior for the solo project. |
| `docs/DECISIONS.md` | v1.1.0 | v1.22.0 | Recorded DEC-019 through DEC-051, including the accepted AI Riser submission architecture and validation contract. |
| `docs/CURRENT-STATUS.md` | v1.1.0 | v1.22.0 | Recorded the submission checkpoint and routed next work to the bounded Eligibility Spike. |
| `docs/PRODUCT.md` | v0.1.0 | v0.12.0 | Added the focused real submission vertical slice, one-way handoff, Gemini role, and security-lab story. |
| `docs/REQUIREMENTS.md` | v0.1.0 | v0.20.0 | Added FR-045 through FR-051 for the accepted submission contract. |
| `docs/ARCHITECTURE.md` | v0.1.0 | v0.15.0 | Added submission topology, workspace separation, GitHub onboarding, Gemini authority, and validation boundaries. |
| `docs/SCAN-LIFECYCLE.md` | v0.1.0 | v0.15.0 | Added configuration discovery, scenario, change invalidation, and evidence-reuse lifecycle behavior. |
| `docs/INSPECTION-SPEC.md` | v0.2.0 | v0.12.0 | Applied the trusted Gitleaks policy, coverage, content eligibility, binary-document skips, and size limits to `SP-CONFIG-001`. |
| `docs/EVIDENCE-MODEL.md` | New | v1.0.0 | Created the accepted canonical Evidence Model. |
| `docs/FINDING-TRACKING.md` | New | v1.3.0 | Added product-owned scan coverage and compatible checkpoint behavior. |
| `docs/RESEARCH-SOURCES.md` | v1.0.0 | v1.9.0 | Added independent secret-detection, OWASP Benchmark, and NIST SARD validation sources. |
| `docs/research/submission/AI-RISER-VIETNAM-2026.md` | New | v0.2.0 | Added the reviewed hybrid toolchain, accepted submission implications, deadline, and remaining live checks. |
| `docs/research/benchmarks/SECRET-DETECTION-VALIDATION.md` | New | v0.1.0 | Added the source-attributed independent validation and controlled security-lab research contract. |
| `docs/research/benchmarks/SECRET-SCANNING-CONTENT-SCOPE.md` | New | v0.4.0 | Added comparative evidence for eligibility and the accepted two-tier full-file size policy under `DEC-036`. |
| `docs/research/benchmarks/GITLEAKS-ADAPTER.md` | New | v0.1.0 | Added the source-attributed trust, command, coverage, safety, and cleanup benchmark contract for `DEC-037`. |
| `docs/research/benchmarks/CONFIGURATION-AWARENESS.md` | New | v0.2.0 | Recorded the accepted general contract and routed the next checkpoint to first-family selection. |
| `docs/research/benchmarks/DOCUMENT-EXTRACTION-ADAPTER.md` | New | v0.3.0 | Preserved the source-attributed adapter plan but marked it deferred to optional Phase 2 by `DEC-033`. |
| `docs/CLOUD-BUDGET.md` | New | v1.1.0 | Added the accepted envelope and removed deferred document-parser capacity from current allocation. |
| `README.md` | v1.0.0 | v1.1.0 | Added Maven to the backend technology direction. |
| `docs/research/security/A02-SECURITY-MISCONFIGURATION.md` | v0.1.0 | v0.5.0 | Closed the general contract and routed A02 research to first-family comparison. |
| `docs/README.md` | v1.0.0 | v1.10.0 | Added the secret-detection validation research entry. |
| `CHANGELOG.md` | v1.0.0 | v2.14.0 | Recorded the AI Riser submission architecture and validation checkpoint. |

## Date Checks

- Existing template-derived documents use `Created: 2026-08-11`, verified from the earliest file-specific Git commit.
- Documents created during migration use `Created: 2026-08-12`.
- Documents edited for the AI Riser submission checkpoint use `Last Updated: 2026-08-15`; unchanged documents retain their prior date.
- No `Created` date occurs after `Last Updated`.

## Final Recommendation

Metadata is ready for normal repository work. Before GitHub Issue decomposition or coding, the under-review product, requirement, architecture, lifecycle, and inspection documents still require content approval; their metadata correctly communicates that limitation.
