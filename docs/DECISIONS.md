> **Document:** Scan Pilot Accepted Decisions  
> **File:** `docs/DECISIONS.md`  
> **Version:** v1.22.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-15  
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

## DEC-029 — Secret scanning classifies all Git-tracked content for eligibility

**Status:** Accepted

For `SP-CONFIG-001`, every Git-tracked content item within the selected branch, commit, and history scope is considered for scan eligibility. `CONSIDERED` and `SCANNED` are distinct states:

- `CONSIDERED` means Scan Pilot identified the item and evaluated it against the applicable content policy;
- `SCANNED` means the detector successfully processed the eligible content;
- an item that cannot or must not be processed is recorded as skipped with a stable reason code and coverage impact.

Scan Pilot does not restrict secret detection by default to conventional source directories. Paths such as documentation, examples, tests, workflows, and generated frontend output are not excluded merely because they are outside `src/`. At the same time, Scan Pilot does not promise successful parsing of every repository byte.

Policy-based exclusions and technical limits must be explicit, versioned, and visible in coverage. A skipped item cannot be silently counted as scanned, and a no-finding result applies only to content processed successfully within the recorded scope. Exact policy for binary content, archives, symbolic links, submodules, dependency trees, generated output, lock files, and user-configured exclusions remains unresolved; full-file size handling is now defined separately by `DEC-036`.

**Reason:** Secret values can appear outside application source folders, while production scanners still need bounded resource and format policies. Eligibility classification preserves broad repository awareness without creating the false claim that every content type was analyzed. Explicit skip reasons make coverage auditable and allow support to expand later without changing the core model.

## DEC-030 — Project Discovery uses an isolated document-extraction adapter

**Status:** Superseded for the MVP by `DEC-033`; retained as Phase 2 research direction

Project Discovery accesses repository documents through a Scan Pilot-owned `Document Extraction Adapter`. Document parsing runs in an isolated scan worker rather than in the Spring Boot API process. Extracted content is treated as untrusted and potentially sensitive input; it must be bounded and secret-redacted before it may be sent to Gemini or persisted as derived project context.

Apache Tika is the first benchmark candidate because it is compatible with the accepted Java direction and supports detection and text or metadata extraction across many document formats. This decision does not select Tika as a production dependency. Production selection requires a benchmark against representative repository documents and failure cases.

Alternative implementations, including Docling or Google Cloud Document AI, may be evaluated behind the same adapter without changing the Project Discovery contract. The initial MVP format scope is defined separately in `DEC-031`. Parser deployment form, size and page limits, timeout and memory limits, retention policy, and fallback behavior remain unresolved.

**Reason:** The adapter keeps Scan Pilot's product contract independent from one parser, while process isolation limits the effect of malformed or hostile documents. Benchmarking before production selection avoids claiming PDF, DOCX, OCR, or layout support that has not been verified on the MVP environment.

## DEC-031 — Project Discovery uses a balanced MVP document scope

**Status:** Superseded by `DEC-033`

Project Discovery inventories every content item within its captured repository scope before deciding whether semantic extraction is supported. The MVP processing target is:

- text documentation, configuration, manifests, and other supported machine-readable text use deterministic readers or structured parsers first;
- DOCX and text-native PDF are sent through the isolated `Document Extraction Adapter`;
- image-only or scanned PDF is recognized as requiring OCR and is not semantically understood by the MVP;
- PPTX, XLSX, and other unsupported binary document formats are inventoried but not semantically extracted unless a later decision expands the scope;
- every unsupported, OCR-required, failed, bounded, or skipped item retains an explicit outcome and reason rather than being silently counted as understood.

Gemini receives only selected, bounded, secret-redacted extracted content and may classify or summarize it. Source files, extracted text, and AI output remain distinct evidence stages. A successful inventory does not imply successful semantic extraction.

**Reason:** This scope provides useful Project Discovery across common repository documentation without making OCR, computer vision, or complex office-layout processing a prerequisite for the solo MVP. It creates a measurable target for the Apache Tika benchmark while preserving honest coverage for every item outside the supported scope.

## DEC-032 — Cloud design uses a two-month USD 250 planning envelope

**Status:** Accepted

Scan Pilot's approximately two-month development and public-demo cloud plan uses:

