# Tài liệu bàn giao kiến trúc Window Authorizer

> Cập nhật: 20/08/2026  
> Mục đích: cung cấp đầy đủ bối cảnh để tiếp tục dự án trong một phiên làm việc khác.

## 1. Mục tiêu hệ thống

Hệ thống quản lý quyền Windows theo kiến trúc nhóm lồng nhau. Luồng mặc định:

```text
User → Business Group → Permission Group → NTFS ACL → Resource
```

Phương án hybrid đã chốt cho các quyền cá nhân legacy:

```text
User → Permission Group → NTFS ACL → Resource
```

`User → PG` là ngoại lệ có audit; `User → BG → PG` vẫn là mô hình quản trị mặc định.

Ý nghĩa:

- User là tài khoản người dùng trong Active Directory.
- Business Group (BG) đại diện cho phòng ban, vai trò hoặc nhóm nghiệp vụ.
- Permission Group (PG) đại diện cho một Resource/Zone và một mức quyền.
- Permission Group được cấu hình sẵn trong NTFS ACL và tương đối ổn định.
- Thay đổi quyền thông thường chủ yếu thay đổi membership `Business Group → Permission Group` trong AD.
- Quyền cá nhân legacy có thể thay đổi membership `User → Permission Group` mà không cần suy đoán hoặc tự tạo BG.
- Không rewrite NTFS ACL trên toàn bộ cây thư mục khi người dùng đổi READ/MODIFY.

Ví dụ chuyển quyền:

```text
READ → MODIFY

BG_FINANCE rời PG_REPORT_READ
BG_FINANCE vào PG_REPORT_MODIFY
```

Active Directory là nguồn sự thật cho user, group và membership có hiệu lực. Database chỉ phục vụ import, tìm kiếm, mapping, validation, snapshot, audit và báo cáo. Cập nhật DB đơn thuần không làm thay đổi quyền Windows.

Trong tương lai, dữ liệu AD sẽ được đồng bộ định kỳ về DB để hiển thị và resolve. Khi thực thi, Engine vẫn phải đọc/kiểm tra lại trạng thái thật trong AD.

## 2. Công nghệ và kiến trúc triển khai

```text
Frontend: React 19 + TypeScript + Vite + Nginx
Backend chính: Java 21 + Spring Boot 4.1 + Maven
Database: MySQL chạy trong Docker
AD Engine tương lai: .NET 8 ASP.NET Core chạy trên Windows
Container orchestration: Docker Compose
```

Địa chỉ mặc định:

| Thành phần | Địa chỉ |
|---|---|
| Frontend Docker | `http://localhost:3000` |
| Frontend Vite dev | `http://localhost:5173` |
| Backend | `http://localhost:8080` |
| Backend health | `http://localhost:8080/actuator/health` |
| MySQL từ host/Workbench | `127.0.0.1:3307` |
| MySQL trong Docker network | `mysql:3306` |

Khởi động:

```powershell
docker compose up --build -d
```

Mật khẩu nằm trong `.env`, không ghi cứng trong source. `.env.example` là file mẫu.

## 3. Phạm vi nghiệp vụ import đã chốt

Luồng người dùng hiện tại được đơn giản hóa:

```text
Upload file
→ Backend parse/normalize/validate
→ Nếu có lỗi: chỉ hiển thị dòng/cell lỗi
→ Người dùng sửa file gốc và import lại
→ Nếu hợp lệ: cho phép Execute
→ Backend chuẩn bị desired permission và gọi Engine
```

Không có chức năng sửa trực tiếp từng dòng trên UI ở phiên bản hiện tại. Valid rows không được lưu toàn bộ vào DB.

File hỗ trợ:

```text
.csv
.xls
.xlsx
```

Năm cột đầu bắt buộc:

```text
A: Duong dan
B: Loai
C: So ACE
D: Ngat ke thua
E: Ai co quyen gi
```

Header được chuẩn hóa bỏ dấu, không phân biệt hoa/thường, nhưng thứ tự năm cột đầu phải đúng.

File khách hàng dùng để kiểm thử:

```text
acl-tong-hop-20260804-045811-p23916/
acl-tong-hop-20260804-045811-p23916.csv
```

