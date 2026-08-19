# Backend quản lý phân quyền

Backend Spring Boot dùng để import và chuẩn hóa báo cáo Windows ACL.

## Chạy toàn bộ hệ thống bằng Docker

Thực hiện từ thư mục gốc của repository:

```powershell
Copy-Item .env.example .env
docker-compose up --build -d
```

Hãy thay các mật khẩu mẫu trong `.env` trước khi dùng ngoài môi trường development cá nhân.

Các địa chỉ mặc định:

- Backend: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- MySQL Docker từ máy host: `localhost:3307`

Backend kết nối MySQL thông qua hostname nội bộ `mysql:3306`. Flyway tự động tạo và cập nhật cấu trúc database. Dữ liệu MySQL và file upload được giữ trong Docker named volume nên không mất khi tạo lại container.

Các lệnh thường dùng:

```powershell
docker-compose ps
docker-compose logs -f backend
docker-compose down
```

`docker-compose down` giữ nguyên dữ liệu. Chỉ dùng `docker-compose down -v` khi thực sự muốn xóa vĩnh viễn database và file upload development.

Hướng dẫn đầy đủ nằm tại `README.md` ở thư mục gốc.

MySQL Server của dự án chỉ chạy trong Docker. MySQL Workbench có thể được dùng làm công cụ client để xem và quản trị database tại `127.0.0.1:3307`.
