> **Document:** Scan Pilot Agent Instructions  
> **File:** `AGENTS.md`  
> **Version:** v1.2.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-13  
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
4. the canonical specification relevant to the task
5. relevant files under `docs/research/`

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
- modular monolith backend
- PostgreSQL
- GitHub App integration
- Gemini as the first AI provider behind a provider abstraction
- asynchronous isolated scan workers
- Google Cloud deployment direction

Exact infrastructure choices listed as open in `docs/DECISIONS.md` remain unresolved.

## Documentation Rules

- Repository Markdown is the canonical development format.
- Important Markdown documents must use the metadata standard described in `.agent/skill/document-metadata-standardizer/SKILL.md`.
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
