> **Document:** Scan Pilot Finding Tracking Model  
> **File:** `docs/FINDING-TRACKING.md`  
> **Version:** v1.3.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Active  

# Scan Pilot Finding Tracking Model

## Purpose

This document defines the accepted architecture for tracking a Finding across repository versions without treating a file path or line number as its durable identity.

```text
Rule-specific identity
+ repository commit history
+ Git diff
+ evidence locations
+ optional semantic context
+ re-scan verification
→ Finding matching decision
```

Fingerprint strategy remains rule-specific. A secret exposure, an authorization weakness, and an unsafe configuration do not have the same stable semantic identity. The accepted `SP-CONFIG-001` strategy is specified below; later rule families still require their own strategies.

## Repository Version Model

GitHub remains the source of truth for repository content. Each scan identifies the repository state with a commit SHA or another explicit immutable snapshot identifier.

Scan Pilot uses two separate local representations:

```text
Immutable Source Snapshot
→ original repository state used as the comparison baseline

Disposable Mutable Workspace
→ temporary copy that tools may instrument or modify during the job
```

The immutable snapshot must not be altered by scanners, formatters, candidate fixes, or temporary markers. The mutable workspace may be changed within sandbox resource and security limits and is destroyed after the job.

Scan Pilot persists repository identity, commit SHA, scan state, Evidence Items, Finding history, and bounded diff metadata by default. It does not retain a complete source snapshot long-term unless a later accepted retention policy provides a specific reason and security boundary.

Reference:

- GitHub compare commits API: https://docs.github.com/en/rest/commits/commits#compare-two-commits

## Scan Coverage and Checkpoints

For `SP-CONFIG-001`, Scan Pilot captures an immutable HEAD SHA and keeps current-snapshot evidence separate from history-coverage evidence. The exact HEAD is scanned first for early warning; reachable history is then scanned with newer commits prioritized before older commits using Git's graph.

The checkpoint belongs to Scan Pilot, not to Gitleaks. It binds repository and branch identity, captured HEAD, mode and Git scope, detector/version, rule/config version and digest, parser schema, expected Git commit count, detector telemetry, timestamps, and terminal status. Detector exit code or a missing Finding is not sufficient evidence of coverage.

After a complete baseline, an incremental scan may use `old_checkpoint..new_head` only when the compatible old checkpoint is an ancestor of the new HEAD. Non-ancestor history or an incompatible scan contract requires a new full-history baseline in the MVP. A checkpoint is created or advanced only after validation succeeds. Rule, detector, configuration, or parser changes may require backfill before history-dependent quality claims can be restored.

## Tracking Signals

### Rule-Specific Identity

A rule-specific fingerprint or semantic identity answers whether a newly observed condition may represent the same underlying Finding. It must not rely only on file path, line, column, branch name, current commit, or detector version.

For `SP-CONFIG-001`, Scan Pilot uses the `SP_SECRET_FP_V1` scheme:

```text
FingerprintInputV1 =
    length_prefix("scan-pilot:secret-fingerprint:v1")
    + length_prefix(stable_workspace_id)
    + length_prefix(stable_repository_id)
    + length_prefix("SP-CONFIG-001")
    + length_prefix(stable_credential_family)
    + length_prefix(exact_detected_secret_bytes)

Fingerprint = HMAC-SHA-256(K_version, FingerprintInputV1)
```

The encoding must be canonical and unambiguous. Length prefixes prevent different field boundaries from producing the same input. Stable internal workspace and repository identifiers are used instead of mutable display names.

The full 32-byte HMAC output is persisted with the fingerprint scheme and key version. Its database or display encoding is an implementation detail. The raw secret is never persisted as identity.

For Google and Gemini candidates, the stable credential family is `GOOGLE_API_KEY`. Context such as likely Gemini usage is a claim attached to evidence and does not change identity.

Exact secret bytes are case-sensitive and are not trimmed, lowercased, or otherwise normalized. A detector may remove quotes, delimiters, or surrounding syntax only after establishing that they are not part of the credential. Obvious placeholders, redacted samples, and environment references do not receive a persisted secret fingerprint.

The fingerprint excludes path, line, column, commit, branch, detector version, rule version, variable name, contextual classification, severity, and lifecycle state. Therefore the same exact credential remains one Finding when its location or classification changes.

One random Finding ID remains the product identity. A matching fingerprint groups rule-defined Evidence Locations beneath that Finding; the fingerprint itself is internal matching material rather than a user-facing identifier.

### Fingerprint Key Boundary and Rotation

The HMAC key belongs only to a trusted fingerprint component. It is not stored in PostgreSQL, hard-coded in source, logged, or exposed to an untrusted execution sandbox. The exact Google Cloud key service remains an infrastructure decision.

Each fingerprint record retains:

```text
scheme: SP_SECRET_FP_V1
key_version: <version identifier>
tag: <full HMAC-SHA-256 output>
```

Before rotating a key used by historical Findings, Scan Pilot must support matching with retained key versions and attaching a new-version fingerprint as an alias to the existing Finding. A historical key version must not be destroyed while it is still required to recognize regression. The exact operational rotation schedule and retirement policy remain open.

Raw candidates exist only in the trusted detection and fingerprint path for the minimum practical time. They must not enter database records, job results, message queues, metrics, logs, error messages, or AI prompts. Managed-runtime memory clearing cannot be claimed as absolute.

References:

