> **Document:** Scan Pilot Delivery Workflow
> **File:** `docs/DELIVERY-WORKFLOW.md`
> **Version:** v1.1.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# Scan Pilot Delivery Workflow

## Purpose

This document defines how accepted requirements become GitHub work items and how agents move an approved Issue through implementation, verification, Product Owner review, and closure.

## Agent Delivery Governance

When the `agent-delivery-governance` skill is active for this project, use these roles:

| Role | Responsibility |
|---|---|
| Product Owner | product scope, UI/UX, cost, permissions, final acceptance, and merge authority |
| Codex | technical decomposition, implementation brief, PR review, risk escalation |
| Antigravity | scoped implementation, tests, local implementation report, and PR handoff |

The `FULL_TRACKED` workflow is installed but remains pending its Integration Check. Activation requires a reviewable Antigravity branch/PR and confirmation that `.agent-work/` is Git-ignored and secret-free. Until then, the general Issue delivery policy remains in force.

## Operational Sources of Truth

| Concern | Source of truth |
|---|---|
| Product intent and accepted decisions | `docs/DECISIONS.md` and canonical specifications |
| Executable work and acceptance criteria | GitHub Issues in `NgaiLong49423/scan-pilot` |
| Priority, dates, ownership area, and progress | [GitHub Project #13](https://github.com/users/NgaiLong49423/projects/13) |
| Implemented behavior | Git-tracked source and migrations |
| Completion evidence | Tests, verification records, reviewed diff, and Issue comments |
| Completed checkpoints | `CHANGELOG.md` and Git history |

Canonical documents do not duplicate the complete task board. They record durable product truth and link to the relevant parent Issue or Project checkpoint.

## Project Status Contract

| Status | Entry condition | Exit condition |
|---|---|---|
| `Backlog` | Work is captured but not sufficiently refined. | Scope, source trace, acceptance criteria, dependencies, priority, estimate, and dates are ready. |
| `Planning` | The Issue is implementation-ready but work has not started. | An agent begins the authorized work or a blocker invalidates readiness. |
| `In Progress` | An agent is actively working on the Issue. | The work is ready for review or cannot continue. |
| `Review` | Changes and applicable verification are complete and await Product Owner acceptance. | The Product Owner accepts the result or returns it for more work. |
| `Done` | Acceptance criteria are verified and the Issue is closed. | The Issue is reopened because the accepted outcome is no longer satisfied. |

Use the `Blocked` label as an independent condition. Do not create a separate blocked status. Record the blocker, owner, and required unblock action in the Issue.

## Work Item Rules

- Use a GitHub Issue for a user-visible feature, defect, security rule, deployment change, database migration, research spike, benchmark, or coherent task that needs independent acceptance.
- Keep minor corrections inside the active Issue when they do not create an independently reviewable outcome.
- Use a parent Issue when three or more related children form one checkpoint or when direct implementation would be too large or risky.
- Keep Issue titles professional English and include source trace, scope, exclusions, testable acceptance criteria, estimation reason, relationships, and verification limits.
- Do not create, close, reschedule, or materially expand an Issue without the applicable user authorization.

## Agent Start Contract

When the user explicitly says to work on Issue `#N`, that instruction authorizes the assigned agent to:

1. read the Issue, parent, dependencies, canonical sources, and relevant code;
2. confirm that the Issue is ready and report conflicts;
3. move only that Issue to `In Progress`; and
4. add concise progress or blocker comments to that Issue when they provide durable value.

The instruction does not authorize creating unrelated Issues, closing the Issue, changing product scope, committing, pushing, opening or merging a pull request, provisioning cloud resources, or changing approved UI/UX.

## Implementation and Branch Contract

- Keep `main` stable.
- Prefer one branch per coherent checkpoint or large workstream rather than one branch for every small child task.
- Use the project-defined branch convention. Scan Pilot currently uses `codex/<issue-number>-<short-kebab-name>`; the reusable skill does not prescribe a prefix.
- Do not combine unrelated Issues merely to reduce branch or commit count.
- Reference the active Issue in progress reports and checkpoint proposals.

When `FULL_TRACKED` is active, every Git-tracked implementation needs a pull request. The PR is the reviewable handoff artifact; use `Refs #N` until its merge will satisfy every acceptance criterion, then use `Closes #N` only with Product Owner merge authority.

## Handoff Channel Contract

| Channel | Required content |
|---|---|
| Issue | requirements, acceptance criteria, scope/exclusions, source links, and Product Owner decisions |
| `.agent-work/` | detailed Codex brief, detailed Antigravity report, long logs, and intermediate analysis; never secrets |
| Branch | Antigravity implementation according to the project branch convention |
| Pull request | exact diff, `Refs #N`, compact scope/verification/limitation summary |
| PR review | Codex outcome and code-specific comments |
| Project #13 | status, priority, dates, workstream, and progress only |

Do not duplicate agent discussion on GitHub. A shared local file is never sole approval evidence: if Codex cannot access it, the PR summary and reviewed head SHA must still establish the minimum handoff.

## Review and Completion Gate

Before moving an Issue to `Review`, the implementer must provide the required PR and compact handoff summary. Codex then reviews the PR and must:

- compare the result with every acceptance criterion;
- run the narrowest relevant tests, then broader checks when practical;
- inspect the final diff for unrelated files, secrets, credentials, generated artifacts, and accidental contract changes;
- synchronize affected documentation, metadata, schema, and changelog entries;
- report failed, skipped, and unavailable verification honestly; and
- record one PR review outcome: `CHANGES_NEEDED`, `BLOCKED`, or `APPROVED_FOR_PO_ACCEPTANCE`; and
- add a concise Issue comment linking to the approved PR only when Product Owner acceptance is requested.

`Review` is the default agent handoff state. `CHANGES_NEEDED` returns the work to `In Progress`; `BLOCKED` retains `Review` with the `Blocked` label; `APPROVED_FOR_PO_ACCEPTANCE` remains in `Review` until the Product Owner comments `PO ACCEPTED` or `PO RETURNED` in the Issue. Only explicit Product Owner acceptance plus explicit merge authority permits merge or closure. A correctly linked pull request may close the Issue automatically after the authorized merge.

## Git and External-Action Boundary

Issue assignment and routine status updates do not authorize Git or deployment actions. Commit, push, pull request creation, merge, history rewrite, production deployment, paid-resource creation, credential changes, and destructive actions retain their separate approval requirements in `AGENTS.md`.

## GitHub Project Automation

Prefer GitHub Project built-in workflows for routine synchronization:

- auto-add repository Issues and pull requests;
- auto-add sub-issues;
- initialize newly added items in `Backlog`;
- update reopened items to `Planning`;
- update closed Issues and merged pull requests to `Done`; and
- link pull requests to Issues.

Keep automatic Issue closure disabled while Product Owner acceptance is the completion gate. Keep auto-archive disabled during the submission-critical period so completed and recently changed work remains visible.

## Verification Limit

This workflow provides traceability and approval boundaries. It does not prove that an implementation is correct, secure, deployable, or complete. Those claims require Issue-specific evidence and applicable tests.
