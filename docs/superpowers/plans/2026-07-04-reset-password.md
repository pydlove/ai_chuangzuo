# 重置密码功能 实施计划 (2026-07-04)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/forgot` 接入完整的前后端重置密码链路（图形码 + 邮箱码 + 新密码），并做旧 refresh-token 失效。

**Architecture:** 复用现有 `/api/v1/user/auth/*` 模块；后端新增 1 个 endpoint + 1 个 Service 方法 + 1 个 DTO + 1 个 Mapper 方法；扩展 `JwtUtil.getRefreshTokenIssuedAt()` 与 `refreshToken()` 校验。前端 `Forgot.vue` 沿用「slider-modal 弹框拖滑块」模式，与 Login.vue 一致。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Caffeine + jjwt 0.12 + Spring MockMvc（后台）+ JUnit 5 + Vue 3 + Ant Design Vue + Playwright（E2E）。

## Global Constraints

- Java 包路径：`com.aichuangzuo.user.modules.auth.*`
- HTTP 路径：`/api/v1/user/auth/reset-password`
- mock 测试：`SPRING_PROFILES_ACTIVE=test` 时 captcha=`TEST12`、邮箱码=`000000`
- BCrypt 密码（与 register/login 同一 `PasswordEncoder` Bean）
- 错误码全部复用现有 `UserAuthErrorCode`：`CAPTCHA_ERROR` / `EMAIL_CODE_ERROR` / `USER_NOT_FOUND` / `PASSWORD_NOT_MATCH` / `REFRESH_TOKEN_INVALID`
- 启动脚本：`scripts/local/user-full-stack/{start,stop,restart}.sh`，后端 25050 / 前端 22345
- 重置日志：`u_user_login_log.login_type = 3`（与 `1 = 登录 / 2 = 注册` 并存）
- 前端 API：`project/user/web/src/api/auth.js` 与 `project/user/web/src/views/Forgot.vue`
- 与 Login.vue 一致：人机验证用弹框拖滑块（参考 `feedback_login_captcha_form.md` 记忆）

---

## File Structure

**后端新增/修改：**
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/dto/request/ResetPasswordRequest.java`
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/AuthService.java`（接口加 1 个方法）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/AuthServiceImpl.java`（加实现 + 改 `refreshToken()`）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/AuthController.java`（加 1 个 endpoint）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java`（加 `updatePassword`）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/JwtUtil.java`（加 `getRefreshTokenIssuedAt`）

**后端测试：**
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/AuthServiceResetPasswordTest.java`

**前端新增/修改：**
- 修改：`project/user/web/src/api/auth.js`（加 `resetPassword` 导出）
- 修改：`project/user/web/src/views/Forgot.vue`（handler + slider modal + watcher）

**E2E：**
- 新增：`tests/e2e/verify_reset_password.py`

---

### Task 1: `ResetPasswordRequest` DTO

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/dto/request/ResetPasswordRequest.java`

**Consumes:** 无

**Produces:** `ResetPasswordRequest` — 邮箱 / 邮箱码 / 新密码 / 二次确认 / 图形码 key / 图形码 code

- [ ] **Step 1: 创建 DTO 文件**

完整内容：

```java
package com.aichuangzuo.user.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String confirmPassword;

    @NotBlank(message = "图形验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q compile -DskipTests
```

Expected: `BUILD SUCCESS`，无报错。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/dto/request/ResetPasswordRequest.java
git commit -m "feat(user-api): 新增 ResetPasswordRequest DTO"
```

---

### Task 2: `JwtUtil.getRefreshTokenIssuedAt()`

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/JwtUtil.java`（在 `getExpiration(String)` 后追加新方法）

**Produces:** `Date getRefreshTokenIssuedAt(String token)` — 返回 refresh token 的 `iat` 声明

- [ ] **Step 1: 添加方法**

在 `getExpiration(String token)` 之后追加：

```java
/**
 * 解析 refresh token 签发时间（用于密码重置后的失效判断）。
 *
 * @param token JWT refresh token
 * @return 签发时间（iat）
 * @throws UnauthorizedException 当签名错误或 token 过期时
 */
public Date getRefreshTokenIssuedAt(String token) {
    SecretKey key = Keys.hmacShaKeyFor(
            authProperties.getJwt().getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    return claims.getIssuedAt();
}
```