Thông tin đã quan sát:

```text
Kích thước: khoảng 31 MB
Source rows: 23.053
ACE: khoảng 584.176
```

## 4. Quy tắc parse và chuẩn hóa hiện tại

### 4.1 Quyền

| Dữ liệu nguồn | PermissionLevel |
|---|---|
| `Doc`, `Doc va chay`, `Read`, `Read & Execute` | `READ` |
| `Sua`, `Modify` | `MODIFY` |
| `Toan quyen`, `Full Control`, `FullControl` | `FULL_CONTROL` |
| `Quyen dac biet`, `Special Permission(s)` | `SPECIAL_PERMISSION` |
| `None` | `NONE` |

`Write` hiện chưa được hỗ trợ và tạo `UNSUPPORTED_PERMISSION`.

### 4.2 Principal

Ví dụ:

```text
FRA\sys.fssm (System MS-FSSM)
→ FRA\sys.fssm
```

Phần display name trong ngoặc cuối được loại bỏ. Hiện mới chuẩn hóa chuỗi, chưa kiểm tra đối tượng có tồn tại trong AD.

### 4.3 Kế thừa

```text
(thua ke) → inheritedAce = true
Co         → breaksInheritance = true
Khong      → breaksInheritance = false
```

### 4.4 DENY

ACE bắt đầu bằng `[TU CHOI]` được bỏ qua:

- Không tạo validation error.
- Không tạo command.
- Tăng `skippedPermissionEntries`.
- Chưa hỗ trợ business-level DENY.

### 4.5 Duplicate và conflict

Khóa nghiệp vụ hiện tại:

```text
Normalized Path + Normalized Principal
```

Không dùng direct/inherited trong khóa.

Quy tắc hiện tại:

- Cùng path, principal và permission: chỉ giữ ACE xuất hiện đầu tiên; ACE sau được tính skipped, không tạo lỗi.
- Cùng path và principal nhưng khác permission: tạo `CONFLICTING_PERMISSION`.
- Direct và inherited giống nhau được hiểu là cùng một desired permission.

Hạn chế đã biết: nếu cùng quyền có cả inherited và direct, code hiện giữ metadata của ACE xuất hiện trước. Chưa có logic luôn ưu tiên metadata direct.

### 4.6 Phân loại User và Business Group

Đã chốt không phân loại principal dựa trên tên, tiền tố hoặc biểu thức heuristic.

```text
Tên trong file
→ lookup AD snapshot trong DB bằng domain + sAMAccountName
→ đọc objectClass/objectGUID/SID đã đồng bộ từ AD
→ phân loại USER hoặc GROUP
```

Nếu là GROUP, hệ thống tiếp tục đọc mapping để phân biệt:

```text
BUSINESS_GROUP
PERMISSION_GROUP
OTHER_GROUP
```

Enum dự kiến:

```java
enum AdPrincipalType {
    USER,
    GROUP
}

enum AdGroupType {
    BUSINESS_GROUP,
    PERMISSION_GROUP,
    OTHER
}

enum BusinessGroupType {
    DEPARTMENT,
    ROLE,
    PROJECT,
    EXCEPTION
}

enum AssignmentType {
    BUSINESS_GROUP_MEMBERSHIP,
    DIRECT_USER_EXCEPTION
}
```

Tên phòng ban là dữ liệu trong DB/AD, không phải enum cứng. Nếu không resolve được hoặc kết quả không duy nhất, Backend phải báo lỗi và không tự đoán.

## 5. API hiện có

| API | Trạng thái | Mục đích |
|---|---|---|
| `POST /api/imports` | Đã có | Upload file, tạo job và bắt đầu parse async |
| `GET /api/imports/{id}` | Đã có | Lấy status và thống kê validation |
| `GET /api/imports/{id}/errors` | Đã có | Lấy lỗi có pagination |
| `POST /api/imports/{id}/execute` | Có khung | Dừng với `ENGINE_NOT_CONFIGURED` vì chưa có Engine thật |
| `GET /api/imports/{id}/result` | Đã có khung | Lấy thống kê và kết quả execution nếu Engine trả kết quả |

Không có API rows/edit/revalidate trong phạm vi đơn giản hiện tại.

