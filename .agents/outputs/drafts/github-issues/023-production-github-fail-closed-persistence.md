> **Document:** GitHub Issue Draft 023 - Production GitHub Fail-Closed and Durable Configuration
> **File:** `.agents/outputs/drafts/github-issues/023-production-github-fail-closed-persistence.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Draft

# [GitHub][FR-020][FR-047] Fail Closed and Persist Repository Configuration

## Problem

Repository discovery can return development repositories after a GitHub API error. Current-project and secondary-branch state also depend on in-memory maps, creating restart and authorization ambiguity.

## Source Trace

- `FR-001`, `FR-020`, `FR-022`, `FR-023`, `FR-047`
- `DEC-054`, `DEC-060`
- `UC-001`, `UC-002`
- `docs/IMPLEMENTATION-BASELINE.md`

## Scope

- Restrict mock repository data and development login to an explicit non-production profile.
- Return an actionable error when GitHub repository discovery fails in production.
- Make PostgreSQL the source of truth for selected repository and branch configuration.
- Enforce installation authorization for the selected repository.
- Persist and test logout, expiry, revocation, and restart behavior relevant to repository access.

## Acceptance Criteria

- [ ] Production GitHub failures never return fabricated repositories.
- [ ] Repository and branch configuration survives an application restart.
- [ ] A user cannot select or scan a repository outside the linked installation scope.
- [ ] Logout/expiry/revocation tests prove access is rejected afterward.
- [ ] Error responses and logs expose no token or private source data.

## Planning

- Type: Security Feature
- Priority: Critical
- Size: L
- Story Points: 8
- Parent: TBD
- Blocked by: None
- Blocking: Draft 024
- Suggested branch: `codex/<issue-number>-github-fail-closed`
- Security alert: Yes - authorization and fabricated data
