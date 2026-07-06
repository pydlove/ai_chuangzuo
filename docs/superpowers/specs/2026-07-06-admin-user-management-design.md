# 管理端注册用户管理设计

## 背景

管理控制台需要查看并管理用户端注册的普通用户，包括：列表查询、详情查看、启用/禁用、重置密码。前端页面 `UserListView.vue` 与组合式函数 `useUserManagement.js` 已实现，需补齐后端接口并联调。

## 目标

- 为管理端提供注册用户 CRUD 管理能力。
- 与现有前端 mock 字段、交互保持一致。
- 仅允许超级管理员访问。

## 非目标

- 不新增用户端注册字段（如独立 username）。
- 不改造用户端认证流程。
- 不做复杂的 RBAC 权限控制（仅校验 SUPER_ADMIN）。

## 方案选型

采用 **admin/api 直接查询用户库表** 方案：

- admin/api 与 user/api 共用同一 MySQL 实例。
- admin/api 直接操作 `u_user`、`u_user_login_log` 表。
- 优点：实现快、不改动 user/api、联调路径短。
- 缺点：admin 模块感知用户表结构；用户表结构大改时需同步。

## 接口设计

### 1. 查询用户列表

```
GET /api/v1/admin/users?page=1&pageSize=10&keyword=xxx
```

**请求参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词，支持邮箱、昵称、邀请码模糊匹配 |
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |

**响应数据**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "account": "user@example.com",
        "email": "user@example.com",
        "nickname": "夜雨微凉",
        "status": "enabled",
        "inviteCode": "ABC123",
        "createdAt": "2026-01-15 10:23:45",
        "lastLoginAt": "2026-07-05 18:30:00"
      }
    ],
    "total": 50
  }
}
```

### 2. 查看用户详情

```
GET /api/v1/admin/users/{id}
```

**响应数据**：与列表项字段一致。

### 3. 修改用户状态

```
PATCH /api/v1/admin/users/{id}/status
```

**请求体**

```json
{
  "status": "enabled"
}
```

`status` 取值：`enabled` | `disabled`。

### 4. 重置用户密码

```
POST /api/v1/admin/users/{id}/reset-password
```

**响应数据**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "newPassword": "adc123456"
  }
}
```

重置后的密码固定为 `adc123456`，使用 BCrypt 加密后写入 `u_user.password_hash`。

## 数据模型

admin/api 内新建实体以映射用户端表：

### PlatformUser

对应 `u_user` 表：

| 字段 | 说明 |
|------|------|
| id | 主键 |
| bizNo | 业务编号 |
| nickname | 昵称 |
| email | 邮箱（账号） |
| passwordHash | 密码哈希 |
| inviteCode | 邀请码 |
| userStatus | 0-禁用，1-正常 |
| createdAt | 注册时间 |

### PlatformUserLoginLog

对应 `u_user_login_log` 表，仅用于取用户最近一次成功登录时间。

## 后端结构

```
project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/
├── controller/
│   └── AdminUserController.java
├── service/
│   ├── AdminUserService.java
│   └── impl/
│       └── AdminUserServiceImpl.java
├── entity/
│   ├── PlatformUser.java
│   └── PlatformUserLoginLog.java
├── mapper/
│   ├── PlatformUserMapper.java
│   └── PlatformUserLoginLogMapper.java
├── dto/
│   └── request/
│       └── AdminUserStatusRequest.java
└── vo/
    ├── AdminUserVO.java
    └── AdminUserPageVO.java
```

同时补充：

- `RoleMapper.java`：按 `role_code` 查询角色。
- `AdminUserRoleRelMapper.java`：查询管理员角色关联。
- 错误码：`AdminUserErrorCode`（22xxxx 范围）。

## 权限控制

- 所有 `/api/v1/admin/users/**` 接口要求当前登录管理员拥有 `SUPER_ADMIN` 角色。
- 通过 `SecurityAdminContext.getCurrentAdminUserId()` 获取当前管理员 ID。
- 查询 `a_admin_user_role_rel` 与 `a_role` 表完成校验。
- 无权限时返回 `ADMIN_USER_NO_PERMISSION`。

## 关键业务逻辑

### 列表查询

1. 校验 SUPER_ADMIN 权限。
2. 构建 MyBatis-Plus 查询：
   - 过滤 `is_deleted = 0`。
   - keyword 非空时，对 `email`、`nickname`、`invite_code` 模糊匹配（OR 关系）。
3. 分页返回，按 `created_at DESC` 排序。

### 用户详情

1. 校验 SUPER_ADMIN 权限。
2. 按 ID 查 `u_user`，不存在则抛 `ADMIN_USER_NOT_FOUND`。
3. 取最近一次 `login_status = 1` 的登录时间作为 `lastLoginAt`。

### 状态切换

1. 校验 SUPER_ADMIN 权限。
2. 校验用户存在。
3. `enabled` → `userStatus = 1`；`disabled` → `userStatus = 0`。
4. 更新 `u_user`。

### 重置密码

1. 校验 SUPER_ADMIN 权限。
2. 校验用户存在。
3. 使用 `PasswordEncoder` 加密 `"adc123456"`。
4. 更新 `u_user.password_hash`。
5. 返回明文 `adc123456` 给管理员。

## 错误码

| 错误码 | 含义 |
|--------|------|
| 220001 | 用户不存在 |
| 220002 | 无权限访问 |
| 220003 | 状态参数非法 |

## 联调要点

- 前端 `src/api/user.js` 需替换 mock 为真实 API。
- 前端 `account` 字段使用后端返回的 `email`。
- 分页参数 `page`、`pageSize` 与后端对齐。
- 开发时使用 `scripts/local/admin-full-stack/start.sh` 一键启动 admin-api + admin-web。

## 风险与假设

- admin/api 直接依赖 `u_user` 表结构；若用户端后续对用户表做破坏性变更，admin 模块需同步。
- 重置密码返回明文，管理员需安全告知用户；后续可考虑强制用户首次登录修改密码。
