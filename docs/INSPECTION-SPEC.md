> **Document:** Scan Pilot Inspection Specification  
> **File:** `docs/INSPECTION-SPEC.md`  
> **Version:** v0.13.0
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-20
> **Status:** Under Review  

# Scan Pilot Inspection Specification

## Specification Model

```text
Versioned Standard Requirement
→ Scan Pilot Rule
→ Scan Evidence
→ Repository-Specific Finding
```

Every official rule must define its purpose, standards basis, priority, automability, detection method, evidence, exclusions, finding wording, remediation, re-scan behavior, and verification limits.

All official rules use the shared provenance and verification contract in `docs/EVIDENCE-MODEL.md`. A rule must define which scoped claims and Evidence Items are sufficient for potential or confirmed wording; the shared model does not create a universal confirmation threshold.

All official rules must also define their applicable identity and matching signals under `docs/FINDING-TRACKING.md`. A rule may use a stable value fingerprint, semantic identity, diff context, or a compatible combination, but it must state when matching is uncertain.

## Official MVP Rules

### SP-CONFIG-001 — Source Code Secret Exposure

| Property | Value |
|---|---|
| Status | Accepted for MVP; detailed contract under review |
| Priority | MUST |
| Automability | PARTIAL |
| Detection | STATIC |
| Primary basis | OWASP ASVS `v5.0.0-13.3.1` |
| Supporting guidance | OWASP Secrets Management Cheat Sheet |

#### Purpose

Detect likely credentials, API keys, tokens, private keys, or security-sensitive secrets committed to repository source or included in build artifacts.

#### Why It Is in the MVP

Secret exposure has real impact, is common in AI-assisted coding, supports deterministic scanning, and can demonstrate the complete finding-to-re-scan lifecycle.

#### Required Evidence

A finding must identify:

- repository snapshot or commit;
- file path and location;
- detector or secret family;
- redacted preview or fingerprint that cannot reconstruct the secret;
- why the value is considered suspicious;
- whether the result is potential or strongly verified.
- the scoped claims and verification statuses supporting the conclusion.

#### Mandatory Safety Rules

- Never persist, log, display, or send the full secret to Gemini.
- Redact before normalizing the finding.
- Do not reproduce the value in remediation, GitHub Issues, screenshots, or reports.
- Treat secret revocation and replacement as required remediation; deleting the line alone is insufficient because Git history or prior copies may retain it.

#### Exclusions and False-Positive Controls

Obvious placeholders and references should not be confirmed as live secrets, including examples such as:

```text
your-api-key-here
example
test-token
${GEMINI_API_KEY}
System.getenv("GEMINI_API_KEY")
```

Detection should combine provider-specific format, context, entropy where useful, filename/location, and explicit test/example markers. Gitleaks is the first MVP detector behind a Scan Pilot adapter; the accepted rule, evidence, and lifecycle contracts remain owned by Scan Pilot rather than by the tool's native report model.

Every Git-tracked content item within the selected Git scope is evaluated for eligibility. Scan Pilot distinguishes:

- `CONSIDERED`: item inventoried and evaluated against the content policy;
- `SCANNED`: eligible content successfully processed by the detector;
- `SKIPPED`: content not processed, with a stable reason code and explicit coverage impact.

Eligibility uses a layered content classifier:

```text
Git object kind
→ recognized content/file signature
→ bounded text-decoding and binary-content signals
→ extension and .gitattributes as supporting hints
→ TEXT | BINARY | UNDETERMINED
```

Neither the filename extension nor repository-controlled `.gitattributes` is authoritative on its own. Conflicting or insufficient evidence produces `UNDETERMINED`. Such an item must not be silently sent to the detector, silently discarded, or counted as clean; the applicable versioned eligibility policy must produce an explicit processing outcome and reason.

Text documentation, examples, tests, GitHub workflows, and generated frontend output are not excluded merely because of their path category. For the MVP, PDF, DOC/DOCX, XLS/XLSX, and PPT/PPTX are `CONSIDERED` and then `SKIPPED` with reason `UNSUPPORTED_BINARY_DOCUMENT`; their internal content is not counted as scanned. Exact handling of other binary content, archives, symbolic links, submodules, dependency trees, generated output, lock files, and user exclusions remains under review.

