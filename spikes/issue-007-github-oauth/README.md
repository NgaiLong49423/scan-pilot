# Issue #7 GitHub OAuth Spike

Temporary Cloud Run service for the AI Riser Eligibility Spike. It verifies a
production same-origin GitHub OAuth authorization-code callback without
persisting or displaying access tokens.

## Safety boundary

- `GITHUB_CLIENT_ID` is a non-secret Cloud Run environment variable.
- `GITHUB_CLIENT_SECRET` must be injected from Secret Manager; it is never
  committed, printed, or provided to this source.
- OAuth `state` and PKCE verifier live only in a short-lived HttpOnly,
  `Secure`, `SameSite=Lax`, host-only cookie and are cleared at callback.
- Cloud Logging excludes automatic request logs for only this temporary service
  because callback query strings can contain the authorization code.
- The service has no database and intentionally stores no user, repository,
  access token, or refresh token.

## Verification limit

This is not the production Spring Boot authentication implementation. Passing
it verifies only the deployed browser redirect, callback validation, and
one-time code exchange path for Issue #7.
