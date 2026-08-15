> **Document:** A02 Security Misconfiguration Research  
> **File:** `docs/research/security/A02-SECURITY-MISCONFIGURATION.md`  
> **Version:** v0.5.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-14  
> **Status:** Under Review  

# A02:2025 — Security Misconfiguration Research

## Research Status

The general Configuration Awareness checkpoint is complete. One security rule, `SP-CONFIG-001`, is accepted. The first deep configuration family and its family-specific rules remain unselected and unaccepted.

## What A02 Means

Security misconfiguration occurs when an application, framework, server, pipeline, container, or cloud service is configured in a way that creates a security weakness.

OWASP examples include debug features in production, default credentials, unnecessary services, detailed error disclosure, insecure framework settings, missing security directives, and overly open cloud permissions.

Primary sources:

- https://owasp.org/Top10/2025/A02_2025-Security_Misconfiguration/
- https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x22-V13-Configuration.md
- https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x25-V16-Security-Logging-and-Error-Handling.md
- https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html

## Accepted Rule

### SP-CONFIG-001 — Source Code Secret Exposure

```text
Priority: MUST
Automability: PARTIAL
Detection: STATIC
Primary basis: OWASP ASVS v5.0.0-13.3.1
```

The rule detects likely credentials or API secrets committed to repository source or build artifacts.

It is `PARTIAL` because a scanner cannot always prove that a suspicious string is a live credential rather than a placeholder or test value.

Required behavior:

- identify file and location without exposing the secret;
- use redacted evidence;
- filter obvious placeholders and environment-variable references;
- distinguish potential from strongly verified findings;
- recommend revocation and replacement, not deletion alone;
- support re-scan to `RESOLVED` and later `REGRESSED` behavior.

### Accepted Google/Gemini Detection Profile

Scan Pilot will recognize a Google API key candidate through a specialized secret detector, then use nearby context and repository location to determine whether it is likely related to Gemini and whether it is exposed in committed content.

The profile distinguishes:

- a possible Google API key based on format evidence;
- a possible Gemini API key based on format plus Gemini context;
- repository exposure based on committed source or configuration location.

Obvious placeholders, redacted examples, and environment-variable references are excluded from confirmed findings. Scan Pilot will not automatically use a discovered credential to call Google for live verification.

**Reason:** Google advises against committing API keys or embedding them in client code, but static evidence cannot always establish whether a key is active, restricted, or used specifically for Gemini. Avoiding live validation protects repository-owner credentials from quota use, logging, and unauthorized handling.

Initial severity is `High`; `Critical` requires later accepted evidence criteria.

Official references:

- https://docs.cloud.google.com/docs/authentication/api-keys-best-practices
- https://docs.cloud.google.com/docs/authentication/api-keys
- https://ai.google.dev/gemini-api/docs/api-key

The official contract is in `docs/INSPECTION-SPEC.md`.

## Candidates Awaiting User Review

| Candidate | Initial research direction | Why it may matter | Verification limit |
|---|---|---|---|
| Production debug enabled | MUST or SHOULD; PARTIAL; STATIC/HYBRID | Debug mode can expose internal details in production. | Repository source may not prove which profile is deployed. |
| Detailed error disclosure | SHOULD; PARTIAL; HYBRID | Stack traces and internal queries can help attackers. | Runtime or production evidence may be necessary. |
| Exposed metadata, directory listing, or management endpoint | SHOULD; PARTIAL; HYBRID | `.git`, listings, docs, or monitoring endpoints can leak information. | Endpoint presence does not prove public exposure. |
| Unsafe CORS policy | SHOULD; PARTIAL; STATIC/HYBRID | Permissive cross-origin access may expose sensitive API data. | A wildcard is not automatically unsafe for a public, credential-free endpoint. |
| Default credentials or insecure sample configuration | SHOULD; PARTIAL; STATIC | Default accounts or credentials are a direct takeover risk. | Samples and fixtures can cause false positives. |
| Security headers | LATER research | Headers can reduce browser attack impact. | Required values vary by app and deployment layer. |
| Cloud IAM, public storage, and container hardening | LATER research | Infrastructure configuration can expose data or expand privileges. | Repository configuration does not necessarily equal live cloud state. |

No row above is an accepted rule merely because it is listed.

## Next Research Task

Compare Spring Boot, GitHub Actions, and Docker and select the first family-specific Configuration Awareness slice. The comparison must cover demo value, recognized security guidance, deterministic identification and parsing, candidate rules, false-positive boundaries, implementation and benchmark cost, and relevance to Scan Pilot's own stack. Do not promote a family or rule until the user accepts it.

**Reason:** The common artifact, classification, scenario, change, and UX contracts are accepted. A narrow first family is now needed to turn that model into a real vertical slice without claiming shallow support for every configuration ecosystem.

Tool research references:

- Gitleaks official repository and CLI documentation: https://github.com/gitleaks/gitleaks
- Git ancestor check: https://git-scm.com/docs/git-merge-base
- OWASP A02:2025 Security Misconfiguration: https://owasp.org/Top10/2025/A02_2025-Security_Misconfiguration/
- Trivy Misconfiguration Scanning: https://trivy.dev/docs/latest/scanner/misconfiguration/
- GitHub Dependency Review: https://docs.github.com/en/pull-requests/how-tos/review-pull-requests/reviewing-dependency-changes-in-a-pull-request
- Spring Boot Externalized Configuration: https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html

The exact Gitleaks version or container digest remains unresolved and must be pinned before implementation. The adapter is required in part because the detector's current official project notice describes Gitleaks as feature complete and limits future releases to security patches; Scan Pilot must retain the ability to replace or supplement it without changing the product rule contract.
