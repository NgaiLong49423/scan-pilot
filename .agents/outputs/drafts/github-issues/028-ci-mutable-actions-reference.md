> **Document:** GitHub Issue Draft 028 — SP-CI-001 Mutable GitHub Actions Reference
> **File:** `.agents/outputs/drafts/github-issues/028-ci-mutable-actions-reference.md`
> **Version:** v1.0.1
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Archived

# [CI][A03:2025][SP-CI-001] Detect Mutable Remote GitHub Actions References

Created as [GitHub Issue #58](https://github.com/NgaiLong49423/scan-pilot/issues/58) on 2026-08-20. This local draft is retained as the source record.

## Tóm tắt

Triển khai rule static `SP-CI-001` để phát hiện remote GitHub Action trong workflow `uses:` đang tham chiếu bằng tag, branch, short SHA hoặc expression thay vì full commit SHA. Kết quả là policy-risk Finding có evidence và lifecycle rõ ràng, không phải khẳng định repository đã bị compromise.

## Source Trace

- Accepted decision: `docs/DECISIONS.md` — `DEC-061`
- Rule contract: `docs/INSPECTION-SPEC.md` — `SP-CI-001 — Mutable Remote GitHub Actions Reference`
- Comparative research: `docs/research/security/SP-CI-001-MUTABLE-GITHUB-ACTIONS-REFERENCE.md`
- External standard: OWASP Top 10:2025 A03 — Software Supply Chain Failures
- Related product requirements: `FR-004`, `FR-007`, `FR-014`, `FR-015`

## Mục tiêu

Cho phép Scan Pilot tạo Finding có thể giải thích và kiểm chứng lại khi một supported remote GitHub Action reference không thỏa immutable-reference policy của rule.

## Phạm vi

- Chỉ đọc YAML workflow trong `.github/workflows/`.
- Phân tích semantic YAML `uses:` node có dạng remote action `OWNER/REPOSITORY@REF`.
- Xem `REF` là compliant chỉ khi khớp chính xác SHA Git 40 ký tự hexadecimal.
- Tạo Finding `Potential Mutable Remote GitHub Actions Reference` cho tag, branch, short SHA hoặc expression-backed reference.
- Lưu evidence an toàn: snapshot/commit, path, YAML location, normalized action target, reference classification, rule/config version và coverage outcome.
- Hỗ trợ lifecycle `OPEN`, `RESOLVED`, `REGRESSED` theo compatible re-scan.
- Thêm test matrix cho tag, branch, full SHA, expression, local action, Docker action, reusable workflow, invalid YAML và re-scan.

## Không nằm trong phạm vi

- Không chạy workflow, action, Docker image hoặc code của repository.
- Không gọi GitHub/Marketplace để xác minh action, tag, chữ ký hay compromise.
- Không kiểm tra Docker digest pinning, reusable-workflow pinning, workflow permissions, dependency CVE/SBOM, hoặc organization policy.
- Không gửi workflow content tới Gemini và không tự sửa workflow.
- Không khẳng định mutable reference là compromise hay Scan Pilot cover toàn bộ OWASP A03.

## Quy tắc nghiệp vụ / Yêu cầu liên quan

- `./path` là local action: `NOT_APPLICABLE_LOCAL_ACTION`, không tạo Finding.
- `docker://...` là Docker action: `NOT_APPLICABLE_DOCKER_ACTION`, không tạo Finding.
- `OWNER/REPOSITORY/.github/workflows/FILE@REF` là reusable workflow: `NOT_APPLICABLE_REUSABLE_WORKFLOW`, không tạo Finding.
- YAML lỗi hoặc syntax chưa hỗ trợ phải tạo coverage outcome rõ ràng; không được coi là clean.
- Finding mặc định `Medium`; wording phải mô tả policy risk và verification limit.

## Implementation Notes

Rule contract yêu cầu semantic YAML node inspection, không regex mọi chuỗi `uses:`. Chi tiết contract và test matrix nằm trong `docs/INSPECTION-SPEC.md` và research note đã dẫn.

## Acceptance Criteria

- [ ] Trước khi code bắt đầu, Issue `#51`, Issue `#49`, và Issue `#53` có bằng chứng nền tảng ổn định theo hợp đồng riêng của chúng; nếu chưa, giữ Issue này `Blocked`.
- [ ] Tag, branch, short SHA và expression-backed remote action reference tạo đúng một Finding policy-risk với evidence snapshot, path, location, action target và reference classification.
- [ ] Remote action reference có full 40-hex commit SHA không tạo Finding.
- [ ] Local action, Docker action và reusable workflow không tạo Finding và có not-applicable coverage outcome đúng reason code.
- [ ] Invalid YAML hoặc unsupported form không tạo clean claim và có coverage outcome truy vấn được.
- [ ] Re-scan tương thích sau khi đổi mutable reference sang full SHA chuyển Finding sang `RESOLVED`; mutable reference xuất hiện lại chuyển sang `REGRESSED`.
- [ ] Test không thực thi repository workflow/action, không gọi network để resolve ref, và không ghi secret hoặc expression-expanded content vào Finding, log, queue hay Gemini input.
- [ ] UI/API hiển thị wording policy-risk và verification limit, không mô tả compromise.

## Project Metadata

- Type: Feature
- Size: M
- Story Points: 5
- Estimation Reason: Một parser-backed static rule, normalized evidence, lifecycle integration, coverage outcomes và regression tests; không bao gồm network resolution hay workflow execution.
- Priority: Medium
- Priority Reason: Tạo chiều sâu platform cho demo nhưng phải nhường nền scan trung thực và coverage đáng tin.
- Start Date: TBD
- Target Date: TBD

## Labels

- `🚀 Feature`
- `🔒 Security`
- `🛠️ Backend`
- `🧪 Testing`
- `⛔ Blocked`
- `🟠 priority-medium`

## Relationships

- Parent: None
- Blocked by: #51, #49, #53
- Blocking: None
- Security alert: None

## Suggested Branch

`codex/58-ci-mutable-actions-reference`

## Ghi chú cho người thực hiện

- Đọc full rule contract và comparative research trước khi code.
- Không mở rộng sang reusable workflow, Docker image, permissions hoặc dependency CVE trong Issue này.
- Sau khi các blocker được Product Owner chấp nhận, cập nhật Issue sang `Planning` trước khi implementation bắt đầu.
