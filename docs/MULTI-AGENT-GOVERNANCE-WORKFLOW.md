> **Document:** Scan Pilot Multi-Agent Governance Workflow
> **File:** `docs/MULTI-AGENT-GOVERNANCE-WORKFLOW.md`
> **Version:** v2.1.0
> **Created:** 2026-08-20
> **Last Updated:** 2026-08-30
> **Status:** Active

# Scan Pilot Multi-Agent Governance Workflow

Tài liệu này quy chuẩn hóa **Quy trình Phối hợp, Kiểm soát Chéo và Báo cáo (Peer-Review & Gatekeeping Protocol)** theo Mô hình Phối hợp Lồng nhau (Nested Coordination Model) kết hợp quy trình **PR-First Delivery Workflow**. Agent 4 (Delivery Gatekeeper / Coordinator) điều phối Agent 1 (Coder), Agent 2 (QA Reviewer độc lập), Agent 3 (AppSec Auditor độc lập) trên feature branch tạo từ `origin/dev`; mở PR hướng vào `dev` và đảm bảo CI xanh (`ci.yml`); Agent 4 bàn giao Codex (Technical Manager / Tech Lead) review độc lập trên đúng commit PR HEAD; Codex phê duyệt `APPROVED_FOR_DEV_MERGE` để merge vào `dev`; Product Owner (User) là người duy nhất nắm quyền mở PR và merge từ `dev` vào `main` (kích hoạt triển khai CD lên Google Cloud Run).

---

## 1. Mô hình Phối hợp Lồng nhau (Nested Coordination Model)

```text
┌────────────────────────────────────────────────────────────────────────┐
│               MÔ HÌNH PHỐI HỢP LỒNG NHAU (NESTED MODEL)                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│            Agent 4 (Delivery Gatekeeper / Coordinator)                 │
│            ├── Agent 1 (Coder: nhánh codex/* từ origin/dev, mở PR dev) │
│            ├── Agent 2 (QA Reviewer độc lập)                           │
│            └── Agent 3 (AppSec Auditor độc lập)                        │
│                 │                                                      │
│                 ▼ CI xanh (ci.yml) + Báo cáo READY_FOR_CODEX_REVIEW    │
│            Codex (Technical Manager / Tech Lead)                       │
│                 │                                                      │
│                 ▼ Review exact PR HEAD commit trên GitHub              │
│                 ▼ APPROVED_FOR_DEV_MERGE                               │
│            Merge vào nhánh dev (Tích hợp tính năng an toàn)           │
│                 │                                                      │
│                 ▼ Product Owner (User) kiểm soát quảng bá production   │
│            Product Owner: Merge dev -> main (Kích hoạt CD Cloud Run)   │
└────────────────────────────────────────────────────────────────────────┘
```

> **Quy định Phân công trước BUILD (Before BUILD Assignment Rule):**
> 1. Trước khi `BUILD` bắt đầu, tác vụ (Issue) phải chỉ định rõ tên **Agent 4 (Delivery Gatekeeper / Coordinator)**;
> 2. Trong Kế hoạch thực thi của Agent 4 phải ghi rõ tên **Agent 1 (Coder)**, **Agent 2 (QA Reviewer độc lập)**, và **Agent 3 (AppSec Auditor độc lập)**;
> 3. **Codex** được ghi nhận riêng biệt là **Technical Lead / Technical Manager** (không phải Agent 4 và không phải subagent của Agent 4).

