---
name: ui-design-audit
description: Comprehensive UI/UX audit checklist for frontend components, pull requests, and views in Scan Pilot. Audits accessibility, contrast, responsiveness, human-centric security UX, and visual polish before merging.
---

> **Document:** UI/UX Design Audit Skill  
> **File:** `.agents/skill/ui-design-audit/SKILL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# UI/UX Design Audit Checklist for Scan Pilot

Use this checklist during Codex PR reviews and pre-merge audits for all Frontend work items to ensure Scan Pilot maintains its competitive advantage in usability and aesthetic quality.

---

## 1. Human-Centric Security UX (Beginner-Friendliness)

- [ ] **Plain-Language Summary:** Does the finding card explain what happened in plain terms before showing raw technical tokens/hashes?
- [ ] **Actionable Remediation:** Is there a clear, syntax-highlighted Before/After fix preview?
- [ ] **Secret Masking:** Are sensitive detected values (API keys, tokens) automatically masked by default with a reveal button?
- [ ] **Positive Reassurance:** When 0 leaks/vulnerabilities are found, is there an encouraging, informative empty/clean state instead of a blank box?
- [ ] **Zero Jargon Overload:** Are confusing acronyms accompanied by plain tooltips or contextual helpers?

---

## 2. Accessibility & Typography (WCAG AA)

- [ ] **Contrast Verification:** Do all text, buttons, and status badges meet the WCAG AA minimum contrast ratio of **4.5:1**?
- [ ] **Font Hierarchy:** Is `font-sans` used for UI/prose and `font-mono` with `tabular-nums` used for code, commit hashes, and numbers?
- [ ] **Heading Balance:** Does `text-wrap: balance` prevent awkward single-word wrapping?
- [ ] **Keyboard Navigation:** Are visible focus rings (`focus-visible:ring-2 focus-visible:ring-offset-2`) present on all interactive elements?

---

## 3. Layout, Density & Responsive Polish

- [ ] **Viewport Height Stability:** Is `min-h-[100dvh]` used instead of `h-screen` to prevent iOS Safari layout shifts?
- [ ] **Bento Grid Cleanliness:** Does the dashboard grid flow densely without broken or empty gaps?
- [ ] **Container Constraints:** Is the page bounded by `max-w-7xl mx-auto px-4` to prevent stretching on ultra-wide monitors?
- [ ] **Button Alignment:** Are action buttons pinned consistently at the bottom of cards across variable content lengths?

---

## 4. State Completeness

- [ ] **Skeleton Loader:** Is there a bespoke skeleton shape while data is loading (no lone spinners)?
- [ ] **Inline Error Handling:** Are API errors reported contextually with retry actions (no browser `alert()`)?
- [ ] **GPU-Accelerated Motion:** Are all hover/transition animations strictly using `transform` and `opacity`?
- [ ] **Motion Sensitivity:** Is `@media (prefers-reduced-motion)` respected?