- [ ] **Step 2: 编译**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q compile -DskipTests
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/infrastructure/security/JwtUtil.java
git commit -m "feat(user-api): JwtUtil 暴露 getRefreshTokenIssuedAt"
```

---

### Task 3: `UserMapper.updatePassword()`

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java`

**Produces:** `int updatePassword(@Param("id") Long id, @Param("hash") String hash)`

- [ ] **Step 1: 添加方法**

在 `UserMapper` 内追加 import 和方法：

```java
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
```

```java
@Update("UPDATE u_user SET password_hash = #{hash}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
int updatePassword(@Param("id") Long id, @Param("hash") String hash);
```

- [ ] **Step 2: 编译**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q compile -DskipTests
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mapper/UserMapper.java
git commit -m "feat(user-api): UserMapper 新增 updatePassword"
```

---

### Task 4: `AuthService.resetPassword()` + 重置时间戳

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/AuthService.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/AuthServiceImpl.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/AuthServiceResetPasswordTest.java`

**Produces:** `AuthService.resetPassword(ResetPasswordRequest, String clientIp)` — 校验 captcha / 邮箱码 / 密码一致性，更新密码，写 password-reset-at 时间戳，写 login log（type=3）。

- [ ] **Step 1: 接口加方法**

`AuthService.java`：

```java
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;

public interface AuthService {
    AuthTokenVO register(RegisterRequest request, String clientIp, String userAgent);
    AuthTokenVO login(LoginRequest request, String clientIp, String userAgent);
    AuthTokenVO refreshToken(RefreshTokenRequest request);
    void logout(String accessToken);
    void resetPassword(ResetPasswordRequest request, String clientIp);
}
```

- [ ] **Step 2: 写失败的集成测试**

Create `AuthServiceResetPasswordTest.java`：

```java
package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AuthServiceResetPasswordTest {

    private static final String RESET_AT_PREFIX = "user:auth:password-reset-at:";

    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CacheUtil cacheUtil;

    @MockBean
    private EmailCodeService emailCodeService;
    @MockBean
    private CaptchaService captchaService;

    private AuthTokenVO registerUser(String email) {
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("OldPass123");
        request.setConfirmPassword("OldPass123");
        return authService.register(request, "127.0.0.1", "test-agent");
    }

    private ResetPasswordRequest buildRequest(String email, String pwd) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(email);
        req.setEmailCode("000000");
        req.setPassword(pwd);
        req.setConfirmPassword(pwd);
        req.setCaptchaKey("test-key");
        req.setCaptchaCode("TEST12");
        return req;
    }

    @Test
    void shouldRejectWhenCaptchaInvalid() {
        registerUser("reset_captcha@example.com");
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(buildRequest("reset_captcha@example.com", "NewPass456"),
                        "127.0.0.1"));
        assertEquals(UserAuthErrorCode.CAPTCHA_ERROR.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldRejectWhenPasswordMismatch() {
        registerUser("reset_mismatch@example.com");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("reset_mismatch@example.com");
        req.setEmailCode("000000");
        req.setPassword("NewPass456");
        req.setConfirmPassword("NewPass789");
        req.setCaptchaKey("k");
        req.setCaptchaCode("c");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(req, "127.0.0.1"));
        assertEquals(UserAuthErrorCode.PASSWORD_NOT_MATCH.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldRejectWhenUserNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(buildRequest("no_such@example.com", "NewPass456"),
                        "127.0.0.1"));
        assertEquals(UserAuthErrorCode.USER_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldRejectWhenEmailCodeWrong() {
        registerUser("reset_code@example.com");
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(buildRequest("reset_code@example.com", "NewPass456"),
                        "127.0.0.1"));
        assertEquals(UserAuthErrorCode.EMAIL_CODE_ERROR.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void shouldUpdatePasswordAndWriteResetTimestamp() throws InterruptedException {
        AuthTokenVO token = registerUser("reset_ok@example.com");
        Long userId = jwtUserId(token);   // helper below

        // 重置
        authService.resetPassword(buildRequest("reset_ok@example.com", "BrandNew789"), "127.0.0.1");

        // 验证密码已更新（DB 中 hash 与新密码匹配）
        var user = userMapper.selectByEmail("reset_ok@example.com");
        assertNotNull(user);
        assertTrue(passwordEncoder.matches("BrandNew789", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPass123", user.getPasswordHash()));

        // 验证 reset-at 时间戳已写入
        Date resetAt = cacheUtil.get(RESET_AT_PREFIX + userId);
        assertNotNull(resetAt);
        assertTrue(resetAt.getTime() <= System.currentTimeMillis());
    }

    private Long jwtUserId(AuthTokenVO token) {
        // 借助 JwtUtil 解析 token subject 得到 userId
        var jwtUtil = applicationContext.getBean(
                com.aichuangzuo.user.infrastructure.security.JwtUtil.class);
        return jwtUtil.parseAccessToken(token.getAccessToken());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext applicationContext;
}
```

