> **Document:** Scan Pilot Current Status  
> **File:** `docs/CURRENT-STATUS.md`  
> **Version:** v1.10.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Active  

# Scan Pilot Current Status

## Current Phase

**Research and specification.** Documentation migration from the original template is complete.

Product code has not started. Do not begin implementation without an explicit user instruction changing the phase.

## Submission Context

- Target: a working product submission for a Google-oriented event.
- Team structure: one solo developer. The previously recorded two-person team belongs to a different project.
- User availability: the user is currently on a break between semesters and can invest substantial time.
- Target window: end of August 2026.
- Exact submission date: not yet recorded in canonical documentation.

## Completed

- Product direction and dashboard-first workflow accepted.
- Core technology and GitHub integration direction accepted.
- A01 Broken Access Control research checkpoint transferred into the main repository.
- Standards and research-source policy documented.
- Official product name changed from VibeGuard to Scan Pilot.
- `SP-CONFIG-001 — Source Code Secret Exposure` accepted as a MUST MVP rule.
- Google/Gemini API key classification, safety, wording, and initial severity policy accepted for `SP-CONFIG-001`.
- Project Discovery, persistent Repository Profiles, scan checkpoints, and finding history accepted as structured PostgreSQL state.
- Asynchronous human-in-the-loop Review Requests accepted, including structured answers, `Another answer`, optional context, supporting references, and secret-safe handling.
- Shared Evidence Model accepted for Technical Evidence, User Assertions, AI Inferences, scoped claims, provenance, and verification status.
- Hybrid Finding Tracking accepted using rule-specific identity, Git history and diff, Evidence Locations, optional semantic context, and re-scan verification.
- Immutable source snapshots and separate Disposable Mutable Workspaces accepted; temporary mutation does not alter repository truth.
- `SP_SECRET_FP_V1` accepted for repository-scoped, HMAC-SHA-256 secret identity with stable Google key family, key versioning, and trusted processing boundaries.
- Git checkpoint policy accepted: agents group coherent work, proactively propose meaningful commit/push checkpoints, and act only after explicit authorization for each Git operation.
- Lightweight solo branch workflow accepted: keep `main` stable, use one branch per coherent large workstream, and reserve optional pull requests for useful self-review of large changes.
- Finding lifecycle is separated from remediation quality. `SP-CONFIG-001` may be `RESOLVED` after clean-source verification and credential invalidation, while remaining history is reported as `RISK_CONTAINED`; adequate clean-history verification produces `VERIFIED_COMPLETE`.
- Primary-branch policy accepted: `PRIMARY` always mirrors the GitHub default branch, monitoring supports up to two user-selected secondary branches, and GitHub default changes synchronize automatically under the accepted capacity policy while preserving historical evidence.
- `SP-CONFIG-001` scan pipeline accepted: Gitleaks behind a detector adapter, immutable HEAD snapshot first, graph-aware full-history baseline, validated coverage/checkpoints, ancestor-only incremental ranges, and full re-baseline after incompatible or rewritten history.
- Onboarding option B accepted: a branch may show early snapshot Findings, but it becomes `MONITORED` only after a validated full reachable-history baseline; secondary configuration unlocks only after the primary reaches that point.
- Repository documentation migrated away from the generic Java web-app template.

## In Progress

- A02 Security Misconfiguration research.
- Converting accepted decisions into canonical product and inspection specifications.
- Refining the agent-readable repository context as new decisions are accepted.
- Defining exact repository content exclusions and operational limits for the accepted snapshot/history pipeline.

## Next Logical Task

Complete the specification of `SP-CONFIG-001` as the first vertical slice:

1. define exact repository scan scope and exclusions for Google/Gemini keys;
2. benchmark the accepted Gitleaks adapter contract, including command behavior, telemetry, timeout, report parsing, and cleanup;
3. define redaction and safe logging requirements for repository and user-provided input;
4. define the operational fingerprint key service and rotation policy when deployment design begins;
5. continue deciding the other supported MVP secret types;
6. define backfill scheduling and resource limits without changing the accepted coverage contract.

**Reason:** The scanner responsibility, baseline/incremental policy, and onboarding gate are now accepted. The remaining pre-implementation boundary is which files and artifacts are in scope and whether the adapter can reliably prove the coverage that Scan Pilot records.

## Current Research Checkpoints

| Area | Status | Canonical file |
|---|---|---|
| A01 Broken Access Control | Checkpoint complete; rules remain candidates | `docs/research/security/A01-BROKEN-ACCESS-CONTROL.md` |
| A02 Security Misconfiguration | Under review; one rule accepted | `docs/research/security/A02-SECURITY-MISCONFIGURATION.md` |
| Inspection specification | Draft; one accepted rule recorded | `docs/INSPECTION-SPEC.md` |
| Evidence model | Accepted and active | `docs/EVIDENCE-MODEL.md` |
| Finding tracking model | Accepted and active | `docs/FINDING-TRACKING.md` |

## Constraints for the Next Agent

- Do not restart A01 research unless new evidence requires revision.
- Do not silently accept remaining A02 candidates.
- Explain one decision at a time in accessible Vietnamese.
- Include the reason, benefit, trade-off, and verification limit for each proposal.
- Do not commit, push, or implement product code without explicit authorization.
