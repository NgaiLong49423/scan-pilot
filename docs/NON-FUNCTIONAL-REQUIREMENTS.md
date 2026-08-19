> **Document:** Scan Pilot Non-Functional Requirements Specification
> **File:** `docs/NON-FUNCTIONAL-REQUIREMENTS.md`
> **Version:** v1.0.0
> **Created:** 2026-08-19
> **Last Updated:** 2026-08-19
> **Status:** Active

# Scan Pilot Non-Functional Requirements (NFR) Specification

This document establishes the quantitative non-functional requirements (`NFR-001` through `NFR-010`) for the Scan Pilot MVP, specifying exact metric targets, constraints, verification methods, and acceptance thresholds.

---

## NFR Summary Matrix

| ID | Category | Metric Target | Verification Method | Traceability |
|---|---|---|---|---|
| **NFR-001** | **API Latency & Responsiveness** | p95 < 200ms (metadata/read queries), p95 < 500ms (job creation) | Automated JMeter / REST integration benchmarks | `FR-008`, `DEC-005` |
| **NFR-002** | **Scan Pipeline Throughput** | Snapshot scan < 5s, Reachable Git History baseline < 15s (standard 500-commit repo) | Pipeline timer telemetry & `ScanPipelineServiceTest` | `FR-002`, `FR-025`, `DEC-040` |
| **NFR-003** | **Gemini AI Latency & Availability** | AI Response < 3s, Max timeout 15s, 100% Fallback uptime (zero scan interruption) | `GeminiExplanationServiceTest` & Mock API tests | `FR-005`, `FR-048`, `DEC-048` |
| **NFR-004** | **Zero Secret Exposure & Privacy** | 0 unmasked secrets in logs, DB, prompts, REST DTOs, or browser storage (100% redaction) | HMAC-SHA-256 validation, regex log scrapers, E2E tests | `FR-017`, `FR-048`, `DEC-038`, `DEC-048` |
| **NFR-005** | **Memory Safety & Resource Bounds** | Bounded 8 KiB byte-sampling buffer; 10/50 MiB size ceilings; 100% workspace deletion | JVM Heap profiler & `GitWorkspaceManagerTest` | `FR-016`, `FR-035`, `FR-037`, `DEC-015` |
| **NFR-006** | **UI Accessibility & Visual Contrast** | WCAG 2.1 AA compliant (contrast ratio > 4.5:1), Tabular figures, Keyboard navigation | Axe-core accessibility audit & Lighthouse | `design-taste-frontend`, `ui-design-audit` |
| **NFR-007** | **Frontend State Completeness** | 4 complete states (Skeleton loading, Empty reassurance, Error recovery, Normal populated) | Vitest UI Component tests & UI design audit | `design-taste-frontend`, `ui-design-audit` |
| **NFR-008** | **Browser & Device Compatibility** | 100% rendering on Chrome 120+, Firefox 120+, Safari 17+, Edge 120+; iOS Safari `dvh` stable | Playwright cross-browser tests | `design-taste-frontend` |
| **NFR-009** | **Cloud Cost & Resource Sizing** | Expected 2-month spend <= $180 ($70 reserve within $250 envelope); Scale-to-zero | Cloud Billing alerts & Budget monitor | `docs/CLOUD-BUDGET.md`, `DEC-054` |
| **NFR-010** | **Database Integrity & Idempotency** | 100% ACID transactional consistency; Unique constraints prevent duplicate findings | PostgreSQL Testcontainers & integration suites | `FR-010`, `FR-011`, `FR-014`, `DEC-006`, `DEC-039` |

---

## Detailed Specifications

### NFR-001: API Latency & Responsiveness
* **Description:** All synchronous REST API endpoints must respond swiftly to provide a fluid user experience without UI freezing.
* **Quantitative Targets:**
  * `GET /api/v1/auth/me`: p95 < 50ms.
  * `GET /api/v1/projects/current`, `GET /api/v1/scans/repositories/{id}/findings`: p95 < 150ms.
  * `POST /api/v1/scans/trigger`: p95 < 300ms (asynchronous dispatch, immediate 202/200 response).
* **Acceptance Criteria:** Under concurrency load of 20 simultaneous users, no query exceeds 1000ms.

---

### NFR-002: Scan Pipeline Throughput
* **Description:** The asynchronous scan pipeline must complete analysis promptly to enable continuous feedback during development.
* **Quantitative Targets:**
  * **Stage 1 (HEAD Snapshot Scan):** Completes in < 5.0 seconds for repositories up to 10,000 files / 50 MB total text.
  * **Stage 2 (Git History Baseline Scan):** Completes in < 15.0 seconds for repositories up to 500 Git commits.
* **Acceptance Criteria:** Total scan duration is accurately captured in `scan_jobs.duration_ms`.

---

### NFR-003: Gemini AI Latency & Bounded Quota
* **Description:** Google Gemini AI integration must deliver rapid structured reasoning and never block or crash core scanning operations.
* **Quantitative Targets:**
  * **Direct API Call Latency:** p90 < 2.5 seconds using `gemini-1.5-flash`.
  * **Local Cache Hit Latency:** < 20ms using in-memory `ConcurrentHashMap`.
  * **Hard Timeout Ceiling:** Exactly 15.0 seconds (`scanpilot.ai.gemini.timeout-seconds = 15`).
  * **Fallback Reliability:** 100% availability of deterministic fallback templates on network drop or quota depletion (`HTTP 429`).
