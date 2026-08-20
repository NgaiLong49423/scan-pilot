> **Document:** SP-CI-001 Mutable GitHub Actions Reference Research
> **File:** `docs/research/security/SP-CI-001-MUTABLE-GITHUB-ACTIONS-REFERENCE.md`
> **Version:** v0.1.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Under Review

# SP-CI-001 — Mutable Remote GitHub Actions Reference

## Problem

Scan Pilot needs one bounded second inspection rule that demonstrates a reusable multi-rule platform without weakening the submission-critical `SP-CONFIG-001` secret-scanning loop. The rule must be deterministic, operate only on repository content, create explainable evidence, and avoid claiming that a repository has been compromised.

## External Standard

| Source owner | Source | Accessed | Observed behavior | Applicable lesson | Verification limit |
|---|---|---:|---|---|---|
| OWASP | [Top 10 2025 A03 — Software Supply Chain Failures](https://owasp.org/Top10/2025/A03_2025-Software_Supply_Chain_Failures/) | 2026-08-20 | Calls for inventorying and hardening CI/CD and supply-chain components; identifies weak CI/CD as a supply-chain concern. | A repository scanner can surface a narrow, testable CI/CD supply-chain policy rather than claim complete A03 coverage. | A03 is a broad risk category, not a direct detection rule. |
| GitHub | [Protecting against security threats](https://docs.github.com/en/code-security/tutorials/secure-your-organization/protect-against-threats) | 2026-08-20 | A tag can move; pinning third-party actions to a full commit SHA runs the reviewed code. | A full SHA is the initial immutable-reference policy for remote action invocations. | GitHub guidance does not establish that every tag reference is malicious or compromised. |
| GitHub | [GitHub Actions policy syntax](https://docs.github.com/en/enterprise-cloud@latest/admin/enforcing-policies/enforcing-policies-for-your-enterprise/enforcing-policies-for-github-actions-in-your-enterprise) | 2026-08-20 | Distinguishes action syntax, reusable-workflow syntax, and local `./` actions; repository policy does not restrict local actions. | The first rule must classify these forms before deciding whether its policy applies. | Enterprise policy behavior is a benchmark, not Scan Pilot runtime behavior. |
| OpenSSF | [Scorecard Pinned-Dependencies check](https://github.com/ossf/scorecard/blob/main/docs/checks/internal/checks.yaml) | 2026-08-20 | Checks workflow dependencies for hash pinning, reports medium possible-compromise risk, and notes pinning requires an update process. | Report a policy risk with clear remediation and retain the update trade-off. | Scorecard covers wider dependency surfaces and has its own scoring model; Scan Pilot does not copy it. |

## Research Inference

The smallest credible second rule is a static check of GitHub Actions workflow `uses:` nodes that invoke a remote action in the form `OWNER/REPOSITORY@REF`. A reference is compliant only when `REF` is exactly a 40-character hexadecimal Git commit SHA. A tag, branch, short SHA, or expression-backed reference is mutable or cannot be verified as immutable by this rule.

This is a repository-declared CI/CD policy observation. It does not prove action takeover, malware execution, a vulnerable dependency, effective organization policy, or general OWASP A03 compliance.

## Proposed Rule Boundary

| Workflow `uses:` form | SP-CI-001 outcome | Reason |
|---|---|---|
| `owner/action@v4` | Finding: mutable reference | Tag may move. |
| `owner/action@<40-hex-SHA>` | No finding | Satisfies this rule's immutable-reference policy. |
| `owner/action@${{ expression }}` | Finding: unverifiable mutable reference | The static rule cannot establish a fixed SHA. |
| `./.github/actions/my-action` | `NOT_APPLICABLE_LOCAL_ACTION`, no finding | It is a local action, not a remote action reference. |
| `docker://alpine:3.20` | `NOT_APPLICABLE_DOCKER_ACTION`, no finding | Docker image digest pinning is a separate rule. |
| `owner/repo/.github/workflows/build.yml@main` | `NOT_APPLICABLE_REUSABLE_WORKFLOW`, no finding | Reusable-workflow policy and semantics are deliberately outside this first rule. |

Malformed YAML, unsupported `uses:` forms, and read failures are coverage outcomes, never clean results. The rule must record them without producing a vulnerability Finding.

## Proposed Finding Contract

**Title:** `Mutable Remote GitHub Actions Reference`

**User-facing meaning:** A remote GitHub Action is referenced by a tag, branch, short SHA, or expression rather than a fixed full commit SHA. This reduces assurance that the code run in CI is the exact version previously reviewed.

**Default severity:** `Medium`.

**Evidence:** captured repository commit, workflow path, YAML location, normalized action owner/repository, reference classification, rule/config version, and coverage status. The evidence must not contain workflow secrets or expanded expressions.

**Remediation:** replace the reference with a reviewed full commit SHA and maintain it using an approved dependency-update workflow. Do not suggest that a mutable reference proves compromise.

## Decision Status

**Proposed rule contract; accepted product direction in `DEC-061`.** Implementation remains blocked until the stabilization and foundation gates recorded in Issue `#58` are satisfied.

## Test Matrix Required Before Implementation Is Accepted

| Case | Expected result |
|---|---|
| Remote action with tag | One `Potential Mutable Remote GitHub Actions Reference` Finding. |
| Remote action with branch | One Finding. |
| Remote action with exactly 40 hexadecimal SHA characters | No Finding; scanned evidence records compliant reference. |
| Remote action with short SHA or expression | One Finding with the appropriate reference classification. |
| Local action | No Finding; not-applicable coverage record. |
| Docker action | No Finding; not-applicable coverage record. |
| Reusable workflow | No Finding; not-applicable coverage record. |
| Invalid workflow YAML | No clean claim; explicit parse-failure coverage record. |
| Re-scan after tag-to-SHA change | Existing Finding becomes `RESOLVED` only after compatible successful coverage. |

## Non-transferable Details

- Scan Pilot does not copy GitHub's enterprise enforcement controls or assume the repository can enable them.
- Scan Pilot does not reuse OpenSSF Scorecard code, scoring, allowlists, or broad Docker/shell dependency scope.
- This rule performs no remote action lookup, signature verification, workflow execution, or network call.

## Verification Limit

The evidence proves only the syntax and repository snapshot observed. A full SHA does not prove the referenced code is safe; a mutable reference does not prove compromise. Workflow behavior, organization policy, runner state, action permissions, and production deployment effects remain outside the rule.
