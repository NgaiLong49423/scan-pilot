> **Document:** AI Studio to Cloud Run CORS Verification  
> **File:** `docs/research/submission/AI-STUDIO-CLOUD-RUN-CORS-VERIFICATION.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-16  
> **Last Updated:** 2026-08-16  
> **Status:** Active  

# AI Studio to Cloud Run CORS Verification

## Verification Result

**Issue:** [#6 — Prove AI Studio frontend can call a minimal Cloud Run API](https://github.com/NgaiLong49423/scan-pilot/issues/6)

**Result:** `PASS` — the actual Google AI Studio browser origin successfully called a minimal public Cloud Run endpoint, while a different browser origin was blocked by CORS.

## Temporary Service Contract

| Property | Verified value |
|---|---|
| Google Cloud project | `gen-lang-client-0098508328` |
| Project operating context | User-owned shared MVP environment whose existing display name is `IotS4` |
| Cloud Run service | `scan-pilot-cors-spike` |
| Region | `asia-southeast1` |
| Endpoint | `GET /spike/ping` |
| Public URL | `https://scan-pilot-cors-spike-drbjfwrlxq-as.a.run.app/spike/ping` |
| Allowed browser origin | `https://aistudio.google.com` |
| Credentials, repository data, Gemini calls, cookies | Not used |
| Minimum instances | `0` by Cloud Run default; no minimum-scale override is configured |
| Maximum instances | `1` |
| Container concurrency | `1` |
| Request timeout | `10` seconds |
| Public invocation | `allUsers` has `roles/run.invoker` |

The Product Owner explicitly accepted use of this existing shared Google Cloud project for the Scan Pilot MVP to avoid unnecessary project migration before the submission deadline. This does not authorize Scan Pilot to use, display, rotate, or otherwise access IoT credentials or other IoT resources in that project. New Scan Pilot resources must retain the `scan-pilot-` prefix and `project=scan-pilot` label.

## Browser Evidence

| Test | Result |
|---|---|
| Google AI Studio page → endpoint with `Content-Type: application/json` | `PASS` — browser returned `200` and the expected JSON payload. The non-simple request exercised the `OPTIONS` preflight path. |
| `https://example.com` → same endpoint with the same request | `PASS` — browser reported that the preflight was blocked because no `Access-Control-Allow-Origin` response header was available for that origin; the request failed with `TypeError: Failed to fetch`. |

The browser console could not read `Access-Control-Allow-Origin` through JavaScript. That is expected: the response header is not exposed to page JavaScript. Browser receipt of the JSON payload is the relevant success evidence. Direct service verification additionally confirmed `Access-Control-Allow-Origin: https://aistudio.google.com` for the allowed request and `403` without that header for a disallowed origin.

## Production Boundary

This endpoint is a temporary connectivity spike, not a Scan Pilot production API. It contains no user data, GitHub integration, Gemini integration, authentication, database access, scan logic, or credentials. It proves only that the AI Studio browser can reach an independently deployed Cloud Run service under an explicit origin policy.

The production authentication and browser-session contract remains Issue `#7`. A future credentialed endpoint must not use `Access-Control-Allow-Origin: *`; it requires an explicit origin and a separately verified authentication design.

## Acceptance-Criteria Evidence

| Acceptance criterion | Result |
|---|---|
| Minimal endpoint has no credentials or private data | `PASS` |
| Google Cloud resource creation separately authorized | `PASS` — Product Owner authorized the temporary public service. |
| AI Studio frontend calls endpoint from browser | `PASS` |
| Request origin and effective CORS response recorded | `PASS` |
| Credentialed behavior not combined with wildcard CORS | `PASS` — no credentialed behavior; exact AI Studio origin only. |
| Browser success and failure evidence recorded | `PASS` |
| Scale, instance, logging, and cleanup controls fit budget guardrails | `PASS` for scale/instance controls; default Cloud Run logging remains subject to the broader cost review. |
| PASS, FAIL, or blocker recorded | `PASS` |

## Verification Limit

This spike does not prove production authentication, GitHub OAuth, session handoff, API authorization, data isolation, capacity, long-term cost, or end-to-end scanning. It also does not make the shared cloud project a permanent production-environment decision. The temporary public service requires explicit cleanup or conversion after the Eligibility Spike conclusion.
