> **Document:** Scan Pilot Multi-Agent Governance Workflow
> **File:** `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`
> **Version:** v1.1.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot Multi-Agent Governance Workflow

Tài liệu này quy chuẩn hóa **Quy trình Phối hợp, Kiểm soát Chéo và Báo cáo (Peer-Review & Gatekeeping Protocol)** theo luồng vận hành năm cấp giữa Agent 4 (Delivery Gatekeeper / Coordinator) điều phối Agent 1 (Coder), Agent 2 (QA Reviewer), Agent 3 (AppSec Auditor), báo cáo Codex (Technical Manager / Tech Lead) và trình Product Owner (User) trong hệ thống **Scan Pilot**.

---

## 1. Mô hình Phối hợp Năm Cấp (Five-Tier Governance Matrix)

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   LUỒNG KIỂM SOÁT NĂM CẤP BẮT BUỘC                     │
├────────────────────────────────────────────────────────────────────────┤
│ 1. [AGENT 1: Coder]       --> Viết mã nguồn & test theo tiêu chuẩn Ponytail│
│                                │                                       │
│                                ▼ (Cổng 1: Quality Gate)                │
│ 2. [AGENT 2: QA Reviewer] --> Audit logic, Clean Code, Test suite      │
│                                │                                       │
│                                ▼ (Cổng 2: Security Gate)               │
│ 3. [AGENT 3: AppSec]      --> Kiểm toán Cookie, OAuth, Zero-Leak       │
│                                │                                       │
│                                ▼ (Cổng 3: Coordination Gate)           │
│ 4. [AGENT 4: Gatekeeper]  --> Tổng hợp bằng chứng READY_FOR_TECH_LEAD_REVIEW│
│                                │                                       │
│                                ▼ (Cổng 4: Technical Lead Review)       │
│ 5. [CODEX: Tech Lead]     --> Review kỹ thuật APPROVED_FOR_PO_ACCEPTANCE│
│                                │                                       │
│                                ▼ (Cổng 5: Product Owner Acceptance)    │
│ 6. [PRODUCT OWNER: User]  --> Đưa ra quyết định PO ACCEPTED & cấp quyền Merge│
└────────────────────────────────────────────────────────────────────────┘
```

| Agent / Role | Vai trò chuyên môn | Kỹ năng bắt buộc (Repo Skills) | Quyền hạn & Trạng thái đầu ra |
| :--- | :--- | :--- | :--- |
| **Agent 1** | **Primary Implementer (Coder)** | `ponytail`, `full-output-enforcement`, `design-taste-frontend` | Thực thi mã nguồn, unit/integration tests, nộp báo cáo bàn giao. **Không được tự duyệt QA/AppSec cho chính mình.** |
| **Agent 2** | **Code Quality & QA Reviewer** | `ponytail-review`, `ui-design-audit` | Kiểm toán Clean Code, Ponytail standards, logic, tests, UX. Quyết định: **`APPROVED`** hoặc **`REQUEST_CHANGES`**. |
| **Agent 3** | **Security & Compliance Auditor** | `agent-delivery-governance`, OWASP ASVS/AISVS | Kiểm toán an toàn thông tin theo loại thay đổi. Quyết định: **`APPROVED`** hoặc **`BLOCKED`**. |
| **Agent 4** | **Delivery Gatekeeper / Coordinator** | `agent-delivery-governance`, `document-metadata-standardizer` | Điều phối Agents 1, 2, 3, kiểm tra 3 Cổng trên cùng head SHA. Báo cáo: **`READY_FOR_TECH_LEAD_REVIEW`** trình Codex. |
| **Codex** | **Technical Manager / Tech Lead** | `agent-delivery-governance`, repository review | Review kỹ thuật, kiến trúc, bảo mật & chất lượng tổng thể. Trạng thái: **`APPROVED_FOR_PO_ACCEPTANCE`** trình Product Owner. |
| **Product Owner** | **Product Owner (User)** | final product authority | Đưa ra quyết định chấp nhận nghiệp vụ (**`PO ACCEPTED`**) và cấp quyền merge PR. |

---

## 2. Vị trí Lưu trữ Báo cáo & Bằng chứng (Storage Locations)

Để đảm bảo bảo mật và minh bạch, các file báo cáo được phân tầng lưu trữ theo nguyên tắc:

### A. Tầng Báo cáo Tác vụ Cục bộ (`.agent-work/` — Đã Git-ignore)
Mọi ghi chép trung gian, log kiểm thử, kết quả phân tích chi tiết của từng Agent được tự động lưu tại:
* 📂 **Báo cáo bàn giao của Agent 1 (Coder):** `.agent-work/reports/handoff-<issue-or-task>.md`
* 📂 **Báo cáo kiểm tra chất lượng của Agent 2 (QA):** `.agent-work/qa-reviews/qa-<issue-or-task>.md`
* 📂 **Báo cáo kiểm toán bảo mật của Agent 3 (AppSec):** `.agent-work/security-audits/sec-<issue-or-task>.md`
* 📂 **Bản tổng hợp nghiệm thu của Agent 4 (Gatekeeper):** `.agent-work/acceptance/acceptance-<issue-or-task>.md`

### B. Tầng Báo cáo Chính thức & Trình duyệt Product Owner (GitHub & Chat UI)
* **Bản tóm tắt nghiệm thu:** Agent 4 xuất trực tiếp báo cáo `READY_FOR_TECH_LEAD_REVIEW` trên giao diện Chat cho Codex và Product Owner nắm bắt.
* **Pull Request Description:** Tóm tắt gọn nhẹ, secret-safe, chứa reviewed head SHA và trạng thái các Cổng (`QA: APPROVED`, `AppSec: APPROVED`, `Gatekeeper: READY_FOR_TECH_LEAD_REVIEW`, `Tech Lead: APPROVED_FOR_PO_ACCEPTANCE`).
* **`CHANGELOG.md`:** Cập nhật ngắn gọn mã commit hoặc trạng thái `Working tree` và nội dung thay đổi.
* **Quy tắc tuyệt đối:** Không ghi raw secrets, tokens, credentials, private source hay sensitive logs vào bất kỳ báo cáo, PR hay changelog nào.

---

## 3. Quy chuẩn Mẫu Báo cáo Bắt buộc (Standard Reporting Templates)

### 📄 Mẫu 1: Báo cáo Bàn giao của Agent 1 (Coder Handoff Report)
*Được tạo tại: `.agent-work/reports/handoff-<task>.md`*

```markdown
# [AGENT 1: HANDOFF REPORT]
- **Mã công việc:** <Issue # hoặc Tên tác vụ>
- **Reviewed Head SHA:** <SHA-1 commit hash hoặc "not committed yet">
- **Vấn đề đã xử lý:** <Mô tả ngắn gọn nguyên nhân gốc và giải pháp>
- **Tệp mã nguồn thay đổi:**
  - `backend/.../FileA.java`
  - `frontend/.../ComponentB.tsx`
