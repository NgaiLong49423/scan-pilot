> **Document:** Scan Pilot Current Status  
> **File:** `docs/CURRENT-STATUS.md`  
> **Version:** v1.22.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-15  
> **Status:** Active  

# Scan Pilot Current Status

## Current Phase

**Research and specification.** Documentation migration from the original template is complete.

Production product implementation has not started. A downloaded AI Studio prototype exists locally for inspection, but it has not been adopted as tracked production source. Do not begin implementation without an explicit user instruction changing the phase.

## Submission Context

- Target: a working product submission for a Google-oriented event.
- Team structure: one solo developer. The previously recorded two-person team belongs to a different project.
- User availability: the user is currently on a break between semesters and can invest substantial time.
- User-reported external deadline: 2026-08-31 at 23:59; live form and timezone still require verification.
- Internal complete-and-stable gate: 2026-08-30, reserving the final day for verification and submission.

## Completed

- AI Riser submission architecture accepted: Google AI Studio is the submission-facing frontend, the approved snapshot is frozen as evidence after a one-way handoff, and GitHub production source becomes the source of truth rather than maintaining two active codebases.
- Submission MVP accepted as a narrower Product V1 vertical slice: one real personal-account repository, current and history secret scan, redacted `SP-CONFIG-001`, Gemini explanation, re-scan lifecycle, and public Cloud Run operation.
- Submission onboarding accepted: sign in first, then install or link the GitHub App, then explicitly select one public or private personal-account repository; organization support is deferred.
- Gemini's submission authority is bounded to explanation and guidance from redacted context. Repository mutation, credential actions, and Finding lifecycle decisions remain outside Gemini.
- Independent validation evidence and a separate user-owned security-lab repository are accepted. Detector quality, Scan Pilot adapter/orchestration behavior, and end-to-end product behavior must be evidenced separately.
- Product Owner decision altitude accepted: the user decides outcome, scope, value, cost, privacy, permissions, and UI/UX; agents own ordinary technical mechanisms within accepted constraints and escalate material impact.
- Product direction and dashboard-first workflow accepted.
- Core technology and GitHub integration direction accepted.
- Apache Maven accepted as the canonical Java backend build and dependency-management tool; exact Wrapper, module, plugin, dependency, profile, and CI details remain for implementation design.
- The initial `SP-CONFIG-001` full-file size policy is accepted: Continuous Monitoring scans eligible text through `10 MiB`, while release-oriented verification scans or reuses compatible evidence through a `50 MiB` hard ceiling; every larger-file skip remains visible and required release coverage becomes incomplete above the ceiling.
- Scan Pilot now owns the trusted Gitleaks detection policy. Repository config, ignore files, inline allow directives, and inherited Gitleaks config variables cannot silently weaken the baseline; exact version pinning and `.gitleaksignore` isolation still require benchmark verification.
- Configuration Awareness is accepted as a dedicated product capability: inventory and deterministic family/role classification come before family-specific analysis; changes trigger reassessment but are not Findings by themselves; repository evidence is not represented as verified production state.
- The general Configuration Awareness checkpoint is complete: multi-dimensional artifact identity, independent classification/parse/support outcomes, scenario-bounded environment and override semantics, targeted change invalidation and evidence reuse, and separate attention/coverage/change UX are accepted.
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
- Content eligibility option B accepted: all Git-tracked content in the selected scope is considered, supported content is scanned, and every skip must carry a reason code and coverage impact.
- Binary document extraction was reconsidered and deferred beyond the MVP. Project Discovery inventories PDF and common Office documents without internal semantic analysis; Apache Tika is not an MVP dependency and its benchmark is stopped for the current phase.
- `SP-CONFIG-001` considers PDF and common Office binary documents, then skips them with `UNSUPPORTED_BINARY_DOCUMENT` and explicit coverage impact.
- Layered content classification accepted: Git object kind, recognized signatures, bounded content signals, and non-authoritative extension or `.gitattributes` hints produce `TEXT`, `BINARY`, or `UNDETERMINED`.
- Every skipped item is retained as a persistent structured coverage record with a stable reason and coverage impact; application logs do not serve as the skip source of truth.
- Two-month cloud budget accepted: USD 250 planning envelope, at most USD 180 operating target, USD 70 reserve, and Google Cloud promotional credit as the only currently recorded funding source.
- Repository documentation migrated away from the generic Java web-app template.

