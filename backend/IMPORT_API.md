# API import quyền

Backend import được tổ chức theo mô hình Spring Boot monolith:

```text
controller
  -> service
     -> parser / validation / storage / engine
        -> repository
           -> MySQL
```

## Luồng xử lý

1. FE upload file bằng `POST /api/imports`.
2. Backend lưu file vào Docker volume, tạo `ImportJob` và trả HTTP `202` với trạng thái `PARSING`.
3. Worker đọc file ở background, chuẩn hóa ACE và chỉ lưu các lỗi vào `import_errors`.
4. FE polling `GET /api/imports/{id}`.
5. Nếu trạng thái là `INVALID`, FE gọi `GET /api/imports/{id}/errors` để hiển thị lỗi. Người dùng sửa file gốc và import lại.
6. Nếu trạng thái là `READY`, FE cho phép gọi `POST /api/imports/{id}/execute`.
7. Execute chỉ chuyển command qua `PermissionEngineClient`. DB không được xem là bằng chứng quyền AD đã thay đổi.

## API

### Upload và validate

```http
POST /api/imports
Content-Type: multipart/form-data
X-Actor: admin
```

Form field chứa file phải có tên `file`. Định dạng hỗ trợ: `.csv`, `.xls`, `.xlsx`.

```bash
curl -H "X-Actor: admin" \
  -F "file=@permission.csv" \
  http://localhost:8080/api/imports
```

### Xem trạng thái

```http
GET /api/imports/{id}
```

Các trạng thái:

- `PARSING`: đang đọc và validate file.
- `INVALID`: có lỗi, không được execute.
- `READY`: hợp lệ và có thể execute.
- `EXECUTING`: đang gửi command sang Engine.
- `COMPLETED`: Engine đã xử lý thành công.
- `FAILED`: parse hoặc execute thất bại.

### Xem lỗi

```http
GET /api/imports/{id}/errors?page=0&size=100
```

`size` tối đa là 200. Mỗi lỗi chỉ rõ dòng nguồn, cột, vị trí ACE, giá trị gốc, mã lỗi và gợi ý.

### Execute

```http
POST /api/imports/{id}/execute
```

Chỉ chấp nhận job `READY` và `errorCount = 0`. Hiện tại adapter AD/.NET chưa được cấu hình nên API trả `503 ENGINE_NOT_CONFIGURED`; backend không giả lập thành công.

### Xem kết quả

```http
GET /api/imports/{id}/result?page=0&size=100
```

Kết quả gồm tổng số `ADD`, `UPDATE`, `REMOVE`, `SKIP`, `FAILED` và danh sách kết quả từ Engine.

## Quy tắc chuẩn hóa hiện tại

| Giá trị nguồn | Giá trị chuẩn |
|---|---|
| `Doc`, `Doc va chay`, `Read`, `Read & Execute` | `READ` |
| `Sua`, `Modify` | `MODIFY` |
| `Toan quyen`, `Full Control`, `FullControl` | `FULL_CONTROL` |
| `Quyen dac biet`, `Special Permission` | `SPECIAL_PERMISSION` |
| `None` | `NONE` |

Các ACE bắt đầu bằng `[TU CHOI]` được đếm là bỏ qua: không tạo lỗi và không sinh command. `Write` vẫn được trả về dưới dạng lỗi.

File phải có năm cột đầu theo đúng thứ tự:

```text
Duong dan, Loai, So ACE, Ngat ke thua, Ai co quyen gi
```
