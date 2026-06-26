# 日志规范

> 本文档定义爱创作（AI Creation）项目的日志输出规范，便于问题排查、审计追踪与性能分析。

---

## 1. 日志框架

- 统一使用 **SLF4J + Logback**（Spring Boot 默认）。
- 类上使用 `@Slf4j`。
- 禁止 `System.out.println()` 和 `e.printStackTrace()`。

---

## 2. 日志级别

| 级别 | 使用场景 |
|---|---|
| `ERROR` | 系统异常、数据库连接失败、关键流程中断 |
| `WARN` | 业务异常、预期外分支、可恢复错误 |
| `INFO` | 关键业务流程、启动信息、外部调用出入口 |
| `DEBUG` | 调试信息，生产环境默认关闭 |
| `TRACE` | 极少使用，必要时打印详细调用链 |

---

## 3. 日志格式

统一格式：

```text
[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%X{traceId}] [%level] [%logger{36}] - %msg%n
```

关键字段：

| 字段 | 说明 |
|---|---|
| `traceId` | 请求链路唯一标识，通过 MDC 传递 |
| `userId` | 当前登录用户 ID，记到 MDC 中 |
| `level` | 日志级别 |
| `logger` | 日志来源类 |

示例：

```text
[2026-06-26 14:30:00.123] [http-nio-8080-exec-1] [abc123] [INFO] [c.a.u.m.a.ArticleService] - 创建文章成功, articleId=10001, userId=20001
```

---

## 4. traceId 传递

- 网关/Filter 在请求入口生成 `traceId` 并写入 MDC。
- 跨线程调用时手动传递 MDC（线程池场景）。
- Feign/WebClient 调用下游服务时，将 `traceId` 放入 Header `X-Trace-Id`。
- 响应头返回 `X-Trace-Id`，便于前端问题定位。

---

## 5. 请求日志

通过拦截器统一记录所有 HTTP 请求：

```java
log.info("请求开始 method={}, uri={}, query={}, clientIp={}, userId={}", 
         request.getMethod(), request.getRequestURI(), 
         request.getQueryString(), clientIp, userId);
```

请求结束记录：

```java
log.info("请求结束 method={}, uri={}, cost={}ms, status={}", 
         method, uri, cost, status);
```

- 请求体/响应体仅在 `DEBUG` 级别记录。
- 敏感参数（密码、Token、手机号）必须脱敏。

---

## 6. 操作日志

### 6.1 用户端操作日志

记录到 `u_user_operation_log` 表，内容包括：

- 用户 ID
- 操作类型（如 `CREATE_ARTICLE`、`CANCEL_TASK`）
- 操作描述
- 请求参数（脱敏后）
- 客户端 IP
- 操作时间

### 6.2 管理端操作日志

记录到 `a_operation_log` 表，必须记录：

- 管理员 ID
- 操作类型与描述
- 请求方法、URL、参数
- 响应结果
- 客户端 IP、User-Agent
- 操作耗时

### 6.3 必须记录的操作

- 登录、登出、密码修改
- 生成任务提交、取消、删除
- 文章创建、更新、删除、导出
- 订单创建、支付回调
- 额度变动
- 管理端：管理员增删改、角色权限变更、系统配置修改

---

## 7. 错误日志

- `BusinessException` 记录 `WARN`，不打印堆栈。
- `SystemException` 和未知异常记录 `ERROR`，必须打印堆栈。
- 错误日志中需包含 `traceId`、用户 ID、关键业务参数。

示例：

```java
try {
    llmClient.generate(request);
} catch (Exception e) {
    log.error("AI 生成失败, taskId={}, userId={}, traceId={}", taskId, userId, traceId, e);
}
```

---

## 8. 日志文件配置

```text
logs/
├── app.log           # 当前运行日志
├── app.error.log     # ERROR 级别日志
├── app.info.log      # INFO 及以上日志
└── archive/          # 历史日志压缩包
```

- 按天滚动，单个文件最大 100MB。
- 保留最近 30 天日志。
- ERROR 日志单独输出，便于监控告警。

---

## 9. 敏感信息脱敏

以下信息禁止以明文形式出现在日志中：

- 密码、Token、Refresh Token
- 手机号、邮箱、身份证号
- LLM API Key
- 银行卡号、支付敏感信息
- 用户详细地址

脱敏示例：

```java
log.info("用户登录成功, phone={}", DesensitizationUtil.phone(phone));
// 输出：用户登录成功, phone=138****8000
```

---

## 10. 禁止事项

- 禁止在循环中打印 INFO/ERROR 日志。
- 禁止使用字符串拼接，必须使用占位符 `{}`。
- 禁止打印完整请求体到 INFO 日志。
- 禁止将 Token、密码等敏感信息作为日志参数。
- 禁止使用 `e.getMessage()` 替代完整堆栈记录系统异常。

---

## 11. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：日志框架、级别、traceId、请求/操作/错误日志、文件切割、脱敏 | - |