- USD 250 as the total planning envelope;
- at most USD 180 as the expected operating target;
- USD 70 as protected reserve for variance, recovery, and final demo needs.

The only currently recorded funding source is Google Cloud promotional credit. A user-supplied billing screenshot showed a nominal USD 300-equivalent Free Trial balance plus four nominal USD 10-equivalent monthly Developer Program credits, but expiry, eligibility, and application order were not visible. Architecture must not assume the full nominal amount can be pooled or used for every service. New funding or free-credit sources are unavailable for planning until the user reports them and the canonical budget is updated.

Initial cost guardrails prefer scale-to-zero, one maximum scan-worker instance, and a small single-zone non-HA Cloud SQL instance without replicas. The earlier document-parser concurrency guardrail no longer applies to the MVP because `DEC-033` defers that subsystem. Cumulative billing notifications are required at USD 25, 50, 100, 150, 180, and 220. Alerts are not hard spending caps; service limits and resource review remain required.

Any paid-service proposal must justify the MVP need, current two-month estimate, credit assumptions, containment controls, cheaper alternative, expiry behavior, and verification limits. Promotional credit does not justify scope expansion or weaker security and privacy boundaries.

**Reason:** The available credit is sufficient for a carefully bounded MVP, but Cloud SQL, always-on capacity, scan concurrency, logging, managed parsing, and AI usage can consume it unexpectedly. A canonical envelope lets every agent make consistent architecture trade-offs while preserving a reserve and avoiding unsupported assumptions about promotional credit.

## DEC-033 — Binary document semantic extraction is deferred beyond the MVP

**Status:** Accepted

Scan Pilot MVP inventories PDF, DOC/DOCX, XLS/XLSX, and PPT/PPTX content items but does not extract or semantically analyze their internal content. Project Discovery records path, detected content type, size, content hash, and source commit where available. Its status model keeps successful inventory separate from semantic support:

```text
inventory_status: INVENTORIED
semantic_analysis: NOT_SUPPORTED_MVP
```

Project Discovery prioritizes repository text and machine-readable evidence, including Markdown, manifests, configuration, CI/CD, IaC, and source-derived technology signals. Selected bounded and secret-redacted text may still be classified or summarized by Gemini, but repository assertions remain weaker than contradictory technical evidence.

For the `SP-CONFIG-001` MVP security baseline, these binary document families are `CONSIDERED` and then `SKIPPED` with reason `UNSUPPORTED_BINARY_DOCUMENT`. They are not counted as scanned, and coverage must disclose that their internal content was not inspected. Other binary, archive, link, submodule, dependency, generated-content, lock-file, and user-exclusion policies remain unresolved; accepted full-file size limits are governed by `DEC-036`.

Apache Tika is not an MVP dependency and its benchmark is stopped for the current phase. `DEC-030` remains only a possible Phase 2 adapter direction, and `DEC-031` is superseded. A future optional Project Understanding capability may allow the user to select additional PDF or Office documents for analysis, but it requires a new accepted consent, privacy, extraction, AI-processing, safety, and operational contract. It is not part of security-baseline completion.

**Reason:** Professional AppSec scanners prioritize code, manifests, configuration, and supported machine-readable formats; GitLab Secret Detection explicitly excludes PDF and common Office documents for performance and low expected secret likelihood, while Snyk Code defines supported source formats and code-graph analysis. More importantly for Scan Pilot, document parsing adds an isolated parser, malformed/encrypted/archive failure modes, benchmark and UI work, prompt-injection and redaction boundaries, and imprecise evidence locations without materially advancing the accepted secret-scanning vertical slice. Deferral reduces attack surface and solo-MVP delivery risk while preserving honest inventory and a future extension path.

## DEC-034 — Content classification is layered and every skip is persisted

**Status:** Accepted

Before `SP-CONFIG-001` decides whether a Git-tracked item is eligible for detection, Scan Pilot classifies its content through layered evidence rather than trusting a filename extension or repository-controlled `.gitattributes` declaration alone. The classifier evaluates, as applicable:

1. Git object kind;
2. recognized content or file signatures;
3. bounded content signals such as text decoding and binary-byte characteristics;
4. filename extension and `.gitattributes` only as supporting hints.

The normalized content classification is:

```text
TEXT
BINARY
UNDETERMINED
```

