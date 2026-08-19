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
- MySQL từ máy host: `localhost:3306`

Backend kết nối MySQL thông qua hostname nội bộ `mysql:3306`. Flyway tự động tạo và cập nhật cấu trúc database. Dữ liệu MySQL và file upload được giữ trong Docker named volume nên không mất khi tạo lại container.

Các lệnh thường dùng:

```powershell
docker-compose ps
docker-compose logs -f backend
docker-compose down
```

`docker-compose down` giữ nguyên dữ liệu. Chỉ dùng `docker-compose down -v` khi thực sự muốn xóa vĩnh viễn database và file upload development.

Hướng dẫn đầy đủ nằm tại `README.md` ở thư mục gốc.

## Chạy riêng backend với MySQL có sẵn

Yêu cầu Java 21 và MySQL 8. Thiết lập biến môi trường trong terminal hiện tại:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/window_authorizer?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'window_app'
$env:DB_PASSWORD = '<mat-khau-local>'
.\mvnw.cmd spring-boot:run
```

Credential được cung cấp qua biến môi trường và không được commit vào source control.
