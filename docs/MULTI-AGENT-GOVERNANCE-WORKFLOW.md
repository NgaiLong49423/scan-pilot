> **Document:** Scan Pilot Multi-Agent Governance Workflow
> **File:** `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`
> **Version:** v2.0.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-20
> **Status:** Active

# Scan Pilot Multi-Agent Governance Workflow

Tài liệu này quy chuẩn hóa **Quy trình Phối hợp, Kiểm soát Chéo và Báo cáo (Peer-Review & Gatekeeping Protocol)** theo Mô hình Phối hợp Lồng nhau (Nested Coordination Model). Agent 4 (Delivery Gatekeeper / Coordinator) điều phối Agent 1 (Coder), Agent 2 (QA Reviewer độc lập), Agent 3 (AppSec Auditor độc lập) trên cùng local diff chưa commit; Agent 4 bàn giao Codex (Technical Manager / Tech Lead) review độc lập; Product Owner (User) là người duy nhất quyết định commit, push, PR, merge và nghiệm thu theo từng quyền riêng.

---

## 1. Mô hình Phối hợp Lồng nhau (Nested Coordination Model)

```text
┌────────────────────────────────────────────────────────────────────────┐
│               MÔ HÌNH PHỐI HỢP LỒNG NHAU (NESTED MODEL)                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│            Agent 4 (Delivery Gatekeeper / Coordinator)                 │
│            ├── Agent 1 (Coder)                                         │
│            ├── Agent 2 (QA Reviewer độc lập)                           │
│            └── Agent 3 (AppSec Auditor độc lập)                        │
│                 │                                                      │
│                 ▼ Báo cáo READY_FOR_TECH_LEAD_REVIEW                     │
│            Codex (Technical Manager / Tech Lead)                       │
│                 │                                                      │
│                 ▼ Review local diff / RCA khi cần                      │
│            Product Owner (User)                                        │
│                 │                                                      │
│                 ▼ PO ACCEPTED; chỉ khi đó mới được commit              │
│                 ▼ Push / PR / Merge cần quyền PO riêng                 │
└────────────────────────────────────────────────────────────────────────┘
```

> **Quy định Phân công trước BUILD (Before BUILD Assignment Rule):**
> 1. Trước khi `BUILD` bắt đầu, tác vụ (Issue) phải chỉ định rõ tên **Agent 4 (Delivery Gatekeeper / Coordinator)**;
> 2. Trong Kế hoạch thực thi của Agent 4 phải ghi rõ tên **Agent 1 (Coder)**, **Agent 2 (QA Reviewer độc lập)**, và **Agent 3 (AppSec Auditor độc lập)**;
> 3. **Codex** được ghi nhận riêng biệt là **Technical Lead / Technical Manager** (không phải Agent 4 và không phải subagent của Agent 4).

| Agent / Role | Vai trò chuyên môn | Kỹ năng bắt buộc (Repo Skills) | Quyền hạn & Trạng thái đầu ra |
| :--- | :--- | :--- | :--- |
| **Agent 4** | **Delivery Gatekeeper / Coordinator** | `agent-delivery-governance`, `document-metadata-standardizer` | Được chỉ định trước `BUILD`. Lập plan Agent 1/2/3, đóng băng local review target và đối chiếu 3 báo cáo cùng diff. Báo cáo: **`READY_FOR_TECH_LEAD_REVIEW`** trình Codex. |
| **Agent 1** | **Primary Implementer (Coder)** | `ponytail`, `full-output-enforcement`, `design-taste-frontend` | Thực thi mã/test trong local worktree, nộp handoff. **Không commit/push/PR trước quyền PO; không tự duyệt QA/AppSec.** |
| **Agent 2** | **Code Quality & QA Reviewer** | `ponytail-review`, `ui-design-audit` | Kiểm toán local diff đã đóng băng. Quyết định: **`APPROVED`** hoặc **`REQUEST_CHANGES`**. |
| **Agent 3** | **Security & Compliance Auditor** | `agent-delivery-governance`, OWASP ASVS/AISVS | Kiểm toán cùng local diff theo loại thay đổi. Quyết định: **`APPROVED`** hoặc **`BLOCKED`**. |
| **Codex** | **Technical Manager / Tech Lead** | `agent-delivery-governance`, repository review | Review độc lập local diff; RCA nguyên nhân workflow sai và sửa contract/template/brief trong scope được chấp thuận. Trạng thái: **`APPROVED_FOR_PO_ACCEPTANCE`**, **`CHANGES_NEEDED`**, hoặc **`BLOCKED`**. |
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
* **Trước Product Owner acceptance:** Agent 4 và Codex chỉ báo cáo local diff chưa commit, worktree path, base commit để tham chiếu và changed-file list; không tạo PR chỉ để có SHA.
* **Pull Request Description:** Chỉ được tạo sau quyền PO commit và push riêng; tóm tắt gọn nhẹ, secret-safe, chứa exact approved commit và trạng thái các Cổng.
* **`CHANGELOG.md`:** Cập nhật ngắn gọn mã commit hoặc trạng thái `Working tree` và nội dung thay đổi.
* **Quy tắc tuyệt đối:** Không ghi raw secrets, tokens, credentials, private source hay sensitive logs vào bất kỳ báo cáo, PR hay changelog nào.