| Agent / Role | Vai trò chuyên môn | Kỹ năng bắt buộc (Repo Skills) | Quyền hạn & Trạng thái đầu ra |
| :--- | :--- | :--- | :--- |
| **Agent 4** | **Delivery Gatekeeper / Coordinator** | `agent-delivery-governance`, `document-metadata-standardizer` | Được chỉ định trước `BUILD`. Lập plan Agent 1/2/3, đảm bảo CI xanh trên PR hướng vào `dev` và đối chiếu bằng chứng QA/AppSec. Báo cáo: **`READY_FOR_CODEX_REVIEW`** trình Codex. |
| **Agent 1** | **Primary Implementer (Coder)** | `ponytail`, `full-output-enforcement`, `design-taste-frontend` | Tạo branch từ `origin/dev`, thực thi mã/test, commit, push và mở PR vào `dev` với `Refs #N`. |
| **Agent 2** | **Code Quality & QA Reviewer** | `ponytail-review`, `ui-design-audit` | Kiểm toán diff và test suite trên PR. Quyết định: **`APPROVED`** hoặc **`REQUEST_CHANGES`**. |
| **Agent 3** | **Security & Compliance Auditor** | `agent-delivery-governance`, OWASP ASVS/AISVS | Kiểm toán cùng PR diff theo tiêu chuẩn bảo mật & zero raw secret. Quyết định: **`APPROVED`** hoặc **`BLOCKED`**. |
| **Codex** | **Technical Manager / Tech Lead** | `agent-delivery-governance`, repository review | Review độc lập exact commit PR HEAD trên GitHub; RCA nguyên nhân workflow sai khi cần. Trạng thái: **`APPROVED_FOR_DEV_MERGE`**, **`CHANGES_NEEDED`**, hoặc **`BLOCKED`**. |
| **Product Owner** | **Product Owner (User)** | final product authority | Nắm quyền tối cao mở PR và merge `dev` vào `main` (kích hoạt CD), nghiệm thu nghiệp vụ và đóng Issue. |

---

## 2. Vị trí Lưu trữ Báo cáo & Bằng chứng (Storage Locations)

Để đảm bảo bảo mật và minh bạch, các file báo cáo được phân tầng lưu trữ theo nguyên tắc:

### A. Tầng Báo cáo Tác vụ Cục bộ (`.agent-work/` — Đã Git-ignore)
Mọi ghi chép trung gian, log kiểm thử, kết quả phân tích chi tiết của từng Agent được tự động lưu tại:
* 📂 **Báo cáo bàn giao của Agent 1 (Coder):** `.agent-work/reports/handoff-<issue-or-task>.md`
* 📂 **Báo cáo kiểm tra chất lượng của Agent 2 (QA):** `.agent-work/qa-reviews/qa-<issue-or-task>.md`
* 📂 **Báo cáo kiểm toán bảo mật của Agent 3 (AppSec):** `.agent-work/security-audits/sec-<issue-or-task>.md`
* 📂 **Bản tổng hợp nghiệm thu của Agent 4 (Gatekeeper):** `.agent-work/acceptance/acceptance-<issue-or-task>.md`

### B. Tầng Báo cáo Chính thức & Trình duyệt Codex / Product Owner (GitHub & Chat UI)
* **Bản tóm tắt nghiệm thu:** Agent 4 xuất trực tiếp báo cáo `READY_FOR_CODEX_REVIEW` trên giao diện Chat và trong PR comment cho Codex và Product Owner nắm bắt.
* **Review Target:** Đối tượng đánh giá kỹ thuật là **exact PR HEAD commit** trên GitHub với kiểm tra tự động CI (`ci.yml`) đã báo xanh (Green).
* **Pull Request Description:** Được tạo khi mở PR hướng vào `dev`; tóm tắt gọn nhẹ, secret-safe, liên kết `Refs #N`, chứa exact approved commit và trạng thái các Cổng.
* **`CHANGELOG.md`:** Cập nhật ngắn gọn mã commit hoặc trạng thái `Working tree` và nội dung thay đổi.
* **Quy tắc tuyệt đối:** Không ghi raw secrets, tokens, credentials, private source hay sensitive logs vào bất kỳ báo cáo, PR hay changelog nào.

---

## 3. Quy chuẩn Mẫu Báo cáo Bắt buộc (Standard Reporting Templates)

### 📄 Mẫu 1: Báo cáo Bàn giao của Agent 1 (Coder Handoff Report)
*Được tạo tại: `.agent-work/reports/handoff-<task>.md`*

```markdown
# [AGENT 1: HANDOFF REPORT]
- **Mã công việc:** <Issue # hoặc Tên tác vụ>
- **Nhánh tính năng / PR URL:** `codex/<issue>-<name>` / <PR URL targeting dev>
- **Exact PR HEAD commit:** <commit SHA>
- **Changed-file list:** <paths>
- **Vấn đề đã xử lý:** <Mô tả ngắn gọn nguyên nhân gốc và giải pháp>
- **Tệp mã nguồn thay đổi:**
  - `backend/.../FileA.java`
  - `frontend/.../ComponentB.tsx`
- **Bằng chứng kiểm thử tự động:**
  - [x] Backend: `mvn clean verify` (X tests passed, 0 failures)
  - [x] Frontend: `npm run lint` & `npm run test` & `npm run build` (0 errors)
  - [x] CI Check: <GitHub Actions run URL (PASS)>
- **Hạn chế & Điểm lưu ý cho QA & AppSec:** <Trường hợp biên hoặc cấu hình cần rà soát>
- **Xác nhận:** Tôi không tự tạo báo cáo duyệt QA hay AppSec cho chính thay đổi này.
```