- [ ] **Step 3: 运行测试，验证全部 FAIL（编译失败即可）**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q test -Dtest=AuthServiceResetPasswordTest
```

Expected: 编译失败，因为 `AuthService.resetPassword` 尚未实现。

- [ ] **Step 4: 实现 `AuthService.resetPassword`**

在 `AuthServiceImpl.java` 中添加 import 与方法实现：

```java
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
```

```java
private static final String PASSWORD_RESET_AT_PREFIX = "user:auth:password-reset-at:";

@Override
public void resetPassword(ResetPasswordRequest request, String clientIp) {
    if (!captchaService.validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode())) {
        throw new BusinessException(UserAuthErrorCode.CAPTCHA_ERROR);
    }

    if (!request.getPassword().equals(request.getConfirmPassword())) {
        throw new BusinessException(UserAuthErrorCode.PASSWORD_NOT_MATCH);
    }

    User user = userMapper.selectByEmail(request.getEmail());
    if (user == null) {
        throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
    }

    if (!emailCodeService.validateEmailCode(request.getEmail(), request.getEmailCode())) {
        throw new BusinessException(UserAuthErrorCode.EMAIL_CODE_ERROR);
    }

    String newHash = passwordEncoder.encode(request.getPassword());
    userMapper.updatePassword(user.getId(), newHash);

    long refreshTtlSeconds = authProperties.getJwt().getRefreshExpiration();
    cacheUtil.set(PASSWORD_RESET_AT_PREFIX + user.getId(),
            new Date(),
            refreshTtlSeconds,
            TimeUnit.SECONDS);

    saveLoginLog(user.getId(), 3, clientIp, "reset-password", 1, null);
}
```

- [ ] **Step 5: 运行测试，验证全部 PASS**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q test -Dtest=AuthServiceResetPasswordTest
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

- [ ] **Step 6: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/AuthServiceResetPasswordTest.java
git commit -m "feat(user-api): AuthService.resetPassword 含密码更新与重置时间戳"
```

---

### Task 5: `AuthController.resetPassword` endpoint

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/AuthController.java`

**Produces:** `POST /api/v1/user/auth/reset-password` public endpoint

- [ ] **Step 1: 添加 endpoint**

`AuthController.java` 中新增 import：

```java
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
```

`AuthController` 类中、紧接 `register` 方法之后、之前的位置添加：

```java
@Operation(summary = "重置密码")
@PostMapping("/reset-password")
public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                   HttpServletRequest httpRequest) {
    authService.resetPassword(request, getClientIp(httpRequest));
    return Result.success();
}
```

- [ ] **Step 2: 编译**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q compile -DskipTests
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/AuthController.java
git commit -m "feat(user-api): AuthController 新增 POST /auth/reset-password"
```

---

