# Kế hoạch ứng dụng Quản lý Phân quyền

## 1. Kiến trúc phân quyền

Ứng dụng sử dụng mô hình phân quyền nhóm lồng nhau:

```text
Người dùng (User)
  → Nhóm nghiệp vụ (Business Group)
    → Nhóm quyền (Permission Group: tài nguyên + mức quyền)
      → NTFS ACL
        → Tài nguyên / Thư mục
```

Nguyên tắc cốt lõi:

- Người dùng là thành viên của **Business Group**.
- **Permission Group** đại diện cho một tài nguyên/khu vực và một mức quyền, ban đầu là `READ` và `MODIFY`.
- Permission Group được cấu hình sẵn trong NTFS ACL và nên ổn định lâu dài.
- Thay đổi quyền thông thường **không** được ghi lại NTFS ACL trên toàn bộ cây thư mục.
- Chuyển Business Group từ `READ` sang `MODIFY` nghĩa là xóa nhóm đó khỏi Permission Group READ của tài nguyên và thêm vào Permission Group MODIFY.
- CSDL ứng dụng phục vụ tìm kiếm, giao diện, kiểm tra hợp lệ, tính quyền, import, audit và báo cáo; nó không thay thế cơ chế phân quyền Windows.
- Thay đổi chỉ có hiệu lực trên Windows File Server sau khi thay đổi membership tương ứng trong Active Directory thành công.
- Active Directory là **nguồn sự thật (source of truth)** cho User, Business Group, Permission Group, membership và quyền hiệu lực. Dữ liệu trong DB chỉ là bản sao phục vụ việc xem, tìm kiếm, xử lý workflow, audit và báo cáo.
- Hệ thống định kỳ query/synchronize dữ liệu từ AD về DB. Khi DB và AD khác nhau, trạng thái thực tế từ AD được ưu tiên để hiển thị và đối soát.
- Không đưa quyền `DENY` ở mức nghiệp vụ vào mô hình. Các mức quyền ban đầu là `NONE`, `READ` và `MODIFY`.

## 2. Mô hình cơ sở dữ liệu

| Thực thể | Mục đích | Trường chính gợi ý |
|---|---|---|
| `User` | Người dùng AD được đồng bộ vào ứng dụng | `id`, `adObjectGuid`, `samAccountName`, `upn`, `displayName`, `status`, timestamps |
| `BusinessGroup` | Nhóm nghiệp vụ/phòng ban chứa người dùng | `id`, `adObjectGuid`, `name`, `description`, `status`, timestamps |
| `Resource` | File share, thư mục hoặc vùng dữ liệu được quản lý | `id`, `resourceCode`, `name`, `uncPath`, `parentResourceId`, `status` |
| `PermissionLevel` | Danh mục mức quyền | `NONE`, `READ`, `MODIFY` |
| `PermissionGroup` | Nhóm AD ổn định, được map vào NTFS ACL | `id`, `resourceId`, `permissionLevel`, `adObjectGuid`, `adGroupName`, `ntfsVerifiedAt`, `active` |
| `GroupMembership` | Membership của User trong Business Group | `businessGroupId`, `userId`, `source`, `syncedAt`, `active` |
| `PermissionAssignment` | Membership của Business Group trong Permission Group | `id`, `businessGroupId`, `permissionGroupId`, `status`, `effectiveFrom`, `effectiveTo`, `lastSyncedAt` |
| `ImportJob` | File Excel import và trạng thái xử lý | `id`, `fileName`, `uploadedBy`, `status`, các bộ đếm parse/validation/change, timestamps |
| `PermissionChange` | Thay đổi quyền và trạng thái thực thi trên AD | `id`, `businessGroupId`, `resourceId`, `fromLevel`, `toLevel`, `status`, `requestedBy`, `approvedBy`, `adCorrelationId`, timestamps |
| `AuditLog` | Nhật ký vận hành và bảo mật không thể sửa | `id`, `actor`, `action`, `entityType`, `entityId`, `beforeJson`, `afterJson`, `correlationId`, timestamp |

### Quy tắc và ràng buộc dữ liệu

- `PermissionGroup` là duy nhất theo `(resourceId, permissionLevel)` cho `READ` và `MODIFY`.
- `NONE` không có Permission Group riêng; nó có nghĩa là Business Group không thuộc Permission Group hiệu lực nào của Resource đó.
- Một Business Group có tối đa một Permission Assignment đang hiệu lực trên mỗi Resource.
- Permission Group được định danh bằng AD `objectGUID` ổn định, không chỉ dựa vào tên nhóm.
- Ứng dụng phải ngăn một Business Group đồng thời thuộc cả Permission Group READ và MODIFY của cùng Resource, trừ khi yêu cầu sau này cho phép.
- Metadata và membership của Permission Group có thể được đồng bộ từ AD để đối soát.

## 3. Luồng cập nhật quyền

Ví dụ: chuyển Business Group `Finance` tại `\\fileserver\Finance\Reports` từ `READ` sang `MODIFY`.