For otherwise eligible supported text, Continuous Monitoring scans the complete file through `10 MiB` and records a larger file as `SKIPPED` with `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`. Release Assessment may reuse compatible full-file evidence and scans complete eligible files through `50 MiB`; a larger required item is `SKIPPED` with `RELEASE_FILE_SIZE_CEILING_EXCEEDED` and produces incomplete release coverage. Limits are inclusive, `1 MiB` is `1,048,576 bytes`, and partial-prefix or chunk results cannot satisfy full-file coverage.

#### Detector and Coverage Contract

Scan Pilot owns the trusted detector policy. Each invocation must bind an exact tested Gitleaks version or immutable artifact digest, an explicit trusted configuration and digest, enabled rules, adapter version, exact-byte size policy, timeout, and full redaction. The worker environment must not inherit unapproved `GITLEAKS_CONFIG` or `GITLEAKS_CONFIG_TOML` values.

Repository `.gitleaks.toml`, `.gitleaksignore`, and inline `gitleaks:allow` directives are untrusted evidence and cannot silently suppress the baseline. The adapter requests `--ignore-gitleaks-allow` behavior and must use a benchmarked detector view or equivalent verified technique that prevents target `.gitleaksignore` from changing results without modifying the immutable source snapshot. Failure to prove that isolation produces incomplete coverage.

Scan Pilot evaluates the `10 MiB` and `50 MiB` boundaries from exact blob byte size. Gitleaks' decimal `--max-target-megabytes` control may provide a second safety boundary but cannot establish Scan Pilot size coverage by itself.

The `SP-CONFIG-001` scan captures one immutable HEAD SHA and uses two evidence flows:

1. Current Snapshot Scan examines the exact HEAD first and may publish safely normalized Findings early.
2. Git History Scan examines reachable commit patches, prioritizing newer commits before older commits with graph-aware Git traversal.

The same `SP_SECRET_FP_V1` identity can group snapshot and historical evidence into one Finding. Snapshot completion alone cannot establish full-history cleanliness or initialize a newly monitored branch under the accepted onboarding policy.

Gitleaks output is processed only inside the trusted adapter boundary. Detector-side full redaction is required as defense in depth, but Scan Pilot must also remove unsafe raw fields, compute the fingerprint, and produce normalized redacted evidence before persistence, queues, logs, errors, metrics, display, or Gemini. The temporary raw report is deleted after successful safe normalization or failure cleanup.

Coverage must bind repository and branch identity, captured HEAD, scan mode and Git range, detector/version, Scan Pilot rule/config version and digest, report/parser schema version, expected Git commit count, content considered/scanned/skipped counts with skip reasons, detector telemetry, timing, and terminal outcome. Exit code alone is not proof that the requested history or content was scanned. A zero-commit baseline or unknown coverage is not clean; an explicitly verified empty incremental range may be `NO_CHANGE`.

Each skipped content item also has a durable structured coverage record containing, where applicable, scan ID, repository and branch, captured commit or Git object identity, repository-relative path, known size, content classification, processing outcome, stable reason code, applicable rule/config version, and coverage impact. These records support audit, UI detail, later policy re-evaluation, and backfill selection. Application logs may record safe operational diagnostics, but they do not replace these records and must not contain detected secret values.

A compatible incremental range may be used only when the previous checkpoint is an ancestor of the captured new HEAD. Force-push, history rewrite, or incompatible detector/rule/config/parser state requires a new full-history baseline for the MVP. Checkpoints advance only after coverage is validated as complete, and relevant rule changes can require backfill.

#### Google and Gemini API Key Profile

The Google/Gemini profile uses three evidence layers:

```text
Google API key candidate
→ nearby Google/Gemini context
→ repository exposure location
```

Context signals may include:

```text
GEMINI_API_KEY
GOOGLE_API_KEY
generativelanguage.googleapis.com
Google Gen AI SDK usage
Gemini model or client references
```

Location evidence includes committed source code, application configuration, frontend bundles or source, CI workflows, container configuration, and Git history.

The scanner must not treat environment lookups as exposed values:

```text
System.getenv("GEMINI_API_KEY")
${GEMINI_API_KEY}
```