## In Progress

- Converting the accepted AI Riser submission architecture into canonical specifications without beginning product implementation.
- Preparing the bounded Eligibility Spike for AI Studio public access, external REST/CORS, authentication handoff, export fidelity, and a minimal Cloud Run endpoint.
- Selecting a safe independent secret-detection benchmark battery and reproducible publication protocol.
- Resolving the pending `DEC-037` revision before the Gitleaks adapter benchmark so repository-controlled suppression behavior matches the user's newer simplification direction; no replacement policy is accepted yet.
- A02 Security Misconfiguration research.
- Selecting and specifying the first family-specific Configuration Awareness slice among Spring Boot, GitHub Actions, and Docker.
- Converting accepted decisions into canonical product and inspection specifications.
- Refining the agent-readable repository context as new decisions are accepted.
- Defining exact non-document binary, archive, link, submodule, dependency, generated-content, lock-file, and user-exclusion policies for the accepted eligibility model.
- Benchmarking the layered classifier implementation, byte-sampling limits, supported encodings, and handling of `UNDETERMINED` content.
- Verifying promotional-credit expiry and eligibility before any deployment commitment or cost-dependent benchmark.

## Next Logical Task

Complete the AI Riser Eligibility Spike specification before changing the implementation phase:

1. verify the AI Studio link in a signed-out browser and record exactly what judges can open and inspect;
2. prove a minimal AI Studio frontend can call a public Cloud Run endpoint under the required browser-origin and CORS policy;
3. define and verify the browser authentication and session handoff for GitHub sign-in and GitHub App installation without exposing tokens to the frontend;
4. verify the supported AI Studio export or transfer workflow and the frozen evidence snapshot;
5. re-check the live completion form, external deadline and timezone, link formats, and source-access expectations;
6. convert successful spike results into the final submission implementation specification and GitHub Issues before explicitly beginning product implementation.

Do not execute the document-extraction benchmark during the MVP phase. Revisit it only if the user explicitly begins the optional Phase 2 Project Understanding capability and accepts the required consent, privacy, safety, and operational contract.

**Reason:** These checks determine whether the accepted AI Studio-plus-Cloud-Run workflow can satisfy the event and the real security product flow before substantial code is built. Configuration Awareness family selection remains queued after the submission-critical vertical slice is technically eligible.

## Current Research Checkpoints

| Area | Status | Canonical file |
|---|---|---|
| A01 Broken Access Control | Checkpoint complete; rules remain candidates | `docs/research/security/A01-BROKEN-ACCESS-CONTROL.md` |
| A02 Security Misconfiguration | General Configuration Awareness checkpoint complete; one security rule accepted; first deep family not selected | `docs/research/security/A02-SECURITY-MISCONFIGURATION.md` |
| Inspection specification | Draft; one accepted rule recorded | `docs/INSPECTION-SPEC.md` |
| Evidence model | Accepted and active | `docs/EVIDENCE-MODEL.md` |
| Finding tracking model | Accepted and active | `docs/FINDING-TRACKING.md` |
| Document extraction adapter | Deferred to optional Phase 2; MVP benchmark stopped | `docs/research/benchmarks/DOCUMENT-EXTRACTION-ADAPTER.md` |
| Cloud budget | Accepted and active; credit details require pre-deployment verification | `docs/CLOUD-BUDGET.md` |
| AI Riser submission architecture | Accepted; Eligibility Spike not yet executed | `docs/research/submission/AI-RISER-VIETNAM-2026.md` |
| Secret detection validation | Independent evidence model accepted; safe battery not yet selected or run | `docs/research/benchmarks/SECRET-DETECTION-VALIDATION.md` |

## Constraints for the Next Agent

- Do not restart A01 research unless new evidence requires revision.
- Do not silently accept remaining A02 candidates.
- Explain one decision at a time in accessible Vietnamese.
- Include the reason, benefit, trade-off, and verification limit for each proposal.
- Do not commit, push, or implement product code without explicit authorization.
- Read `docs/CLOUD-BUDGET.md` before proposing paid services, resource sizing, deployment, or cost-bearing benchmark work.
