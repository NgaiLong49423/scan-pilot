> **Document:** Scan Pilot Agent Instructions
> **File:** `AGENTS.md`
> **Version:** v3.0.0
> **Created:** 2026-08-11
> **Last Updated:** 2026-08-20
> **Status:** Active

# Agent Instructions

## Project

This repository contains **Scan Pilot**, a continuous multi-project health monitoring platform for AI-generated and AI-assisted software projects.

The former working title was **VibeGuard**. Use **Scan Pilot** in all new product documentation, rule identifiers, code, and user-facing text.

## Required Reading Order

Before proposing work, read:

1. `docs/PROJECT-CONTEXT.md`
2. `docs/DECISIONS.md`
3. `docs/CURRENT-STATUS.md`
4. `docs/DELIVERY-WORKFLOW.md` when a task involves GitHub Issue planning, assignment, execution, review, acceptance, or closure
5. `docs/CLOUD-BUDGET.md` when a task may affect architecture, deployment, external services, resource sizing, benchmarking cost, Gemini usage, or public operation
6. relevant submission-context documents under `docs/research/submission/` when a task may affect product direction, MVP scope, Google technology integration, deployment, demo, or submission material
7. the canonical specification relevant to the task
8. relevant files under `docs/research/`

Do not ask the user to repeat information already recorded in these documents.

## Source-of-Truth Order

If information conflicts, use this precedence:

1. explicit latest user instruction;
2. `docs/DECISIONS.md`;
3. canonical specifications under `docs/`;
4. `docs/CURRENT-STATUS.md`;
5. research notes;
6. implementation code.

Report meaningful conflicts instead of silently resolving them.

## Current Phase

The project is in **implementation**, following Product Owner acceptance of the Eligibility Spike `CONDITIONAL GO` recommendation on 2026-08-16. Documentation migration from the original template is complete.

Implementation must preserve the accepted MVP scope and four conditions recorded in `DEC-054`: Completion Form verification before final submission, Cloud Billing alert verification before ongoing public deployment, production authentication/token/private-source lifecycle work before private-repository scanning, and explicit Issue-level authorization for individual work items. Do not treat this phase change as authorization to bypass Issue delivery, commit/push, deploy, change UI/UX, or broaden scope.

Workflow:

```text
Research
→ Discussion with reasons and trade-offs
→ User acceptance
→ Accepted Decision
→ Specification
→ GitHub Issues
→ Implementation
```

Research candidates are not product requirements until the user accepts them and they are recorded in `docs/DECISIONS.md`.

Submission-context documents describe external event constraints and evaluation signals. They must inform relevant solution choices, but do not override a user instruction or become accepted product requirements without an explicit decision.

## Comparative Research Before Decisions

- Before recommending a material product, security, architecture, workflow, scanning, or UX decision, investigate how relevant mature products, open-source tools, standards bodies, or official platform guidance address the same problem when such references exist.
- Prefer official documentation, published specifications, source repositories, and other primary sources. Use secondary commentary only to discover or contextualize primary evidence.
- Explain to the user, in accessible language, which sources were reviewed, how each source handles the problem, what Scan Pilot could learn, the trade-offs, and what cannot be verified or transferred directly.
- Keep `External Standard`, `Product Benchmark`, `Research Inference`, and `Accepted Scan Pilot Decision` distinct. A competitor behavior or popular practice is evidence for discussion, not an automatically accepted requirement.
- Record material comparative research under `docs/research/` with source owner, title, official link, access date, observed behavior, applicable lesson, rejected or non-transferable details, and verification limits.
- Attribute ideas and behavioral patterns. Do not copy proprietary code, private implementation details, protected text, branding, or UI assets. For open-source code, review its license before any implementation reuse; attribution in research documentation does not by itself grant reuse permission.
- If adequate sources cannot be found or accessed, state that limitation instead of presenting memory or assumption as professional consensus.

## Changelog Traceability

