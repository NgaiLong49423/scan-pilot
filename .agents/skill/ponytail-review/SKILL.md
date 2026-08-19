---
name: ponytail-review
description: Review git diffs and code proposals exclusively for over-engineering, speculative abstractions, redundant dependencies, and reinvention of standard library/platform features. Use during pull request reviews, pre-commit checks, or when assessing code complexity in Scan Pilot.
---

> **Document:** Lean Code Review Skill (Ponytail Review)  
> **File:** `.agents/skill/ponytail-review/SKILL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Lean Code Review (Ponytail Review)

Review code diffs and implementations exclusively for unnecessary complexity and over-engineering. The goal of this review is to help the diff get shorter, cleaner, and more maintainable without losing required capabilities or safety.

## Review Focus Tags

When reviewing a diff, categorize over-engineering findings using these standard tags:

- `delete:` Dead code, unused flexibility, speculative abstractions, or empty scaffolding. Replacement: nothing.
- `stdlib:` Hand-rolled logic that Java 21, TypeScript, or Node standard library already provides. Name the stdlib class/method.
- `native:` Custom code or 3rd-party library doing what HTML5, CSS3, Web API, or PostgreSQL natively handles.
- `yagni:` Single-implementation interface, single-product factory, unused config parameters, or single-caller helper layers. Inline or simplify.
- `shrink:` Valid logic that can be expressed in significantly fewer lines using modern language features (Java records, pattern matching, stream operations, object destructuring).

## Format for Review Findings

Report each finding in a single, crisp line:

`<file>:L<line>: <tag> <what to simplify/cut>. <replacement or recommendation>.`

### Examples

- `SecurityConfig.java:L45-60: yagni: Custom TokenValidator interface with only 1 implementation. Inline class directly until multi-token support is required.`
- `DateUtil.ts:L12-30: stdlib: 18-line date formatter function. Use new Intl.DateTimeFormat().`
- `UserRepository.java:L80: native: App-level unique check query before insert. Add UNIQUE constraint to PostgreSQL table and handle DataIntegrityViolationException.`
- `pom.xml:L110: delete: Apache Commons Lang imported for 1 StringUtils call. Replace with Spring Framework's org.springframework.util.StringUtils.`

## Scoring & Conclusion

Conclude the review with:
- `Net potential reduction: -<N> lines, -<M> dependencies.`
- If the implementation is already lean and minimal: `Lean already. Approved.`

## Boundaries

This review focuses strictly on **complexity and over-engineering**. Correctness bugs, security vulnerabilities (ASVS/OWASP), and performance profiling should be handled in primary technical/security review passes.