A classification conflict or insufficient evidence produces `UNDETERMINED`; it must not be silently treated as text, binary, successfully scanned, or clean. Exact classifier library, sampling thresholds, supported encodings, and handling of each non-document binary family remain unresolved and require implementation benchmarking.

For every item that becomes `SKIPPED`, Scan Pilot persists a structured coverage record containing at least repository and branch scope, scan ID, captured commit or object identity where applicable, repository-relative path, content classification, processing outcome, stable reason code, size where known, applicable rule/config version, and coverage impact. This product record is distinct from an application log. Logs may contain safe operational diagnostics, but they are not the source of truth for which items were considered, scanned, or skipped and must never contain detected secret values.

**Reason:** Extensions and repository attributes can be mistaken, inconsistent, or deliberately misleading, while any single content heuristic has blind spots. Layered evidence makes classification more defensible without claiming perfect format detection. Persisting each skip makes coverage auditable, lets the user see which files were not inspected and why, and prevents log retention or aggregation from erasing the scan contract. The trade-off is additional classifier and storage work; the verification limit is that malformed, encrypted, polyglot, uncommon-encoding, or otherwise ambiguous content can remain `UNDETERMINED` until later support is accepted.

## DEC-035 — Java backend uses Apache Maven

**Status:** Accepted

Scan Pilot uses Apache Maven as the canonical build and dependency-management tool for its Java 21 and Spring Boot 3 backend. Backend dependencies, including the future Google Gen AI SDK adapter dependency, will be declared through Maven rather than maintaining a parallel Gradle build for the same backend.

This decision selects the build tool only. Exact Maven version, Maven Wrapper policy, module layout, plugin versions, dependency versions, build profiles, and CI commands remain unresolved until the implementation structure is designed and verified.

**Reason:** Maven provides one reproducible dependency and build contract for the Java backend, fits Spring Boot and the published Java dependencies under consideration, and avoids maintaining two competing Java build systems in a solo project. The trade-off is Maven-specific project configuration and lifecycle conventions; the verification limit is that no `pom.xml` or executable backend exists yet, so compatibility and build reproducibility cannot be tested until implementation begins.

## DEC-036 — Secret scanning uses two accepted per-file size limits

**Status:** Accepted

For otherwise eligible supported text content, `SP-CONFIG-001` uses two initial full-file size limits, measured in mebibytes where `1 MiB = 1,048,576 bytes`:

- Continuous Monitoring scans files up to and including `10 MiB`. A larger file is `CONSIDERED` then `SKIPPED` with reason `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`; it must not be represented as clean or successfully scanned.
- Release Assessment may reuse compatible evidence for files already scanned and performs a full-file scan for otherwise eligible text up to and including `50 MiB`. Content above that hard ceiling is `SKIPPED` with reason `RELEASE_FILE_SIZE_CEILING_EXCEEDED`; when that content belongs to required release coverage, the assessment is `INCOMPLETE`, not `PASS`, `CLEAN`, or `VERIFIED_COMPLETE`.

The limits do not override format policy: PDF and common Office binary documents remain governed by `DEC-033`, and other unsupported content remains skipped under its applicable reason. The MVP does not use partial-prefix results, chunk checkpoints, or progressive byte-range resumption to claim that a large file was scanned. A monitoring skip may identify the item as eligible for later release verification, but it is not a scheduled `DEFERRED` job unless a job is actually created.

These are versioned initial operational limits, not claims of detector capacity. Before implementation is considered verified, Scan Pilot must benchmark Gitleaks against representative eligible text at `1`, `10`, `25`, `50`, and `100 MiB`, measuring completion, time, peak memory, and timeout behavior in the intended worker boundary. Any later threshold change requires an explicit versioned policy change and updated coverage semantics rather than a silent configuration edit.

**Reason:** Continuous Monitoring needs a predictable low-cost limit, while a release-oriented verification path should inspect materially larger eligible files without removing the worker's absolute safety boundary. The `10 MiB` and `50 MiB` tiers are simple to explain and avoid chunk lifecycle complexity. The benefit is bounded recurring work with transparent stronger verification when requested. The trade-off is that a secret in a file between the two limits may remain undetected until Release Assessment, and content above the release ceiling can keep that assessment incomplete. The verification limit is that Scan Pilot has not yet run the required worker benchmark, and this decision does not by itself settle the broader MVP delivery scope, triggers, build or artifact checks, or full completion contract of Release Assessment.

