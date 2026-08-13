> **Document:** Scan Pilot Documentation Index  
> **File:** `docs/README.md`  
> **Version:** v1.2.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-12  
> **Status:** Active  

# Scan Pilot Documentation

This directory is the canonical source of product, research, and specification context.

## Start Here

| Order | Document | Purpose | Status |
|---:|---|---|---|
| 1 | [Project Context](PROJECT-CONTEXT.md) | Product identity, users, direction, and open questions | Active |
| 2 | [Accepted Decisions](DECISIONS.md) | Decisions an agent must not silently override | Active |
| 3 | [Current Status](CURRENT-STATUS.md) | Current phase, checkpoint, and next logical task | Active |
| 4 | [Product Definition](PRODUCT.md) | Product scope and MVP outcome | Under Review |
| 5 | [Requirements](REQUIREMENTS.md) | High-level functional and quality requirements | Under Review |
| 6 | [Inspection Specification](INSPECTION-SPEC.md) | Official rule contracts and rule candidates | Under Review |
| 7 | [Evidence Model](EVIDENCE-MODEL.md) | Provenance, evidence types, scoped claims, and verification status | Active |
| 8 | [Finding Tracking Model](FINDING-TRACKING.md) | Hybrid identity, repository versions, diff, workspaces, and re-scan tracking | Active |

## Design and Lifecycle

- [Architecture Direction](ARCHITECTURE.md)
- [Scan Lifecycle](SCAN-LIFECYCLE.md)
- [Evidence Model](EVIDENCE-MODEL.md)
- [Finding Tracking Model](FINDING-TRACKING.md)
- Scoring model: not yet created because the scoring formula is unresolved.
- Data model: not yet created because the exact database schema remains unresolved.

## Research

- [Research Sources](RESEARCH-SOURCES.md)
- [A01 Broken Access Control](research/security/A01-BROKEN-ACCESS-CONTROL.md)
- [A02 Security Misconfiguration](research/security/A02-SECURITY-MISCONFIGURATION.md)

Research notes may contain unresolved candidates. Only user-accepted content recorded in `DECISIONS.md` and promoted into a specification is authoritative for implementation.

## Legacy Template Documents

- `requirements/PRD.md` and `requirements/SRS.md` are deprecated template documents retained only to prevent broken historical references.
- `diagrams/` and `reports/` contain reusable template guidance; they are not current Scan Pilot specifications.

## Documentation Metadata

Important Markdown files must begin with document name, repository-relative file path, document version, created date, last updated date, and status. See `../.agent/skill/document-metadata-standardizer/SKILL.md`.
