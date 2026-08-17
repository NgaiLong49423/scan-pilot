> **Document:** Scan Pilot Product Definition  
> **File:** `docs/PRODUCT.md`  
> **Version:** v0.13.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-16  
> **Status:** Under Review  

# Scan Pilot Product Definition

## Product Vision

Scan Pilot helps people who build software with AI understand which repositories are at risk, what changed, what to fix next, and whether a fix actually improved the project.

It is a continuous monitoring product rather than a one-time AI code review.

## Core User Story

> As a developer managing AI-assisted projects, I want one dashboard that monitors my GitHub repositories and explains evidence-backed problems so that I can prioritize fixes and verify improvement without being a security expert.

## Core Product Loop

```text
Connect repository
→ baseline scan
→ inspect prioritized findings
→ create an action or GitHub Issue
→ fix with a developer or coding agent
→ re-scan
→ mark RESOLVED or REGRESSED
```

## MVP Outcome

The MVP succeeds when a user can complete the core loop against a real repository and understand the result without specialist security knowledge.

## AI Riser Submission MVP

The submission MVP is a focused vertical slice of Product V1:

```text
AI Studio evidence snapshot
→ public Cloud Run production frontend
→ GitHub sign-in and GitHub App installation
→ select one personal-account repository
→ scan current source and reachable Git history
→ show redacted SP-CONFIG-001 evidence
→ use Gemini for explanation and remediation guidance
→ re-scan
→ show Finding lifecycle and remediation quality
```

The Google AI Studio project remains submission evidence for the build stage. The public Scan Pilot frontend and backend run as the real Cloud Run application from GitHub-managed production source. Scan Pilot will not create a separate judge-only mock mode or bypass the normal GitHub onboarding flow. Public and private repositories owned by personal GitHub accounts are eligible only when explicitly selected and authorized; organization support is deferred.

The approved AI Studio prototype is frozen as submission evidence after its one-way handoff. Production source then evolves in GitHub as the source of truth. The two workspaces are not maintained as parallel production codebases.

Submission evidence must include the AI Studio link, the public Cloud Run deployment when available, a public demo video, source access through the actual submission mechanism, and independent validation evidence. Broader Product V1 capabilities are included only after the real end-to-end secret-scanning loop is stable.

Gemini explains redacted findings, remediation steps, and lifecycle transitions. It does not modify repositories, create patches, commit or push code, rewrite Git history, revoke credentials, or decide Finding lifecycle state.

A separate user-owned security-lab repository supplies nonfunctional synthetic secret candidates and known ground truth. The intended real-pipeline demonstration is:

```text
OPEN / ACTION_REQUIRED
→ source removed and credential response recorded
→ RESOLVED / RISK_CONTAINED
→ reachable history cleaned and re-verified
→ RESOLVED / VERIFIED_COMPLETE
```

Required product capabilities currently accepted:

- GitHub repository connection;
- validated current-snapshot and Git-history baseline scans;
- event-driven scan direction for push and relevant GitHub events;
- evidence-backed findings with severity and remediation;
- Gemini-assisted contextual explanation;
- GitHub Issue workflow;
- re-scan and lifecycle tracking;
- working Google Cloud deployment direction;
- a focused set of strong rules rather than a mock-heavy rule catalog;
- Project Discovery and a persistent Repository Profile;
- asynchronous human review for conclusions that require project or business context.

## Monitored Branch Direction

Each repository has one required `PRIMARY` branch and up to two optional `SECONDARY` branches in the MVP. The primary always mirrors the GitHub default branch and is not a separate user preference.

The primary is scanned first during repository initialization. Its exact captured HEAD is checked first so current Findings can appear early, then its full reachable Git history is scanned. Secondary-branch configuration becomes available only after that history baseline completes with validated coverage; detected Findings do not make initialization fail. During normal monitoring, branches scan independently, and a later primary failure does not cancel secondary scans.

After a valid baseline, Scan Pilot uses compatible checkpoints for incremental history scanning. A force-push or other non-ancestor history rewrite causes a new baseline rather than an unsafe clean assumption.

For secret scanning, every Git-tracked content item in the selected scope is considered for eligibility. Supported content is scanned; unsupported or bounded content is shown as skipped with a reason. The product must not present a repository-wide clean claim when the recorded coverage does not support it.

For otherwise eligible supported text, Continuous Monitoring performs full-file secret scanning through `10 MiB`. Larger items remain visible as skipped by the monitoring size policy rather than being labeled safe. The release-oriented verification tier accepts compatible prior evidence and scans eligible full files through `50 MiB`; a required item above that ceiling makes release coverage incomplete. The exact broader MVP scope and workflow of Release Assessment remain under review, but neither tier may use partial-file processing to claim complete coverage.

