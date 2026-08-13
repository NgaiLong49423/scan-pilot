> **Document:** Scan Pilot Project Context  
> **File:** `docs/PROJECT-CONTEXT.md`  
> **Version:** v1.3.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Active  

# Scan Pilot Project Context

## Product Direction

Scan Pilot is a continuous multi-project health monitoring platform for AI-generated and AI-assisted applications.

Its operating model resembles antivirus software for source-code repositories. A user connects projects, Scan Pilot monitors relevant changes, produces evidence-backed findings, explains what matters, and verifies whether later fixes improve or regress the project.

```text
Connect GitHub repositories
→ baseline scan
→ dashboard
→ push / pull request / merge / scheduled / manual scan
→ findings
→ Gemini explanation and remediation guidance
→ GitHub Issue workflow
→ developer or coding agent fixes externally
→ re-scan
→ prove improvement or identify regression
```

The primary interface is a multi-project dashboard. AI chat or assistant features are supporting experiences, not the core product surface.

## Persistent Project Context

When a repository is connected, Scan Pilot performs Project Discovery before or alongside the first security scan. Deterministic extractors inspect repository metadata, structure, manifests, selected documentation, CI/CD, container, and infrastructure files. Gemini may summarize selected context where natural-language reasoning is useful, but it does not replace deterministic evidence.

Scan Pilot stores the resulting Repository Profile, scan checkpoints, finding history, and user-provided project context in PostgreSQL. Repository Markdown may be used as untrusted evidence, but it is not runtime state and Scan Pilot does not automatically commit a generated memory file to the user's repository.

Each profile claim must retain its source, scope, source commit, and verification status. The profile is refreshed when relevant inputs change or when the extractor version changes.

## Human-in-the-Loop Direction

When available evidence is insufficient and additional context could materially change a conclusion, Scan Pilot creates an asynchronous Review Request in the dashboard. The scan completes without waiting for a response.

A Review Request supports structured choices, `I don't know`, `Another answer`, optional free-text context, and an optional repository-relative file or GitHub link. User responses are recorded as user assertions, not silently promoted to technical evidence. Secret-like input must be redacted before persistence, logging, display, or Gemini analysis.

## User and Problem

Primary users are solo builders, students, small teams, and developers managing several repositories with help from AI coding agents.

## Delivery Context

Scan Pilot itself is currently built by one solo developer. References to a two-person team belong to a different project and must not be used for Scan Pilot planning, architecture justification, or delivery estimates.

The repository uses a lightweight branch workflow: `main` remains stable, while each coherent large workstream uses a working branch. Pull requests are optional self-review checkpoints for large changes, not a requirement for every small documentation edit.

Their problem is not merely finding one vulnerability. They need to understand:

- which project needs attention first;
- what new problems appeared after a change;
- what can be fixed next;
- whether project health is improving or declining;
- whether a previously fixed problem returned.

## Monitored Dimensions

The broader product direction includes:

- Security
- Reliability
- Testing
- Code Quality
- Documentation
- Deployment Readiness

Security is the current research focus. Exact scoring across all dimensions is not yet accepted.

## GitHub Workflow

GitHub is the primary integration. The intended full workflow includes repository selection, repository content access, relevant pull-request and workflow metadata, webhook events, and GitHub Issue creation.

Primary scan triggers:

- push;
- pull request;
- merge;
- scheduled scan;
- manual scan.

ZIP upload may later support a one-time scan, but it does not replace GitHub-based continuous monitoring. V1 does not require keystroke-level local editor monitoring.

## AI Strategy

AI is not the sole scanner and does not define security truth.

```text
deterministic or static tools
→ candidate evidence
→ context retrieval
→ Gemini analysis where useful
→ normalized finding
```

Gemini may explain findings, reason across files, prioritize, propose remediation, and draft GitHub Issues. Standards-backed requirements and deterministic evidence remain separate from AI interpretation.

The architecture must permit future AI providers and BYOK, but the exact BYOK secret-storage design remains unresolved.

## Architecture Direction

- React + TypeScript + Vite frontend
- Spring Boot 3 + Java 21 backend
- RESTful API
- modular monolith
- PostgreSQL with JPA/Hibernate and Flyway direction
- GitHub App integration
- asynchronous scan jobs
- isolated workers for repository scanning
- Gitleaks as the first secret detector behind a Scan Pilot detector adapter
- Google Cloud deployment direction

Untrusted repository code must never execute inside the main API server process.

## Inspection Principles

```text
External Standard Requirement
→ Scan Pilot Rule
→ Repository Scan
→ Finding
```

- OWASP Top 10 categories are risk groupings, not direct scanner rules.
- Rules should preserve versioned standards references.
- Requirement coverage and detection method are separate dimensions.
- Findings must state evidence and verification limits.
- Scan Pilot must not overclaim compliance or certainty.
- MVP rules must work on real repositories; the main demo must not depend on mocked findings.

## Current Research Checkpoint

A01 Broken Access Control research is complete as a checkpoint. Its authorization rules remain research candidates until consolidated and accepted into the inspection specification.

The first accepted official MVP rule is:

```text
SP-CONFIG-001 — Source Code Secret Exposure
Priority: MUST
Automability: PARTIAL
Detection: STATIC
```

The current work remains documentation, research, and specification. Implementation requires an explicit phase change from the user.

## Open Questions

Do not invent answers for:

- exact scoring formula;
- exact confidence scale;
- whether Evidence Strength is persisted;
- thresholds for confirmed findings;
- final number of V1 rules;
- exact detectors for rule families beyond the accepted Gitleaks-backed secret-scanning path;
- exact ASVS coverage UI;
- exact BYOK encryption and storage design;
- exact V1 queue technology;
- exact project status thresholds.
