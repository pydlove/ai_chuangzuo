# 异常处理与错误码规范

> 本文档定义爱创作（AI Creation）项目后端的异常分类、错误码定义、异常抛出与全局拦截方式，与 `docs/architecture/api-interface-conventions.md` 中的错误码体系保持一致。

---

## 1. 目标

- 统一后端异常分类、错误码定义、异常抛出与拦截方式。
- 保证 API 始终返回 `Result<T>` 结构。
- 便于前端根据错误码做精准提示，便于日志排查问题。

---

## 2. 异常体系

### 2.1 异常类层级

```text
RuntimeException
├── BaseException（抽象，所有自定义异常基类）
│   ├── BusinessException（业务异常，预期内）
│   ├── SystemException（系统异常，预期外）
│   ├── AuthException（认证失败）
│   │   ├── UnauthorizedException（未登录/Token 失效）
│   │   └── ForbiddenException（无权限）
│   └── ParamException（参数异常）
│       └── ValidationException（参数校验失败）
```

### 2.2 异常类位置

通用异常类统一放在 `project/shared` 模块：

```text
com.aichuangzuo.shared.exception/
├── BaseException.java
├── BusinessException.java
├── SystemException.java
├── AuthException.java
├── UnauthorizedException.java
├── ForbiddenException.java
├── ParamException.java
└── ValidationException.java
```

用户端与管理端原则上复用 shared 中的异常类。仅在端特有语义非常明确时，才允许在各自 `common/exception/` 下扩展，且必须继承 `BaseException`。

### 2.3 BaseException 定义

```java
package com.aichuangzuo.shared.exception;

public abstract class BaseException extends RuntimeException {
    private final Integer code;
    private final Object[] args;

    protected BaseException(Integer code, String message) {
        super(message);
        this.code = code;
        this.args = null;
    }

    protected BaseException(Integer code, String message, Object[] args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    protected BaseException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.args = null;
    }

    public Integer getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
```

### 2.4 常用异常类定义

```java
package com.aichuangzuo.shared.exception;

import com.aichuangzuo.shared.result.ErrorCode;

public class BusinessException extends BaseException {

    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode(), errorCode.getMessage(), args);
    }
}
```

```java
public class SystemException extends BaseException {

    public SystemException(String message) {
        super(100001, message);
    }

    public SystemException(String message, Throwable cause) {
        super(100001, message, cause);
    }
}
```

```java
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}

public class ForbiddenException extends BaseException {
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }
}
```

---

## 3. 错误码定义

### 3.1 ErrorCode 接口

```java
package com.aichuangzuo.shared.result;

public interface ErrorCode {
    Integer getCode();
    String getMessage();
}
```

所有错误码枚举必须实现 `ErrorCode` 接口。

### 3.2 错误码枚举位置

按模块分文件，放在 `project/shared`：

```text
com.aichuangzuo.shared.enums.error/
├── SystemErrorCode.java
├── UserAuthErrorCode.java
├── UserErrorCode.java
├── ArticleErrorCode.java
├── GenerationErrorCode.java
├── CreditErrorCode.java
├── BillingErrorCode.java
├── AdminAuthErrorCode.java
├── AdminUserErrorCode.java
└── AdminConfigErrorCode.java
```

### 3.3 枚举结构

```java
package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;

public enum UserAuthErrorCode implements ErrorCode {
    PHONE_FORMAT_ERROR(111001, "手机号格式错误"),
    CAPTCHA_ERROR(111002, "验证码错误"),
    ACCOUNT_OR_PASSWORD_ERROR(111003, "账号或密码错误"),
    TOKEN_EXPIRED(111004, "登录已过期，请重新登录"),
    ACCOUNT_DISABLED(111005, "账号已被禁用");

    private final int code;
    private final String message;

    UserAuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

### 3.4 错误码分段

采用 6 位数字分段：

```text
1XXYYY
```

| 段 | 含义 |
|---|---|
| `1` | 固定前缀 |
| `XX` | 端或模块 |
| `YYY` | 具体错误序号 |

模块编码：

| 编码 | 模块 |
|---|---|
| `00` | 系统级通用 |
| `01` | 用户端通用 |
| `02` | 管理端通用 |
| `11` | 用户端 - 认证 |
| `12` | 用户端 - 用户 |
| `13` | 用户端 - 文章 |
| `14` | 用户端 - 生成任务 |
| `15` | 用户端 - 额度 |
| `16` | 用户端 - 订单/支付 |
| `21` | 管理端 - 认证 |
| `22` | 管理端 - 管理员 |
| `23` | 管理端 - 角色权限 |
| `24` | 管理端 - 系统配置 |

---

## 4. 统一响应封装

放在 `com.aichuangzuo.shared.result`：

```java
package com.aichuangzuo.shared.result;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
```

---

## 5. 全局异常处理

放在 `com.aichuangzuo.user.common.exception.handler`（用户端）和 `com.aichuangzuo.admin.common.exception.handler`（管理端）：

```java
package com.aichuangzuo.user.common.exception.handler;

