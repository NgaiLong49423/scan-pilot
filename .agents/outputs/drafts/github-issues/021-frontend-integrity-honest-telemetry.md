> **Document:** GitHub Issue Draft 021 - Frontend Integrity and Honest Telemetry
> **File:** `.agents/outputs/drafts/github-issues/021-frontend-integrity-honest-telemetry.md`
> **Version:** v1.1.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Archived

# [Frontend][DEC-060] Restore Build Integrity and Evidence-Backed Telemetry

Created as [GitHub Issue #51](https://github.com/NgaiLong49423/scan-pilot/issues/51) on 2026-08-20. This local draft is retained as the source record and was refined before implementation handoff.

## Tóm tắt

Khôi phục frontend buildable và trung thực: mọi security state hiển thị phải đến từ dữ liệu backend đã tồn tại, hoặc hiển thị neutral `Not available`. Issue này không xây asynchronous progress API/worker.

## Source Trace

- `DEC-060`
- `FR-004`, `FR-007`, `FR-008`, `FR-028`
- `NFR-004`, `NFR-007`
- `docs/IMPLEMENTATION-BASELINE.md`
- [Issue #52](https://github.com/NgaiLong49423/scan-pilot/issues/52) owns asynchronous scan jobs and real intermediate progress.

## Mục tiêu

Người dùng không thể nhìn thấy số liệu, file path, pipeline stage, duration, trend, MTTR, AI rate hoặc lifecycle state do client tự tạo mà tưởng là evidence của scanner.

## Phạm vi

- Fix the `HealthGauge` prop contract and restore green lint/build.
- Remove timer-generated file paths, counts, stage progression, fallback values, static MTTR, AI success rate, and trend data from security telemetry.
- Render only persisted/backend-backed completed scan data currently exposed by the application; render unavailable data as a neutral state.
- Remove client-only `RESOLVED / VERIFIED_COMPLETE` mutation; remediation remains clearly non-mutating guidance.
- Perform and record the manual state matrix for unscanned, trigger-in-progress, failed trigger, completed scan, and unavailable metric states.

## Không nằm trong phạm vi

- Asynchronous worker, scan-job persistence/polling, server-sent events, websocket, or true intermediate progress: Issue #52.
- New backend API, database schema, queue, cloud resource, dependency, test framework, UI redesign, or metric/scoring redesign.
- Changing a Finding lifecycle through any frontend-only action.

## Data Presentation Contract

| Data or UI state | #51 requirement |
|---|---|
| Existing backend findings and coverage after a completed scan | May be rendered with their recorded scope and limits. |
| Live file path, intermediate file count, intermediate finding count, stage progression, and elapsed scan timer | Must be removed or rendered unavailable; the current backend does not provide trustworthy intermediate telemetry. |
| `375` file fallback, MTTR `12`, AI success `98`/`100`, static trend arrays | Must not be shown as observed data. |
| `Apply AI Fix` | Must become non-mutating guidance/copy behavior; it cannot change `OPEN`, `RESOLVED`, `REGRESSED`, `ACTION_REQUIRED`, `RISK_CONTAINED`, or `VERIFIED_COMPLETE`. |
| Missing metrics | Must use `Not available`, `Awaiting scan`, or another neutral wording; never green/safe/verified. |

## Implementation Notes

- The observed lint blocker is `App.tsx` passing `isScanned` to `HealthGauge` without a declared prop.
- Current simulation and client mutation are concentrated in `frontend/src/App.tsx`, `LiveScanTerminal.tsx`, `ScanProgressStepper.tsx`, `TrendSparkline.tsx`, `MetricsGrid.tsx`, and `RemediationDiff.tsx`.
- `frontend/package.json` has `lint` and `build` scripts but no test script or test framework. Do not add a dependency solely for this Issue; provide the required manual state verification instead.

## Acceptance Criteria

- [ ] `npm run lint` and `npm run build` pass without adding dependencies.
- [ ] `HealthGauge` prop contract is internally consistent and TypeScript has no error for `isScanned`.
- [ ] No timer-generated file path, scan count, finding count, duration, MTTR, AI success rate, static trend, or fallback file count is labeled as observed/verified security data.
- [ ] The running state never presents simulated stage completion, percentage, file count, file path, leak count, or elapsed duration as backend telemetry.
- [ ] A Finding cannot become `RESOLVED` or `VERIFIED_COMPLETE` from client state alone; remediation UI is explicitly non-mutating.
- [ ] Missing telemetry and unavailable metrics render a neutral state and do not imply clean, safe, completed, or verified coverage.
- [ ] Manual verification covers unscanned, trigger-in-progress, failed trigger, completed scan, and unavailable-metric states, with results recorded in the implementation report.

## Planning

- Type: Bug / Security / Frontend
- Priority: Critical
- Size: M
- Story Points: 5
- Parent: None
- Blocked by: None
- Blocking: #52
- Suggested branch: `codex/<issue-number>-frontend-honest-telemetry`
- Security alert: Yes - false security-state claims
