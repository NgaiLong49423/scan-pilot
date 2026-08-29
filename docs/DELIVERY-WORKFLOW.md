> **Document:** Scan Pilot Delivery Workflow
> **File:** `docs/DELIVERY-WORKFLOW.md`
> **Version:** v2.2.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-30
> **Status:** Active

# Scan Pilot Delivery Workflow

## Purpose

This document defines how accepted requirements become GitHub work items and how agents move an approved Issue through implementation, verification, Product Owner review, and closure.

## Agent Delivery Governance

When the `agent-delivery-governance` skill is active for this project, use this Nested Coordination Model (Mô hình Phối hợp Lồng nhau):

| Role | Responsibility |
|---|---|
| Agent 4 — Delivery Gatekeeper / Coordinator | agent coordinator; named in Issue before `BUILD`; names Agent 1, 2, 3 in its execution plan; verifies scoped code, QA, and AppSec evidence on the PR branch; ensures green CI on the PR targeting `dev`; outputs `READY_FOR_CODEX_REVIEW` |
| Agent 1 — Coder | primary implementer (named by Agent 4); creates scoped branch from `origin/dev`, writes scoped code/tests, commits, pushes to origin, and opens a Pull Request targeting `dev` with `Refs #N` |
| Agent 2 — QA Reviewer | independent reviewer (named by Agent 4); audits the PR diff and automated test evidence (`APPROVED` / `REQUEST_CHANGES` in `.agent-work/qa-reviews/qa-<issue>.md`) |
| Agent 3 — AppSec Auditor | independent reviewer (named by Agent 4); audits the same PR diff and security constraints (`APPROVED` / `BLOCKED` in `.agent-work/security-audits/sec-<issue>.md`) |
| Codex — Technical Manager / Tech Lead | independent reviewer outside Agent 4; reviews the exact PR HEAD commit on GitHub, performs RCA for workflow failures, and outputs `APPROVED_FOR_DEV_MERGE`, `CHANGES_NEEDED`, or `BLOCKED` |
| Product Owner (User) | product scope, UI/UX, cost, permissions, sole authority to merge `dev` into `main` (triggering production CD), and final Issue closure |

Before implementation (`BUILD`) begins, the work item's implementation brief / contract must explicitly name Agent 4 (Delivery Gatekeeper / Coordinator). In Agent 4's execution plan, Agent 4 must explicitly name Agent 1 (Coder), Agent 2 (independent QA Reviewer), and Agent 3 (independent AppSec Auditor). Codex is recorded separately as Technical Lead / Technical Manager (not Agent 4, and not a subagent of Agent 4).

The `FULL_TRACKED` PR-First delivery workflow is active. Every Git-tracked feature implementation uses Issue → feature branch from `origin/dev` → commit and push → Pull Request targeting `dev` → automated CI checks (`ci.yml`) → multi-agent review → Codex Tech Lead review (`APPROVED_FOR_DEV_MERGE`) → merge into `dev` → Product Owner promotion from `dev` to `main`.

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

- Keep `main` and `dev` stable.
- The repository uses a PR-First delivery model:
  1. `origin/dev` is the shared integration branch branched from `origin/main`.
  2. Feature branches use the convention `codex/<issue-number>-<short-kebab-name>` and are branched from `origin/dev`.
  3. Feature branches open Pull Requests targeting `dev`.
  4. GitHub Actions CI (`.github/workflows/ci.yml`) runs automated checks on PRs targeting `dev` and updates to `dev`.
  5. Codex reviews the exact PR HEAD commit targeting `dev`.
  6. Upon approval and green CI, the PR is merged into `dev`.
  7. Only the Product Owner has the authority to open a Pull Request and merge `dev` into `main`.
  8. Merging `dev` into `main` triggers Continuous Deployment (`.github/workflows/deploy-cloud-run.yml`) to Google Cloud Run.
- Do not combine unrelated Issues merely to reduce branch or commit count.
- Reference the active Issue in progress reports and pull requests using `Refs #N` (or `Closes #N` when authorized).

## Handoff Channel Contract

| Channel | Required content |
|---|---|
| Issue | requirements, acceptance criteria, scope/exclusions, source links, and Product Owner decisions |
| `.agent-work/reports/handoff-<issue>.md` | Coder handoff: feature branch, PR URL, exact HEAD commit, changed files, test evidence, verification, limits |
| `.agent-work/qa-reviews/qa-<issue>.md` | QA review of the PR diff and test suite (`APPROVED` / `REQUEST_CHANGES`) |
| `.agent-work/security-audits/sec-<issue>.md` | AppSec audit of the PR diff and security constraints (`APPROVED` / `BLOCKED`) |
| `.agent-work/acceptance/acceptance-<issue>.md` | Agent 4 PR and CI verification (`READY_FOR_CODEX_REVIEW`) |
| `.agent-work/acceptance/tech-lead-<issue>.md` | Codex technical review (`APPROVED_FOR_DEV_MERGE` / `CHANGES_NEEDED` / `BLOCKED`) |
| `.agent-work/diagnostics/rca-<issue>.md` | Codex root-cause analysis and prevention/re-dispatch plan when a workflow or contract failure is found |
| Branch | Coder implementation on `codex/<issue-number>-<short-kebab-name>` branched from `origin/dev` |
| Pull request | created targeting `dev` with `Refs #N`, exact approved HEAD commit, compact secret-safe summary, and CI status link |
| Project #13 | status, priority, dates, workstream, and progress only |

