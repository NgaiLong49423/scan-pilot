> **Document:** Issue Draft 019 — Connect React Dashboard to Real Scan Pilot Backend REST APIs
> **File:** `.agents/outputs/drafts/github-issues/019-frontend-real-api-integration.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [Frontend][FR-004][FR-008][FR-044] Connect React Dashboard to Real Scan Pilot Backend REST APIs

## Tóm tắt

Tích hợp ứng dụng giao diện người dùng React + TypeScript + Vite với hệ thống REST API thực tế của backend Spring Boot. Thay thế toàn bộ mock state bằng dữ liệu thật: luồng đăng nhập GitHub OAuth, chọn repository theo dõi, hiển thị bảng điều khiển trung tâm (Dashboard), kích hoạt quét thủ công (Manual Scan Trigger), theo dõi tiến độ quét (Scan Job Progress), hiển thị chi tiết Finding kèm giải thích từ Gemini, và thể hiện trực quan các trạng thái vòng đời Finding và báo cáo Coverage.

## Source Trace

- Requirements: `FR-004` — Normalized findings with rule, severity, evidence, and remediation guidance.
- Requirements: `FR-008` — Dashboard showing monitored projects and latest scan state.
- Requirements: `FR-013` — Review request UI integration.
- Requirements: `FR-044` — Configuration UX separates Security Attention, Verification Coverage, and Configuration Change.
- Decisions: `DEC-002` — Product is dashboard-first.
- Decisions: `DEC-005` — React frontend communicates with Spring Boot via REST APIs.
- Decisions: `DEC-043` — Configuration UX separates attention, coverage, and change.
- Decisions: `DEC-044`, `DEC-045` — Production React frontend in GitHub source connects to real backend.

## Mục tiêu

- Hoàn thiện tầng gọi API phía client (API Client Service / React Query hoặc fetch hooks) với xử lý credentials và session cookie tự động.
- Kết nối các màn hình chính theo thiết kế UX/UI prototype đã được phê duyệt:
  1. Màn hình Đăng nhập & Onboarding: Nút đăng nhập GitHub, liên kết GitHub App, danh sách repo để chọn.
  2. Màn hình Dashboard Tổng quan: Thông tin repo được theo dõi, nhánh `PRIMARY`, trạng thái scan mới nhất, số lượng Finding theo mức độ nghiêm trọng (`Critical`, `High`, `Medium`, `Low`).
  3. Màn hình Tiến độ Quét (Scan Execution): Nút "Trigger Scan", thanh tiến độ quét (Job status: `PENDING` -> `RUNNING` -> `COMPLETED`), hiển thị thông tin snapshot commit.
  4. Màn hình Chi tiết Finding: Hiển thị code snippet đã redact, dấu vết `SP_SECRET_FP_V1`, các nhãn trạng thái vòng đời (`OPEN`, `RESOLVED`, `REGRESSED`), huy hiệu chất lượng khắc phục (`ACTION_REQUIRED`, `RISK_CONTAINED`, `VERIFIED_COMPLETE`), và khung hướng dẫn từ Gemini AI.
  5. Màn hình Báo cáo Coverage: Danh sách các tệp đã quét (`SCANNED`) và các tệp bị bỏ qua (`SKIPPED`) kèm mã lý do (như `UNSUPPORTED_BINARY_DOCUMENT`, `MONITORING_FILE_SIZE_LIMIT_EXCEEDED`).

## Phạm vi

- Xây dựng TypeScript interfaces đồng bộ với backend DTOs.
- Cấu hình Axios / Fetch client với `withCredentials: true` và base URL cấu hình động (`/api/v1` hoặc qua biến môi trường `VITE_API_BASE_URL`).
- Triển khai cơ chế Polling hoặc SSE/WebSocket để cập nhật trạng thái scan job theo thời gian thực.
- Xử lý các trạng thái giao diện: Loading state, Error banner, Empty state, và Session expired redirect.

## Không nằm trong phạm vi

- Không tự ý thay đổi màu sắc, bố cục hoặc thiết kế UI/UX đã được Product Owner chấp thuận từ prototype.
- Không lưu token xác thực vào `localStorage` hay biến JavaScript (dùng HttpOnly cookie do server quản lý).

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Giao diện phải phân biệt rõ ràng giữa bằng chứng kỹ thuật (`Technical Evidence`) và suy luận từ AI (`AI Inference`) (theo `docs/EVIDENCE-MODEL.md`).
- Tuyệt đối không hiển thị hoặc yêu cầu người dùng nhập raw secret không an toàn trên UI.

## Implementation Notes

- Đảm bảo Vite dev server proxy cấu hình đúng với backend local (`/api` -> `http://localhost:8080`).
- Viết component tests bằng Vitest và React Testing Library cho các view quan trọng.

## Acceptance Criteria

- [ ] Người dùng có thể click Đăng nhập -> chuyển hướng GitHub -> quay lại trang với thông tin tài khoản hiển thị trên header.
- [ ] Chọn một repository và kích hoạt quét thành công từ giao diện.
- [ ] Tiến độ quét hiển thị trực quan và tự động cập nhật khi job hoàn thành mà không cần reload trang thủ công.
- [ ] Màn hình chi tiết Finding hiển thị chính xác: vị trí file, dòng code đã redact, hướng dẫn từng bước từ Gemini, và badge trạng thái lifecycle.
- [ ] Danh sách tệp bị bỏ qua hiển thị đúng lý do trong tab Coverage.
- [ ] Xử lý mượt mà khi backend trả về lỗi hoặc khi hết phiên đăng nhập (redirect về login).
- [ ] Frontend build (`npm run build`) và test (`npm test`) vượt qua 100%.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Kết nối toàn bộ các endpoint REST API backend với React components, quản lý state bất đồng bộ, polling job status, và hiển thị giao diện đa trạng thái.
- Priority: High
- Priority Reason: Đưa toàn bộ hệ thống backend lên giao diện tương tác thực tế cho người dùng và ban giám khảo.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🎨 Frontend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: 011, 012, 017, 018
- Blocking: 020
- Security alert: None

## Suggested Branch

`codex/19-frontend-real-api-integration`

## Ghi chú cho người thực hiện

- Kiểm tra tính tương thích trên các trình duyệt phổ biến (Chrome, Firefox, Safari, Edge).
- Giữ nguyên các component mẫu đã chuẩn hóa từ Issue #9, chỉ gắn nối data fetching và state management thật.
