> **Document:** Scan Pilot Research Sources  
> **File:** `docs/RESEARCH-SOURCES.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-12  
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
| GitHub Secret Scanning | provider-specific detection, lifecycle, remediation | Pending; high priority for `SP-CONFIG-001` |
| SARIF | interoperable static-analysis result format | Candidate only |
| Semgrep | rule design, triage, false positives, remediation | Pending |
| SonarQube | project health, quality gates, new-code model | Pending |
| Snyk | dependency monitoring and remediation UX | Pending |

Official links:

- https://docs.github.com/en/code-security/code-scanning
- https://docs.github.com/en/code-security/secret-scanning
- https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
- https://semgrep.dev/docs/
- https://docs.sonarsource.com/sonarqube-server/
- https://docs.snyk.io/

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

For each proposed rule, record the source, exact requirement ID, evidence, automability, detection method, false-positive limits, and V1 priority. User acceptance is required before promotion into `docs/INSPECTION-SPEC.md` as an official rule.
