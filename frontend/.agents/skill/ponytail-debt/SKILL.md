---
name: ponytail-debt
description: Harvest and track intentional technical simplifications and MVP shortcuts marked with `// ponytail:` comments across the Scan Pilot repository. Use when assessing technical debt, planning post-MVP refactoring, or auditing deferred improvements.
---

> **Document:** Technical Debt Tracking Skill (Ponytail Debt)  
> **File:** `.agents/skill/ponytail-debt/SKILL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Technical Debt Tracking (Ponytail Debt)

During MVP implementation, pragmatic shortcuts are often taken to meet delivery milestones without over-engineering. To ensure these shortcuts do not rot into permanent unmanaged debt, every deliberate simplification is tagged in code with:

```java
// ponytail: <ceiling or known limitation>, <upgrade trigger or path>
```

This skill collects all markers into a structured Technical Debt Ledger.

## Scanning Procedure

Scan all repository source files (excluding `.git`, `node_modules`, `target`, `build`, `dist`):

1. Search for comment markers: `(?://|#|/\*)\s*ponytail:\s*(.+)`
2. Parse the ceiling and upgrade path for each match.
3. Flag any marker missing an explicit upgrade trigger as `[NO-TRIGGER]` (highest risk of silent decay).

## Output Format

Present the findings grouped by module/component:

```markdown
# Scan Pilot Technical Debt Ledger (Ponytail Markers)

| Location | Component | Simplification / Known Ceiling | Upgrade Trigger / Path | Status |
|---|---|---|---|---|
| `ScanWorker.java:42` | Scanner Engine | In-memory sequential queue | When concurrent jobs exceed 5 -> Move to RabbitMQ/Redis | `Tracked` |
| `TokenStore.ts:18` | Frontend Auth | LocalStorage session token | When refresh token rotation is deployed -> Move to HttpOnly Cookie | `Tracked` |

**Summary:** <N> tracked shortcuts found (<M> missing upgrade triggers).
```

If no markers are found: `Clean ledger. No deferred ponytail debt.`
