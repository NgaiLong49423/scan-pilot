> **Document:** Issue Draft 012 — Support GitHub App Installation Linking and Repository Selection
> **File:** `.agents/outputs/drafts/github-issues/012-github-app-linking-repo-selection.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [GitHub][FR-001][FR-020][FR-047] Support GitHub App Installation Linking and Repository Selection

## Tóm tắt

Triển khai chức năng liên kết GitHub App Installation với tài khoản người dùng đã đăng nhập, truy vấn danh sách repositories cá nhân được cấp quyền truy cập, và cho phép người dùng chọn một repository cụ thể để kích hoạt theo dõi (monitoring). Đồng thời tự động xác định nhánh `PRIMARY` từ GitHub default branch và quản lý hạn ngạch nhánh theo dõi theo đúng đặc tả Submission MVP.

## Source Trace

- Requirements: `FR-001` — Connect and select GitHub repositories for monitoring.
- Requirements: `FR-020` — Exactly one PRIMARY branch derived from GitHub default branch, up to 2 SECONDARY branches in MVP.
- Requirements: `FR-022` — GitHub default branch change automatically synchronizes PRIMARY branch.
- Requirements: `FR-023` — Capacity and secondary retention policy when default branch changes.
- Requirements: `FR-047` — Personal accounts supported; installation granted repository access.
- Decisions: `DEC-046` — Submission MVP scope: one selected repository.
- Decisions: `DEC-047` — GitHub onboarding flow: sign in first, then link app, then select repo.
- Architecture: `docs/ARCHITECTURE.md` — Submission GitHub Onboarding Boundary.

## Mục tiêu

- Cung cấp API hướng dẫn người dùng cài đặt hoặc cấp quyền GitHub App (`GET /api/v1/github/install-url`).
- Tiếp nhận GitHub App installation callback / webhook hoặc API đồng bộ installation ID với User Session (`POST /api/v1/github/installations/link`).
- Truy vấn danh sách repository mà GitHub App được cấp quyền (`GET /api/v1/github/repositories`).
- Cung cấp API chọn repository để onboard vào hệ thống (`POST /api/v1/projects/select-repository`), lưu thông tin repo (owner, name, github_repo_id, default_branch, private/public).
- Khởi tạo cấu hình nhánh: nhánh `PRIMARY` được trích xuất tự động từ `default_branch` của GitHub.

## Phạm vi

- Tích hợp GitHub REST API với GitHub App JWT authentication (tạo App JWT bằng Private Key để sinh Installation Access Token tạm thời khi cần truy vấn).
- Quản lý metadata của GitHub App Installation (installation_id, account_login, permissions).
- Trích xuất danh sách repository thuộc tài khoản cá nhân có quyền truy cập.
- Xử lý tạo bản ghi Project / Repository trong PostgreSQL khi người dùng chọn repo.
- Thiết lập quy tắc phân bổ nhánh: 1 `PRIMARY` (default branch) và tối đa 2 slot `SECONDARY` (người dùng chọn sau khi baseline hoàn tất).

## Không nằm trong phạm vi

- Không quét tự động toàn bộ hàng loạt repository chưa được người dùng xác nhận chọn.
- Không hỗ trợ phân quyền nhóm tổ chức (Organization membership / Admin approval flows) trong MVP.
- Không thực hiện quét mã nguồn tại bước này (quét mã nguồn do Scan Pipeline xử lý ở Issue 017).

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Quyền truy cập GitHub App tuân thủ nguyên tắc Least Privilege (Read-only contents, metadata).
- Không lưu private key hoặc installation token thô trong database; private key được nạp từ biến môi trường an toàn.
- Khi GitHub default branch thay đổi, Scan Pilot cập nhật `PRIMARY` tương ứng mà không xóa lịch sử quét cũ (theo `FR-022`, `FR-023`).

## Implementation Notes

- Backend sử dụng thư viện `org.kohsuke:github-api` hoặc HTTP client (như Spring `RestClient` / `WebClient`) kết hợp thư viện ký JWT `io.jsonwebtoken:jjwt-api` / `org.bouncycastle` để tương tác với GitHub API.
- Cấu hình biến môi trường: `GITHUB_APP_ID`, `GITHUB_APP_PRIVATE_KEY` (PEM format), `GITHUB_APP_SLUG`.

## Acceptance Criteria

- [ ] Sinh đúng URL cài đặt GitHub App dẫn đến giao diện cấp quyền của GitHub.
- [ ] Xử lý lưu và liên kết `installation_id` với User ID của phiên hiện tại sau khi cài đặt thành công.
- [ ] API `GET /api/v1/github/repositories` trả về danh sách repos cá nhân được ủy quyền kèm thông tin `is_selected`, `default_branch`, `visibility`.
- [ ] API `POST /api/v1/projects/select-repository` tạo thành công Project Profile trong hệ thống với nhánh `PRIMARY` khớp với `default_branch` từ GitHub.
- [ ] Xử lý lỗi đầy đủ khi người dùng chưa cài app, revoke app hoặc không có quyền truy cập repo.
- [ ] Unit/Integration tests với WireMock mô phỏng GitHub App API.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Tích hợp xác thực GitHub App JWT, lấy installation token, gọi GitHub API lấy repo metadata và lưu trữ thông tin repo ban đầu.
- Priority: High
- Priority Reason: Cần thiết để người dùng chọn repo mục tiêu trước khi thực hiện bất kỳ lệnh scan nào.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: 011 (Auth & Session)
- Blocking: 016, 017, 019
- Security alert: None

## Suggested Branch

`codex/12-github-app-linking-repo-selection`

## Ghi chú cho người thực hiện

- Kiểm tra định dạng `GITHUB_APP_PRIVATE_KEY` khi nạp từ biến môi trường (xử lý newline `\n`).
- Đảm bảo Installation Access Token chỉ sinh on-demand và có TTL ngắn theo quy định của GitHub.