Do not duplicate agent discussion on GitHub. The PR summary, exact HEAD commit, green CI run, and Codex review comments establish the public handoff evidence.

## Review and Completion Gate

The review workflow follows the Nested Coordination Model (Mô hình Phối hợp Lồng nhau) under the PR-First delivery model:

```text
Agent 4 (Coordinator / Delivery Gatekeeper)
├── Agent 1 (Coder)
├── Agent 2 (QA Reviewer)
└── Agent 3 (AppSec Auditor)
     ↓
Codex (Tech Lead / Technical Manager)
     ↓ [APPROVED_FOR_DEV_MERGE]
Merge to dev
     ↓
Product Owner (Promotion: dev -> main)
```

1. **Pre-BUILD Assignment & Execution Plan:** Issue contract names Agent 4 (Delivery Gatekeeper / Coordinator). Agent 4's execution plan explicitly names Agent 1 (Coder), Agent 2 (QA Reviewer), and Agent 3 (AppSec Auditor) before `BUILD` starts.
2. **Gate 1 — Feature Implementation & Scoped Commit (Agent 1):** Creates feature branch from `origin/dev`, writes scoped code/tests, commits, pushes to origin, and opens a Pull Request targeting `dev` with `Refs #N`.
3. **Gate 2 — Automated CI Verification:** GitHub Actions CI (`.github/workflows/ci.yml`) runs automated checks on the PR targeting `dev`. A green CI run is a mandatory prerequisite for technical review.
4. **Gate 3 — Independent QA & AppSec Review (Agents 2 & 3):** Agent 2 audits code quality/tests (`APPROVED` / `REQUEST_CHANGES`) and Agent 3 audits security constraints/secrets (`APPROVED` / `BLOCKED`).
5. **Gate 4 — Delivery Coordination (Agent 4):** Confirms that CI is green, changed files match the authorized scope, and QA/AppSec evidence agrees; produces `READY_FOR_CODEX_REVIEW`.
6. **Gate 5 — Technical Manager Review (Codex):** Independently reviews the exact PR HEAD commit on GitHub. On contract, scope, or evidence failure, performs RCA and returns `CHANGES_NEEDED`. When all acceptance criteria and quality gates are satisfied, outputs `APPROVED_FOR_DEV_MERGE`.
7. **Gate 6 — Integration Merge (`dev`):** The approved PR is merged into `dev`. Merging to `dev` does NOT trigger production deployment.
8. **Gate 7 — Production Promotion & Final Acceptance (Product Owner):** Only the Product Owner has the authority to open a Pull Request and merge `dev` into `main`. Merging `dev` into `main` triggers Continuous Deployment (`.github/workflows/deploy-cloud-run.yml`) to Google Cloud Run. The Product Owner records final acceptance and closes completed Issues with `Closes #N`.

Remediation loop: If QA returns `REQUEST_CHANGES`, AppSec returns `BLOCKED`, or Codex returns `CHANGES_NEEDED`/`BLOCKED`, Agent 4 routes the remediation card to Coder. Coder applies bounded fixes, pushes a new HEAD commit to the PR branch, and the review cycle re-runs on the exact new HEAD commit.

## Target, Proof, and Remediation Controls

### Pre-BUILD Target Manifest

Before Agent 1 edits code, Agent 4 records this compact manifest in `.agent-work/`:

```text
Base ref and resolved commit: origin/dev (<commit SHA>)
Target branch: dev
Review mode: PR HEAD commit on GitHub with automated CI
Production artifact: <for example, frontend/src or backend/src>
Allowed paths: <paths>
Prohibited paths: <frozen prototype/evidence and unrelated paths>
Manual deployment handoff: <when applicable>
```

The manifest is checked before BUILD and again before QA/AppSec review. For Scan Pilot frontend work, `frontend/src/**` is the production artifact that the Product Owner manually transfers to Google AI Studio. Frozen AI Studio evidence must not be changed unless the Issue explicitly authorizes it.

### High-Risk Acceptance Test Matrix

For repository identity/isolation, persistence, external I/O, security, fail-closed behavior, and frontend-backend contracts, Agent 4 adds a focused proof before BUILD:

| Acceptance criterion | Contract layer | Focused proof | Expected failure and success | Dependency isolation |
|---|---|---|---|---|
| Requested scan branch remains exact | pipeline and evidence persistence | pipeline regression test | missing branch fails with no evidence; configured branch succeeds | mocked ZIP transport; no live GitHub request |

The matrix selects the smallest useful proof. A live-service check is labelled integration evidence, never deterministic automated evidence.

### Risk-Based Verification