import com.aichuangzuo.shared.exception.*;
import com.aichuangzuo.shared.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return Result.fail(100002, "参数校验失败", errors);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleForbiddenException(ForbiddenException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(SystemException e) {
        log.error("SystemException: code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknownException(Exception e) {
        log.error("Unknown exception: ", e);
        return Result.fail(100001, "系统繁忙，请稍后重试");
    }
}
```

---

## 6. 异常抛出规范

### 6.1 Service 层

```java
public User login(LoginRequest request) {
    User user = userMapper.selectByPhone(request.getPhone());
    if (user == null) {
        throw new BusinessException(UserAuthErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
    }
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        throw new BusinessException(UserAuthErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
    }
    if (user.getStatus() == 0) {
        throw new BusinessException(UserAuthErrorCode.ACCOUNT_DISABLED);
    }
    return user;
}
```

### 6.2 Controller 层

Controller 层只做参数校验和调用 Service，不主动抛业务异常：

```java
@PostMapping("/login")
public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
    User user = authService.login(request);
    return Result.success(authConverter.toLoginVO(user));
}
```

### 6.3 禁止行为

- 禁止直接抛 `RuntimeException`。
- 禁止抛 `NullPointerException`、`IllegalArgumentException` 等 JDK 异常表达业务错误。
- 禁止在 Service 层返回错误码字符串，必须抛异常。

---

## 7. 参数校验失败

### 7.1 DTO 定义

```java
public class CreateArticleRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 256, message = "标题长度不能超过 256 个字符")
    private String title;

    @NotNull(message = "字数不能为空")
    @Min(value = 100, message = "字数不能少于 100")
    @Max(value = 3000, message = "字数不能超过 3000")
    private Integer wordCount;
}
```

### 7.2 失败响应

```json
{
  "code": 100002,
  "message": "参数校验失败",
  "data": {
    "title": "文章标题不能为空",
    "wordCount": "字数不能为空"
  }
}
```

---

## 8. 错误消息管理

当前阶段错误消息直接写在错误码枚举中，硬编码中文：

```java
PHONE_FORMAT_ERROR(111001, "手机号格式错误")
```

后续如需支持国际化，将 message 改为 key：

```java
PHONE_FORMAT_ERROR(111001, "error.user.phone-format")
```

配合 `messages_zh_CN.properties` / `messages_en_US.properties` 实现。

---

## 9. 日志记录规范

| 异常类型 | 日志级别 | 是否打印堆栈 |
|---|---|---|
| `BusinessException` | `WARN` | 否 |
| `UnauthorizedException` / `ForbiddenException` | `WARN` | 否 |
| `ParamException` / `ValidationException` | `WARN` | 否 |
| `SystemException` | `ERROR` | 是 |
| 未知 `Exception` | `ERROR` | 是 |

---

## 10. 与 API 规范的对应关系

| API 规范 | 异常/错误码实现 |
|---|---|
| `code: 0` | 正常返回 `Result.success()` |
| `code: 100001` | `SystemException` 或未知异常兜底 |
| `code: 100002` | 参数校验失败 |
| `code: 100003` | 限流（可在限流拦截器中直接返回） |
| HTTP `401` | `UnauthorizedException` |
| HTTP `403` | `ForbiddenException` |
| HTTP `400` | `ParamException` / `ValidationException` |
| HTTP `500` | `SystemException` / 未知异常 |

---

## 11. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：异常体系、错误码枚举、全局异常处理、参数校验、日志规范 | - |
