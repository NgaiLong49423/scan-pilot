> **Document:** Configuration Awareness Research  
> **File:** `docs/research/benchmarks/CONFIGURATION-AWARENESS.md`  
> **Version:** v0.2.0  
> **Created:** 2026-08-14  
> **Last Updated:** 2026-08-14  
> **Status:** Under Review  

# Configuration Awareness Research

## Research Question

How should Scan Pilot discover, remember, monitor, and analyze repository configuration without treating every structured text file alike or claiming that repository values prove effective production state?

**Decision status:** The general Configuration Awareness contract is accepted as `DEC-038` through `DEC-043`. Exact taxonomies, parser implementations, UI wireframes, and family-specific rules remain under review.

## Comparative Sources

| Source owner | Official source | Accessed | Observed behavior | Applicable lesson | Non-transferable detail or verification limit |
|---|---|---|---|---|---|
| OWASP | [A02:2025 Security Misconfiguration](https://owasp.org/Top10/2025/A02_2025-Security_Misconfiguration/) | 2026-08-14 | Identifies insecure settings across application, framework, server, cloud, and deployment layers and recommends repeatable automated configuration verification. | Configuration deserves explicit security coverage, but broad risk descriptions must still be converted into evidence-backed rules. | OWASP A02 is a risk category, not a parser, artifact taxonomy, or directly executable rule set. |
| Aqua Security / Trivy | [Misconfiguration Scanning](https://trivy.dev/docs/latest/scanner/misconfiguration/) | 2026-08-14 | Detects known Infrastructure as Code families such as Docker, Kubernetes, Terraform, CloudFormation, Helm, and Azure ARM, then applies family-specific checks. | Detect the technical family before selecting checks; one generic YAML or text rule is not sufficient. | Trivy's supported families and policies are product behavior, not automatically accepted Scan Pilot scope. Tool selection and license review remain separate. |
| GitHub | [Reviewing dependency changes in a pull request](https://docs.github.com/en/pull-requests/how-tos/review-pull-requests/reviewing-dependency-changes-in-a-pull-request) | 2026-08-14 | Parses supported manifest and lock files to present dependency additions, removals, updates, and vulnerability context, while warning that unsupported or non-dependency changes still require source-diff review. | Configuration and manifest changes can receive specialized interpretation while preserving explicit support and coverage limits. | Pull Request scanning is outside the accepted repository-scan scope and remains a separate future capability. |
| Spring | [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html) | 2026-08-14 | Documents ordered property sources, profile-specific files, imports, environment variables, system properties, and command-line overrides. | Repository files provide scoped evidence, not guaranteed effective runtime values; profile and precedence relationships must be modeled cautiously. | The reviewed Spring Boot documentation does not define other ecosystems and cannot prove a deployed environment's actual configuration. |
| Docker | [Compose environment-variable precedence](https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/) and [Merge Compose files](https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/) | 2026-08-14 | Defines family-specific precedence across CLI, shell, environment files, Compose declarations, image values, and ordered override files. | Activation and override semantics require a family-specific scenario model rather than a universal environment merge. | Repository files cannot reveal every CLI, shell, image, or deployment-time value. |
| GitHub | [Actions variables](https://docs.github.com/en/actions/concepts/workflows-and-actions/variables) | 2026-08-14 | Configuration variables may live at workflow, organization, repository, or environment scope, and sensitive data should use secrets. | Repository workflow evidence cannot prove all platform-side values or secret state. | Access to platform metadata and secrets requires separate permissions; secret values should not be retrieved for configuration memory. |
| HashiCorp | [Terraform plan](https://developer.hashicorp.com/terraform/cli/commands/plan) | 2026-08-14 | Separates written configuration change from a scenario and state-aware proposed effect, and warns that later remote-state change can alter the result. | Git diff should trigger analysis; it does not by itself prove runtime or infrastructure impact. | Terraform execution and remote-state access are not accepted MVP capabilities. |
| Git | [git-diff](https://git-scm.com/docs/git-diff.html) and [diffcore rename detection](https://git-scm.com/docs/gitdiffcore) | 2026-08-14 | Represents additions, modifications, deletions, and heuristic rename or copy similarity. | Rename evidence is useful history context but not permanent Configuration Artifact identity. | Similarity thresholds are Git behavior and cannot establish semantic equivalence. |
| GitLab | [Infrastructure as Code scanning](https://docs.gitlab.com/user/application_security/iac_scanning/) | 2026-08-14 | Separates branch findings and default-branch vulnerabilities and exposes severity, location, scanner, identifiers, remediation, and supported-format limits. | Security Findings, coverage, and change context should remain distinct but linked. | GitLab's tiers, pipeline model, supported families, and UI are not Scan Pilot requirements. |
| Snyk | [Snyk IaC](https://docs.snyk.io/scan-with-snyk/snyk-iac/getting-started-with-current-iac) | 2026-08-14 | Presents issues for scanned configuration files and explicitly limits supported environments. | A Configuration Map can inventory artifacts while Findings remain rule-based and support limits remain visible. | Scan Pilot does not copy Snyk UI, proprietary implementation, or supported-scope claims. |

## Accepted General Contract

Scan Pilot will:

- inventory Configuration Artifacts during Project Discovery;
- classify technical family and repository role from deterministic evidence before optional AI assistance;
- keep secret scanning separate from family-specific misconfiguration rules;
- treat a configuration change as a reassessment trigger rather than a Finding by itself;
- retain source identity, digest, classifier or parser version, and safe derived facts for compatible evidence reuse;
- describe static evidence as repository-declared configuration rather than verified production state;
- prioritize Spring Boot, GitHub Actions, and Docker for initial deep-analysis research.
- model each artifact through separate format, family, roles, module, and exact declared labels, with optional parser-derived logical units;
- keep recognition, family, parse outcome, and analysis support independent and route family rules only from sufficient deterministic evidence;
- preserve exact environment/profile labels, family-specific activation and precedence, and `DECLARED` versus scenario-scoped `REPOSITORY_EFFECTIVE` claims while deferring `RUNTIME_VERIFIED`;
- record Git changes as Configuration Change Events, invalidate direct configuration relationships, and reuse evidence only under compatible content and contextual versions;
- separate Security Attention, Verification Coverage, and Configuration Change in the dashboard and Configuration Map without adding another color hierarchy.

## Next Decision Boundary

The next checkpoint selects the first deep configuration family among Spring Boot, GitHub Actions, and Docker, then defines its deterministic fixtures, parser boundary, scenario support, initial rule candidates, false-positive limits, evidence contract, and benchmark plan.

No family-specific security rule is accepted by this note.