---

## 3. Quy chuẩn Mẫu Báo cáo Bắt buộc (Standard Reporting Templates)

### 📄 Mẫu 1: Báo cáo Bàn giao của Agent 1 (Coder Handoff Report)
*Được tạo tại: `.agent-work/reports/handoff-<task>.md`*

```markdown
# [AGENT 1: HANDOFF REPORT]
- **Mã công việc:** <Issue # hoặc Tên tác vụ>
- **Review target:** `uncommitted local worktree`
- **Worktree / Base commit:** <path> / <base SHA for context only>
- **Frozen changed-file list:** <paths>
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
- **Review target:** <same frozen uncommitted local worktree as Coder handoff>
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
- **Review target:** <same frozen uncommitted local worktree as Coder handoff>
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
- **Mã tác vụ:** <Issue # hoặc Tên tác vụ>
- **Phân công Nhân sự trước BUILD:**
  - Agent 4 (Coordinator): <Tên Agent 4>
  - Agent 1 (Coder): <Tên Agent 1>
  - Agent 2 (QA Reviewer độc lập): <Tên Agent 2>
  - Agent 3 (AppSec Auditor độc lập): <Tên Agent 3>
  - Technical Lead: Codex (Technical Manager / Tech Lead)
- **Review target:** <same frozen uncommitted local worktree as Coder, QA, and AppSec>
- **Trạng thái khuyến nghị:** [READY_FOR_TECH_LEAD_REVIEW / CHANGES_NEEDED / BLOCKED]
- **Tóm tắt giải pháp kỹ thuật:** <Giải thích 2-3 câu ngắn gọn, dễ hiểu>
- **Xác nhận 3 Cổng Chặn:**
  - [x] Cổng 1: Agent 1 bàn giao đủ mã nguồn, bằng chứng test & handoff report.
  - [x] Cổng 2: Agent 2 (QA Reviewer độc lập) duyệt `APPROVED`.
  - [x] Cổng 3: Agent 3 (AppSec Auditor độc lập) duyệt `APPROVED`.
- **Xác nhận local diff:** <unchanged from Coder handoff through QA/AppSec review>
- **Khuyến nghị hành động tiếp theo:** [Trình Codex (Tech Lead) review kỹ thuật local diff]
```

---

### 📄 Mẫu 5: Báo cáo Đánh giá Kỹ thuật của Codex (Tech Lead Review)
*Được tạo tại: `.agent-work/acceptance/tech-lead-<task>.md` và trình lên Product Owner*

```markdown
# [CODEX: TECH LEAD REVIEW REPORT]
- **Review target:** <same frozen uncommitted local worktree as Gatekeeper submission>
- **Kiểm tra Kỹ thuật & Kiến trúc:**
  - [x] Tuân thủ kiến trúc Modular Monolith & Ponytail principles.
  - [x] Không phình to over-engineering hoặc nợ kỹ thuật không kiểm soát.
  - [x] An toàn thông tin, Zero secret leak, tuân thủ OWASP ASVS/AISVS.
- **Trạng thái phê duyệt kỹ thuật:** [APPROVED_FOR_PO_ACCEPTANCE / CHANGES_NEEDED / BLOCKED]
- **RCA khi không đạt:** <path to `.agent-work/diagnostics/rca-<task>.md` or `NA`>
- **Khuyến nghị hành động tiếp theo:** [Trình Product Owner (User) đưa ra quyết định PO ACCEPTED và, nếu được chấp thuận rõ ràng, quyền commit local diff]
```