### Task 6: `refreshToken()` 拒绝密码重置前签发的 token

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/AuthServiceImpl.java`
- Modify: `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/AuthServiceTokenTest.java` 或新建 `AuthServiceRefreshAfterResetTest.java`

**Produces:** 当用户密码被重置后，所有 refresh token 的 `iat < reset-at` 都被 `REFRESH_TOKEN_INVALID` 拒绝。

- [ ] **Step 1: 写失败测试**

Create `AuthServiceRefreshAfterResetTest.java`：

```java
package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.auth.dto.request.RefreshTokenRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AuthServiceRefreshAfterResetTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private EmailCodeService emailCodeService;
    @MockBean
    private CaptchaService captchaService;

    private AuthTokenVO registerUser(String email) {
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("OldPass123");
        request.setConfirmPassword("OldPass123");
        return authService.register(request, "127.0.0.1", "test-agent");
    }

    @Test
    void shouldInvalidateOldRefreshTokenAfterPasswordReset() throws InterruptedException {
        AuthTokenVO token = registerUser("refresh_reset@example.com");
        Long userId = jwtUtil.parseAccessToken(token.getAccessToken());
        String oldRefresh = token.getRefreshToken();

        // 模拟密码重置：写一个 reset 时间戳（晚于 token 签发）
        // 重置密码
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);

        ResetPasswordRequest rp = new ResetPasswordRequest();
        rp.setEmail("refresh_reset@example.com");
        rp.setEmailCode("000000");
        rp.setPassword("BrandNew789");
        rp.setConfirmPassword("BrandNew789");
        rp.setCaptchaKey("k");
        rp.setCaptchaCode("c");
        authService.resetPassword(rp, "127.0.0.1");

        // 用旧 refresh token 应失败
        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(oldRefresh);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(refreshReq));
        assertEquals(UserAuthErrorCode.REFRESH_TOKEN_INVALID.getCode(), ex.getErrorCode().getCode());

        // 用新密码登录拿到新 refresh
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);
        com.aichuangzuo.user.modules.auth.dto.request.LoginRequest loginReq =
                new com.aichuangzuo.user.modules.auth.dto.request.LoginRequest();
        loginReq.setEmail("refresh_reset@example.com");
        loginReq.setPassword("BrandNew789");
        loginReq.setCaptchaKey("k");
        loginReq.setCaptchaCode("c");
        AuthTokenVO newToken = authService.login(loginReq, "127.0.0.1", "test-agent");

        // 新 refresh token 能用
        RefreshTokenRequest newRefreshReq = new RefreshTokenRequest();
        newRefreshReq.setRefreshToken(newToken.getRefreshToken());
        AuthTokenVO refreshed = authService.refreshToken(newRefreshReq);
        assertNotNull(refreshed.getAccessToken());
    }
}
```

- [ ] **Step 2: 跑测试，验证 FAIL**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q test -Dtest=AuthServiceRefreshAfterResetTest
```

Expected: 抛出 `REFRESH_TOKEN_INVALID` 不发生（旧 refresh 现在还能用）。

- [ ] **Step 3: 修改 `AuthServiceImpl.refreshToken`**

`AuthServiceImpl.java` 中 `refreshToken` 方法头部加：

```java
@Override
public AuthTokenVO refreshToken(RefreshTokenRequest request) {
    Long userId = jwtUtil.parseRefreshToken(request.getRefreshToken());

    // 校验是否在密码重置之前签发（重置后所有 iat ≤ reset-at 的 refresh 都失效）
    Date resetAt = cacheUtil.get(PASSWORD_RESET_AT_PREFIX + userId);
    if (resetAt != null) {
        Date tokenIat = jwtUtil.getRefreshTokenIssuedAt(request.getRefreshToken());
        if (tokenIat == null || !tokenIat.after(resetAt)) {
            throw new BusinessException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    User user = userMapper.selectById(userId);
    if (user == null || user.getUserStatus() == 0) {
        throw new BusinessException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
    }
    return buildAuthTokenVO(user);
}
```

- [ ] **Step 4: 跑测试，验证 PASS**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q test -Dtest=AuthServiceRefreshAfterResetTest
```

Expected: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

- [ ] **Step 5: 跑完整测试套件，确认没有回归**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn -q test
```

Expected: 全部现有测试 + 新增测试都通过。

- [ ] **Step 6: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/AuthServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/AuthServiceRefreshAfterResetTest.java
git commit -m "feat(user-api): refreshToken 拒绝密码重置前签发的 token"
```

---

### Task 7: 前端 `api/auth.js` 新增 `resetPassword` 导出

**Files:**
- Modify: `project/user/web/src/api/auth.js`

**Produces:** `resetPassword(data)` 调用 `POST /api/v1/user/auth/reset-password`

- [ ] **Step 1: 添加导出**

在文件末尾、现有 export 区域追加：

```js
/**
 * 重置密码（公开接口）
 * @param {object} data
 * @param {string} data.email
 * @param {string} data.emailCode
 * @param {string} data.password
 * @param {string} data.confirmPassword
 * @param {string} data.captchaKey
 * @param {string} data.captchaCode
 */
export const resetPassword = (data) =>
  request.post('/auth/reset-password', data)
```

- [ ] **Step 2: 构建验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run build 2>&1 | tail -5
```

Expected: `✓ built in <N>s`，无报错。

