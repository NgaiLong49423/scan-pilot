> **Document:** Scan Pilot Frontend Agent Instructions & Design Taste
> **File:** `AGENTS.md`
> **Version:** v1.0.0
> **Created:** 2026-08-19
> **Status:** Active (Frontend & Google AI Studio Root)

# Scan Pilot — Frontend Agent Instructions & UI/UX Design System

Welcome to **Scan Pilot** (formerly VibeGuard) — a continuous multi-project health monitoring platform for AI-assisted and AI-generated software.

This workspace represents the **Frontend Single Page Application (SPA)** built with React 18, TypeScript, Vite, Tailwind CSS, and Lucide Icons, connected to a live Google Cloud Run backend.

---

## 1. System Architecture & Live Backend Contract

### 1.1 Backend Connection
* **Live Production Backend:** `https://scan-pilot-api-drbjfwrlxq-as.a.run.app`
* **Environment Variable:** `VITE_API_BASE_URL` (configured in Secrets or `.env`).
* **API Route Prefix:** `/api/v1/*`

### 1.2 Immutable API Layer (`src/api/` — DO NOT BREAK)
When generating or modifying frontend code in Google AI Studio, **NEVER delete, overwrite, or mutate the API contracts** in `src/api/`:
* `src/api/client.ts`: Native `fetch` client with dynamic `VITE_API_BASE_URL` resolution and `credentials: 'include'`.
* `src/api/authApi.ts`: User profile (`/api/v1/auth/me`), GitHub OAuth redirect, and session logout.
* `src/api/projectsApi.ts`: Monitored repository selection and secondary branch slot management.
* `src/api/scansApi.ts`: Scan trigger (`/api/v1/scans/trigger`), scan job polling, security findings, and coverage records.
* `src/api/aiApi.ts`: Google Gemini 1.5 Flash finding explanation and remediation guide.
* `src/api/githubApi.ts`: GitHub App repository discovery.

---

## 2. "Security for Humans" UI/UX Design Directives

Scan Pilot makes software security accessible, approachable, and actionable for developers and beginners:

### 2.1 Plain-Language Summaries Over Jargon Walls
* Highlight **What happened**, **Why it matters**, and **What to do next** in plain, friendly language before showing technical hashes/tokens.
* Pair severity tags (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) with human explanations (e.g. `CRITICAL: Active Google API Key exposed in public commit`).

### 2.2 Actionable Remediation (Before/After Diff Previews)
* Show syntax-highlighted **Before / After remediation diffs** so the user immediately understands how to fix the issue.
* Provide a 1-click **Copy Secret Revocation Command** or Guided Action link.

### 2.3 Visual Health Status & Reassuring Empty States
* When no security findings exist, render a clean, reassuring empty state (`EmptyState.tsx`) rather than a blank box.
* Show scan progress indicators (`ScanProgressBar.tsx`) with real-time step animations.

### 2.4 Masking & Secret Privacy by Design
* Automatically redact sensitive secret values in the UI (e.g. `AIzaSyD...****`) with click-to-copy masked text.
* Zero raw plaintext secrets should ever be displayed or stored in local storage.

---

## 3. Visual & Styling Standards (Tailwind CSS)

### 3.1 Color & Contrast (WCAG AA Compliance)
* **Backgrounds:** Deep rich navy/slate (`bg-slate-950`, `bg-slate-900/80`, `bg-slate-900`).
* **Borders:** Subtle translucent borders (`border-slate-800`, `border-slate-700/50`).
* **Glow Accents:** Soft cyan (`cyan-500`), blue (`blue-500`), and emerald (`emerald-500`).
* **Severity Colors:**
  * **Critical:** `text-red-400 bg-red-950/60 border-red-800`
  * **High:** `text-orange-400 bg-orange-950/60 border-orange-800`
  * **Medium:** `text-amber-400 bg-amber-950/60 border-amber-800`
  * **Low / Info:** `text-blue-400 bg-blue-950/60 border-blue-800`

### 3.2 Typography & Alignment
* **Primary Font:** Sans-serif (`font-sans`, Inter, system stack).
* **Monospace Font:** Code font (`font-mono`) for file paths, line numbers, commit SHAs, and secret masks.
* **Tabular Figures:** Always apply `tabular-nums` on timestamps, metric counts, and line numbers to prevent layout jitter.

---

## 4. Component Structure (`src/components/`)

| Component | Responsibility |
|---|---|
| `Header.tsx` | App branding, monitored repository badge, user avatar, Sign in / Logout. |
| `FindingCard.tsx` | Individual finding card with severity badge, masked secret, and expandable AI guide. |
| `AiRemediationGuide.tsx` | Gemini AI explanation card: Summary, Risk Impact, Scoped Claims, Step Checklist, Before/After Diff. |
| `ScanProgressBar.tsx` | Animated scan progress bar with step status indicators. |
| `CoverageTab.tsx` | Scanned vs skipped audit metrics donut/bar and skipped files list. |
| `RepoSelectorModal.tsx` | Repository switcher modal with branch slot indicators (max 2 secondary). |
| `EmptyState.tsx` | Reassurance empty state when repository is clean. |
| `LoadingSkeleton.tsx` | Shimmer loading skeleton cards. |
| `ErrorBanner.tsx` | Graceful error dismissible alert banner. |

---

## 5. Deployment Guidelines (Google AI Studio to Cloud Run)

1. Set `VITE_API_BASE_URL=https://scan-pilot-api-drbjfwrlxq-as.a.run.app` in AI Studio Project Settings / Secrets.
2. Verify all UI components in the **Preview** tab.
3. Click **Deploy to Cloud Run** in Google AI Studio (`asia-southeast1`) to publish the production frontend service.
