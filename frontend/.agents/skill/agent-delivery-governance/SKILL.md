---
name: agent-delivery-governance
description: Govern tracked software delivery across a Product Owner, a technical manager/reviewer, and a primary coding agent. Use when a project explicitly activates the FULL_TRACKED workflow, or when Codex must prepare an implementation brief, review an implementation pull request, record a review outcome, or guide Product Owner acceptance without duplicating agent chatter on GitHub.
---

> **Document:** Agent Delivery Governance Skill
> **File:** `SKILL.md`
> **Version:** v1.0.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# Agent Delivery Governance

Use this skill only for projects that deliberately adopt the strict `FULL_TRACKED` delivery model. It separates authority from implementation while keeping GitHub as a compact audit trail rather than an archive of agent conversation.

## Roles and Authority

| Role | Owns | Must not self-authorize |
|---|---|---|
| Product Owner (PO) | scope, UI/UX, cost, permissions, risk acceptance, final acceptance | technical review evidence |
| Technical Manager / Reviewer | decomposition, implementation brief, technical review, risk escalation | product changes or final acceptance |
| Primary Implementer | approved implementation, tests, implementation report | scope/UI/UX/architecture expansion, review approval, final acceptance |

Never treat an implementation-complete claim as approval evidence. Require a reviewable PR and the applicable evidence.

## Activation Gate

Before applying governance, read the project's instructions and verify an activation declaration based on [assets/activation-contract.md](assets/activation-contract.md). Require all of the following:

1. `Delivery mode: FULL_TRACKED`.
2. A Git repository, an executable Issue tracker, and a durable status board or equivalent tracker.
3. Named PO, technical manager/reviewer, and primary implementer.
4. A project-defined branch convention and local coordination directory.
5. A passed integration check: the reviewer can reach the implementer's branch/PR, and local coordination artifacts cannot leak secrets into Git.

If a condition is absent, report `Governance: NOT ACTIVATED` and do not pretend that strict governance is in effect. Do not invent a project branch prefix.

## Channel Contract

Use channels by purpose; do not duplicate content merely because more channels exist.

| Channel | Canonical content |
|---|---|
| Issue | requirement, acceptance criteria, scope/exclusions, source links, PO decisions |
| Local coordination directory | detailed brief, detailed implementation report, long logs and intermediate analysis; never secrets |
| Branch | implementer's work-in-progress code |
| Pull request | exact reviewable diff, compact handoff summary, link to Issue |
| PR review | technical review outcome and code-specific comments |
| Project board | status, ownership metadata, dates, priority and workstream; not detailed evidence |

For Git-tracked implementation, require a PR. Use the project's branch convention. Keep research-only work, local discussion, and long test logs out of GitHub unless they become a durable decision or evidence needed to understand the merged change.

If a shared local workspace is unverified, send the brief through an accessible channel, but keep the PR handoff summary sufficient for a reviewer who cannot read local files. A local file is never the only approval evidence.

## Delivery Gates

1. **DISCOVER — Technical Manager:** clarify the task, source of truth, risks, scope and exclusions.
2. **RATIFY — PO:** accept the work contract whenever it changes user behavior, scope, UI/UX, cost, permissions, privacy, architecture, or deadline.
3. **BUILD — Implementer:** work only from an approved brief. Use [assets/implementation-brief.md](assets/implementation-brief.md) and [assets/implementation-report.md](assets/implementation-report.md) locally.
4. **REVIEW — Technical Manager:** move to `Review` when the implementer provides a PR plus compact handoff. Review the PR against the Issue and brief. Use [assets/pr-handoff-summary.md](assets/pr-handoff-summary.md) and [assets/codex-review-summary.md](assets/codex-review-summary.md).
5. **ACCEPT — PO:** record `PO ACCEPTED` or `PO RETURNED` in the Issue using [assets/po-decision.md](assets/po-decision.md). Only an explicit merge authorization permits merging or closure.

Keep the Issue in `Review` while technical review or PO acceptance is pending. Return it to `In Progress` for `CHANGES_NEEDED`. Use the project's blocker mechanism for `BLOCKED`.

## Technical Review Gate

Review all of the following before approval:

1. Contract: Issue, brief, implementation report and PR diff agree.
2. Scope: no unauthorized feature, UI/UX, dependency, public API, schema, permission, or cost expansion.
3. Behavior: acceptance criteria have concrete evidence; relevant failure paths are handled.
4. Safety: no secret/private data, unsafe logging, excessive permission, or unsupported execution risk.
5. Verification: required checks ran, with commands/results and honest skipped/failure limits.
6. Traceability: reviewed head SHA, relevant docs, and durable handoff links are present.

Use exactly one outcome:

```text
CHANGES_NEEDED              Missing/incorrect scope, acceptance criteria, safety, or required evidence.
BLOCKED                      Cannot proceed safely without an external decision, permission, environment, or dependency.
APPROVED_FOR_PO_ACCEPTANCE   Reviewed diff meets the accepted contract within stated verification limits.
```

`APPROVED_FOR_PO_ACCEPTANCE` is not a claim of absolute security or correctness.

## Escalation and Exceptions

Ask the PO before any change to UI/UX, accepted scope, cost, paid service, external permission, privacy/data handling, vendor lock-in, public contract, destructive action, credential, deployment, commit, push, merge, or Issue closure.

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
