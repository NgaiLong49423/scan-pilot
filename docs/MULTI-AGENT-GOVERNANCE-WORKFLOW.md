> **Document:** Scan Pilot Multi-Agent Governance Workflow
> **File:** `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`
> **Version:** v1.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot Multi-Agent Governance Workflow

Tài liệu này quy chuẩn hóa **Quy trình Phối hợp, Kiểm soát Chéo và Báo cáo (Peer-Review & Gatekeeping Protocol)** giữa 4 Agent chuyên trách trong quá trình phát triển, kiểm toán và nghiệm thu hệ thống **Scan Pilot**.

---

## 1. Mô hình 4 Agent Chuyên Trách (Agent Roles Matrix)

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   LUỒNG KIỂM SOÁT 3 CỔNG CHẶN BẮT BUỘC                  │
├────────────────────────────────────────────────────────────────────────┤
│ 1. [AGENT 1: Coder]       --> Viết mã nguồn theo tiêu chuẩn Ponytail   │
│                                │                                       │
│                                ▼ (Cổng 1: Quality Gate)                │
│ 2. [AGENT 2: QA Reviewer] --> Soi bug logic, Clean Code, Test suite   │
│                                │                                       │
│                                ▼ (Cổng 2: Security Gate)               │
│ 3. [AGENT 3: AppSec]      --> Kiểm toán Cookie, OAuth, Zero-Leak       │
│                                │                                       │
│                                ▼ (Cổng 3: Final Acceptance Gate)       │
│ 4. [AGENT 4: Gatekeeper]  --> Tổng hợp bằng chứng, nghiệm thu & PO     │
└────────────────────────────────────────────────────────────────────────┘
```

| Agent | Vai trò chuyên môn | Kỹ năng bắt buộc (Repo Skills) | Quyền hạn đặc biệt |
| :--- | :--- | :--- | :--- |
| **Agent 1** | **Primary Implementer (Coder)** | `ponytail`, `full-output-enforcement`, `design-taste-frontend` | Thực thi mã nguồn, tạo unit/integration tests |
| **Agent 2** | **Code Quality & QA Reviewer** | `ponytail-review`, `ui-design-audit` | **Quyền VETO (Bác bỏ):** Trả về Agent 1 nếu phát hiện bug hoặc code thừa |
| **Agent 3** | **Security & Compliance Auditor** | `agent-delivery-governance`, OWASP ASVS/AISVS | **Quyền BLOCK (Chặn):** Chặn merge nếu phát hiện nguy cơ rò rỉ token hoặc hở bảo mật |
| **Agent 4** | **Lead Gatekeeper (Nghiệm thu)** | `agent-delivery-governance`, `document-metadata-standardizer` | **Nghiệm thu & Trình duyệt:** Chỉ ký duyệt khi Cổng 1 & Cổng 2 đã `PASS` |

---

## 2. Vị trí Lưu trữ Báo cáo & Bằng chứng (Storage Locations)

Để đảm bảo bảo mật và minh bạch, các file báo cáo được phân tầng lưu trữ theo nguyên tắc:

### A. Tầng Báo cáo Tác vụ Cục bộ (`.agent-work/` — Đã Git-ignore)
Mọi ghi chép trung gian, log kiểm thử, kết quả phân tích chi tiết của từng Agent được tự động lưu tại:
* 📂 **Báo cáo bàn giao của Agent 1:** `.agent-work/reports/handoff-<issue-or-task>.md`
* 📂 **Báo cáo kiểm tra chất lượng của Agent 2:** `.agent-work/qa-reviews/qa-<issue-or-task>.md`
* 📂 **Báo cáo kiểm toán bảo mật của Agent 3:** `.agent-work/security-audits/sec-<issue-or-task>.md`
* 📂 **Bản nghiệm thu tổng hợp của Agent 4:** `.agent-work/acceptance/acceptance-<issue-or-task>.md`

### B. Tầng Báo cáo Chính thức & Trình duyệt Product Owner (GitHub & Chat UI)
* **Bản tóm tắt nghiệm thu:** Agent 4 xuất trực tiếp trên giao diện Chat cho Product Owner (Bạn) nắm bắt nhanh gọn.
* **Pull Request Description:** Được đính kèm đầy đủ bảng ký duyệt của cả 3 Agent trước khi bấm Merge.
* **`CHANGELOG.md`:** Cập nhật ngắn gọn mã commit và số hiệu PR đã được nghiệm thu.

---

## 3. Quy chuẩn Mẫu Báo cáo Bắt buộc (Standard Reporting Templates)

### 📄 Mẫu 1: Báo cáo Bàn giao của Agent 1 (Coder Handoff Report)
*Được tạo tại: `.agent-work/reports/handoff-<task>.md`*

```markdown
# [AGENT 1: HANDOFF REPORT]
- **Mã công việc:** <Issue # hoặc Tên tác vụ>
- **Vấn đề đã xử lý:** <Mô tả ngắn gọn nguyên nhân gốc và giải pháp>
- **Tệp mã nguồn thay đổi:**
  - `backend/.../FileA.java`
  - `frontend/.../ComponentB.tsx`
