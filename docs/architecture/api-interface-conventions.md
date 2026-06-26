# API 接口规范

> 本文档定义爱创作（AI Creation）项目前后端交互的 API 接口规范，适用于用户端（`project/user/api`）与管理端（`project/admin/api`）。

---

## 1. 基础约定

### 1.1 协议与编码

- **协议**：HTTP/1.1 或 HTTPS，统一使用 JSON 交换数据。
- **编码**：`UTF-8`。
- **Content-Type**：`application/json`。

### 1.2 Base URL

```text
https://{host}/api/{version}/{端}/{模块}
```

| 段 | 说明 | 示例 |
|---|---|---|
| `api` | 固定前缀 | |
| `{version}` | API 版本，当前 `v1` | `v1` |
| `{端}` | `user`（用户端）或 `admin`（管理端） | `user` |
| `{模块}` | 业务模块名 | `article`、`auth` |

示例：

```text
POST   /api/v1/user/auth/login
GET    /api/v1/user/articles
POST   /api/v1/admin/system-configs
```

### 1.3 版本控制

- 版本号放在 URL 路径中。
- 当前版本统一为 `v1`。
- 重大不兼容变更时升级版本号（如 `v2`），老版本保留兼容期。

---

## 2. HTTP 方法

| 方法 | 用途 |
|---|---|
| `GET` | 查询资源或列表 |
| `POST` | 创建资源 |
| `PUT` | 全量更新资源 |
| `PATCH` | 部分更新资源 |
| `DELETE` | 删除资源 |

禁止使用 `POST` 做查询（除非请求体特别大或涉及敏感参数）。

---

## 3. URL 设计规范

### 3.1 资源名

- 使用名词复数形式。
- 全小写，单词间用 `-` 连接（kebab-case）。
- 禁止动词，动作通过 HTTP 方法表达。

| 操作 | URL |
|---|---|
| 创建文章 | `POST /api/v1/user/articles` |
| 查询文章列表 | `GET /api/v1/user/articles` |
| 查询文章详情 | `GET /api/v1/user/articles/{id}` |
| 更新文章 | `PUT /api/v1/user/articles/{id}` |
| 删除文章 | `DELETE /api/v1/user/articles/{id}` |

### 3.2 非 REST 动作

对于不适合用标准方法表达的动作，使用 `{资源}/{id}/actions/{动作}`。

```text
POST /api/v1/user/generation-tasks/{id}/actions/cancel
POST /api/v1/user/articles/{id}/actions/publish
```

### 3.3 嵌套资源

嵌套层级不超过两层：

```text
GET /api/v1/user/articles/{articleId}/versions
```

避免：

```text
GET /api/v1/user/articles/{articleId}/versions/{versionId}/comments
```

---

## 4. 统一响应格式

所有接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 4.1 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | Integer | 业务状态码，`0` 表示成功，非 `0` 表示失败 |
| `message` | String | 提示信息，失败时返回可读错误描述 |
| `data` | Object / Array | 业务数据，失败时可为 `null` 或省略 |

### 4.2 成功响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "title": "如何写好一篇小红书文案"
  }
}
```

### 4.3 失败响应示例

```json
{
  "code": 100102,
  "message": "文章标题不能为空",
  "data": null
}
```

### 4.4 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "pageNum": 1,
    "pageSize": 20,
    "total": 100,
    "pages": 5
  }
}
```

---

## 5. 状态码

### 5.1 HTTP 状态码

| 状态码 | 场景 |
|---|---|
| `200 OK` | 通用成功 |
| `201 Created` | 创建成功 |
| `204 No Content` | 删除成功，无返回体 |
| `400 Bad Request` | 请求参数错误 |
| `401 Unauthorized` | 未登录或 Token 无效 |
| `403 Forbidden` | 无权限 |
| `404 Not Found` | 资源不存在 |
| `409 Conflict` | 资源冲突（如重复提交） |
| `429 Too Many Requests` | 限流 |
| `500 Internal Server Error` | 服务器内部错误 |

### 5.2 业务错误码

采用 **6 位数字分段**：

```text
1XXYYY
```

| 段 | 含义 |
|---|---|
| `1` | 固定前缀 |
| `XX` | 端或模块 |
| `YYY` | 具体错误序号 |

#### 端/模块编码

| 编码 | 含义 |
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

#### 错误示例

| 错误码 | 含义 |
|---|---|
| `100001` | 系统繁忙 |
| `100002` | 参数校验失败 |
| `101001` | 用户端通用：操作过于频繁 |
| `111001` | 手机号格式错误 |
| `111002` | 验证码错误 |
| `111003` | 账号或密码错误 |
| `113001` | 文章不存在 |
| `114001` | 生成任务不存在 |
| `115001` | 额度不足 |

---

