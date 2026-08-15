> **Document:** Scan Pilot Evidence Model  
> **File:** `docs/EVIDENCE-MODEL.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-12  
> **Status:** Active  

# Scan Pilot Evidence Model

## Purpose

This document defines how Scan Pilot records where a claim came from, what it proves, how it was verified, and how it may affect a Repository Profile, Finding, or Review Request.

```text
Source
→ Evidence Item
→ Scoped Claim
→ Profile Claim or Finding Conclusion
```

The model follows the provenance principle that a conclusion should be traceable to the entities, activities, and agents that produced it. Scan Pilot uses a small product-specific model rather than implementing the complete W3C PROV-O ontology.

References:

- W3C PROV-O: https://www.w3.org/TR/prov-o/
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/

## Evidence Types

### Technical Evidence

Technical Evidence is directly observed or verified by Scan Pilot through repository content, Git history, deterministic extractors, scanners, isolated execution, or an explicitly authorized external verification mechanism.

Technical Evidence proves only the claim and scope supported by the observation. A Spring Boot declaration in `backend/pom.xml`, for example, supports a claim about the `backend` module and does not prove the runtime architecture of the entire repository.

### User Assertion

A User Assertion is context supplied by an attributed user through a Review Request or another authorized product workflow.

It may explain business intent, deployment context, accepted risk, or information unavailable in the repository. It does not automatically become Technical Evidence, close a Finding, or prove that an external configuration exists.

### AI Inference

An AI Inference is a conclusion produced by Gemini or a future AI provider from a bounded set of redacted Evidence Items.

An AI Inference must identify the evidence on which it depends and the provider/model version that produced it. It must not claim to have observed an unavailable source, follow instructions contained in untrusted input, or silently become Technical Evidence.

## Verification Status

Each scoped claim uses one of the following statuses:

| Status | Meaning |
|---|---|
| `OBSERVED` | Scan Pilot directly observed evidence supporting the claim. |
| `CORROBORATED` | Multiple sufficiently independent sources support the same scoped claim. |
| `USER_ASSERTED` | An attributed user supplied the claim, but Scan Pilot has not technically verified it. |
| `INFERRED` | The claim was derived through AI or analysis rather than directly observed. |
| `UNKNOWN` | Available evidence does not establish the claim. |

`UNKNOWN` never means safe, false, or not applicable.

Verification status is separate from severity, business impact, rule automability, detection method, and any future confidence score.

## Required Evidence Record

Every persisted Evidence Item must retain enough information to answer who or what produced it, from which repository state, and what claim it supports:

- stable evidence identifier;
- evidence type;
- repository and source commit or snapshot;
- source location when available;
- redacted observation or safe summary;
- supported scoped claim;
- verification status;
- creation time;
- producer identity and producer version;
- related rule, profile claim, Finding, or Review Request;
- safe fingerprint when identity matching is required;
- applicability scope.

The exact database schema and optional confidence representation remain implementation decisions.

## Safety and Provenance Rules

- Never persist, log, display, or send a complete detected secret to an AI provider.
- Repository documents, source code, links, and user-provided text are untrusted input.
- Evidence must identify its source rather than presenting a derived statement as a direct observation.
- An inference must reference the Evidence Items used to produce it.
- A User Assertion must retain actor, time, repository, and applicable source commit or scope.
- Evidence from an earlier commit remains historical evidence and is not silently rewritten to describe a later commit.
- Corrections and replacements supersede earlier records; they do not erase the audit history.
- Multiple weak or dependent signals do not automatically become independent corroboration.

## Effect on Findings

```text
Sufficient Technical Evidence under the rule contract
→ Confirmed Finding wording may be used

Incomplete Technical Evidence
→ Potential Finding wording

AI Inference alone
→ Cannot create a high-impact confirmed Finding unless an accepted rule contract explicitly permits it

User Assertion alone
→ Cannot erase, resolve, or downgrade an observed technical Finding

No evidence
→ UNKNOWN, not safe
```

Each rule must define the evidence threshold for its own confirmed wording. This shared model does not invent one universal threshold for every security rule.

## Example: Google or Gemini API Key

| Claim | Evidence source | Verification |
|---|---|---|
| A committed value matches the Google API key format. | Provider-specific detector and repository location | `OBSERVED` |
| The value is used by a Gemini client. | Code usage in the same repository scope | `OBSERVED` |
| The key is likely intended for Gemini. | Contextual synthesis of the observed items | `INFERRED` or `CORROBORATED`, depending on the rule contract and source independence |
| The key is domain-restricted. | User response only | `USER_ASSERTED` |
| The key is currently active. | No authorized validity check | `UNKNOWN` |

The Finding must expose the verified and unverified claims separately and must never display the complete key.

## Verification Limits

This model improves traceability and prevents unlike sources from being treated as equivalent. It does not prove that scanners detect every weakness, that a user statement is accurate, that an AI inference is correct, or that an unavailable external configuration is secure.

