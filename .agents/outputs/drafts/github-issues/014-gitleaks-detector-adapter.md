> **Document:** Issue Draft 014 — Implement Gitleaks Detector Adapter with Trusted SP-CONFIG-001 Policy
> **File:** `.agents/outputs/drafts/github-issues/014-gitleaks-detector-adapter.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Detector][FR-009][FR-026][FR-038] Implement Gitleaks Detector Adapter with Trusted SP-CONFIG-001 Policy

## Tóm tắt

Triển khai module Gitleaks Detector Adapter đóng vai trò cầu nối độc quyền giữa Scan Pilot và công cụ dò tìm Gitleaks. Adapter thực thi Gitleaks theo cấu hình chính sách chuẩn hóa `SP-CONFIG-001` do Scan Pilot kiểm soát, vô hiệu hóa hoàn toàn khả năng repository tự ý ghi đè hoặc làm suy yếu chính sách (bỏ qua `.gitleaks.toml`, `.gitleaksignore`, và `gitleaks:allow` nội bộ của repo target), và cô lập, xóa sạch báo cáo thô nhạy cảm ngay sau khi trích xuất kết quả.

## Source Trace

- Requirements: `FR-009` — Implement `SP-CONFIG-001` against real repository content.
- Requirements: `FR-026` — Gitleaks behind a Scan Pilot adapter; Scan Pilot owns normalization, redaction, and lifecycle.
- Requirements: `FR-038` — Pinned trusted policy; repository config/ignore cannot silently suppress detection.
- Security Requirements: `docs/REQUIREMENTS.md` — Raw Gitleaks reports are sensitive temporary artifacts; zero secret exposure.
- Decisions: `DEC-037` — Pinned Gitleaks detection policy owned by Scan Pilot.
- Decisions: `DEC-046` — Submission MVP vertical slice: current snapshot & reachable history secret scan.
- Decisions: `DEC-053` — One strong MUST rule `SP-CONFIG-001` covering multiple secret families.
- Specifications: `docs/INSPECTION-SPEC.md` — Rule contract for `SP-CONFIG-001`.

## Mục tiêu

- Cung cấp `GitleaksDetectorAdapter` có khả năng kích hoạt Gitleaks binary đối với một snapshot thư mục (directory scan) hoặc dải commit Git (git log / git history scan).
- Đóng gói file cấu hình chuẩn `sp-config-001-gitleaks.toml` bên trong Scan Pilot chứa các rule nhận diện quan trọng:
  - Google API Key / Gemini API Key (phù hợp với tiêu chuẩn submission Google).
  - GitHub Personal Access Token / OAuth Token / App Token.
  - AWS Access Key / Secret Key.
  - Generic Private Key (RSA, OpenSSH, EC, PGP).
  - High-entropy secret patterns và token phổ biến.
- Bắt buộc Gitleaks sử dụng cấu hình do Scan Pilot chỉ định thông qua cờ `--config`, không đọc cấu hình mặc định của repository đích.
- Phân tích cú pháp (parse) output JSON của Gitleaks sang các đối tượng phát hiện trung gian (Raw Finding DTO).
- Thực hiện cơ chế tiêu hủy an toàn (secure deletion) đối với tệp báo cáo JSON thô ngay sau khi parse xong, đảm bảo không lưu tệp nhạy cảm trong hệ thống tệp tạm thời.

## Phạm vi

- Tích hợp và quản lý Gitleaks binary (hoặc thực thi process với arguments được kiểm soát chặt chẽ).
- Định nghĩa file cấu hình TOML chuẩn cho `SP-CONFIG-001` được kiểm thử kỹ lưỡng.
- Xử lý các cờ thực thi an toàn: `--no-git` (cho snapshot scan), `--log-opts` (cho commit range scan), `--report-path`, `--report-format=json`, `--redact=false` (để adapter lấy raw secret chuyển ngay cho fingerprinting trước khi tiêu hủy).
- Map các trường từ Gitleaks output (`RuleID`, `Description`, `StartLine`, `EndLine`, `StartColumn`, `EndColumn`, `Commit`, `Author`, `Email`, `Date`, `Message`, `File`, `Secret`, `Match`) sang mô hình trung gian nội bộ.
- Xử lý mã thoát (exit code) của Gitleaks: `0` (không phát hiện secret), `1` (có secret), các mã lỗi khác (lỗi thực thi, corrupt repository, timeout).

## Không nằm trong phạm vi

- Không gửi trực tiếp Raw Secret sang cơ sở dữ liệu hay log (nhiệm vụ của Fingerprinting & Redaction ở Issue 015).
- Không tự động kiểm tra tính sống/chết của credential với nhà cung cấp (theo `FR-030`).
- Không thực hiện các rule ngoài `SP-CONFIG-001` trong giai đoạn này.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Repository đích được coi là untrusted input; không một file cấu hình hoặc chú thích nào trong repo được phép làm sai lệch kết quả đánh giá (theo `FR-038`).
- Báo cáo JSON thô là dữ liệu nhạy cảm có thời hạn ngắn; phải được xóa ngay cả khi có ngoại lệ xảy ra (sử dụng khối `try-finally`).

## Implementation Notes

- Phiên bản Gitleaks được ghim (pinned version) cố định (ví dụ v8.18.x hoặc mới nhất ổn định).
- Cấu hình custom TOML phải chứa rule entropy và keyword context hợp lý để giảm thiểu false positive trên mock/example data mà không bỏ sót key thật.
- Xử lý đường dẫn tệp tương đối so với root repository, tránh path traversal vulnerability.

## Acceptance Criteria

- [ ] `GitleaksDetectorAdapter` thực thi thành công Gitleaks trên snapshot thư mục và dải commit Git giả lập.
- [ ] Cấu hình `sp-config-001-gitleaks.toml` nhận diện chính xác các secret mẫu (Google API Key, GitHub PAT, AWS Key, RSA Private Key).
- [ ] Gitleaks bị chặn không đọc `.gitleaks.toml` hoặc `.gitleaksignore` trong repository được quét.
- [ ] File output JSON thô được xóa sạch sau khi adapter hoàn thành việc trích xuất DTO.
- [ ] Bắt và xử lý đúng mọi exit code (`0`, `1`, error codes) kèm thông báo telemetry có cấu trúc.
- [ ] Đầy đủ unit tests với các fixture repository mẫu, không để rò rỉ secret ra output stream của test runner.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Tích hợp process thực thi bên ngoài an toàn, xây dựng và tinh chỉnh tập rule TOML chuẩn, parse JSON DTO, cơ chế dọn dẹp file nhạy cảm và unit tests.
- Priority: High
- Priority Reason: Module thực thi việc dò tìm cốt lõi của quy tắc `SP-CONFIG-001`.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🔒 Security`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: 013 (Content Classifier & Eligibility)
- Blocking: 015, 017
- Security alert: None

## Suggested Branch

`codex/14-gitleaks-detector-adapter`

## Ghi chú cho người thực hiện

- Kiểm tra quyền thực thi binary (chạy trên Linux container runner cũng như Windows local test).
- Sử dụng temporary directory cô lập cho mỗi lần scan và xóa toàn bộ thư mục tạm khi kết thúc job.
