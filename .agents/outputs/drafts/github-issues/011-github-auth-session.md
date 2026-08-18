> **Document:** Issue Draft 011 — Implement GitHub OAuth Sign-In and Server-Side Session Management
> **File:** `.agents/outputs/drafts/github-issues/011-github-auth-session.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Auth][FR-001][FR-047] Implement GitHub OAuth Sign-In and Server-Side Session Management

## Tóm tắt

Triển khai luồng xác thực GitHub OAuth 2.0 Web Application Flow và quản lý phiên người dùng (Session Management) phía server trong backend Spring Boot. Đảm bảo người dùng đăng nhập bằng tài khoản GitHub cá nhân, tạo session cookie an toàn (HttpOnly, SameSite, Secure), không để lộ token sang frontend JavaScript, và hỗ trợ đăng xuất, hết hạn session cũng như xử lý thu hồi quyền truy cập.

## Source Trace

- Product Direction: `DEC-045` — Public Cloud Run application hosts real product authentication.
- Onboarding Order: `DEC-047` — Sign in with GitHub before installing/linking the GitHub App; personal accounts officially supported.
- Security Gate: `DEC-054` (Condition 3) — Production Spring Boot path must implement server-side session storage, logout, expiry, revocation handling before real scanning.
- Requirements: `FR-001` — Connect and select GitHub repositories for monitoring.
- Requirements: `FR-046` — No judge-only anonymous scan bypass; standard sign-in and onboarding workflow.
- Requirements: `FR-047` — Sign in with GitHub before linking GitHub App installation.
- Architecture: `docs/ARCHITECTURE.md` — Submission GitHub Onboarding Boundary.

## Mục tiêu

- Cung cấp endpoint khởi tạo đăng nhập OAuth (`GET /api/v1/auth/github/login`) chuyển hướng người dùng đến GitHub authorization screen với `state` param chống CSRF.
- Cung cấp callback endpoint (`GET /api/v1/auth/github/callback`) trao đổi authorization code lấy GitHub user access token.
- Lấy thông tin profile GitHub user (ID, username, avatar_url, email) và lưu trữ / cập nhật thông tin user trong cơ sở dữ liệu.
- Thiết lập session phía server và trả về secure session cookie (`SCANPILOT_SESSION`) cho trình duyệt.
- Cung cấp các endpoint quản lý phiên: kiểm tra trạng thái đăng nhập (`GET /api/v1/auth/me`) và đăng xuất (`POST /api/v1/auth/logout`).

## Phạm vi

- Cấu hình Spring Security / OAuth2 client hoặc custom OAuth client handler chuẩn mực trong Spring Boot.
- Triển khai cơ chế lưu trữ session an toàn trên backend.
- Tạo cookie bảo vệ với cờ `HttpOnly; Secure; SameSite=Lax` (hoặc `SameSite=Strict` tuỳ luồng chuyển hướng).
- Lưu trữ GitHub User Access Token đã mã hóa hoặc quản lý an toàn phía server, tuyệt đối không trả token về client.
- Kiểm tra tính hợp lệ của session khi gọi các REST API nội bộ cần xác thực.
- Xử lý kịch bản đăng xuất (xóa session server và xóa cookie client).

## Không nằm trong phạm vi

- Không hỗ trợ tổ chức (GitHub Organizations) hay luồng phân quyền đa người dùng trong team (chỉ hỗ trợ personal account trong MVP).
- Không xử lý GitHub App installation token (được tách riêng sang Issue 012).
- Không lưu token thô vào logs, metrics hay giao diện người dùng.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Trình tự onboarding bắt buộc: Đăng nhập GitHub -> Tạo session Scan Pilot -> Liên kết GitHub App -> Chọn repository (theo `DEC-047`).
- Không tạo tài khoản giả lập hoặc bypass authentication không kiểm tra (theo `FR-046`).
- Xử lý lỗi an toàn: nếu GitHub từ chối cấp code hoặc `state` không khớp, trả về mã lỗi bảo mật chuẩn `401 Unauthorized` hoặc `400 Bad Request` kèm redirect về trang login kèm thông báo lỗi thân thiện.

## Implementation Notes

- Cấu hình client ID và client secret thông qua Environment Variables (`GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`), không hardcode trong mã nguồn.
- Triển khai `state` parameter bằng token ngẫu nhiên mã hóa có thời hạn ngắn (cryptographically secure random).
- Các endpoint API `/api/v1/auth/**` tuân thủ chuẩn REST của dự án.

## Acceptance Criteria

- [ ] Endpoint `GET /api/v1/auth/github/login` sinh `state` hợp lệ và redirect đúng đến GitHub OAuth authorize URL.
- [ ] Endpoint `GET /api/v1/auth/github/callback` validate `state`, trao đổi code lấy access token thành công, và xử lý đúng nếu `state` sai.
- [ ] Thông tin người dùng GitHub (id, login, avatar) được trích xuất và tạo/cập nhật user record trong database.
- [ ] Trả về session cookie an toàn (HttpOnly, Secure, SameSite) cho client.
- [ ] Endpoint `GET /api/v1/auth/me` trả về thông tin user khi có session hợp lệ và trả về 401 khi chưa đăng nhập.
- [ ] Endpoint `POST /api/v1/auth/logout` hủy session server-side và xóa cookie ở client.
- [ ] Đạt unit test và integration test với mock OAuth provider; mã nguồn không chứa secret hoặc token hardcoded.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Xử lý luồng OAuth 2.0 chuẩn, state CSRF validation, quản lý cookie bảo mật, Spring Security context và unit/integration test.
- Priority: High
- Priority Reason: Auth & Session là nền tảng bảo mật tiên quyết cho mọi tương tác người dùng và kết nối GitHub sau này.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🔒 Security`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: 010 (CI workflow)
- Blocking: 012, 016, 019
- Security alert: None

## Suggested Branch

`codex/11-github-auth-session`

## Ghi chú cho người thực hiện

- Kiểm tra kịch bản chạy local (localhost) vs chạy Cloud Run (HTTPS/proxy headers `X-Forwarded-Proto`).
- Đảm bảo Spring Security không chặn các public endpoints (`/api/v1/auth/github/**`, `/actuator/health`).
