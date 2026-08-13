> **Document:** Scan Pilot Accepted Decisions  
> **File:** `docs/DECISIONS.md`  
> **Version:** v1.10.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Active  

# Scan Pilot Accepted Decisions

Only user-accepted decisions belong in this document. Research candidates remain outside the official product contract until accepted.

## DEC-001 — Canonical development documentation is Markdown

**Status:** Accepted

Core development documentation is stored as Markdown in the repository. PDF or DOCX may be generated later for external presentation or submission.

**Reason:** Repository Markdown is versionable, reviewable, linkable, and available to every local agent without relying on chat history.

## DEC-002 — Product is dashboard-first

**Status:** Accepted

The core product is a continuous multi-project monitoring dashboard. AI assistant or chat is supporting UX.

**Reason:** The main user problem is comparing project health and acting on findings across repositories, which requires persistent workflow and state rather than a chat-only interface.

## DEC-003 — GitHub is the primary integration

**Status:** Accepted

GitHub provides the full continuous workflow. ZIP may later support one-time scanning.

**Reason:** GitHub events, repository history, pull requests, Issues, and re-scans are required to demonstrate continuous monitoring and regression detection.

## DEC-004 — Source monitoring is event-driven

**Status:** Accepted

Scan Pilot monitors push, pull request, merge, scheduled, and manual scan events. V1 does not require keystroke-level local monitoring.

**Reason:** Repository events provide stable, reviewable checkpoints and avoid requiring an editor plugin for the MVP.

## DEC-005 — Backend is a RESTful modular monolith

**Status:** Accepted

The React frontend communicates with a Spring Boot modular-monolith backend through REST APIs. Heavy scans run asynchronously in isolated workers.

**Reason:** A modular monolith keeps deployment and development manageable for one solo developer while preserving clear module boundaries. Worker isolation prevents untrusted scan workloads from running inside the main API process.

## DEC-006 — Core technology direction

**Status:** Accepted

- React + TypeScript + Vite
- Spring Boot 3 + Java 21
- PostgreSQL
- GitHub App
- Gemini API
- isolated scan workers
- Google Cloud deployment direction

**Reason:** This stack supports the intended dashboard, repository workflow, Gemini integration, and Google-oriented submission while remaining familiar and deployable as an MVP.

## DEC-007 — AI does not define security truth

**Status:** Accepted

Security requirements come from recognized standards or explicit Scan Pilot policy. AI assists detection and analysis but is not the sole authority.

**Reason:** Model output can be incomplete or incorrect. Combining standards and evidence with AI reasoning produces more defensible findings.

## DEC-008 — Requirement, rule, scan, and finding are separate

**Status:** Accepted

```text
Standard Requirement
→ Scan Pilot Rule
→ Repository Scan
→ Finding
```

**Reason:** Separating these concepts prevents a broad standard statement from becoming an unsupported scanner alert and lets one rule map to evidence from a specific repository.

## DEC-009 — Standards references are versioned

**Status:** Accepted

Use identifiers such as `OWASP ASVS v5.0.0-8.2.2` rather than ambiguous unversioned references.

**Reason:** Requirement identifiers and wording may change between releases.

## DEC-010 — Do not overclaim compliance

**Status:** Accepted

Scan Pilot must not claim full ASVS or other standards compliance unless verified coverage supports the claim.

**Reason:** Partial static or AI-assisted checks cannot prove that every relevant requirement is satisfied.

## DEC-011 — Automability and detection method are separate

**Status:** Accepted

Automability:

- `FULL`
- `PARTIAL`
- `MANUAL`

Detection methods:

- `STATIC`
- `EXECUTION`
- `AI`
- `HYBRID`

**Reason:** A rule can use several techniques while still verifying only part of a security requirement.

## DEC-012 — Preserve the REGRESSED lifecycle state

**Status:** Accepted

A previously resolved issue that returns must be distinguishable from a new unrelated finding.

**Reason:** Detecting regression is central to continuous health monitoring and demonstrates improvement or degradation over time.

## DEC-013 — A01 authorization research checkpoint

**Status:** Accepted research checkpoint

MUST candidates:

- `SP-AUTHZ-001` Function-Level Authorization Verification
- `SP-AUTHZ-002` Object-Level Authorization Verification
- `SP-AUTHZ-003` Server-Side Authorization Enforcement

SHOULD candidate:

- `SP-AUTHZ-004` Field-Level Authorization Verification

These remain research candidates, not official inspection rules.