- **Bằng chứng kiểm thử tự động:**
  - [x] Backend: `mvn test` (X tests passed, 0 failures)
  - [x] Frontend: `npm run lint` & `npm run build` (0 type errors)
- **Hạn chế & Điểm lưu ý cho QA & AppSec:** <Trường hợp biên hoặc cấu hình cần rà soát>
- **Xác nhận:** Tôi không tự tạo báo cáo duyệt QA hay AppSec cho chính thay đổi này.
```

---

### 📄 Mẫu 2: Báo cáo Đánh giá Chất lượng của Agent 2 (QA Review)
*Được tạo tại: `.agent-work/qa-reviews/qa-<task>.md`*

```markdown
# [AGENT 2: CODE QUALITY & LOGIC REVIEW]
- **Tác vụ được review:** <Tên tác vụ>
- **Reviewed Head SHA:** <Exact SHA-1 match with Coder Handoff>
- **Tiêu chí đánh giá (Thay đổi theo loại công việc):**
  1. Clean Code & Tuân thủ Ponytail (Không over-engineering, YAGNI): [PASS / FAIL / NA]
  2. Xử lý Logic & Bắt ngoại lệ (Exception Handling): [PASS / FAIL / NA]
  3. Tính hoàn chỉnh của Test (Coverage & Command output): [PASS / FAIL / NA]
  4. Trải nghiệm người dùng & Trạng thái UI (Loading/Error/Accessibility): [PASS / FAIL / NA]
- **Nhận xét chi tiết:** <Chỉ ra các điểm cần sửa nếu có>
- **Quyết định:** [ APPROVED / REQUEST_CHANGES ]
```

---

### 📄 Mẫu 3: Báo cáo Kiểm toán Bảo mật của Agent 3 (Security Audit)
*Được tạo tại: `.agent-work/security-audits/sec-<task>.md`*

```markdown
# [AGENT 3: SECURITY & COMPLIANCE AUDIT]
- **Tác vụ được kiểm toán:** <Tên tác vụ>
- **Reviewed Head SHA:** <Exact SHA-1 match with Coder Handoff>
- **Ma trận Kiểm soát Bảo mật (Tỷ lệ thuận theo loại thay đổi):**
  1. OAuth 2.0 / CSRF State Validation: [SECURE / AT_RISK / NA]
  2. Cookie Configuration (SameSite, Secure, HttpOnly): [SECURE / AT_RISK / NA]
  3. CORS & Origin Isolation: [SECURE / AT_RISK / NA]
  4. Zero Raw Secret Policy (Không log token, không hardcode credentials): [CONFIRMED]
  5. OWASP ASVS Alignment: [COMPLIANT / GAP_DETECTED / NA]
