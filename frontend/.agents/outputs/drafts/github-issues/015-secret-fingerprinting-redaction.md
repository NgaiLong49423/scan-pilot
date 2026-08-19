> **Document:** Issue Draft 015 — Implement SP_SECRET_FP_V1 HMAC-SHA-256 Fingerprinting and Redaction Engine
> **File:** `.agents/outputs/drafts/github-issues/015-secret-fingerprinting-redaction.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Security][FR-017] Implement SP_SECRET_FP_V1 HMAC-SHA-256 Fingerprinting and Redaction Engine

## Tóm tắt

Xây dựng module mã hóa băm dấu vết `SP_SECRET_FP_V1` bằng HMAC-SHA-256 có phạm vi theo repository (repository-scoped) và công cụ che giấu/làm mờ bí mật (Redaction Engine). Đảm bảo mỗi phát hiện secret đều có một định danh băm tất định để theo dõi qua các commit và vị trí tệp khác nhau mà tuyệt đối không lưu trữ, không ghi log, không hiển thị và không gửi chuỗi secret thô sang AI hay cơ sở dữ liệu.

## Source Trace

- Requirements: `FR-017` — Create repository-scoped `SP_SECRET_FP_V1` HMAC-SHA-256 fingerprint from canonical identity and exact secret bytes without persisting secret.
- Requirements: `FR-004` — Normalized findings with safe evidence, location, and severity.
- Security Requirements: `docs/REQUIREMENTS.md` — Zero secret exposure; HMAC key remains inside trusted memory boundary, outside PostgreSQL, source code, and logs.
- Decisions: `DEC-038` — Repository-scoped `SP_SECRET_FP_V1` HMAC-SHA-256 fingerprinting with key versioning.
- Decisions: `DEC-048` — Gemini receives only normalized secret-redacted evidence.
- Specifications: `docs/INSPECTION-SPEC.md` — Mandatory Safety Rules for `SP-CONFIG-001`.

## Mục tiêu

- Cung cấp `SecretFingerprintService` tính toán fingerprint chuẩn `SP_SECRET_FP_V1`:
  - Input: `repository_id` (hoặc repo UUID/canonical URL), `rule_id`, `secret_raw_bytes`, và hệ thống salt/HMAC key bí mật.
  - Thuật toán: HMAC-SHA-256 với canonical string định dạng chuẩn `v1|repo_id|rule_id|secret_bytes`.
  - Output: Chuỗi hex digest 64 ký tự duy nhất cho mỗi secret trong phạm vi repository đó.
- Cung cấp `SecretRedactionEngine` chịu trách nhiệm:
  - Sinh chuỗi preview an toàn (ví dụ: `AIzaSy*******************************3x7Q` cho Google API Key hoặc `ghp_********************************4a2Z` cho GitHub Token).
  - Che giấu toàn bộ chuỗi secret trong đoạn trích mã nguồn (code snippet context) trước khi tạo `EvidenceItem`.
  - Làm sạch các chuỗi secret tiềm năng trong các thông điệp commit, log scanner và Review Request content.
- Đảm bảo tính bất biến: cùng một secret trong cùng một repo sẽ luôn sinh ra cùng một fingerprint (phục vụ theo dõi vòng đời lifecycle ở Issue 017), nhưng khác repo sẽ sinh ra fingerprint khác nhau (chống tương quan chéo không mong muốn).

## Phạm vi

- Triển khai `SecretFingerprintService` với cấu hình khóa HMAC nạp từ biến môi trường (`SCANPILOT_HMAC_SECRET_KEY`).
- Triển khai `SecretRedactionService` với các quy tắc làm mờ theo họ secret (Google Key, GitHub PAT, AWS Key, Generic Secret).
- Xây dựng tiện ích làm sạch code snippet: thay thế secret bằng chuỗi placeholder dạng `[REDACTED_SECRET:<fingerprint_prefix>]` hoặc mask an toàn.
- Kiểm tra validation: đảm bảo không bao giờ có secret thô lọt vào các DTO persisted hoặc DTO gửi ra ngoài REST API.

## Không nằm trong phạm vi

- Không lưu trữ HMAC Secret Key vào database PostgreSQL.
- Không hỗ trợ đảo ngược fingerprint (HMAC là hàm băm một chiều).
- Không thực hiện so khớp ngữ nghĩa đa tệp (thuộc trách nhiệm của Scan Pipeline).

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Khóa HMAC phải được giữ tuyệt đối trong bộ nhớ tiến trình backend đáng tin cậy.
- Chuỗi preview an toàn chỉ được giữ lại tối đa 4-6 ký tự đầu (nếu là tiền tố công khai đã biết như `AIzaSy`, `ghp_`) và tối đa 3-4 ký tự cuối để người dùng nhận diện credential của họ, phần giữa bắt buộc phải che bằng dấu sao `*`.

## Implementation Notes

- Sử dụng `javax.crypto.Mac` với thuật toán `HmacSHA256` chuẩn của Java Cryptography Architecture (JCA).
- Đảm bảo canonical input string được encode UTF-8 đồng nhất và xử lý khoảng trắng chuẩn xác trước khi băm.

## Acceptance Criteria

- [ ] `SecretFingerprintService` sinh ra fingerprint 64-char hex nhất quán cho cùng một cặp (repo, rule, secret).
- [ ] Khác repository ID sinh ra 2 fingerprint hoàn toàn khác nhau cho cùng một secret.
- [ ] Khác secret sinh ra fingerprint khác nhau, không xảy ra xung đột.
- [ ] `SecretRedactionEngine` làm mờ chính xác các loại token phổ biến, giữ lại prefix hợp lệ và che dấu an toàn phần nội dung nhạy cảm.
- [ ] Code snippet chứa secret được thay thế hoàn toàn phần nhạy cảm bằng masked text.
- [ ] Đạt 100% unit test coverage với các bộ dữ liệu secret mẫu; không in bất kỳ raw secret nào ra console hay test log.

## Project Metadata

- Type: Feature
- Size: S
- Story Points: 3
- Estimation Reason: Xử lý mật mã học HMAC-SHA-256 tiêu chuẩn, logic masking chuỗi theo pattern, rủi ro thấp nhưng yêu cầu độ chính xác và an toàn tuyệt đối.
- Priority: High
- Priority Reason: Thành phần bảo mật cốt lõi để bảo vệ dữ liệu người dùng và tạo khóa định danh cho Finding Tracking.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🔒 Security`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: None (Issue #9 foundation is merged)
- Blocking: 016, 017, 018
- Security alert: None

## Suggested Branch

`codex/15-secret-fingerprinting-redaction`

## Ghi chú cho người thực hiện

- Kiểm tra trường hợp biến môi trường `SCANPILOT_HMAC_SECRET_KEY` chưa được set: backend phải từ chối khởi động hoặc fail-fast thay vì fallback về một key rỗng không an toàn trong môi trường production.