## DEC-037 — Scan Pilot owns the trusted Gitleaks detection policy

**Status:** Accepted

For the `SP-CONFIG-001` security baseline, Scan Pilot owns the detector version, trusted rule configuration, configuration digest, enabled rules, exact-byte size policy, timeout, redaction, and suppression lifecycle. Repository-controlled `.gitleaks.toml`, `.gitleaksignore`, inline `gitleaks:allow`, and inherited `GITLEAKS_CONFIG` or `GITLEAKS_CONFIG_TOML` values must not silently suppress or redefine baseline detection.

The adapter explicitly supplies a trusted configuration, removes unapproved Gitleaks configuration variables from the worker environment, requests full detector redaction with `--redact=100`, and invokes behavior equivalent to `--ignore-gitleaks-allow`. Because the current Gitleaks CLI also discovers `.gitleaksignore` from the target repository, the exact isolation technique must be benchmarked and verified without modifying the immutable source snapshot. If repository suppression cannot be proven inactive, coverage is incomplete rather than clean.

Repository Gitleaks configuration and ignore files may still be inventoried and shown as untrusted repository evidence. A legitimate false-positive or risk-acceptance workflow belongs to Scan Pilot's own attributed, reviewable suppression model; exact authorization, scope, expiry, and invalidation behavior remain unresolved. The adapter must pin an exact tested Gitleaks version or immutable artifact digest and must not use a floating `latest` reference.

Scan Pilot applies the accepted `MiB` thresholds using exact blob bytes before detector invocation. Gitleaks' `--max-target-megabytes` uses decimal megabytes in the reviewed source and may be used only as defense in depth; it is not the source of truth for `DEC-036` coverage.

**Reason:** A repository is the untrusted subject being assessed and must not be able to redefine or disable the assessor. Product-owned policy makes results reproducible across repositories and preserves honest coverage. The benefit is resistance to silent detector suppression and configuration drift. The trade-off is that Scan Pilot may surface findings a repository's local Gitleaks workflow intentionally ignores and therefore needs its own suppression UX. The verification limit is that the adapter has not yet demonstrated a safe `.gitleaksignore` isolation method, exact pinned version, command matrix, or end-to-end report cleanup in the intended worker.

## DEC-038 — Configuration Awareness is a dedicated product capability

**Status:** Accepted

Scan Pilot treats repository configuration as a dedicated product capability rather than as undifferentiated text or a filename-extension list. Project Discovery inventories configuration artifacts and classifies them by technical family and repository role using deterministic evidence such as path conventions, recognized structure or schema, content signals, and detected technology context. AI may assist only when deterministic evidence remains ambiguous, and its output remains an attributed inference rather than classification proof.

Configuration changes are security-analysis triggers, not automatic Findings. Scan Pilot tracks compatible artifact identity, content digest, source commit, classifier or parser version, and safe derived facts so unchanged compatible evidence may be reused and changed artifacts can be reassessed with family-specific rules. Secret scanning remains applicable across eligible content, while later misconfiguration checks are defined separately for supported families rather than through one universal configuration rule.

The initial deep-analysis direction prioritizes Spring Boot application configuration, GitHub Actions workflows, and Docker configuration. Exact artifact taxonomy, family-detection contract, parser choices, configuration-change impact graph, rule set, and support boundary remain unresolved and require separate accepted decisions. Repository evidence describes repository-declared configuration only; Scan Pilot must not claim that it proves the effective production configuration when runtime profiles, environment variables, command-line values, external stores, or cloud-side settings may override it.

**Reason:** Configuration controls application, build, deployment, CI/CD, container, and security-tool behavior, and OWASP identifies repeatable configuration verification as an important security practice. Family-aware handling provides stronger and more explainable evidence than treating every YAML, JSON, XML, TOML, or properties file alike. The benefit is targeted rescanning and a structured path toward meaningful Security Misconfiguration rules. The trade-off is additional classifier, parser, versioning, and ecosystem-specific rule work. The verification limit is that no classifier, parser, benchmark, or configuration rule beyond `SP-CONFIG-001` has been implemented or validated, and static repository evidence cannot establish live production state.

## DEC-039 — Configuration Artifacts use a multi-dimensional model

**Status:** Accepted

