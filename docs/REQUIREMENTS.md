> **Document:** Scan Pilot Requirements  
> **File:** `docs/REQUIREMENTS.md`  
> **Version:** v0.8.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Under Review  

# Scan Pilot Requirements

This document records accepted high-level requirements and explicitly marks unresolved details. It is not yet ready for GitHub Issue decomposition.

## Functional Requirements

| ID | Requirement | Status | Reason |
|---|---|---|---|
| FR-001 | A user can connect and select GitHub repositories for monitoring. | Accepted direction | GitHub is the primary integration and source of continuous events. |
| FR-002 | The system can create and process a scan job for a selected repository. | Accepted direction | A real scan job is the foundation of the product workflow. |
| FR-003 | The system can initiate a manual scan and is designed for push, pull request, merge, and scheduled triggers. | Accepted direction | Event-driven monitoring distinguishes Scan Pilot from a one-time reviewer. |
| FR-004 | A scan produces normalized findings with rule ID, location, severity, evidence, verification limits, and remediation guidance. | Accepted direction | Users need actionable, defensible output rather than an unsupported AI statement. |
| FR-005 | Gemini can explain and analyze findings where contextual reasoning is useful. | Accepted direction | Gemini provides accessible explanations and cross-file context while remaining separate from security truth. |
| FR-006 | A user can create or draft a GitHub Issue from a finding. | Accepted direction | Findings must connect to a real remediation workflow. |
| FR-007 | Re-scanning can distinguish at least `OPEN`, `RESOLVED`, and `REGRESSED`. | Accepted direction | Proving improvement and detecting recurrence are core product differentiators. |
| FR-008 | The dashboard can show multiple monitored projects and their latest scan state. | Accepted direction | The product is dashboard-first and multi-project. |
| FR-009 | The system implements `SP-CONFIG-001` against real repository content. | Accepted | This is the first user-approved MUST rule. |
| FR-010 | The system performs Project Discovery and persists a structured Repository Profile with source attribution, scope, source commit, and verification status. | Accepted | Scan Pilot must remember what a repository is without relying on transient AI memory or repeatedly reading everything. |
| FR-011 | The system persists scan checkpoints and finding history so later scans can avoid unnecessary repeated work and maintain finding lifecycle. | Accepted | Continuous monitoring requires durable and inspectable runtime state. |
| FR-012 | The system can create a non-blocking Review Request when missing context could materially change a conclusion. | Accepted | Human input can provide business context that repository evidence cannot prove. |
| FR-013 | A Review Request supports structured choices, `I don't know`, `Another answer`, optional free-text context, and an optional supporting repository-relative path or GitHub link. | Accepted | Users need to add information beyond predefined yes/no answers while the system retains processable structure. |
| FR-014 | The system persists typed, scoped, attributable Evidence Items and links them to the claims, Findings, Repository Profile entries, or Review Requests they support. | Accepted | Every important conclusion must be traceable to its source and repository state. |
| FR-015 | Finding tracking combines rule-specific identity, commit history, Git diff, Evidence Locations, optional semantic context, and re-scan verification. | Accepted | No single source-location field is stable enough to track every security condition across repository changes. |
| FR-016 | Each scan identifies an immutable source snapshot and may use a separate disposable mutable workspace without altering the GitHub repository. | Accepted | Analysis instrumentation and experiments must remain distinguishable from developer changes and repository truth. |
| FR-017 | `SP-CONFIG-001` creates a repository-scoped `SP_SECRET_FP_V1` HMAC-SHA-256 fingerprint from canonical identity fields and exact secret bytes, without persisting the secret. | Accepted | The same credential must remain trackable across commits and locations without exposing it or enabling routine cross-repository correlation. |
| FR-018 | A Finding records lifecycle separately from remediation quality using `ACTION_REQUIRED`, `RISK_CONTAINED`, or `VERIFIED_COMPLETE`, with `NOT_ASSESSED` for insufficient evidence. | Accepted | Users need to distinguish mandatory security action from optional cleanup completeness without treating color as proof of project safety. |
| FR-019 | `SP-CONFIG-001` becomes `RESOLVED` only after a clean current-source re-scan and attributable evidence that the exposed credential was invalidated; historical exposure controls remediation quality rather than blocking resolution. | Accepted | Credential containment closes immediate risk while reachable-history cleanup remains visible and rewarded. |
| FR-020 | Each monitored repository has exactly one `PRIMARY` branch derived from the current GitHub default branch and may have up to two user-selected `SECONDARY` branches in the MVP. | Accepted | The product follows GitHub's repository default while keeping multi-branch monitoring bounded and understandable. |
| FR-021 | Secondary-branch configuration is unlocked only after the primary branch completes a validated full reachable-history baseline; early snapshot Findings and baseline Findings do not make technically complete initialization fail. | Accepted | Verified baseline coverage, rather than a clean security result, proves that repository monitoring was initialized successfully. |
| FR-022 | A GitHub default-branch change automatically changes Scan Pilot's primary without user confirmation and triggers a prioritized scan of the new primary. | Accepted | GitHub is the source of truth for primary-branch identity and Scan Pilot does not maintain a competing custom-primary setting. |
| FR-023 | If a new GitHub default branch is unmonitored while all three slots are occupied, Scan Pilot retains both user-selected secondary branches and removes the former primary from current monitoring without deleting its historical scans, Findings, or evidence. | Accepted | The former primary was system-derived while the secondary branches were explicitly selected by the user. |
| FR-024 | After initialization, monitored branches remain independently scannable; a primary failure reduces coverage but does not cancel secondary scans or hide their Findings. | Accepted | Missing evidence must limit positive claims without suppressing known problems elsewhere. |
| FR-025 | `SP-CONFIG-001` captures an immutable branch HEAD, scans that exact snapshot first, then scans reachable Git history with newer commits prioritized before older commits without assuming a linear graph. | Accepted | Current exposure should be reported early while the full baseline scope remains mandatory and correct for merge histories. |
| FR-026 | Gitleaks is the first MVP secret detector behind a Scan Pilot detector adapter; Scan Pilot remains responsible for safe normalization, redaction, coverage, checkpoints, fingerprinting, and lifecycle. | Accepted | The product contract must not depend directly on one tool's report model or expose raw detector output. |
| FR-027 | A compatible checkpoint may advance incrementally only when it is an ancestor of the captured new HEAD; non-ancestor history or incompatible coverage requires a new baseline in the MVP. | Accepted | Force-pushes, rewrites, and changed scan contracts invalidate assumptions behind a simple commit range. |
| FR-028 | Coverage records bind the repository and branch, captured HEAD, mode and Git scope, detector/version, rule/config version and digest, parser schema, Git commit expectations, scanner telemetry, timestamps, and terminal status; exit code or an unverified zero-commit result cannot prove clean coverage. | Accepted | Scan Pilot must distinguish a real clean scan from an empty, partial, failed, or incompatible scanner run. |
| FR-029 | A baseline checkpoint is persisted or advanced only after coverage validation completes; relevant detector, rule, config, or parser changes can require history backfill or re-verification. | Accepted | Incremental scans are trustworthy only when they build on a compatible verified baseline. |
| FR-030 | Scan Pilot must not automatically validate a discovered credential against its provider; live credential verification is a separate future capability requiring explicit policy and authorization. | Accepted | Detection must not misuse credentials, consume quota, or create unauthorized external requests. |

