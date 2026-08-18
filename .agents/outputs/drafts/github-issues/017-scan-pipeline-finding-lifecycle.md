> **Document:** Issue Draft 017 — Implement Snapshot and Git History Scan Pipeline with Finding Lifecycle
> **File:** `.agents/outputs/drafts/github-issues/017-scan-pipeline-finding-lifecycle.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Scan][FR-002][FR-007][FR-018][FR-025][FR-051] Implement Snapshot and Git History Scan Pipeline with Finding Lifecycle

## Tóm tắt

Xây dựng luồng xử lý quét bất đồng bộ (Scan Pipeline) và động cơ quản lý vòng đời phát hiện (Finding Lifecycle Engine). Luồng quét thực hiện lấy snapshot commit bất biến vào không gian làm việc tạm thời, thực hiện quét HEAD snapshot trước để phát hiện ngay rủi ro hiện tại, tiếp theo quét toàn bộ lịch sử Git có thể tiếp cận (Git history baseline). Quản lý chính xác chuyển đổi trạng thái của Finding (`OPEN`, `RESOLVED`, `REGRESSED`) kết hợp với chất lượng khắc phục (`ACTION_REQUIRED`, `RISK_CONTAINED`, `VERIFIED_COMPLETE`).

## Source Trace

- Requirements: `FR-002` — Create and process scan jobs for selected repositories.
- Requirements: `FR-003` — Manual and event-driven scan triggers.
- Requirements: `FR-007` — Re-scanning distinguishes `OPEN`, `RESOLVED`, and `REGRESSED`.
- Requirements: `FR-016` — Immutable source snapshot in disposable workspace; no repository alteration.
- Requirements: `FR-018` — Finding records lifecycle separately from remediation quality (`ACTION_REQUIRED`, `RISK_CONTAINED`, `VERIFIED_COMPLETE`).
- Requirements: `FR-019` — Clean current-source re-scan moves finding to `RESOLVED` + `RISK_CONTAINED`; clean history re-scan reaches `VERIFIED_COMPLETE`.
- Requirements: `FR-025` — HEAD snapshot scanned first, followed by reachable history newer commits before older commits.
- Requirements: `FR-027` — Incremental scans only from ancestor checkpoints; full re-baseline on history rewrite.
- Requirements: `FR-028`, `FR-029` — Validated coverage records required before advancing checkpoints.
- Requirements: `FR-051` — 3-stage demonstration story (`ACTION_REQUIRED` -> `RISK_CONTAINED` -> `VERIFIED_COMPLETE`).
- Decisions: `DEC-012` — Preserve `REGRESSED` state.
- Decisions: `DEC-040`, `DEC-041` — HEAD snapshot first, Git history baseline, checkpoint advancement.
- Decisions: `DEC-050` — Finding tracking and lifecycle state model.

## Mục tiêu

- Cung cấp `ScanOrchestrationService` điều phối toàn bộ chu trình quét:
  1. Tạo `ScanJob` (`status = PENDING`) và chuyển sang `RUNNING`.
  2. Clone / fetch mã nguồn từ GitHub vào `DisposableMutableWorkspace` (thư mục tạm thời cô lập) sử dụng GitHub App installation token ngắn hạn.
  3. Lấy thông tin commit HEAD hiện tại để đảm bảo tính bất biến.
  4. Chạy Content Classifier & File Eligibility (Issue 013) để lọc các tệp đủ điều kiện và sinh coverage items.
  5. Kích hoạt Gitleaks Adapter (Issue 014) quét snapshot HEAD trước, sau đó quét dải commit lịch sử Git (`git log` reachable history).
  6. Áp dụng Secret Fingerprinting & Redaction (Issue 015) để chuẩn hóa các phát hiện.
  7. Thực hiện đối chiếu Finding Lifecycle: cập nhật trạng thái `OPEN`, `RESOLVED`, `REGRESSED` và đánh giá remediation quality.
  8. Lưu trữ kết quả, cập nhật checkpoint nếu đạt chuẩn coverage, và tiêu hủy sạch thư mục tạm thời.
  9. Chuyển `ScanJob` sang `COMPLETED` (hoặc `FAILED` nếu có lỗi).

## Phạm vi

- Triển khai `GitWorkspaceManager` quản lý clone an toàn bằng JGit hoặc tiến trình Git, xóa sạch workspace sau khi hoàn thành.
- Triển khai `FindingLifecycleEngine`:
  - Secret xuất hiện ở HEAD hiện tại -> Trạng thái `OPEN`, Remediation Quality `ACTION_REQUIRED`.
  - Re-scan thấy secret đã biến mất khỏi HEAD (mã nguồn đã sửa) nhưng vẫn còn tồn tại trong commit lịch sử cũ -> Trạng thái `RESOLVED`, Remediation Quality `RISK_CONTAINED`.
  - Re-scan sau khi repository đã thanh lọc lịch sử Git (clean history rewrite) và sạch cả ở HEAD -> Trạng thái `RESOLVED`, Remediation Quality `VERIFIED_COMPLETE`.
  - Secret từng được đánh dấu `RESOLVED` nhưng xuất hiện trở lại ở commit mới -> Trạng thái `REGRESSED`, Remediation Quality `ACTION_REQUIRED`.
- Cơ chế quản lý Checkpoint: kiểm tra tính tương thích tổ tiên (`isAncestor`) trước khi cho phép quét gia tăng (incremental scan); nếu lịch sử bị rewrite/force-push thì kích hoạt quét lại toàn bộ baseline (full re-baseline).

## Không nằm trong phạm vi

- Không tự động sửa mã nguồn, không commit/push hay rewrite git history của người dùng (theo `DEC-048`).
- Không kiểm tra live credential trực tiếp với bên thứ ba (theo `FR-030`).

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Zero-commit scan hoặc lỗi thực thi scanner không bao giờ được coi là bằng chứng rằng repository đã sạch hoặc Finding đã được sửa (theo `FR-028`, `Inspection Requirements`).
- Remediation quality áp dụng độc lập cho từng Finding và không được coi là chứng nhận an toàn toàn diện cho cả dự án (theo `Inspection Requirements`).

## Implementation Notes

- Xử lý bất đồng bộ sử dụng Spring `@Async` hoặc `TaskExecutor` với bounded thread pool để không làm treo REST API thread.
- Các bước thực thi được bao bọc trong `try-finally` để đảm bảo tài nguyên tạm thời luôn được giải phóng kể cả khi gặp sự cố đột ngột.

## Acceptance Criteria

- [ ] Job quét thực thi bất đồng bộ thành công trên repository mẫu có chứa secret.
- [ ] Giai đoạn 1 (Snapshot HEAD) phát hiện secret hiện tại và lưu Finding với `OPEN / ACTION_REQUIRED`.
- [ ] Giai đoạn 2 (History Baseline) quét sâu vào các commit cũ và gắn các occurrences tương ứng.
- [ ] Re-scan sau khi commit xóa secret ở HEAD: Finding chuyển sang `RESOLVED / RISK_CONTAINED`.
- [ ] Re-scan sau khi rewrite Git history sạch: Finding chuyển sang `RESOLVED / VERIFIED_COMPLETE`.
- [ ] Re-scan khi thêm lại secret: Finding chuyển sang `REGRESSED / ACTION_REQUIRED`.
- [ ] Thư mục làm việc tạm thời chứa mã nguồn được xóa sạch 100% sau khi quét xong.
- [ ] Checkpoint chỉ được ghi nhận khi scan hoàn tất với đầy đủ bằng chứng coverage hợp lệ.

## Project Metadata

- Type: Feature
- Size: L
- Story Points: 8
- Estimation Reason: Vertical slice lớn nhất và phức tạp nhất của Submission MVP; kết hợp Git clone, 2 giai đoạn quét, đối chiếu trạng thái lifecycle đa chiều, checkpoint validation và xử lý bất đồng bộ.
- Priority: Critical
- Priority Reason: Động cơ xử lý nghiệp vụ trung tâm của toàn bộ sản phẩm Scan Pilot.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🛠️ Backend`
- `🚨 Critical`

## Relationships

- Parent: None
- Blocked by: 012, 013, 014, 015, 016
- Blocking: 018, 019, 020
- Security alert: None

## Suggested Branch

`codex/17-scan-pipeline-finding-lifecycle`

## Ghi chú cho người thực hiện

- Cấu hình timeout hợp lý cho mỗi scan job (ví dụ tối đa 5 phút cho submission MVP) để tránh treo worker.
- Đảm bảo installation token được truyền qua HTTPS/stdin an toàn khi clone, không để lộ token trong URL command line hay process table.