If GitHub changes the default branch, Scan Pilot synchronizes the primary automatically. When the new default is not already monitored and all three slots are occupied, the former primary leaves the current monitored scope while both user-selected secondary branches remain. Historical scans, evidence, and Findings for the former primary remain available for audit.

## Project Understanding and Human Review

Scan Pilot remembers structured facts about each repository, including detected technologies, important project files, architecture signals, external services, and the scan checkpoint. This state is persisted in the application database and refreshed when relevant evidence changes.

Project Discovery first inventories all content in its captured repository scope. It reads supported repository text, configuration, manifests, CI/CD, IaC, and source-derived signals through deterministic or structured processing. Only selected, bounded, secret-redacted supported text may be classified or summarized by Gemini.

PDF, DOC/DOCX, XLS/XLSX, and PPT/PPTX are visible as inventoried items but their internal content is not extracted or semantically analyzed in the MVP. This limitation remains explicit in Project Discovery and security coverage. Optional user-selected binary document analysis is a possible Phase 2 Project Understanding enhancement, not part of MVP security-baseline completion.

The dashboard includes an Action Center for Review Requests. A user can select a suggested answer, state that they do not know, provide another answer, add free-text context, and reference supporting repository or GitHub evidence. Scans do not pause while awaiting a response.

User responses remain attributed assertions. They help interpretation but do not automatically replace technical evidence or prove that a risk has been fixed.

## Configuration Awareness

Scan Pilot treats configuration as a first-class repository surface. Project Discovery builds a Configuration Map that distinguishes application runtime configuration, build and dependency manifests, CI/CD workflows, container configuration, Infrastructure as Code, and security-tool policy where supported. Classification relies on deterministic path, recognized structure or schema, content, and technology evidence before optional AI assistance.

A configuration change causes the relevant artifact and supported dependent analysis to be reconsidered; it does not become a security Finding merely because the file changed. Secret detection continues to cover eligible content broadly, while misconfiguration Findings require family-specific rules and evidence. Initial deep-analysis research prioritizes Spring Boot, GitHub Actions, and Docker.

The product describes this evidence as repository-declared configuration. It does not claim to know the effective production state when runtime profiles, environment variables, command-line arguments, external configuration stores, or cloud-side settings may override repository values.

Each Configuration Artifact keeps format, technical family, roles, module scope, and exact declared environment or profile labels separate. Recognition, family, parse outcome, and analysis support also remain independent, so a malformed known workflow, an unsupported Terraform artifact, and an unknown YAML file do not collapse into the same result. Family-specific analyzers require deterministic classification and successful required parsing; eligible generic secret analysis remains independent.

Environment interpretation is scenario-based. Repository observations are `DECLARED`; a supported, explicit, repository-complete scenario may be `REPOSITORY_EFFECTIVE`; authorized `RUNTIME_VERIFIED` configuration is a later capability. Branch names and labels such as `prod` or `live` do not prove production use, and user mappings remain attributed assertions.

Git changes create Configuration Change Events and targeted reassessment, not automatic Findings. Direct imports, overrides, environment-file references, activation relationships, and module-context dependencies may invalidate compatible evidence. Unrelated artifacts may reuse evidence only when their content and contextual analyzer contracts remain compatible.

The dashboard separates security attention, verification coverage, and configuration change. Findings and blocked verification appear first; analyzed non-finding changes are grouped. A Configuration Map organizes all artifacts by module and role. Unsupported, ambiguous, or parse-failed artifacts remain visible as neutral coverage limitations rather than clean or critical results, and configuration introduces no new color hierarchy.

## Dashboard Direction

The dashboard is expected to support multiple projects, current health, category findings, last scan, project trend, and action priority. Exact scoring and status thresholds remain unresolved.

Potential project states under consideration:

- `PROTECTED`
- `NEEDS_ATTENTION`
- `AT_RISK`
- `SCAN_REQUIRED`

These state names are product direction, not yet a finalized scoring contract.

## Supported Stack Direction

Deep V1 analysis should prioritize Java/Spring Boot, React/TypeScript, GitHub Actions, Docker, and common configuration formats. Generic secret scanning may cover more languages.

**Reason:** A narrow set of supported stacks enables stronger cross-file and framework-aware evidence than claiming shallow support for every language.

## Out of Scope Unless Later Accepted

- keystroke-level editor monitoring;
- training a proprietary security model for the MVP;
- claiming complete OWASP ASVS compliance;
- executing untrusted repository code inside the main API process;
- supporting every programming language equally in V1;
- automatically fixing or pushing changes without explicit user authorization.

## Review Note

This document remains `Under Review` because exact V1 feature boundaries, final rule count, scoring, and confidence behavior have not been accepted.
