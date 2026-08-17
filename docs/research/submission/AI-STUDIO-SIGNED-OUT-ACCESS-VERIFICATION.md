> **Document:** AI Studio Signed-Out Access Verification
> **File:** `docs/research/submission/AI-STUDIO-SIGNED-OUT-ACCESS-VERIFICATION.md`
> **Version:** v1.0.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# AI Studio Signed-Out Access Verification

## Verification Result

**Issue:** [#4 — Verify signed-out access to the Google AI Studio project](https://github.com/NgaiLong49423/scan-pilot/issues/4)

**Result:** `PASS` — the required access behavior was observed and documented.

## Test Method

The Product Owner opened the shared Scan Pilot AI Studio project link in an Incognito browser window, initially without a Google session, then signed in using a different Google account. No project settings, sharing settings, prompts, source, or deployment configuration were changed during this test.

The captured screenshot remains conversation evidence and is not copied into the repository.

## Observed Behavior

| Surface | Observation |
|---|---|
| Initial signed-out access | The direct AI Studio project link redirected to Google Sign In. |
| Authentication requirement | A Google account and AI Studio sign-in were required before opening the project. |
| Non-owner account access | The separate Google account could open the shared project after sign-in. |
| Preview | Visible. |
| Code | Visible. |
| Prompt or project information | Not observed. |
| Original creation conversation | Not visible to the non-owner account. |
| Sensitive information | No sensitive data was observed in the tested view. |
| Remix control | The viewer saw `Remix`, indicating an available copy/remix path. This does not prove permission to edit the original project. |

## Interpretation

The AI Studio submission link is not anonymous-public: a judge must authenticate with Google before viewing it. For a signed-in Google account that can open the shared project, Preview and Code are visible. This is compatible with the observed event requirement for an AI Studio share link, but it introduces evaluator friction and does not prove that every judge account will have the same access.

The test does not prove original-project edit permission, the exact source/prompt surfaces beyond the observed Code tab, external API behavior, or future Google AI Studio sharing behavior.

## Acceptance-Criteria Evidence

| Acceptance criterion | Result |
|---|---|
| Link opened in a signed-out or clean browser session | `PASS` |
| Sign-in requirement and access behavior recorded | `PASS` |
| Preview, source/code, prompt, and project surfaces recorded separately | `PASS` — Preview and Code visible; prompt/project info not observed |
| No sensitive data exposed | `PASS` in the tested view |
| Reproducible evidence captured with sensitive information redacted | `PASS` — Product Owner screenshot and structured observation |
| PASS, FAIL, or blocker recorded | `PASS` |

## Verification Limit

This result reflects one Incognito session and one separate Google account on 2026-08-16. It does not prove access for all judge accounts, anonymous access, original-project editing permission, exact source/prompt metadata visibility, or later sharing-policy behavior.