* **Acceptance Criteria:** A failed Gemini request never transitions a `ScanJob` to `FAILED`.

---

### NFR-004: Zero Secret Exposure & Masking Privacy
* **Description:** Detected secrets must be protected at all times across memory, disk, network, prompts, and user displays.
* **Quantitative Targets:**
  * 0 raw unmasked secrets stored in PostgreSQL `findings`, `evidence_items`, or `scan_jobs`.
  * 0 raw unmasked secrets logged via SLF4J / Logback.
  * 0 raw secrets sent in Google Gemini API prompts (only `[REDACTED_SECRET]` and masked tokens `AIzaSy...****`).
  * 100% of finding fingerprints use length-prefixed HMAC-SHA-256 (`SP_SECRET_FP_V1`).
* **Acceptance Criteria:** Automated regex inspection across all test outputs and log artifacts confirms zero credential leakage.

---

### NFR-005: Memory Safety & Resource Bounds
* **Description:** Backend memory consumption must remain strictly bounded to prevent Out-Of-Memory (OOM) crashes in resource-constrained environments (e.g. 512MB / 1GB Cloud Run).
* **Quantitative Targets:**
  * **Classification Byte Sampling Buffer:** Fixed 8 KiB buffer (`SAMPLE_SIZE_BYTES = 8192`). Large files (10MB/50MB) are never buffered wholly into the JVM heap.
  * **Continuous Monitoring Limit:** Files > 10 MiB skipped with `MONITORING_FILE_SIZE_LIMIT_EXCEEDED` (`FR-037`).
  * **Release Assessment Ceiling:** Files > 50 MiB skipped with `RELEASE_FILE_SIZE_CEILING_EXCEEDED` (`FR-037`).
  * **Workspace Cleanup:** 100% of temporary directories (`scanpilot-ws-*`) deleted in `try-finally` blocks.
* **Acceptance Criteria:** No leftover directories in `java.io.tmpdir` after 100 consecutive scan iterations.

---

### NFR-006: UI Accessibility & Visual Contrast (WCAG 2.1 AA)
* **Description:** The user interface must be accessible and readable for all developers, including those with visual impairments.
* **Quantitative Targets:**
  * **Color Contrast Ratio:** All normal text against background strictly exceeds **4.5:1**; large text (> 18pt) exceeds **3.0:1**.
  * **Severity Badges:** Must pair color with text labels (never color alone) and contrast-compliant borders (`CRITICAL`: soft red container; `RESOLVED`: soft emerald container).
  * **Numeric Stability:** Timestamps, line numbers, and metrics must use `tabular-nums` font property to eliminate layout shifting.
* **Acceptance Criteria:** Automated Axe-core / Lighthouse audit returns 0 contrast violations.

---

### NFR-007: Frontend State Completeness
* **Description:** Every interactive dashboard component must gracefully handle all lifecycle states.
* **Quantitative Targets:**
  * **1. Loading State:** Shimmering skeleton shaped like the actual finding card/table (no lonely spinners).
  * **2. Empty State:** Reassuring graphic and guidance when 0 findings exist or when no repo is connected.
  * **3. Error State:** Clear plain-language explanation with a dedicated "Retry" button.
  * **4. Normal State:** Rich data with responsive grid and clean visual hierarchy.
* **Acceptance Criteria:** 100% of primary views implement the 4 explicit UI states.

---

### NFR-008: Browser & Device Compatibility
* **Description:** Frontend must render identically and operate smoothly across modern web browsers and mobile viewports.
* **Quantitative Targets:**
  * Target browser versions: Chrome 120+, Firefox 120+, Safari 17+, Edge 120+.
  * Viewport adaptability: Mobile (375px+), Tablet (768px+), Desktop (1024px, 1440px, 1920px).
  * iOS Safari Viewport: Uses `min-h-[100dvh]` to prevent dynamic address bar jumping bugs.
* **Acceptance Criteria:** Zero horizontal overflow or layout breakage across tested screen widths.

---

### NFR-009: Cloud Cost & Resource Sizing
* **Description:** Deployment and operation must strictly adhere to the accepted Cloud Budget constraints (`docs/CLOUD-BUDGET.md`).
* **Quantitative Targets:**
  * Two-month total planning envelope: $250.00 USD.
  * Expected target operational spend: <= $180.00 USD.
  * Preserved budget reserve: >= $70.00 USD.
  * Cloud Run Scale-to-Zero configured (`min-instances = 0`) during idle periods.
  * Maximum 1 active scan-worker instance during MVP evaluation.
* **Acceptance Criteria:** GCP Cloud Billing alerts configured at $25, $50, $100, $150, $180, and $220.

---

### NFR-010: Database Integrity & Idempotency
* **Description:** Database persistence must guarantee absolute transactional consistency and prevent duplicate or corrupted states.
* **Quantitative Targets:**
  * Composite Unique Constraint `UNIQUE(repository_id, fingerprint)` prevents duplicate findings.
  * Composite Unique Constraint `UNIQUE(repository_id, branch_name)` guarantees maximum 1 PRIMARY and 2 SECONDARY branches.
  * Re-scan operations must update findings idempotently without duplicating historical evidence.
* **Acceptance Criteria:** Concurrent re-scans on the same repository produce identical deterministic finding records.
