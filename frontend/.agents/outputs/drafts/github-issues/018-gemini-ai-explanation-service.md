> **Document:** Issue Draft 018 — Implement Gemini Explanation and Remediation Guidance Service
> **File:** `.agents/outputs/drafts/github-issues/018-gemini-ai-explanation-service.md`
> **Version:** v1.0.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-18
> **Status:** Draft

# [AI][FR-005][FR-048] Implement Gemini Explanation and Remediation Guidance Service

## Tóm tắt

Triển khai dịch vụ giải thích và hướng dẫn khắc phục bảo mật thông qua Gemini API (Google Gen AI SDK). Dịch vụ chỉ tiếp nhận dữ liệu bằng chứng đã được chuẩn hóa và che giấu bí mật hoàn toàn (secret-redacted context), sinh ra giải thích bảo mật dễ hiểu, phân tích tác động rủi ro, nêu rõ giới hạn của bằng chứng, và đề xuất lộ trình khắc phục từng bước có cấu trúc. Đảm bảo ranh giới an toàn tuyệt đối: AI không có quyền sửa đổi mã nguồn, không tự ý đóng Finding và không tự động vô hiệu hóa khóa.

## Source Trace

- Requirements: `FR-005` — Gemini explains and analyzes findings with contextual reasoning.
- Requirements: `FR-048` — Bounded normalized secret-redacted evidence; structured explanation and guidance; no repo mutation or lifecycle decision.
- Decisions: `DEC-007` — AI does not define security truth.
- Decisions: `DEC-048` — Gemini explains evidence but does not mutate repositories.
- Architecture: `docs/ARCHITECTURE.md` — Submission Gemini Boundary.

## Mục tiêu

- Xây dựng `GeminiExplanationService` kết nối với Google Gemini API (sử dụng model `gemini-1.5-flash` hoặc `gemini-1.5-pro`).
- Thiết kế mẫu Prompt an toàn (Prompt Engineering) với ngữ cảnh giới hạn:
  - Thông tin Finding (rule ID, loại credential, tên file, dòng vi phạm).
  - Code snippet đã được che giấu secret bằng `[REDACTED_SECRET]`.
  - Trạng thái vòng đời hiện tại (`OPEN`, `RESOLVED`, `REGRESSED`) và chất lượng khắc phục.
- Yêu cầu Gemini trả về phản hồi định dạng JSON có cấu trúc (Structured JSON Output) tuân thủ schema:
  - `summary`: Tóm tắt sự cố bằng ngôn ngữ phổ thông, dễ hiểu cho lập trình viên mới.
  - `risk_impact`: Tại sao điều này nguy hiểm và kẻ tấn công có thể lợi dụng ra sao.
  - `evidence_limits`: Bằng chứng này chứng minh được điều gì và chưa chứng minh được điều gì (ví dụ: chứng minh key có trong code, nhưng không chứng minh được key còn hiệu lực hay đã bị sao chép ra ngoài).
  - `remediation_steps`: Các bước hành động theo thứ tự ưu tiên (1. Thu hồi key trên nhà cung cấp, 2. Đưa key vào biến môi trường / secret manager, 3. Thanh lọc lịch sử Git).
  - `lifecycle_transition_note`: Giải thích lý do chuyển đổi trạng thái khi re-scan.
- Lưu trữ kết quả AI dưới dạng `EvidenceItem` loại `AI_INFERENCE` gắn liền với Finding tương ứng.
- Cung cấp cơ chế dự phòng (fallback template) khi Gemini API gặp sự cố mạng hoặc hết hạn ngạch (quota).

## Phạm vi

- Tích hợp Google Cloud Vertex AI SDK hoặc Google Gen AI Client SDK cho Java.
- Xây dựng lớp trừu tượng `AiProviderRouter` hỗ trợ mở rộng.
- Xây dựng cơ chế validate JSON output trả về từ model, chống prompt injection từ nội dung code của người dùng.
- Xử lý cache kết quả giải thích theo fingerprint để tiết kiệm chi phí gọi API và tuân thủ ngân sách Cloud (`docs/CLOUD-BUDGET.md`).

## Không nằm trong phạm vi

- Không gửi toàn bộ repository hoặc file chưa redacted cho Gemini.
- Không cho phép Gemini tạo Pull Request, commit mã nguồn, sinh bản vá tự động áp dụng (auto-patch), hoặc xóa file (theo `DEC-048`).
- Không để Gemini tự quyết định Finding là `RESOLVED` hay `OPEN` (theo `DEC-007`, `DEC-048`).

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- Secret thô tuyệt đối không được xuất hiện trong bất kỳ câu prompt nào gửi sang Google Gemini.
- Kết quả từ Gemini luôn được hiển thị với nhãn rõ ràng là "AI Inferred Guidance" (theo `docs/EVIDENCE-MODEL.md`).

## Implementation Notes

- Cấu hình API key thông qua biến môi trường `GEMINI_API_KEY`.
- Sử dụng tính năng `response_mime_type: "application/json"` và JSON Schema Enforcement của Gemini API.

## Acceptance Criteria

- [ ] `GeminiExplanationService` gọi thành công Gemini API với prompt chứa code snippet đã được redact.
- [ ] Model trả về JSON hợp lệ khớp 100% với schema định nghĩa trước.
- [ ] Thông tin giải thích thể hiện đầy đủ 4 phần: Tóm tắt, Tác động rủi ro, Giới hạn bằng chứng, và Các bước khắc phục.
- [ ] Xử lý an toàn khi mất kết nối mạng hoặc key hết quota: hệ thống trả về fallback guidance có sẵn mà không làm lỗi scan job.
- [ ] Unit test xác minh không có raw secret nào lọt vào chuỗi prompt được sinh ra.
- [ ] Lưu trữ thành công đối tượng `EvidenceItem` loại `AI_INFERENCE` vào PostgreSQL.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Tích hợp Gemini SDK, thiết kế structured prompt & JSON schema enforcement, xử lý error/quota fallback và lưu trữ evidence.
- Priority: High
- Priority Reason: Tính năng trọng tâm thể hiện giá trị AI của sản phẩm cho sự kiện Google AI Riser.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🛠️ Backend`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: 015 (Secret Redaction), 017 (Scan Pipeline)
- Blocking: 019, 020
- Security alert: None

## Suggested Branch

`codex/18-gemini-ai-explanation-service`

## Ghi chú cho người thực hiện

- Kiểm tra chi phí gọi API: sử dụng `gemini-1.5-flash` làm model mặc định để tối ưu tốc độ phản hồi và chi phí trong ngân sách dự án.
- Thiết lập timeout tối đa 15 giây cho mỗi lượt gọi Gemini API.
