# Frontend Import Permissions

Frontend React + TypeScript cho luồng import quyền.

## Chạy bằng Docker

Từ thư mục gốc dự án:

```powershell
docker compose up -d --build
```

Mở `http://localhost:3000`. Nginx chuyển tiếp `/api/*` sang backend trong Docker nên không cần cấu hình CORS hoặc URL API trên trình duyệt.

## Chạy development

Backend cần chạy tại `http://localhost:8080`.

```powershell
cd frontend
npm install
npm run dev
```

Mở `http://localhost:5173`. Vite tự proxy `/api/*` sang backend.

## Luồng màn hình

1. Chọn file `.csv`, `.xls` hoặc `.xlsx`.
2. Upload và nhận `ImportJob` trạng thái `PARSING`.
3. Polling trạng thái cho đến khi `READY`, `INVALID` hoặc `FAILED`.
4. Nếu `INVALID`, chỉ hiển thị danh sách lỗi có phân trang. Người dùng sửa file nguồn và import lại.
5. Nếu `READY`, bật nút thực thi.
6. Sau khi Permission Engine xử lý, hiển thị tổng số `ADD`, `UPDATE`, `REMOVE`, `SKIP`, `FAILED`.

Các quyền chuẩn hiện tại gồm `NONE`, `READ`, `MODIFY`, `FULL_CONTROL` và `SPECIAL_PERMISSION`. Giá trị `Toan quyen` được chuẩn hóa thành `FULL_CONTROL`; `Quyen dac biet` được chuẩn hóa thành `SPECIAL_PERMISSION`. ACE `[TU CHOI]` được bỏ qua và không gửi sang Engine.
