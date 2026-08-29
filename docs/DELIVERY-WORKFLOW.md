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
| Agent 4 — Delivery Gatekeeper / Coordinator | agent coordinator; named in Issue before `BUILD`; names Agent 1, 2, 3 in its execution plan; freezes the local review target; verifies Coder + QA + AppSec reports against the unchanged local diff; outputs `READY_FOR_TECH_LEAD_REVIEW` and reports to Codex |
| Agent 1 — Coder | primary implementer (named by Agent 4); writes scoped code/tests and a local handoff report; does not commit, push, or create a PR before Product Owner authorization |
| Agent 2 — QA Reviewer | independent reviewer (named by Agent 4); audits the frozen local diff (`APPROVED` / `REQUEST_CHANGES` in `.agent-work/qa-reviews/qa-<issue>.md`) |
| Agent 3 — AppSec Auditor | independent reviewer (named by Agent 4); audits the same frozen local diff (`APPROVED` / `BLOCKED` in `.agent-work/security-audits/sec-<issue>.md`) |
| Codex — Technical Manager / Tech Lead | independent reviewer outside Agent 4; reviews the local diff/package, performs RCA for workflow failures, and outputs `APPROVED_FOR_PO_ACCEPTANCE`, `CHANGES_NEEDED`, or `BLOCKED` |
| Product Owner (User) | product scope, UI/UX, cost, permissions, final acceptance decision (`PO ACCEPTED`), and sole merge authority |

Before implementation (`BUILD`) begins, the work item's implementation brief / contract must explicitly name Agent 4 (Delivery Gatekeeper / Coordinator). In Agent 4's execution plan, Agent 4 must explicitly name Agent 1 (Coder), Agent 2 (independent QA Reviewer), and Agent 3 (independent AppSec Auditor). Codex is recorded separately as Technical Lead / Technical Manager (not Agent 4, and not a subagent of Agent 4).

The `FULL_TRACKED` workflow is active. Every Git-tracked implementation uses Issue → local branch/worktree → multi-agent local review → Codex Tech Lead review → Product Owner decision → separately authorized commit → separately authorized push/PR/merge.

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
| `.agent-work/reports/handoff-<issue>.md` | Coder handoff: `uncommitted local worktree`, base commit for context, changed files, test evidence, verification, limits |
| `.agent-work/qa-reviews/qa-<issue>.md` | QA review of the frozen local diff (`APPROVED` / `REQUEST_CHANGES`) |
| `.agent-work/security-audits/sec-<issue>.md` | AppSec audit of the same frozen local diff (`APPROVED` / `BLOCKED`) |
| `.agent-work/acceptance/acceptance-<issue>.md` | Agent 4 local-diff consistency verification (`READY_FOR_TECH_LEAD_REVIEW`) |
| `.agent-work/acceptance/tech-lead-<issue>.md` | Codex technical review (`APPROVED_FOR_PO_ACCEPTANCE` / `CHANGES_NEEDED` / `BLOCKED`) |
| `.agent-work/diagnostics/rca-<issue>.md` | Codex root-cause analysis and prevention/re-dispatch plan when a workflow or contract failure is found |
| Branch | Coder implementation according to the project branch convention |
| Pull request | created only after separately authorized commit and push; exact approved commit, `Refs #N`, compact secret-safe summary and gate status references |
| Project #13 | status, priority, dates, workstream, and progress only |

Do not duplicate agent discussion on GitHub. Before a commit exists, local reports establish the review evidence; after an authorized PR exists, its summary and exact approved head commit establish the minimum public handoff.

## Review and Completion Gate

Before the Product Owner decision, the Coder provides a local handoff report and Agent 4 freezes the local diff. Coder MUST NOT self-approve as QA/AppSec or commit, push, or open a PR without Product Owner authorization.

The review workflow follows the Nested Coordination Model (Mô hình Phối hợp Lồng nhau):

```text
Agent 4 (Coordinator / Delivery Gatekeeper)
├── Agent 1 (Coder)
├── Agent 2 (QA Reviewer)
└── Agent 3 (AppSec Auditor)
     ↓
Codex (Tech Lead)
     ↓
Product Owner (User)
```

1. **Pre-BUILD Assignment & Execution Plan:** Issue contract names Agent 4 (Delivery Gatekeeper / Coordinator). Agent 4's execution plan explicitly names Agent 1 (Coder), Agent 2 (QA Reviewer), and Agent 3 (AppSec Auditor) before `BUILD` starts.
2. **Gate 1 — Local Coder Handoff (Agent 1):** Implements code/tests locally and hands off an uncommitted worktree diff. Agent 4 records the worktree path, base commit for context only, and changed-file list, then freezes edits.
3. **Gate 2 — Quality Gate (Agent 2):** Audits that frozen local diff and produces strictly `APPROVED` or `REQUEST_CHANGES`.
4. **Gate 3 — Security Gate (Agent 3):** Audits the same frozen local diff and produces strictly `APPROVED` or `BLOCKED`.
5. **Gate 4 — Delivery Coordination (Agent 4):** Confirms the local review target did not change and that Coder/QA/AppSec evidence agrees; produces `READY_FOR_TECH_LEAD_REVIEW`.
6. **Gate 5 — Technical Lead (Codex):** Independently reviews the same local diff and package. On a workflow, contract, or evidence failure, performs RCA and may correct accepted delivery artifacts. The RCA is the corrective-change handoff; Agent 4 then obtains fresh independent QA/AppSec evidence on that local diff before re-dispatching Codex. Codex does not self-approve its own correction. Produces `APPROVED_FOR_PO_ACCEPTANCE`, `CHANGES_NEEDED`, or `BLOCKED`.
7. **Gate 6 — Product Owner:** Records `PO ACCEPTED` or `PO RETURNED`. `PO ACCEPTED` may explicitly authorize the local commit only; push, PR creation, merge, Issue closure, and deployment remain separate decisions.

Remediation loop: If QA returns `REQUEST_CHANGES`, AppSec returns `BLOCKED`, or Codex returns `CHANGES_NEEDED`/`BLOCKED`, Agent 4 returns work to Coder. Any changed local diff invalidates prior reviews and requires fresh QA, AppSec, Gatekeeper, and Tech Lead evidence. If the Product Owner does not authorize commit, the accepted local diff remains uncommitted.

Product Owner acceptance: `Review` remains the active state while Gatekeeper review, Tech Lead sign-off, and Product Owner acceptance are pending. The Product Owner records `PO ACCEPTED` or `PO RETURNED` in the Issue and provides explicit merge authorization. Codex Tech Lead recommendations do NOT replace Product Owner acceptance or merge permission. Only explicit Product Owner merge authority permits PR merge and Issue closure.

## Target, Proof, and Remediation Controls

### Pre-BUILD Target Manifest

Before Agent 1 edits code, Agent 4 records this compact manifest in `.agent-work/`:

```text
Base ref and resolved commit: <origin/main SHA>
Local worktree: <path retained locally only>
Review mode: frozen local diff before commit
Production artifact: <for example, frontend/src>
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

Complete a coherent code slice, run its narrow relevant check, and repair the cause before expanding verification. Run the full applicable backend/frontend verification once on the frozen local diff before handoff. Do not rerun an unchanged passing suite unless source changed, a prior check failed, or the remediation explicitly requires it. Bruno is used only for affected REST/integration flows and never replaces compilation, automated tests, lint, or production build evidence.

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
