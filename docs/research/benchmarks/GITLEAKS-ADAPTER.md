> **Document:** Gitleaks Adapter Trust and Verification Benchmark  
> **File:** `docs/research/benchmarks/GITLEAKS-ADAPTER.md`  
> **Version:** v0.1.0  
> **Created:** 2026-08-14  
> **Last Updated:** 2026-08-14  
> **Status:** Under Review  

# Gitleaks Adapter Trust and Verification Benchmark

## Research Question

How must Scan Pilot invoke and isolate Gitleaks so an untrusted repository cannot silently redefine the `SP-CONFIG-001` baseline and the resulting evidence, coverage, and cleanup contract can be verified?

**Decision status:** Scan Pilot ownership of the trusted detector policy is accepted as `DEC-037`. The exact Gitleaks version, immutable artifact digest, `.gitleaksignore` isolation technique, command matrix, timeout, telemetry extraction, report parsing, and cleanup implementation remain under review.

## Sources Reviewed

| Source owner | Official source | Accessed | Research role |
|---|---|---:|---|
| Gitleaks | [Official repository and CLI documentation](https://github.com/gitleaks/gitleaks) | 2026-08-14 | Supported modes, flags, configuration precedence, reporting, redaction, timeouts, and project-maintenance notice |
| Gitleaks | [Root command source](https://raw.githubusercontent.com/gitleaks/gitleaks/master/cmd/root.go) | 2026-08-14 | Config and ignore discovery, exact flag defaults, size-unit implementation, report writing, and exit behavior |
| Gitleaks | [Git command source](https://raw.githubusercontent.com/gitleaks/gitleaks/master/cmd/git.go) | 2026-08-14 | Git-log invocation, partial scan handling, and finding flow |
| Gitleaks | [CLI MIT license](https://raw.githubusercontent.com/gitleaks/gitleaks/master/LICENSE) | 2026-08-14 | Scanner reuse and attribution boundary |

## Observed Behavior

### Configuration precedence

Gitleaks documents and implements configuration precedence as explicit `--config`, configuration environment variables, target `.gitleaks.toml`, then the built-in default. An adapter that omits an explicit trusted configuration can therefore let repository content redefine the detector.

**Applicable lesson:** Scan Pilot must supply a trusted config and persist its digest with coverage.

### Repository suppression discovery

The reviewed command source automatically inspects target `.gitleaksignore`. Inline `gitleaks:allow` behavior is active unless the dedicated ignore-allow flag is requested.

**Applicable lesson:** Passing a trusted rule config is necessary but insufficient; suppression isolation is a separate adapter responsibility.

**Verification limit:** The research has not yet established which detector-view technique disables target `.gitleaksignore` while preserving accurate Git history, paths, lines, and immutable source evidence.

### Redaction and sensitive reports

The CLI supports full redaction, but the reviewed flag initialization requires the adapter to request it explicitly rather than inferring safety from descriptive text. JSON reports contain fields capable of carrying raw match and secret material before Scan Pilot normalization.

**Applicable lesson:** `--redact=100` is defense in depth, while trusted normalization and immediate raw-report cleanup remain mandatory.

### Size units

The reviewed implementation defines its megabyte constant as `1,000,000 bytes`; Scan Pilot's accepted policy defines `1 MiB` as `1,048,576 bytes`.

**Applicable lesson:** Scan Pilot applies exact-byte eligibility before invocation. The Gitleaks size flag can only be a secondary resource control.

### Scan result and exit behavior

Gitleaks uses a configurable nonzero exit code when findings exist and also exits nonzero on scan errors. Its Git path can report findings from a partial scan before exiting with failure.

**Applicable lesson:** Exit code alone cannot distinguish valid findings coverage from partial or failed scanning. Scan Pilot must combine parsed output with its independently measured scope and telemetry.

### License and maintenance boundary

The standalone Gitleaks CLI repository uses the MIT License. The separately distributed Gitleaks GitHub Action has its own licensing terms and is not the component selected for the Scan Pilot worker. The official repository currently describes Gitleaks as feature complete with future work focused on security patches.

**Applicable lesson:** Scan Pilot should invoke a pinned standalone CLI artifact behind an adapter, preserve required MIT notices if redistribution occurs, and retain detector replaceability.

## Accepted Scan Pilot Outcome

`DEC-037` establishes:

```text
untrusted repository
→ immutable source snapshot
→ Scan Pilot-controlled detector view
→ pinned Gitleaks CLI
→ explicit trusted config and digest
→ unapproved config environment removed
→ inline allow directives ignored
→ repository ignore suppression proven inactive
→ fully redacted temporary report
→ trusted normalization and fingerprinting
→ raw report deleted
```

Repository Gitleaks files remain inventory evidence, not baseline instructions. Legitimate suppression belongs to a future Scan Pilot-owned, attributed workflow.

## Required Benchmark Matrix

The benchmark must use synthetic non-production fixtures and cover:

1. exact-HEAD directory scan and graph-aware Git-history scan;
2. a repository `.gitleaks.toml` that attempts to change enabled rules;
3. a repository `.gitleaksignore` that attempts to suppress a fixture finding;
4. an inline `gitleaks:allow` directive;
5. inherited `GITLEAKS_CONFIG` and `GITLEAKS_CONFIG_TOML` values;
6. exact byte boundaries around `10 MiB` and `50 MiB`, plus representative `1`, `25`, and `100 MiB` files;
7. no finding, finding, invalid config, timeout, cancellation, malformed report, unwritable report, and partial Git scan cases;
8. report schema parsing, full redaction, normalized safe evidence, and raw-report deletion on success and failure;
9. detector version, artifact digest, config digest, adapter version, elapsed time, peak memory, processed bytes, and independently measured Git coverage.

## Acceptance Criteria for the Future Benchmark

- Repository configuration and suppression fixtures do not alter baseline results.
- Exact-byte size routing matches `DEC-036`, independent of Gitleaks decimal-megabyte behavior.
- A finding exit, scanner failure, timeout, cancellation, partial scan, and malformed report remain distinguishable.
- No raw fixture secret appears in persisted results, queues, logs, metrics, errors, or AI inputs.
- Temporary raw reports are removed after both successful normalization and failure cleanup.
- Zero or unknown Git coverage cannot produce a clean checkpoint.
- The selected CLI version and artifact digest are recorded and reproducible.

## Trade-offs and Verification Limits

Ignoring repository suppressions can increase false positives and requires a Scan Pilot-owned review workflow. Creating a suppression-neutral detector view adds adapter complexity and must not change immutable evidence or Git semantics. Public source review does not substitute for executing the pinned binary in the intended isolated worker, and no benchmark has yet established the final command line, timeout, memory envelope, or supported report schema.

This note records general behavior and independently designed Scan Pilot policy. It does not copy Gitleaks implementation into Scan Pilot. Any binary redistribution must comply with the CLI's MIT License and preserve required notices.
