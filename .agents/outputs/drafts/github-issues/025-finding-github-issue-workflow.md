> **Document:** GitHub Issue Draft 025 - Finding to GitHub Issue Workflow
> **File:** `.agents/outputs/drafts/github-issues/025-finding-github-issue-workflow.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Draft

# [Remediation][FR-006] Create a Secret-Safe GitHub Issue from a Finding

## Problem

`FR-006` is accepted, but no end-to-end API or UI workflow creates or drafts a GitHub Issue from a Finding.

## Source Trace

- `FR-004`, `FR-006`, `FR-014`, `FR-030`
- `DEC-003`, `DEC-007`, `DEC-021`
- `docs/IMPLEMENTATION-BASELINE.md`

## Scope

- Generate a reviewable issue draft from normalized redacted Finding data.
- Require explicit user confirmation before the external GitHub write.
- Prevent raw secrets, unsafe snippets, internal tokens, and unsupported certainty claims.
- Store the created Issue number/URL and idempotency relationship without using Issue closure as proof of Finding resolution.

## Acceptance Criteria

- [ ] Preview contains rule, affected location, masked evidence, limits, and remediation steps without raw secret material.
- [ ] No GitHub Issue is created before explicit confirmation.
- [ ] Repeated submission cannot create unintended duplicates.
- [ ] Created Issue linkage is persisted and visible.
- [ ] Closing the GitHub Issue does not automatically resolve the Finding.

## Planning

- Type: Feature
- Priority: Medium
- Size: M
- Story Points: 5
- Parent: TBD
- Blocked by: Draft 023
- Blocking: None
- Suggested branch: `codex/<issue-number>-finding-github-issue`
- Security alert: Yes - external disclosure boundary