A Configuration Artifact is a Git-tracked file whose primary purpose is to declare or control application, build, dependency, test, CI/CD, container, infrastructure, or security-tool behavior. Scan Pilot describes an artifact through independent dimensions for physical format, technical family, one or more repository roles, module scope, and declared environment or profile scope rather than one overloaded `type` label. Artifact identity binds repository, branch or captured source scope, repository-relative path, Git object or content digest, and source commit.

Source code that configures behavior through classes, annotations, builder APIs, or equivalent program logic remains source code for the MVP. A later framework-aware rule may relate that source to Configuration Artifacts without reclassifying all source files as configuration. A supported family parser may expose multiple logical Configuration Units inside one physical artifact, such as profile-specific Spring YAML documents, but the physical Git item remains the artifact boundary.

**Reason:** The same syntax can configure unrelated systems, and one manifest can serve several roles. Separate dimensions allow correct parser and rule routing without turning ordinary source into an unbounded configuration inventory. The benefit is explainable monorepo and multi-role support. The trade-off is a richer model than one filename-derived enum. The verification limit is that the exact family, role, scope, and logical-unit taxonomies remain under review.

## DEC-040 — Configuration classification separates recognition, family, parsing, and support

**Status:** Accepted

Configuration recognition, technical-family classification, parse outcome, and family-analysis support are independent facts. Platform-defined paths or names, recognized schema or structure, valid family syntax, and compatible detected technology context are deterministic evidence. Extensions, generic directory names, documentation, neighboring files, and AI classifications are supporting evidence only. AI may propose a candidate family but cannot by itself route a family-specific security analyzer.

When deterministic evidence conflicts, Scan Pilot retains the candidate families and provenance, represents the family as unresolved, and does not run a family-specific analyzer. A deterministic platform path or artifact identity may preserve intended family classification when parsing fails; parse failure remains a separate coverage outcome. Generic eligible analysis such as secret scanning continues independently. Family-specific rules run only when deterministic family evidence, support status, and required parsing are sufficient.

**Reason:** Knowing an artifact's intended family, successfully parsing it, and supporting security analysis are different claims. The separation avoids false Findings and preserves honest coverage for malformed, ambiguous, or unsupported artifacts. The trade-off is additional evidence and outcome state. The verification limit is that deterministic recognition fixtures and precedence for each family require separate benchmark validation.

## DEC-041 — Configuration effect is scenario-scoped and repository-bounded

**Status:** Accepted

Scan Pilot preserves declared environment and profile labels exactly and does not automatically equate labels such as `prod`, `live`, `main`, or `release` with a production environment. An optional normalized meaning requires deterministic corroboration or an attributed User Assertion. Activation conditions, imports, overrides, and precedence are interpreted through a model specific to the technical family rather than one universal ordering.

Configuration claims use three levels: `DECLARED` for a value or relationship observed in one source; `REPOSITORY_EFFECTIVE` only when an explicit scenario and every required repository-visible input in the supported precedence chain have been processed; and future `RUNTIME_VERIFIED` evidence from an authorized live integration. Unknown environment variables, command-line arguments, external configuration stores, platform variables, secrets, cloud-side settings, or other runtime inputs keep the effective result unresolved. `RUNTIME_VERIFIED` is outside the MVP.

**Reason:** Spring Boot, Docker Compose, GitHub Actions, and other ecosystems have different activation and override behavior, while a Git branch or profile name does not prove deployment meaning. The benefit is useful scenario analysis without overstating production truth. The trade-off is that many results remain declared or unresolved. The verification limit is that even `REPOSITORY_EFFECTIVE` is valid only for its recorded commit, scenario, supported family model, and repository-visible inputs.

## DEC-042 — Configuration changes trigger reassessment rather than Findings

**Status:** Accepted

Git change types are change evidence, not security conclusions. Scan Pilot records a Configuration Change Event separately from a Finding, reclassifies and reparses a changed artifact as required, and invalidates directly related import, override, environment-file, activation, reference, and module-context evidence. The MVP uses this bounded direct relationship graph rather than attempting a complete source-to-runtime impact graph.

Content-level evidence may be reused only with a compatible content digest and parser version. Context and rule evidence additionally require compatible path, module, family, environment scenario, classifier, family analyzer, rule, and rule-configuration versions. Rename detection is supporting Git evidence rather than permanent identity, and deletion or movement does not automatically resolve a Finding. Semantic rules decide whether a change produces, resolves, or regresses a Finding.