- [ ] **Step 3: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/web/src/api/auth.js
git commit -m "feat(user-web): api/auth 新增 resetPassword 导出"
```

---

### Task 8: Forgot.vue 接入 slider-modal + handleReset

**Files:**
- Modify: `project/user/web/src/views/Forgot.vue`

**Consumes:**
- `SliderCaptcha` 组件（现有 `components/SliderCaptcha.vue`）
- `getCaptcha` / `sendEmailCode` / `resetPassword` from `api/auth.js`
- `message` from `ant-design-vue`

**Produces:** `Forgot.vue` 完整可工作的重置流程：
- 「获取验证码」前先弹框拖滑块，通过后调 `sendEmailCode`（沿用现有 mock captcha key + TEST12 流程）
- 「重置密码」前先弹框拖滑块，通过后调 `resetPassword` API，跳 /login

**前置约束：** 参考 `feedback_login_captcha_form.md` — 滑块必须是 `<a-modal>` 弹框，**不**放在表单内联。

- [ ] **Step 1: 改造 imports**

`Forgot.vue` script setup 顶部 imports 改为：

```js
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import NavBar from '@/components/layout/NavBar.vue'
import { getCaptcha, sendEmailCode, resetPassword } from '@/api/auth'

const router = useRouter()
```

- [ ] **Step 2: 添加状态与常量**

紧接 `const navLinks` / `ctaTo` / `ctaLabel` 之下、`// ---------- 鼠标方向律动 ----------` 之上添加：

```js
// ---------- 人机验证常量（与 Login.vue 一致） ----------
const SLIDER_CAPTCHA_VALUE = 'TEST12'

// ---------- 后端 captcha 会话 ----------
const captchaKey = ref('')

// ---------- 注册流程：滑块弹框状态 ----------
const sliderModalVisible = ref(false)
const sliderModalPassed = ref(false)
let modalSending = false

// ---------- 重置流程：滑块弹框状态 ----------
const resetModalVisible = ref(false)
const resetModalPassed = ref(false)
let resetSending = false
```

- [ ] **Step 3: 改造发送邮箱验证码流程**

替换现有的 `sendCode` 函数：

```js
// === 邮箱验证码发送：弹框拖滑块 → 通过后才调 sendEmailCode ===
const openCodeSlider = async () => {
  if (codeCountdown.value > 0) return
  if (!form.email) {
    message.warning('请先填写邮箱')
    return
  }
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    sliderModalPassed.value = false
    sliderModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

watch(sliderModalPassed, async (val) => {
  if (!val || modalSending) return
  modalSending = true
  try {
    await sendEmailCode({
      email: form.email,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE,
    })
    startCodeCountdown()
    message.success('验证码已发送')
    sliderModalVisible.value = false
  } catch (err) {
    message.error(err?.message || '发送失败')
    sliderModalVisible.value = false
  } finally {
    modalSending = false
  }
})

// template 里把「获取验证码」按钮的 @click 由 sendCode 改为 openCodeSlider
```

模板里「获取验证码」按钮：

```vue
<button class="code-btn"
        :disabled="codeCountdown > 0"
        @click="openCodeSlider">
  {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
</button>
```

- [ ] **Step 4: 改造 `handleReset` + 重置 watcher**

```js
const handleReset = async () => {
  // 基础兜底校验
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
    resetModalPassed.value = false
    resetModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

watch(resetModalPassed, async (val) => {
  if (!val || resetSending) return
  resetSending = true
  try {
    await resetPassword({
      email: form.email,
      emailCode: form.code,
      password: form.password,
      confirmPassword: form.confirmPassword,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE,
    })
    message.success('密码已重置，请重新登录')
    resetModalVisible.value = false
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    codeCountdown.value = 0
    router.push('/login')
  } catch (err) {
    message.error(err?.message || '重置失败')
    resetModalVisible.value = false
  } finally {
    resetSending = false
  }
})
```

- [ ] **Step 5: 在 template 末尾追加两个滑块弹框**

紧接 `</div>`（template 最外层 forgot-page 的闭合）之前、`<style>` 标签之前的位置添加：

