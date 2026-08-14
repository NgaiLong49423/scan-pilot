> **Document:** Scan Pilot Scan Lifecycle  
> **File:** `docs/SCAN-LIFECYCLE.md`  
> **Version:** v0.15.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-14  
> **Status:** Draft  

# Scan Pilot Scan Lifecycle

## Accepted Lifecycle Direction

```text
GitHub event or manual request
→ create scan job
→ acquire repository snapshot
→ preserve immutable source and create a mutable workspace only when required
→ create or refresh Repository Profile when relevant inputs changed
→ run deterministic scanners
→ retrieve relevant context
→ use Gemini where useful
→ normalize findings
→ create Review Requests when material context is missing
→ update dashboard and finding lifecycle
→ optionally create GitHub Issue
```

Review Requests do not block scan completion. The dashboard may show a potential conclusion and request user input while the scan job reaches its terminal state. A later response updates attributed context and may trigger re-evaluation, but it does not automatically resolve a finding.

## Project Discovery Document Flow

```text
document candidate within the captured repository scope
→ deterministic type and policy checks
→ inventory outcome
→ supported text/config/manifest: deterministic reader
   or PDF/Office binary: INVENTORIED + NOT_SUPPORTED_MVP
→ normalize supported-text evidence and provenance
→ bound and redact selected supported text
→ Gemini classification or summary only when useful
→ persist attributed Repository Profile claims and coverage
```

The MVP does not extract internal content from PDF, DOC/DOCX, XLS/XLSX, or PPT/PPTX and does not include Apache Tika. Their successful inventory cannot be represented as semantic understanding. For `SP-CONFIG-001`, the same items are recorded as `CONSIDERED` then `SKIPPED` with `UNSUPPORTED_BINARY_DOCUMENT` and explicit coverage impact.

## Configuration Awareness Lifecycle

```text
Git-tracked repository item
→ detect configuration candidacy
→ classify recognition and technical family from deterministic evidence
→ parse through a supported bounded family parser
→ record roles, module, declared labels, activation, and direct relationships
→ run supported family rules
→ persist safe Configuration Map evidence and coverage
```

Recognition, family, parse outcome, and analysis support are independent. An unresolved or AI-only family does not route a family-specific analyzer, and parse failure does not erase deterministic artifact intent. Generic eligible scanning continues under its own coverage contract.

Declared profile and environment labels remain exact repository data. Family-specific precedence can produce `REPOSITORY_EFFECTIVE` only for an explicit scenario with every required supported repository-visible input. Unknown environment variables, command-line values, external stores, platform variables, or cloud settings keep the result unresolved; runtime verification is outside the MVP.

For a compatible incremental scan:

```text
Git change
→ Configuration Change Event
→ reclassify and reparse changed artifact as required
→ invalidate direct import/override/env-file/activation/reference/module dependents
→ reuse unrelated compatible evidence
→ evaluate semantic rules
→ create, update, resolve, or regress Findings only from valid rule evidence
```

A rename is supporting history evidence rather than permanent artifact identity, and a deletion cannot resolve a Finding without successful reassessment. Generic change records contain safe paths, change types, digests, key paths where safe, and redacted or normalized evidence rather than raw sensitive before-and-after values.

## Review Request Lifecycle

The initial lifecycle direction is:

```text
PENDING → ANSWERED
       ↘ EXPIRED
       ↘ CANCELLED
```

A response may use a structured option, `I don't know`, `Another answer`, optional free-text context, and an optional supporting repository-relative path or GitHub link. Exact expiry and invalidation behavior remains unresolved.

## Evidence Across Scans

Evidence is attached to the repository commit or snapshot from which it was produced. A later scan creates new evidence rather than rewriting the earlier observation. User corrections and new technical observations may supersede earlier context while preserving its provenance.

Finding lifecycle evaluation uses the accepted Evidence Model. `SP-CONFIG-001` uses the accepted `SP_SECRET_FP_V1` identity contract; other rule families still require rule-specific identity strategies. See `docs/EVIDENCE-MODEL.md` and `docs/FINDING-TRACKING.md`.

