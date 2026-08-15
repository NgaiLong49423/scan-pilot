> **Document:** AI Riser Vietnam 2026 Submission Context
> **File:** `docs/research/submission/AI-RISER-VIETNAM-2026.md`
> **Version:** v0.6.0
> **Created:** 2026-08-13
> **Last Updated:** 2026-08-16
> **Status:** Under Review

# AI Riser Vietnam 2026 Submission Context

## Purpose and Status

This document records external event information that can materially affect Scan Pilot product direction, MVP scope, Google technology integration, deployment, demo, and submission material.

It is a required review source before proposing a relevant direction-setting change. It is not an accepted Scan Pilot product requirement, does not override the source-of-truth order in `AGENTS.md`, and does not replace an explicit user decision in `docs/DECISIONS.md`.

## Source Documents

| Source | Purpose | Retrieved |
|---|---|---:|
| [Official AI Riser Vietnam event page](https://rsvp.withgoogle.com/events/airiservietnam) | Live event deadline, timezone, core deliverables, tiers, scoring, and deployment condition | 2026-08-16 |
| Completion-form text provided by the user (no direct form URL supplied) | Submission deliverables, scoring, and bonus conditions | 2026-08-13 |
| [AI Riser Vietnam - Suggested Partner Challenges](https://docs.google.com/presentation/d/1n0tQyXDO3BVnisXfhO_RRJvAsu95pTm2-clKgpxPn2Y/edit) | Optional partner-provided problem references | 2026-08-13 |
| [AI Riser Vietnam 2026 Participant Handbook](https://docs.google.com/document/d/1_zaaLs-FW3-9epNl_nER5VawSp4EOEmy6tpDrbn3p6c/edit) | Google AI Studio build and publishing guidance | 2026-08-13 |
| [Google AI & Vibe Coding Handbook](https://docs.google.com/presentation/d/e/2PACX-1vT5FmgwnjE8Q2FhcWx7Cg89PrW6CujORX4bzUacuABBg1oeFrn6kXkPKhFXGxcVcbfkfUrF5tOxgrDx/pub?start=false&loop=false&delayms=60000&slide=id.g3c46f2f9e31_0_54) | Public 47-slide deck (`goo.gle/itsvibecoding`) describing Think, Build, and Publish workflows across AI Studio, Antigravity, and Cloud Run | 2026-08-15 |

The official public event page was verified on 2026-08-16. It states that the Completion Form is sent in the instruction email rather than exposing the final form URL publicly. The public page confirms the core submission contract, while the exact emailed form schema remains unverified. See [AI Riser Submission Contract Verification](AI-RISER-SUBMISSION-CONTRACT-VERIFICATION.md).

The AI Studio share link was tested in an Incognito browser with a separate Google account on 2026-08-16. Signed-out access redirects to Google Sign In; after sign-in, that account could view Preview and Code but did not see prompt/project information or the original creation conversation. The tested view exposed no sensitive data. See [AI Studio Signed-Out Access Verification](AI-STUDIO-SIGNED-OUT-ACCESS-VERIFICATION.md).

The AI Studio project export was also verified on 2026-08-16. The standard ZIP export is the frozen, secret-safe evidence artifact; Antigravity export transfers local workspace state and one configured secret, so it remains internal-only. See [AI Studio Export and Frozen Evidence Verification](AI-STUDIO-EXPORT-VERIFICATION.md).

The public Google AI & Vibe Coding Handbook deck was reviewed as a 47-slide intermediate resource. It presents a workflow of thinking through the product, building with either Google AI Studio or Antigravity, and publishing to Cloud Run. It positions AI Studio for rapid app prototyping and Antigravity or its CLI for broader agentic engineering work. This supports a hybrid toolchain; it does not prove that a mock-only AI Studio link is sufficient, that local tools can directly edit the AI Studio workspace, or that a separate source-code submission is required.

## Reported Submission Deliverables

The completion-form text provided by the user states that a submission needs:

- a public Google AI Studio project link;
- a public YouTube demo-video link;
- a public LinkedIn or Facebook social post sharing the video and project journey; and
- optionally, a public deployed application link.

The handbook distinguishes the public AI Studio project link, which gives judges access to the project code and prompt information, from a deployed link, which gives users access to the application.

## Reported Evaluation Signals

| Area | Reported value | Implication for Scan Pilot |
|---|---:|---|
| Project evaluation | Up to 100 points | Demonstrate a concrete user problem, a credible solution, and observable impact. |
| Google technology integration | Up to 10 bonus points | Gemini and Google AI Studio must be used meaningfully and visibly in the demo. Other Google services may strengthen the evidence when justified. |
| Public deployment | 10 bonus points | For a web application, the completion-form text requires a public Google Cloud Run deployment. |
| Early form submission | 3 points | The first 200 submitted projects reportedly receive the bonus. |

The reported maximum is 123 points. The official public event page confirms the `100 + 10 + 10 + 3` structure, while the final emailed Completion Form remains authoritative for its exact fields and validators.

The official live event page confirms the external deadline as `2026-08-30 23:59 GMT+7`, replacing the earlier unverified August 31 assumption. On 2026-08-16, the Product Owner explicitly retained `2026-08-30` as the internal complete-and-stable gate and accepted that no separate contingency day exists. Whether the early-submission bonus remains available cannot be independently measured.

## Partner Challenge Interpretation

The partner-challenge deck says participants may choose any suitable problem from the ten topics on the program website. It presents its partner challenges as references for participants who need an idea.

Therefore, the partner challenges are not treated as mandatory Scan Pilot scope. Scan Pilot should not be relabelled or materially redirected toward a partner challenge merely for apparent category alignment without explicit user acceptance.

## Direction-Setting Review Gate

Before recommending or accepting a change that affects any item below, review this document alongside the applicable canonical specification:

- product problem framing, target user, or MVP scope;
- Google AI Studio or Gemini use;
- Google service integrations;
- web deployment target;
- demo flow, video, social post, or completion-form evidence.

The review must state the expected benefit, trade-off, and verification limit. If current event information is material to the decision, re-check the live official source rather than relying only on this recorded snapshot.

## Accepted Scan Pilot Implications

- The submission MVP is narrower than Product V1 and demonstrates one real repository-security vertical slice.
- Google AI Studio provides the submission-facing frontend and frozen evidence snapshot; after a one-way handoff, GitHub production source becomes the engineering source of truth.
- The AI Studio frontend calls the real production backend on Cloud Run. There is no judge-only mock mode or alternate onboarding bypass.
- The submission flow uses GitHub sign-in, GitHub App installation or linking, and explicit selection of one public or private personal-account repository. Organization accounts are deferred.
- Gemini has a visible but bounded role: it explains redacted findings, remediation, and lifecycle transitions. Deterministic evidence and application logic retain security authority.
- Independent benchmark evidence is required and remains distinct from Gitleaks' own regression fixtures and from the deployed end-to-end demonstration.
- A separate user-owned security-lab repository uses nonfunctional synthetic candidates and known ground truth to demonstrate detection, remediation, re-scan, and history verification through the real pipeline.
- The user remains final authority for UI and UX decisions. Technical implementation choices may be made within accepted architecture unless they materially affect product outcome, scope, cost, privacy, permissions, or UI/UX.

These implications are recorded as accepted decisions in `docs/DECISIONS.md`. They do not change the current research-and-specification phase or authorize implementation.

## Open Verification Items

- Confirm the official ten program topics and whether Scan Pilot is eligible as an independent problem framing.
- Open the emailed Completion Form and confirm its exact fields, validators, Facebook-versus-LinkedIn behavior, and any separate source-code field rather than inferring one.
- Verify that the AI Studio frontend can call the external Cloud Run API with the required CORS and authentication behavior.
