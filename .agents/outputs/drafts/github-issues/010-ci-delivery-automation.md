> **Document:** Issue Draft 010 — Implement Continuous Integration Workflow for Frontend and Backend
> **File:** `.agents/outputs/drafts/github-issues/010-ci-delivery-automation.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [CI][Automation] Implement Continuous Integration Workflow for Frontend and Backend

## Tóm tắt

Xây dựng workflow GitHub Actions CI cho repository `scan-pilot` nhằm tự động hóa kiểm tra tính toàn vẹn của mã nguồn trên Pull Request và nhánh `main`. Workflow bao gồm lint/typecheck/build cho frontend (React + TypeScript + Vite) và compile/test/package cho backend (Spring Boot 3 + Java 21 + Maven).

## Source Trace

- Delivery Automation Policy: `AGENTS.md` — CI first; required checks after green evidence; CD deferred.
- Delivery Governance: `DEC-055` — hybrid agent delivery governance under `FULL_TRACKED`.
- Current Status: `docs/CURRENT-STATUS.md` — Next Logical Task: separate Issue for the first CI workflow.
- Requirements: `FR-036` — Java 21 and Spring Boot 3 backend uses Apache Maven.
- Architecture: `docs/ARCHITECTURE.md` — modular monolith layout (`frontend/` & `backend/`).

## Mục tiêu

- Thiết lập pipeline GitHub Actions tự động kích hoạt khi có Pull Request trỏ vào `main` hoặc push trực tiếp vào `main`.
- Đảm bảo mọi thay đổi mã nguồn phía frontend đều được kiểm tra linting (`eslint`), TypeScript typechecking (`tsc`), và production build (`vite build`).
- Đảm bảo mọi thay đổi mã nguồn phía backend đều được kiểm tra biên dịch (`javac` qua Java 21), chạy unit/integration test (`mvn test`), và verify package (`mvn package`).
- Báo cáo kết quả kiểm tra rõ ràng trên giao diện GitHub PR để phục vụ Codex technical review theo quy trình `FULL_TRACKED`.

## Phạm vi

- Tạo file workflow `.github/workflows/ci.yml` cấu hình matrix hoặc multi-job pipeline cho `frontend` và `backend`.
- Thiết lập caching hiệu quả (npm cache và maven `.m2` repository cache) nhằm tối ưu thời gian chạy pipeline.
- Cấu hình chạy trên môi trường chuẩn `ubuntu-latest`.
- Kiểm tra xử lý path filtering để chạy job tương ứng khi có thay đổi trong `frontend/**`, `backend/**`, hoặc các file cấu hình gốc.
- Đảm bảo CI chạy thành công trên commit hiện tại của nhánh `main`.

## Không nằm trong phạm vi

- Không cấu hình Continuous Deployment (CD) hoặc tự động deploy lên Google Cloud Run.
- Không cấu hình Branch Protection Rule bắt buộc (Required Status Check) trên GitHub settings ngay lập tức (sẽ được bật thủ công bởi PO sau khi CI chứng minh ổn định).
- Không nhúng secret, token thật hoặc credential vào workflow.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Workflow phải fail-fast nếu có lỗi cú pháp, compile error, type error hoặc unit test thất bại.
- Build job phải độc lập, không dựa vào cache không xác định hoặc local state của máy lập trình viên.
- Không tạo dependency vòng hoặc trigger lặp vô tận.

## Implementation Notes

Tài liệu `AGENTS.md` và `docs/CURRENT-STATUS.md` quy định rõ:
1. CI là bước tự động hóa phân phối đầu tiên.
2. Java version cố định là Java 21 (Eclipse Temurin hoặc Temurin distribution).
3. Backend dùng Maven Wrapper (`mvnw`) nếu có hoặc Maven 3.9+ tiêu chuẩn.
4. Node version cho frontend là Node 20 LTS.
5. Chưa cấu hình CD hoặc trigger deploy khi PR merge.

## Acceptance Criteria

- [ ] File `.github/workflows/ci.yml` được tạo với cấu trúc rõ ràng, chia job `frontend` và `backend` hợp lý.
- [ ] Job frontend chạy: `npm ci`, `npm run lint` (hoặc typecheck), và `npm run build` thành công trong thư mục `frontend/`.
- [ ] Job backend chạy: `mvn clean test` (hoặc `mvn verify`) thành công trên Java 21 trong thư mục `backend/`.
- [ ] Workflow được trigger đúng khi có PR vào `main` và khi push vào `main`.
- [ ] Tích hợp cache cho npm dependencies và Maven artifacts giúp giảm thời gian build.
- [ ] Pipeline chạy thành công (green) trên mã nguồn hiện tại của repository.

## Project Metadata

- Type: Task
- Size: S
- Story Points: 3
- Estimation Reason: Phạm vi công việc rõ ràng, tập trung vào cấu hình GitHub Actions cho 2 thư mục đã có build script hoàn chỉnh từ Issue #9; rủi ro thấp.
- Priority: High
- Priority Reason: CI là điều kiện tiên quyết bắt buộc trước khi triển khai các vertical slice tiếp theo theo quy trình `FULL_TRACKED`.
- Start Date: TBD
- Target Date: TBD

## Labels

- `📋 Task`
- `🛠️ Backend`
- `🎨 Frontend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: None (Issue #9 foundation is merged)
- Blocking: 011, 012, 013, 014, 015, 016, 017, 018, 019, 020
- Security alert: None

## Suggested Branch

`codex/10-ci-delivery-automation`

## Ghi chú cho người thực hiện

- Tuân thủ quy tắc bảo mật: không sử dụng action của bên thứ ba không rõ nguồn gốc.
- Kiểm tra tính tương thích của đường dẫn tương đối giữa root repository và thư mục con `frontend/`, `backend/`.
- Đảm bảo CI không bị nghẽn do thiếu memory trên runner.
