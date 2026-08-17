> **Document:** Production Same-Origin Session Spike
> **File:** `spikes/issue-007-production-session/README.md`
> **Version:** v1.0.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# Production Same-Origin Session Spike

Temporary, credential-free code for Scan Pilot Eligibility Spike Issue `#7`.

## Contract

- test page, `GET /spike/session/start`, and `GET /spike/session/check` share one Cloud Run origin;
- `start` issues a random, opaque `__Host-` cookie with `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/`, and ten-minute lifetime;
- `check` returns only whether the request carried that cookie; it never returns the value;
- no GitHub App, GitHub token, Gemini key, database, repository data, user record, or persistent session state exists;
- scale target: `min instances = 0`, `max instances = 1`.

The service is not a Scan Pilot production API and must not be promoted into the product backend.

## Local Verification

```powershell
$env:PORT = 8080
py main.py
```

Open `http://localhost:8080` and select **Run verification**. The deployment test must use HTTPS because the cookie is intentionally marked `Secure`.

## Expected Deployed Result

Open the deployed root URL and select **Run verification**. `PASS: server received the HttpOnly session cookie.` proves the bounded same-origin browser transport. It does not prove GitHub OAuth, token exchange, server-side session persistence, CSRF protection, authorization, or the final production topology.