```vue
<!-- 发送邮箱验证码前的滑块弹框 -->
<a-modal v-model:open="sliderModalVisible"
         title="人机验证"
         :footer="null"
         :mask-closable="false"
         :keyboard="false"
         width="420px"
         class="slider-modal slider-modal-send-code">
  <p class="slider-modal-tip">
    拖动滑块完成验证后将向
    <b>{{ form.email || '当前邮箱' }}</b> 发送 6 位邮箱验证码
  </p>
  <SliderCaptcha v-model="sliderModalPassed" />
</a-modal>

<!-- 重置密码前的滑块弹框 -->
<a-modal v-model:open="resetModalVisible"
         title="人机验证"
         :footer="null"
         :mask-closable="false"
         :keyboard="false"
         width="420px"
         class="slider-modal slider-modal-reset">
  <p class="slider-modal-tip">
    拖动滑块完成验证后将重置账号
    <b v-if="form.email">「{{ form.email }}」</b>
    的密码
  </p>
  <SliderCaptcha v-model="resetModalPassed" />
</a-modal>
```

- [ ] **Step 6: 追加滑块弹框样式**

在 `Forgot.vue` 的 `<style scoped>` 中、`.submit-btn` 相关样式之后添加：

```css
/* ========== 滑块弹框 ========== */
.slider-modal-tip {
  font-size: 13px;
  color: #595959;
  margin-bottom: 16px;
  line-height: 1.6;
}
.slider-modal-tip b {
  color: #FF2442;
  font-weight: 500;
  word-break: break-all;
}
body[data-theme="dark"] .slider-modal-tip {
  color: #a6a6a6;
}
body[data-theme="dark"] .slider-modal-tip b {
  color: #ff4d6f;
}
.slider-modal :deep(.ant-modal-header) {
  margin-bottom: 12px;
}
body[data-theme="dark"] .slider-modal :deep(.ant-modal-content) {
  background: #1f1f1f;
}
body[data-theme="dark"] .slider-modal :deep(.ant-modal-header) {
  background: transparent;
}
body[data-theme="dark"] .slider-modal :deep(.ant-modal-title) {
  color: #e0e0e0;
}
```

- [ ] **Step 7: 构建**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run build 2>&1 | tail -5
```

Expected: `✓ built in <N>s`，无报错。

- [ ] **Step 8: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/web/src/views/Forgot.vue
git commit -m "feat(user-web): Forgot.vue 接入 slider-modal 重置密码流程"
```

---

### Task 9: E2E 验证脚本 `verify_reset_password.py`

**Files:**
- Create: `tests/e2e/verify_reset_password.py`

**Consumes:** 已 mock 的 test profile 后端（captcha=TEST12, email=000000），前端 22345，后端 25050。

**场景：**
- 场景 A：注册一个用户 → 退出 → /forgot 重置密码 → 跳 /login → 用新密码登录成功
- 场景 B：refresh token 失效校验：登录拿到 refresh → 重置密码 → 旧 refresh token 调 `/refresh-token` 应失败

- [ ] **Step 1: 创建脚本**

完整文件：