- `CHANGELOG.md` is a chronological history, ordered newest first by `YYYY-MM-DD`; do not keep one growing catch-all `Unreleased` list.
- Each entry must identify its status as `Committed` with the verified short Git hash or `Working tree` when it is not committed yet.
- Record the coherent scope, concise human-readable changes, and affected files or file groups. Do not invent historical detail that is not supported by Git history or the current diff.
- When a working-tree checkpoint is committed, replace its `Working tree` label with the verified short hash instead of duplicating the same change under a new entry.

## Communication

- Communicate primarily in Vietnamese.
- Explain important English technical terms in Vietnamese the first time they appear.
- The user is new to application security. Explain one decision at a time using a concrete example.
- Every recommendation or decision must include the reason, expected benefit, trade-off, and verification limit.
- Do not expect the user to read an entire security standard before making progress; provide official links as supporting material.

## Decision Altitude

- Treat the user as the product owner with some technical background. Ask for decisions about user-visible behavior, product value, scope, deadline, cost, privacy, permissions, material risk, submission evidence, and UI/UX.
- The agent owns routine technical decomposition and implementation choices within accepted product, architecture, security, budget, and phase boundaries. Examples include OAuth callback handling, session mechanisms, CORS, internal API shape, module structure, database mapping, retry policy, and test strategy.
- Report important technical decisions with their reason, benefit, trade-off, and verification limit, but do not force the user to choose among low-level mechanisms that do not change the product contract.
- Return a technical choice to the user when it materially changes user behavior, accepted scope, cost, privacy, permissions, vendor lock-in, deadline risk, external state, or an irreversible action.
- UI/UX remains user-controlled. Technical work may identify constraints or accessibility and security requirements, but it must not silently treat a generated prototype design as approved production UX.

## Security Rule Policy

- Prefer recognized, versioned sources such as OWASP Top 10, OWASP ASVS, OWASP AISVS, CWE, and official OWASP guidance.
- Keep `Standard Requirement`, `Scan Pilot Rule`, and `Finding` separate.
- Record automability as `FULL`, `PARTIAL`, or `MANUAL`.
- Record detection method as `STATIC`, `EXECUTION`, `AI`, or `HYBRID`.
- Never invent security requirements or claim certainty from weak evidence.
- Never claim standards compliance beyond verified coverage.
- A rule intended for the MVP must run against a real repository and produce evidence; mock-only rules are not acceptable for the main demo flow.
- Never expose a detected secret in logs, findings, prompts, screenshots, or reports.

## Accepted Architecture Direction

- React + TypeScript + Vite frontend
- Spring Boot 3 + Java 21 REST API
- Apache Maven for Java build and dependency management
- modular monolith backend
- PostgreSQL
- GitHub App integration
- Gemini as the first AI provider behind a provider abstraction
- asynchronous isolated scan workers
- Google Cloud deployment direction

Exact infrastructure choices listed as open in `docs/DECISIONS.md` remain unresolved.

## Cloud Cost Boundary

- Treat `docs/CLOUD-BUDGET.md` as the canonical funding and cost-control source.
- Do not propose an architecture that intentionally exceeds the accepted two-month USD 250 planning envelope without explicit user acceptance.
- Do not treat promotional credit as guaranteed cash or as a reason to add paid services, always-on capacity, or product scope.
- Every material paid-service proposal must include a current estimate, cheaper alternative, cost controls, credit-expiry behavior, and verification limit.

## Installed Agent Skills & Trigger Map

All agent skills are installed in `.agents/skill/`. Agents must consult and apply these canonical skills for their respective domains:

