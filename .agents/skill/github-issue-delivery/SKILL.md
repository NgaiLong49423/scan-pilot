---
name: github-issue-delivery
description: Execute an approved Scan Pilot GitHub Issue through readiness checks, Project status updates, scoped implementation or research, verification, review handoff, and Product Owner acceptance. Use when the user asks an agent to start, work on, continue, review, or finish a numbered Scan Pilot Issue; do not use to decompose requirements into new Issues.
---

> **Document:** GitHub Issue Delivery Skill
> **File:** `.agents/skill/github-issue-delivery/SKILL.md`
> **Version:** v1.0.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# GitHub Issue Delivery

Follow `AGENTS.md`, `docs/DELIVERY-WORKFLOW.md`, and the target Issue. Treat the Issue acceptance criteria as the work contract and canonical specifications as product truth.

## 1. Resolve and Inspect

1. Resolve the repository, Issue number, parent, sub-issues, dependencies, milestone, and Project fields.
2. Read the canonical source references named by the Issue.
3. Inspect relevant code, tests, documentation, and current Git state.
4. Stop and report conflicts, missing authority, unsafe scope, or an Issue that is not ready.

Do not infer that a request to discuss or inspect an Issue authorizes implementation.

## 2. Start Authorized Work

When the user explicitly asks to work on Issue `#N`:

- move only `#N` to `In Progress`;
- add a progress comment only when it creates durable project value;
- keep the work inside the Issue scope; and
- propose a branch when useful, but do not create it without the applicable authorization.

This authorization does not include creating or closing Issues, committing, pushing, opening or merging a pull request, provisioning cloud resources, or changing product scope or UI/UX.

## 3. Execute the Smallest Coherent Solution

- Prefer a complete vertical slice over disconnected layer work.
- Preserve accepted architecture and public contracts.
- Escalate changes to user behavior, scope, deadline, cost, privacy, permissions, UI/UX, vendor lock-in, or irreversible external state.
- Record a blocker with the `Blocked` label, reason, owner, and unblock action; do not hide it by changing dates silently.
- Avoid noisy progress comments and per-file Git checkpoints.

## 4. Verify

Before claiming readiness:

1. map each acceptance criterion to evidence;
2. run narrow relevant checks and broader checks when practical;
3. inspect the final diff for unrelated changes, secrets, generated files, and unsafe data;
4. synchronize affected docs, metadata, schema, tests, and changelog;
5. record failed, skipped, or unavailable checks; and
6. state the verification limit.

## 5. Hand Off for Review

When the Issue contract is satisfied:

- add one concise completion comment with scope, evidence, tests, limitations, affected files, and remaining decisions;
- move the Issue to `Review`;
- ask for Product Owner acceptance; and
- separately propose commit, push, or pull request actions when the checkpoint is ready.

Do not manually close the Issue or set `Done` without explicit user acceptance. If accepted work is merged with an authorized `Closes #N` relationship, GitHub may close it automatically.

## 6. Finish

After explicit acceptance, verify the Issue state, Project status, parent progress, and linked pull request or commit. Report the final state without claiming checks that did not run.

## Output Contract

Keep updates concise:

```text
Issue: #N
Status: Planning | In Progress | Review | Done | Blocked
Outcome: ...
Verification: ...
Limitations: ...
Next approval needed: ...
```
