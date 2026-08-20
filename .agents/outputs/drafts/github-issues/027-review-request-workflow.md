> **Document:** GitHub Issue Draft 027 - Review Request Workflow
> **File:** `.agents/outputs/drafts/github-issues/027-review-request-workflow.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Draft

# [Review][FR-012][FR-013] Implement Asynchronous Review Requests

## Problem

The schema contains Review Request foundations, but the application cannot create, list, answer, invalidate, or apply attributed user context through a complete workflow.

## Source Trace

- `FR-012`, `FR-013`, `FR-014`
- `DEC-020`, `DEC-021`
- `docs/EVIDENCE-MODEL.md`

## Scope

- Create non-blocking Review Requests from explicit insufficient-evidence conditions.
- Support structured choices, `I don't know`, `Another answer`, optional text, and a repository-relative path or GitHub link.
- Persist author/time/scope/source commit and treat responses as User Assertions.
- Implement pending, answered, invalidated, and expired behavior plus dashboard/API surfaces.

## Acceptance Criteria

- [ ] Scan completion never waits for a Review Request response.
- [ ] Secret-like response content is redacted before persistence, logging, display, or Gemini use.
- [ ] User Assertions cannot silently resolve a Finding or become Technical Evidence.
- [ ] Stale source-commit context is visibly invalidated or re-requested.
- [ ] Authorization tests prevent cross-user/repository access.

## Planning

- Type: Feature
- Priority: Medium
- Size: L
- Story Points: 8
- Parent: Draft 026 or TBD
- Blocked by: Evidence-producing caller and authorization model
- Blocking: None
- Suggested branch: `codex/<issue-number>-review-requests`
- Security alert: Yes - user content and authorization