## 6. Luồng Backend import hiện tại

### 6.1 Luồng HTTP tiếp nhận file

```text
Frontend
→ ImportController.upload()
→ ImportJobService.uploadAndValidate()
→ ImportFileStorage.store()
← StoredFile
→ ImportJobPersistenceService.create()
→ ImportJobRepository.saveAndFlush()
← ImportJob(status=PARSING, có ID)
→ ImportValidationJobService.validate()
→ Spring Async Proxy đưa task vào importTaskExecutor
→ ImportJobResponse.from(job)
← HTTP 202 + status PARSING
```

Lời gọi đẩy công việc vào worker nằm trong `ImportJobService`:

```java
validationJobService.validate(job.getId(), storedFile.storagePath());
```

Method đích có:

```java
@Async("importTaskExecutor")
```

Worker không phải container riêng. Nó là thread pool trong cùng JVM/backend container:

```text
corePoolSize = 1
maxPoolSize = 2
queueCapacity = 20
thread prefix = import-worker-
```

### 6.2 Luồng background parse/validate

```text
ImportValidationJobService.validate()
→ ImportFileStorage.resolve()
← Path
→ PermissionImportProcessor.scan(path, errorBuffer::add, command -> {})
→ ImportFileParserRegistry.parse(path, accumulator::accept)
→ CsvImportFileParser.parse() hoặc ExcelImportFileParser.parse()
→ HeaderValidator.validate()
→ Parser tạo từng RawImportRow
→ rowConsumer.accept(rawRow)
→ Accumulator.accept(rawRow)
→ normalize/validate/count
→ lỗi: ErrorBuffer.add(ValidationIssue)
→ mỗi 500 lỗi: ImportJobPersistenceService.saveErrors()
→ đọc hết file
← ValidationReport
→ ErrorBuffer.flush()
→ ImportJobPersistenceService.completeValidation()
→ ImportJobRepository.saveAndFlush()
← READY hoặc INVALID
```

`parser.parse(path, accumulator::accept)` sử dụng callback streaming. Parser không trả `List<RawImportRow>`; nó đọc đến đâu đẩy từng dòng sang Accumulator đến đó để không giữ toàn bộ file trong RAM.

Trong giai đoạn validation, callback command là:

```java
command -> { }
```

Do đó command hợp lệ chỉ được đếm, chưa lưu DB và chưa gửi Engine.

### 6.3 FE lấy kết quả

```text
Frontend polling GET /api/imports/{id}
→ PARSING: tiếp tục polling
→ READY/INVALID/FAILED: dừng polling

Nếu errorCount > 0:
GET /api/imports/{id}/errors?page=0&size=100
```

`errorCount` là số validation issue, không phải số dòng lỗi. Một dòng có thể có nhiều lỗi.

## 7. Trạng thái Import Job

```text
PARSING
├─ READY: validation hoàn tất, errorCount = 0
├─ INVALID: validation hoàn tất, errorCount > 0
└─ FAILED: lỗi hệ thống khi parse/lưu file

READY
→ EXECUTING
→ COMPLETED hoặc FAILED
```

## 8. Database hiện tại

Flyway migrations hiện có V1–V4.

Bảng đang dùng:

### `import_jobs`

Lưu:

- Thông tin file, checksum và storage path.
- Trạng thái job.
- Tổng source rows và ACE.
- Valid/skipped/error counts.
- Add/update/remove/skip/failed counts khi execute.
- Timestamps và failure message.

### `import_errors`

Chỉ lưu lỗi cần hiển thị:

```text
source row
column
ACE index
raw value
error code
message
suggestion
```

### `import_execution_results`

Khung lưu kết quả từng command do Engine trả về.

Valid rows không được lưu toàn bộ trong DB. File gốc được giữ trong upload volume; khi Execute, Backend kiểm tra checksum và parse lại bằng cùng logic.

## 9. Backend Execute đã có gì

`ImportExecutionService` hiện đã có orchestration khung:

```text
Kiểm tra job READY và không có lỗi
→ kiểm tra Engine configured
→ kiểm tra checksum file
→ revalidate
→ parse lại file
→ gom batch 100 PermissionCommand
→ gọi PermissionEngineClient.execute(batch)
→ kiểm tra số response
→ lưu execution results
→ cập nhật counters/status
```

