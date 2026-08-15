> **Document:** AI Riser Submission Contract Verification
> **File:** `docs/research/submission/AI-RISER-SUBMISSION-CONTRACT-VERIFICATION.md`
> **Version:** v1.1.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-16
> **Status:** Active

# AI Riser Submission Contract Verification

## Verification Result

**Issue:** [#3 — Confirm AI Riser submission requirements and deadline](https://github.com/NgaiLong49423/scan-pilot/issues/3)

**Result:** `PASS` with a documented completion-form access limitation.

The official live event page provides an authoritative deadline, timezone, core submission deliverables, scoring signals, and deployment condition. The exact Completion Form schema is not publicly linked from the landing page; the Vietnamese page says the form is sent in the instruction email. Exact field labels, validators, and any separate source-code field therefore remain unverified.

## Official Sources Observed

| Source owner | Source | Accessed | Observed role |
|---|---|---:|---|
| Google | [AI Riser Vietnam official event page](https://rsvp.withgoogle.com/events/airiservietnam) | 2026-08-16 | Authoritative live event dates, deliverables, tiers, scoring, and deployment requirement |
| Google | [Official Vietnamese event page](https://rsvp.withgoogle.com/events/airiservietnam/home_vietnam) | 2026-08-16 | Vietnamese confirmation of deadline and form-delivery wording |
| Google | [AI Riser Vietnam Participant Handbook](https://goo.gle/airiser-handbook) | 2026-08-16 | Public read-only build and publish guidance; not the final Completion Form |

## Confirmed Deadline

```text
Official submission deadline:
2026-08-30 23:59 GMT+7
```

The official event calendar shows the event ending at `August 30, 2026 at 11:59 PM GMT+7`. Both the English and Vietnamese event pages identify August 30 as the deadline. This supersedes the earlier unverified user-provided assumption of August 31. The Product Owner subsequently retained August 30 as the internal complete-and-stable gate without a separate contingency day.

## Confirmed Submission Contract

| Item | Live official evidence | Classification for Scan Pilot |
|---|---|---|
| Google AI Studio link | Bronze-tier checklist requires a Google AI Studio share link; Vietnamese wording says the product is built with Google AI Studio. | Required |
| Demo video | English page requires a public YouTube demo video with a maximum duration of two minutes. | Required |
| Social post | Bronze-tier checklist identifies a public LinkedIn post sharing the project. | Required |
| Live application | A public Cloud Run or Google Play link is marked optional for the Bronze submission checklist. | Optional for base submission |
| Deployment bonus | A public deployed link earns 10 bonus points; a web application must be hosted on Google Cloud Run, while a mobile application must be on Google Play. | Required only to claim deployment bonus |
| Google technology integration | Depth, effectiveness, and appropriate use of Google technologies may earn up to 10 bonus points. | Optional bonus evidence |
| Early submission | The first 200 completed submissions reportedly receive three bonus points. | Conditional bonus; current availability cannot be independently measured |
| Source repository or separate source upload | No separate source-repository or source-upload field is stated on the public event page. | Unverified; do not invent a requirement |

## Important Distinctions

- The AI Studio share link is a confirmed required deliverable. The exact source, prompt, and metadata surfaces visible through that link are a separate access question covered by Issue `#4`.
- A deployed Cloud Run link is optional for base Bronze eligibility but required to claim the web deployment bonus.
- The public Bronze checklist names LinkedIn. The general journey text mentions sharing on LinkedIn or Facebook, but the exact Completion Form acceptance of Facebook as a substitute is not verified.
- The landing page does not expose the final Completion Form URL. The visible public form endpoint is event registration, not project completion.
- The public page confirms the high-level fields but not their exact form labels, URL validation rules, character limits, evidence-upload fields, or later changes.

## Product Owner Deadline Resolution

`DEC-051` originally used an unverified external deadline of `2026-08-31 23:59` and set `2026-08-30` as the internal safety gate. The official deadline is now confirmed as `2026-08-30 23:59 GMT+7`.

On 2026-08-16, the Product Owner explicitly accepted retaining the same internal complete-and-stable date:

```text
Internal complete-and-stable gate: 2026-08-30
Official submission deadline: 2026-08-30 23:59 GMT+7
Separate contingency day: none
```

**Reason:** The Product Owner prefers to retain the full available development schedule rather than reserve a separate calendar day.

**Expected benefit:** One additional day remains available for product completion.

**Trade-off:** Deployment, link, video, or form failures discovered on August 30 have less recovery time before the official cutoff.

**Verification limit:** The final emailed Completion Form may impose an earlier operational cutoff, account-specific access, or changed fields; it must still be opened and checked before submission.

## Acceptance-Criteria Evidence

| Acceptance criterion | Result |
|---|---|
| Official live source located or limitation recorded | `PASS` — official live event pages located; final Completion Form access limitation recorded |
| Final deadline and timezone confirmed | `PASS` — 2026-08-30 23:59 GMT+7 |
| Required and optional fields listed | `PASS` at the public-contract level |
| AI Studio, source access, video, social, and Cloud Run distinguished | `PASS` — source field remains explicitly unverified |
| Unverified statements remain labeled | `PASS` |
| PASS, FAIL, or blocker recorded | `PASS` with documented limitation |

## Verification Limit

This record proves what the official public event pages displayed on 2026-08-16. It does not prove the exact private or emailed Completion Form schema, later page changes, current early-submission rank, signed-out AI Studio project visibility, or successful Cloud Run deployment.