Scan Pilot will not automatically send a discovered key to Google or another provider for live verification. A failed validation could result from restrictions rather than invalidity, while validation itself could consume quota, create logs, or misuse a credential belonging to the repository owner.

References:

- Google Cloud API key best practices: https://docs.cloud.google.com/docs/authentication/api-keys-best-practices
- Google API key management and restrictions: https://docs.cloud.google.com/docs/authentication/api-keys
- Gemini API key guidance: https://ai.google.dev/gemini-api/docs/api-key

#### Finding Wording

- Incomplete evidence: `Potential Source Code Secret Exposure`.
- Strongly verified evidence: `Source Code Secret Exposed`.

Google/Gemini-specific wording:

- format evidence only: `Potential Google API Key Exposure`;
- format plus Gemini context: `Potential Gemini API Key Exposure`;
- committed credential evidence: `Google/Gemini API Key Exposed in Repository`.

The exact threshold for strongly verified evidence remains unresolved.

The initial Google/Gemini severity direction is `High`. The rule must not automatically emit `Critical` while key activity, restrictions, and blast radius are unknown. Exact promotion criteria remain unresolved.

#### Remediation Contract

1. Revoke or invalidate the exposed credential.
2. Create a replacement credential.
3. Store the replacement in a suitable secret manager or environment configuration.
4. Remove the secret from current source.
5. Restrict the replacement key to the required API and appropriate request origin where applicable.
6. Review provider usage and billing for unexpected activity.
7. Assess Git history and other copies for continued exposure.
8. Re-scan to verify that the repository no longer exposes the credential.

#### Re-scan Direction

- A matching active Finding becomes `RESOLVED` only after a successful compatible re-scan verifies that the secret is absent from current source and the exposed credential is confirmed invalidated through `USER_ATTESTED` or a future authorized `PROVIDER_VERIFIED` mechanism.
- User confirmation alone cannot replace technical clean-source evidence. Scan Pilot must not use a discovered credential for unauthorized provider validation.
- A resolved Finding with the invalidated value remaining in accessible Git history has remediation quality `RISK_CONTAINED`.
- A resolved Finding has `VERIFIED_COMPLETE` only when an adequate scan also verifies accessible Git history is clean.
- A failed, partial, skipped, incompatible, shallow, or insufficiently authorized history scan cannot produce `VERIFIED_COMPLETE`; when no reliable quality can be assigned, display `NOT_ASSESSED`.
- If the same secret returns to current source after resolution, it becomes `REGRESSED` with `ACTION_REQUIRED`.
- Static secret detection does not require repository code execution.

#### Secret Identity and Safe Fingerprint

`SP-CONFIG-001` uses `SP_SECRET_FP_V1` from `docs/FINDING-TRACKING.md`:

```text
HMAC-SHA-256(
    K_version,
    canonical_length_prefixed(
        scheme_domain,
        stable_workspace_id,
        stable_repository_id,
        SP-CONFIG-001,
        stable_credential_family,
        exact_detected_secret_bytes
    )
)
```

The full HMAC output is stored with scheme and key version. The raw secret is not persisted. Location, commit, branch, detector or rule version, variable name, severity, lifecycle state, and contextual usage classification are not fingerprint inputs.

Google and likely Gemini candidates use `GOOGLE_API_KEY` as their stable credential family. Gemini classification is an evidence-backed claim that may change without changing Finding identity.

Within one repository scope, equal fingerprints under a compatible scheme and key version refer to the same credential Finding and may have multiple current or historical Evidence Locations. A different exact credential produces a separate Finding even when it replaces the previous value at the same location.

Key rotation requires retained-version matching and aliasing a new-version fingerprint to the existing Finding before an old version is retired. The exact cloud key service and rotation operation remain unresolved.

Resolution and remediation quality are not decided by fingerprint equality alone. The fingerprint matches identity; current-source scan evidence, attributable invalidation evidence, and history coverage determine lifecycle and quality under the re-scan contract above.

#### Verification Limit

Automability is `PARTIAL` because static analysis cannot always distinguish a real active credential from sample or test data. Provider validation, where later considered, must avoid unsafe use or unnecessary access.

`VERIFIED_COMPLETE` is limited to the repository refs and history accessible to the completed scan. It does not prove removal from forks, old clones, caches, or external copies and does not claim that the project is generally safe.