```python
#!/usr/bin/env python3
"""重置密码端到端验证。

场景 A — 完整重置流程：
  1. /register 创建测试账号拿到 token（不通过 UI）
  2. /forgot 页面：填邮箱 → 点获取验证码 → 弹框拖滑块 → 通过（自动发码）
  3. 填 mock 邮箱码 000000 + 新密码
  4. 点重置密码 → 弹框拖滑块 → 通过 → 调用重置接口 → 跳转 /login
  5. /login 用新密码登录成功
  6. 登录页用旧密码登录应失败

场景 B — refresh token 失效：
  1. 用场景 A 拿到的 access 模拟一个 refresh-token
  2. 后端直接调 /refresh-token 应失败（旧 refresh token 在密码重置前签发）

前置：
  - 后端 SPRING_PROFILES_ACTIVE=test 启动（captcha=TEST12, email=000000）
  - 前端 22345 运行

用法：
  python3 tests/e2e/verify_reset_password.py
"""
import re
import sys
import time
from pathlib import Path

import requests
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:22345"
API_URL = "http://localhost:25050/api/v1/user"
EMAIL_CODE_MOCK = "000000"
OLD_PASSWORD = "OldPass123"
NEW_PASSWORD = "BrandNew789"
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
SCREENSHOT_DIR.mkdir(exist_ok=True)


def drag_slider_to_end(page):
    """与 verify_user_auth.py 相同的滑块模拟拖动实现。"""
    handle = page.locator(".slider-modal .slider-captcha .slider-handle").first
    handle.wait_for(state="visible", timeout=5000)
    page.wait_for_timeout(500)
    handle_box = handle.bounding_box()
    assert handle_box, "未找到滑块 handle 位置"
    start_x = handle_box["x"] + handle_box["width"] / 2
    start_y = handle_box["y"] + handle_box["height"] / 2
    end_x = start_x + 400

    page.evaluate(
        """({x, y}) => {
            const el = document.querySelector('.slider-modal .slider-captcha .slider-handle');
            el.dispatchEvent(new MouseEvent('mousedown', {
                bubbles: true, cancelable: true,
                clientX: x, clientY: y, button: 0
            }));
        }""",
        {"x": start_x, "y": start_y},
    )
    for step in range(1, 31):
        cur_x = start_x + (end_x - start_x) * step / 30
        page.evaluate(
            """({x, y}) => {
                document.dispatchEvent(new MouseEvent('mousemove', {
                    bubbles: true, cancelable: true,
                    clientX: x, clientY: y, button: 0
                }));
            }""",
            {"x": cur_x, "y": start_y},
        )
        page.wait_for_timeout(10)
    page.evaluate(
        """({x, y}) => {
            document.dispatchEvent(new MouseEvent('mouseup', {
                bubbles: true, cancelable: true,
                clientX: x, clientY: y, button: 0
            }));
        }""",
        {"x": end_x, "y": start_y},
    )
    page.wait_for_timeout(200)
    slider_class = page.locator(".slider-modal .slider-captcha").first.get_attribute("class")
    assert "is-passed" in (slider_class or ""), f"滑块未通过，class={slider_class}"


def register_via_api(email, password):
    """通过后端 API（不经过 UI）注册一个账号拿到 refresh_token。"""
    # 先发邮箱码
    cap = requests.get(f"{API_URL}/auth/captcha").json()
    requests.post(f"{API_URL}/auth/email-codes", json={
        "email": email,
        "captchaKey": cap["data"]["captchaKey"],
        "captchaCode": "TEST12",
    })
    cap2 = requests.get(f"{API_URL}/auth/captcha").json()
    resp = requests.post(f"{API_URL}/auth/register", json={
        "email": email,
        "emailCode": EMAIL_CODE_MOCK,
        "password": password,
        "confirmPassword": password,
        "captchaKey": cap2["data"]["captchaKey"],
        "captchaCode": "TEST12",
    })
    assert resp.status_code == 200, f"register failed: {resp.text}"
    return resp.json()["data"]


def main():
    email = f"e2e_reset_{int(time.time())}@example.com"
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # ============== 0. 通过 API 预备测试账号 ==============
        api_token = register_via_api(email, OLD_PASSWORD)
        assert api_token and api_token.get("accessToken"), "注册失败"
        results.append(("API 注册测试账号成功", True))

        # ============== 场景 A ==============
        page.goto(f"{BASE_URL}/forgot")
        page.wait_for_selector(".forgot-card", timeout=10000)
        results.append(("打开 /forgot 页面", "/forgot" in page.url))

        # 1. 填邮箱
        page.fill("input[placeholder='请输入注册邮箱']:visible", email)
        results.append(("填写邮箱", True))

        # 2. 点获取验证码 → 弹框拖滑块 → 通过后自动发码
        page.click("button.code-btn:has-text('获取验证码')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        results.append(("点获取验证码弹出滑块弹框", True))
        drag_slider_to_end(page)
        page.wait_for_selector(".slider-modal", state="hidden", timeout=10000)
        page.wait_for_selector("button.code-btn:has-text('s')", timeout=5000)
        results.append(("滑块通过自动发送邮箱验证码", True))

        # 3. 填邮箱码 + 新密码
        page.fill("input[placeholder='输入 6 位验证码']:visible", EMAIL_CODE_MOCK)
        page.fill("input[placeholder='6-20 位新密码']:visible", NEW_PASSWORD)
        page.fill("input[placeholder='再次输入新密码']:visible", NEW_PASSWORD)

        # 4. 点重置 → 弹框拖滑块 → 通过后调后端 → 跳 /login
        page.click("button.submit-btn:has-text('重置密码')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        results.append(("点重置密码弹出滑块弹框", True))
        drag_slider_to_end(page)
        page.wait_for_url(re.compile(r"/login"), timeout=15000)
        results.append(("滑块通过自动重置并跳转 /login", "/login" in page.url))

        page.screenshot(path=str(SCREENSHOT_DIR / "reset_password_01_after_reset.png"))

        # 5. /login 用新密码登录成功
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        page.fill("input[placeholder='请输入密码']:visible", NEW_PASSWORD)
        page.click("button.submit-btn:has-text('登录')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        drag_slider_to_end(page)
        page.wait_for_url(re.compile(r"/console"), timeout=15000)
        results.append(("用新密码登录成功跳转 /console", "/console" in page.url))

        new_access = page.evaluate(
            "() => localStorage.getItem('aichuangzuo_access_token')"
        )
        results.append(("新密码登录后 token 已写入", bool(new_access)))

        # 清理：退出登录
        page.click(".console-avatar")
        page.wait_for_selector(".user-action-logout", timeout=5000)
        page.click(".user-action-logout")
        page.wait_for_url(re.compile(r"/login"), timeout=10000)

        # 6. 旧密码登录应失败（在 /login 页面再走一遍）
        page.fill("input[placeholder='请输入邮箱']:visible", email)
        page.fill("input[placeholder='请输入密码']:visible", OLD_PASSWORD)
        page.click("button.submit-btn:has-text('登录')")
        page.wait_for_selector(".slider-modal", state="visible", timeout=5000)
        drag_slider_to_end(page)
        # 期望 error 提示或不跳转（在 /login）
        page.wait_for_timeout(2000)
        results.append(("旧密码登录被拒绝", "/login" in page.url))
        page.screenshot(path=str(SCREENSHOT_DIR / "reset_password_02_old_pwd_rejected.png"))

        # ============== 场景 B — 旧 refresh token 失效 ==============
        old_refresh = api_token["refreshToken"]
        resp = requests.post(f"{API_URL}/auth/refresh-token",
                             json={"refreshToken": old_refresh})
        results.append(("旧 refresh token 在密码重置后被拒绝",
                        resp.status_code != 200 or resp.json().get("code") != 0))

        browser.close()

    print("\n=== 重置密码端到端验证结果 ===")
    print(f"测试邮箱：{email}")
    all_ok = True
    for name, ok in results:
        status = "✓ PASS" if ok else "✗ FAIL"
        print(f"{status}  {name}")
        if not ok:
            all_ok = False
    print()
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 启动用户全栈**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
./scripts/local/user-full-stack/restart.sh
```

