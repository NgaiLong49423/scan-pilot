> **Document:** Scan Pilot Cloud Budget and Cost Guardrails  
> **File:** `docs/CLOUD-BUDGET.md`  
> **Version:** v1.1.0  
> **Created:** 2026-08-13  
> **Last Updated:** 2026-08-13  
> **Status:** Active  

# Scan Pilot Cloud Budget and Cost Guardrails

## Purpose

This document is the canonical cost constraint for architecture, deployment, benchmarking, and external-service proposals. Agents must review it before proposing a choice that can create Google Cloud or Gemini charges.

The values below are a design envelope, not permission to provision resources, enable billing, purchase services, or spend money. External actions still require the authorization applicable to the task.

## Accepted Two-Month Envelope

| Budget component | Accepted amount | Meaning |
|---|---:|---|
| Planning envelope | USD 250 | Architecture and deployment should fit within this total for approximately two months. |
| Operating target | At most USD 180 | Expected usage should remain below this amount. |
| Protected reserve | USD 70 | Held for benchmark variance, configuration mistakes, final demo load, and recovery. |

Do not intentionally design a two-month plan above USD 250 without a new explicit user decision. Reaching the USD 180 target is a review signal, not permission to consume the reserve automatically.

## Current Funding Source

The only currently recorded funding source is Google Cloud promotional credit. The user has not allocated cash or another free-credit source to Scan Pilot. If another source becomes available, the user will report it and this document must be updated before agents rely on it.

A user-supplied Google Cloud Billing screenshot dated 2026-08-13 showed:

- one available Free Trial credit with `₫7,897,351` remaining, corresponding nominally to approximately USD 300 in the displayed billing context;
- four available Google Developer Program premium monthly-credit entries, each with `₫263,246` remaining, corresponding nominally to approximately USD 10 each.

These observations do not establish that approximately USD 340 can be pooled or used for every service. Credit expiry, eligible services, application order, and monthly-credit conditions were not visible in the supplied evidence. Planning therefore uses the accepted USD 250 envelope rather than the full nominal display.

The Google Cloud Free Trial documentation states that its USD 300 welcome credit is valid for 90 days. Before deployment, verify the actual account start and expiry dates in Cloud Billing. Two months of operation is feasible only when the remaining credit validity covers the required window or a separately authorized continuity plan exists.

## Initial Cost Allocation

These are planning ranges, not service quotations or guaranteed bills:

| Cost area | Two-month planning range |
|---|---:|
| Cloud SQL PostgreSQL | USD 50–90 |
| Cloud Run API and web delivery | USD 5–20 |
| Isolated Cloud Run scan workers | USD 10–40 |
| Gemini usage | USD 5–25 |
| Storage, build artifacts, logging, and network | USD 5–20 |
| Contingency | USD 25–50 |

Actual prices depend on region, service configuration, traffic, model, and billing terms. Recalculate with current official prices before provisioning and again before the public demo period.

## Accepted Cost Guardrails

- Prefer scale-to-zero for request-driven Cloud Run services and workers where the product contract permits it.
- Start the scan worker at maximum one instance; increase only after measured need and cost review.
- Start Cloud SQL as a small, single-zone, non-HA instance without read replicas for the MVP unless reliability evidence justifies a separately accepted change.
- Put explicit limits on repository size, file size, scan frequency, job concurrency, parser time and memory, Gemini input/output, retained artifacts, and log volume before public access.
- Keep API, worker, database, storage, Artifact Registry, logging, network, and Gemini costs visible as separate categories.
- Do not add a paid managed parser, always-on worker, HA database, replica, GPU, or additional external service merely because promotional credit exists. Binary document parsing is deferred beyond the MVP by `DEC-033` and has no current cost allocation.
- Treat promotional-credit expiry and eligibility as operational risks; never present nominal credit as guaranteed available cash.

## Monitoring Thresholds

Configure billing notifications at cumulative actual-cost thresholds of:

```text
USD 25
USD 50
USD 100
USD 150
USD 180
USD 220
```

The USD 180 notification requires a cost review before further optional benchmarking or scope expansion. The USD 220 notification requires immediate review of active resources and remaining demo-critical work.

Google Cloud alerts-only budgets do not automatically stop usage or spending. Automatic billing disablement is not the default Scan Pilot control because it can stop services and risk resource loss. Cost containment should primarily use service-level maximum instances, concurrency and quota limits, retention policies, and deliberate shutdown of nonessential resources.

## Agent Decision Rules

Every proposal that introduces or materially changes a paid service must state:

1. why the service is needed for the accepted MVP;
2. estimated two-month cost and estimation source;
3. free-tier or promotional-credit assumptions;
4. scale-to-zero, quota, maximum-instance, retention, and shutdown controls;
5. a cheaper alternative and its trade-off;
6. what happens when credit expires;
7. which costs remain unverified until deployment measurement.

Agents must not treat unspent credit as a reason to enlarge scope. Security, privacy, simplicity, and evidence quality remain independent decision gates.

## Verification Before Deployment

- Confirm exact Free Trial expiry and each monthly credit's expiry and eligibility.
- Confirm the deployment region and use current regional prices.
- Estimate Cloud SQL as the principal always-on fixed-cost candidate.
- Estimate Cloud Run API and worker separately, including minimum and maximum instances.
- Confirm whether Gemini usage is billed through the same eligible Cloud Billing account.
- Create the accepted budget notifications and verify their recipients.
- Review cost reports daily during benchmark bursts and the public demo window.
- Record actual versus estimated spend so later agents do not continue using stale assumptions.

## Official References

- [Google Cloud Free Program](https://docs.cloud.google.com/free/docs/free-cloud-features)
- [Cloud Run pricing](https://cloud.google.com/run/pricing)
- [Cloud Run minimum instances](https://docs.cloud.google.com/run/docs/configuring/min-instances)
- [Cloud SQL pricing](https://cloud.google.com/sql/pricing/)
- [Cloud Storage pricing](https://cloud.google.com/storage/pricing)
- [Cloud Billing budgets](https://docs.cloud.google.com/billing/docs/how-to/budgets)
- [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing)

## Verification Limits

- The screenshot did not show expiry dates, eligibility restrictions, or credit application order.
- The allocation table is a planning estimate, not a Google Cloud Pricing Calculator export.
- Exact Cloud SQL, worker-memory, parser-runtime, Gemini-token, and regional costs remain unverified until benchmark and deployment configuration exist.
- Google pricing and credit terms can change; current official sources must be checked before provisioning.