- NIST FIPS 198-1 HMAC: https://csrc.nist.gov/pubs/fips/198-1/final
- Google Cloud KMS MAC signatures: https://docs.cloud.google.com/kms/docs/mac-signatures
- Google Cloud KMS HMAC algorithms: https://docs.cloud.google.com/kms/docs/algorithms

### Commit History and Diff

Commit SHAs establish which repository versions were scanned. Git diff provides supporting evidence about files and code that were added, removed, modified, or renamed.

Diff supports matching and explanation but is not sufficient identity by itself. Large refactors, copied code, rewritten history, and generated files can make diff relationships incomplete or ambiguous.

### Evidence Locations

Current and historical Evidence Locations describe where a condition was observed. Location changes update the history of a Finding rather than automatically creating a new Finding.

Whether several locations form one Finding or several Findings is defined by the applicable rule contract and the underlying security object being tracked.

### Semantic Context

Rules that cannot fingerprint a stable value may use bounded structural context such as module, class, method, route, configuration property, or resource type.

Semantic matching is optional and rule-specific. When the match remains uncertain, Scan Pilot must not silently transition the lifecycle. It may retain the current state and create a Review Request.

### Re-scan Verification

A matching decision explains identity; re-scan evidence determines whether the vulnerable condition still exists. A missing result from a failed, partial, skipped, or incompatible scan is not proof of resolution.

Lifecycle and remediation quality are separate:

```text
Lifecycle: OPEN → RESOLVED → REGRESSED

Remediation quality:
ACTION_REQUIRED
RISK_CONTAINED
VERIFIED_COMPLETE
```

`NOT_ASSESSED` indicates insufficient evidence and is not a fourth quality level. Quality labels apply to one Finding; they do not claim that the repository or project is generally safe. Colors may support the UI, but semantic labels are the data contract.

For `SP-CONFIG-001`, `RESOLVED` requires both:

1. a successful compatible re-scan that verifies the matching secret is absent from current source; and
2. evidence that the exposed credential no longer works because it was revoked, deleted, expired, or fully rotated so the old value was invalidated.

For the MVP, invalidation may be `USER_ATTESTED` through an attributable Review Request. A future explicitly authorized provider integration may produce `PROVIDER_VERIFIED`. The user assertion cannot replace technical clean-source evidence, and Scan Pilot must not use a discovered credential for unauthorized live validation.

Historical exposure determines remediation quality rather than blocking resolution:

| Current source | Credential | Accessible Git history | Lifecycle | Remediation quality |
|---|---|---|---|---|
| Exposed | Active or unknown | Any | `OPEN` | `ACTION_REQUIRED` |
| Clean | Active or unknown | Any | `OPEN` | `ACTION_REQUIRED` |
| Exposed | Invalidated | Any | `OPEN` | `ACTION_REQUIRED` |
| Clean | Invalidated | Matching historical exposure remains | `RESOLVED` | `RISK_CONTAINED` |
| Clean | Invalidated | Verified clean within adequate scan scope | `RESOLVED` | `VERIFIED_COMPLETE` |
| Clean | Invalidated | Insufficiently scanned | `RESOLVED` | `NOT_ASSESSED` |

If the same secret returns to current source after resolution, the Finding becomes `REGRESSED` with `ACTION_REQUIRED`. Scan Pilot may claim only that history is clean within the repository refs and retention available to the completed scan; it cannot prove deletion from forks, old clones, caches, or external copies.

## Temporary Instrumentation

Scan Pilot may add temporary comments, markers, AST annotations, or other instrumentation only inside the Disposable Mutable Workspace.

Temporary instrumentation may support analysis, candidate-patch comparison, build/test verification, or UI rendering. It is not persistent Finding identity and must not be written to the user's repository by an ordinary scan.

The original repository may be changed only through a separately accepted and explicitly authorized write workflow. Automatic fixing or pushing is not part of the accepted MVP.

## Untrusted Execution Boundary

Static rules should not execute repository code when execution is unnecessary. If a rule later requires build, test, or runtime behavior, execution occurs only in an isolated environment selected for that rule.

The trusted acquisition stage obtains the immutable repository snapshot with narrowly scoped, short-lived credentials. Those credentials must not be available when untrusted repository code executes.

The execution environment must not contain application database credentials, Gemini credentials, GitHub App private keys, or other backend secrets. It requires bounded CPU, memory, storage, time, output, and network access, plus workspace cleanup.

The exact Google Cloud sandbox or VM technology remains an open infrastructure decision.

References:

- Cloud Run overview: https://docs.cloud.google.com/run/docs/overview/what-is-cloud-run
- Cloud Run code execution: https://docs.cloud.google.com/run/docs/code-execution

## MVP Boundary

For `SP-CONFIG-001`, the core MVP path is static:

```text
Acquire immutable repository state
→ detect secret candidates
→ create safe rule-specific identity
→ associate Evidence Locations
→ compare with prior scan state
→ re-scan without executing repository code
```

Candidate code generation, automatic remediation, build/test execution, and `Verify Fix Before PR` remain future directions. They are not required for the first vertical slice and do not authorize repository writes.

## Verification Limits

- A stable fingerprint supports identity matching but does not prove current exploitability or credential activity.
- Git diff explains changes but may not identify semantic continuity after a large refactor.
- Semantic matching can be uncertain and must not invent lifecycle transitions.
- A disposable workspace protects the original snapshot but does not by itself provide a sufficient security sandbox.
- Commit metadata cannot recreate source that is no longer reachable unless a retention policy preserved it.
