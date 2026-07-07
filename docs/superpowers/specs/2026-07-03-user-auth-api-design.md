# 用户端登录注册退出接口设计

## 背景

爱创作用户端 Vue 控制台（`project/user/web/`）已有登录注册页面 `Login.vue`，当前为前端 mock 状态。为支撑真实账号体系，设计用户端后端认证接口，覆盖：图形验证码、邮箱验证码、注册、登录、Token 刷新、退出。

## 目标

1. 提供用户端完整的认证接口（登录、注册、退出、刷新 Token）。
2. 接入邮箱验证码完成注册验证。
3. 支持注册时填写邀请码，建立邀请关系。
4. 满足项目安全规范：JWT、BCrypt、登录限流、Token 黑名单。
5. 通过 IP 永久注册上限 + 邮箱验证控制批量注册风险。

## 非目标

- 不支持手机号登录 / 短信验证码。
- 不支持微信 / 第三方 OAuth 登录。
- 不实现真实邮件发送服务（接口预留，本期可先 mock 或控制台输出）。
- 不实现设备指纹（后续可扩展）。
- 不实现"忘记密码"接口（本期只覆盖登录、注册、退出）。

## 接口清单

| 方法 | URL | 说明 |
|---|---|---|
| `GET` | `/api/v1/user/auth/captcha` | 获取图形验证码 |
| `POST` | `/api/v1/user/auth/email-codes` | 发送邮箱验证码 |
| `POST` | `/api/v1/user/auth/register` | 邮箱注册（自动登录，返回 token） |
| `POST` | `/api/v1/user/auth/login` | 邮箱密码登录 |
| `POST` | `/api/v1/user/auth/refresh-token` | 刷新 accessToken |
| `POST` | `/api/v1/user/auth/logout` | 退出登录（accessToken 加入黑名单） |

## DTO / VO

### 请求 DTO

```java
// GET /api/v1/user/auth/captcha 无请求体

// POST /api/v1/user/auth/email-codes
public class SendEmailCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "图形验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}

// POST /api/v1/user/auth/register
public class RegisterRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码为 6 位")
    private String emailCode;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Size(max = 6, message = "邀请码最多 6 位")
    private String inviteCode;
}

// POST /api/v1/user/auth/login
public class LoginRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "图形验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}

// POST /api/v1/user/auth/refresh-token
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
```

### 响应 VO

```java
public class CaptchaVO {
    private String captchaKey;
    private String captchaImage; // data:image/png;base64,...
}

public class AuthTokenVO {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn; // accessToken 有效期秒数，固定 7200
    private UserVO user;
}

public class UserVO {
    private Long id;
    private String bizNo;
    private String nickname;
    private String email; // 已脱敏
    private String avatarUrl;
}
```

## 错误码

沿用 `1XXYYY` 分段，`11` 为用户端认证：

| 错误码 | 含义 | 触发场景 |
|---|---|---|
| `111001` | 邮箱格式错误 | 邮箱参数校验失败 |
| `111002` | 图形验证码错误 | captchaKey 不存在或 captchaCode 不匹配 |
| `111003` | 邮箱验证码错误或已过期 | emailCode 不存在、不匹配或已过期 |
| `111004` | 账号或密码错误 | 登录时邮箱不存在或密码不匹配（统一提示） |
| `111005` | 账号已被禁用 | user_status = 0 |
| `111006` | 邮箱已注册 | 注册时邮箱已存在 |
| `111007` | 两次密码不一致 | password ≠ confirmPassword |
| `111008` | 密码格式不符合要求 | 不在 6-20 位范围内 |
| `111009` | 邀请码无效 | 邀请码不存在 |
| `111010` | 登录已过期，请重新登录 | accessToken 无效、过期或在黑名单 |
| `111011` | refreshToken 无效或已过期 | refreshToken 校验失败 |
| `111012` | 操作过于频繁，请稍后再试 | 触发限流或 IP 注册上限 |

## 数据库表设计

### u_user（用户表）

```sql
CREATE TABLE IF NOT EXISTS u_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '用户唯一编号',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    email VARCHAR(128) NOT NULL COMMENT '邮箱',
    password_hash VARCHAR(256) NOT NULL COMMENT '密码哈希（BCrypt）',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    invite_code VARCHAR(16) DEFAULT NULL COMMENT '个人邀请码',
    user_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    email_verified TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '邮箱是否验证：0-否，1-是',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_biz_no (biz_no),
    UNIQUE KEY uk_u_user_email (email),
    UNIQUE KEY uk_u_user_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

### u_user_login_log（登录日志表）

```sql
CREATE TABLE IF NOT EXISTS u_user_login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID，0表示未登录',
    login_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型：1-密码登录，2-注册登录',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    login_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    fail_reason VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_u_user_login_log_user_id (user_id),
    KEY idx_u_user_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';