1. Nhận yêu cầu thay đổi quyền.
2. Kiểm tra Business Group, Resource, mức quyền đích, trạng thái workflow và trạng thái đồng bộ AD.
3. Xác định quyền hiện tại từ `PermissionAssignment`; khi cần có thể kiểm tra lại trạng thái thực tế trên AD.
4. Tạo `PermissionChange` với trạng thái `PENDING`.
5. Gọi AD qua lớp tích hợp AD:
   1. Xóa Business Group khỏi Permission Group READ của Resource.
   2. Thêm Business Group vào Permission Group MODIFY của Resource.
6. Nếu AD thành công, cập nhật CSDL:
   - đóng hoặc vô hiệu hóa assignment READ;
   - tạo hoặc kích hoạt assignment MODIFY;
   - đánh dấu `PermissionChange` là `APPLIED`;
   - ghi thêm các bản ghi `AuditLog`.
7. Nếu AD thất bại, không thể hiện quyền mong muốn là đã có hiệu lực trong CSDL. Lưu kết quả kỹ thuật và đánh dấu thay đổi là `FAILED` hoặc `PARTIALLY_APPLIED`.
8. Retry hoặc đối soát lỗi dựa trên kết quả AD và correlation ID đã lưu.

Với `NONE`, xóa Business Group khỏi cả Permission Group READ và MODIFY phù hợp của Resource. Với `READ` hoặc `MODIFY`, đảm bảo Business Group chỉ thuộc Permission Group tương ứng.

## 4. Ranh giới tích hợp Active Directory

Mọi giao tiếp với Active Directory được cô lập sau `ActiveDirectoryGateway` (hoặc adapter tương đương). Domain service không được gọi trực tiếp LDAP hoặc PowerShell.

Gateway cần hỗ trợ:

- Tìm User, Business Group và Permission Group theo AD `objectGUID`, distinguished name hoặc `sAMAccountName`.
- Đọc nested membership hiện tại.
- Thêm Business Group vào Permission Group.
- Xóa Business Group khỏi Permission Group.
- Đồng bộ định kỳ metadata và membership AD về CSDL ứng dụng.
- Trả về chi tiết kỹ thuật, correlation ID và lỗi có thể xử lý.

Việc tách lớp này giúp mock/test dễ dàng, tập trung quản lý credential, timeout, retry và logging; đồng thời cho phép dùng LDAP, PowerShell Remoting hoặc một AD worker/service chuyên dụng trong tương lai.

Hệ thống cần các job đồng bộ/đối soát định kỳ để query dữ liệu từ AD về DB, gồm User, Business Group, Permission Group và nested membership. DB chỉ giữ dữ liệu sao chép phục vụ đọc; AD vẫn là nguồn sự thật. Khi phát hiện sai lệch, trạng thái AD được ghi nhận vào DB, đồng thời tạo audit/cảnh báo để xử lý các workflow hoặc thay đổi đang dang dở.

## 5. Cấu trúc API

### Dữ liệu danh mục và tìm kiếm

- `GET /api/users`
- `GET /api/business-groups`
- `GET /api/resources`
- `GET /api/resources/{id}/permission-groups`
- `GET /api/permission-levels`

### Thao tác quyền

- `GET /api/business-groups/{id}/permissions`
- `GET /api/resources/{id}/business-group-permissions`
- `GET /api/permissions/effective?businessGroupId=&resourceId=`
- `POST /api/permission-changes`
- `GET /api/permission-changes/{id}`
- `POST /api/permission-changes/{id}/retry`

API thay đổi quyền nhận `businessGroupId`, `resourceId`, `targetPermissionLevel`, lý do và thông tin phê duyệt nếu có. Server tự xác định quyền hiện tại và Permission Group AD thực tế; client không được tự chọn mã nhóm AD.

### Import Excel

- `POST /api/import-jobs` — tải workbook lên.
- `POST /api/import-jobs/{id}/parse` — đọc các cột và dòng.
- `POST /api/import-jobs/{id}/validate` — kiểm tra dữ liệu và resolve tham chiếu.
- `GET /api/import-jobs/{id}/validation-results` — trả kết quả theo từng dòng.
- `POST /api/import-jobs/{id}/generate-changes` — tạo tập thay đổi để xem trước.
- `POST /api/import-jobs/{id}/apply` — áp dụng các thay đổi đã được duyệt qua AD.
- `GET /api/import-jobs/{id}` — lấy trạng thái và tổng kết job.

### Vận hành và audit

- `GET /api/audit-logs`
- `POST /api/synchronization/ad`
- `GET /api/reconciliation/discrepancies`

## 6. Luồng import Excel

```text
Tải file lên
 → Parse
 → Kiểm tra định dạng và mức quyền hợp lệ
 → Resolve Business Group và Resource
 → Đọc trạng thái hiện tại
 → So sánh trạng thái hiện tại với quyền mong muốn
 → Tạo tập PermissionChange
 → Review / phê duyệt (nếu bật)
 → Áp dụng thay đổi membership trên AD
 → Cập nhật trạng thái DB đã được AD xác nhận
 → Ghi audit log và báo cáo import
```

Cách xử lý từng dòng import:

- `NONE`: bảo đảm Business Group không thuộc cả Permission Group READ lẫn MODIFY của Resource.
- `READ`: bảo đảm Business Group chỉ thuộc Permission Group READ.
- `MODIFY`: bảo đảm Business Group chỉ thuộc Permission Group MODIFY.
- Nếu quyền mong muốn bằng quyền hiện tại, đánh dấu dòng `NO_CHANGE` và không gọi AD.
- Nếu không resolve được Business Group hoặc Resource, lưu validation error theo dòng và không áp dụng dòng đó.
- Phải có bước validation-only/dry-run trước khi cho phép ghi thay đổi vào AD.

## 7. Chiến lược transaction và xử lý lỗi

CSDL và Active Directory không thể dùng chung một transaction nguyên tử. Vì vậy ứng dụng sử dụng durable workflow/saga, không giả định DB transaction khiến quyền Windows có hiệu lực.

### Trình tự xử lý

1. Dùng DB transaction ngắn để lưu yêu cầu/import, kết quả validation và `PermissionChange` ở trạng thái `PENDING`.
2. Commit DB transaction.
3. Worker áp dụng các thao tác membership dự kiến lên AD.
4. Sau khi AD xác nhận thành công, dùng DB transaction mới để cập nhật `PermissionAssignment`, `PermissionChange` và `AuditLog`.
5. Khi lỗi xảy ra, giữ đầy đủ bản ghi thay đổi để retry và đối soát với trạng thái thực tế trên AD.

### Trạng thái đề xuất

```text
DRAFT → VALIDATED → PENDING → APPLYING → APPLIED
                                  ├→ FAILED
                                  ├→ PARTIALLY_APPLIED
                                  └→ RETRY_PENDING
```

### Biện pháp an toàn vận hành

- Dùng idempotency key để retry không sinh thay đổi membership trùng lặp hoặc không an toàn.
- Tuần tự hóa thay đổi trên cùng cặp `(BusinessGroup, Resource)`.
- Áp dụng timeout, retry có giới hạn và exponential backoff cho lỗi AD tạm thời.
- Với thay đổi `READ → MODIFY` bị thực hiện một phần, phải lưu chính xác thao tác đã hoàn tất. Ưu tiên rollback an toàn về READ nếu chính sách cho phép và trạng thái có thể khôi phục đáng tin cậy.
- `AuditLog` là append-only, có actor, lý do, dữ liệu trước/sau, kết quả và correlation ID.
- UI/báo cáo cần phân biệt rõ: `đã yêu cầu trong DB`, `đã áp dụng trên AD` và `có sai lệch khi đối soát`; quyền hiển thị là hiệu lực phải dựa trên dữ liệu AD đã đồng bộ gần nhất.

## 8. Cấu trúc project đề xuất

```text
src/
  domain/
    business-group/
    user/
    resource/
    permission-group/
    permission-assignment/
    permission-change/
    import-job/
    audit/

  application/
    commands/
      request-permission-change/
      apply-permission-change/
      import-permissions/
      synchronize-ad/
      reconcile-permissions/
    queries/
      get-effective-permission/
      search-permissions/

  infrastructure/
    persistence/
      repositories/
      migrations/
    active-directory/
      active-directory-gateway/
      ldap-or-powershell-client/
    excel/
      parser/
      validator/
    jobs/
      change-processor/
      reconciliation-job/

  api/
    controllers/
    dto/
    middleware/

  shared/
    errors/
    audit/
    idempotency/
    authorization/
```

## 9. Kế hoạch triển khai theo giai đoạn

1. Chốt quy ước đặt tên Business Group/Permission Group trên AD, quy tắc định danh Resource và ownership của UNC path.
2. Triển khai schema, migration và dữ liệu tham chiếu cho `NONE`, `READ`, `MODIFY`.
3. Xây dựng đồng bộ AD chỉ-đọc để lập inventory User, Business Group, Permission Group và nested membership hiện có.
4. Xây dựng tìm kiếm, màn hình xem quyền, báo cáo và tra cứu audit từ CSDL.
5. Triển khai workflow đổi một quyền, có dry-run, validation, audit và không ghi lại NTFS ACL trực tiếp.
6. Triển khai AD worker, chính sách retry và quy trình đối soát.
7. Triển khai parse, validation, preview, phê duyệt và apply Excel import.
8. Bổ sung RBAC, quy tắc phê duyệt, dashboard và cảnh báo vận hành.
9. Kiểm thử trên AD Organizational Unit phi production và ACL file share đại diện trước khi rollout production.

## 10. Các quyết định cần xác nhận trước khi triển khai

1. Có yêu cầu phê duyệt trước khi áp dụng thay đổi quyền lên AD không? Nếu có, vai trò nào được tạo yêu cầu và vai trò nào được phê duyệt?
2. Với Excel import, các dòng hợp lệ có được áp dụng khi có dòng lỗi, hay phải từ chối toàn bộ file?
3. Resource sẽ được quản lý từ nguồn nào: nhập tay, discovery từ file server, hay CMDB/hệ thống nguồn khác?
4. Cơ chế tích hợp AD và service account sẽ là LDAP, PowerShell Remoting hay worker/service chuyên dụng?
