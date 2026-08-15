> **Document:** Secret Detection Validation Research  
> **File:** `docs/research/benchmarks/SECRET-DETECTION-VALIDATION.md`  
> **Version:** v0.1.0  
> **Created:** 2026-08-15  
> **Last Updated:** 2026-08-15  
> **Status:** Under Review  

# Secret Detection Validation Research

## Question

How should Scan Pilot demonstrate that its first security rule works without relying only on self-authored tests or on Gitleaks' own fixtures?

## Accepted Boundary

Validation has three separate layers:

```text
Independent detector benchmark
→ adapter and integration verification
→ deployed end-to-end security-lab journey
```

The layers answer different questions and must not be combined into one unsupported effectiveness claim.

1. **Detector benchmark:** How accurately does the selected detector recognize a known safe test battery?
2. **Adapter and integration verification:** Does Scan Pilot invoke the detector correctly, redact output, normalize Findings, validate coverage, and update checkpoints safely?
3. **End-to-end lab:** Can a user complete the real onboarding, scan, explanation, remediation, and re-scan lifecycle against a controlled repository?

## Sources Reviewed

### Academic SecretBench

- **Owner:** Setu Kumar Basak, Lorenzo Neil, Bradley Reaves, and Laurie Williams
- **Source:** [SecretBench repository](https://github.com/setu1421/SecretBench) and [MSR 2023 paper](https://doi.org/10.1109/MSR59073.2023.00053)
- **Accessed:** 2026-08-15
- **License and access:** Repository code and metadata state MIT licensing, but the dataset contains sensitive information and requires an agreement with the researchers.
- **Observed behavior:** The published metadata describes 97,479 candidates, 15,084 labeled true secrets, 818 public GitHub repositories, 49 programming languages, and 311 file types.
- **Applicable lesson:** It is a substantial independent research benchmark and shows the value of labeled positive and negative candidates across varied source contexts.
- **Not transferred:** Scan Pilot will not use, redistribute, upload to Gemini, or place this real-secret dataset in a public demo without the required agreement, ethics review, and a separately accepted handling protocol.
- **Verification limit:** Public metadata was reviewed; dataset access and safe execution were not performed.

### SecretBench False-Negative Battery

- **Owner:** GitHub repository `brendtmcfeeley/SecretBench`; its README names Ryan Delaney as the contact and attributes the original battery to OWASP SEDATED
- **Source:** [brendtmcfeeley/SecretBench](https://github.com/brendtmcfeeley/SecretBench)
- **Accessed:** 2026-08-15
- **License:** BSD-3-Clause
- **Observed behavior:** The project provides credentials, keys, and token patterns in formatting and syntax variations to evaluate false-negative behavior.
- **Applicable lesson:** It is a safer independent candidate battery for recall-oriented validation of secret detectors.
- **Not transferred:** It is not a complete precision benchmark and cannot alone justify a broad accuracy claim.
- **Verification limit:** Repository purpose and license were reviewed; the battery has not yet been audited for nonfunctional values or executed against the pinned Gitleaks version.

### OWASP SEDATED

- **Owner:** OWASP Foundation project
- **Source:** [OWASP/SEDATED](https://github.com/OWASP/SEDATED)
- **Accessed:** 2026-08-15
- **License:** BSD-3-Clause
- **Observed behavior:** SEDATED prevents sensitive data from being pushed through a Git pre-receive workflow. Its offline regex test script uses explicitly expected positive and negative cases.
- **Applicable lesson:** Positive and negative fixtures should be versioned together with clear expected outcomes, and detector limitations must remain explicit.
- **Not transferred:** Scan Pilot does not copy SEDATED's product workflow, branding, or implementation. The project is historical and regex-oriented, while Scan Pilot uses Gitleaks behind an adapter.
- **Verification limit:** The repository documentation was reviewed; no code or fixture reuse is accepted until the exact files and license obligations are audited.

### OWASP Benchmark

- **Owner:** OWASP Foundation
- **Source:** [OWASP Benchmark](https://owasp.org/www-project-benchmark/)
- **Accessed:** 2026-08-15
- **License:** The Java repository states GPL-2.0; any future reuse requires a separate license review.
- **Observed behavior:** The project provides runnable applications, known expected results, and scorecard tooling for comparing the accuracy, coverage, and speed of SAST, DAST, and IAST tools.
- **Applicable lesson:** Future non-secret Scan Pilot rules should use known ground truth, versioned tool output, and reproducible scoring rather than screenshots alone.
- **Not transferred:** OWASP Benchmark does not currently serve as evidence for `SP-CONFIG-001`; its vulnerability families and runtime requirements are different.
- **Verification limit:** Official project documentation was reviewed; no benchmark was downloaded or run.

### NIST SARD and Juliet

- **Owner:** United States National Institute of Standards and Technology
- **Source:** [Software Assurance Reference Dataset](https://www.nist.gov/itl/csd/secure-systems-and-applications/samate/software-assurance-reference-dataset-sard) and [SARD test suites](https://samate.nist.gov/SARD/test-suites)
- **Accessed:** 2026-08-15
- **Observed behavior:** SARD contains documented weakness test programs ranging from synthetic cases to large applications. Juliet provides many language-specific cases intended for static-analysis tool evaluation.
- **Applicable lesson:** SARD or Juliet may support later rule families when the exact CWE, language, and detector contract match.
- **Not transferred:** These suites do not prove secret-scanning effectiveness and are not part of the submission MVP validation path.
- **Verification limit:** Official catalog and documentation were reviewed; individual suites and their current license terms were not audited for reuse.

### Gitleaks Fixtures

- **Owner:** Gitleaks open-source project
- **Source:** [Gitleaks repository](https://github.com/gitleaks/gitleaks)
- **Accessed:** 2026-08-15
- **Applicable lesson:** Upstream fixtures are useful for compatibility and regression tests of the pinned detector version.
- **Not independent:** They are produced by the detector project itself and therefore cannot be the sole evidence that Scan Pilot or Gitleaks performs well on an external test set.

## Proposed Safe Submission Protocol

The exact benchmark battery remains a technical checkpoint, but it must satisfy these accepted constraints:

- only nonfunctional synthetic candidates or fixtures audited as safe;
- no live credential validation and no provider calls using detected values;
- no raw candidate values in logs, screenshots, reports, prompts, or persistent Findings;
- pinned benchmark revision, detector version, Scan Pilot rule/config digest, and execution environment;
- expected labels stored separately from detector output;
- repeatable commands and machine-readable results;
- documented exclusions, failed cases, timeouts, and incomplete coverage;
- public reporting limited to evidence that the relevant licenses permit.

At minimum, the detector report should calculate true positives, false positives, false negatives, and true negatives when the battery supports all four classes. Derived metrics may include precision, recall, and F1, but Scan Pilot must not claim whole-product accuracy from a detector-only result.

## End-to-End Security-Lab Repository

The deployed demonstration uses a separate repository owned by the user, not the Scan Pilot production repository. It contains nonfunctional synthetic candidates and an explicit ground-truth manifest outside the scanned evidence path.

The demonstration must exercise the real pipeline:

```text
GitHub authorization
→ immutable snapshot and history scan
→ redacted Finding
→ Gemini explanation from redacted context
→ user performs remediation outside Scan Pilot
→ re-scan
→ lifecycle and remediation-quality transition
```

The lab may contain multiple controlled commits so the demo can prove that current-source cleanup and reachable-history cleanup are distinct. Scan Pilot must never auto-commit, auto-push, rewrite history, or revoke credentials in this flow.

## Accepted Decision

`DEC-049` requires independent validation evidence and `DEC-050` requires the controlled security-lab journey. The exact safe battery, pinned revisions, execution commands, and publication format remain unresolved technical work. No benchmark was executed in this research checkpoint.
