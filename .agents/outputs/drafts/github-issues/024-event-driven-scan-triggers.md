> **Document:** GitHub Issue Draft 024 - Event-Driven Scan Triggers
> **File:** `.agents/outputs/drafts/github-issues/024-event-driven-scan-triggers.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Draft

# [Monitoring][FR-003] Add Authorized Event-Driven Scan Triggers

## Problem

The product direction promises push, pull-request, merge, scheduled, and manual scans, but only manual triggering is implemented.

## Source Trace

- `FR-003`, `FR-022`, `FR-024`, `FR-027`
- `DEC-003`, `DEC-004`
- `docs/IMPLEMENTATION-BASELINE.md`

## Scope

- Verify GitHub webhook signatures and installation/repository scope.
- Map supported push and pull-request events to idempotent scan jobs.
- Synchronize GitHub default-branch changes.
- Add a bounded scheduled trigger and retain manual scan behavior.
- Record trigger source, delivery identity, deduplication outcome, and coverage scope.

## Acceptance Criteria

- [ ] Invalid or replayed webhook deliveries cannot create duplicate unauthorized jobs.
- [ ] Push, pull-request, merge/default-branch, scheduled, and manual sources are distinguishable.
- [ ] Default-branch changes update PRIMARY according to `FR-022`/`FR-023`.
- [ ] Trigger bursts obey a documented concurrency/deduplication policy.
- [ ] Tests cover authorization, retries, duplicates, and branch routing.

## Planning

- Type: Feature
- Priority: High
- Size: L
- Story Points: 8
- Parent: TBD
- Blocked by: Draft 022, Draft 023
- Blocking: None
- Suggested branch: `codex/<issue-number>-event-scan-triggers`
- Security alert: Yes - webhook authorization
