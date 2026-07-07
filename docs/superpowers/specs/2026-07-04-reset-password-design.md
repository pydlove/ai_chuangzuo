# 重置密码功能 设计 (2026-07-04)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 /forgot 页面提供完整的前后端重置密码流程，含人机验证、邮箱验证码校验、密码更新、旧 token 作废。

**Architecture:** 复用现有 `/api/v1/user/auth/*` 模块的 DTO / 校验器 / Service / 风格；走「弹框拖滑块 → 通过后调后端」的弹框模式（与 Login / Register 一致）。新增 1 个 endpoint + 1 个 Service 方法 + 1 个 DTO，扩展 `JwtUtil` 和 `AuthServiceImpl.refreshToken`。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Caffeine（已有）+ jjwt 0.12 + Vue 3 + Pinia + Axios + Ant Design Vue。

## Global Constraints

- 沿用 `UserAuthErrorCode` 既有错误码（CAPTCHA_ERROR / EMAIL_CODE_ERROR / USER_NOT_FOUND / PASSWORD_NOT_MATCH / REFRESH_TOKEN_INVALID），不新增
- 后端 mock 测试环境（`SPRING_PROFILES_ACTIVE=test`）保留 captcha mock code `TEST12`、邮箱码 mock code `000000`
- 密码 BCrypt 编码，与 register/login 同一 `PasswordEncoder` Bean
- 启动脚本：`scripts/local/user-full-stack/{start,stop,restart}.sh`（后端 25050 / 前端 22345）
- 重置日志复用 `u_user_login_log.login_type`：约定 `3 = 密码重置`，与 `1 = 登录 / 2 = 注册` 并存
- HTTP 路径对齐 `/api/v1/user/auth/reset-password`
- Java 包路径：`com.aichuangzuo.user.modules.auth.*`
- 前端 API 封装：`project/user/web/src/api/auth.js`，新增 `resetPassword(data)` 导出
- 与 Login.vue 保持相同的 slider-modal 弹框模式（避免歧义、不强制旧代码改）

---

## 设计要点

### 1. 接口

```
POST /api/v1/user/auth/reset-password
Headers: 无（公开接口）
Body: {
  "email":            "<email>",
  "emailCode":        "<6 digits>",
  "password":         "<6-20 chars>",
  "confirmPassword":  "<6-20 chars>",
  "captchaKey":       "<uuid>",
  "captchaCode":      "<mock: TEST12>"
}

200 OK → Result<Void>

错误码（沿用）：
- CAPTCHA_ERROR          captchaKey/code 不匹配或过期
- EMAIL_CODE_ERROR       邮箱码错误或已使用（一次性）
- USER_NOT_FOUND         邮箱未注册
- PASSWORD_NOT_MATCH     password != confirmPassword
```

### 2. 后端新增

#### 2.1 DTO

`/project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/dto/request/ResetPasswordRequest.java`
- `@Email @NotBlank private String email;`
- `@NotBlank private String emailCode;`
- `@NotBlank @Size(min = 6, max = 20) private String password;`
- `@NotBlank @Size(min = 6, max = 20) private String confirmPassword;`
- `@NotBlank private String captchaKey;`
- `@NotBlank private String captchaCode;`

#### 2.2 Service

`AuthService` 接口新增：
```java
void resetPassword(ResetPasswordRequest request, String clientIp);
```

`AuthServiceImpl.resetPassword()` 流程：
```
1. captchaService.validateCaptcha(captchaKey, captchaCode)
   → 抛 CAPTCHA_ERROR
2. if (!password.equals(confirmPassword)) 抛 PASSWORD_NOT_MATCH
3. User user = userMapper.selectByEmail(email)
   if (user == null) 抛 USER_NOT_FOUND   // 不静默，与 register/login 行为一致
4. if (!emailCodeService.validateEmailCode(email, emailCode))
   抛 EMAIL_CODE_ERROR
5. passwordEncoder.encode(newPassword) → userMapper.updatePassword(user.id, hash)
6. cacheUtil.set("user:auth:password-reset-at:" + user.id,
                new Date(),
                authProperties.getJwt().getRefreshExpiration() /* 7 天，秒 */,
                TimeUnit.SECONDS);
7. saveLoginLog(user.id, 3 /* 重置 */, ip, ua, 1, null)
8. return;  // Void
```

#### 2.3 Controller

`AuthController` 新增：
```java
@PostMapping("/reset-password")
@Operation(summary = "重置密码")
public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                  HttpServletRequest httpRequest) {
    authService.resetPassword(request, getClientIp(httpRequest));
    return Result.success();
}
```

#### 2.4 Mapper

`UserMapper` 新增：
```java
@Update("UPDATE u_user SET password_hash = #{hash}, updated_at = NOW() WHERE id = #{id}")
int updatePassword(@Param("id") Long id, @Param("hash") String hash);
```

#### 2.5 JwtUtil 扩展

新增方法以支持 refresh 端校验 token 签发时间：
```java
/**
 * 解析 refresh token 签发时间（用于密码重置后的失效判断）
 */
public Date getRefreshTokenIssuedAt(String token) {
    SecretKey key = Keys.hmacShaKeyFor(
        authProperties.getJwt().getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token).getPayload();
    return claims.getIssuedAt();
}
```

