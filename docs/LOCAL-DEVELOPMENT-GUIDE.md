> **Document:** Scan Pilot Local Development and Run Guide
> **File:** `docs/LOCAL-DEVELOPMENT-GUIDE.md`
> **Version:** v1.1.0
> **Created:** 2026-08-18
> **Last Updated:** 2026-08-20
> **Status:** Active

# Hướng dẫn Khởi chạy và Kiểm thử Scan Pilot trên máy Local

Tài liệu này cung cấp hướng dẫn chi tiết từng bước giúp bạn khởi chạy song song cả **Backend (Spring Boot)** và **Frontend (React + Vite)** trên máy tính cá nhân để trải nghiệm giao diện và kiểm thử các tính năng bằng tay.

---

## 1. Yêu cầu môi trường (Prerequisites)

Trước khi chạy, đảm bảo máy tính đã cài đặt các công cụ sau:

* **Java JDK 21** trở lên (Khuyên dùng Eclipse Temurin 21).
  * Kiểm tra: `java -version`
* **Apache Maven 3.9+** (hoặc dùng Maven Wrapper).
  * Kiểm tra: `mvn -version`
* **Node.js 20 LTS** trở lên và **npm**.
  * Kiểm tra: `node -v` và `npm -v`

---

## 2. Các bước Khởi chạy 2 Server

Ứng dụng gồm 2 phần độc lập chạy trên 2 cổng khác nhau:
* **Frontend:** Cổng `3000` (hoặc `5173`)
* **Backend:** Cổng `8080`

### Bước 1: Khởi động Backend (Spring Boot)

1. Mở một cửa sổ Terminal (PowerShell hoặc CMD).
2. Di chuyển vào thư mục `backend`:
   ```powershell
   cd d:\Github-Projects\scan-pilot\backend
   ```
3. Chạy lệnh:
   ```powershell
   mvn spring-boot:run
   ```
   *(Tùy chọn: Nếu muốn test đăng nhập GitHub OAuth thật, bạn có thể truyền thêm biến môi trường):*
   ```powershell
   $env:GITHUB_CLIENT_ID="your_client_id"
   $env:GITHUB_CLIENT_SECRET="your_client_secret"
   mvn spring-boot:run
   ```
4. **Dấu hiệu thành công:** Khi terminal xuất hiện dòng chữ:
   ```text
   Started ScanPilotApplication in ... seconds
   ```
   Backend đã sẵn sàng phục vụ tại: **`http://localhost:8080`**.

---

### Bước 2: Khởi động Frontend (React / Vite)

1. Mở **cửa sổ Terminal thứ hai** (giữ cửa sổ Backend vẫn đang chạy).
2. Di chuyển vào thư mục `frontend`:
   ```powershell
   cd d:\Github-Projects\scan-pilot\frontend
   ```
3. *(Chỉ cần làm lần đầu tiên hoặc khi có thư viện mới)* Cài đặt dependencies:
   ```powershell
   npm install
   ```
4. Chạy server phát triển (Dev server):
   ```powershell
   npm run dev
   ```
5. **Dấu hiệu thành công:** Terminal sẽ hiển thị:
   ```text
   VITE v6.x.x  ready in ... ms
   ➜  Local:   http://localhost:3000/
   ```
   Frontend đã sẵn sàng phục vụ tại: **`http://localhost:3000`**.

---

## 3. Trải nghiệm và Kiểm thử bằng tay trên Trình duyệt

