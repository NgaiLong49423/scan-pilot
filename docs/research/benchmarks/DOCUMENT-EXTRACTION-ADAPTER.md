> **Document:** Deferred Document Extraction Adapter Benchmark Plan  
> **File:** `docs/research/benchmarks/DOCUMENT-EXTRACTION-ADAPTER.md`  
> **Version:** v0.3.0  
> **Created:** 2026-08-13  
> **Last Updated:** 2026-08-13  
> **Status:** Under Review  

# Deferred Document Extraction Adapter Benchmark Plan

## Current Disposition

**Deferred to optional Phase 2 by `DEC-033`. Do not execute this benchmark for the MVP.**

Scan Pilot MVP inventories PDF and common Office binary documents without extracting or semantically analyzing their internal content. Apache Tika is not an MVP dependency. This research is retained so the project preserves why the adapter was considered and what would need verification if the user later accepts an opt-in Project Understanding capability.

Before reactivating this plan, define and accept user consent, content exclusion, privacy, AI-provider processing, retention, parser isolation, cost, and the relationship between optional document analysis and security-baseline coverage.

### Evidence supporting MVP deferral

Access date: 2026-08-13.

| Source | Verified behavior | Applicable lesson | Verification limit |
|---|---|---|---|
| [GitLab Pipeline Secret Detection](https://docs.gitlab.com/user/application_security/secret_detection/pipeline/) | GitLab automatically excludes PDF and common Word, Excel, and PowerPoint formats to improve performance for file types with low expected secret likelihood. | A professional repository secret scanner can preserve a focused, explicit binary-document exclusion policy without treating it as a failure to scan supported code/config content. | This describes GitLab secret detection, not every possible Project Understanding product. |
| [Snyk Java and Kotlin support](https://docs.snyk.io/supported-languages/supported-languages-list/java-and-kotlin) and [technical guidance](https://docs.snyk.io/supported-languages/technical-specifications-and-guidance) | Snyk Code declares supported source extensions and constructs an event graph for code analysis; Snyk Open Source uses manifests such as `pom.xml` and Gradle files. | AppSec depth comes from supported source and machine-readable evidence, not from claiming semantic support for every repository artifact. | Snyk's implementation and product scope are not copied as Scan Pilot requirements. |
| [Gitleaks archive scanning](https://github.com/gitleaks/gitleaks#archive-scanning) | Archive traversal is optional and defaults to depth zero. | Deep archive/container processing adds separate cost and safety policy and should not be implied by ordinary text secret scanning. | DOCX packaging and parser behavior still require their own evaluation if Phase 2 is reactivated. |

These sources support prioritization and honest scope, not a claim that PDF or Office documents can never contain secrets or useful project context.

## Research Question

How should Project Discovery safely extract text and metadata from repository documents without coupling Scan Pilot to one parser or promising format support that has not been verified?

## Historical Boundary — Superseded for the MVP

`DEC-030` previously accepted the following direction, which `DEC-033` superseded for the MVP and retained only as Phase 2 research:

- Project Discovery accesses documents through a Scan Pilot-owned `Document Extraction Adapter`;
- parsing occurs in an isolated scan worker, not the Spring Boot API process;
- extracted content is treated as untrusted and potentially sensitive;
- content is bounded and secret-redacted before Gemini or persistent derived context receives it;
- Apache Tika is benchmarked first;
- no production parser or operational support claim is selected until benchmark evidence is accepted.

`DEC-031` previously accepted the following benchmark target, which `DEC-033` superseded:

- inventory every content item within the captured Project Discovery scope;
- process supported text, configuration, and manifests deterministically;
- benchmark DOCX and text-native PDF semantic extraction through the adapter;
- identify scanned or image-only PDF as `NEEDS_OCR` without performing OCR;
- keep PPTX, XLSX, and other unsupported binary document formats inventory-only;
- preserve an explicit outcome for every item not semantically understood.

The former scope is preserved as decision history, not as a current MVP test target or implementation requirement.

## Source Review

Access date for all sources: 2026-08-13.

| Source | Observed behavior | Applicable lesson | Non-transferable detail or verification limit |
|---|---|---|---|
| [Apache Tika supported formats](https://tika.apache.org/3.2.2/formats.html) | Tika documents detection and text or metadata extraction across many document families, including Microsoft Office and PDF. | Tika is a credible first candidate for broad local extraction in the accepted Java direction. | A documented parser list does not prove acceptable fidelity, speed, memory use, or safety for Scan Pilot's corpus. The production version and module set remain open. |
| [The Robustness of Apache Tika](https://cwiki.apache.org/confluence/display/TIKA/The%2BRobustness%2Bof%2BApache%2BTika) | The Tika project warns that untrusted parsing may hang or exhaust memory and recommends forked-process defenses rather than running beside critical code. | Isolation, timeout, memory limits, and crash recovery are part of the adapter contract, not optional implementation polish. | Forking reduces impact but does not prove a parser safe or prevent every malformed-file failure. |
| [Docling architecture](https://docling-project.github.io/docling/concepts/architecture/) and [document converter](https://docling-project.github.io/docling/reference/document_converter/) | Docling exposes format-specific backends and pipelines and produces a structured document representation for formats such as PDF, DOCX, PPTX, images, HTML, and Markdown. | Docling is a useful comparison when layout, tables, hierarchy, OCR, or structured output matters more than plain text extraction. | Its Python and model/runtime footprint must be measured against the solo-MVP deployment; listed capabilities do not establish Scan Pilot integration cost. |
| [Google Cloud Document AI supported files](https://docs.cloud.google.com/document-ai/docs/file-types) and [layout parser](https://docs.cloud.google.com/document-ai/docs/layout-parse-chunk) | Document AI supports several document and image formats, with format-specific limits, layout features, and billing conditions. | It is a credible managed fallback candidate when OCR or complex layout quality justifies a cloud service. | Cloud upload, cost, quotas, regional/privacy policy, and user authorization must be evaluated before repository content is sent to it. |

These are product and tool benchmarks, not security standards. Scan Pilot adapts the general patterns independently and does not copy proprietary code, UI, wording, or undocumented implementation details.

## Adapter Contract to Benchmark

The benchmark should test whether a candidate can produce at least:

```text
source identity and immutable source SHA
detected media type
parser name and version
normalized text and selected metadata
truncation and warning indicators
terminal outcome and reason code
processing time and resource telemetry
```

Raw extracted content must remain temporary. The benchmark must verify that size bounding and secret redaction occur before content can enter Gemini prompts, persistent Repository Profile claims, logs, queues, or user-visible diagnostics.

## Representative Corpus

The initial corpus should include repository-safe synthetic files for:

- Markdown and plain text as control cases;
- DOCX with headings, lists, tables, headers, and footers;
- text-native PDF;
- scanned-image PDF requiring OCR;
- layout-heavy PDF with columns, tables, and images;
- encrypted or password-protected document;
- malformed or truncated document;
- oversized document and oversized extracted output;
- PPTX and XLSX controls that must remain inventory-only for the accepted MVP;
- a document containing a synthetic secret-like value;
- a document containing prompt-injection text that attempts to override Scan Pilot instructions.

No private user document or real credential should be added to the benchmark corpus.

## Measurements and Failure Tests

| Area | Evidence required |
|---|---|
| Detection | Actual type versus declared extension; MIME result and parser selected |
| Fidelity | Text completeness, ordering, headings, lists, tables, and metadata retained |
| Resource cost | Cold and warm time, peak memory, dependency or image footprint, and output size |
| Isolation | Timeout, out-of-memory, crash, and worker-restart behavior without API-process failure |
| Bounds | Input, page, nested-content, and extracted-output limits with explicit truncation or skip outcome |
| Security | No raw secret in logs, persisted state, async messages, prompts, or diagnostics |
| Prompt safety | Extracted instructions remain untrusted document data and cannot alter system policy |
| Operability | Configuration, version reporting, upgrade path, error mapping, and cleanup behavior |

## Candidates

### Apache Tika — Former First Candidate

Expected advantage: Java compatibility and broad local format detection/extraction.

Primary risk: parser and dependency behavior on malformed or hostile inputs, plus unknown fidelity for layout-heavy or scanned documents.

### Docling — Future Comparison Candidate

Expected advantage: richer structure, layout, and document representation.

Primary risk: Python/model deployment weight and operational complexity for the MVP.

### Google Cloud Document AI — Future Managed Comparison Candidate

Expected advantage: managed OCR and layout parsing that aligns with the Google Cloud direction.

Primary risk: content leaves the local worker boundary and introduces billing, quota, privacy, regional, and authorization decisions.

## Decision Gate

This gate is inactive for the MVP. It applies only if a later accepted Phase 2 decision reactivates document semantic extraction.

A production parser can be proposed only after the benchmark reports:

1. tested formats and corpus limitations;
2. extraction fidelity and resource measurements;
3. isolation and failure behavior;
4. redaction, prompt-safety, and cleanup verification;
5. deployment and licensing implications;
6. whether DOCX and text-native PDF meet the future scope accepted when this plan is reactivated;
7. whether scanned PDF, PPTX, XLSX, and other unsupported controls receive honest non-success outcomes.

Apache Tika, Docling, and Google Cloud Document AI remain deferred research candidates rather than production dependencies. `DEC-033` governs the current MVP and this plan cannot select an implementation unless a later user decision reactivates it.

## Open Questions

- Which Tika version and module set should be tested?
- Should Tika run through a forked library process, server process, or another isolated invocation?
- What measurable threshold distinguishes a text-native PDF from an image-only PDF that should produce `NEEDS_OCR`?
- What are the input-size, page-count, extracted-output, timeout, and memory limits?
- Can any extracted text be retained, or only hashes and derived attributed claims?
- When, if ever, may repository content be sent to a managed cloud parser?
- What user controls or exclusions are required for AI processing of repository documents?
