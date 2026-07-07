# 用户端消息中心 前后端 + 数据库设计

## 背景

`project/user/web/src/views/console/ConsoleLayout.vue` 已内置完整的消息中心 UI：右上角铃铛 + 未读角标、弹框内 5 个分类 tab（公告 / 生成完成 / 会员提醒 / 新功能 / 优惠活动）、每条消息展示标题/摘要/时间、未读红点、"全部已读"、点击跳转。目前数据完全存在 `localStorage`（key `aichuangzuo_notifications`）并带前端种子数据。

本设计将该数据层替换为真实的 Spring Boot + MySQL 后端，做到跨设备、服务端持久化。前端 UI 结构基本不动，只把 localStorage 读写换成 API 调用，并按项目规范删除不再使用的 localStorage/种子代码。

## 目标

- 用户端消息中心由后端 + MySQL 持久化，替换现有 localStorage 实现。
- 支持两类消息：广播类（公告 / 新功能 / 优惠活动，发给所有人）与个人类（生成完成 / 会员提醒，发给单个用户）。
- 支持列表查询、单条已读、全部已读，未读数与分类未读数由列表派生。
- 为未来的文章生成 / 会员模块预留进程内推送接口。

## 非目标

- 不做管理端发消息界面（后续独立任务）。
- 不触发生成完成 / 会员提醒（对应后端模块尚不存在），仅预留内部推送 API。
- 不做消息推送（Web Push / 短信 / 邮件）、单条删除、消息搜索。

## 消息类型

| 分类 | msg_type | scope | 产生方 |
|---|---|---|---|
| 公告 | `announcement` | 广播 | Flyway 种子 / 未来管理端 |
| 新功能 | `feature` | 广播 | Flyway 种子 / 未来管理端 |
| 优惠活动 | `promotion` | 广播 | Flyway 种子 / 未来管理端 |
| 生成完成 | `generation` | 个人 | 未来生成模块调 `pushPersonal` |
| 会员提醒 | `membership` | 个人 | 未来会员模块调 `pushPersonal` |

## 数据库设计

Flyway 脚本：`project/user/api/src/main/resources/db/migration/V1.0.0_016__create_message_tables.sql`。遵循 `u_` 前缀、`utf8mb4_unicode_ci`、`DATETIME(3)` 及标准审计列。

### `u_message` — 消息内容表

存储广播 / 个人消息内容。广播消息只存一行，对所有用户（含新注册用户）可见，不按人复制。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AUTO_INCREMENT | 主键 |
| `msg_type` | VARCHAR(32) NOT NULL | announcement / feature / promotion / generation / membership |
| `scope` | TINYINT UNSIGNED NOT NULL | 1=广播（全体），2=个人 |
| `target_user_id` | BIGINT UNSIGNED NULL | 个人消息填用户ID；广播为 NULL |
| `title` | VARCHAR(128) NOT NULL | 标题 |
| `summary` | VARCHAR(512) NOT NULL | 摘要 |
| `link_url` | VARCHAR(256) NULL | 点击跳转路由，如 `/console/works`、`/pricing`；空则前端按类型默认跳转 |
| `tenant_id` | BIGINT UNSIGNED NOT NULL DEFAULT 0 | 租户ID |
| `is_deleted` | TINYINT UNSIGNED NOT NULL DEFAULT 0 | 逻辑删除 |
| `created_at` / `updated_at` | DATETIME(3) | 审计 |
| `created_by` / `updated_by` | BIGINT UNSIGNED DEFAULT 0 | 审计 |

索引：
- `idx_message_scope_type (scope, msg_type)`
- `idx_message_target (target_user_id)`
- `idx_message_created (created_at)`

### `u_message_read` — 用户已读记录表

只存"已读"记录；未读 = 可见消息里没有对应已读行。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT UNSIGNED NOT NULL | 用户ID |
| `message_id` | BIGINT UNSIGNED NOT NULL | 消息ID |
| `read_at` | DATETIME(3) NOT NULL | 已读时间 |
| `tenant_id` | BIGINT UNSIGNED NOT NULL DEFAULT 0 | 租户ID |
| `is_deleted` | TINYINT UNSIGNED NOT NULL DEFAULT 0 | 逻辑删除 |
| `created_at` / `updated_at` | DATETIME(3) | 审计 |
| `created_by` / `updated_by` | BIGINT UNSIGNED DEFAULT 0 | 审计 |

索引：
- `uk_message_read_user_msg (user_id, message_id)` 唯一
- `idx_message_read_message (message_id)`

### 可见性与已读判定

某用户可见消息 = 全部广播消息（`scope=1`） + 该用户的个人消息（`scope=2 AND target_user_id=当前用户`），且 `is_deleted=0`。

查询用 `LEFT JOIN`，join 不上已读行即为未读：

```sql
SELECT m.id, m.msg_type, m.title, m.summary, m.link_url, m.created_at,
       (mr.id IS NOT NULL) AS is_read
FROM u_message m
LEFT JOIN u_message_read mr
  ON mr.message_id = m.id AND mr.user_id = #{userId} AND mr.is_deleted = 0
WHERE m.is_deleted = 0
  AND (m.scope = 1 OR (m.scope = 2 AND m.target_user_id = #{userId}))
ORDER BY m.created_at DESC
LIMIT 200
```

### 种子数据