---

### 📄 Mẫu 2: Báo cáo Đánh giá Chất lượng của Agent 2 (QA Review)
*Được tạo tại: `.agent-work/qa-reviews/qa-<task>.md`*

```markdown
# [AGENT 2: CODE QUALITY & LOGIC REVIEW]
- **Tác vụ được review:** <Tên tác vụ / Issue #>
- **Review target:** <exact PR HEAD commit on GitHub>
- **Tiêu chí đánh giá (Thay đổi theo loại công việc):**
  1. Clean Code & Tuân thủ Ponytail (Không over-engineering, YAGNI): [PASS / FAIL / NA]
  2. Xử lý Logic & Bắt ngoại lệ (Exception Handling): [PASS / FAIL / NA]
  3. Tính hoàn chỉnh của Test (Coverage & CI Output): [PASS / FAIL / NA]
  4. Trải nghiệm người dùng & Trạng thái UI (Loading/Error/Accessibility): [PASS / FAIL / NA]
- **Nhận xét chi tiết:** <Chỉ ra các điểm cần sửa nếu có>
- **Quyết định:** [ APPROVED / REQUEST_CHANGES ]
```

---

### 📄 Mẫu 3: Báo cáo Kiểm toán Bảo mật của Agent 3 (Security Audit)
*Được tạo tại: `.agent-work/security-audits/sec-<task>.md`*

```markdown
# [AGENT 3: SECURITY & COMPLIANCE AUDIT]
- **Tác vụ được kiểm toán:** <Tên tác vụ / Issue #>
- **Review target:** <exact PR HEAD commit on GitHub>
- **Ma trận Kiểm soát Bảo mật (Tỷ lệ thuận theo loại thay đổi):**
  1. OAuth 2.0 / CSRF State Validation: [SECURE / AT_RISK / NA]
  2. Cookie Configuration (SameSite, Secure, HttpOnly): [SECURE / AT_RISK / NA]
  3. CORS & Origin Isolation: [SECURE / AT_RISK / NA]
  4. Zero Raw Secret Policy (Không log token, không hardcode credentials): [CONFIRMED]
  5. OWASP ASVS / AISVS Alignment: [COMPLIANT / GAP_DETECTED / NA]
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
- **Review target:** <exact PR HEAD commit on GitHub targeting dev>
- **Trạng thái CI:** [PASS — GitHub Actions run URL]
- **Trạng thái khuyến nghị:** [READY_FOR_CODEX_REVIEW / CHANGES_NEEDED / BLOCKED]
- **Tóm tắt giải pháp kỹ thuật:** <Giải thích 2-3 câu ngắn gọn, dễ hiểu>
- **Xác nhận 3 Cổng Chặn:**
  - [x] Cổng 1: Agent 1 bàn giao PR sạch, CI xanh & handoff report.
  - [x] Cổng 2: Agent 2 (QA Reviewer độc lập) duyệt `APPROVED`.
  - [x] Cổng 3: Agent 3 (AppSec Auditor độc lập) duyệt `APPROVED`.
- **Khuyến nghị hành động tiếp theo:** [Trình Codex (Tech Lead) review kỹ thuật PR HEAD để cấp quyền merge dev]
```

---

### 📄 Mẫu 5: Báo cáo Đánh giá Kỹ thuật của Codex (Tech Lead Review)
*Được tạo tại: `.agent-work/acceptance/tech-lead-<task>.md` và comment trực tiếp trên PR GitHub*

