> **Document:** Scan Pilot Scan Lifecycle  
> **File:** `docs/SCAN-LIFECYCLE.md`  
> **Version:** v0.8.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
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
- exact identity and regression matching strategies for rule families other than the accepted `SP_SECRET_FP_V1` contract;
- duplicate webhook handling;
- retention and cleanup of repository snapshots;
- partial-scan result semantics;
- Review Request expiry, invalidation, and notification behavior;
- conditions under which a human response triggers finding or profile re-evaluation.
