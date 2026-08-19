# Window Authorizer

Ứng dụng quản lý phân quyền Windows theo kiến trúc nhóm lồng nhau:

```text
Người dùng → Nhóm nghiệp vụ → Nhóm quyền → NTFS ACL → Tài nguyên
```

Backend sử dụng Spring Boot và MySQL. Môi trường phát triển được đóng gói bằng Docker Compose để mọi người có thể lấy source về và chạy với cùng cấu trúc dịch vụ, cấu hình database và migration.

## Yêu cầu

- Docker Desktop trên Windows/macOS hoặc Docker Engine trên Linux.
- Docker Compose.

Không cần cài Java, Maven hoặc MySQL trực tiếp trên máy nếu chạy toàn bộ hệ thống bằng Docker.

## Chạy hệ thống bằng Docker

### 1. Tạo file cấu hình môi trường

Không sửa trực tiếp `.env.example`. Hãy sao chép file mẫu thành `.env`.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env
```

Linux/macOS:

```bash
cp .env.example .env
```

Thay hai mật khẩu mẫu trong `.env`:

```env
MYSQL_ROOT_PASSWORD=mat-khau-quan-tri-mysql
DB_PASSWORD=mat-khau-cua-ung-dung
```

`DB_USERNAME=window_app` có thể giữ nguyên. File `.env` đã được Git bỏ qua và không được commit lên repository.

### 2. Build và khởi động hệ thống

```powershell
docker-compose up --build -d
```

Với Docker Compose dạng plugin, có thể dùng lệnh tương đương:

```powershell
docker compose up --build -d
```

Docker sẽ tự động:

- tải image MySQL;
- build image Spring Boot backend;
- tạo database `window_authorizer`;
- tạo tài khoản ứng dụng `window_app`;
- chạy Flyway để tạo/cập nhật cấu trúc bảng;
- kết nối backend với MySQL qua mạng nội bộ Docker;
- tạo volume để lưu database và file upload.

### 3. Kiểm tra trạng thái

```powershell
docker-compose ps
```

Kiểm tra backend:

```text
http://localhost:8080/actuator/health
```

Kết quả hợp lệ:

```json
{"status":"UP"}
```

Các địa chỉ mặc định:

| Dịch vụ | Địa chỉ |
|---|---|
| Backend | `http://localhost:8080` |
| Health check | `http://localhost:8080/actuator/health` |
| MySQL từ máy host | `localhost:3306` |
| MySQL bên trong Docker | `mysql:3306` |

Khi kết nối bằng MySQL Workbench, dùng `DB_USERNAME` và `DB_PASSWORD` trong `.env`.

## Các lệnh Docker thường dùng

Xem log backend:

```powershell
docker-compose logs -f backend
```

Xem log MySQL:

```powershell
docker-compose logs -f mysql
```

Dừng hệ thống và giữ dữ liệu:

```powershell
docker-compose down
```

Khởi động lại:

```powershell
docker-compose up -d
```

Build lại backend sau khi thay đổi source:

```powershell
docker-compose up --build -d
```

Xóa container và toàn bộ dữ liệu development:

```powershell
docker-compose down -v
```

> Cảnh báo: `docker-compose down -v` xóa vĩnh viễn database và file upload đang lưu trong Docker volume.

## Cấu trúc hiện tại

```text
WindowAuthorizer/
├── compose.yaml
├── .env.example
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   ├── .mvn/
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       └── test/java/
└── README.md
```

MySQL là nguồn lưu dữ liệu ứng dụng, workflow và audit. Khi tích hợp AD ở giai đoạn sau, Active Directory vẫn là nguồn sự thật cho người dùng, nhóm và membership có hiệu lực.

