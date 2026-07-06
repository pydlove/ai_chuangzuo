# 管理端登录功能设计规格

> 日期：2026-07-06  
> 范围：`project/admin/api/`、`project/admin/web/`、管理端数据库  
> 状态：待实现

---

## 1. 概述

为爱创作（AI Creation）管理控制台实现真实的登录认证能力。当前 `project/admin/web/src/views/LoginView.vue` 已存在，但使用前端本地图形验证码和 mock 账号；本设计将其接入后端，完成管理端账号体系、JWT 认证、登录安全与数据库初始化。

---

## 2. 目标与非目标

### 2.1 目标

- 提供管理端登录接口，内置超级管理员账号 `admin / Root1qaz!QAZ`。
- 前端登录页使用滑块弹框人机验证，验证通过后调用后端。
- 管理端 JWT 独立 Secret，与用户端完全隔离。
- 满足项目安全规范：BCrypt、登录失败锁定、IP 限流、登录日志。

### 2.2 非目标

- 不提供管理端注册、忘记密码、邮箱验证码功能。
- 不提供角色权限管理页面（表结构先建，页面后续再做）。
- 不实现「记住我」持久化登录（UI 可保留，但本次只做本地账号记忆）。

---

## 3. 接口清单

| 方法 | URL | 说明 |
|---|---|---|
| `POST` | `/api/v1/admin/auth/login` | 管理员账号密码登录 |
| `POST` | `/api/v1/admin/auth/refresh-token` | 刷新 accessToken |
| `POST` | `/api/v1/admin/auth/logout` | 退出登录（accessToken 加入黑名单） |

---

## 4. DTO / VO

### 4.1 请求 DTO

```java
// POST /api/v1/admin/auth/login
public class AdminLoginRequest {
    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 64, message = "账号长度 2-64 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;
}

// POST /api/v1/admin/auth/refresh-token
public class AdminRefreshTokenRequest {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
```

### 4.2 响应 VO

```java
public class AdminAuthTokenVO {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn; // accessToken 有效期秒数，固定 7200
    private AdminUserVO user;
}

public class AdminUserVO {
    private Long id;
    private String username;
    private String realName;
    private String avatarUrl;
}
```

---

## 5. 错误码

沿用 `1XXYYY` 分段，`21` 为管理端认证：

| 错误码 | 含义 | 触发场景 |
|---|---|---|
| `211001` | 账号或密码错误 | 用户名不存在或密码不匹配 |
| `211002` | 账号已被禁用 | `status = 0` |
| `211003` | 登录已过期，请重新登录 | accessToken 无效、过期或在黑名单 |
| `211004` | refreshToken 无效或已过期 | refreshToken 校验失败 |
| `211005` | 操作过于频繁，请稍后再试 | 触发限流或账号锁定 |
| `100002` | 参数校验失败 | 请求字段校验不通过 |

---

## 6. 数据库设计

### 6.1 a_admin_user（管理员账号表）

```sql
CREATE TABLE IF NOT EXISTS a_admin_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(256) NOT NULL COMMENT '密码哈希（BCrypt）',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    last_login_at DATETIME(3) DEFAULT NULL COMMENT '最后登录时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_admin_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';
```

### 6.2 a_role（角色表）

```sql
CREATE TABLE IF NOT EXISTS a_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(256) DEFAULT NULL COMMENT '描述',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
```

### 6.3 a_admin_user_role_rel（管理员角色关联表）

```sql
CREATE TABLE IF NOT EXISTS a_admin_user_role_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_admin_user_role_rel (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员角色关联表';
```

### 6.4 a_permission（权限表）

```sql
CREATE TABLE IF NOT EXISTS a_permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    resource_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '资源类型：1-菜单，2-按钮，3-接口',
    parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
```

### 6.5 a_role_permission_rel（角色权限关联表）

```sql
CREATE TABLE IF NOT EXISTS a_role_permission_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_role_permission_rel (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
```

