> **Document:** AI Studio Export and Frozen Evidence Verification  
> **File:** `docs/research/submission/AI-STUDIO-EXPORT-VERIFICATION.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-16  
> **Last Updated:** 2026-08-16  
> **Status:** Active  

# AI Studio Export and Frozen Evidence Verification

## Verification Result

**Issue:** [#5 — Verify AI Studio export and frozen evidence snapshot](https://github.com/NgaiLong49423/scan-pilot/issues/5)

**Result:** `PASS` — a reproducible one-way export was observed and a secret-safe frozen source archive was identified.

## Verified Transfer Paths

| Path | Observed behavior | Approved use |
|---|---|---|
| `Download as zip file` | AI Studio produced the standard archive `scan-pilot.zip`. | Frozen evidence snapshot and controlled source transfer. |
| `Export to Antigravity` | AI Studio opened a local Antigravity workspace and stated that all project files, conversation history, and one named secret would transfer. | Internal local workspace bootstrap only; not a shareable or commit-ready artifact. |

## Frozen Source Snapshot

| Property | Recorded value |
|---|---|
| AI Studio project identity | `9015a0c0-3972-426a-8c2b-26db961194b8` |
| Archive name | `scan-pilot.zip` |
| Capture timestamp | `2026-08-16 01:10:44 +07:00` |
| Archive size | `27,698` bytes |
| SHA-256 | `6B45ABFC72D0020F22FBAB7AC949F4DCAF22D24456A49477A6AF282D2A071219` |

The archive contains the expected Vite/React source and project metadata, including `src/`, `assets/`, `metadata.json`, `.env.example`, `.gitignore`, `package.json`, TypeScript/Vite configuration, and `README.md`. It contains no real `.env` entry. References to `GEMINI_API_KEY` occur only in source or example/documentation files and do not prove that a credential is present in the archive.

## Export Differences and Safety Boundary

The Antigravity transfer directory additionally contains a real `.env`, IDE configuration under `.vscode/`, and an Antigravity workspace record under `.antigravity/`. The export dialog also states that conversation history transfers. These items make that directory sensitive local working state, not an artifact for Git, public upload, or blind copying into the production repository.

The local repository prototype and the Antigravity source matched for the inspected core application files: `src/App.tsx`, `src/index.css`, `src/main.tsx`, `package.json`, `.env.example`, `.gitignore`, `index.html`, `tsconfig.json`, and `vite.config.ts`. `metadata.json` and `README.md` differed. This is expected evidence that a handoff occurred, not proof that either workspace is production-ready.

## Production Promotion Boundary

```text
Approved AI Studio prototype
→ ZIP evidence snapshot retained outside Git
→ selected source is reviewed and promoted into GitHub production source
→ production evolves only from GitHub
```

No continuous manual copy-paste synchronization is adopted. The Antigravity workspace is not a second source of truth. Credentials, conversation history, IDE state, and `.antigravity/` artifacts must remain outside the production Git history.

## Acceptance-Criteria Evidence

| Acceptance criterion | Result |
|---|---|
| Supported export or transfer executed and documented | `PASS` — ZIP and Antigravity paths observed. |
| Exported files and omissions/transforms identified | `PASS` — standard ZIP and additional Antigravity workspace state identified. |
| Exported source preserved as a versioned frozen evidence snapshot | `PASS` — ZIP archive is identified by project ID, timestamp, and digest. |
| Snapshot records identity, capture date, and integrity digest without secrets | `PASS` — SHA-256 recorded; no credential value stored in this record. |
| Production-source promotion boundary explicit | `PASS` — GitHub production repository remains the sole source of truth after promotion. |
| Continuous manual copy-paste synchronization not adopted | `PASS`. |
| PASS, FAIL, or blocker recorded | `PASS`. |

## Verification Limit

This check proves the observed export behavior and the recorded ZIP archive only. It does not prove production build compatibility, AI Studio round-trip import, future export behavior, Cloud Run/CORS compatibility, browser authentication, or that every uninspected workspace artifact is safe. The local Antigravity `.env` was intentionally not read or recorded.
