> **Document:** Scan Pilot Architecture Direction  
> **File:** `docs/ARCHITECTURE.md`  
> **Version:** v0.7.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-13  
> **Status:** Under Review  

# Scan Pilot Architecture Direction

## Accepted Shape

```text
React Web App
    ↓ REST
Spring Boot API
    ├── Authentication / Workspace
    ├── Project
    ├── GitHub Integration
    ├── Scan Orchestrator
    ├── Finding
    ├── Repository Profile
    ├── Human Review
    ├── Evidence
    ├── Finding Tracking
    ├── Scoring
    └── AI Provider Router
            ↓
          Gemini

GitHub → Webhooks → API → Scan Job → Isolated Scan Worker
PostgreSQL stores application state
```

## Repository Profile and Persistent State

Project Discovery uses deterministic extractors for manifests, repository structure, and machine-readable technology signals. Gemini may summarize selected, bounded document context after secret redaction. Profile claims retain their evidence source, scope, source commit, extractor version, and verification status.

PostgreSQL is the source of truth for Repository Profiles, scan checkpoints, finding history, and user-provided project context. Markdown inside a scanned repository is untrusted input and is not used as a runtime database or as an instruction channel for the scanner.

## Human Review Boundary

Human review is represented as persistent Review Requests linked to the relevant project, finding or profile claim, and source commit. Review Requests are asynchronous: scan jobs can finish while requests remain pending.

Structured answers and optional user additions are stored with actor and time attribution. They remain user assertions unless separate technical evidence corroborates them. Free-text content and supporting links are untrusted input, and secret redaction occurs before storage or AI processing.

## Evidence Boundary

The Evidence module provides a shared provenance contract for Repository Profile claims, Findings, and Review Requests. It persists typed Evidence Items and scoped claims without merging Technical Evidence, User Assertions, and AI Inferences into one undifferentiated confidence value.

Evidence records are append-oriented for auditability. A correction or later repository state may supersede an earlier record, but it does not rewrite the source and meaning of historical evidence. See `docs/EVIDENCE-MODEL.md`.

## Finding Tracking and Workspace Boundary

The Finding Tracking module combines rule-specific identity with commit history, diff metadata, Evidence Locations, optional semantic context, and re-scan results. See `docs/FINDING-TRACKING.md`.

Repository acquisition produces an immutable source snapshot identified by commit SHA. Tools that require mutation operate on a separate Disposable Mutable Workspace. Temporary markers, instrumentation, or candidate changes never modify the immutable snapshot or the GitHub repository.

Trusted acquisition credentials are removed before optional untrusted execution begins. Execution workers do not receive application database credentials, Gemini credentials, or GitHub App private keys. Full source snapshots are not retained long-term by default.

The trusted fingerprint boundary for `SP-CONFIG-001` computes `SP_SECRET_FP_V1` before any raw candidate can enter persisted or asynchronous result paths. The boundary has access to the fingerprint HMAC operation but does not expose key material to PostgreSQL or untrusted execution. This is a logical trust boundary and does not require a separate microservice.

## Frontend Direction

- React
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui
- TanStack Query

## Backend Direction

- Spring Boot 3
- Java 21
- RESTful API
- modular monolith
- Spring Data JPA/Hibernate
- Flyway migrations
- PostgreSQL

## Scan Processing

Repository scanning is asynchronous. The API orchestrates work and stores state; isolated workers perform repository checkout and scanner execution.

For `SP-CONFIG-001`, Git supplies commit-graph and range semantics and Gitleaks is the first detector behind an internal adapter. Scan Pilot captures an immutable HEAD SHA, runs an exact-HEAD snapshot scan for early results, then runs the required reachable-history scan. The history priority is graph-aware and newest-first; it must not assume a linear `HEAD~N` sequence.

The adapter is a trusted normalization boundary. Raw detector output is temporary sensitive material because it may contain the secret, match, or source line. Before results leave this boundary, Scan Pilot applies redaction, creates `SP_SECRET_FP_V1`, drops unsafe fields, and emits only normalized evidence and coverage telemetry. The temporary raw report is then deleted.

Coverage and checkpoint storage remains product-owned rather than detector-owned. A complete record binds repository and branch identity, captured HEAD, scan mode and scope, detector/version, rule/config digest, parser schema, Git commit expectations, detector telemetry, timing, and terminal outcome. Scanner exit code alone is insufficient. Checkpoints advance only after compatible coverage validation.

**Reason:** Scanned repositories are untrusted input. Isolation limits the effect of malicious files, resource exhaustion, and unsafe build/test behavior.

## AI Provider Boundary

Gemini is the first provider, but application modules should depend on an internal provider interface rather than Gemini-specific types.

**Reason:** This preserves the accepted future direction for BYOK and additional providers without requiring multi-provider delivery in the first release.

## Deployment Direction

- Google Cloud
- Cloud Run direction for deployable services
- Cloud SQL PostgreSQL direction
- Secret Manager for service secrets
- Firebase Hosting or another appropriate Google-hosted frontend option
- GitHub Actions for CI/CD

These are directions, not a final topology. Queue technology, worker service shape, network controls, and cost limits remain open.

## Module Boundary Principle

Keep the backend as one deployable modular monolith unless worker isolation requires a separate process. Do not introduce microservices merely because the domain has several modules.

**Reason:** A solo developer with an August 2026 deadline benefits from one coherent deployment and transaction boundary while still maintaining internal modularity.

## Unresolved Architecture Decisions

- exact authentication mechanism;
- GitHub App installation and token lifecycle details;
- queue/cache technology and whether Redis is necessary for V1;
- worker sandbox technology and resource limits;
- repository artifact retention;
- exact deployment split and networking;
- AI prompt/context storage policy;
- BYOK encryption and rotation;
- exact database schema and optional confidence representation for the accepted evidence model;
- exact semantic-matching strategies for non-secret Finding families;
- exact fingerprint key service, IAM policy, and rotation operation;
- exact execution sandbox technology and source-snapshot retention controls;
- Review Request authorization, notification, expiry, and invalidation rules.
- exact Gitleaks adapter command, telemetry extraction, timeout, and report-cleanup implementation after the specification benchmark;
- exact pinned Gitleaks version or container digest and upgrade policy;