- **Nhận xét chi tiết:** <Được điều chỉnh theo loại thay đổi: Frontend UI, Backend API, Auth, Database, CI/Workflow>
- **Kết luận:** [ APPROVED / BLOCKED ]
```

---

### 📄 Mẫu 4: Báo cáo Nghiệm thu Tổng hợp của Agent 4 (Delivery Gatekeeper / Coordinator)
*Được tạo tại: `.agent-work/acceptance/acceptance-<task>.md` và trình lên Codex (Tech Lead)*

```markdown
# [AGENT 4: DELIVERY GATEKEEPER REPORT]
- **Reviewed Head SHA:** <Exact SHA-1 matching Coder, QA, and AppSec>
- **Trạng thái khuyến nghị:** [READY_FOR_TECH_LEAD_REVIEW / CHANGES_NEEDED / BLOCKED]
- **Tóm tắt giải pháp kỹ thuật:** <Giải thích 2-3 câu ngắn gọn, dễ hiểu>
- **Xác nhận 3 Cổng Chặn:**
  - [x] Cổng 1: Agent 1 bàn giao đủ mã nguồn, bằng chứng test & handoff report.
  - [x] Cổng 2: Agent 2 (QA Reviewer độc lập) duyệt `APPROVED`.
  - [x] Cổng 3: Agent 3 (AppSec Auditor độc lập) duyệt `APPROVED`.
- **Khuyến nghị hành động tiếp theo:** [Trình Codex (Tech Lead) review kỹ thuật và chuyển trạng thái READY_FOR_TECH_LEAD_REVIEW]
```

---

### 📄 Mẫu 5: Báo cáo Đánh giá Kỹ thuật của Codex (Tech Lead Review)
*Được tạo tại: `.agent-work/acceptance/tech-lead-<task>.md` và trình lên Product Owner*

```markdown
# [CODEX: TECH LEAD REVIEW REPORT]
- **Reviewed Head SHA:** <Exact SHA-1 matching Gatekeeper submission>
- **Kiểm tra Kỹ thuật & Kiến trúc:**
  - [x] Tuân thủ kiến trúc Modular Monolith & Ponytail principles.
  - [x] Không phình to over-engineering hoặc nợ kỹ thuật không kiểm soát.
  - [x] An toàn thông tin, Zero secret leak, tuân thủ OWASP ASVS/AISVS.
- **Trạng thái phê duyệt kỹ thuật:** [APPROVED_FOR_PO_ACCEPTANCE / REJECTED]
- **Khuyến nghị hành động tiếp theo:** [Trình Product Owner (User) đưa ra quyết định PO ACCEPTED và cấp quyền Merge PR]
```

---

## 4. Nguyên tắc Vận hành Cốt lõi

1. **Không bỏ bước (No Skipping Gates):** Delivery Gatekeeper (Agent 4) chỉ báo cáo `READY_FOR_TECH_LEAD_REVIEW` khi có đầy đủ Coder handoff + QA `APPROVED` + AppSec `APPROVED` trên cùng một reviewed head SHA. Codex (Tech Lead) chỉ cấp `APPROVED_FOR_PO_ACCEPTANCE` khi Agent 4 đã hoàn tất nghiệm thu 3 cổng.
2. **Nguyên tắc Độc lập:** Coder không được tự ký duyệt QA hoặc AppSec cho commit/head SHA do mình tạo ra. Agent 2 và Agent 3 phải là các Agent chuyên trách độc lập.
3. **Quy trình Sửa lỗi (Remediation Loop):** Nếu Agent 2 chọn `REQUEST_CHANGES`, Agent 3 chọn `BLOCKED`, hoặc Codex chọn `REJECTED`, tác vụ lập tức quay lại trạng thái `In Progress` cho Agent 1 (Coder). Sau khi sửa xong, bắt buộc phải có review mới từ đầu trên head SHA mới.
4. **Báo cáo Tỷ lệ thuận (Proportional Checklists):** Danh mục kiểm tra của QA và AppSec phải phù hợp với bản chất thay đổi (Frontend UI, Backend REST API, Auth/GitHub integration, Database migration, CI/Workflow). Không áp dụng checklist OAuth/cookie máy móc cho các thay đổi CSS/giao diện thuần túy.
5. **Quyền Quyết định Tối cao (Product Owner Authority):** Báo cáo của Agent 4 và Codex là khuyến nghị kỹ thuật. Product Owner (Người dùng) là người duy nhất có quyền đưa ra quyết định chấp nhận (`PO ACCEPTED`) và cấp quyền bấm **Merge PR**.
