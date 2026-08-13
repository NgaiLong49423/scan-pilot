> **Document:** Scan Pilot Project README  
> **File:** `README.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-11  
> **Last Updated:** 2026-08-12  
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

## Technology Direction

| Area | Direction |
|---|---|
| Frontend | React, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack Query |
| Backend | Spring Boot 3, Java 21, RESTful modular monolith |
| Database | PostgreSQL, Spring Data JPA/Hibernate, Flyway |
| Repository integration | GitHub App and webhooks |
| AI | Gemini first, provider abstraction for later providers and BYOK |
| Scanning | asynchronous isolated scan workers |
| Deployment | Google Cloud direction, GitHub Actions CI/CD |

Detailed infrastructure choices that remain open are recorded in [Accepted Decisions](docs/DECISIONS.md).

## Documentation Entry Points

- [Project context](docs/PROJECT-CONTEXT.md)
- [Accepted decisions](docs/DECISIONS.md)
- [Current status](docs/CURRENT-STATUS.md)
- [Product definition](docs/PRODUCT.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Inspection specification](docs/INSPECTION-SPEC.md)
- [Architecture direction](docs/ARCHITECTURE.md)
- [Research sources](docs/RESEARCH-SOURCES.md)
- [Documentation index](docs/README.md)

Agents must begin with [AGENTS.md](AGENTS.md).

## Current Phase

The project is in research and specification. Product implementation has not started.

The target submission window ends in August 2026. The exact external deadline has not been independently recorded, so agents must not invent a more precise date without user confirmation.

## Contributing

Follow [CONTRIBUTING.md](CONTRIBUTING.md). Do not commit, push, merge, or open a pull request unless the user explicitly authorizes that action.

## License

See [LICENSE](LICENSE).
