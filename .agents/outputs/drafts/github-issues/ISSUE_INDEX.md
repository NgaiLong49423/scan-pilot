> **Document:** Scan Pilot GitHub Issue Index
> **File:** `.agents/outputs/drafts/github-issues/ISSUE_INDEX.md`
> **Version:** v1.1.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-17
> **Status:** Active

# Scan Pilot GitHub Issue Index

## Purpose

This index backfills local traceability for the first approved operational Issues created directly from the accepted AI Riser Eligibility Spike checkpoint. No pre-creation draft files existed for these items; the live GitHub Issue is the preserved work-item body.

## Project Sync

- Repository: `NgaiLong49423/scan-pilot`
- Project: [Scan Pilot — AI Riser 2026](https://github.com/users/NgaiLong49423/projects/13)
- Milestone: `AI Riser 2026 — Stable Submission`
- Sync status: `Synced`
- Verified: `2026-08-15`

## Traceability

| Issue | Relationship | Source trace | Points | Priority | Workstream | Status at creation |
|---|---|---|---:|---|---|---|
| [#2](https://github.com/NgaiLong49423/scan-pilot/issues/2) Validate AI Riser submission architecture | Parent | `DEC-044`–`DEC-051`; `docs/CURRENT-STATUS.md` Next Logical Task | — | Critical | Submission | Planning |
| [#3](https://github.com/NgaiLong49423/scan-pilot/issues/3) Confirm submission requirements and deadline | Child of #2 | `DEC-051`; `docs/research/submission/AI-RISER-VIETNAM-2026.md` Open Verification Items | 1 | Critical | Submission | Planning |
| [#4](https://github.com/NgaiLong49423/scan-pilot/issues/4) Verify signed-out AI Studio access | Child of #2 | `DEC-045`; submission-context Open Verification Items | 2 | Critical | Submission | Planning |
| [#5](https://github.com/NgaiLong49423/scan-pilot/issues/5) Verify export and frozen evidence | Child of #2 | `DEC-044`; submission-context Open Verification Items | 2 | High | Frontend | Planning |
| [#6](https://github.com/NgaiLong49423/scan-pilot/issues/6) Prove AI Studio to Cloud Run call | Child of #2 | `DEC-045`; `docs/ARCHITECTURE.md` Eligibility Spike | 3 | Critical | Cloud | Planning |
| [#7](https://github.com/NgaiLong49423/scan-pilot/issues/7) Verify GitHub authentication handoff | Child of #2 | `DEC-047`; `FR-046`; `docs/ARCHITECTURE.md` Eligibility Spike | 5 | Critical | GitHub | Planning |
| [#8](https://github.com/NgaiLong49423/scan-pilot/issues/8) Record go/no-go result | Child of #2 | `docs/CURRENT-STATUS.md` Next Logical Task | 2 | Critical | Submission | Planning |
| [#9](https://github.com/NgaiLong49423/scan-pilot/issues/9) Establish production foundation and Antigravity handoff | Requirement-derived Issue | `DEC-044`, `DEC-045`, `DEC-054`, `DEC-055`; `FR-036`, `FR-045`–`FR-046` | 5 | High | Frontend + Backend + Documentation | Created 2026-08-17 |

## Retrospective Note

These Issues were created after explicit user approval and then synchronized with verified Project field and relationship IDs. Future requirement-derived Issue creation should use the planning, draft, creation, and Project-sync stages in `.agents/skill/srs-to-github-issues/SKILL.md` before creating live Issues.