#### 2.6 refreshToken 校验增强

`AuthServiceImpl.refreshToken()` 头部加：
```java
Long userId = jwtUtil.parseRefreshToken(request.getRefreshToken());

// 校验是否在密码重置之后签发
Date resetAt = cacheUtil.get("user:auth:password-reset-at:" + userId);
if (resetAt != null) {
    Date tokenIat = jwtUtil.getRefreshTokenIssuedAt(request.getRefreshToken());
    if (tokenIat == null || !tokenIat.after(resetAt)) {
        throw new BusinessException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
    }
}

User user = userMapper.selectById(userId);
// ... existing code
```

### 3. 前端

#### 3.1 `api/auth.js` 新增

```js
export const resetPassword = (data) => request.post('/auth/reset-password', data);
```

#### 3.2 `Forgot.vue` 改造

**新增响应式状态**：
```js
const sliderModalVisible = ref(false)
const sliderModalPassed = ref(false)
let modalSending = false
const captchaKey = ref('')
const SLIDER_CAPTCHA_VALUE = 'TEST12'
```

**新增滑块弹框**（template 末尾、登录弹框前）：
```vue
<a-modal v-model:open="sliderModalVisible" title="人机验证" :footer="null"
         :mask-closable="false" :keyboard="false" width="420px"
         class="slider-modal slider-modal-reset">
  <p class="slider-modal-tip">
    拖动滑块完成验证后将重置账号
    <b v-if="form.email">「{{ form.email }}」</b> 的密码
  </p>
  <SliderCaptcha v-model="sliderModalPassed" />
</a-modal>
```

**改造 `handleReset`**：
```js
const handleReset = async () => {
  // 前端基础校验
  if (form.password !== form.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  if (!form.email || !form.code || !form.password) {
    message.warning('请完整填写表单')
    return
  }
  if (codeCountdown.value <= 0) {
    message.warning('请先获取邮箱验证码')
    return
  }
  // 打开滑块弹框
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    sliderModalPassed.value = false
    sliderModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

// 滑块通过 → 调重置接口
watch(sliderModalPassed, async (val) => {
  if (!val || modalSending) return
  modalSending = true
  try {
    await resetPasswordApi({
      email: form.email,
      emailCode: form.code,
      password: form.password,
      confirmPassword: form.confirmPassword,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE,
    })
    message.success('密码已重置，请重新登录')
    sliderModalVisible.value = false
    router.push('/login')
  } catch (err) {
    message.error(err?.message || '重置失败')
    sliderModalVisible.value = false
  } finally {
    modalSending = false
  }
})
```

**发送邮箱验证码**：沿用现有 `openCode` 模式（点「获取验证码」前先调 getCaptcha + 拖滑块 + 通过后再 sendEmailCode）。

### 4. 验证流程

`tests/e2e/verify_reset_password.py`：

```
前置：
  - 后端 SPRING_PROFILES_ACTIVE=test 启动
  - 前端 22345 运行

场景 A（mock 验证码：captcha=TEST12, email=000000）：
  1. POST /api/v1/user/auth/register   注册一个新用户 → 拿到 token
  2. 调用 logout 清空 token
  3. 浏览器打开 /forgot → 输入新邮箱 → 点获取验证码 → 弹框拖滑块 → 通过
  4. 填 mock 邮箱码 000000 + 新密码 NewPass123
  5. 点重置 → 弹框拖滑块 → 通过 → 调 resetPassword
  6. 验证：URL 跳转到 /login
  7. /login 用新密码登录成功 → /console
  8. localStorage 拿到新 token
  9. 退出登录

场景 B（refresh token 失效验证）：
  1. 用户 A 登录（拿到 refresh_token_1）
  2. 重置 A 的密码（场景 A 流程）
  3. 调用 POST /api/v1/user/auth/refresh-token 带 refresh_token_1
  4. 期望：返回 REFRESH_TOKEN_INVALID / 401
  5. 调用 POST /api/v1/user/auth/login 用新密码
  6. 拿到新的 refresh_token_2
  7. 调用 refresh-token 带 refresh_token_2 → 200 OK
```

### 5. 不在范围内 (YAGNI)

- 不做「重置成功邮件通知」（已有日志记录，可后续补）
- 不做「登录验证码」（沿用现状）
- 不做「修改密码」（登录态修改，功能独立，留作下一迭代）
- 不做「国际化文案」（沿用现有中文）
- 不动 Admin 端
- 不动邀请码 / 创作币规则

---

## Self-Review

1. **Placeholder scan** — 无 TBD / TODO；邮箱码 mock code 与现有 test profile 一致；端口与现有脚本一致。
2. **Internal consistency** — 错误码全部复用，`password-reset-at` cache key 与 refresh TTL 一致；步骤编号 1-5 与端点调用顺序匹配。
3. **Scope check** — 单 endpoint + 单 Service 方法 + 单 DTO + 2 个工具扩展，符合「单个实施计划」颗粒度。
4. **Ambiguity check** — 接口路径、参数、错误码、用户体验路径均已明确；mock code 与现有策略一致。