在迁移脚本中 `INSERT` 几条广播消息（`scope=1, target_user_id=NULL`），覆盖公告 / 新功能 / 优惠活动三类，供 UI 展示。个人消息（生成 / 会员）不种子，对应 tab 显示"暂无消息"。

## 后端模块设计

包路径：`com.aichuangzuo.user.modules.message`，标准分层。

```
modules/message/
├── controller/MessageController.java
├── service/MessageService.java
├── service/impl/MessageServiceImpl.java
├── mapper/MessageMapper.java              # BaseMapper<Message> + 自定义 join 查询
├── mapper/MessageReadMapper.java          # BaseMapper<MessageRead>
├── entity/Message.java                    # @TableName("u_message")
├── entity/MessageRead.java                # @TableName("u_message_read")
├── entity/MessageScope.java               # 常量：广播/个人（1/2）
├── vo/MessageVO.java
└── enums/MessageErrorCode.java
```

自定义 join 查询 SQL 写在 `project/user/api/src/main/resources/mapper/MessageMapper.xml`（与 LeaderboardAggregateMapper 一致的方式）。

### REST 接口（`/api/v1/user/messages`）

均通过 `SecurityUserContext.getCurrentUserId()` 取当前用户，返回统一 `Result<T>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/user/messages` | 返回当前用户全部可见消息（广播+个人），带 `read` 标记，按 `created_at` 倒序，服务端封顶 200 条。前端一次拉取后客户端按 tab 分组、派生未读数。 |
| PUT | `/api/v1/user/messages/{id}/read` | 单条标记已读。校验该消息对当前用户可见，`INSERT ... ON DUPLICATE` 或"存在则跳过"保证幂等。消息不可见/不存在返回 `MessageErrorCode.MESSAGE_NOT_FOUND`。 |
| PUT | `/api/v1/user/messages/read-all` | 把当前用户所有未读的可见消息批量插入已读记录。 |

### 内部推送方法（非 REST，供未来模块进程内调用）

定义在 `MessageService`：

- `Long pushPersonal(Long userId, String msgType, String title, String summary, String linkUrl)` — 插入一条个人消息（scope=2）。
- `Long publishBroadcast(String msgType, String title, String summary, String linkUrl)` — 插入一条广播消息（scope=1，target_user_id=NULL）。

### MessageVO 字段

对齐前端现有字段，`read` 由 join 结果得出：

| 字段 | 来源 |
|---|---|
| `id` | m.id |
| `type` | m.msg_type |
| `title` | m.title |
| `summary` | m.summary |
| `link` | m.link_url |
| `read` | is_read（boolean） |
| `createdAt` | m.created_at |

### 已读语义

手动已读：仅在用户点击单条消息或点击"全部已读"时写入已读记录。打开铃铛弹框不自动标记已读（保持红点准确，用户可逐条消费）。

## 前端设计

### 新增 API 封装 `project/user/web/src/api/message.js`

```js
import request from '@/utils/request'

export function getMessages() {
  return request.get('/messages')
}
export function markMessageRead(id) {
  return request.put(`/messages/${id}/read`)
}
export function markAllMessagesRead() {
  return request.put('/messages/read-all')
}
```

### 改造 `ConsoleLayout.vue`

- `loadNotifications()` → 调用 `getMessages()` 填充 `notifications`。ConsoleLayout 为常驻壳组件，`onMounted` 加载一次即可。
- `handleNotifClick(n)` → 先 `markMessageRead(n.id)`，本地把该条 `read=true`；再按 `n.link` 优先、否则按 `type` 默认跳转（generation→`/console/works`，membership→`/pricing`）。
- `markAllRead()` → 调用 `markAllMessagesRead()`，成功后本地全部置 `read=true`。
- **删除**不再使用的代码：`STORAGE_KEY`、`saveNotifications()`、`seedNotifications()`、`aichuangzuo_notif_seeded` 相关逻辑，以及所有对 `localStorage` 消息读写。
- 保留：`notifTabs`、`currentNotifs`、`unreadCount`、`getUnreadByType`、`formatTime`、`switchTab`、弹框模板结构。
- 字段兼容：后端 VO 返回 `type/title/summary/read/createdAt/link`，模板无需改动。

## 与现有代码的关系

- 认证复用 `SecurityUserContext`，接口前缀 `/api/v1/user`（前端 `request` baseURL 已含 `/api/v1/user`，故前端路径写 `/messages`）。
- 审计列自动填充复用现有 `MyMetaObjectHandler`（`infrastructure/persistence/handler`）。
- 自定义 join 查询复用 `resources/mapper/*.xml` 约定。
- 遵循 CLAUDE.md："不用的代码开发结束后必须删掉"——localStorage 消息逻辑改造后需 `grep` 确认无引用并删除。

## 测试与验证

- 后端：`MessageServiceImpl` 单元/集成测试覆盖可见性过滤（广播对所有人可见、个人仅对目标用户）、已读幂等、read-all 批量、未读判定。
- 端到端：启动用户端前后端，登录后验证铃铛角标数、tab 分类、点击已读、全部已读、跳转，写一个 Playwright 脚本放 `tests/e2e/`。

## 后续可扩展

- 管理端广播消息的增删改查发布界面。
- 生成完成 / 会员提醒模块落地后调用 `pushPersonal` 接入。
- 消息详情页、单条删除、消息搜索、移动端入口。
