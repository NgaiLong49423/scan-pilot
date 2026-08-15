> **Document:** Scan Pilot Research Sources  
> **File:** `docs/RESEARCH-SOURCES.md`  
> **Version:** v1.9.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-15  
> **Status:** Active  

# Scan Pilot Research Sources

This is the canonical external-source policy for proposing or implementing inspection rules.

## Research Policy

1. Prefer primary and official sources.
2. Use versioned standards references whenever possible.
3. Do not invent a security requirement when a recognized standard already defines it.
4. Do not turn a broad risk category directly into a scanner rule without concrete verification logic.
5. Keep `Standard Requirement`, `Scan Pilot Rule`, and `Finding` separate.
6. Record automability as `FULL`, `PARTIAL`, or `MANUAL`.
7. Record detection method as `STATIC`, `EXECUTION`, `AI`, or `HYBRID`.
8. State verification limits and avoid unsupported compliance claims.

## Comparative Product Research and Attribution

For a material product, security, architecture, scanning, workflow, or UX question, research relevant mature products and tools before proposing a Scan Pilot decision when suitable references exist.

Each comparative research note must record:

| Field | Required content |
|---|---|
| Problem | The specific Scan Pilot question being investigated |
| Source | Product/tool/standards owner, document title, official URL, and access date |
| Observed behavior | What the source explicitly documents or demonstrates |
| Design reason | The stated reason, or a clearly labeled research inference when no reason is stated |
| Applicable lesson | The principle Scan Pilot could adapt |
| Non-transferable detail | Product-specific limits, licensing constraints, scale assumptions, or behavior Scan Pilot should not copy automatically |
| Verification limit | Missing implementation details, inaccessible evidence, version drift, or other uncertainty |
| Decision status | `Research only`, `Proposed`, or the accepted decision reference after user approval |

Research must preserve these distinctions:

```text
External Standard
≠ Product Benchmark
≠ Research Inference
≠ Accepted Scan Pilot Decision
```

Learning from a documented behavior is not plagiarism. Scan Pilot may independently adapt a general engineering pattern while citing where it was learned. Do not copy proprietary code, private implementation details, protected prose, branding, screenshots, or UI assets. Before reusing open-source code or configuration, inspect and comply with its license; a citation alone does not authorize reuse.

When sources disagree, report the difference and the product context behind it instead of choosing silently. When no reliable source is available, state that limitation and do not imply industry consensus.

## Tier 1 — Core Standards

### OWASP Top 10:2025

- Purpose: high-level application-security risk taxonomy, not a direct rule catalog.
- Official: https://owasp.org/Top10/
- A01: https://owasp.org/Top10/2025/A01_2025-Broken_Access_Control/
- A02: https://owasp.org/Top10/2025/A02_2025-Security_Misconfiguration/
- Status: A01 reviewed; A02 in progress.

### OWASP ASVS 5.0.0

- Purpose: primary source of concrete application-security requirements.
- Project: https://owasp.org/www-project-application-security-verification-standard/
- Stable source: https://github.com/OWASP/ASVS/tree/v5.0.0_release/5.0/en
- V8 Authorization: https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x17-V8-Authorization.md
- V13 Configuration: https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x22-V13-Configuration.md
- V16 Logging and Error Handling: https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x25-V16-Security-Logging-and-Error-Handling.md

Preserve full identifiers, for example `v5.0.0-13.3.1`.

### OWASP AISVS 1.0

- Purpose: future AI-specific requirements for AI-enabled applications and agents.
- Official: https://owasp.org/www-project-artificial-intelligence-security-verification-standard-aisvs-docs/
- Status: selected; detailed mapping pending.

### CWE

- Purpose: standardized weakness identifiers for mapping rules and findings.
- Official: https://cwe.mitre.org/
- Status: selected; detailed mapping pending.

## Tier 2 — Practical OWASP Guidance

Reviewed for A01:

- https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Testing_Automation_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Regression_Testing_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html

Reviewed or selected for A02:

- https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html
- https://owasp.org/www-project-secure-headers/

## Tier 3 — Product and Scanner Benchmarks

These are benchmarks, not security standards.