### 6.6 a_admin_login_log（管理员登录日志表）

```sql
CREATE TABLE IF NOT EXISTS a_admin_login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    admin_user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '管理员ID，0表示未登录',
    login_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型：1-密码登录',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    login_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    fail_reason VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_a_admin_login_log_admin_user_id (admin_user_id),
    KEY idx_a_admin_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员登录日志表';
```

### 6.7 初始化数据

```sql
-- 超级管理员角色
INSERT INTO a_role (role_code, role_name, description, status) VALUES
('SUPER_ADMIN', '超级管理员', '拥有全部权限，不可删除', 1);

-- 内置超级管理员账号 admin / Root1qaz!QAZ（BCrypt 强度 12）
INSERT INTO a_admin_user (username, password_hash, real_name, status, created_by, updated_by) VALUES
('admin', '$2a$12$..., hash of Root1qaz!QAZ', '超级管理员', 1, 0, 0);

-- 关联角色
INSERT INTO a_admin_user_role_rel (admin_user_id, role_id) VALUES
(LAST_INSERT_ID(), (SELECT id FROM a_role WHERE role_code = 'SUPER_ADMIN'));
```

> 注：初始化 SQL 中的 `password_hash` 需用 BCrypt 实际哈希值替换；实施时可通过代码生成后回填，或在迁移脚本中直接写入预生成的哈希。

---

## 7. 后端设计

### 7.1 模块结构

```text
project/admin/api/
├── pom.xml
├── src/main/java/com/aichuangzuo/admin/
│   ├── AdminApiApplication.java
│   ├── config/
│   │   ├── AuthProperties.java
│   │   ├── CaffeineConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebMvcConfig.java
│   ├── common/
│   │   ├── exception/handler/GlobalExceptionHandler.java
│   │   └── interceptor/RateLimitInterceptor.java
│   ├── infrastructure/
│   │   ├── cache/CacheUtil.java
│   │   ├── persistence/handler/MybatisPlusMetaObjectHandler.java
│   │   └── security/
│   │       ├── JwtAuthenticationFilter.java
│   │       ├── JwtUtil.java
│   │       └── SecurityAdminContext.java
│   └── modules/
│       └── auth/
│           ├── controller/AdminAuthController.java
│           ├── converter/AdminAuthConverter.java
│           ├── dto/request/AdminLoginRequest.java
│           ├── dto/request/AdminRefreshTokenRequest.java
│           ├── entity/AdminUser.java
│           ├── entity/AdminLoginLog.java
│           ├── entity/Role.java
│           ├── entity/AdminUserRoleRel.java
│           ├── mapper/AdminUserMapper.java
│           ├── mapper/AdminLoginLogMapper.java
│           ├── service/AdminAuthService.java
│           ├── service/impl/AdminAuthServiceImpl.java
│           └── vo/AdminAuthTokenVO.java
│           └── vo/AdminUserVO.java
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    └── db/migration/V1.0.0_001__create_admin_auth_tables.sql
```

### 7.2 登录流程

```
前端滑块验证通过
        ↓
POST /api/v1/admin/auth/login
        ↓
校验 IP 登录限流（每分钟 10 次）
        ↓
校验账号是否被锁定（5 分钟 5 次失败锁定 30 分钟）
        ↓
查询 a_admin_user by username
        ↓
校验密码（BCrypt）
        ↓
校验 status = 1
        ↓
清除失败次数、更新 last_login_at
        ↓
记录登录日志 login_status=1
        ↓
生成 accessToken / refreshToken
        ↓
返回 AdminAuthTokenVO
```

### 7.3 JWT 与认证

- 独立 Secret：`ADMIN_JWT_SECRET`，环境变量注入，长度 ≥ 256 位。
- accessToken 有效期 2 小时，refreshToken 有效期 7 天。
- Token 在 `Authorization: Bearer {token}` 中传递。
- `JwtAuthenticationFilter` 解析 accessToken，将管理员 ID 写入 `SecurityAdminContext`。
- 退出时 accessToken 加入 Caffeine 黑名单，TTL = Token 剩余有效期。