Gitleaks detection does not authorize live provider verification. Scan Pilot will not use a discovered credential to call Google or another provider. Any future TruffleHog-style verification is a separate capability requiring explicit authorization and policy.

Tool references:

- Gitleaks official repository and CLI documentation: https://github.com/gitleaks/gitleaks
- Git ancestor check: https://git-scm.com/docs/git-merge-base

## Proposed Stretch Rule Contracts

### SP-CI-001 — Mutable Remote GitHub Actions Reference

| Property | Value |
|---|---|
| Status | Accepted direction; implementation gated by `DEC-061` |
| Priority | Stretch scope |
| Automability | FULL for supported syntax; explicit coverage otherwise |
| Detection | STATIC |
| Primary basis | OWASP Top 10:2025 A03 — Software Supply Chain Failures |
| Supporting guidance | GitHub Actions security hardening; OpenSSF Scorecard Pinned-Dependencies |

#### Purpose

Detect a remote GitHub Action reference in a workflow `uses:` node that does not use an exact full commit SHA. The rule reports a mutable-reference policy risk, never a confirmed compromise.

#### Supported Scope and Classification

The initial scope is readable YAML workflow files under `.github/workflows/` and remote action syntax `OWNER/REPOSITORY@REF`. A compliant `REF` matches exactly `^[0-9a-fA-F]{40}$`.

- tag, branch, short SHA, or expression-backed `REF`: produce `Potential Mutable Remote GitHub Actions Reference`;
- local `./` action: `NOT_APPLICABLE_LOCAL_ACTION`, no Finding;
- `docker://` action: `NOT_APPLICABLE_DOCKER_ACTION`, no Finding;
- reusable workflow path containing `/.github/workflows/`: `NOT_APPLICABLE_REUSABLE_WORKFLOW`, no Finding;
- malformed YAML or unsupported form: explicit coverage outcome, never a clean claim.

The implementation must inspect semantic YAML `uses:` nodes, not apply a raw text rule to every occurrence of `uses:`. It must not execute a workflow, resolve an action remotely, expand expressions, or transmit workflow content to Gemini.

#### Evidence, Severity, and Remediation

Each Finding must retain the captured source commit, repository-relative workflow path, YAML location, normalized remote action reference, reference classification, rule/config version, and coverage status. Do not persist workflow secrets or expanded expressions.

Default severity is `Medium`. Remediation is to replace the reference with a reviewed full commit SHA and maintain it through an approved update process. Finding wording must explain that a mutable reference reduces assurance of the reviewed action version; it must not state that the repository or action is compromised.

#### Lifecycle and Verification

The Finding identity is a deterministic rule-scoped semantic identity over stable repository identity, `SP-CI-001`, normalized workflow path, and normalized remote action target. A compatible successful re-scan that observes a full SHA at the same identity may resolve the Finding; incomplete, skipped, or parse-failed coverage cannot resolve it. A later mutable reference at the same identity is `REGRESSED`.

The implementation requires the test matrix in [`SP-CI-001 research`](research/security/SP-CI-001-MUTABLE-GITHUB-ACTIONS-REFERENCE.md#test-matrix-required-before-implementation-is-accepted), including tags, branches, full SHA, expressions, local actions, Docker actions, reusable workflows, invalid YAML, and re-scan behavior.

#### Verification Limit

The rule establishes only static syntax and snapshot evidence. It does not establish the safety of SHA-pinned code, the effective GitHub organization policy, workflow runtime behavior, action permissions, or full OWASP A03 coverage.

## A01 Research Candidates

The following are not official rules yet:

- `SP-AUTHZ-001` Function-Level Authorization Verification
- `SP-AUTHZ-002` Object-Level Authorization Verification
- `SP-AUTHZ-003` Server-Side Authorization Enforcement
- `SP-AUTHZ-004` Field-Level Authorization Verification

See `docs/research/security/A01-BROKEN-ACCESS-CONTROL.md`.

## Other A02 Candidates Requiring User Review

- production debug enabled;
- detailed error disclosure;
- exposed metadata, directory listing, or management endpoint;
- unsafe CORS policy;
- default credentials or insecure sample configuration;
- security-header verification;
- cloud IAM, public storage, and container hardening.

No item in this list is accepted merely because it appears here.
