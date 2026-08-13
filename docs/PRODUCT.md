> **Document:** Scan Pilot Product Definition  
> **File:** `docs/PRODUCT.md`  
> **Version:** v0.4.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Under Review  

# Scan Pilot Product Definition

## Product Vision

Scan Pilot helps people who build software with AI understand which repositories are at risk, what changed, what to fix next, and whether a fix actually improved the project.

It is a continuous monitoring product rather than a one-time AI code review.

## Core User Story

> As a developer managing AI-assisted projects, I want one dashboard that monitors my GitHub repositories and explains evidence-backed problems so that I can prioritize fixes and verify improvement without being a security expert.

## Core Product Loop

```text
Connect repository
→ baseline scan
→ inspect prioritized findings
→ create an action or GitHub Issue
→ fix with a developer or coding agent
→ re-scan
→ mark RESOLVED or REGRESSED
```

## MVP Outcome

The MVP succeeds when a user can complete the core loop against a real repository and understand the result without specialist security knowledge.

Required product capabilities currently accepted:

- GitHub repository connection;
- validated current-snapshot and Git-history baseline scans;
- event-driven scan direction for push and relevant GitHub events;
- evidence-backed findings with severity and remediation;
- Gemini-assisted contextual explanation;
- GitHub Issue workflow;
- re-scan and lifecycle tracking;
- working Google Cloud deployment direction;
- a focused set of strong rules rather than a mock-heavy rule catalog.
- Project Discovery and a persistent Repository Profile;
- asynchronous human review for conclusions that require project or business context.

## Monitored Branch Direction

Each repository has one required `PRIMARY` branch and up to two optional `SECONDARY` branches in the MVP. The primary always mirrors the GitHub default branch and is not a separate user preference.

The primary is scanned first during repository initialization. Its exact captured HEAD is checked first so current Findings can appear early, then its full reachable Git history is scanned. Secondary-branch configuration becomes available only after that history baseline completes with validated coverage; detected Findings do not make initialization fail. During normal monitoring, branches scan independently, and a later primary failure does not cancel secondary scans.

After a valid baseline, Scan Pilot uses compatible checkpoints for incremental history scanning. A force-push or other non-ancestor history rewrite causes a new baseline rather than an unsafe clean assumption.

If GitHub changes the default branch, Scan Pilot synchronizes the primary automatically. When the new default is not already monitored and all three slots are occupied, the former primary leaves the current monitored scope while both user-selected secondary branches remain. Historical scans, evidence, and Findings for the former primary remain available for audit.

## Project Understanding and Human Review

Scan Pilot remembers structured facts about each repository, including detected technologies, important project files, architecture signals, external services, and the scan checkpoint. This state is persisted in the application database and refreshed when relevant evidence changes.

The dashboard includes an Action Center for Review Requests. A user can select a suggested answer, state that they do not know, provide another answer, add free-text context, and reference supporting repository or GitHub evidence. Scans do not pause while awaiting a response.

User responses remain attributed assertions. They help interpretation but do not automatically replace technical evidence or prove that a risk has been fixed.

## Dashboard Direction

The dashboard is expected to support multiple projects, current health, category findings, last scan, project trend, and action priority. Exact scoring and status thresholds remain unresolved.

Potential project states under consideration:

- `PROTECTED`
- `NEEDS_ATTENTION`
- `AT_RISK`
- `SCAN_REQUIRED`

These state names are product direction, not yet a finalized scoring contract.

## Supported Stack Direction

Deep V1 analysis should prioritize Java/Spring Boot, React/TypeScript, GitHub Actions, Docker, and common configuration formats. Generic secret scanning may cover more languages.

**Reason:** A narrow set of supported stacks enables stronger cross-file and framework-aware evidence than claiming shallow support for every language.

## Out of Scope Unless Later Accepted

- keystroke-level editor monitoring;
- training a proprietary security model for the MVP;
- claiming complete OWASP ASVS compliance;
- executing untrusted repository code inside the main API process;
- supporting every programming language equally in V1;
- automatically fixing or pushing changes without explicit user authorization.

## Review Note

This document remains `Under Review` because exact V1 feature boundaries, final rule count, scoring, and confidence behavior have not been accepted.