Finding matching combines the applicable rule-specific identity with commit history, diff, Evidence Locations, optional semantic context, and re-scan evidence. Temporary workspace markers are job-local instrumentation and are never persistent identity. See `docs/FINDING-TRACKING.md`.

For secret candidates, redaction and trusted fingerprinting occur before normalized results leave the trusted detection path. Only the safe fingerprint, scheme, key version, redacted evidence, and provenance continue into persistence and lifecycle matching.

## Finding Lifecycle

At minimum, the MVP must demonstrate:

```text
OPEN → RESOLVED → REGRESSED
```

Other states considered but not finalized:

- `ACKNOWLEDGED`
- `IN_PROGRESS`
- `ACCEPTED_RISK`
- `FALSE_POSITIVE`

`REGRESSED` means the same previously resolved weakness returns after a later repository change. `SP-CONFIG-001` uses the accepted `SP_SECRET_FP_V1` identity contract; identity rules for other Finding families remain unresolved.

### Remediation Quality

Remediation quality is independent from lifecycle:

- `ACTION_REQUIRED`: mandatory security work remains;
- `RISK_CONTAINED`: the primary risk has been contained, but recommended cleanup remains;
- `VERIFIED_COMPLETE`: Scan Pilot verified all applicable remediation steps within the accessible scan scope.

`NOT_ASSESSED` means the available evidence is insufficient to assign a reliable quality level. It is not a fourth level. UI colors are optional presentation and must not replace these labels.

For `SP-CONFIG-001`, a successful current-source re-scan plus credential invalidation can transition the Finding to `RESOLVED`. Remaining historical exposure produces `RISK_CONTAINED`; adequate verification that accessible history is clean produces `VERIFIED_COMPLETE`. Incomplete scan coverage cannot produce `VERIFIED_COMPLETE`.

## Repository Branch Initialization and Synchronization

```text
Connect repository
→ read GitHub default branch
→ assign it as PRIMARY
→ capture immutable primary HEAD SHA
→ scan the exact HEAD and publish safe early Findings
→ scan full reachable Git history
→ validate coverage and create the baseline checkpoint
→ mark PRIMARY as MONITORED
→ unlock up to two SECONDARY branches
```

A completed baseline may contain Findings and still initialize monitoring successfully. Before the primary full-history baseline completes with valid coverage, secondary branches cannot be configured. An early snapshot result does not initialize monitoring and cannot support a whole-history clean claim.

After initialization, branch scans are independent. A primary scan failure marks its assessment unavailable and reduces repository coverage, but it does not cancel scans for accessible secondary branches or suppress their Findings.

The primary always mirrors the current GitHub default branch. A default-branch change updates the role immediately and queues a prioritized scan of the new primary. If the new default is already monitored, roles are adjusted without losing a branch. If it is new and capacity remains, the old primary is retained as secondary. If it is new and all three slots are occupied, the old primary leaves current monitoring while the two user-selected secondaries remain.

Scope removal stops future monitoring for the old primary; it does not erase historical scan results, Findings, Evidence Items, or the audit event explaining the GitHub-derived change. Until the new primary completes a valid scan, repository coverage remains incomplete.

## Snapshot, History, and Incremental Scans

Each branch scan starts from one captured immutable HEAD SHA. The Current Snapshot Scan examines that exact HEAD first. The Git History Scan then examines reachable commit patches, with newer commits prioritized before older commits using Git's commit graph. The order improves time to first useful Finding but does not reduce the baseline scope.

Within that Git scope, Scan Pilot inventories all Git-tracked content and classifies eligibility before or alongside detector execution:

```text
Git-tracked item
→ CONSIDERED
→ SCANNED
   or
→ SKIPPED + reason code + coverage impact
```

No default source-folder allowlist defines secret-scanning coverage. A skipped item remains part of the recorded scope but is not represented as successfully scanned.