Generic change records persist safe change types, key paths where safe, digests, locations, and redacted or normalized evidence; they do not persist raw sensitive before-and-after values. Changed supported configuration receives higher analysis scheduling priority, but processing priority is not security severity.

**Reason:** A textual edit may be formatting-only, may change behavior indirectly, or may alter context without changing bytes in a dependent file. The benefit is accurate incremental analysis without alert spam or full-repository recomputation. The trade-off is compatibility tracking and a bounded relationship graph. The verification limit is that the MVP cannot prove every downstream runtime or infrastructure effect.

## DEC-043 — Configuration UX separates attention, coverage, and change

**Status:** Accepted

Configuration UX presents Security Attention, Verification Coverage, and Configuration Change as separate dimensions. The dashboard prioritizes active Findings, coverage blockers, and material Review Requests. Changes that produce no Finding are grouped as analyzed information rather than duplicated as alerts. A Configuration Map inventories artifacts by module and role and exposes family, declared profile or environment, analysis support, last assessed commit, recent change, Findings, and coverage limitations.

Ambiguous, unsupported, or parse-failed artifacts are represented in neutral coverage or availability detail and are never presented as green or red merely because they could not be analyzed. Configuration reuses the accepted Finding lifecycle and attention/remediation labels; it does not introduce another color hierarchy. Human review is requested only when missing context materially affects interpretation, required verification, or an applicable rule, and responses remain User Assertions.

**Reason:** Users must distinguish a confirmed security problem from an ordinary change and from missing verification. The benefit is an action-first dashboard that remains understandable to a security beginner while retaining audit detail. The trade-off is layered summary and detail views rather than one flat alert table. The verification limit is that exact wireframes, labels, filters, accessibility behavior, and final scoring remain unresolved.

## DEC-044 — Submission uses a one-way AI Studio-to-production handoff

**Status:** Accepted

Google AI Studio is the origin and submission workspace for a functional Scan Pilot frontend prototype. The user approves its UI/UX, then a captured export is retained as submission evidence and selectively promoted into the production React frontend. After implementation starts, the GitHub production repository is the source of truth and the AI Studio and production codebases are not developed in parallel as competing sources.

AI Studio's workspace agent may edit the open AI Studio project. Local Codex and Antigravity tools operate on the local Git repository and must not be assumed to access the AI Studio filesystem directly. Transfer requires a verified export, GitHub, or another explicitly tested handoff. A final controlled submission snapshot may be refreshed from production-compatible frontend work, but continuous manual `App.tsx` copy-paste is not the development workflow.

**Reason:** The handoff preserves meaningful AI Studio provenance without forcing a security product's Spring Boot, PostgreSQL, GitHub, and isolated-worker architecture into a prototype workspace. The benefit is one production source of truth and traceable submission evidence. The trade-off is that the frozen AI Studio snapshot may not contain every production implementation detail. The verification limit is that round-trip import and final public-link behavior still require the Eligibility Spike.

## DEC-045 — AI Studio is a real submission frontend, not a separate mock product

**Status:** Accepted

The intended submission shape is:

```text
AI Studio submission frontend
        ↓ HTTPS REST
Google Cloud production backend
├── Spring Boot API
├── isolated scan worker
├── Gemini provider integration
└── PostgreSQL through the accepted database direction
```

The AI Studio project must represent the same Scan Pilot product and may call the production backend when the cross-origin and authentication contract is verified. It does not need to contain the Java backend, database, or scanner worker. Scan Pilot does not create a judge-only anonymous demo mode or bypass normal product authorization. The public AI Studio link, demo video, production Cloud Run link, and production source are distinct submission evidence surfaces.

**Reason:** A functional frontend makes AI Studio use material while Cloud Run preserves production-grade boundaries. Requiring judges who choose to test the product to follow the normal GitHub workflow avoids a costly, abusable anonymous scanning path. The benefit is one product behavior rather than a special contest-only product. The trade-off is higher evaluator friction than an anonymous demo. The verification limit is that public sharing, exact judge visibility, external REST access, CORS, authentication handoff, and live-form acceptance are not yet proven.

## DEC-046 — Submission MVP is narrower than Product V1

