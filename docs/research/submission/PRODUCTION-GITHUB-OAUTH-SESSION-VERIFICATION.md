> **Document:** Production GitHub OAuth and Session Verification  
> **File:** `docs/research/submission/PRODUCTION-GITHUB-OAUTH-SESSION-VERIFICATION.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-16  
> **Last Updated:** 2026-08-16  
> **Status:** Active  

# Production GitHub OAuth and Session Verification

## Verification Result

**Issue:** [#7 — Verify GitHub authentication and production browser session](https://github.com/NgaiLong49423/scan-pilot/issues/7)

**Result:** `PASS` at the bounded Eligibility Spike level. A real browser completed the deployed same-origin GitHub authorization-code flow and received the success result without a GitHub access token being displayed or persisted by the temporary frontend.

## Verified Boundary

| Concern | Verified result |
|---|---|
| Browser origin | A temporary page and its OAuth callback run on the same public Cloud Run service origin. AI Studio is not the authentication origin. |
| Callback ownership | The Cloud Run service owns `/auth/github/callback`; the browser does not exchange the authorization code. |
| Browser session transport | A prior same-origin spike verified a short-lived `Secure`, `HttpOnly`, `SameSite=Lax`, host-only `__Host-` cookie round trip. |
| Authorization request | The deployed service generated `state` and PKCE S256 challenge data, then redirected the browser to GitHub. |
| Callback validation | The callback required the matching short-lived HttpOnly cookie and `state` before a code exchange. |
| Token boundary | The temporary service exchanged the code server-side and returned only a success/failure page. It displayed and persisted no access or refresh token. |
| GitHub App scope | The private `Scan Pilot MVP` GitHub App uses `Contents: Read-only`, required `Metadata: Read-only`, no webhook, personal-account installation only, and the Product Owner selected only `NgaiLong49423/scan-pilot`. |
| Secret boundary | The Client Secret is stored only as a Secret Manager version and injected into Cloud Run as `GITHUB_CLIENT_SECRET`. Its value was not supplied to source, chat, terminal command text, logs, or this document. |
| OAuth callback logging | A project-level Log Router exclusion covers automatic request logs only for `scan-pilot-auth-spike`. A post-test query found no `run.googleapis.com/requests` entries for that service. Application code also suppresses request-path logging. |

## Temporary Resource Contract

| Property | Value |
|---|---|
| Google Cloud project | `gen-lang-client-0098508328` (shared user-owned MVP environment) |
| Cloud Run service | `scan-pilot-auth-spike` |
| Region | `asia-southeast1` |
| Public origin | `https://scan-pilot-auth-spike-drbjfwrlxq-as.a.run.app` |
| Callback path | `/auth/github/callback` |
| Minimum instances | `0` |
| Maximum instances | `1` |
| Container concurrency | `1` |
| Request timeout | `10` seconds |
| Persisted user/repository/token data | None |

This resource is a temporary security spike, not the future Spring Boot API. It does not authorize reuse of IoT resources or credentials in the shared Google Cloud project.

## Defined Production Outcomes

The spike verified the successful path. The following are the required production outcomes, not claims of live end-to-end verification:

| Event | Required production outcome |
|---|---|
| User denies GitHub authorization | No session or token is created; show a safe retry outcome. |
| User logs out | Invalidate the server-side opaque session and expire the host-only session cookie; do not remove the GitHub App installation. |
| GitHub user token expires or refresh fails | Reject the affected operation, invalidate the session when identity can no longer be established, and require GitHub sign-in again. |
| Selected repository is removed from the App installation or installation access is lost | Mark the repository connection unavailable, stop repository work, preserve redacted historical evidence, and require the user to restore access through GitHub. Never broaden to another repository automatically. |
| User revokes GitHub authorization | Clear the local session at the next validated failure and require a new sign-in; do not retain or display a stale token. |

## Verification Limit

This does not implement or prove the production Spring Boot session store, logout endpoint, refresh-token lifecycle, installation-token lifecycle, multi-user isolation, repository transfer, full revocation propagation, background-worker authorization, or browser compatibility beyond the tested Chrome flow. The temporary Cloud Run service and its narrow log exclusion require explicit cleanup or conversion after the Eligibility Spike conclusion.