```

### u_user_invite_relation（邀请关系表）

```sql
CREATE TABLE IF NOT EXISTS u_user_invite_relation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    inviter_id BIGINT UNSIGNED NOT NULL COMMENT '邀请人ID',
    invitee_id BIGINT UNSIGNED NOT NULL COMMENT '被邀请人ID',
    invite_code VARCHAR(16) NOT NULL COMMENT '邀请码',
    source_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '来源：1-链接，2-手动填写',
    effective_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待验证，1-有效，2-无效',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_invite_relation_invitee_id (invitee_id),
    KEY idx_u_user_invite_relation_inviter_id (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户邀请关系表';
```

### u_ip_register_limit（IP 注册累计限制表）

```sql
CREATE TABLE IF NOT EXISTS u_ip_register_limit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    client_ip VARCHAR(45) NOT NULL COMMENT '客户端IP',
    register_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计成功注册数',
    is_blocked TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否永久封禁：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_ip_register_limit_client_ip (client_ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IP注册累计限制表';
```

## 安全与限流

### JWT 配置

- 用户端独立 Secret，环境变量注入，长度 ≥ 256 位。
- Access Token 有效期 2 小时，Refresh Token 有效期 7 天。
- Token 在 `Authorization: Bearer {token}` 中传递。
- 退出后 Access Token 写入 Caffeine 黑名单，TTL = Token 剩余有效期。

### 密码存储

- 使用 BCrypt 加密，强度因子 12。
- 禁止明文、MD5、SHA1 存储。

### 限流策略

| 接口 | 维度 | 限制 |
|---|---|---|
| `GET /api/v1/user/auth/captcha` | IP | 每分钟 30 次 |
| `POST /api/v1/user/auth/email-codes` | IP | 每分钟 5 次 |
| `POST /api/v1/user/auth/email-codes` | 邮箱 | 24 小时内最多 10 次 |
| `POST /api/v1/user/auth/login` | IP | 每分钟 10 次 |
| `POST /api/v1/user/auth/login` | 账号 | 5 分钟内密码错误 5 次，锁定 30 分钟 |
| `POST /api/v1/user/auth/register` | IP | 累计成功注册上限 10 个，达到后永久禁止该 IP 注册 |

触发限流统一返回 HTTP `429` + 业务码 `100003` 或 `111012`。

## 邀请码与注册奖励流程

```
用户填写 inviteCode 并点击注册
        ↓
校验 IP 注册上限
        ↓
校验邮箱是否已注册
        ↓
校验 inviteCode 是否存在（查 u_user.invite_code）
        ↓
校验 emailCode 正确
        ↓
校验 password == confirmPassword
        ↓
创建 u_user 记录，email_verified = 1，生成 invite_code
        ↓
若填写了 inviteCode：
  插入 u_user_invite_relation（inviter_id, invitee_id, invite_code, source_type=2, effective_status=0）
        ↓
调用钱包服务：被邀请人创作币 +5
        ↓
记录登录日志 login_type=2
        ↓
返回 AuthTokenVO，前端跳转 /console
```

说明：
- "自邀请"校验由前端在提交前完成（现有 `Login.vue` 已做），后端只校验邀请码是否存在。
- 邀请关系初始状态为 `0-待验证`，待后续风控扫描后改为 `1-有效` 再正式计入邀请人阶梯奖励。
- 创作币账户表 `creation_coin_account`、流水表 `creation_coin_transaction` 属于钱包模块，本次注册登录设计只触发"新用户注册奖励 +5 创作币"事件，具体表结构放到钱包实施计划中实现。

## 缓存 Key 设计

| 用途 | Key | TTL |
|---|---|---|
| 图形验证码 | `user:auth:captcha:{captchaKey}` | 5 分钟 |
| 邮箱验证码 | `user:auth:email-code:{email}` | 5 分钟 |
| 登录错误次数 | `user:auth:login-fail:{email}` | 5 分钟 |
| 账号锁定 | `user:auth:account-lock:{email}` | 30 分钟 |
| Token 黑名单 | `user:auth:token-blacklist:{jti}` | Token 剩余有效期 |

## 前端适配点

当前 `Login.vue` 的图形验证码为前端本地 mock，接入后端后需要：

1. 页面加载时调用 `GET /api/v1/user/auth/captcha`。
2. 把 `captcha-box` 从显示文字改为显示 `captchaImage`（base64）。
3. 点击刷新时重新获取验证码。
4. 登录请求携带 `captchaKey` 和 `captchaCode`。
5. 注册成功后直接使用返回的 `accessToken` / `refreshToken`，存入 Pinia / localStorage。
6. `handleLogout` 增加调用 `POST /api/v1/user/auth/logout`，并清除前端 token。

## 边界情况

| 场景 | 处理方案 |
|---|---|
| 同一 IP 已注册 10 个账号后再次注册 | 返回 `111012`，拒绝注册 |
| 邮箱已注册 | 返回 `111006`，提示邮箱已存在 |
| 邀请码不存在 | 返回 `111009`，提示邀请码无效 |
| 图形验证码过期 | 返回 `111002`，前端刷新验证码 |
| 邮箱验证码过期 | 返回 `111003`，提示重新获取 |
| 密码错误 5 次 | 账号锁定 30 分钟，返回 `111004` |
| accessToken 被加入黑名单后访问 | 返回 `111010` |
| refreshToken 过期 | 返回 `111011`，前端跳转登录页 |

## 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-07-03 | v1.0 | 初稿：登录、注册、退出、刷新 Token、图形/邮箱验证码、JWT、限流、邀请码、IP 注册上限 | - |
