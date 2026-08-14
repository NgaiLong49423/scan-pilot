> **Document:** AI Riser Vietnam 2026 Submission Context  
> **File:** `docs/research/submission/AI-RISER-VIETNAM-2026.md`  
> **Version:** v0.1.1  
> **Created:** 2026-08-13  
> **Last Updated:** 2026-08-13  
> **Status:** Under Review  

# AI Riser Vietnam 2026 Submission Context

## Purpose and Status

This document records external event information that can materially affect Scan Pilot product direction, MVP scope, Google technology integration, deployment, demo, and submission material.

It is a required review source before proposing a relevant direction-setting change. It is not an accepted Scan Pilot product requirement, does not override the source-of-truth order in `AGENTS.md`, and does not replace an explicit user decision in `docs/DECISIONS.md`.

## Source Documents

| Source | Purpose | Retrieved |
|---|---|---:|
| Completion-form text provided by the user (no direct form URL supplied) | Submission deliverables, scoring, and bonus conditions | 2026-08-13 |
| [AI Riser Vietnam - Suggested Partner Challenges](https://docs.google.com/presentation/d/1n0tQyXDO3BVnisXfhO_RRJvAsu95pTm2-clKgpxPn2Y/edit) | Optional partner-provided problem references | 2026-08-13 |
| [AI Riser Vietnam 2026 Participant Handbook](https://docs.google.com/document/d/1_zaaLs-FW3-9epNl_nER5VawSp4EOEmy6tpDrbn3p6c/edit) | Google AI Studio build and publishing guidance | 2026-08-13 |
| [Google AI & Vibe Coding Handbook](https://docs.google.com/presentation/d/e/2PACX-1vT5FmgwnjE8Q2FhcWx7Cg89PrW6CujORX4bzUacuABBg1oeFrn6kXkPKhFXGxcVcbfkfUrF5tOxgrDx/pub?start=false&loop=false&delayms=60000&slide=id.g3c46f2f9e31_0_54) | Intermediate public deck (`goo.gle/itsvibecoding`) for AI Studio and vibe-coding guidance; detailed review pending | 2026-08-13 |

The completion-form URL is not available in the supplied material; the summary below preserves the text provided by the user and must be checked against the live form before final submission.

The public Google AI & Vibe Coding Handbook deck was identified as a 47-slide intermediate resource. Its detailed content was not fully extracted during the initial review, so it must be read before relying on it for a new technical, product, or submission claim.

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

The reported maximum is 123 points. The official live form remains the final source for evaluation details.

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

## Current Implications for Scan Pilot

- The intended MVP should demonstrate a real repository scan and evidence-backed finding lifecycle; mock-only findings cannot be the main proof of value.
- Gemini should have a visible, bounded role such as explaining normalized findings or remediation guidance; it must not be presented as the sole security authority.
- A public Cloud Run deployment is the current preferred web deployment target for the submission bonus.
- The eventual demo should show the distinction between deterministic scan evidence and Gemini-assisted explanation, including the redaction of detected secret values.

These are planning implications, not new accepted decisions. Any change to the accepted product architecture, rule contract, or implementation phase requires the normal user-acceptance workflow.

## Open Verification Items

- Confirm the exact final submission deadline from the official live source.
- Confirm the official ten program topics and whether Scan Pilot is eligible as an independent problem framing.
- Reconcile the handbook's AI Studio publishing guidance with the completion-form requirement that a web-app deployment bonus uses Google Cloud Run.
- Read and assess the detailed Google AI & Vibe Coding Handbook guidance before treating it as a source for an implementation or deployment decision.
- Confirm current public-access and link-format requirements before submitting.