### A. Kiểm thử Giao diện trực quan (UI Dashboard)
Mở trình duyệt (Chrome, Edge, Firefox, Brave, ...) và truy cập:
👉 [**`http://localhost:3000`**](http://localhost:3000)

**Các tính năng bạn có thể tương tác trực tiếp:**
* **Fleet Dashboard:** Xem các repository đã monitor và mở trang chi tiết từng repository.
* **Tab Findings:** Lọc Finding theo severity/trạng thái và xem hướng dẫn remediation. Nút `Apply fix` hiện chỉ đổi state phía frontend, không phải sửa repository hoặc xác minh re-scan.
* **Tab Coverage & Audit:** Xem coverage và nội dung bị skip khi backend đã có bản ghi tương ứng.
* **Live Scan Terminal:** Chỉ dùng để kiểm tra giao diện ở baseline hiện tại; các log/count trung gian vẫn được mô phỏng bằng timer, không phải backend telemetry.

`Configuration Map`, `Review Requests`, event-driven scan và localization chưa phải luồng hoàn chỉnh trong app hiện tại. Xem `docs/IMPLEMENTATION-BASELINE.md` trước khi demo.

---

### B. Kiểm thử các API Backend trực tiếp

Bạn có thể mở các đường link sau trên trình duyệt hoặc công cụ Postman:

| Mục tiêu kiểm tra | Đường link API | Kết quả mong đợi |
|---|---|---|
| **Kiểm tra trạng thái Backend** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Trả về `{"status":"UP"}` |
| **Kiểm tra thông tin hệ thống** | [http://localhost:8080/api/v1/system/status](http://localhost:8080/api/v1/system/status) | Trả về JSON trạng thái hệ thống |
| **Kiểm tra Session người dùng** | [http://localhost:8080/api/v1/auth/me](http://localhost:8080/api/v1/auth/me) | Trả về `401 Unauthorized` (do chưa có session cookie) |
| **Bắt đầu đăng nhập GitHub** | [http://localhost:8080/api/v1/auth/github/login](http://localhost:8080/api/v1/auth/github/login) | Chuyển hướng trình duyệt sang GitHub OAuth screen |

---

## 4. Các lệnh kiểm thử mã nguồn tự động (Automated Tests)

Nếu bạn muốn chạy kiểm tra toàn bộ unit test trên máy trước khi commit:

* **Kiểm thử Backend:**
  ```powershell
  cd d:\Github-Projects\scan-pilot\backend
  mvn clean test
  ```
  *(Baseline ngày 2026-08-20: 210 tests, 0 failures, `BUILD SUCCESS`.)*

* **Kiểm tra TypeScript & Build Frontend:**
  ```powershell
  cd d:\Github-Projects\scan-pilot\frontend
  npm run lint
  npm run build
  ```

  Baseline ngày 2026-08-20: `npm run lint` đang fail do `App.tsx` truyền prop `isScanned` không tồn tại trong `HealthGaugeProps`. Không coi frontend là đã verify cho đến khi lint và build cùng pass.

---

## 5. Xử lý sự cố thường gặp (Troubleshooting)

1. **Lỗi cổng 8080 hoặc 3000 bị chiếm dụng (Port already in use):**
   * Tìm tiến trình đang chiếm cổng trên Windows:
     ```powershell
     netstat -ano | findstr :8080
     ```
   * Tắt tiến trình bị treo bằng PID:
     ```powershell
     taskkill /PID <PID_NUMBER> /F
     ```
2. **Lỗi `npm run dev` không mở đúng cổng 3000:**
   * Nếu cổng 3000 bận, Vite sẽ tự động chuyển sang 3001 hoặc 5173. Hãy kiểm tra đường link hiển thị trong Terminal sau khi chạy lệnh.
3. **Lỗi Java Version không đúng 21:**
   * Kiểm tra biến môi trường `JAVA_HOME` trỏ tới JDK 21:
     ```powershell
     $env:JAVA_HOME
     ```

---

## 6. Dừng Server (Stop)

Khi bạn muốn tắt server:
* Trên cửa sổ Terminal đang chạy Backend: Nhấn tổ hợp phím **`Ctrl + C`** (chọn `Y` nếu được hỏi).
* Trên cửa sổ Terminal đang chạy Frontend: Nhấn tổ hợp phím **`Ctrl + C`**.
