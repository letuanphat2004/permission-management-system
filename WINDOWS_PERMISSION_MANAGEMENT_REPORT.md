# Báo cáo quản lý quyền Windows

## 1. Cơ chế Windows xác thực người dùng

Người dùng đăng nhập bằng **Domain Account**. Windows gửi yêu cầu xác thực đến Domain Controller/Active Directory (AD). Sau khi xác thực, Windows tạo **Access Token** chứa SID của user và các group mà user thuộc về.

```text
User Login
    ↓
Windows
    ↓
Domain Controller / Active Directory
    ↓
Authentication
    ↓
User SID + Group Membership
    ↓
Windows Access Token
```

- AD/DC xác thực user và quản lý User, Group, SID, Group Membership.
- SID là định danh bảo mật của User/Group.
- Windows dùng các SID trong Access Token để kiểm tra quyền.

## 2. Kiểm tra quyền File/Folder

Ví dụ user mở `Z:\Projects\A.docx`:

```text
User
 ↓
Windows / SMB Client
 ↓
File Server
 ↓
Authenticated SMB Session
 ↓
Share Permission
 ↓
NTFS ACL
 ↓
Compare User/Group SID with ACL
 ↓
Effective Permission → Allow / Access Denied
```

- File Server nhận diện user qua phiên SMB đã xác thực.
- ACL chứa SID của User/Group; Windows đối chiếu với Security Context.
- Khi truy cập network share, quyền hiệu lực cần xét cả **Share Permission** và **NTFS Permission**.

## 3. User, Group, SID và Inheritance

Nên cấp quyền cho **AD Group** thay vì cấp trực tiếp cho từng user.

```text
User: Phat
     ↓ Member of
ProjectA-Developer
     ↓ Modify
ProjectA
     ↓ Inherited
 ┌───────┬───────────┬─────────┐
Source   Documents   Release
```

- User có thể nhận quyền qua Group.
- Quyền từ folder cha có thể kế thừa xuống folder con.
- Web cần phân biệt: **Direct Permission**, **Inherited Permission**, và **Permission through Group**.

## 4. Vấn đề Web cần giải quyết

Folder tree có thể sâu, path dài và dữ liệu quyền lớn nên khó quản lý bằng Windows GUI.

```text
Data
└── Department
    └── IT
        └── Projects
            └── ProjectA
                └── Backend
                    └── Documents
```

- Khó xác định user nào có quyền ở đâu, trực tiếp hay qua group.
- Khó theo dõi inheritance, Import/Export và Audit.
- Path dài: Windows truyền thống có giới hạn `MAX_PATH`; backend phải dùng API/runtime hỗ trợ Long Path khi cần.
- Folder tree trên Web nên dùng **lazy loading**, không tải toàn bộ cây thư mục.

## 5. Kiến trúc Web Application đề xuất

```text
                Admin
                  ↓
         Web Frontend (React / TypeScript)
                  ↓
       Backend / Permission Engine
          ┌───────┼────────┐
          ↓       ↓        ↓
         AD   File Server  DB
       User/   Folder/    Audit,
       Group    ACL      Config
        SID
```

| Thành phần | Source of truth |
| --- | --- |
| Active Directory | User, Group, SID, Group Membership |
| File Server | Folder structure, NTFS ACL, Permission |
| Database | Audit log, history, configuration, Import/Export history, cache |

Database không phải source of truth cho quyền thực tế.

## 6. Cách Web thay đổi Permission

### Thay đổi quyền Folder

```text
Admin → Web → Backend → Resolve User/Group to SID
                         ↓
                  Read Current ACL
                         ↓
                  Modify ACL → File Server
                         ↓
                  Verify → Audit Log
```

Quyền phải được áp dụng vào **NTFS ACL trên File Server**, không chỉ ghi Database.

### Thay đổi User thuộc Group

```text
Admin → Web → Backend → Active Directory
                         ↓
                 Add / Remove User from Group
                         ↓
                    Verify → Audit
```

Group Membership phải được áp dụng vào **Active Directory**.

## 7. Import / Export

```text
Import File → Parse → Validate → Preview Changes → Admin Confirm
                                                    ↓
                                                  Apply
                                           ┌────────┴────────┐
                                           ↓                 ↓
                                          AD          File Server ACL
                                           └────────┬────────┘
                                                    ↓
                                              Verify → Audit
```

```text
AD + File Server → Backend → Normalize Data → Export File
```

- Import không chỉ ghi Database.
- Group Membership áp dụng vào AD; Folder Permission áp dụng vào File Server ACL.
- Cần Validate và Preview trước khi Admin Apply.

## 8. Hai màn hình chính

### Permission Management

```text
┌─────────────────────────┬──────────────────────────────────┐
│ Folder Tree             │ Selected Folder                  │
│ ▼ Data                  │ User/Group | Permission | Source │
│   ▼ Projects            │ -------------------------------- │
│     ▼ ProjectA          │ Dev Group  | Modify | Direct     │
│       Source            │ IT Admin   | Full   | Inherited  │
│       Documents         │                                  │
└─────────────────────────┴──────────────────────────────────┘
```

- Browse/Search folder; xem và Add/Remove/Update quyền.
- Hiển thị rõ Direct/Inherited; nhận diện Deny khi đọc ACL, dù không nhất thiết expose thao tác Deny.

### User / Group Permission Overview

```text
Phat → ProjectA-Developer → Modify → ProjectA
                                      ├── Source
                                      ├── Documents
                                      └── Release
```

- Search User/Group; xem Group Membership.
- Xem quyền theo folder và nguồn quyền: trực tiếp hoặc qua Group.

## 9. Công nghệ Backend

```text
Option 1: React → .NET Backend → AD + File Server

Option 2: React → Java Backend → .NET Permission Service → AD + File Server
```

| Lựa chọn | Phù hợp |
| --- | --- |
| .NET | Tương tác trực tiếp Windows API, AD, SID, NTFS ACL, Security Descriptor |
| Java | REST API, business logic, database, LDAP/AD integration; phần Windows-specific có thể phức tạp hơn |

Nếu hệ thống phụ thuộc mạnh vào Windows/NTFS, ưu tiên **.NET cho Permission Engine**.

## 10. Kết luận đề xuất

| Thành phần | Source / Direction |
| --- | --- |
| User / Group / SID / Group Membership | Active Directory |
| Folder | File Server |
| Permission | NTFS ACL |
| Permission Strategy | Ưu tiên AD Group |
| Database | Audit / History / Config |
| Folder Tree | Lazy Loading |
| Long Path | Backend phải hỗ trợ |
| Import / Export | Apply vào AD + File Server |
| Backend | Ưu tiên .NET cho Windows Permission Engine |

```text
Windows Authentication
        ↓
AD User / Group / SID → Security Context → SMB
        ↓                                  ↓
Web Application → Backend / Permission Engine
        ├──────────────→ Active Directory (User / Group)
        ├──────────────→ File Server (Folder / ACL)
        └──────────────→ Audit Database
                           ↓
                 Share Permission → NTFS ACL → Real File Access
```
