> **Document:** Scan Pilot Project README
> **File:** `README.md`
> **Version:** v1.3.1
> **Created:** 2026-08-11
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot

Scan Pilot is a continuous multi-project health monitoring platform for software created or maintained with AI coding tools.

It connects to GitHub repositories, scans them after relevant events, presents evidence-backed findings in a dashboard, explains problems in accessible language with Gemini, and verifies whether fixes improve the project over time.

## Product Idea

Scan Pilot follows a workflow similar to antivirus software, but for source-code projects:

```text
Connect repositories
→ baseline scan
→ review findings
→ fix externally
→ re-scan
→ verify improvement
→ detect regressions
```

The product is dashboard-first rather than chatbot-first. AI supports analysis, explanation, prioritization, and remediation guidance; it is not the sole authority for security truth.

## Primary Users

- solo builders and students;
- small development teams;
- developers using Codex, Gemini, Antigravity, or similar coding agents;
- people responsible for several AI-generated or AI-assisted repositories.

## MVP Direction

The MVP is intended to run against real repositories and demonstrate a complete workflow:

- connect a GitHub repository;
- run a baseline or event-triggered scan;
- produce findings with severity, evidence, verification limits, and remediation guidance;
- use Gemini for contextual analysis where useful;
- create a GitHub Issue from a finding;
- re-scan after a fix;
- distinguish `OPEN`, `RESOLVED`, and `REGRESSED` findings;
- deploy the working product on Google Cloud.

The first accepted MVP rule is `SP-CONFIG-001 — Source Code Secret Exposure`.

The accepted second-rule direction is the gated stretch rule `SP-CI-001 — Mutable Remote GitHub Actions Reference`. It will check immutable-reference policy for supported remote GitHub Actions references only after the core scan path is stable; it does not claim broad OWASP coverage or compromise detection.

## Technology Direction

| Area | Direction |
|---|---|
| Frontend | React 19, TypeScript 5.8, Vite 6, Tailwind CSS 4, Lucide Icons |
| Backend | Spring Boot 3.4.3, Java 21, Apache Maven, RESTful modular monolith |
| Database | PostgreSQL, Spring Data JPA/Hibernate, Flyway migrations |
| Security Inspection | Pinned SP-CONFIG-001 policy, Gitleaks engine, HMAC-SHA-256 fingerprinting |
| AI Guidance | Configurable Gemini REST integration with `gemini-1.5-flash` as the current default and deterministic fallback guidance |
| Continuous Deployment | GitHub Actions automated CD pipeline to Google Cloud Run |

## Live Deployment

- **Backend API (Google Cloud Run):** [`https://scan-pilot-api-drbjfwrlxq-as.a.run.app`](https://scan-pilot-api-drbjfwrlxq-as.a.run.app/api/v1/system/status) (Region: `asia-southeast1`, Scale-to-Zero $0 idle cost)
- **Deployment Specification:** [Cloud Run Deployment Spec](docs/DEPLOYMENT-SPEC.md)
- **AI Studio Deployment Guide:** [AI Studio Frontend Sync & Publish Guide](docs/AI-STUDIO-DEPLOYMENT-GUIDE.md)

Detailed infrastructure choices that remain open are recorded in [Accepted Decisions](docs/DECISIONS.md).

## Documentation Entry Points

- [Project context](docs/PROJECT-CONTEXT.md)
- [Accepted decisions](docs/DECISIONS.md)
- [Current status](docs/CURRENT-STATUS.md)
- [Implementation baseline and gap register](docs/IMPLEMENTATION-BASELINE.md)
- [Product definition](docs/PRODUCT.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Inspection specification](docs/INSPECTION-SPEC.md)
- [Architecture direction](docs/ARCHITECTURE.md)
- [Research sources](docs/RESEARCH-SOURCES.md)
- [Documentation index](docs/README.md)

Agents must begin with [AGENTS.md](AGENTS.md).

## Current Phase

The project is in **implementation stabilization and next-slice planning**. The backend scanning vertical slice, persistence, CI, and backend Cloud Run deployment exist, but the application must not yet be described as a complete continuous-monitoring product. The current frontend has a TypeScript lint failure, remote scans do not include complete Git history, scan execution is synchronous, and some dashboard telemetry remains simulated. See the [implementation baseline](docs/IMPLEMENTATION-BASELINE.md) for verified, partial, UI-only, and specified capabilities.

On 2026-08-16, the Product Owner accepted the Eligibility Spike `CONDITIONAL GO` and authorized Issue-driven implementation under `DEC-054`; that decision did not waive the remaining security, cost, verification, or delivery gates.

The official external submission deadline for AI Riser Vietnam 2026 is **2026-08-30 at 23:59 GMT+7**.

## Contributing

Follow [CONTRIBUTING.md](CONTRIBUTING.md). Do not commit, push, merge, or open a pull request unless the user explicitly authorizes that action.

## License

See [LICENSE](LICENSE).
