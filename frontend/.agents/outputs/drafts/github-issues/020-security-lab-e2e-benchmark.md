> **Document:** Issue Draft 020 — Execute Security-Lab E2E Lifecycle Verification and Independent Secret Benchmark
> **File:** `.agents/outputs/drafts/github-issues/020-security-lab-e2e-benchmark.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Verification][FR-049][FR-050][FR-051] Execute Security-Lab E2E Lifecycle Verification and Independent Secret Benchmark

## Tóm tắt

Thiết lập kịch bản kiểm thử tích hợp đầu-cuối (E2E Integration Verification) trên một repository phòng thí nghiệm an ninh riêng biệt (Security-Lab target) và thực thi bộ đo điểm chuẩn phát hiện bí mật độc lập (Independent Safe Secret Benchmark). Xác minh trọn vẹn luồng trải nghiệm 3 giai đoạn của Scan Pilot: `OPEN / ACTION_REQUIRED` -> `RESOLVED / RISK_CONTAINED` -> `RESOLVED / VERIFIED_COMPLETE`, đồng thời xuất bản báo cáo độ chính xác của bộ phát hiện `SP-CONFIG-001`.

## Source Trace

- Requirements: `FR-049` — Source-attributed independent safe secret-detection benchmark evidence.
- Requirements: `FR-050` — End-to-end verification using separate user-owned security-lab repository with synthetic secrets.
- Requirements: `FR-051` — Submission core demonstrates 3-stage story (`ACTION_REQUIRED` -> `RISK_CONTAINED` -> `VERIFIED_COMPLETE`).
- Decisions: `DEC-049` — Independent secret-detection benchmark evidence.
- Decisions: `DEC-050` — Controlled security-lab repository with synthetic secret candidates and controlled Git history.
- Specifications: `docs/research/benchmarks/SECRET-DETECTION-VALIDATION.md` — Benchmark protocol and metrics contract.

## Mục tiêu

- Xây dựng hoặc cấu hình repository `scan-pilot-security-lab` chứa các mẫu synthetic secret (khóa giả lập an toàn, không có hiệu lực thật) thuộc các họ: Google API Key, GitHub Token, AWS Key.
- Tạo lịch sử commit Git có kiểm soát để kiểm thử tự động toàn bộ chu trình:
  1. Commit `c1`: Đưa secret vào mã nguồn -> Quét lần 1: Hệ thống phát hiện và gắn nhãn `OPEN / ACTION_REQUIRED`.
  2. Commit `c2`: Sửa mã nguồn ở HEAD (xóa secret khỏi code) -> Quét lần 2: Hệ thống ghi nhận khắc phục một phần, chuyển trạng thái sang `RESOLVED / RISK_CONTAINED` (do vẫn còn trong commit `c1`).
  3. Commit `c3` (sau khi git rebase/filter-repo làm sạch commit `c1`): Lịch sử Git hoàn toàn sạch -> Quét lần 3: Hệ thống xác minh toàn diện, nâng cấp trạng thái lên `RESOLVED / VERIFIED_COMPLETE`.
  4. Commit `c4`: Đưa secret cũ quay lại mã nguồn -> Quét lần 4: Hệ thống phát hiện tái xuất hiện, chuyển sang `REGRESSED / ACTION_REQUIRED`.
- Chạy bộ benchmark an toàn độc lập (Safe Secret Detection Benchmark Suite) đo lường:
  - Tỷ lệ phát hiện đúng (True Positives).
  - Tỷ lệ dương tính giả (False Positives).
  - Tỷ lệ bỏ sót (False Negatives).
- Xuất bản báo cáo kiểm thử hoàn chỉnh làm bằng chứng nộp bài (submission evidence).

## Phạm vi

- Tạo script kiểm thử E2E tự động hóa (End-to-End Test Suite) kích hoạt từ backend API hoặc Playwright/Cypress.
- Tạo dataset synthetic secrets có gán nhãn ground truth rõ ràng.
- Ghi lại nhật ký quét, dữ liệu telemetry, ảnh chụp bằng chứng (screenshots) và log xác minh lifecycle.
- Đóng gói tài liệu kết quả benchmark vào `docs/research/benchmarks/` theo đúng chuẩn lưu trữ của dự án.

## Không nằm trong phạm vi

- Không sử dụng credential thật còn hoạt động trên môi trường sản xuất (chỉ sử dụng synthetic non-functional secrets).
- Không quét vào các repository cá nhân khác ngoài phạm vi lab đã được phê duyệt.
- Không gửi dataset mật mã học nhạy cảm chưa được kiểm duyệt sang các API bên thứ ba.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Ground truth phải được lưu trữ độc lập bên ngoài repository mục tiêu quét (theo `FR-050`, `DEC-050`).
- Trạng thái `VERIFIED_COMPLETE` chỉ được công nhận khi toàn bộ lịch sử commit trong phạm vi quét đã được xác minh sạch và đầy đủ bằng chứng coverage hợp lệ (theo `Inspection Requirements`).

## Implementation Notes

- Script tạo repo lab tự động sử dụng `git init`, `git commit`, `git rebase` trong thư mục tạm để đảm bảo tính lặp lại (reproducibility).
- Báo cáo benchmark ghi rõ phiên bản Gitleaks được ghim, digest cấu hình TOML, thời gian thực thi và chi tiết từng mẫu kiểm thử.

## Acceptance Criteria

- [ ] Kịch bản 3 giai đoạn trên Security-Lab repository chạy thành công 100% từ giao diện và API.
- [ ] Giai đoạn 1 chuyển đổi đúng sang `OPEN / ACTION_REQUIRED`.
- [ ] Giai đoạn 2 chuyển đổi đúng sang `RESOLVED / RISK_CONTAINED`.
- [ ] Giai đoạn 3 chuyển đổi đúng sang `RESOLVED / VERIFIED_COMPLETE`.
- [ ] Kịch bản kiểm thử hồi quy (Regression test) chuyển đổi đúng sang `REGRESSED / ACTION_REQUIRED`.
- [ ] Báo cáo Benchmark độc lập được tạo với đầy đủ các chỉ số True Positive / False Positive / False Negative.
- [ ] Không có bất kỳ live credential thật nào xuất hiện trong quá trình kiểm thử hoặc báo cáo.

## Project Metadata

- Type: Testing
- Size: M
- Story Points: 5
- Estimation Reason: Xây dựng kịch bản kiểm thử E2E đa commit phức tạp, tự động hóa Git operations giả lập, đo lường benchmark độc lập và tổng hợp báo cáo bằng chứng.
- Priority: Critical
- Priority Reason: Bước xác minh tối hậu bảo đảm chất lượng và tính trung thực của toàn bộ câu chuyện sản phẩm trước khi hoàn tất nộp bài.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🧪 Testing`
- `🔒 Security`
- `🚨 Critical`

## Relationships

- Parent: None
- Blocked by: 017 (Scan Pipeline), 018 (Gemini Explanation), 019 (Frontend API Integration)
- Blocking: Final Submission Delivery
- Security alert: None

## Suggested Branch

`codex/20-security-lab-e2e-benchmark`

## Ghi chú cho người thực hiện

- Kiểm tra tính độc lập của ground truth: đảm bảo test runner không đọc cheat code hay hardcoded kết quả.
- Lưu trữ video demo hoặc animated GIF của chu trình E2E để phục vụ việc chuẩn bị tài liệu nộp bài.
