> **Document:** Scan Pilot Cloud Run Deployment Specification
> **File:** `docs/DEPLOYMENT-SPEC.md`
> **Version:** v1.0.0
> **Created:** 2026-08-19
> **Last Updated:** 2026-08-19
> **Status:** Active

# Scan Pilot Cloud Run Deployment Specification

## 1. Overview & Architecture

Scan Pilot adopts a **Decoupled Multi-Service Cloud Run Architecture** (`DEC-056`) aligning with the official *Google AI & Vibe Coding Handbook* (`goo.gle/itsvibecoding`) and AI Riser Vietnam 2026 competition evaluation criteria:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      Google AI Studio Project                          │
│            (Frontend React Source + Gemini Reasoning Prompt)           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ AI Studio "Deploy to Cloud Run"
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
│  - Flyway Database Migrations (12 Core PostgreSQL tables)              │
│  - Pinned SP-CONFIG-001 Policy & Gitleaks Detection Engine             │
│  - Gemini AI Explanation Client (gemini-1.5-flash)                     │
│  - Multi-Origin CORS: https://aistudio.google.com + Web Service Origin │
│  - Scale-to-Zero (min-instances = 0, $0 idle cost)                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ JDBC Connection Pool (HikariCP)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Database Layer: PostgreSQL                      │
│  - Managed Cloud SQL / Serverless PostgreSQL (PostgreSQL 15/16)        │
│  - Strict UUID primary keys and repository tenant isolation            │
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
  * Image size budget: $< 180\text{ MB}$.

### 2.2 Environment Variables & Secrets Configuration

| Variable Name | Required | Default / Format | Description |
|---|:---:|---|---|
| `PORT` | Yes | `8080` | Container listening port assigned by Cloud Run |
| `SPRING_PROFILES_ACTIVE` | Yes | `prod` | Spring profile activating Cloud configuration |
| `JDBC_DATABASE_URL` | Yes | `jdbc:postgresql://<host>:5432/<db>` | Database JDBC URL |
| `JDBC_DATABASE_USERNAME` | Yes | - | Database username |
| `JDBC_DATABASE_PASSWORD` | Yes | - | Database password (stored in Cloud Secret Manager) |
| `SCANPILOT_HMAC_SECRET_KEY`| Yes | 64-character Hex string | Cryptographic key for `SP_SECRET_FP_V1` |
| `GEMINI_API_KEY` | Yes | Google GenAI API Key | Gemini 1.5 Flash API Key |
| `GITHUB_CLIENT_ID` | Optional | GitHub OAuth App ID | GitHub OAuth client ID |
| `GITHUB_CLIENT_SECRET` | Optional | GitHub OAuth Secret | GitHub OAuth client secret |
| `SCANPILOT_CORS_ALLOWED_ORIGINS` | Yes | `https://aistudio.google.com,https://scan-pilot-web-*.run.app` | Comma-separated CORS allowed origins |

### 2.3 Cloud Run Resource Sizing & Cost Guardrails (`docs/CLOUD-BUDGET.md`)

* **Region:** `asia-southeast1` (Singapore) or `asia-east1` (Taiwan).
* **CPU:** `1 vCPU`.
* **Memory:** `512 MiB` (Max `1 GiB`).
* **Concurrency:** `80`.
* **Scaling:** `min-instances = 0` (Scale-to-Zero), `max-instances = 2`.
* **Timeout:** `60s`.

---

## 3. Frontend & Google AI Studio Contract

### 3.1 Dual-Origin & Base URL Resolution (`src/api/client.ts`)

```typescript
const BASE_URL = import.meta.env.VITE_API_BASE_URL 
  ? `${import.meta.env.VITE_API_BASE_URL}/api/v1` 
  : '/api/v1';
```

### 3.2 AI Studio Prompt & File Boundary Guardrails

To prevent LLM code generation inside Google AI Studio from corrupting API integration:
1. **Immutable API Layer (`src/api/`):** Contains typed contracts (`authApi`, `githubApi`, `projectsApi`, `scansApi`, `aiApi`).
2. **Customizable UI Layer (`src/components/`):** Presentation components (`FindingCard`, `ScanProgressBar`, `CoverageTab`, `Header`) that consume API state.
3. **Graceful Fallback Guarantee:** All API calls catch HTTP `404/500/NetworkError` and render friendly error banners or demo reassurance states instead of unhandled crashes.

---

## 4. Step-by-Step Deployment Runbook

### Step 1: Deploy Backend to Cloud Run
```bash
# Build and submit container image to Google Artifact Registry
gcloud builds submit --tag asia-southeast1-docker.pkg.dev/gen-lang-client-0098508328/scan-pilot/api:v1.0.0 backend/

# Deploy to Cloud Run
gcloud run deploy scan-pilot-api \
  --image asia-southeast1-docker.pkg.dev/gen-lang-client-0098508328/scan-pilot/api:v1.0.0 \
  --region asia-southeast1 \
  --min-instances 0 \
  --max-instances 2 \
  --memory 512Mi \
  --cpu 1 \
  --allow-unauthenticated \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod,SCANPILOT_CORS_ALLOWED_ORIGINS=https://aistudio.google.com" \
  --set-secrets "JDBC_DATABASE_URL=scanpilot-db-url:latest,JDBC_DATABASE_PASSWORD=scanpilot-db-pass:latest,GEMINI_API_KEY=scanpilot-gemini-key:latest,SCANPILOT_HMAC_SECRET_KEY=scanpilot-hmac-key:latest"
```

### Step 2: Configure AI Studio Frontend & Deploy
1. Open Google AI Studio Project workspace.
2. In Project Settings / Environment Variables, add `VITE_API_BASE_URL=https://scan-pilot-api-drbjfwrlxq-as.a.run.app`.
3. Click **Deploy to Cloud Run** in Google AI Studio.
4. Update Backend `SCANPILOT_CORS_ALLOWED_ORIGINS` with the generated Frontend URL.
