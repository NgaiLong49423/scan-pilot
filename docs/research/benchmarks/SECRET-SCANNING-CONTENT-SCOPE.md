> **Document:** Secret Scanning Content Scope Benchmark  
> **File:** `docs/research/benchmarks/SECRET-SCANNING-CONTENT-SCOPE.md`  
> **Version:** v0.4.0  
> **Created:** 2026-08-13  
> **Last Updated:** 2026-08-14  
> **Status:** Under Review  

# Secret Scanning Content Scope Benchmark

## Research Question

How should Scan Pilot decide which Git-tracked content is eligible for `SP-CONFIG-001` secret scanning without claiming that every byte can be processed successfully?

**Decision status:** The eligibility principle was accepted as `DEC-029`; layered classification and persistent skip records were accepted as `DEC-034`; the initial two-tier full-file size policy was accepted as `DEC-036`. Exact classifier implementation and remaining content limits stay under review.

## Sources Reviewed

| Source owner | Official source | Accessed | Research role |
|---|---|---:|---|
| GitHub | [Secret scanning](https://docs.github.com/en/code-security/concepts/secret-security/secret-scanning) | 2026-08-13 | Repository and Git-history scope benchmark |
| GitLab | [Pipeline secret detection](https://docs.gitlab.com/user/application_security/secret_detection/pipeline/) | 2026-08-13 | Historic scanning, incremental behavior, and default exclusions |
| GitLab | [Secret push protection](https://docs.gitlab.com/user/application_security/secret_detection/secret_push_protection/) | 2026-08-13 | Latency-sensitive size, binary, and push-volume limits |
| GitLab | [Secret detection exclusions](https://docs.gitlab.com/user/application_security/secret_detection/exclusions/) | 2026-08-13 | User-configured exclusion model |
| Gitleaks | [Official repository and CLI documentation](https://github.com/gitleaks/gitleaks) | 2026-08-13 | Detector size, archive, Git, and directory controls |
| GitHub | [Repository limits](https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits) | 2026-08-14 | Recommended and enforced Git object size boundaries |
| SonarSource | [Quality gates](https://docs.sonarsource.com/sonarqube-server/2026.1/quality-standards-administration/managing-quality-gates/introduction-to-quality-gates) | 2026-08-14 | Continuous-analysis results reused in release-readiness evaluation |
| Git | [gitattributes](https://git-scm.com/docs/gitattributes) | 2026-08-13 | Content heuristics and repository-controlled text or binary hints |
| Sourcegraph | [Search configuration](https://sourcegraph.com/docs/admin/search) | 2026-08-13 | Binary, UTF-8, size, and visible indexing-skip boundaries |

## Observed Product Behavior

### GitHub Secret Scanning

GitHub documents repository secret scanning across the entire Git history on all branches and also covers several GitHub-hosted collaboration surfaces. Its public description is repository- and history-oriented rather than limited to conventional source directories.

**Applicable lesson:** Secret exposure can occur outside `src/` or `config/`; repository scope should not be defined only by application source folders.

**Non-transferable detail:** Scan Pilot has already accepted monitored-branch limits for the MVP. GitHub's all-branch platform scope must not silently replace that decision.

### GitLab Pipeline Secret Detection

GitLab separates ordinary pipeline scanning from a one-time historic scan. Its documentation says pipeline detection balances coverage with runtime and automatically excludes many low-value or expensive categories, including media, documents, archives, executables, dependency directories, build outputs, compiled artifacts, caches, and selected generated assets.

**Applicable lesson:** Considering repository content does not require parsing every content type. Eligibility classification and explicit exclusions are normal production controls.

**Non-transferable detail:** GitLab's exact extension and directory list reflects its analyzer, scale, false-positive experience, and product contract. Scan Pilot should not copy the list without testing its own detector and demo repositories.

### GitLab Secret Push Protection

GitLab documents stricter limits in the latency-sensitive push path. Binary files and file or diff patches larger than 1 MiB are not checked, and very large pushes can bypass protection under documented thresholds.

**Applicable lesson:** A blocking push check requires tighter latency limits than an asynchronous baseline worker.

**Non-transferable detail:** The 1 MiB threshold is evidence about GitLab Push Protection, not a justified Scan Pilot baseline limit. Scan Pilot's asynchronous scan may choose a different measured limit.

### Gitleaks

Gitleaks supports Git, directory, and standard-input scan modes. It exposes `--max-target-megabytes` to skip oversized content and `--max-archive-depth` to enable bounded archive traversal. Archive traversal is disabled by default at depth zero.

**Applicable lesson:** File-size and archive-depth policy can be explicit detector-adapter inputs and must be recorded as coverage limits.

**Non-transferable detail:** Gitleaks capability does not prove that archive scanning is safe or affordable for the Scan Pilot MVP. Exact limits require benchmarking in the isolated worker design.

### GitHub Repository Limits

GitHub recommends keeping individual Git objects at or below `1 MB` and enforces a `100 MB` maximum for ordinary Git objects, directing larger content to Git LFS.

**Applicable lesson:** A scanner operating on GitHub repositories needs an explicit safety boundary below the host's maximum ordinary object size rather than assuming every accepted object is cheap to process repeatedly across history.

**Non-transferable detail:** GitHub's limits protect repository hosting and Git operations; they are not secret-scanner performance thresholds and do not prove that `50 MiB` is safe for the Scan Pilot worker.

### SonarQube Quality Gates

SonarQube distinguishes frequent analysis of new code from a quality gate that evaluates whether existing analysis results meet release conditions.

**Applicable lesson:** A release-oriented assessment can reuse compatible evidence and request missing verification instead of becoming a second source of security truth or blindly repeating every scan.

**Non-transferable detail:** SonarQube's quality metrics, editions, and gate conditions do not define Scan Pilot's secret coverage, file-size limits, or Release Assessment delivery scope.

### Git Attributes and Content Heuristics

Git documents that unspecified diff behavior may inspect file content and size, while `.gitattributes` can explicitly force text or binary diff treatment. A repository declaration can therefore override Git's normal content guess for Git operations.

**Applicable lesson:** Content evidence and repository hints are separate. Scan Pilot can learn from Git's content inspection but must not let an untrusted repository declaration alone define security-scan coverage.

**Non-transferable detail:** Git's goal is correct diff and line-ending behavior, not proof that a secret detector safely and completely processed a file. Scan Pilot requires its own versioned classifier and coverage contract.

### Sourcegraph Code Search

Sourcegraph documents several independent indexing boundaries: binary files, invalid UTF-8 content, oversized files, and excessive trigram counts can be skipped, and skipped files are visible through repository indexing status.

**Applicable lesson:** Mature repository tooling combines content and resource signals and exposes omissions instead of treating them as successful analysis.

**Non-transferable detail:** Sourcegraph's indexing thresholds optimize code search rather than secret scanning. Its 1 MB default and UTF-8 requirement are benchmark evidence, not accepted Scan Pilot limits.

## Comparative Conclusion

The sources support this research direction:

```text
All Git-tracked content in the selected Scan Pilot Git scope
→ content eligibility classification
→ supported content is scanned
→ unsupported or bounded content is skipped with a reason
→ coverage reports what was and was not processed
```

This is more precise than either extreme:

```text
Scan source folders only
```

or:

```text
Guarantee successful parsing of every repository byte
```

Default path exclusions such as `docs/`, `examples/`, `tests/`, `.github/`, or `dist/` are not yet justified for `SP-CONFIG-001`, because committed secrets can exist in those locations. The accepted classifier combines Git object kind, signatures, bounded content signals, and non-authoritative filename or Git-attribute hints. Exact implementation, other binary families, archives, symbolic links, submodules, dependency trees, generated output, lock files, and user exclusions remain unresolved.

`DEC-036` independently adapts these observations into a Scan Pilot policy: Continuous Monitoring scans otherwise eligible supported text through `10 MiB`, and release-oriented verification scans or reuses compatible full-file evidence through `50 MiB`. These numbers are an accepted initial operational choice, not values copied from GitLab, GitHub, Gitleaks, Sourcegraph, or SonarQube.

## Attribution and Independent Design

Scan Pilot is learning general product and engineering patterns from the cited official documentation. This note does not copy source code, proprietary implementation details, product wording, branding, UI assets, or GitLab's exclusion list into the Scan Pilot specification.

Any later implementation reuse from an open-source project requires a separate license review. Attribution here records research provenance; it does not grant permission to reuse code.

## Verification Limits

- Public documentation does not reveal every internal classification or optimization used by GitHub or GitLab.
- Git and Sourcegraph documentation describes their own diff or search goals, not a complete secret-scanning classifier suitable for direct copying.
- Product behavior and limits may change after the access date.
- GitLab pipeline detection and push protection serve different latency and workflow goals.
- Gitleaks flags describe available controls, not recommended Scan Pilot values.
- No Scan Pilot worker benchmark has yet validated the accepted initial file-size limits, archive-depth behavior, timeout, or memory envelope.

## Accepted Outcome and Next Decision

Scan Pilot accepts the eligibility principle: all Git-tracked content in the selected Git scope is considered, supported content is scanned, and every policy-based skip is persisted in structured coverage with a reason code. It also accepts layered content classification into `TEXT`, `BINARY`, or `UNDETERMINED`; extension and `.gitattributes` are supporting hints rather than sole authorities.

The two-tier oversized-file policy is accepted as `DEC-036`. The next verification task is to benchmark Gitleaks at `1`, `10`, `25`, `50`, and `100 MiB`, followed by exact handling for remaining binary families, archives, symbolic links, submodules, dependency and generated content, lock files, and user-configured exclusions.
