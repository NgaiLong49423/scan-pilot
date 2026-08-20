> **Document:** Scan Pilot Delivery Workflow
> **File:** `docs/DELIVERY-WORKFLOW.md`
> **Version:** v1.3.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot Delivery Workflow

## Purpose

This document defines how accepted requirements become GitHub work items and how agents move an approved Issue through implementation, verification, Product Owner review, and closure.

## Agent Delivery Governance

When the `agent-delivery-governance` skill is active for this project, use this Nested Coordination Model (Mô hình Phối hợp Lồng nhau):

| Role | Responsibility |
|---|---|
| Agent 4 — Delivery Gatekeeper / Coordinator | agent coordinator & gatekeeper; named in Issue before `BUILD`; names Agent 1, 2, 3 in execution plan; verifies Coder + QA + AppSec handoffs on exact same reviewed head SHA; outputs summary report (`READY_FOR_TECH_LEAD_REVIEW` in `.agent-work/acceptance/acceptance-<issue>.md`) and reports to Codex |
| Agent 1 — Coder | primary implementer (named by Agent 4); writes scoped code, unit/integration tests, local handoff report (`.agent-work/reports/handoff-<issue>.md`) |
| Agent 2 — QA Reviewer | independent code quality reviewer (named by Agent 4); audits logic, Ponytail standards, unit/integration tests, UI (`APPROVED` / `REQUEST_CHANGES` in `.agent-work/qa-reviews/qa-<issue>.md`) |
| Agent 3 — AppSec Auditor | independent security auditor (named by Agent 4); audits security controls, OAuth, cookies, secrets, OWASP compliance (`APPROVED` / `BLOCKED` in `.agent-work/security-audits/sec-<issue>.md`) |
| Codex — Technical Manager / Tech Lead | independent technical reviewer & architectural sign-off (separate from Agent 4); reviews quality/security/scope evidence and outputs `APPROVED_FOR_PO_ACCEPTANCE` |
| Product Owner (User) | product scope, UI/UX, cost, permissions, final acceptance decision (`PO ACCEPTED`), and sole merge authority |

Before implementation (`BUILD`) begins, the work item's implementation brief / contract must explicitly name Agent 4 (Delivery Gatekeeper / Coordinator). In Agent 4's execution plan, Agent 4 must explicitly name Agent 1 (Coder), Agent 2 (independent QA Reviewer), and Agent 3 (independent AppSec Auditor). Codex is recorded separately as Technical Lead / Technical Manager (not Agent 4, and not a subagent of Agent 4).

The `FULL_TRACKED` workflow is active. Every Git-tracked implementation requires the full Issue → branch → PR → Multi-Gate Review → Product Owner decision path.

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
| `.agent-work/reports/handoff-<issue>.md` | Coder handoff report: changed files, test evidence, verification, limits |
| `.agent-work/qa-reviews/qa-<issue>.md` | QA review report: Ponytail compliance, logic, test coverage (`APPROVED` / `REQUEST_CHANGES`) |
| `.agent-work/security-audits/sec-<issue>.md` | AppSec audit report: security controls, secrets, cookies, OAuth (`APPROVED` / `BLOCKED`) |
| `.agent-work/acceptance/acceptance-<issue>.md` | Delivery Gatekeeper report: 3-gate verification summary & recommendation (`READY_FOR_TECH_LEAD_REVIEW`) |
| Branch | Coder implementation according to the project branch convention |
| Pull request | exact diff, `Refs #N`, compact secret-safe summary with head SHA and gate status references (`QA`, `AppSec`, `Gatekeeper`, `Tech Lead`) |
| Project #13 | status, priority, dates, workstream, and progress only |

Do not duplicate agent discussion on GitHub. A shared local file is never sole approval evidence: the PR summary and reviewed head SHA must establish the minimum public handoff.

## Review and Completion Gate

Before moving an Issue to `Review`, the Coder must provide the required PR, compact handoff summary, and local handoff report `.agent-work/reports/handoff-<issue>.md`. Coder MUST NOT self-approve as QA or AppSec.

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
2. **Gate 1 — Coder Implementation & Handoff (Agent 1):** Implements code and tests, produces `.agent-work/reports/handoff-<issue>.md`, and submits secret-safe PR with reviewed head SHA. Coder MUST NOT self-approve as QA or AppSec.
3. **Gate 2 — Quality Gate (Agent 2 - QA Reviewer):** Audits code quality, Ponytail standards, logic correctness, exception handling, unit/integration test suite, and UX completeness. Produces strictly `APPROVED` or `REQUEST_CHANGES` in `.agent-work/qa-reviews/qa-<issue>.md`.
4. **Gate 3 — Security Gate (Agent 3 - AppSec Auditor):** Audits task-relevant security controls, OAuth PKCE, cookie security, CORS, and zero secret exposure. Produces strictly `APPROVED` or `BLOCKED` in `.agent-work/security-audits/sec-<issue>.md`.
5. **Gate 4 — Delivery Coordination Gate (Agent 4 - Delivery Gatekeeper / Coordinator):** Verifies that Coder handoff + QA APPROVED + AppSec APPROVED exist for the exact same reviewed head SHA. Produces `READY_FOR_TECH_LEAD_REVIEW` in `.agent-work/acceptance/acceptance-<issue>.md` and reports to Codex.
6. **Gate 5 — Technical Lead Gate (Codex - Technical Manager / Tech Lead):** Conducts overall technical, architectural, and security review. Produces `APPROVED_FOR_PO_ACCEPTANCE`.
7. **Gate 6 — Product Owner Gate (Product Owner - User):** Evaluates product value and final acceptance. Records `PO ACCEPTED` (or `PO RETURNED`) and grants explicit merge authority.

Remediation loop: If QA returns `REQUEST_CHANGES`, AppSec returns `BLOCKED`, or Tech Lead rejects the submission, the Issue returns to `In Progress` (Coder). After Coder remediation, new QA, AppSec, Gatekeeper, and Tech Lead reviews are required for the new head SHA.

Product Owner acceptance: `Review` remains the active state while Gatekeeper review, Tech Lead sign-off, and Product Owner acceptance are pending. The Product Owner records `PO ACCEPTED` or `PO RETURNED` in the Issue and provides explicit merge authorization. Codex Tech Lead recommendations do NOT replace Product Owner acceptance or merge permission. Only explicit Product Owner merge authority permits PR merge and Issue closure.

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
- [ ] Unit and integration tests pass (`mvn test`).
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

No GitHub Actions CI/CD workflow is configured yet. Until a CI workflow is implemented and verified, the implementer must provide applicable local test/build evidence and Codex must review it manually.

### Continuous Integration First

A separately authorized CI Issue must introduce the smallest relevant automated checks for both production workspaces. The initial target is frontend dependency installation, lint, and production build plus backend Java 21 Maven verification. It should run for pull requests targeting `main` and for updates to `main`.

Once that workflow has produced reliable green evidence on real pull requests, a later authorized repository-settings task may make its named checks required for `main`. Do not make a check required before it has run successfully, because a missing or skipped required check can block all pull requests.

### Continuous Delivery Is Deferred

CI success is not deployment authorization. Cloud Run deployment, credentials, database migration, release validation, cost controls, and public availability remain a separate deployment/release Issue. A production deploy requires explicit Product Owner authorization for that release, even if all CI checks pass.

### Review Boundary

Automated checks provide repeatable build and test evidence only. Codex still reviews scope, security, architecture, documentation, known limitations, and the Issue acceptance criteria. The Product Owner still controls final acceptance, merge, and public deployment.

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
