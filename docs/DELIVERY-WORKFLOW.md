> **Document:** Scan Pilot Delivery Workflow
> **File:** `docs/DELIVERY-WORKFLOW.md`
> **Version:** v1.0.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# Scan Pilot Delivery Workflow

## Purpose

This document defines how accepted requirements become GitHub work items and how agents move an approved Issue through implementation, verification, Product Owner review, and closure.

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
- When an Issue-specific branch is useful, use `codex/<issue-number>-<short-kebab-name>`.
- Do not combine unrelated Issues merely to reduce branch or commit count.
- Reference the active Issue in progress reports and checkpoint proposals.

Use `Refs #N` while a pull request provides partial or reviewable progress. Use `Closes #N` only when merging into the default branch will satisfy every acceptance criterion for that Issue.

## Review and Completion Gate

Before moving an Issue to `Review`, the agent must:

- compare the result with every acceptance criterion;
- run the narrowest relevant tests, then broader checks when practical;
- inspect the final diff for unrelated files, secrets, credentials, generated artifacts, and accidental contract changes;
- synchronize affected documentation, metadata, schema, and changelog entries;
- report failed, skipped, and unavailable verification honestly; and
- add a concise Issue comment containing the result, evidence, known limitations, and remaining Product Owner decisions.

`Review` is the default agent handoff state. Only explicit user acceptance authorizes manual Issue closure or a manual move to `Done`. A correctly linked pull request may close the Issue automatically when the user authorizes and completes the merge.

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
