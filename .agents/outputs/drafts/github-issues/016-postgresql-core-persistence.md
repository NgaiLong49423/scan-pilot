> **Document:** Issue Draft 016 — Implement PostgreSQL Schema and Repositories for Scan Pilot Core Entities
> **File:** `.agents/outputs/drafts/github-issues/016-postgresql-core-persistence.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Database][FR-010][FR-011][FR-014] Implement PostgreSQL Schema and Repositories for Scan Pilot Core Entities

## Tóm tắt

Thiết kế và triển khai cơ sở dữ liệu PostgreSQL hoàn chỉnh cho Scan Pilot bằng Flyway migration script và Spring Data JPA Repositories. Quản lý toàn bộ vòng đời thực thể: User & Session, Project / Repository Profile, Scan Job & Checkpoint, Finding & Evidence Item, Coverage Record và Review Request theo đúng mô hình dữ liệu chuẩn của dự án.

## Source Trace

- Requirements: `FR-010` — Project Discovery and persistent Repository Profile with source attribution and verification status.
- Requirements: `FR-011` — Scan checkpoints and finding history for continuous monitoring.
- Requirements: `FR-014` — Persist typed, scoped, attributable Evidence Items.
- Requirements: `FR-015` — Finding tracking model combining identity, Git diff, and evidence locations.
- Decisions: `DEC-006` — Core technology stack: PostgreSQL.
- Decisions: `DEC-039` — Structured PostgreSQL state for profiles, checkpoints, and findings.
- Specifications: `docs/EVIDENCE-MODEL.md` — Evidence types (`Technical Evidence`, `User Assertion`, `AI Inference`) and verification statuses.
- Specifications: `docs/FINDING-TRACKING.md` — Hybrid finding tracking schema and location mapping.

## Mục tiêu

- Tạo các bảng dữ liệu có cấu trúc chuẩn hóa, có quan hệ khóa ngoại (foreign key), ràng buộc toàn vẹn (constraints) và chỉ mục (indexes) tối ưu.
- Quản lý phiên bản database schema bằng Flyway migrations (`V1__init_schema.sql`).
- Cung cấp các JPA Entities và Spring Data Repositories tương ứng trong backend Spring Boot.
- Đảm bảo hỗ trợ đầy đủ các trạng thái nghiệp vụ:
  - Scan Job: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`.
  - Finding Lifecycle: `OPEN`, `RESOLVED`, `REGRESSED`.
  - Remediation Quality: `ACTION_REQUIRED`, `RISK_CONTAINED`, `VERIFIED_COMPLETE`, `NOT_ASSESSED`.
  - Evidence Type: `TECHNICAL_EVIDENCE`, `USER_ASSERTION`, `AI_INFERENCE`.

## Phạm vi

- Tạo Flyway script khởi tạo các bảng:
  - `users` & `user_sessions`: Lưu trữ thông tin tài khoản GitHub và phiên đăng nhập.
  - `repositories` & `monitored_branches`: Lưu trữ profile repo, installation ID, nhánh `PRIMARY` và nhánh phụ `SECONDARY`.
  - `scan_jobs`: Quản lý tác vụ quét, chế độ (`SNAPSHOT`, `FULL_BASELINE`, `INCREMENTAL`), commit SHA, thời gian và telemetry.
  - `scan_checkpoints`: Lưu trữ điểm mốc commit đã được xác minh coverage của từng nhánh.
  - `findings`: Lưu trữ định danh phát hiện, `rule_id`, `fingerprint` (SP_SECRET_FP_V1), mức độ nghiêm trọng, lifecycle state, remediation quality.
  - `finding_occurrences` / `finding_locations`: Lưu trữ vị trí xuất hiện (file_path, line range, commit_sha, is_current_head).
  - `evidence_items`: Lưu trữ bằng chứng chuẩn hóa, trích dẫn code redacted, claims và trạng thái kiểm chứng.
  - `coverage_records` & `coverage_items`: Lưu trữ báo cáo tệp được quét hoặc bị bỏ qua (`SKIPPED`) kèm mã lý do.
  - `review_requests`: Quản lý yêu cầu review không chặn luồng quét.
- Viết JPA Entities, Mappers và Repositories với các custom query cần thiết (truy vấn findings theo repo, lọc theo status/severity, tìm checkpoint mới nhất, v.v.).
- Viết Integration test với Testcontainers (PostgreSQL container) để xác minh tính tương thích của schema.

## Không nằm trong phạm vi

- Không lưu trữ tệp mã nguồn thô hoặc raw secret vào cơ sở dữ liệu.
- Không tự động thay đổi dữ liệu mà không thông qua business service layer.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Fingerprint của finding là duy nhất trong cùng một repository (`UNIQUE(repository_id, fingerprint)`).
- Không xóa cứng (hard delete) dữ liệu lịch sử quét, findings hay evidence khi có thay đổi nhánh default hoặc re-scan (theo `FR-023`).
- Dữ liệu `evidence_items` mang tính append-oriented để bảo toàn tính minh bạch trong kiểm toán bảo mật (theo `docs/EVIDENCE-MODEL.md`).

## Implementation Notes

- Sử dụng kiểu dữ liệu `UUID` cho primary keys của các thực thể chính để đảm bảo an toàn phân tán.
- Cột `fingerprint` lưu `VARCHAR(64)` cho SHA-256 hex digest.
- Cột timestamps sử dụng `TIMESTAMPTZ` (UTC).

## Acceptance Criteria

- [ ] Flyway migration `V1__init_schema.sql` thực thi thành công không lỗi trên PostgreSQL 15/16.
- [ ] Mọi bảng đều có đầy đủ index cho các trường thường xuyên query (`repository_id`, `fingerprint`, `job_id`, `status`).
- [ ] Toàn bộ Spring Data JPA Repositories (`UserRepository`, `RepositoryProfileRepository`, `ScanJobRepository`, `FindingRepository`, `EvidenceItemRepository`, `CheckpointRepository`, v.v.) hoạt động chính xác.
- [ ] Integration tests sử dụng Testcontainers PostgreSQL chạy pass 100% các thao tác CRUD và transactional updates.
- [ ] Không có trường nào lưu raw secret hoặc token không mã hóa.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Thiết kế schema quan hệ toàn diện cho 9 bảng chính, Flyway scripts, JPA mappings, constraint validations và Testcontainers integration tests.
- Priority: High
- Priority Reason: Tầng lưu trữ cốt lõi là nền móng cho toàn bộ hoạt động của Scan Pipeline, Finding Lifecycle và API.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🗄️ Database`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: 011, 012, 015
- Blocking: 017, 019
- Security alert: None

## Suggested Branch

`codex/16-postgresql-core-persistence`

## Ghi chú cho người thực hiện

- Đảm bảo cấu hình `application.yml` hỗ trợ cả profile local (`H2` hoặc `PostgreSQL`) và profile production (`Cloud SQL / PostgreSQL`).
- Chú ý cấu hình `hibernate.ddl-auto=validate` khi chạy với Flyway để tránh Hibernate tự động sửa schema ngoài ý muốn.