## Inspection Requirements

- Each official rule must identify its standards basis or explicit Scan Pilot policy.
- Each rule must declare automability and detection method.
- A finding must not reveal detected secret values.
- Weak evidence must produce cautious wording such as `Potential`, not a confirmed-vulnerability claim.
- The main demo flow must use real scan results.
- A re-scan must use stable finding identity sufficient to distinguish resolution and regression.
- Evidence must distinguish `Technical Evidence`, `User Assertion`, and `AI Inference`.
- Claims must use `OBSERVED`, `CORROBORATED`, `USER_ASSERTED`, `INFERRED`, or `UNKNOWN` without treating `UNKNOWN` as safe.
- Confirmed wording must meet the evidence threshold defined by the applicable accepted rule contract.
- Failed, partial, skipped, or incompatible scan output must not be treated as evidence that a Finding is resolved.
- `VERIFIED_COMPLETE` must not be assigned from incomplete current-source or Git-history coverage.
- Remediation quality applies to one Finding and must not be presented as overall project safety or health.

## Security and Isolation Requirements

- The main API process must not execute untrusted repository code.
- Heavy or execution-based scanning must run in an isolated worker environment.
- Credentials and provider keys must not be stored in source code.
- GitHub App and cloud permissions must follow least privilege when their exact permission set is designed.
- Findings, logs, AI prompts, and reports must redact secrets.
- Repository documents and user-provided review content must be treated as untrusted input and must not override Scan Pilot system rules.
- Secret-like content entered into a Review Request must be redacted before persistence, logging, display, or Gemini analysis.
- Credentials used to acquire private repository content must not remain available when untrusted repository code executes.
- Disposable execution environments must not contain backend database, Gemini, or GitHub App private credentials.
- The fingerprint HMAC key must remain inside a trusted component, outside PostgreSQL, source code, logs, and untrusted execution environments.
- Raw secret candidates must not enter persisted findings, job results, queues, metrics, logs, errors, or AI prompts.
- Raw Gitleaks reports are sensitive temporary artifacts; safe fingerprints and normalized redacted evidence must be produced inside the trusted adapter boundary before any asynchronous or persistent handoff, then the raw report must be deleted.

## Usability Requirements

- Security terms must be explained in accessible language.
- Each finding should answer: what happened, why it matters, where the evidence is, how certain the result is, what to do next, and how Scan Pilot will verify the fix.
- The dashboard should prioritize actions rather than only display raw scanner output.
- The dashboard should expose pending Review Requests without blocking scan completion.
- User assertions must remain distinguishable from technical evidence and AI inference.

## Non-Functional Direction

- Scans are asynchronous and must expose clear job state.
- Failures in one scan must not crash the main API or corrupt other project state.
- Webhook processing should be idempotent when designed.
- The product should be deployable on the accepted Google Cloud direction.
- The demo-critical core loop must have repeatable verification before submission.

## Unresolved Requirements

- exact authentication and workspace model;
- exact GitHub App permissions;
- exact scan job states and retry policy;
- exact severity and confidence scales;
- exact scoring formula and status thresholds;
- exact retention policy for repository checkouts and scan artifacts;
- exact BYOK requirements;
- exact optional confidence scale beyond the accepted verification statuses;
- Review Request authorization, expiry, invalidation, and notification behavior;
- exact fingerprint key service, access policy, rotation schedule, and retirement operation;
- exact sandbox technology and controls for execution-based rules;
- source snapshot retention policy beyond the accepted default of no long-term full-source retention;
- final V1 rule count and supported secret providers;
- exact deployment service choices within Google Cloud.
