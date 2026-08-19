> **Document:** Issue Draft 013 — Implement Layered Content Classifier and File Eligibility Policy
> **File:** `.agents/outputs/drafts/github-issues/013-content-classifier-file-eligibility.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Scanner][FR-031][FR-035][FR-037] Implement Layered Content Classifier and File Eligibility Policy

## Tóm tắt

Xây dựng module phân loại nội dung nhiều lớp (Layered Content Classifier) và chính sách xác định tính hợp lệ của tệp quét (File Eligibility Policy). Module phân loại mọi tệp tin trong repository thành `TEXT`, `BINARY`, hoặc `UNDETERMINED` dựa trên Git object kind, signature nội dung, tín hiệu byte giới hạn, cùng với extension/.gitattributes làm gợi ý bổ trợ. Áp dụng hạn mức kích thước tệp (10 MiB cho Continuous Monitoring, 50 MiB cho Release Assessment) và tạo bản ghi Coverage Record có cấu trúc cho mọi trường hợp bị bỏ qua (`SKIPPED`).

## Source Trace

- Requirements: `FR-031` — Consider all Git-tracked items, scan supported eligible content, record structured skip reasons.
- Requirements: `FR-034` — Inventory binary documents (PDF/Office) and skip them with `UNSUPPORTED_BINARY_DOCUMENT` without internal extraction.
- Requirements: `FR-035` — Layered content classification (`TEXT`, `BINARY`, `UNDETERMINED`); persistent coverage records for all skips.
- Requirements: `FR-037` — Size limits: 10 MiB for Continuous Monitoring, 50 MiB hard ceiling for Release Assessment.
- Decisions: `DEC-034` — Binary document extraction deferred; inventory only.
- Decisions: `DEC-035` — Layered content classification and persistent coverage records.
- Decisions: `DEC-036` — Two-tier full-file size limit: 10 MiB monitoring, 50 MiB release ceiling.
- Specifications: `docs/INSPECTION-SPEC.md` — Content eligibility and classification specification.

## Mục tiêu

- Cung cấp dịch vụ phân loại tệp độc lập, an toàn và có tính tất định cao trước khi đưa nội dung vào detector.
- Xác định trạng thái phân loại: `TEXT`, `BINARY`, hoặc `UNDETERMINED`.
- Nhận diện các định dạng tài liệu văn phòng / nhị phân phổ biến (PDF, DOCX, XLSX, PPTX) để gắn mã bỏ qua `UNSUPPORTED_BINARY_DOCUMENT` theo `FR-034`.
- Áp dụng các quy tắc trần kích thước tệp chính xác:
  - Tệp văn bản > 10 MiB trong chế độ Continuous Monitoring -> Bỏ qua với mã `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`.
  - Tệp văn bản > 50 MiB trong chế độ Release Assessment -> Bỏ qua với mã `RELEASE_FILE_SIZE_CEILING_EXCEEDED` và đánh dấu độ bao phủ `INCOMPLETE`.
- Xuất ra danh sách các bản ghi `CoverageItem` có cấu trúc: đường dẫn tệp, phân loại, kích thước, trạng thái (`CONSIDERED`, `SCANNED`, `SKIPPED`), mã lý do bỏ qua và mức độ ảnh hưởng bao phủ.

## Phạm vi

- Triển khai `ContentClassifierService` thực hiện thuật toán phân loại theo thứ tự:
  1. Kiểm tra Git object kind (blob, tree, commit, submodule, symlink).
  2. Kiểm tra magic bytes / signature nội dung (ví dụ: ELF, PE, Mach-O, ZIP header, PDF header, image signatures).
  3. Bounded byte sampling (quét mẫu 8KB đầu tiên để phát hiện ký tự null byte `0x00` hoặc tỉ lệ ký tự không in được cao).
  4. Đọc extension và `.gitattributes` như thông tin tham khảo bổ sung, không có quyền quyết định tuyệt đối.
  5. Trả về `UNDETERMINED` nếu có xung đột không thể giải quyết rõ ràng.
- Triển khai `FileEligibilityEngine` áp dụng policy chế độ quét (Continuous vs Release) và policy kích thước tệp.
- Tạo cấu trúc dữ liệu `CoverageRecord` và `CoverageItem` sẵn sàng cho việc lưu trữ PostgreSQL.

## Không nằm trong phạm vi

- Không giải mã / trích xuất nội dung bên trong tệp nhị phân (không sử dụng Apache Tika theo `DEC-034`).
- Không thực hiện quét phát hiện secret (chức năng của Gitleaks Adapter ở Issue 014).
- Không tự ý gửi tệp `UNDETERMINED` vào detector mà không có chính sách xử lý rõ ràng.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Log ứng dụng không được coi là source of truth cho các tệp bị bỏ qua; mọi tệp bị bỏ qua bắt buộc phải có bản ghi dữ liệu có cấu trúc (theo `FR-035`).
- Tệp `UNDETERMINED` không được âm thầm coi là an toàn hay đã quét sạch (theo `Inspection Requirements`).
- Phép tính byte: `1 MiB` = `1,048,576 bytes`.

## Implementation Notes

- Xử lý sampling byte an toàn với buffer cố định (ví dụ 8192 bytes) để không đọc toàn bộ tệp lớn vào bộ nhớ khi chỉ cần phân loại.
- Các reason codes chuẩn mực:
  - `UNSUPPORTED_BINARY_DOCUMENT`
  - `UNSUPPORTED_BINARY_FILE`
  - `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`
  - `RELEASE_FILE_SIZE_CEILING_EXCEEDED`
  - `UNDETERMINED_CONTENT_POLICY_SKIP`
  - `UNSUPPORTED_SPECIAL_OBJECT` (submodule, broken symlink)

## Acceptance Criteria

- [ ] `ContentClassifierService` phân loại chính xác các tệp test chuẩn: text files (Java, JS, JSON, YAML, Markdown), binary files (PNG, ZIP, EXE), Office documents (PDF, DOCX).
- [ ] Tệp có đuôi giả mạo (ví dụ: `.txt` chứa binary payload hoặc `.bin` chứa source text) được phân loại đúng dựa trên magic bytes và byte inspection.
- [ ] Tệp vượt quá 10 MiB bị từ chối trong chế độ Continuous với đúng mã lỗi `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`.
- [ ] Tệp vượt quá 50 MiB bị từ chối trong chế độ Release với đúng mã lỗi `RELEASE_FILE_SIZE_CEILING_EXCEEDED`.
- [ ] Mọi tệp xét duyệt đều sinh ra bản ghi `CoverageItem` đầy đủ metadata (path, classification, size, status, reason_code).
- [ ] Đạt 100% test coverage cho các case biên (boundary test 10 MiB ± 1 byte, empty file, non-UTF8 text).

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Thuật toán phân loại đa tầng, xử lý byte-sampling hiệu năng cao, boundary tests kích thước tệp và kiến trúc dữ liệu coverage record.
- Priority: High
- Priority Reason: Quyết định danh sách tệp đủ điều kiện đưa vào Gitleaks scan, đảm bảo tính toàn vẹn và trung thực của báo cáo coverage.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: None (Issue #9 foundation is merged)
- Blocking: 014, 017
- Security alert: None

## Suggested Branch

`codex/13-content-classifier-file-eligibility`

## Ghi chú cho người thực hiện

- Tránh load toàn bộ file 50 MiB vào bộ nhớ JVM; sử dụng `InputStream` và `FileChannel` có giới hạn buffer khi kiểm tra header.
- Tích hợp các test case với nhiều loại encoding (UTF-8, UTF-16, ISO-8859-1).
