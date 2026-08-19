---
name: design-taste-frontend
description: Enforce high-taste, anti-slop, and beginner-friendly frontend UI/UX standards for Scan Pilot. Focuses on transforming complex security data into intuitive, human-readable, and beautifully structured interfaces (React, TypeScript, Vite, Tailwind CSS).
---

> **Document:** Frontend Design Taste & Human-Centric UX Skill  
> **File:** `.agents/skill/design-taste-frontend/SKILL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19  
> **Last Updated:** 2026-08-19  
> **Status:** Active  

# Frontend Design Taste & Human-Centric UX for Scan Pilot

Scan Pilot is a continuous health and security monitoring platform designed to make software security **accessible, approachable, and actionable for developers and beginners**, unlike traditional security tools (GitHub Security, SonarQube, Snyk) whose interfaces are often intimidating, cluttered with jargon, and difficult to navigate.

---

## 1. Dial Configuration for Scan Pilot

Scan Pilot operates as a **Developer Tool / Security Health Dashboard**. Balance clarity, high information density, and refined aesthetics using these dials:

- **`DESIGN_VARIANCE: 4`** — Clean, structured, highly scannable layouts with subtle asymmetrical accents.
- **`MOTION_INTENSITY: 3`** — Micro-interactions on hover, subtle spring transitions, smooth accordion reveals; zero distracting animations.
- **`VISUAL_DENSITY: 7`** — Efficient information density for tables, finding cards, and metrics without feeling cramped.

---

## 2. The "Security for Humans" UX Directives (Competitive Advantage)

Traditional security tools fail beginners because they present raw CVE codes, overwhelming alert lists, and vague warnings. Scan Pilot UI must enforce:

### 2.1. Plain-Language Summaries Over Jargon Walls
- Every security finding card must highlight **What happened**, **Why it matters**, and **What to do next** in plain, friendly language before showing technical hashes/tokens.
- Pair severity tags (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) with plain explanations (e.g., `CRITICAL: Active Google API Key exposed in public commit`).

### 2.2. One-Click Actionable Remediation (Diff Previews)
- Do not just say "Leak at line 42". Show a clean, syntax-highlighted **Before / After remediation diff** so the user immediately understands how to fix it.
- Include a 1-click **Copy Secret Revocation Command** or **Step-by-step Guided Action**.

### 2.3. Clear Visual Health Status & Reassurance
- Show clear, encouraging health progress (e.g., `98% Health Score`, `0 Leaks Detected`).
- **Engaging Empty States:** When no findings exist, render a clean, reassuring success card with guidance on continuous monitoring rather than a blank box.

### 2.4. Masking & Secret Privacy by Design
- Automatically redact sensitive secret values in the UI (e.g., `AIzaSyD...9xZ4`) with an explicit "Click to reveal" toggle that requires user intent.

---

## 3. Anti-Slop Visual & Code Standards

### 3.1. Typography & Hierarchy
- **Primary Typeface:** `font-sans` with `Geist Sans` or system UI font stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`).
- **Monospace Typeface:** `font-mono` with `Geist Mono` or `JetBrains Mono` for code snippets, fingerprints, and commit hashes.
- **Tabular Figures:** Always use `tabular-nums` for timestamps, line numbers, finding counts, and statistics to prevent layout jitter.
- **Heading Line Wrapping:** Use `text-wrap: balance` on headings to prevent single-word orphaned lines.

### 3.2. Color & Contrast (WCAG AA Compliance)
- **Backgrounds:** Never use pure `#000000` or sterile `#FFFFFF`. Use rich dark slate (`#0B0F17` / `#111827`) or warm clean off-white (`#F9FAFB` / `#F8FAFC`).
- **Severity Badge Palette:**
  - `CRITICAL`: Soft red container (`bg-red-500/10 text-red-400 border border-red-500/20`).
  - `HIGH`: Soft orange/amber container (`bg-amber-500/10 text-amber-400 border border-amber-500/20`).
  - `MEDIUM`: Soft yellow container (`bg-yellow-500/10 text-yellow-400 border border-yellow-500/20`).
  - `LOW / INFO`: Soft blue/cyan container (`bg-sky-500/10 text-sky-400 border border-sky-500/20`).
  - `RESOLVED / SAFE`: Soft emerald container (`bg-emerald-500/10 text-emerald-400 border border-emerald-500/20`).
- Ensure all text-to-background contrast ratios strictly exceed **4.5:1** (WCAG AA).

### 3.3. Layout & Responsiveness
- Use `min-h-[100dvh]` instead of `h-screen` to prevent iOS Safari viewport jump bugs.
- Max-width content constraint: `max-w-7xl mx-auto px-4 sm:px-6 lg:px-8`.
- Bento Grid Layouts for Dashboards: Use CSS Grid with `grid-flow-dense` to avoid awkward empty cells.

### 3.4. State Completeness Matrix
Every interactive view or component must implement 4 explicit states:
1. **Normal / Populated State:** Data rendered cleanly with crisp visual hierarchy.
2. **Skeleton Loading State:** Shimmering skeleton shaped like the actual finding card/table (never a solitary generic spinner).
3. **Empty / Reassurance State:** Friendly graphic or icon, helpful title, and clear call-to-action ("Connect a repository to start scanning").
4. **Error / Recovery State:** Clear error explanation with a retry button and diagnostic details if needed.

### 3.5. Motion & Transitions
- Micro-interactions only: `transition-all duration-200 ease-out`.
- Animate only GPU-accelerated properties: `transform` and `opacity`. Never animate `width`, `height`, `top`, or `left`.
- Support `prefers-reduced-motion`: disable animations when user preference is set.