**Status:** Accepted

The August 2026 submission prioritizes one complete real vertical slice:

1. AI Studio submission frontend and public Cloud Run production deployment;
2. GitHub sign-in, GitHub App installation, and one selected repository;
3. current default-branch snapshot plus validated reachable-history scanning through the accepted Gitleaks adapter;
4. redacted `SP-CONFIG-001` evidence;
5. bounded Gemini explanation and remediation guidance;
6. external remediation followed by re-scan and real lifecycle progression; and
7. independent secret-detection benchmark evidence.

Broader Product V1 capabilities remain accepted directions but cannot displace completion of this slice. Secondary branches, broad multi-project behavior, GitHub Issue creation, full Project Discovery, human review, Configuration Awareness families, A01 rules, and complete Release Assessment enter the submission only after the core is stable and verified.

**Reason:** A solo project with a fixed August deadline needs depth and reliability before breadth. The benefit is a defensible scanner, AI explanation, lifecycle, and deployment story rather than several unfinished subsystems. The trade-off is that the submitted product may expose less of the full vision. The verification limit is that actual GitHub, worker, benchmark, and Cloud Run measurements may require further scope reduction rather than unverified claims.

## DEC-047 — GitHub onboarding signs in before installing the app

**Status:** Accepted

Submission onboarding uses this order:

```text
Sign in with GitHub
→ create a Scan Pilot user session
→ install or select the GitHub App installation
→ grant only selected repository access
→ select a repository in Scan Pilot
→ begin repository onboarding
```

Submission MVP officially supports personal GitHub accounts and public or private personal repositories explicitly selected through the GitHub App. Organization installation and admin-approval behavior are Product V1 scope until verified. User identity and installation authorization remain separate. Background repository operations use short-lived installation authorization rather than a browser credential, and callback, token, session, and CORS mechanics are technical design responsibilities constrained by least privilege.

**Reason:** Sign-in first creates an attributable session before installation linkage, while selected-repository access matches the solo-builder audience and avoids unbounded account access. Supporting private repositories demonstrates real product value. The trade-off is that organization teams are not officially supported in the submission and private-source handling raises the isolation bar. The verification limit includes revocation, repository transfer, token expiry, user isolation, temporary-workspace deletion, and AI Studio authentication compatibility.

## DEC-048 — Gemini explains evidence but does not mutate repositories

**Status:** Accepted

For the submission slice, Gemini receives only bounded, normalized, secret-redacted evidence. It explains what was detected, why it matters, what the evidence proves and cannot prove, the ordered remediation plan, and why a re-scan changed remediation state. Detector evidence and backend rules remain responsible for lifecycle and coverage.

Gemini and Scan Pilot do not generate a patch for automatic application, edit source, create a branch, commit, push, rewrite Git history, revoke a credential, or mark a Finding resolved. A developer or external coding agent performs remediation outside Scan Pilot. Scan Pilot then re-scans repository truth.

**Reason:** Explanation and contextual remediation are useful to security beginners and visibly use Gemini without making AI the security authority. The benefit is meaningful Google AI integration with a bounded trust surface. The trade-off is no one-click autofix. The verification limit is that structured output still requires schema validation, safety review, fallback content, and prompt/evidence tests.

## DEC-049 — Submission quality includes independent benchmark evidence

**Status:** Accepted

`SP-CONFIG-001` quality evidence separates detector accuracy from Scan Pilot orchestration. The submission uses an independent safe secret-test battery where licensing and data handling permit, records the detector version and trusted configuration, and reports true positives, false negatives, and false positives only for the benchmark scope. Gitleaks-owned fixtures remain internal regression evidence and are not represented as independent validation.

Academic SecretBench data containing secrets mined from real repositories is not used without the required access agreement and an accepted sensitive-data protocol. It is not sent to Gemini, committed to Scan Pilot, or used in the public demo. OWASP Benchmark and NIST SARD remain candidates for later rule families rather than evidence for `SP-CONFIG-001`.

**Reason:** Ground-truth comparison is more credible than self-selected success cases, while attribution prevents Scan Pilot from claiming Gitleaks detection performance as proprietary accuracy. The benefit is measurable and reproducible quality evidence. The trade-off is benchmark-adapter and reporting work. The verification limit is that no benchmark run, pinned detector, or final safe battery has yet been completed.