---

### 📄 Mẫu 6: Phân tích Nguyên nhân Gốc của Codex (RCA)
*Được tạo tại: `.agent-work/diagnostics/rca-<task>.md` khi Codex phát hiện workflow, contract hoặc evidence sai.*

```markdown
# [CODEX: ROOT-CAUSE ANALYSIS]
- **Issue / Review target:** <Issue and uncommitted local worktree>
- **Symptom:** <what failed or was misunderstood>
- **Root cause:** <why the governing contract, template, brief, or workflow allowed it>
- **Affected artifacts:** <canonical documents, template, or brief>
- **Bounded correction:** <what Codex corrected within accepted scope>
- **Prevention rule:** <new deterministic rule/check to prevent recurrence>
- **Re-dispatch criteria:** <what Agent 4 must provide before fresh review>
```

Khi Codex sửa delivery artifact trong phạm vi RCA, RCA là handoff cho Agent 4. Agent 4 phải điều phối QA/AppSec review mới trên local diff đó trước khi Codex review lại; Codex không tự duyệt bản sửa của chính mình.

---

## 4. Nguyên tắc Vận hành Cốt lõi

1. **Phân công Nhân sự trước BUILD:** Trước khi `BUILD` bắt đầu, tác vụ (Issue) phải chỉ định tên Agent 4 (Delivery Gatekeeper / Coordinator). Trong Kế hoạch thực thi của Agent 4 phải phân công rõ Agent 1 (Coder), Agent 2 (QA Reviewer độc lập), và Agent 3 (AppSec Auditor độc lập). Codex được xác định riêng biệt là Technical Manager / Tech Lead.
2. **Không bỏ bước (No Skipping Gates):** Delivery Gatekeeper (Agent 4) chỉ báo cáo `READY_FOR_TECH_LEAD_REVIEW` khi có đầy đủ Coder handoff + QA `APPROVED` + AppSec `APPROVED` trên cùng local diff chưa commit. Codex chỉ cấp `APPROVED_FOR_PO_ACCEPTANCE` khi Agent 4 đã hoàn tất nghiệm thu 3 cổng.
3. **Không Commit trước PO:** Agent 1/2/3/4 và Codex không tự commit, push, tạo PR hoặc merge trước quyền PO rõ ràng. Sau `PO ACCEPTED`, commit, push, PR và merge vẫn là các quyền tách biệt. Nếu local diff thay đổi sau review, toàn bộ review trước đó mất hiệu lực.
4. **Nguyên tắc Độc lập:** Coder không được tự ký duyệt QA hoặc AppSec cho local diff do mình tạo ra. Agent 2 và Agent 3 phải là các Agent chuyên trách độc lập.
5. **Quy trình Sửa lỗi và RCA:** Nếu Agent 2 chọn `REQUEST_CHANGES`, Agent 3 chọn `BLOCKED`, hoặc Codex chọn `CHANGES_NEEDED`/`BLOCKED`, tác vụ quay lại Agent 4 rồi Agent 1. Codex phải phân tích nguyên nhân gốc, sửa delivery artifact trong scope nếu đó là nguyên nhân, rồi Agent 4 tổ chức review mới trên local diff mới.
6. **Báo cáo Tỷ lệ thuận (Proportional Checklists):** Danh mục kiểm tra của QA và AppSec phải phù hợp với bản chất thay đổi (Frontend UI, Backend REST API, Auth/GitHub integration, Database migration, CI/Workflow). Không áp dụng checklist OAuth/cookie máy móc cho các thay đổi CSS/giao diện thuần túy.
7. **Quyền Quyết định Tối cao (Product Owner Authority):** Báo cáo của Agent 4 và Codex là khuyến nghị kỹ thuật. Product Owner (Người dùng) là người duy nhất có quyền `PO ACCEPTED`, cho phép commit, cho phép push, tạo PR hoặc bấm **Merge PR**.
