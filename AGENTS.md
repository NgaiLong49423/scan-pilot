> **Document:** Scan Pilot Agent Instructions  
> **File:** `AGENTS.md`  
> **Version:** v1.7.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-14  
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
4. `docs/CLOUD-BUDGET.md` when a task may affect architecture, deployment, external services, resource sizing, benchmarking cost, Gemini usage, or public operation
5. relevant submission-context documents under `docs/research/submission/` when a task may affect product direction, MVP scope, Google technology integration, deployment, demo, or submission material
6. the canonical specification relevant to the task
7. relevant files under `docs/research/`

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

The project is in **research and specification**. Documentation migration from the original template is complete. Do not start product implementation until the user explicitly changes the phase.

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
- A pull request is optional for a large feature or checkpoint when reviewing the complete diff would be useful. Do not introduce `develop`, `release`, or `hotfix` branches unless a later accepted need justifies them.
- Do not create a commit after each file edit, discussion, or individual accepted decision.
- Group related decisions, specifications, or implementation changes into a coherent checkpoint that can be reviewed and restored as one meaningful unit.
- A checkpoint is ready to propose only when its intended scope is complete, relevant documentation and metadata are synchronized, applicable verification has run, and the diff has been inspected for unrelated files and secrets.
- When a meaningful checkpoint is ready or when an off-device backup would materially reduce risk, proactively ask the user whether it should be committed and, separately, pushed to GitHub.
- The proposal must explain the checkpoint scope, why it is ready, verification performed, known limitations, affected files, and suggested Conventional Commit message.
- A proposal is not authorization. Commit only after explicit user permission to commit. Push only after explicit user permission to push; permission to commit alone does not imply permission to push.
- Do not split one coherent checkpoint into noisy per-file commits, and do not combine unrelated work merely to reduce commit count.
- Important checkpoints should be pushed to GitHub after authorization so the repository has an off-device copy outside the laptop.

## Current Checkpoint

- A01 Broken Access Control research checkpoint exists.
- `SP-CONFIG-001 — Source Code Secret Exposure` is the first accepted MVP inspection rule.
- Continue from `docs/CURRENT-STATUS.md`; do not restart the research from zero.