Complete a coherent code slice, run its narrow relevant check, and repair the cause before expanding verification. Run the full applicable backend/frontend verification once before PR push/handoff to ensure clean CI execution. Do not rerun an unchanged passing suite unless source changed, a prior check failed, or the remediation explicitly requires it. Bruno is used only for affected REST/integration flows and never replaces compilation, automated tests, lint, or production build evidence.

### Directed Remediation Contract

Every Codex `CHANGES_NEEDED` finding must be a bounded remediation card, not an open-ended request to rediscover a defect:

| Field | Required content |
|---|---|
| Finding ID and severity | Stable identifier and impact |
| Violated contract | Exact acceptance criterion, brief requirement, or guardrail |
| Evidence | Observed behavior and exact file, symbol, line, test, or request/response evidence when available |
| Cause | Confirmed root cause, or explicitly labelled hypothesis |
| Required change | Concrete bounded behavior/file/symbol target; never only “investigate” |
| Required proof | Focused regression test, request, or direct assertion |
| Non-goals | Explicitly prohibited refactor, API, schema, UI, or external-action expansion |

**Return flow:** Codex → Agent 4 → Agent 1. Agent 1 implements only the stated remediation and reports a genuine conflict or missing external fact. Agent 2 and Agent 3 review the new frozen diff for the remediation and affected regression surface; Agent 4 returns one consolidated evidence package to Codex. Unchanged passing broad evidence is cited, not rerun, unless the remediation card states the reason.

## Change-Proportional Checklists

Checklists for QA (Agent 2) and AppSec (Agent 3) must scale to the specific change category:

### 1. Frontend / UI Changes
- [ ] Clean code & Ponytail principles applied (no over-engineering, YAGNI).
- [ ] UI visual polish, responsive behavior, and accessibility (WCAG AA contrast).
- [ ] Loading, error, and empty states handled properly.
- [ ] `npm run lint` and `npm run build` pass with zero type errors.
- *(Note: OAuth/cookie checklists are NOT enforced for pure CSS or layout changes).*

### 2. Backend / REST API Changes
- [ ] Modular monolith architecture preserved; API contract intact.
- [ ] Defensive programming, null checks, and explicit exception handling.
- [ ] Narrow affected tests pass during implementation; the full applicable Maven suite runs once on the frozen handoff diff.
- [ ] Performance and resource utilization within boundaries.

### 3. Auth / GitHub Integration / Cookie / Session Changes
- [ ] OAuth 2.0 PKCE flow and CSRF state validation verified.
- [ ] Session cookies set with `SameSite=Strict/Lax`, `Secure`, `HttpOnly`.
- [ ] GitHub token scope minimal and securely handled.
- [ ] Zero secret logging verified across all log paths and error outputs.

### 4. Database / Migration Changes
- [ ] Flyway migration script strictly versioned and deterministic.
- [ ] Foreign keys, indexes, and constraints (`UNIQUE(repository_id, fingerprint)`) enforced.
- [ ] Backward compatibility and rollback safety preserved.
- [ ] JPA entities avoid `@Data` and maintain clear state mapping.

### 5. CI/CD / Workflow / Infrastructure Changes
- [ ] Least-privilege `GITHUB_TOKEN` permissions explicitly declared.
- [ ] Third-party Actions pinned to commit SHAs or verified tags.
- [ ] Cloud budget boundaries (USD 250 planning envelope) strictly respected.
- [ ] Secrets injected via secure secret manager / GitHub secrets; zero plaintext credentials.

## Git and External-Action Boundary

Issue assignment and routine status updates do not authorize Git or deployment actions. Commit, push, pull request creation, merge, history rewrite, production deployment, paid-resource creation, credential changes, and destructive actions retain their separate approval requirements in `AGENTS.md`.

## Delivery Automation Policy

Delivery automation is staged so repeatable checks reduce manual review effort without allowing an unreviewed change to reach a public environment.

### Current State

GitHub Actions Continuous Integration (`.github/workflows/ci.yml`) is active for all pull requests targeting `dev` and `main`, as well as direct pushes to `dev` and `main`. Continuous Deployment (`.github/workflows/deploy-cloud-run.yml`) is configured for pushes to `main` only.

### Continuous Integration

The CI workflow executes automated checks across both production workspaces:
- **Frontend (Node.js 20):** `npm ci`, `npm run lint`, `npm run test`, `npm run build`.
- **Backend (Java 21 / Maven):** `mvn -B clean verify`, container smoke test (`git --version`, `gitleaks version`).

A green CI run is a mandatory prerequisite before Codex technical review and merge into `dev`.

### Continuous Delivery

CD is triggered automatically only when the Product Owner merges `dev` into `main`. Merging feature PRs into `dev` never triggers CD or modifies the production Cloud Run deployment.

### Review Boundary

Automated checks provide repeatable build and test evidence only. Codex still reviews scope, security, architecture, documentation, known limitations, and the Issue acceptance criteria. The Product Owner still controls final acceptance, merge to `main`, and public deployment.

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