| Source | Research purpose | Status |
|---|---|---|
| GitHub Code Scanning / CodeQL | alert model, evidence, PR workflow | Pending |
| GitHub Secret Scanning | provider-specific detection, lifecycle, remediation | Content-scope and size-boundary benchmark reviewed through 2026-08-14; broader lifecycle review pending |
| GitLab Secret Detection | file exclusions, historic scans, push-protection limits, finding behavior | Content-scope and latency-sensitive size benchmark reviewed through 2026-08-14 |
| Gitleaks | detector behavior, Git/directory modes, configuration precedence, suppression, size, reporting, and archive limits | Adapter trust-policy research accepted as `DEC-037`; exact version and worker behavior still require benchmark verification |
| Git | text/binary content heuristics and repository-controlled attributes | Layered-classification benchmark reviewed on 2026-08-13 |
| Sourcegraph | binary, encoding, file-size, and visible indexing-skip boundaries | Layered-classification benchmark reviewed on 2026-08-13 |
| Apache Tika | local document detection/extraction and hostile-input isolation | Deferred to optional Phase 2 by `DEC-033`; not an MVP dependency |
| Docling | structured document conversion, layout, table, and OCR comparison | Deferred Phase 2 research alternative |
| Google Cloud Document AI | managed document/OCR/layout parsing and Google Cloud fit | Deferred Phase 2 alternative; privacy, authorization, cost, and limits unresolved |
| SARIF | interoperable static-analysis result format | Candidate only |
| Semgrep | rule design, triage, false positives, remediation | Pending |
| SonarQube | project health, quality gates, new-code model | Quality-gate evidence reuse reviewed on 2026-08-14 for the release-oriented size-policy distinction; broader product-health research pending |
| Snyk | dependency monitoring and remediation UX | Pending |
| Trivy | configuration-family detection and family-specific misconfiguration checks | Configuration Awareness direction reviewed and accepted as `DEC-038`; exact adapter or reuse decision remains open |
| Spring Boot | repository and runtime configuration sources, profiles, imports, and override precedence | Reviewed on 2026-08-14 to bound repository-declared configuration claims |
| Docker Compose | family-specific environment precedence, interpolation, and ordered override-file merge | Reviewed on 2026-08-14 for scenario-bounded configuration effect |
| Terraform | configuration-change planning versus current state and speculative-result limits | Reviewed on 2026-08-14 as a non-MVP benchmark for separating text change from runtime effect |
| GitLab IaC | supported configuration scope and linked finding/report UX | Reviewed on 2026-08-14 for attention, coverage, and change separation |
| Snyk IaC | per-configuration issue presentation and explicit support boundaries | Reviewed on 2026-08-14 for Configuration Map and Finding separation |
| Academic SecretBench | large labeled secret dataset and research-grounded detector evaluation | Reviewed on 2026-08-15; real-secret data requires an agreement and is excluded from the public MVP protocol |
| SecretBench false-negative battery | independent recall-oriented credential-format variations | Candidate safe battery reviewed on 2026-08-15; fixtures require safety and license audit before execution |
| OWASP SEDATED | explicit positive/negative secret-regex fixtures and documented limitations | Historical supplemental source reviewed on 2026-08-15 |
| OWASP Benchmark | runnable known-ground-truth applications and reproducible scorecards | Future non-secret rule benchmark; not evidence for `SP-CONFIG-001` |
| NIST SARD / Juliet | documented weakness test programs for static-analysis evaluation | Future rule-family source; not evidence for secret scanning |

Official links:

- https://docs.github.com/en/code-security/code-scanning
- https://docs.github.com/en/code-security/secret-scanning
- https://docs.gitlab.com/user/application_security/secret_detection/pipeline/
- https://docs.gitlab.com/user/application_security/secret_detection/secret_push_protection/
- https://docs.gitlab.com/user/application_security/secret_detection/exclusions/
- https://github.com/gitleaks/gitleaks
- https://trivy.dev/docs/latest/scanner/misconfiguration/
- https://docs.github.com/en/pull-requests/how-tos/review-pull-requests/reviewing-dependency-changes-in-a-pull-request
- https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html
- https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/
- https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/
- https://docs.github.com/en/actions/concepts/workflows-and-actions/variables
- https://developer.hashicorp.com/terraform/cli/commands/plan
- https://git-scm.com/docs/git-diff.html
- https://git-scm.com/docs/gitdiffcore
- https://docs.gitlab.com/user/application_security/iac_scanning/
- https://docs.snyk.io/scan-with-snyk/snyk-iac/getting-started-with-current-iac
- https://raw.githubusercontent.com/gitleaks/gitleaks/master/cmd/root.go
- https://raw.githubusercontent.com/gitleaks/gitleaks/master/cmd/git.go
- https://raw.githubusercontent.com/gitleaks/gitleaks/master/LICENSE
- https://git-scm.com/docs/gitattributes
- https://sourcegraph.com/docs/admin/search
- https://tika.apache.org/3.2.2/formats.html
- https://cwiki.apache.org/confluence/display/TIKA/The%2BRobustness%2Bof%2BApache%2BTika
- https://docling-project.github.io/docling/concepts/architecture/
- https://docling-project.github.io/docling/reference/document_converter/
- https://docs.cloud.google.com/document-ai/docs/file-types
- https://docs.cloud.google.com/document-ai/docs/layout-parse-chunk
- https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
- https://semgrep.dev/docs/
- https://docs.sonarsource.com/sonarqube-server/
- https://docs.snyk.io/
- https://github.com/setu1421/SecretBench
- https://doi.org/10.1109/MSR59073.2023.00053
- https://github.com/brendtmcfeeley/SecretBench
- https://github.com/OWASP/SEDATED
- https://owasp.org/www-project-benchmark/
- https://www.nist.gov/itl/csd/secure-systems-and-applications/samate/software-assurance-reference-dataset-sard
- https://samate.nist.gov/SARD/test-suites

## Tier 4 — Secure Development Lifecycle

### NIST SSDF

- Purpose: broader secure-development lifecycle and risk-management coverage.
- Official: https://csrc.nist.gov/projects/ssdf
- Status: lifecycle mapping pending.

## Current Research Order

```text
A01 checkpoint completed
→ A02 research and SP-CONFIG-001 specification
→ continue OWASP Top 10 category mapping as justified
→ benchmark GitHub Secret Scanning for the first vertical slice
→ benchmark normalized finding and lifecycle models
→ scoring and quality-gate research
→ NIST SSDF lifecycle mapping
→ OWASP AISVS AI-specific mapping
```

The Document Extraction Adapter benchmark is retained but deferred beyond the MVP by `DEC-033`; it is not part of the current research order.

For each proposed rule, record the source, exact requirement ID, evidence, automability, detection method, false-positive limits, and V1 priority. User acceptance is required before promotion into `docs/INSPECTION-SPEC.md` as an official rule.
