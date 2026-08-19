---
name: ponytail
description: Enforce the simplest, cleanest, and most minimal solution that actually works (YAGNI, stdlib first, native platform features before dependencies, zero over-engineering). Use for all coding, refactoring, designing, and dependency selection tasks in Scan Pilot.
---

> **Document:** Lean Code Crafting Skill (Ponytail Method)  
> **File:** `.agents/skill/ponytail/SKILL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Lean Code Crafting (Ponytail Method)

You act with the mindset of an experienced, pragmatic senior engineer: write the cleanest, most efficient code by writing only what is strictly necessary. The best code is the code that never had to be written.

## The 7-Rung Ladder (Priority Order)

Before writing any new code or creating files, evaluate the task through this ladder and stop at the first rung that satisfies the requirement:

```text
1. Does this need to exist at all?        → No: Skip it (YAGNI).
2. Already in this codebase?              → Reuse existing util, helper, pattern, or record. Look before you write.
3. Language Standard Library does it?     → Use Java 21 / TypeScript / Node built-in stdlib.
4. Native platform feature covers it?     → Use HTML5, CSS3, Web APIs, or PostgreSQL native DB capabilities.
5. Already-installed dependency solves it?→ Use Spring Boot / React existing dependencies. Never add a new dependency for what a few lines can do.
6. Can it be a clean one-liner?           → Keep it concise and direct.
7. Only then:                             → Write the minimum readable code that works.
```

The ladder runs **after** fully understanding the problem and tracing the data flow—not instead of it. Be lazy about adding complexity, never about reading and understanding.

## Core Rules for Scan Pilot

1. **No Speculative Abstractions:**
   - No interfaces with only one implementation.
   - No factory classes for a single product.
   - No configuration layers for static values that never change.
2. **Standard Library & Framework Built-ins First:**
   - **Java 21 / Spring Boot 3:** Use Java Records, Pattern Matching, `java.net.http.HttpClient`, `java.util.UUID`, Spring `StringUtils` / `CollectionUtils` before importing heavy external libraries.
   - **React / TypeScript:** Use native `Intl.DateTimeFormat`, `URLSearchParams`, `structuredClone`, `crypto.randomUUID()` before adding npm packages.
   - **PostgreSQL:** Enforce integrity via `CHECK`, `UNIQUE`, `FOREIGN KEY` constraints, JSONB operators, and `gen_random_uuid()` at the database level rather than reimplementing checks in Java service layers.
3. **Deletion Over Addition:**
   - Prefer removing dead code or simplifying structures over introducing new helper classes.
   - Shortest clean working diff wins.
4. **Bug Fixes Target Root Cause:**
   - Fix issues at the shared source/function rather than patching every caller individually.
5. **Technical Debt Conventions for MVP:**
   - Deliberate simplifications with known ceilings must be tagged with a comment:  
     `// ponytail: <known ceiling/limitation>, <upgrade trigger/path>`  
     *Example:* `// ponytail: in-memory map rate limiter, replace with Redis when scaled horizontally`

## Non-Negotiable Boundaries (When NOT to Simplify)

Never cut corners or simplify away:
- **Input validation** at trust boundaries (REST API inputs, external webhook payloads).
- **Error handling & transactional safety** that prevents data corruption or loss.
- **Security controls** (OWASP rules, secret scanning sanitization, authorization checks).
- **Accessibility basics** (a11y for frontend UI).
- **Test verification:** Non-trivial logic must have at least one runnable unit/integration test verifying the happy path and edge case.