| Skill Name | Canonical Path | Trigger & Usage Scope |
|---|---|---|
| **Lean Code Crafting (Ponytail)** | `.agents/skill/ponytail/SKILL.md` | Mandatory for all code writing, refactoring, designing, and dependency selection. Enforces the 7-rung ladder, YAGNI, and stdlib/native-first approach. |
| **Over-Engineering Review** | `.agents/skill/ponytail-review/SKILL.md` | Mandatory for PR reviews and pre-commit self-checks. Identifies speculative abstractions, single-implementation interfaces, and bloat. |
| **Technical Debt Ledger** | `.agents/skill/ponytail-debt/SKILL.md` | Use when auditing MVP simplifications or harvesting `// ponytail:` comment markers. |
| **Frontend Design Taste & Human UX** | `.agents/skill/design-taste-frontend/SKILL.md` | Mandatory for all React, Tailwind, and UI tasks. Enforces "Security for Humans" (plain-language summaries, before/after fix diffs, secret masking, reassuring empty states, and WCAG AA contrast). |
| **UI/UX Design Audit** | `.agents/skill/ui-design-audit/SKILL.md` | Mandatory for reviewing Frontend PRs, ensuring beginner-friendly usability, accessibility, and visual polish. |
| **Full Output Enforcement** | `.agents/skill/full-output-enforcement/SKILL.md` | Mandatory across all implementation tasks. Strictly bans `// TODO`, `// rest of code`, and truncated outputs. |
| **Platform-Native Cheat-Sheet** | `.agents/docs/platform-native-cheatsheet.md` | Reference guide for Java 21, Spring Boot 3, PostgreSQL, and Web APIs native features. |
| **Agent Delivery Governance** | `.agents/skill/agent-delivery-governance/SKILL.md` | Mandatory for `FULL_TRACKED` delivery workflow, PO ratifications, and Codex PR reviews. |
| **GitHub Issue Delivery** | `.agents/skill/github-issue-delivery/SKILL.md` | Mandatory for executing assigned GitHub Issues. |
| **SRS to GitHub Issues** | `.agents/skill/srs-to-github-issues/SKILL.md` | Use when decomposing approved specifications into actionable GitHub Issues. |
| **Document Metadata Standardizer** | `.agents/skill/document-metadata-standardizer/SKILL.md` | Mandatory when creating, editing, auditing, or standardizing repository Markdown files. |
| **Changelog Automatic** | `.agents/skill/changelog-automatic/SKILL.md` | Use when updating `CHANGELOG.md` with structured working tree entries. |

## Documentation Rules

- Repository Markdown is the canonical development format.
- `.agents/` is the canonical repository directory for agent skills, contracts, and generated agent outputs. Current instructions and templates must use this path consistently.
- Important Markdown documents must use the metadata standard described in `.agents/skill/document-metadata-standardizer/SKILL.md`.
- Use repository-relative links; never use local absolute paths or local file URIs.
- Update `CHANGELOG.md` after major documentation, database, feature, or structural changes.
- Preserve the distinction between `Active`, `Under Review`, `Draft`, `Template`, and `Deprecated` documents.

## Safety and Git

- Do not hard-code or commit credentials, `.env` files, private keys, or production data.
- Do not execute untrusted scanned repository code in the main API process.
- Do not delete data or files, change public contracts, commit, push, merge, or open a pull request without explicit user authorization.
- Follow `CONTRIBUTING.md` and Conventional Commits when Git actions are authorized.

## Git Checkpoint Policy

- Scan Pilot is a solo project. Use a lightweight workflow: keep `main` stable and use one working branch for each coherent large workstream.
- Documentation research and specification may share a branch such as `codex/docs-research-specification`; a large implementation feature should use its own branch such as `codex/secret-scanning`.
- Small documentation corrections may stay on the current working branch. Do not create a new branch or pull request for every minor edit.
- Outside active `FULL_TRACKED` governance, a pull request is optional for a large feature or checkpoint when reviewing the complete diff would be useful. Do not introduce `develop`, `release`, or `hotfix` branches unless a later accepted need justifies them.
- Do not create a commit after each file edit, discussion, or individual accepted decision.
- Group related decisions, specifications, or implementation changes into a coherent checkpoint that can be reviewed and restored as one meaningful unit.
- A checkpoint is ready to propose only when its intended scope is complete, relevant documentation and metadata are synchronized, applicable verification has run, and the diff has been inspected for unrelated files and secrets.
- When a meaningful checkpoint is ready or when an off-device backup would materially reduce risk, proactively ask the user whether it should be committed and, separately, pushed to GitHub.
- The proposal must explain the checkpoint scope, why it is ready, verification performed, known limitations, affected files, and suggested Conventional Commit message.
- A proposal is not authorization. Commit only after explicit user permission to commit. Push only after explicit user permission to push; permission to commit alone does not imply permission to push.
- Do not split one coherent checkpoint into noisy per-file commits, and do not combine unrelated work merely to reduce commit count.
- Important checkpoints should be pushed to GitHub after authorization so the repository has an off-device copy outside the laptop.