Expected: 后端 25050 / 前端 22345 都监听中。

- [ ] **Step 3: 跑 E2E**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_reset_password.py
```

Expected: `All checks passed`，全部断言 OK（包括场景 B 旧 refresh token 被拒）。

- [ ] **Step 4: Commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add tests/e2e/verify_reset_password.py
git commit -m "test(user): 添加重置密码 Playwright 验证脚本"
```

---

## Self-Review

1. **Spec 覆盖**：
   - 接口路径 DTO（spec §2.1）→ Task 1 ✓
   - AuthService.resetPassword（spec §2.2）→ Task 4 ✓
   - AuthController endpoint（spec §2.3）→ Task 5 ✓
   - UserMapper.updatePassword（spec §2.4）→ Task 3 ✓
   - JwtUtil.getRefreshTokenIssuedAt（spec §2.5）→ Task 2 ✓
   - refreshToken 校验增强（spec §2.6）→ Task 6 ✓
   - 前端 api/auth.js resetPassword（spec §3.1）→ Task 7 ✓
   - 前端 Forgot.vue slider modal（spec §3.2）→ Task 8 ✓
   - E2E 验证脚本（spec §4）→ Task 9 ✓

2. **Placeholder 扫描**：
   - 无 TBD / TODO / 后续再补；
   - mock captcha/email 取自现有 test profile，与 verify_user_auth.py 一致；
   - 前后端口与脚本路径全部用绝对路径或相对仓库根写明。

3. **类型一致性**：
   - `ResetPasswordRequest` 字段在 Task 1 定义、Task 4 测试与实现、Task 5 controller 引用均一致（email / emailCode / password / confirmPassword / captchaKey / captchaCode）。
   - `PASSWORD_RESET_AT_PREFIX` 在 Task 4 中定义常量、Task 6 直接引用，无重命名。
   - `JwtUtil.getRefreshTokenIssuedAt` 在 Task 2 定义、Task 6 引用，签名一致（`String → Date`）。
   - `UserMapper.updatePassword(id, hash)` 在 Task 3 定义、Task 4 引用，参数名一致。
   - 前端 `resetPassword({ email, emailCode, password, confirmPassword, captchaKey, captchaCode })` 在 Task 7 导出、Task 8 调用一致。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-04-reset-password.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