## DEC-050 — The end-to-end story uses a controlled security-lab repository

**Status:** Accepted

The submission video and end-to-end verification use a separate repository owned by the user. It contains non-functional synthetic secret candidates and a controlled Git history with ground truth. It is scanned through the normal GitHub App workflow; findings are produced by the real detector and backend rather than mocked by the frontend. The lab remains private during development unless a later review proves a public sanitized form is useful and safe.

The target story is:

```text
OPEN / ACTION_REQUIRED
→ external source and credential remediation
→ RESOLVED / RISK_CONTAINED while reachable history remains
→ external history cleanup
→ RESOLVED / VERIFIED_COMPLETE within recorded scan coverage
```

Scan Pilot never rewrites or force-pushes the lab repository itself. Ground truth is stored separately from the scanned target. `VERIFIED_COMPLETE` remains bounded by scanned Git scope, detector, rules, and coverage; it never proves that an earlier credential was not copied.

**Reason:** A controlled lab makes the real pipeline repeatable without contaminating the Scan Pilot repository or using a live credential. The benefit is one fixture for video, lifecycle, redaction, history, and end-to-end regression. The trade-off is that it is an intentionally vulnerable test target and must be described honestly. The verification limit is that synthetic candidates cannot represent every real credential family and therefore do not replace independent benchmarks.

## DEC-051 — Submission planning uses an internal August 30 completion gate

**Status:** Accepted planning constraint

The user-reported external submission deadline is `2026-08-31 23:59` in the event's applicable local time. Scan Pilot uses `2026-08-30` as the internal complete-and-deployed deadline; August 31 is reserved for final verification, video/link checks, and contingency only. The live completion form remains the authoritative external source and must be rechecked before submission.

**Reason:** A one-day safety margin reduces the risk that deployment, sharing permissions, video, or form problems consume the final submission window. The benefit is a clear delivery gate. The trade-off is one fewer implementation day. The verification limit is that the completion-form URL and authoritative timezone have not yet been recorded in the repository.

## Intentionally Open Decisions

- exact scoring formula and status thresholds;
- exact optional confidence scale beyond the accepted verification statuses;
- exact final V1 rule count;
- exact detectors for rule families beyond the accepted Gitleaks-backed `SP-CONFIG-001` path;
- optional Phase 2 document-extraction consent, privacy, implementation, format, OCR, resource, retention, and fallback policies;
- exact eligibility reason-code taxonomy and policies for non-document binary families, archived, linked, generated, dependency, and user-excluded content beyond the accepted size reason codes;
- exact Scan Pilot suppression authorization, scope, expiry, invalidation, and audit workflow;
- exact Gitleaks version or artifact digest and the verified mechanism that prevents repository `.gitleaksignore` from controlling baseline detection;
- exact Configuration Artifact taxonomy, deterministic classification evidence, ambiguity policy, parser set, safe derived-fact schema, change-impact model, and family-specific MVP rule set;
- exact Configuration Map wireframe, filters, accessibility behavior, and configuration-specific Review Request triggers;
- exact layered-classifier library, byte-sampling thresholds, supported text encodings, and conflict-resolution details;
- exact fingerprint key service, access policy, rotation schedule, and retirement operation;
- exact semantic matching strategies for later rule families;
- exact sandbox or VM technology for rules that execute repository code;
- exact evidence threshold for promoting a Google/Gemini key finding to `Critical`;
- exact Review Request expiry, invalidation, and authorization policy;
- exact ASVS coverage presentation;
- exact BYOK encryption and storage;
- exact queue/cache technology;
- exact Maven version, Wrapper policy, backend module layout, plugins, dependency versions, profiles, and CI commands;
- exact MVP delivery scope, triggers, evidence-validity rules, build or artifact checks, and full completion contract for Release Assessment beyond `DEC-036`;
- final deployment topology within the accepted Google Cloud direction.
- exact regional deployment prices and post-promotional-credit continuity plan.
- exact AI Studio public-link permissions, judge-visible source/prompt surface, external API origin, CORS, and authenticated-session handoff;
- exact personal-account GitHub session mechanism, callback routing, installation-token lifecycle, revocation handling, and private-source deletion verification;
- exact independent safe secret benchmark battery, adapter, metrics report, and accepted accuracy thresholds;