## Issue-Driven Delivery

- Use GitHub Issues as executable work contracts and GitHub Project #13 as the operational view for status, priority, dates, workstream, and progress. Canonical repository documents remain the product source of truth.
- Use `.agents/skill/srs-to-github-issues/SKILL.md` to decompose accepted requirements into Issues. Use `.agents/skill/github-issue-delivery/SKILL.md` to execute an authorized Issue.
- A user instruction such as `work on #N` authorizes the agent to inspect and perform only that Issue's accepted scope and to move that item to `In Progress`. It does not authorize creating or closing Issues, committing, pushing, opening or merging pull requests, deploying, or changing product scope or UI/UX.
- Move completed work to `Review` with evidence and known limits. Move it to `Done` and close it only after explicit Product Owner acceptance.
- Follow the complete state, traceability, approval, and automation contract in `docs/DELIVERY-WORKFLOW.md`.

## Agent Delivery Governance

> **Status:** Active (`FULL_TRACKED`)
> **Installed skill:** `.agents/skill/agent-delivery-governance/` v1.0.0
> **Delivery mode:** `FULL_TRACKED` (Integration Check passed on 2026-08-17; nested four-agent local-review-first workflow active on 2026-08-20)
> **Product Owner / final acceptance & merge authority:** User
> **Agent 4 — Delivery Gatekeeper / Coordinator:** agent coordinator & 3-gate compliance verifier (assigns Agent 1, 2, 3 in execution plan before BUILD)
> **Agent 1 — Coder (Primary Implementer):** Antigravity / delegated coding agent (named by Agent 4)
> **Agent 2 — QA Reviewer (Independent Code Quality):** named by Agent 4 per work item
> **Agent 3 — AppSec Auditor (Independent Security):** named by Agent 4 per work item
> **Codex — Technical Manager / Tech Lead:** independent technical reviewer & architectural sign-off (separate from Agent 4)
> **Executable work tracker:** GitHub Issues in `NgaiLong49423/scan-pilot`
> **Operational status board:** GitHub Project #13
> **Branch convention:** `codex/<issue-number>-<short-kebab-name>`
> **Local coordination directory:** `.agent-work/` (Git-ignored; no secrets)

The Integration Check **PASSED** on 2026-08-17:
- Codex successfully accessed and reviewed the branch/PR handed off by Antigravity;
- `.agent-work/` is properly Git-ignored;
- No secrets, tokens, or private credentials were detected in the review scope.

Every Git-tracked code change must follow the mandatory Nested Coordination Model (Mô hình Phối hợp Lồng nhau) and the review-before-commit boundary:

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

**Pre-BUILD Assignment Rule:** Before `BUILD` starts, the Issue contract must explicitly name Agent 4 (Delivery Gatekeeper / Coordinator). In Agent 4's execution plan, Agent 4 must explicitly name Agent 1 (Coder), Agent 2 (independent QA Reviewer), and Agent 3 (independent AppSec Auditor). Codex is recorded separately as Technical Lead / Technical Manager (not Agent 4, and not a subagent of Agent 4).