## 6. 认证与鉴权

### 6.1 Token 传递

登录成功后返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200
  }
}
```

后续请求在 Header 中携带：

```text
Authorization: Bearer {accessToken}
```

### 6.2 Token 刷新

```text
POST /api/v1/user/auth/refresh-token
```

请求体：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### 6.3 Token 有效期

| Token | 有效期 |
|---|---|
| Access Token | 2 小时 |
| Refresh Token | 7 天 |

### 6.4 管理端认证

管理端使用独立的 JWT Secret 和认证接口：

```text
POST /api/v1/admin/auth/login
Authorization: Bearer {adminAccessToken}
```

---

## 7. 请求规范

### 7.1 请求体

- 创建/更新操作使用 JSON 请求体。
- 字段命名使用 camelCase。
- 日期时间统一使用 ISO 8601 格式：`2026-06-26T14:30:00.000+08:00`。

### 7.2 查询参数

| 参数 | 说明 | 示例 |
|---|---|---|
| `pageNum` | 当前页，默认 1 | `?pageNum=1` |
| `pageSize` | 每页条数，默认 20，最大 100 | `?pageSize=20` |
| `sortField` | 排序字段 | `?sortField=createdAt` |
| `sortOrder` | 排序方向：`asc` / `desc` | `?sortOrder=desc` |
| `keyword` | 通用搜索关键词 | `?keyword=文案` |

### 7.3 请求 DTO 命名

| 场景 | 命名 |
|---|---|
| 创建请求 | `Create{Module}Request` |
| 更新请求 | `Update{Module}Request` |
| 查询请求 | `{Module}PageRequest` / `{Module}QueryRequest` |
| 操作请求 | `{Action}{Module}Request` |

### 7.4 响应 VO 命名

| 场景 | 命名 |
|---|---|
| 列表项 | `{Module}VO` |
| 详情 | `{Module}DetailVO` |
| 分页 | 复用 `{Module}VO`，外层用统一分页结构 |

---

## 8. 参数校验

- 使用 Bean Validation（`@Valid`、`@NotBlank`、`@Size` 等）。
- 校验失败统一返回 `400` + 业务码 `100002`。
- 错误信息聚焦到字段：

```json
{
  "code": 100002,
  "message": "参数校验失败",
  "data": {
    "title": "文章标题不能为空",
    "wordCount": "字数范围必须在 100-3000 之间"
  }
}
```

---

## 9. 幂等性

### 9.1 强制幂等的接口

以下接口必须实现幂等：

- 所有创建类接口（`POST`）
- 支付类接口
- 额度扣减类接口

### 9.2 幂等实现

客户端在请求头中携带幂等键：

```text
Idempotency-Key: {uuid}
```

服务端使用 MySQL 唯一索引或 Caffeine 缓存记录已处理的幂等键，有效期 24 小时。

重复请求返回上一次的结果，不重复执行业务。

---

## 10. 限流

- 本地限流使用 Bucket4j / Guava RateLimiter。
- 限流触发时返回 `429` + 业务码 `100003`。
- 响应头中可携带：

```text
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1719392400
```

---

## 11. 跨域

由后端统一配置 CORS，允许的来源按环境区分：

- 本地开发：`http://localhost:5173`
- 生产环境：管理后台域名 + 用户端域名

---

## 12. 接口文档

- 使用 Knife4j（Swagger 增强）生成接口文档。
- 访问地址：
  - 用户端：`/doc.html`
  - 管理端：`/admin/doc.html`
- 所有 Controller、DTO、VO 必须写 Swagger 注解。

---

## 13. 示例

### 13.1 用户登录

```text
POST /api/v1/user/auth/login
```

请求：

```json
{
  "phone": "13800138000",
  "password": "yourPassword"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "user": {
      "id": 10001,
      "nickname": "创作者小王"
    }
  }
}
```

### 13.2 提交生成任务

```text
POST /api/v1/user/generation-tasks
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

请求：

```json
{
  "topic": "夏季防晒产品推广",
  "wordCount": 1500,
  "styleCode": "xiaohongshu",
  "templateCode": "wechat-standard"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": 20001,
    "bizNo": "GEN202606260001",
    "status": 0,
    "queuePosition": 3
  }
}
```

### 13.3 查询文章列表

```text
GET /api/v1/user/articles?pageNum=1&pageSize=20&keyword=文案
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 10001,
        "title": "如何写好一篇小红书文案",
        "articleStatus": 1,
        "createdAt": "2026-06-26T14:30:00.000+08:00"
      }
    ],
    "pageNum": 1,
    "pageSize": 20,
    "total": 100,
    "pages": 5
  }
}
```

---

## 14. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：URL 版本、6 位错误码、JWT 认证、强制幂等、ISO 8601 时间、Knife4j 文档 | - |
