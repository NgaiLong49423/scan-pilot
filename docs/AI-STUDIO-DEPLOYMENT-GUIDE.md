> **Document:** Google AI Studio Frontend Sync & Cloud Run Deployment Guide
> **File:** `docs/AI-STUDIO-DEPLOYMENT-GUIDE.md`
> **Version:** v2.0.0
> **Created:** 2026-08-19
> **Last Updated:** 2026-09-02
> **Status:** Deprecated

# Deprecated: Google AI Studio Frontend Sync & Cloud Run Deployment Guide

Production frontend delivery is now GitHub-controlled. The `.github/workflows/deploy-frontend-cloud-run.yml` workflow builds the tracked `frontend/` directory and deploys `scan-pilot-web` to Cloud Run. Do not manually sync or publish this application from Google AI Studio: that would create a second, unverifiable production source.

This document is retained only as historical context for the earlier prototype workflow.

This guide provides step-by-step instructions for syncing the Scan Pilot React Frontend to your **Google AI Studio Project** and deploying it to **Google Cloud Run** using AI Studio's native publishing feature (`DEC-056`).

---

## 1. Prerequisites

1. **Deployed Backend Cloud Run URL**: Example: `https://scan-pilot-api-drbjfwrlxq-as.a.run.app`
2. **Google AI Studio Access**: Project workspace at [https://aistudio.google.com](https://aistudio.google.com).
3. **Google Cloud Project**: Shared MVP project `gen-lang-client-0098508328` (or your personal GCP project).

---

## 2. Step-by-Step Sync & Deployment Workflow

### Bước 1: Mở Workspace Dự án trên Google AI Studio
1. Truy cập [https://aistudio.google.com](https://aistudio.google.com).
2. Mở dự án **Scan Pilot** của bạn (Project ID: `9015a0c0-3972-426a-8c2b-26db961194b8`).

### Bước 2: Thiết lập Biến Môi trường Backend URL
1. Trong phần **Project Settings** (hoặc Environment Variables / `.env`), cấu hình:
   ```bash
   VITE_API_BASE_URL=https://scan-pilot-api-drbjfwrlxq-as.a.run.app
   ```
2. Lưu cài đặt.

### Bước 3: Kiểm tra Giao diện trên Preview của AI Studio
1. Xem tab **Preview** trên Google AI Studio.
2. Kiểm tra xem các component:
   * Thanh Header với Badge trạng thái và Nút kết nối GitHub.
   * Danh sách Lỗ hổng Secret Finding Cards với Secret Redaction (`AIzaSy...****`).
   * Gemini AI Remediation Guide với Before/After Fix Diffs.
   * Tab Coverage Audit với Scanned vs Skipped Files.
3. Nếu chưa đăng nhập OAuth, hệ thống sẽ tự động kích hoạt chế độ Reassurance Demo State mượt mà.

### Bước 4: Bấm "Deploy to Cloud Run" trên Google AI Studio
1. Bấm vào nút **Deploy to Cloud Run** (hoặc Publish) ở góc trên bên phải giao diện Google AI Studio.
2. Chọn Google Cloud Project: `gen-lang-client-0098508328`.
3. Chọn Region: `asia-southeast1` (Singapore).
4. Xác nhận và đợi Google AI Studio hoàn tất đóng gói và sinh ra đường link public:
   * `https://scan-pilot-web-xxxx.asia-southeast1.run.app`

### Bước 5: Cập nhật CORS Origin cho Backend
1. Sau khi nhận được link Frontend Cloud Run, cập nhật biến `SCANPILOT_CORS_ALLOWED_ORIGINS` của Backend Cloud Run:
   ```bash
   gcloud run services update scan-pilot-api \
     --region asia-southeast1 \
     --update-env-vars "SCANPILOT_CORS_ALLOWED_ORIGINS=https://aistudio.google.com,https://scan-pilot-web-xxxx.asia-southeast1.run.app"
   ```

---

## 3. Quy tắc Bảo vệ An toàn khi Prompt trên AI Studio (Guardrails)

* **An toàn khi Prompt UI:** Bạn có thể prompt AI Studio để đổi màu sắc, dịch thuật ngữ tiếng Việt/tiếng Anh, hoặc đổi biểu đồ trong `src/components/`.
* **Giữ nguyên thư mục `src/api/`:** Luôn nhắc AI Studio giữ nguyên cấu trúc các file trong `src/api/` (`client.ts`, `authApi.ts`, `scansApi.ts`, `aiApi.ts`, `projectsApi.ts`) để bảo toàn kết nối tới Backend Cloud Run.