- **Bằng chứng kiểm thử tự động:**
  - [x] Backend: `mvn test` (X tests passed, 0 failures)
  - [x] Frontend: `npm run lint` & `npm run build` (0 type errors)
- **Điểm lưu ý cho QA & AppSec:** <Những trường hợp biên hoặc cấu hình cần rà soát>
```

---

### 📄 Mẫu 2: Báo cáo Đánh giá Chất lượng của Agent 2 (QA Review)
*Được tạo tại: `.agent-work/qa-reviews/qa-<task>.md`*

```markdown
# [AGENT 2: CODE QUALITY & LOGIC REVIEW]
- **Tác vụ được review:** <Tên tác vụ>
- **Tiêu chí đánh giá:**
  1. Clean Code & Tuân thủ Ponytail (Không over-engineering, YAGNI): [PASS / FAIL]
  2. Xử lý Logic & Bắt ngoại lệ (Exception Handling): [PASS / FAIL]
  3. Tính hoàn chỉnh của Test (Coverage): [PASS / FAIL]
  4. Trải nghiệm người dùng & Trạng thái UI (Loading/Error): [PASS / FAIL]
- **Nhận xét chi tiết:** <Chỉ ra các điểm cần sửa nếu có>
- **Quyết định:** [ APPROVED / REQUEST_CHANGES ]
```

---

### 📄 Mẫu 3: Báo cáo Kiểm toán Bảo mật của Agent 3 (Security Audit)
*Được tạo tại: `.agent-work/security-audits/sec-<task>.md`*

```markdown
# [AGENT 3: SECURITY & COMPLIANCE AUDIT]
- **Tác vụ được kiểm toán:** <Tên tác vụ>
- **Ma trận Kiểm soát Bảo mật (Security Checklist):**
  1. OAuth 2.0 / CSRF State Validation: [SECURE / AT_RISK]
  2. Cookie Configuration (SameSite, Secure, HttpOnly): [SECURE / AT_RISK]
  3. CORS & Origin Isolation: [SECURE / AT_RISK]
  4. Zero Raw Secret Policy (Không log token, không hardcode credentials): [CONFIRMED]
  5. OWASP ASVS Alignment: [COMPLIANT / GAP_DETECTED]
- **Kết luận:** [ APPROVED / BLOCKED ]
```

---

### 📄 Mẫu 4: Báo cáo Nghiệm thu Tổng hợp của Agent 4 (Executive Sign-off)
*Được tạo tại: `.agent-work/acceptance/acceptance-<task>.md` và trình lên Product Owner*

```markdown
# [AGENT 4: FINAL ACCEPTANCE REPORT]
- **Trạng thái phê duyệt:** [ĐÃ NGHIỆM THU ĐẠT CHUẨN (3/3 Agent Ký Duyệt)]
- **Tóm tắt giải pháp kỹ thuật:** <Giải thích 2-3 câu ngắn gọn, dễ hiểu cho PO>
- **Xác nhận 3 Cổng Chặn:**
  - [x] Cổng 1: Agent 1 bàn giao đủ mã nguồn & bằng chứng test.
  - [x] Cổng 2: Agent 2 xác nhận chất lượng code đạt chuẩn `ponytail`.
  - [x] Cổng 3: Agent 3 xác nhận an toàn bảo mật, zero secret leak.
- **Khuyến nghị hành động tiếp theo:** [Sẵn sàng tạo PR / Cần PO cấu hình biến môi trường trên Cloud]
```

---

## 4. Nguyên tắc Vận hành Cốt lõi
1. **Không bỏ bước (No Skipping Gates):** Không được chuyển sang Agent 4 nghiệm thu nếu chưa có chữ ký `APPROVED` của cả Agent 2 và Agent 3.
2. **Quyền Phủ quyết (Veto Power):** Nếu Agent 2 hoặc Agent 3 từ chối (`REQUEST_CHANGES` hoặc `BLOCKED`), Agent 1 bắt buộc phải khắc phục lại và nộp lại Báo cáo bàn giao mới.
3. **Quyền Quyết định Tối cao:** Product Owner (Người dùng) là người duy nhất có quyền bấm **Merge PR** hoặc yêu cầu thay đổi nghiệp vụ.