**Reason:** A01 research identified strong V1 opportunities, but the exact detection and evidence contract still requires review before implementation.

## DEC-014 — Official product name is Scan Pilot

**Status:** Accepted

The former working title `VibeGuard` is replaced by `Scan Pilot`. New internal rule identifiers use the `SP-` prefix. Legacy `VG-` identifiers in handoff material map to the corresponding `SP-` research identifiers.

**Reason:** The user explicitly selected Scan Pilot as the official product name, so one canonical name prevents agent and documentation drift.

## DEC-015 — MVP must run against real repositories

**Status:** Accepted

The main MVP flow and accepted rules must execute on real repository content and produce evidence. Mocked findings may support UI development but cannot be the primary demo evidence.

**Reason:** The product must demonstrate genuine technical capability, not only a dashboard prototype.

## DEC-016 — SP-CONFIG-001 is a MUST MVP rule

**Status:** Accepted

```text
SP-CONFIG-001 — Source Code Secret Exposure
Priority: MUST
Automability: PARTIAL
Detection: STATIC
```

The rule detects likely credentials or API secrets committed to source or build artifacts, redacts secret values, avoids treating obvious placeholders as confirmed secrets, and recommends revocation plus replacement rather than deletion alone.

**Reason:** Secret exposure is a real, high-impact problem common in AI-assisted development. It can be scanned deterministically, demonstrated end to end, and verified through a re-scan. Automability is `PARTIAL` because a scanner cannot always distinguish a live secret from sample data with certainty.

## DEC-017 — MVP scope favors depth over rule count

**Status:** Accepted

The MVP should provide a focused set of strong rules and a complete GitHub-to-re-scan workflow rather than many shallow or mock-only rules.

**Reason:** For the August 2026 submission and one solo developer, end-to-end reliability, evidence, remediation, and lifecycle tracking create more product value than a large unverified rule catalog.

## DEC-018 — Google and Gemini API key detection policy

**Status:** Accepted

For the Google/Gemini portion of `SP-CONFIG-001`, Scan Pilot will:

- use a specialized secret detector to recognize Google API key candidates;
- combine the candidate format with nearby Google/Gemini context and repository location;
- distinguish a general Google API key candidate from a likely Gemini API key candidate;
- treat committed source, configuration, frontend, workflow, container, and Git-history locations as exposure evidence;
- exclude obvious placeholders, redacted examples, and environment-variable references from confirmed findings;
- never automatically call a Google API using a discovered key merely to verify whether it is active;
- avoid automatically assigning `Critical` severity when activity, restrictions, and impact are unknown;
- recommend replacement, deployment of the replacement, revocation of the exposed key, usage/billing review, secure server-side storage, and re-scan.

Initial wording:

- format evidence only: `Potential Google API Key Exposure`;
- format plus Gemini context: `Potential Gemini API Key Exposure`;
- committed credential evidence: `Google/Gemini API Key Exposed in Repository`.

The initial severity direction is `High`. Promotion to `Critical` requires later accepted evidence criteria showing greater confidence and impact.

**Reason:** Google warns against committing API keys or placing them in client code, but a Google-shaped key does not by itself prove Gemini usage, current activity, or unrestricted impact. Contextual classification produces stronger findings while avoiding unsafe credential use and exaggerated severity.

## DEC-019 — Repository context and scan state are persistent structured data

**Status:** Accepted

When a repository is connected, Scan Pilot performs Project Discovery to create a Repository Profile. Deterministic extraction is used first for manifests, repository structure, technology signals, and other machine-readable evidence. Gemini may summarize a selected and bounded set of documents where language understanding is useful.

The Repository Profile, scan checkpoints, finding history, and user-provided project context are persisted in PostgreSQL. Repository Markdown is untrusted evidence rather than runtime state, and Scan Pilot does not automatically write or commit a generated project-memory file to the user's repository.

Each profile claim must retain its source, scope, source commit, and verification status. A profile is refreshed when relevant source files change or the extractor version changes.

**Reason:** Persistent structured state lets Scan Pilot remember what a project is and what has already been scanned without repeatedly asking Gemini or treating Markdown as a database. Source attribution limits unsupported AI inference and makes detected project context reviewable.

## DEC-020 — Human review is asynchronous, structured, and extensible

**Status:** Accepted

When missing context could materially change a security or project conclusion, Scan Pilot creates a dashboard Review Request without blocking scan completion. The request may provide structured choices, `I don't know`, `Another answer`, optional free-text context, and an optional repository-relative file or GitHub link as supporting context.

