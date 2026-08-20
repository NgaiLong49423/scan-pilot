> **Document:** GitHub Issue Draft 022 - Asynchronous Scan Jobs and Progress
> **File:** `.agents/outputs/drafts/github-issues/022-async-scan-job-progress.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Draft

# [Scan][FR-002][NFR-001] Dispatch Asynchronous Scan Jobs and Expose Real Progress

## Problem

`POST /api/v1/scans/trigger` currently executes the complete pipeline synchronously, while the SRS and Use Case contract require immediate job creation and frontend polling of durable status.

## Source Trace

- `FR-002`, `FR-003`, `FR-028`, `FR-029`
- `NFR-001`, `NFR-002`
- `UC-003`
- `docs/IMPLEMENTATION-BASELINE.md`

## Scope

- Persist and return a queued scan job without running the scan on the request thread.
- Execute the job in a bounded backend executor suitable for the current modular monolith.
- Persist monotonic stages, timestamps, terminal status, and sanitized error detail.
- Poll `GET /api/v1/scans/jobs/{jobId}` from the frontend and render only returned state.
- Define duplicate-trigger and application-restart behavior.

## Acceptance Criteria

- [ ] Trigger response returns promptly with a job ID and non-terminal status.
- [ ] Job status progresses through persisted, documented stages and one terminal state.
- [ ] Frontend stops polling on completion/failure and refreshes findings/coverage once.
- [ ] Concurrent or duplicate triggers follow an explicit tested policy.
- [ ] Errors contain no raw secret or private source content.

## Planning

- Type: Feature
- Priority: High
- Size: L
- Story Points: 8
- Parent: TBD
- Blocked by: Draft 021
- Blocking: Draft 024
- Suggested branch: `codex/<issue-number>-async-scan-jobs`
- Security alert: No
