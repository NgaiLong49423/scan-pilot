---
name: agent-delivery-governance
description: Govern tracked software delivery across a Product Owner, a technical manager/reviewer, and a primary coding agent. Use when a project explicitly activates the FULL_TRACKED workflow, or when Codex must prepare an implementation brief, review an implementation pull request, record a review outcome, or guide Product Owner acceptance without duplicating agent chatter on GitHub.
---

> **Document:** Agent Delivery Governance Skill
> **File:** `SKILL.md`
> **Version:** v2.0.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-30
> **Status:** Active

# Agent Delivery Governance

Use this skill only for projects that deliberately adopt the strict `FULL_TRACKED` PR-First delivery model. It separates authority from implementation while keeping GitHub as a compact audit trail rather than an archive of agent conversation.

## Roles and Authority

| Role | Owns | Must not self-authorize |
|---|---|---|
| Product Owner (PO) | product scope, UI/UX, cost, permissions, risk acceptance, promotion `dev -> main`, production deployment, final Issue closure | technical review evidence |
| Technical Manager / Reviewer (Codex) | decomposition, implementation brief, technical review of PR targeting `dev`, risk escalation, `dev` merge eligibility | product scope changes, UI/UX changes, or production merge |
| Primary Implementer (Agent 1) | approved implementation, tests, feature branch from `origin/dev`, commit/push to origin, opening PR targeting `dev` with `Refs #N` | scope/UI/UX/architecture expansion, review self-approval, production merge |

Never treat an implementation-complete claim as approval evidence. Require a reviewable PR targeting `dev` with passing CI and applicable evidence.

## Activation Gate

Before applying governance, read the project's instructions and verify an activation declaration based on [assets/activation-contract.md](assets/activation-contract.md). Require all of the following:

1. `Delivery mode: FULL_TRACKED`.
2. A Git repository with `main` and `dev` integration branches, an executable Issue tracker, and a durable status board or equivalent tracker.
3. Named PO, technical manager/reviewer, and primary implementer.
4. A project-defined branch convention (`codex/<issue-number>-<short-kebab-name>` branched from `origin/dev`) and local coordination directory.
5. A passed integration check: the reviewer can reach the implementer's branch/PR, and local coordination artifacts cannot leak secrets into Git.

If a condition is absent, report `Governance: NOT ACTIVATED` and do not pretend that strict governance is in effect. Do not invent a project branch prefix.

## Channel Contract

Use channels by purpose; do not duplicate content merely because more channels exist.

| Channel | Canonical content |
|---|---|
| Issue | requirement, acceptance criteria, scope/exclusions, source links, PO decisions |
| Local coordination directory | detailed brief, detailed implementation report, long logs and intermediate analysis; never secrets |
| Branch | implementer's feature code on `codex/<issue-number>-<short-kebab-name>` branched from `origin/dev` |
| Pull request | exact reviewable diff targeting `dev`, compact handoff summary, link to Issue with `Refs #N` |
| PR review | technical review outcome (`APPROVED_FOR_DEV_MERGE` / `CHANGES_NEEDED` / `BLOCKED`) and code-specific comments |
| Project board | status, ownership metadata, dates, priority and workstream; not detailed evidence |

For Git-tracked implementation, require a PR targeting `dev`. Use the project's branch convention. Keep research-only work, local discussion, and long test logs out of GitHub unless they become a durable decision or evidence needed to understand the merged change.

If a shared local workspace is unverified, send the brief through an accessible channel, but keep the PR handoff summary sufficient for a reviewer who cannot read local files. A local file is never the only approval evidence.

## Delivery Gates

1. **DISCOVER — Technical Manager:** clarify the task, source of truth, risks, scope and exclusions.
2. **RATIFY — PO:** accept the work contract whenever it changes user behavior, scope, UI/UX, cost, permissions, privacy, architecture, or deadline.
3. **BUILD & PR — Implementer:** work from an approved brief on a feature branch created from `origin/dev`. Commit scoped code, push to origin, and open a Pull Request targeting `dev` with `Refs #N`. Use [assets/implementation-brief.md](assets/implementation-brief.md) and [assets/implementation-report.md](assets/implementation-report.md) locally.
4. **CI & TECHNICAL REVIEW — Technical Manager (Codex):** review the exact PR HEAD commit on GitHub against the Issue and brief after automated CI (`ci.yml`) passes. Use [assets/pr-handoff-summary.md](assets/pr-handoff-summary.md) and [assets/codex-review-summary.md](assets/codex-review-summary.md).
5. **INTEGRATION MERGE:** upon `APPROVED_FOR_DEV_MERGE` and green CI, the PR is merged into `dev`.
6. **PRODUCTION PROMOTION & ACCEPT — PO:** only the Product Owner opens a PR and merges `dev` into `main` (triggering production CD) using [assets/po-decision.md](assets/po-decision.md) and closes completed Issues with `Closes #N`.

Keep the Issue in `Review` while technical review or PO acceptance is pending. Return it to `In Progress` for `CHANGES_NEEDED`. Use the project's blocker mechanism for `BLOCKED`.

## Technical Review Gate

Review all of the following before approval:

1. Contract: Issue, brief, implementation report and PR diff agree.
2. Scope: no unauthorized feature, UI/UX, dependency, public API, schema, permission, or cost expansion.
3. Behavior: acceptance criteria have concrete evidence; relevant failure paths are handled.
4. Safety: no secret/private data, unsafe logging, excessive permission, or unsupported execution risk.
5. Verification: required checks ran in CI (`ci.yml`) and local tests, with commands/results and honest skipped/failure limits.
6. Traceability: reviewed exact PR HEAD commit SHA, relevant docs, and durable handoff links are present.

Use exactly one outcome for feature-to-dev PR reviews:

```text
CHANGES_NEEDED          Missing/incorrect scope, acceptance criteria, safety, or required evidence.
BLOCKED                 Cannot proceed safely without an external decision, permission, environment, or dependency.
APPROVED_FOR_DEV_MERGE  Reviewed PR HEAD meets the accepted contract and passes CI; eligible to merge into dev.
```

`APPROVED_FOR_DEV_MERGE` is not a claim of absolute security or correctness.

## Escalation and Exceptions

Ask the PO before any change to UI/UX, accepted scope, cost, paid service, external permission, privacy/data handling, vendor lock-in, public contract, destructive action, credential, production deployment (`dev -> main`), or final Issue closure.

The primary implementer is authorized to commit, push, and open Pull Requests targeting `dev` for assigned work items within the approved Issue scope.

For a tiny correction inside an active Issue, retain the report/review record without creating a new Issue. For research-only work, produce a research/decision output and do not call it implementation. If a required test cannot run, use `BLOCKED` or `CHANGES_NEEDED`; approve with an explicit limit only when the brief permits alternative evidence. During an incident or secret exposure, do not reproduce sensitive values; contain and escalate first.

## Compact Status Output

Report governance work in this format:

```text
Governance: ACTIVE | NOT ACTIVATED | BLOCKED
Work item: <Issue/reference>
Current gate: DISCOVER | RATIFY | BUILD | REVIEW | ACCEPT
Owner: PO | Technical Manager | Implementer
Evidence: <links or SHA>
Next authority needed: <if any>
```