Tuy nhiên implementation hiện tại là `UnconfiguredPermissionEngineClient`, nên API Execute trả HTTP 503 với mã:

```text
ENGINE_NOT_CONFIGURED
```

Hệ thống cố ý không giả lập rằng DB update đã thay đổi quyền Windows.

`PermissionCommand` hiện mới chứa dữ liệu sơ bộ:

```text
sourceRowNumber
aceIndex
resourcePath
principalName
desiredPermission
inheritedAce
breaksInheritance
```

Nó chưa chứa Business Group/Resource/Permission Group đã resolve.

## 10. Những phần đã hoàn thành

- Khởi tạo Spring Boot monolith theo controller/service/repository/entity/dto.
- MySQL Docker và Flyway.
- Upload/lưu file/checksum.
- Async Import Job và polling.
- Parser CSV/XLS/XLSX.
- Header validation.
- Normalize/validate dòng và ACE.
- Mapping READ/MODIFY/FULL_CONTROL/SPECIAL_PERMISSION.
- Skip `[TU CHOI]`.
- Gộp duplicate cùng path/principal/permission.
- Phát hiện conflict cùng path/principal nhưng khác permission.
- Lưu thống kê và lỗi theo batch.
- API summary/errors/result.
- Khung Execute và Permission Engine boundary.
- Frontend import đơn giản: upload, polling, thống kê, bảng lỗi, execute.
- Unit/integration tests backend hiện chạy thành công.
- Frontend build thành công.

## 11. Những phần chưa hoàn thành

### 11.1 Dữ liệu tham chiếu và AD snapshot

Chưa có đầy đủ model/table/service cho:

```text
AdPrincipal snapshot
BusinessGroup
User
Resource/Folder/Zone
PermissionGroup
PermissionAssignment
GroupMembership snapshot
PermissionChange
AuditLog đầy đủ
AD synchronization job
```

### 11.2 Resolve

Chưa có:

```text
principalName → BusinessGroup
path → Resource/Zone
Resource + PermissionLevel → PermissionGroup
```

### 11.3 Consolidation sau resolve

Chưa gộp theo khóa ổn định:

```text
MemberPrincipal objectGUID + Resource ID
```

Chưa có `ResolvedPermissionCommand`/desired-state request.

### 11.4 HTTP Engine client

Chưa có:

```text
HttpPermissionEngineClient
request/response contract chính thức
authentication
timeout/retry
mock HTTP Engine integration test
```

### 11.5 .NET AD Engine

Chưa xây dựng. Dự kiến:

```text
.NET 8 ASP.NET Core
Windows Service
System.DirectoryServices.Protocols
LDAPS 636
gMSA hoặc service account có delegated permission tối thiểu
```

Engine chạy trên Windows host/domain-joined server, không nhất thiết nằm trên Domain Controller. Spring Boot trong Docker development có thể gọi:

```text
https://host.docker.internal:7443
```

## 12. Kiến trúc mục tiêu Java → .NET → AD

```text
Frontend
  ↓
Spring Boot
  ├─ Parse/normalize/validate
  ├─ Resolve User hoặc BG/Resource/PG
  ├─ Consolidate desired permission
  ├─ Tạo changeId/idempotency key
  ├─ Lưu job/audit/result
  └─ HTTPS request
       ↓
.NET Permission Engine
  ├─ Xác thực request
  ├─ Allowlist OU/Permission Group được quản lý
  ├─ Đọc membership hiện tại từ AD
  ├─ Tính ADD/REMOVE/NO_CHANGE
  ├─ Thực hiện idempotent
  └─ Trả kết quả từng operation
       ↓ LDAPS
Active Directory
  ↓ BG membership hoặc direct-user exception membership
Permission Group đã nằm trong NTFS ACL
  ↓
Windows File Server
```

Khuyến nghị Java gửi desired state thay vì gửi các lệnh ADD/REMOVE mù. Engine đọc AD và reconcile về trạng thái mong muốn. Retry cùng `changeId` phải an toàn.

Ví dụ desired-state request tương lai:

