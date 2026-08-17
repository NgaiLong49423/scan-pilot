> **Document:** Issue Draft 009 — Establish Production Foundation and Antigravity Handoff
> **File:** `.agents/outputs/drafts/github-issues/009-production-foundation-and-antigravity-handoff.md`
> **Version:** v1.0.1
> **Created:** 2026-08-17
> **Last Updated:** 2026-08-17
> **Status:** Created

# Establish Production Foundation and Antigravity Handoff

## Tóm tắt

Thiết lập cấu trúc production tối thiểu cho Scan Pilot từ prototype đã được chấp nhận, đồng thời xác minh rằng Antigravity có thể thực hiện một workstream Git-tracked theo branch và Pull Request để Codex review. Đây là nền móng kỹ thuật, chưa phải vertical slice quét bảo mật hoàn chỉnh.

## Source Trace

- Product direction: `DEC-044` — one-way AI Studio-to-production handoff.
- Product direction: `DEC-045` — AI Studio is submission evidence; Cloud Run hosts the real product.
- Implementation gate: `DEC-054` — Eligibility Spike `CONDITIONAL GO` and conditional implementation authorization.
- Delivery governance: `DEC-055` — hybrid agent delivery governance when `FULL_TRACKED` is activated.
- Requirements: `FR-036` — Java 21/Spring Boot 3 uses Apache Maven.
- Requirements: `FR-045`–`FR-046` — production source and real GitHub authentication/onboarding contract.
- Architecture: `docs/ARCHITECTURE.md` — accepted React/Vite, Spring Boot 3/Java 21, Maven, and modular-monolith direction.

## Mục tiêu

- Có một production workspace được theo dõi bằng Git, tách rõ khỏi prototype AI Studio.
- Chuyển giao có chọn lọc phần frontend đã được Product Owner duyệt, không tự ý thay đổi UI/UX.
- Có backend Spring Boot 3/Java 21 dùng Maven với build/test tối thiểu chạy được.
- Xác minh Antigravity có thể tạo branch và Pull Request mà Codex đọc được để hoàn tất Integration Check của governance.
- Đồng bộ tài liệu trạng thái để không còn tuyên bố sai rằng dự án đang ở research/specification phase.

## Phạm vi

- Xác định layout production frontend/backend trong repository hiện tại.
- Đưa phần frontend prototype được duyệt vào production source ở mức cần thiết cho build.
- Tạo Spring Boot 3/Java 21 Maven backend skeleton tối thiểu, chưa kết nối nghiệp vụ thật.
- Bổ sung cấu hình local an toàn và placeholder cần thiết; không đưa secret vào source.
- Chạy frontend build và Maven test/build.
- Thực hiện handoff qua branch và Pull Request theo governance; PR phải có implementation report, head SHA và verification evidence.
- Cập nhật tài liệu phase/status liên quan trong cùng checkpoint.

## Không nằm trong phạm vi

- Không triển khai OAuth production, GitHub App token lifecycle, private-repository scanning hoặc onboarding thật.
- Không kết nối PostgreSQL thật, Gitleaks, scan worker, Gemini workflow hoặc Cloud Run production.
- Không thiết kế lại hoặc tự phê duyệt UI/UX.
- Không mở rộng MVP, thay đổi public API, schema hoặc kiến trúc đã chốt.
- Không coi Integration Check là đạt nếu branch/PR không thể kiểm tra được trong Git repository.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- AI Studio prototype là evidence; GitHub production source là source of truth khi implementation bắt đầu.
- Một thay đổi Git-tracked trong `FULL_TRACKED` phải được bàn giao qua Pull Request.
- Chỉ Product Owner mới chấp nhận cuối cùng và chuyển work item sang `Done`.
- Không đưa `.env`, private key, client secret, token hoặc dữ liệu riêng tư vào repository, branch, PR hay evidence.

## Implementation Notes

Tài liệu nguồn xác định stack và handoff direction ở mức kiến trúc; không áp đặt layout package cụ thể. Người thực hiện phải chọn layout nhỏ nhất phù hợp với cấu trúc hiện tại và ghi lại lựa chọn trong PR report.

## Acceptance Criteria

- [ ] Production layout được xác định và không phụ thuộc vào thư mục template/legacy ngoài phạm vi đã chấp nhận.
- [ ] Frontend production build thành công bằng command được ghi trong PR report.
- [ ] Backend Spring Boot 3/Java 21 build và test thành công bằng Maven.
- [ ] Diff không chứa secret, `.env`, private state, generated credentials hoặc dữ liệu riêng tư.
- [ ] Antigravity tạo branch theo convention dự án và Pull Request mà Codex có thể truy cập, đọc diff và head SHA.
- [ ] PR description có Implementation Report, verification evidence, known limitations và nêu rõ UI/UX không bị tự ý thay đổi.
- [ ] Codex có thể ghi review outcome: `CHANGES_NEEDED`, `BLOCKED`, hoặc `APPROVED_FOR_PO_ACCEPTANCE`.
- [ ] Tài liệu phase/status liên quan phản ánh đúng trạng thái implementation sau khi checkpoint hoàn tất.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Phạm vi trung bình, gồm hai ứng dụng build được và một handoff branch/PR có rủi ro tích hợp; chưa bao gồm nghiệp vụ scan thật.
- Priority: High
- Priority Reason: Đây là nền móng trực tiếp cho mọi vertical slice production tiếp theo và là điều kiện để kiểm chứng governance Antigravity/Codex.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🛠️ Backend`
- `🎨 Frontend`
- `📋 Documentation`
- `🔴 priority-high`

## Relationships

- Parent: None
- Blocked by: None
- Blocking: Future production implementation Issues; exact Issue numbers TBD.
- Security alert: None

## Suggested Branch

`codex/9-production-foundation`

## Ghi chú cho người thực hiện

- GitHub Issue đã được tạo từ draft này: [#9](https://github.com/NgaiLong49423/scan-pilot/issues/9).
- Không commit, push, merge hoặc deploy chỉ vì Issue đã tồn tại.
- Nếu Antigravity export workspace vẫn không phải Git checkout, phải ghi `BLOCKED` cho Integration Check thay vì tuyên bố `FULL_TRACKED` đã hoạt động.
- Khi implementation xong, chuyển work item sang `Review` kèm PR và evidence; chỉ Product Owner mới chuyển sang `Done`.
