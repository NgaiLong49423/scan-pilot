---
name: full-output-enforcement
description: Enforces complete, unabridged, production-ready code generation. Strictly bans placeholder patterns (// TODO, // rest of code, // implement here) and handles long output token splits gracefully in Scan Pilot.
---

> **Document:** Full Output & Complete Code Enforcement Skill  
> **File:** `.agents/skill/full-output-enforcement/SKILL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Full Output Enforcement

## Core Directive

Every implementation deliverable in Scan Pilot must be complete, runnable, and production-ready. Truncation, skipping sections, or inserting lazy placeholders is strictly forbidden.

## Banned Patterns (Immediate Rejection)

Never produce or commit the following lazy patterns:

1. **In Code Blocks:**
   - `// ...` or `/* ... */` representing skipped logic.
   - `// rest of code goes here`
   - `// implement here` / `// TODO: add remaining fields`
   - `// similar to above`
   - Unimplemented switch branches or empty fallback handlers.

2. **In Explanations:**
   - "For brevity, I omitted..."
   - "The rest follows the same pattern..."
   - "I'll leave the remaining components as an exercise."

## Token Boundary Handling

When a response is approaching token limits on very large files:
1. Do not compress or truncate the remaining code into a half-working draft.
2. Complete all code cleanly up to a logical boundary (end of a class, method, or component).
3. Explicitly state:
   ```text
   [PAUSED — Part 1 of 2 complete. Say "continue" to generate Part 2 starting from: <Component/Method Name>]
   ```
4. On "continue", resume immediately from that exact point without repeating previous code.