```json
{
  "changeId": "IMPORT-125-BG-15-RESOURCE-25",
  "memberObjectGuid": "guid-user-or-bg",
  "memberType": "BUSINESS_GROUP",
  "assignmentType": "BUSINESS_GROUP_MEMBERSHIP",
  "resourceCode": "ERP_REPORTS",
  "desiredPermission": "MODIFY",
  "desiredPermissionGroupGuid": "guid-pg-modify",
  "managedPermissionGroupGuids": [
    "guid-pg-read",
    "guid-pg-modify"
  ]
}
```

## 13. Các quyết định nghiệp vụ

Phải chốt trước khi cho phép thay đổi AD thật:

### 13.1 User trực tiếp trong ACL — đã chốt

File khách hàng có cả user và group:

```text
FRA\dungnh
FRA\P.KeToan
```

Áp dụng mô hình hybrid:

```text
Mặc định: User → Business Group → Permission Group
Ngoại lệ: User → Permission Group
```

Principal là group nghiệp vụ được xử lý theo `Business Group → Permission Group`. Principal là user được phép thêm trực tiếp vào Permission Group dưới dạng `DIRECT_USER_EXCEPTION`. Không tự động thêm user vào một BG hiện có và không tự tạo BG chỉ dựa trên ACL import, vì việc đó có thể vô tình cấp toàn bộ quyền khác của BG cho user.

Direct-user membership phải lưu/audit tối thiểu:

```text
memberObjectGuid/SID
assignmentType = DIRECT_USER_EXCEPTION
sourceImportJob
reason
owner/approver (khi bổ sung workflow)
expiresAt (nếu là quyền tạm thời)
```

Khi nhiều user có cùng mục đích ổn định, quản trị viên có thể chủ động gom họ vào BG phù hợp ở giai đoạn maintain sau.

Phân loại user/group dựa trên `objectClass`, `objectGUID` và SID từ AD. `name` chỉ dùng để lookup snapshot; không dùng để suy đoán loại đối tượng.

### 13.2 FULL_CONTROL và SPECIAL_PERMISSION

Ban đầu hệ thống chỉ định hướng NONE/READ/MODIFY, nhưng parser hiện đã nhận FULL_CONTROL và SPECIAL_PERMISSION theo yêu cầu. Cần quyết định:

- Mỗi Resource có PG_FULL_CONTROL/PG_SPECIAL hay không.
- Hay chuyển một số quyền về READ/MODIFY theo policy.
- SPECIAL_PERMISSION cần access-mask/mapping rõ ràng; chuỗi báo cáo chỉ ghi tên chung có thể không đủ chi tiết.

### 13.3 Resource so với Zone

Cần quyết định:

- Mỗi folder path là một Resource riêng; hoặc
- Nhiều folder thuộc một permission zone dùng chung PG.

Nếu tạo READ/MODIFY PG cho mọi path trong file 23.053 dòng, số lượng AD group có thể rất lớn.

### 13.4 Inherited ACE

Đã chốt direct/inherited cùng permission là một desired permission ở tầng nghiệp vụ. Cần chốt thêm việc inherited-only có được sinh assignment cho resource con hay chỉ dùng để hiển thị nguồn quyền từ resource cha.

### 13.5 Thứ tự chuyển membership

Gợi ý an toàn:

```text
READ → MODIFY: add MODIFY trước, remove READ sau
MODIFY → READ: remove MODIFY trước, add READ sau
```

AD không có transaction nguyên tử cho hai group membership, nên Engine cần trả partial result và hỗ trợ reconciliation/retry.

## 14. Thứ tự triển khai tiếp theo

### Giai đoạn A — hoàn thiện import v1

1. Bổ sung test và logic ưu tiên metadata direct khi duplicate direct/inherited.
2. Bổ sung test regression với file khách hàng.
3. Chạy lại import lớn sau mỗi thay đổi parser; job cũ không tự được revalidate.

### Giai đoạn B — reference data và resolver

4. Tạo Flyway migration cho `ad_principals`, `business_groups`, `resources`, `permission_groups` và AD identifiers.
5. Tạo entity/repository/service tương ứng.
6. Tạo dữ liệu seed/test hoặc cơ chế nhận AD snapshot ban đầu.
7. Viết `PermissionResolver` với cache theo Import Job, tránh query DB cho từng ACE; resolver phải trả USER hoặc BUSINESS_GROUP từ dữ liệu AD snapshot.
8. Đưa lỗi resolve vào validation preview.