### 7.4 限流与锁定

| 维度 | 规则 | Key |
|---|---|---|
| IP 限流 | 每分钟 10 次 | `admin:auth:login-rate:{ip}` |
| 账号失败次数 | 5 分钟内 5 次失败 | `admin:auth:login-fail:{username}` |
| 账号锁定 | 失败达 5 次后锁定 30 分钟 | `admin:auth:account-lock:{username}` |

### 7.5 缓存 Key

| 用途 | Key | TTL |
|---|---|---|
| 登录错误次数 | `admin:auth:login-fail:{username}` | 5 分钟 |
| 账号锁定 | `admin:auth:account-lock:{username}` | 30 分钟 |
| IP 登录限流 | `admin:auth:login-rate:{ip}` | 1 分钟 |
| Token 黑名单 | `admin:auth:token-blacklist:{jti}` | Token 剩余有效期 |

---

## 8. 前端适配

### 8.1 改造点

1. **移除本地图形验证码**：删除 `LoginView.vue` 中的 `captchaText`、`generateCaptcha`、`refreshCaptcha` 及相关 DOM。
2. **字段对齐**：前端表单字段从 `account` 改为 `username`，与后端 `AdminLoginRequest.username` 保持一致。
3. **接入滑块弹框**：点击登录按钮后先校验账号密码格式，再打开滑块验证弹框；验证通过后调用 `adminAuthLogin`。
3. **更新 `src/api/auth.js`**：删除 mock，调用真实后端接口。
4. **预填开发账号**：登录表单默认填写 `admin / Root1qaz!QAZ`（仅开发环境），避免测试时反复输入。
5. **Token 存储**：保存 `accessToken` 和 `refreshToken`。
6. **请求拦截**：`request.js` 已携带 `Authorization: Bearer {token}`，无需改动。
7. **路由守卫**：已根据 token 判断登录态，无需改动。

### 8.2 登录交互流程

```
用户点击登录
        ↓
校验账号、密码非空
        ↓
打开滑块验证弹框（mask-closable=false）
        ↓
滑块验证通过
        ↓
调用 POST /api/v1/admin/auth/login
        ↓
成功：保存 token + userInfo，跳转 /console/users
失败：message.error，关闭弹框，不清空账号密码（表单仍保留预填的 admin/Root1qaz!QAZ）
```

---

## 9. 边界情况

| 场景 | 处理 |
|---|---|
| 账号不存在 | 统一返回 `211001` |
| 密码错误 | 累计失败次数，统一返回 `211001`；达到 5 次锁定 30 分钟 |
| 账号被禁用 | 返回 `211002` |
| 触发 IP 限流 | 返回 `429` + `211005` |
| accessToken 过期 | 返回 `211003`，前端跳转登录页 |
| refreshToken 过期 | 返回 `211004`，前端跳转登录页 |

---

## 10. 验收标准

- [ ] 后端启动后 Flyway 自动建表并插入 `admin` 账号。
- [ ] `POST /api/v1/admin/auth/login` 正确账号密码返回 token。
- [ ] 错误密码 5 次后账号锁定 30 分钟。
- [ ] 同一 IP 每分钟超过 10 次登录返回 `429`。
- [ ] 退出后原 accessToken 无法继续使用。
- [ ] 前端登录页移除图形验证码，改为滑块弹框。
- [ ] 前端登录成功后跳转 `/console/users`。
- [ ] 前端在未登录状态下访问 `/console` 自动跳 `/login`。
- [ ] 管理端 JWT Secret 与用户端不同。

---

## 11. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-07-06 | v1.0 | 初稿：管理端登录认证、数据库初始化、JWT、滑块验证 |