```markdown
# [CODEX: TECH LEAD REVIEW REPORT]
- **Review target:** <exact PR HEAD commit on GitHub targeting dev>
- **Kiểm tra Kỹ thuật & Kiến trúc:**
  - [x] Tuân thủ kiến trúc Modular Monolith & Ponytail principles.
  - [x] Không phình to over-engineering hoặc nợ kỹ thuật không kiểm soát.
  - [x] An toàn thông tin, Zero secret leak, tuân thủ OWASP ASVS/AISVS.
  - [x] Bằng chứng CI xanh và bộ test pass đầy đủ.
- **Trạng thái phê duyệt kỹ thuật:** [APPROVED_FOR_DEV_MERGE / CHANGES_NEEDED / BLOCKED]
- **RCA khi không đạt:** <path to `.agent-work/diagnostics/rca-<task>.md` or `NA`>
- **Khuyến nghị hành động tiếp theo:** [Hợp nhất PR vào nhánh dev sau khi được phê duyệt]
```

---

### 📄 Mẫu 6: Phân tích Nguyên nhân Gốc của Codex (RCA)
*Được tạo tại: `.agent-work/diagnostics/rca-<task>.md` khi Codex phát hiện workflow, contract hoặc evidence sai.*

```markdown
# [CODEX: ROOT-CAUSE ANALYSIS]
- **Issue / Review target:** <Issue and PR HEAD commit>
- **Symptom:** <what failed or was misunderstood>
- **Root cause:** <why the governing contract, template, brief, or workflow allowed it>
- **Affected artifacts:** <canonical documents, template, or brief>
- **Bounded correction:** <what Codex corrected within accepted scope>
- **Prevention rule:** <new deterministic rule/check to prevent recurrence>
- **Re-dispatch criteria:** <what Agent 4 must provide before fresh review>
```

Khi Codex gửi `CHANGES_NEEDED`, đó phải là một remediation card cụ thể. Agent 4 chuyển trực tiếp cho Agent 1 sửa, đẩy commit HEAD mới lên PR, và Agent 2/3 tái kiểm toán trước khi Codex review lại SHA mới.

---

## 4. Nguyên tắc Vận hành Cốt lõi

1. **Phân công Nhân sự trước BUILD:** Trước khi `BUILD` bắt đầu, tác vụ (Issue) phải chỉ định tên Agent 4 (Delivery Gatekeeper / Coordinator). Trong Kế hoạch thực thi của Agent 4 phải phân công rõ Agent 1 (Coder), Agent 2 (QA Reviewer độc lập), và Agent 3 (AppSec Auditor độc lập). Codex được xác định riêng biệt là Technical Manager / Tech Lead.
2. **Quy trình PR-First trên nhánh `dev`:** Mọi tính năng đều tạo nhánh `codex/<issue>-<name>` từ `origin/dev`, mở PR vào `dev` với `Refs #N`, và chạy CI tự động.
3. **Không bỏ bước (No Skipping Gates):** Delivery Gatekeeper (Agent 4) chỉ báo cáo `READY_FOR_CODEX_REVIEW` khi có đầy đủ Coder PR + CI xanh + QA `APPROVED` + AppSec `APPROVED` trên đúng PR HEAD commit. Codex chỉ cấp `APPROVED_FOR_DEV_MERGE` khi Agent 4 đã hoàn tất nghiệm thu các cổng.
4. **Nguyên tắc Độc lập:** Coder không được tự ký duyệt QA hoặc AppSec cho mã nguồn do mình tạo ra. Agent 2 và Agent 3 phải là các Agent chuyên trách độc lập.
5. **Quy trình Sửa lỗi và Remediation Loop:** Nếu Agent 2 chọn `REQUEST_CHANGES`, Agent 3 chọn `BLOCKED`, hoặc Codex chọn `CHANGES_NEEDED`/`BLOCKED`, tác vụ quay lại Agent 4 rồi Agent 1. Coder sửa, push commit mới lên branch, và toàn bộ quy trình kiểm toán chạy lại trên commit HEAD mới.
6. **Báo cáo Tỷ lệ thuận (Proportional Checklists):** Danh mục kiểm tra của QA và AppSec phải phù hợp với bản chất thay đổi (Frontend UI, Backend REST API, Auth/GitHub integration, Database migration, CI/Workflow).
7. **Phân quyền Quảng bá Production (Product Owner Authority):** Việc merge PR vào nhánh `dev` chỉ tích hợp tính năng nội bộ. Product Owner (Người dùng) là người duy nhất nắm quyền mở PR và merge từ `dev` vào `main` (kích hoạt CD lên Google Cloud Run) và đóng Issue chính thức (`Closes #N`).