1. **Agent 4 (Delivery Gatekeeper / Coordinator):** Assigned to the Issue before `BUILD`; names Agent 1, 2, and 3; freezes the local review target before specialist review; verifies all three local reports refer to that unchanged worktree diff; and reports `READY_FOR_TECH_LEAD_REVIEW` in `.agent-work/acceptance/acceptance-<issue>.md`.
2. **Agent 1 (Coder):** Named by Agent 4; implements code/tests only in the local worktree; creates `.agent-work/reports/handoff-<issue>.md`; and MUST NOT commit, push, create a PR, or self-approve as QA/AppSec before explicit Product Owner authorization.
3. **Agent 2 (QA Reviewer):** Named by Agent 4; independently audits the frozen local diff and outputs strictly `APPROVED` or `REQUEST_CHANGES` in `.agent-work/qa-reviews/qa-<issue>.md`.
4. **Agent 3 (AppSec Auditor):** Named by Agent 4; independently audits the same frozen local diff and outputs strictly `APPROVED` or `BLOCKED` in `.agent-work/security-audits/sec-<issue>.md`.
5. **Codex (Technical Manager / Tech Lead):** Independently reviews the same local diff and Agent 4 package. Before returning `CHANGES_NEEDED` or `BLOCKED`, Codex performs root-cause analysis and corrects the accepted contract, template, or brief when that is the cause. Codex returns `APPROVED_FOR_PO_ACCEPTANCE` only within the stated verification limit.
6. **Product Owner (User):** Holds final acceptance authority. Only after explicit Product Owner acceptance may an agent commit the approved local diff. Push, pull-request creation, merge, Issue closure, and deployment each still require separate explicit Product Owner authorization.

**Local review target:** Before any QA/AppSec/Tech Lead review, Agent 4 records the worktree path, base commit for context only, and changed-file list, then stops implementation edits. Every pre-commit report must state `uncommitted local worktree`; it must not invent an implementation commit SHA. Any change to that local diff invalidates the QA, AppSec, Gatekeeper, and Tech Lead reports and requires fresh review.

**Technical Lead RCA and prevention:** For a workflow, contract, or evidence failure, Codex records `.agent-work/diagnostics/rca-<issue>.md` with the symptom, root cause, affected source-of-truth files/templates, bounded correction, prevention rule, and re-dispatch criteria. Codex may correct these delivery artifacts within the accepted Issue scope; the RCA is the corrective-change handoff and Agent 4 must obtain fresh independent QA/AppSec review of that local diff before returning it to Codex. Codex must not self-approve its correction. Any product, UI/UX, cost, privacy, permission, architecture, dependency, or external-state change still requires Product Owner direction.

Remediation rule: Any QA `REQUEST_CHANGES`, AppSec `BLOCKED`, or Codex `CHANGES_NEEDED`/`BLOCKED` returns the item through Agent 4 to Coder. After remediation, fresh QA, AppSec, Gatekeeper, and Tech Lead reviews are required for the changed local diff. Keep detailed briefs, reports, logs, and intermediate analysis in `.agent-work/`; keep eventual PR descriptions compact and secret-safe.


## Delivery Automation Policy

The Product Owner accepted a staged delivery-automation direction on 2026-08-17. It does not authorize implementation of a workflow by itself.

- **CI first:** a separate authorized Issue must introduce repeatable frontend and backend checks for pull requests and `main`. Once CI is active, a passing required check is a prerequisite for Codex technical review; before that point, agents must retain manual verification evidence.
- **Branch protection later:** require CI checks for `main` only after the new workflow has produced reliable green evidence on one or more real pull requests. Do not configure a required check that has not run successfully.
- **CD deferred:** production deployment is a separate release/deployment work item. It requires explicit Product Owner authorization and must not be triggered automatically merely because CI passes or a pull request merges.
- **Human gates remain:** CI reports repeatable build/test evidence. It does not replace Codex scope/security review or Product Owner acceptance and merge authority.
- **Current state:** no GitHub Actions CI/CD workflow is configured in this repository yet.

## Current Checkpoint

- A01 Broken Access Control research checkpoint exists.
- `SP-CONFIG-001 — Source Code Secret Exposure` is the first accepted MVP inspection rule.
- Continue from `docs/CURRENT-STATUS.md`; do not restart the research from zero.
