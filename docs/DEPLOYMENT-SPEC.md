> **Document:** Scan Pilot Cloud Run Deployment Specification
> **File:** `docs/DEPLOYMENT-SPEC.md`
> **Version:** v2.0.1
> **Created:** 2026-08-19
> **Last Updated:** 2026-09-02
> **Status:** Active

# Scan Pilot Cloud Run Deployment Specification

## 1. Overview & Architecture

Scan Pilot adopts a **Decoupled Multi-Service Cloud Run Architecture** (`DEC-056`) aligning with the official *Google AI & Vibe Coding Handbook* (`goo.gle/itsvibecoding`) and AI Riser Vietnam 2026 competition evaluation criteria:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                 GitHub repository (`frontend/`)                         │
│      (Canonical React source, CI verification, Cloud Run workflow)     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ GitHub Actions frontend delivery
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 Frontend Service: scan-pilot-web                       │
│              Domain: https://scan-pilot-web-*.run.app                  │
│  - React 18 + TypeScript + Vite + Tailwind CSS + Lucide Icons          │
│  - Single Page Application (SPA) with Client-Side Routing              │
│  - Base API URL resolved dynamically from VITE_API_BASE_URL            │
│  - Scale-to-Zero (min-instances = 0, $0 idle cost)                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS + CORS Credentials
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 Backend Service: scan-pilot-api (LIVE)                 │
│              Domain: https://scan-pilot-api-drbjfwrlxq-as.a.run.app    │
│  - Spring Boot 3.4.3 + Java 21 JRE                                     │
│  - Cloud SQL Postgres Socket Factory (com.google.cloud.sql.postgres)   │
│  - Dedicated Runtime Service Account: scan-pilot-api-runner            │
│  - Flyway Database Migrations (12 Core PostgreSQL tables)              │
│  - Pinned SP-CONFIG-001 Policy & Gitleaks Detection Engine             │
│  - Gemini AI Explanation Client (gemini-1.5-flash)                     │
│  - Exact CORS origin for the deployed web service                      │
│  - Scale-to-Zero (min-instances = 0, $0 idle cost)                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Cloud SQL Unix Socket / Socket Factory
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Database Layer: Managed Cloud SQL PostgreSQL             │
│  - Instance: gen-lang-client-0098508328:asia-southeast1:scan-pilot-db   │
│  - Engine: PostgreSQL 16 (Enterprise HA/Zonal storage)                │
│  - Strict UUID primary keys and repository tenant isolation            │
│  - Fail-closed startup validator (ProductionDatasourceStartupValidator)│
│  - Zero raw credential persistence                                     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Backend Containerization & Cloud Run Contract

### 2.1 Multi-Stage Dockerfile Standard (`backend/Dockerfile`)

* **Stage 1 (Builder):** `maven:3.9.9-eclipse-temurin-21-alpine` compiles and packages the JAR.
* **Stage 2 (Runtime):** `eclipse-temurin:21-jre-alpine` runs the optimized executable.
* **Security Guardrails:**
  * Runs as non-root user (`scanpilot:scanpilot`, UID 10001).
  * Read-only root filesystem with explicit `/tmp` volume.
  * Dedicated runtime service account: `scan-pilot-api-runner@gen-lang-client-0098508328.iam.gserviceaccount.com` (provisioned after reviewed merge and deployment).
  * Image size budget: $< 180\text{ MB}$.

### 2.2 Environment Variables & Secrets Configuration

All database credentials and sensitive API secrets are injected securely via Google Cloud Secret Manager (provisioned and verified after reviewed merge and deployment). No raw secrets or connection strings are stored in code or repository configuration.

| Variable Name | Required | Source / Secret Name | Description |
|---|:---:|---|---|
| `PORT` | Yes | Cloud Run Default (`8080`) | Container listening port assigned by Cloud Run |
| `SPRING_PROFILES_ACTIVE` | Yes | Static (`prod`) | Spring profile activating Cloud configuration & fail-closed validation |
| `COOKIE_SECURE` | Yes | Static (`true`) | Enforces HTTPS-only secure cookies in production |
| `SPRING_DATASOURCE_URL` | Yes | Secret `scan-pilot-db-url:latest` | PostgreSQL JDBC connection URL with Cloud SQL Socket Factory |
| `SPRING_DATASOURCE_USERNAME` | Yes | Secret `scan-pilot-db-user:latest` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Secret `scan-pilot-db-password:latest` | Database password |
| `GITHUB_CLIENT_ID` | Optional | GitHub Actions Secret `GH_APP_CLIENT_ID` | GitHub App OAuth client ID |
| `GITHUB_CLIENT_SECRET` | Optional | Secret `scan-pilot-github-client-secret:latest` | GitHub App OAuth client secret |
| `SCANPILOT_HMAC_SECRET_KEY`| Yes | Secret `scanpilot-hmac-key:latest` | Cryptographic key for `SP_SECRET_FP_V1` fingerprinting (Mandatory in prod, fail-closed) |
| `GEMINI_API_KEY` | Optional | Secret `scanpilot-gemini-key:latest` | Gemini 1.5 Flash API Key |
| `SCANPILOT_CORS_ALLOWED_ORIGINS` | Yes | Workflow-managed Env | Exact deployed frontend origin |

