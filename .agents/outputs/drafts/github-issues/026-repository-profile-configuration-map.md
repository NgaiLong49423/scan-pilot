> **Document:** GitHub Issue Draft 026 - Repository Profile and Configuration Map Foundation
> **File:** `.agents/outputs/drafts/github-issues/026-repository-profile-configuration-map.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Draft

# [Discovery][FR-010][FR-039] Build the Repository Profile and Configuration Inventory Vertical Slice

## Problem

Repository Profile and Configuration Awareness are accepted product capabilities, but the current application has no complete discovery extractor, attributed profile, or Configuration Map.

## Source Trace

- `FR-010`, `FR-034`, `FR-039`–`FR-044`
- `DEC-019`, `DEC-033`, `DEC-038`–`DEC-043`
- `docs/ARCHITECTURE.md`

## Scope

- Inventory repository structure, supported manifests, and configuration artifacts deterministically.
- Persist source commit, digest, classifier/parser version, evidence, and verification status.
- Implement one bounded family slice selected through a separate Product Owner decision.
- Expose a neutral Configuration Map separating inventory, coverage, change, and Findings.

## Acceptance Criteria

- [ ] Every profile/configuration claim has source, scope, commit, producer version, and verification status.
- [ ] Binary documents remain inventory-only for the MVP.
- [ ] Ambiguous/unsupported/parse-failed items are neutral limitations, not clean or critical.
- [ ] No repository document can instruct the scanner or override system policy.
- [ ] The selected family scope and exclusions are explicitly approved before implementation.

## Planning

- Type: Epic candidate
- Priority: Medium
- Size: XL
- Story Points: 13
- Parent: TBD
- Blocked by: Product Owner family selection
- Blocking: Draft 027 when profile claims require user context
- Suggested branch: `codex/<issue-number>-repository-profile`
- Security alert: No