Every response records its author, time, applicable repository and source commit, and verification status. User-provided content is a user assertion and does not automatically become technical evidence or close a finding. Secret-like input must be redacted before persistence, logging, display, or Gemini analysis.

**Reason:** A human-in-the-loop can supply business context that source code cannot prove, while asynchronous review preserves continuous scanning. Combining structured answers with free-form additions avoids forcing every decision into yes/no choices and still supports reliable processing and auditability.

## DEC-021 — Evidence is typed, scoped, attributable, and preserved

**Status:** Accepted

Scan Pilot distinguishes `Technical Evidence`, `User Assertion`, and `AI Inference`. Each persisted Evidence Item identifies its source, repository state, scope, producer and version, supported claim, and verification status.

The accepted verification statuses are `OBSERVED`, `CORROBORATED`, `USER_ASSERTED`, `INFERRED`, and `UNKNOWN`. `UNKNOWN` does not mean safe. Verification status is separate from severity, business impact, automability, detection method, and any future confidence score.

Sufficient Technical Evidence may support confirmed wording only according to an accepted rule contract. Incomplete evidence produces cautious wording. A User Assertion or AI Inference alone cannot silently erase, resolve, or replace an observed technical Finding. Historical evidence is preserved; corrections supersede rather than rewrite its provenance.

The normative model is defined in `docs/EVIDENCE-MODEL.md`.

**Reason:** Findings, Repository Profiles, and Review Requests must remain auditable and must not treat scanner observations, human statements, and AI reasoning as equivalent forms of proof.

## DEC-022 — Finding tracking combines stable identity with repository history

**Status:** Accepted

Finding tracking combines a rule-specific fingerprint or semantic identity with repository commit history, Git diff, Evidence Locations, optional semantic context, and re-scan verification. File path, line, column, branch, current commit, or detector version alone is not durable Finding identity.

Each scan keeps an immutable source snapshot as its comparison baseline and may create a disposable mutable workspace for temporary markers, instrumentation, or experiments. Workspace changes do not affect the GitHub repository and do not become persistent identity. GitHub remains the source of truth, and complete source snapshots are not retained long-term by default.

Untrusted repository execution is optional by rule and occurs only in an isolated environment without backend credentials. Static rules such as the core `SP-CONFIG-001` path do not execute repository code when execution is unnecessary. Automatic remediation and `Verify Fix Before PR` remain future directions, not accepted MVP requirements.

The normative tracking model is defined in `docs/FINDING-TRACKING.md`.

**Reason:** Stable identity determines whether an issue is the same, while commit history, diff, locations, semantic context, and re-scan evidence explain how it changed and whether the condition remains. Separating immutable source from a mutable disposable workspace permits safe analysis experiments without altering the user's repository.

## DEC-023 — SP-CONFIG-001 uses a scoped keyed fingerprint

**Status:** Accepted

`SP-CONFIG-001` uses the versioned `SP_SECRET_FP_V1` scheme: HMAC-SHA-256 over a canonical length-prefixed input containing the scheme domain, stable workspace ID, stable repository ID, rule family, stable credential family, and exact detected secret bytes.

For Google and Gemini candidates, identity uses the stable `GOOGLE_API_KEY` family. Likely Gemini usage is contextual classification and does not change the Finding identity. Secret bytes remain case-sensitive and are not trimmed or otherwise normalized; only detector-confirmed surrounding syntax is excluded.

The fingerprint excludes source location, commit, branch, detector or rule version, variable name, contextual classification, severity, and lifecycle state. The full HMAC output is persisted with scheme and key version, while the raw secret is never persisted or sent through result, queue, log, metric, error, or AI-prompt paths. Obvious placeholders, redacted samples, and environment references do not receive persisted secret fingerprints.

The HMAC key is available only to a trusted fingerprint component and never to untrusted repository execution. Rotation must preserve the ability to match retained key versions and attach a new-version alias before an old key version is retired. The exact Google Cloud key service, rotation schedule, and retirement operation remain deployment decisions.

The normative contract is recorded in `docs/FINDING-TRACKING.md` and applied by `docs/INSPECTION-SPEC.md`.

**Reason:** A keyed, repository-scoped fingerprint recognizes the same credential across commits and locations without storing it or enabling routine cross-repository correlation. Separating stable credential family from contextual usage prevents a Google key from becoming a different Finding merely because later evidence indicates Gemini usage.

## DEC-024 — Git history uses authorized coherent checkpoints

**Status:** Accepted

