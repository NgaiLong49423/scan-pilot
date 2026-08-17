> **Document:** AI Riser Eligibility Spike Result  
> **File:** `docs/research/submission/ELIGIBILITY-SPIKE-RESULT.md`  
> **Version:** v1.1.0  
> **Created:** 2026-08-16  
> **Last Updated:** 2026-08-16  
> **Status:** Active  

# AI Riser Eligibility Spike Result

## Purpose

This document consolidates the completed AI Riser submission Eligibility Spike and records the Product Owner's accepted implementation-start decision.

**Parent:** [Issue #2 — Validate AI Riser submission architecture](https://github.com/NgaiLong49423/scan-pilot/issues/2)  
**Decision issue:** [Issue #8 — Record Eligibility Spike result and production go/no-go](https://github.com/NgaiLong49423/scan-pilot/issues/8)

## Accepted Decision

```text
CONDITIONAL GO — ACCEPTED
→ implementation is authorized under the listed conditions.
```

On 2026-08-16, the Product Owner accepted this result and explicitly authorized the transition from research/specification into implementation. This is not a claim that Scan Pilot is feature-complete, benchmarked, or production-ready. It means the core toolchain and browser boundaries no longer show an evidence-backed blocker that would make the intended submission architecture infeasible.

## Evidence Summary

| Issue | Result | What it establishes | Important limit |
|---|---|---|---|
| [#3](https://github.com/NgaiLong49423/scan-pilot/issues/3) | `PASS` | Official public deadline, required AI Studio link/video/social evidence, optional Cloud Run base link, and deployment-bonus condition. | The exact emailed Completion Form fields and validators remain unavailable. |
| [#4](https://github.com/NgaiLong49423/scan-pilot/issues/4) | `PASS` | A separate signed-in Google account could view AI Studio Preview and Code; the creation conversation was not visible. | The link is not anonymous; judge access can still depend on Google sign-in/sharing behavior. |
| [#5](https://github.com/NgaiLong49423/scan-pilot/issues/5) | `PASS` | ZIP is a secret-safe frozen AI Studio evidence snapshot; GitHub is the post-handoff production source of truth. | Future import/export behavior and production build compatibility are not proved. |
| [#6](https://github.com/NgaiLong49423/scan-pilot/issues/6) | `PASS` | AI Studio browser reached a minimal Cloud Run endpoint under exact CORS; a different origin was blocked. | This is connectivity evidence only, not the production authentication design. |
| [#7](https://github.com/NgaiLong49423/scan-pilot/issues/7) | `PASS` | A same-origin Cloud Run browser flow completed GitHub OAuth callback and server-side code exchange without frontend token persistence. The App is read-only and selected-repository scoped. | The temporary Python spike is not Spring Boot production authentication; lifecycle behavior beyond the happy path is specified, not fully tested. |

## Why This Is a Conditional Rather Than Unqualified Go

| Condition | Owner | Required before | Reason |
|---|---|---|---|
| Open the actual emailed Completion Form and record its current fields, validation rules, and allowed social-link options. | Product Owner | Final submission | The public event page cannot prove the private form schema or an early operational cutoff. |
| Configure and verify the accepted Cloud Billing alert thresholds, then monitor actual spike and implementation costs. | Product Owner with agent assistance | Ongoing public implementation/deployment | Promotional credit is not guaranteed cash; alerts are visibility controls, not automatic stop controls. |
| Implement and test the production Spring Boot authentication/session store, selected-repository installation-token path, logout, expiry, revocation, and temporary private-source cleanup. | Implementation workstream | Before a real private-repository scan | The successful spike only proves the selected browser/OAuth boundary. |
| Explicitly accept this recommendation and explicitly authorize the change from research/specification to implementation. | Product Owner | Before product code starts | Phase change is a Product Owner decision, not an inference from a successful spike. |

## Proposed Canonical Direction After Acceptance

If the Product Owner accepts `CONDITIONAL GO`, the implementation contract should preserve these boundaries:

```text
AI Studio link
→ frozen submission evidence

GitHub production repository
→ source of truth

Cloud Run production origin
→ same-origin React frontend + Spring Boot API
→ server-side GitHub OAuth/session and token boundary
→ selected-repository GitHub App installation
```

The MVP remains constrained to the accepted single-rule vertical slice: a real `SP-CONFIG-001` scan, redacted evidence, bounded Gemini explanation, re-scan lifecycle, controlled security-lab demonstration, and public Cloud Run deployment. No additional rule or broad Product V1 scope is unlocked by this recommendation.

## Risks Carried Forward

- The official deadline is `2026-08-30 23:59 GMT+7`, and the Product Owner retains no separate contingency day.
- AI Studio evidence may require judges to sign in with Google and can expose Preview/Code to an eligible shared account.
- The shared Google Cloud project also contains unrelated IoT work; Scan Pilot resources must remain prefixed and labeled, with credentials strictly separated.
- Public Cloud Run spikes are temporary resources and need an explicit cleanup or conversion decision after the Eligibility Spike.
- Independent detector quality evidence, the Gitleaks adapter benchmark, the controlled security-lab repository, and the actual scan pipeline are not yet implemented or measured.

## Verification Limit

This result proves only submission-workflow eligibility at the tested boundaries. It does not prove secure multi-user operation, full GitHub authorization lifecycle, database isolation, worker sandboxing, scanner accuracy, Gemini safety, capacity, credit duration, production accessibility, or successful event submission.