Content classification uses several evidence layers rather than a trusted extension list:

```text
Git object kind
→ recognized signature
→ bounded decoding and binary-content signals
→ extension and .gitattributes hints
→ TEXT | BINARY | UNDETERMINED
```

The eligibility policy then converts the classification and other limits into `SCANNED` or `SKIPPED`. A conflict is `UNDETERMINED`, not an implicit clean result.

Every `SKIPPED` outcome is persisted as structured scan coverage with item identity, repository-relative path, classification, stable reason code, applicable policy version, and coverage impact. Application logs remain operational diagnostics only; deleting or expiring logs must not erase the product's knowledge that an item was considered and skipped.

### Full-file size routing

For otherwise eligible supported text, the initial versioned limits are:

```text
size <= 10 MiB
→ Continuous Monitoring: full-file SCANNED

10 MiB < size <= 50 MiB
→ Continuous Monitoring: SKIPPED
  reason: MONITORING_FILE_SIZE_LIMIT_EXCEEDED
→ Release Assessment: full-file SCANNED when requested
  or compatible evidence reused

size > 50 MiB
→ Continuous Monitoring: SKIPPED
→ Release Assessment: SKIPPED
  reason: RELEASE_FILE_SIZE_CEILING_EXCEEDED
→ required release coverage: INCOMPLETE
```

`1 MiB` means `1,048,576 bytes`. Format exclusions take precedence over this size routing, so an unsupported binary document does not become eligible merely because it is small. A monitoring size skip is not `DEFERRED` unless Scan Pilot has created a real follow-up job. Partial prefixes, byte-range checkpoints, and chunk-resume results cannot produce `SCANNED` or complete coverage in the MVP.

After a validated baseline, an incremental scan may use `old_checkpoint..new_head` only if the old compatible checkpoint is an ancestor of the new HEAD. An empty verified range is `NO_CHANGE`. If Git reports a non-ancestor relationship, or the detector/rule/config/parser contract is incompatible, the MVP runs a new full-history baseline.

The history baseline is `COMPLETE` only after Scan Pilot validates its requested scope, structured detector output, parser compatibility, coverage telemetry, timeout/cancellation state, and expected Git traversal. Detector exit code alone is not sufficient. A zero-commit baseline or unknown coverage is incomplete or unavailable. Expected Git commit count must not be equated directly with detector-reported commits when commits without relevant additions are omitted from detector telemetry.

Only a compatible `COMPLETE` scan can create or advance the checkpoint. Relevant rule, detector, configuration, or parser changes may require backfill before later coverage-dependent claims are restored.

## Safety Boundary

- Repository content is untrusted.
- Static scanning should not execute project code.
- Any build, test, or execution-based analysis must occur in an isolated worker with explicit resource and network controls.
- Secret values must be redacted before persistence, logging, AI analysis, or display.

## Open Lifecycle Decisions

- exact scan job states;
- retry and timeout behavior;
- cancellation behavior;
- exact retry, backfill scheduling, and operational limits for the accepted baseline/incremental policy;
- exact non-document binary, size, archive, link, submodule, dependency, generated-content, lock-file, and user-exclusion policies;
- exact layered-classifier implementation, bounded sampling, supported encodings, and ambiguous-content handling;
- exact Configuration Artifact taxonomy, family fixtures, parser set, direct-relationship invalidation algorithm, and Configuration Map UX;
- exact identity and regression matching strategies for rule families other than the accepted `SP_SECRET_FP_V1` contract;
- duplicate webhook handling;
- retention and cleanup of repository snapshots;
- partial-scan result semantics;
- Review Request expiry, invalidation, and notification behavior;
- conditions under which a human response triggers finding or profile re-evaluation;
- optional Phase 2 document-parser selection, consent, privacy, timeout, memory, input/output limits, and fallback policy;
- future PDF and Office semantic extraction, OCR, and additional binary document support.