### Giai đoạn C — desired state

9. Viết `DesiredPermissionConsolidator` theo `MemberPrincipal objectGUID + Resource ID`.
10. Định nghĩa `ResolvedPermissionCommand` và `changeId` ổn định.
11. Không gửi duplicate command xuống Engine.

### Giai đoạn D — hoàn thiện Backend tới ranh giới Engine

12. Chốt OpenAPI/JSON contract Java ↔ .NET.
13. Viết `HttpPermissionEngineClient` bằng Spring `RestClient`.
14. Cấu hình base URL, auth, connect/read timeout và retry có điều kiện.
15. Viết Mock Engine/WireMock integration tests cho APPLIED, NO_CHANGE, FAILED, timeout và 503.
16. Tích hợp request đã resolve vào `POST /api/imports/{id}/execute`.
17. Kiểm tra persistence của execution result và audit.

### Giai đoạn E — .NET Engine và AD lab

18. Tạo .NET Engine health/read-only AD endpoints.
19. Dựng OU và BG/PG test trong AD lab.
20. Thêm reconcile membership với allowlist và least privilege.
21. Kết nối Java với Engine thật.
22. Test quyền trên thư mục lab đã gán ACL PG từ trước.

## 15. Definition of Done cho Backend trước Engine thật

Backend được xem là hoàn thành tới ranh giới Engine khi:

- File hợp lệ được parse, normalize, validate và resolve đầy đủ.
- Mỗi MemberPrincipal (USER hoặc BUSINESS_GROUP)/Resource chỉ có một desired permission.
- Request chứa AD identifier ổn định, không chỉ tên text.
- `POST /execute` gửi HTTPS request thật đến Mock Engine.
- Mock response APPLIED/NO_CHANGE/FAILED được lưu đúng.
- Retry không tạo command lặp hoặc báo thành công giả.
- Lỗi resolve hiển thị trước khi người dùng Execute.
- Test tự động bao phủ upload, async validation, duplicate/conflict, resolve, HTTP client và persistence.

## 16. Cấu trúc code quan trọng

```text
backend/src/main/java/com/windowauthorizer/permission/
├─ common/
├─ config/
│  └─ AsyncConfiguration.java
└─ importjob/
   ├─ controller/ImportController.java
   ├─ service/
   │  ├─ ImportJobService.java
   │  ├─ ImportValidationJobService.java
   │  ├─ ImportJobPersistenceService.java
   │  └─ ImportExecutionService.java
   ├─ parser/
   │  ├─ ImportFileParser.java
   │  ├─ ImportFileParserRegistry.java
   │  ├─ CsvImportFileParser.java
   │  ├─ ExcelImportFileParser.java
   │  ├─ HeaderValidator.java
   │  └─ RawImportRow.java
   ├─ validation/
   │  ├─ PermissionImportProcessor.java
   │  ├─ ValidationIssue.java
   │  └─ ValidationReport.java
   ├─ engine/
   │  ├─ PermissionEngineClient.java
   │  ├─ UnconfiguredPermissionEngineClient.java
   │  └─ PermissionCommand.java
   ├─ entity/
   ├─ repository/
   ├─ dto/
   └─ storage/

frontend/src/
├─ App.tsx
├─ api/importApi.ts
├─ components/
└─ styles.css
```

## 17. Lưu ý khi tiếp tục trong phiên khác

- Đọc tài liệu này và kiểm tra `git status` trước khi sửa.
- Worktree hiện có nhiều thay đổi/untracked files của BE/FE chưa được commit; không reset hoặc xóa chúng.
- Chạy `backend\\mvnw.cmd -q test` sau thay đổi backend.
- Chạy `npm run build` trong `frontend` sau thay đổi frontend.
- Cần rebuild Docker để code mới có hiệu lực: `docker compose up --build -d`.
- Import Job cũ giữ kết quả validation cũ; phải import lại file để dùng logic parser mới.
- Không tuyên bố quyền Windows đã thay đổi khi Engine/AD chưa xác nhận thành công.
