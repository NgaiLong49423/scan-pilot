---
name: pull-request-reviewer
description: Review GitHub Pull Requests for code quality, security, verification evidence, and PR-First dev delivery workflow compliance.
---

> **Document:** Pull Request Reviewer Skill
> **File:** `SKILL.md`
> **Version:** v1.0.0
> **Created:** 2026-08-30
> **Last Updated:** 2026-08-30
> **Status:** Active

# Pull Request Reviewer

This skill guides technical review of GitHub Pull Requests within the `FULL_TRACKED` PR-First delivery workflow. It ensures that pull requests meet code quality, security, and verification standards without blocking development on non-existent PR templates or premature Issue closures.

## Target Branch Rules & Issue Linking

| Target Branch | Purpose | Required Issue Link | Authority & Outcome |
|---|---|---|---|
| `dev` | Integration of feature work items | `Refs #N` | Codex technical review (`APPROVED_FOR_DEV_MERGE`, `CHANGES_NEEDED`, `BLOCKED`). Does **not** require `Closes #N`. |
| `main` | Production release / promotion | `Closes #N` (when closing completed issues) | Solely controlled, initiated, and merged by the Product Owner (User). Triggers CD deployment. |

1. **Feature PRs targeting `dev`:**
   - Feature branches follow the project naming convention: `codex/<issue-number>-<short-kebab-name>` branched from `origin/dev`.
   - The PR description must link the relevant Issue using `Refs #N` (or `Refs #N, #M`).
   - Reviewers must **not** demand `Closes #N` on PRs targeting `dev`, as Issues remain open across the delivery lifecycle until verified and merged into `main` by the Product Owner.

2. **Promotion PRs targeting `main`:**
   - Initiated and merged exclusively by the Product Owner.
   - Uses `Closes #N` upon final acceptance to close completed GitHub Issues.

## PR Template Verification Gate

1. **Conditional Verification:** Reviewers must enforce PR template conformance **only when** a tracked PR template exists in the repository (e.g., at `.github/PULL_REQUEST_TEMPLATE.md` or `.github/pull_request_template.md`).
2. **Missing Template Handling:** If no tracked PR template exists in the repository, the reviewer must note `PR template: NOT PRESENT IN REPOSITORY` as a verification limit. This is **never** a blocking defect and must **never** trigger `CHANGES_NEEDED`.
3. **Minimum Required PR Content:** In the absence of a template, the PR description must provide:
   - Concise, secret-safe summary of changes;
   - Reference to the active Issue (`Refs #N` for `dev` PRs);
   - Verification commands and results (or link to green GitHub Actions CI run).

## Technical Review Outcomes for Dev PRs

Codex / Technical Reviewer evaluates the exact GitHub PR HEAD commit and produces exactly one outcome:

```text
APPROVED_FOR_DEV_MERGE  The reviewed PR HEAD commit satisfies the Issue scope, passes CI, contains no secrets, and meets quality/security criteria. Eligible to merge into dev.
CHANGES_NEEDED          Bounded remediation required for missing scope, broken contracts, security defects, or failing tests.
BLOCKED                 Cannot proceed safely without an external PO decision, permission, environment, or dependency.
```

> **Reviewer Role Boundary:** The technical reviewer evaluates code and evidence. Codex **never** pushes commits, merges pull requests, or triggers deployments. Merging into `dev` is performed upon green CI and `APPROVED_FOR_DEV_MERGE`.

## Safety and Quality Safeguards

1. **Zero Raw Secrets:** Inspect diffs, commit messages, PR descriptions, and comments for leaked credentials, private keys, or API tokens. Secrets must never be committed.
2. **Exact PR HEAD Review:** Review must be performed strictly on the exact commit SHA of the current PR HEAD. Pushing any new commit invalidates previous reviews and requires fresh review of the new SHA.
3. **CI Gate Prerequisite:** GitHub Actions CI (`.github/workflows/ci.yml`) must report green (`PASS`) on the PR HEAD before approval.
4. **Lean Engineering (Ponytail):** Ensure no speculative abstractions, over-engineering, or unnecessary dependencies are introduced.
5. **Security Baseline:** Ensure adherence to OWASP ASVS/AISVS and zero regression in access control and input validation.