### 2.3 Cloud SQL Connectivity & Socket Factory

Scan Pilot connects to Cloud SQL using the official Google Cloud SQL Postgres Socket Factory (`com.google.cloud.sql:postgres-socket-factory`):
- **Cloud SQL Instance Connection Name:** `gen-lang-client-0098508328:asia-southeast1:scan-pilot-db`
- **Socket Factory Driver Class:** `com.google.cloud.sql.postgres.SocketFactory`
- **HikariCP Pool Bounds:** Maximum 5 connections, minimum 1 idle connection, connection timeout 30s, max lifetime 30m.
- **Fail-Closed Validation:** `ProductionDatasourceStartupValidator` verifies the active database product at startup and immediately aborts if any non-PostgreSQL (e.g. H2) or inaccessible datasource is detected.

### 2.4 Cloud Run Resource Sizing & Cost Guardrails (`docs/CLOUD-BUDGET.md`)

* **Region:** `asia-southeast1` (Singapore).
* **Service Account:** `scan-pilot-api-runner@gen-lang-client-0098508328.iam.gserviceaccount.com` (effective after reviewed merge and deployment).
* **CPU:** `1` (`--cpu=1`).
* **Memory:** `512Mi` (`--memory=512Mi`).
* **Scaling:** `min-instances = 0` (`--min-instances=0`, Scale-to-Zero), `max-instances = 2` (`--max-instances=2`).

---

## 3. Frontend Delivery Contract

### 3.1 Dual-Origin & Base URL Resolution (`frontend/src/services/api.ts`)

```typescript
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');
```

### 3.2 Source and Runtime Boundary

1. **Canonical source:** Git-tracked `frontend/` is the only production frontend source.
2. **Container contract:** `frontend/Dockerfile` builds Vite with the public API URL and serves the SPA through Nginx on port `8080`; `frontend/nginx.conf` provides `/_scanpilot_health` and history fallback.
3. **Delivery contract:** `.github/workflows/deploy-frontend-cloud-run.yml` runs Node 20 verification, deploys `scan-pilot-web`, then sets the API's `FRONTEND_URL` and exact allowed CORS origin to the deployed service URL.
4. **AI Studio boundary:** Google AI Studio may be used for experiments only. It must not publish a production copy of the application.

---

## 4. Step-by-Step Deployment Runbook

### Step 1: Deploy Backend to Cloud Run (Automated via GitHub Actions / Manual CLI)

```bash
# Build and submit container image via Cloud Build / Artifact Registry
PROJECT_ID="gen-lang-client-0098508328"
IMAGE_TAG="asia-southeast1-docker.pkg.dev/${PROJECT_ID}/scan-pilot/api:latest"
gcloud builds submit --tag "${IMAGE_TAG}" backend/

# Deploy to Cloud Run with dedicated service account and Cloud SQL connection
gcloud run deploy scan-pilot-api \
  --image "${IMAGE_TAG}" \
  --region asia-southeast1 \
  --min-instances 0 \
  --max-instances 2 \
  --memory 512Mi \
  --cpu 1 \
  --allow-unauthenticated \
  --service-account scan-pilot-api-runner@gen-lang-client-0098508328.iam.gserviceaccount.com \
  --set-cloud-sql-instances gen-lang-client-0098508328:asia-southeast1:scan-pilot-db \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod,COOKIE_SECURE=true,GITHUB_CLIENT_ID=${GH_APP_CLIENT_ID}" \
  --set-secrets "SPRING_DATASOURCE_URL=scan-pilot-db-url:latest,SPRING_DATASOURCE_USERNAME=scan-pilot-db-user:latest,SPRING_DATASOURCE_PASSWORD=scan-pilot-db-password:latest,GITHUB_CLIENT_SECRET=scan-pilot-github-client-secret:latest,SCANPILOT_HMAC_SECRET_KEY=scanpilot-hmac-key:latest"
```

### Step 2: Deploy the Git-Tracked Frontend
1. Merge an authorized pull request that changes `frontend/**` or `.github/workflows/deploy-frontend-cloud-run.yml` into `main`.
2. GitHub Actions verifies the frontend, builds `frontend/Dockerfile` with Cloud Build, and deploys the `scan-pilot-web` Cloud Run service.
3. The workflow reads the deployed service URL, then updates `scan-pilot-api` with that exact `FRONTEND_URL` and CORS origin.
4. The workflow calls `/_scanpilot_health` and asserts that the response body is `ok`; a successful response proves only that the frontend container is reachable. Browser login, logout, and scan flow still require production acceptance testing.