Agents do not commit after each file edit or individual accepted decision. Related decisions, specifications, documentation, or implementation work are grouped into a coherent checkpoint that can be reviewed and restored as one meaningful unit.

A checkpoint may be proposed when its intended scope is complete, related documentation and metadata are synchronized, applicable verification has run, and the diff has been inspected for unrelated files and secrets. The agent may proactively recommend committing and pushing when a checkpoint is ready or when an off-device GitHub copy would materially reduce loss risk.

Every proposal states the scope, readiness reason, verification, limitations, affected files, and suggested Conventional Commit message. A proposal grants no authority: commit requires explicit user authorization, and push requires explicit user authorization separate from commit permission. Important authorized checkpoints should be pushed to GitHub to preserve an off-device copy.

**Reason:** Per-file or per-conversation commits create noisy history, while waiting indefinitely leaves meaningful work only on one laptop. Coherent, reviewed checkpoints balance readable history, recoverability, and user control over externally visible Git actions.

## DEC-025 — Solo development uses a lightweight branch workflow

**Status:** Accepted

Scan Pilot is a solo project. Keep `main` stable and use one working branch for each coherent large workstream. Research and specification documents may share a branch such as `codex/docs-research-specification`; a large feature should use a separate branch such as `codex/secret-scanning`. Small documentation corrections may remain on the current working branch.

Pull requests are optional self-review checkpoints for large features or major changes and are not required for every small documentation edit. Do not add `develop`, `release`, or `hotfix` branches unless a later accepted need justifies the added workflow.

Branch creation, commit, push, merge, and pull-request actions remain subject to explicit user authorization where required by the repository safety rules.

**Reason:** A working branch protects stable `main` from experimental or agent-generated changes. A lightweight model gives one developer that recovery and review boundary without the coordination overhead of a multi-team Git flow.

## DEC-026 — Finding lifecycle and remediation quality are separate

**Status:** Accepted

Finding lifecycle remains `OPEN → RESOLVED → REGRESSED`. A separate remediation-quality assessment describes how completely an accepted fix has been finished without adding more lifecycle states.

The three remediation-quality levels are:

- `ACTION_REQUIRED`: at least one mandatory security action remains;
- `RISK_CONTAINED`: the immediate credential risk has been contained, but repository cleanup remains incomplete;
- `VERIFIED_COMPLETE`: all applicable remediation steps that Scan Pilot can verify within the accessible scan scope are complete.

`NOT_ASSESSED` is a neutral insufficient-evidence state, not a fourth quality level. Color is presentation only; APIs and persisted records use semantic labels. These labels apply to one Finding and do not represent overall project health or safety.

For `SP-CONFIG-001`, a Finding may become `RESOLVED` after a successful re-scan verifies that the secret is absent from current source and the exposed credential is confirmed invalidated. MVP confirmation may be `USER_ATTESTED`; a future authorized provider integration may produce `PROVIDER_VERIFIED`. User confirmation alone cannot replace the clean-source re-scan, and Scan Pilot never uses a discovered credential for unauthorized live validation.

If reachable Git history still contains the invalidated credential, the Finding is `RESOLVED` with `RISK_CONTAINED`. If an adequate scan verifies that current source and accessible Git history are clean and the credential is invalidated, it is `RESOLVED` with `VERIFIED_COMPLETE`. Failed, partial, skipped, incompatible, shallow, or insufficiently authorized history scans cannot produce `VERIFIED_COMPLETE` and instead yield `NOT_ASSESSED` for remediation quality when no reliable level can be assigned.

**Reason:** Lifecycle answers whether mandatory security action remains, while remediation quality communicates whether cleanup reached the best state Scan Pilot can verify. This avoids forcing risky Git-history rewrites merely to close contained credential risk while still rewarding professional repository cleanup and preserving honest verification limits.

## DEC-027 — PRIMARY always mirrors the GitHub default branch

**Status:** Accepted

Each monitored repository has exactly one `PRIMARY` branch and may have up to two user-selected `SECONDARY` branches in the MVP. `PRIMARY` is derived from the current GitHub default branch; users cannot select a custom primary or directly remove the current primary from monitoring. Scan Pilot scans and presents the primary with priority, while all monitored branches remain independently scannable and continue to participate in repository assessment.

When GitHub changes the default branch, Scan Pilot updates `PRIMARY` automatically without asking the user:

- if the new default is already monitored, it is promoted and the old primary is demoted to secondary;
- if the new default is not monitored and capacity remains, it is added as primary and the old primary is retained as secondary;
- if the new default is not monitored and all three slots are occupied, the new default is added as primary, both user-selected secondary branches are retained, and the old primary is removed from the current monitored scope.

The new primary role takes effect immediately and receives a prioritized scan. Until that scan completes, its assessment is unavailable and repository coverage is incomplete. A failed primary scan does not cancel independently runnable secondary scans. Removing an old primary from current scope stops future monitoring for that branch but does not delete prior scans, Findings, Evidence Items, or audit history.

**Reason:** GitHub remains the single source of truth for which branch represents the repository by default, while the two secondary slots preserve branches explicitly selected by the user. Automatic synchronization avoids a competing primary-branch configuration. Preserving historical records prevents an external scope change from erasing security evidence or making repository status appear healthier without a valid scan of the new primary.

## DEC-028 — Secret scanning uses a validated snapshot-and-history pipeline

**Status:** Accepted

For the `SP-CONFIG-001` MVP path, Git owns commit-graph and range traversal, Gitleaks is the first deterministic secret detector, and Scan Pilot owns orchestration, trusted normalization and redaction, coverage validation, checkpoints, `SP_SECRET_FP_V1`, and Finding lifecycle. Gitleaks is accessed through a detector adapter so a later accepted detector can be added or substituted without redefining the Scan Pilot rule contract.

Each branch scan captures an immutable HEAD SHA and separates two evidence flows:

1. Current Snapshot Scan examines the exact captured HEAD first so current exposure can be reported early.
2. Git History Scan examines reachable commit patches, prioritized from newer history toward older history using Git's graph rather than assuming a linear `HEAD~N` chain.

Evidence from both flows may belong to the same Finding. The early snapshot result does not reduce the required baseline scope and cannot support a whole-history clean claim.

A new branch follows the accepted onboarding option B: snapshot findings may be shown as soon as they are safely normalized, but the branch becomes `MONITORED` and its initialization completes only after its full reachable-history baseline is validated. For the initial primary branch, secondary-branch configuration remains locked until this validated baseline completes. Detected Findings do not make a technically complete baseline fail.

After a valid baseline, an incremental history scan may inspect `old_checkpoint..new_head` only when the compatible checkpoint is an ancestor of the captured new HEAD. A non-ancestor result caused by force-push, history rewrite, or incompatible state requires the MVP to run a new full-history baseline. A checkpoint advances only after compatible coverage is validated as complete.

Coverage records include repository and branch identity, captured HEAD SHA, scan mode and Git scope, detector name and version, Scan Pilot rule/config version and digest, report/parser schema version, expected Git commit count, scanner telemetry, timestamps, and terminal completion or failure state. Expected Git commits are not compared directly with a detector's reported commit count when the detector omits commits without relevant additions. Exit code alone is not proof of a complete scan. A baseline that scans zero commits or has unknown scope is incomplete or unavailable; an explicitly verified empty incremental range may be recorded as no change.

Relevant detector, rule, configuration, or parser changes can invalidate prior coverage and require backfill or re-verification. Gitleaks raw output is treated as sensitive temporary data: Scan Pilot uses detector redaction as defense in depth, computes the safe fingerprint and normalized evidence inside the trusted adapter boundary, prevents raw secret fields from entering persistence or asynchronous paths, and deletes the temporary raw report.

Automatic provider verification is not part of this pipeline. Scan Pilot does not use a discovered credential to call its provider. TruffleHog-style live verification remains a separate future capability requiring an explicit policy and authorization boundary.

**Reason:** The split gives users a fast warning about current code while preserving an honest, reproducible history baseline. Explicit coverage and checkpoint validation prevent an empty, partial, failed, or incompatible detector run from being mislabeled clean. A detector adapter keeps product semantics under Scan Pilot control instead of coupling Finding identity and lifecycle to one CLI tool.

## Intentionally Open Decisions

- exact scoring formula and status thresholds;
- exact optional confidence scale beyond the accepted verification statuses;
- exact final V1 rule count;
- exact detectors for rule families beyond the accepted Gitleaks-backed `SP-CONFIG-001` path;
- exact fingerprint key service, access policy, rotation schedule, and retirement operation;
- exact semantic matching strategies for later rule families;
- exact sandbox or VM technology for rules that execute repository code;
- exact evidence threshold for promoting a Google/Gemini key finding to `Critical`;
- exact Review Request expiry, invalidation, and authorization policy;
- exact ASVS coverage presentation;
- exact BYOK encryption and storage;
- exact queue/cache technology;
- final deployment topology within the accepted Google Cloud direction.
