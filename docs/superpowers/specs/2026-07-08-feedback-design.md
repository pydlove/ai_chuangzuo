# 用户端意见反馈功能设计

> 2026-07-08 · 端到端：用户提交 → 后端落库 → 管理端回复 → 用户消息通知

## Context

`project/user/web/src/views/console/MineIndex.vue:540-572` 已有反馈弹框 UI，包含「反馈类型」（功能建议 / 问题反馈 / 其他）与「反馈内容」。当前 `submitFeedback` 只 `console.log`，没有持久化、没有通知链路。

目标：
1. 用户能在现有弹框中提交真实反馈，落到后端
2. 管理端可看待回复列表，回复用户
3. 用户在消息中心收到反馈回复通知
4. 防止刷量：每个用户 24h 内最多 5 条

非目标：
- 富文本 / 图片附件
- 多轮对话（管理员只能回复一次）
- 用户端主动查看自己的反馈历史（本期通过消息中心间接看到）

## 决策

| 主题 | 选择 | 理由 |
|---|---|---|
| 表 | 单表 `u_feedback`，reply 字段内联 | 单轮回复足够，避免双表 join |
| 类型字段 | VARCHAR(32) 存中文 label | 与现有 `feedbackTypes` 常量对齐 |
| 联系方式 | VARCHAR(128) 可空（手机或邮箱） | 用户主动填，无需校验格式 |
| 状态机 | `status TINYINT`：0 待回复 / 1 已回复 / 2 已忽略 | 默认列表只看 status=0 |
| 限频 | DB count + 24h 窗口，无中间件 | 已有表内查询足够，无 Redis 依赖 |
| 通知 | 复用 `MessageService.pushPersonal(userId, 'feedback', title, summary, null, content, 'reply')` | 消息中心已支持 `content`+`subType`，零侵入 |
| 管理员身份 | 用现有 `a_admin.id`，admin-api 自带 JWT 鉴权 | 无需新角色 |
| 限频数值 | 5 条 / 24h | 防刷而非拦截业务异常 |

## 数据形状

### Flyway 迁移 `V1.0.0_021__create_feedback_table.sql`

```sql
CREATE TABLE u_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '提交人',
    type VARCHAR(32) NOT NULL COMMENT '反馈类型',
    content VARCHAR(2000) NOT NULL COMMENT '反馈正文',
    contact VARCHAR(128) DEFAULT NULL COMMENT '联系方式（可选）',
    reply_content VARCHAR(2000) DEFAULT NULL COMMENT '管理员回复',
    reply_admin_id BIGINT UNSIGNED DEFAULT NULL COMMENT '回复管理员',
    replied_at DATETIME(3) DEFAULT NULL COMMENT '回复时间',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 待回复 / 1 已回复',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_u_feedback_user_created (user_id, created_at),
    KEY idx_u_feedback_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈';
```

`@TableLogic` 由 MP 在所有查询自动追加 `is_deleted=0`。

### DTO / VO

```java
// dto/request/SubmitFeedbackRequest
String type;            // 必填，限定三个枚举值
@Size(max=2000) String content;
@Size(max=128) String contact;

// vo/FeedbackVO
Long id; String type; String content; String contact;
String replyContent; LocalDateTime repliedAt;
LocalDateTime createdAt;
Integer status;         // 0 / 1

// vo/AdminFeedbackVO extends FeedbackVO
String userEmail; String userBizNo;
String replyAdminName;
```

## 模块划分

### 后端 user-api

新增包 `com.aichuangzuo.user.modules.feedback/`

- `entity/Feedback.java`
- `mapper/FeedbackMapper.java` + `FeedbackMapper.xml`
- `enums/FeedbackType.java`（功能建议 / 问题反馈 / 其他）
- `enums/FeedbackErrorCode.java`
  - `117001` 今日反馈次数已达上限
  - `117002` 反馈内容不能为空
- `dto/request/SubmitFeedbackRequest.java`
- `vo/FeedbackVO.java`
- `service/FeedbackService.java` + `impl/FeedbackServiceImpl.java`
  - `submit(userId, request)`：校验、限频、insert、返回 `Long id`
  - `countDailySubmissions(userId)`：`SELECT COUNT(*) WHERE user_id=? AND created_at >= ?`（24h 前）
- `controller/FeedbackController.java`
  - `POST /api/v1/user/feedback/submit`

> 注：用户端不需要历史列表接口，避免暴露内部状态。回复的内容通过消息中心透出。

### 后端 admin-api

新增包 `com.aichuangzuo.admin.modules.feedback/`

- `entity/AdminFeedbackView.java`（连表 `u_user` 取用户信息）
- `mapper/AdminFeedbackMapper.java`
- `dto/request/AdminReplyFeedbackRequest.java`（`String content`，`@Size(min=1, max=2000)`）
- `vo/AdminFeedbackVO.java`（含 `userEmail` / `userBizNo` / `replyAdminName` / `createdAt` 等）
- `enums/AdminFeedbackErrorCode.java`
  - `217001` 反馈不存在
  - `217002` 该反馈已回复，不可重复
- `service/AdminFeedbackService.java` + `impl`
  - `pagePending(page, size)`、`pageReplied(page, size)`、`detail(id)`、`reply(id, adminId, content)`
- 调用 client 端 `MessageServiceClient`（已有的 RPC 风格接口）→ `pushPersonal` 发消息

**注意**：admin-api 不能直接 new MessageServiceImpl，需要走已有的内部 RPC 或 feign。看现有是否已有。设计阶段确认下：`project/user/api/src/main/java/com/aichuangzuo/user/modules/message` 是内部模块，被 admin-api 通过 client 调用。

实施时查：是否已有 `internal-client` 模块封装 `MessageService.pushPersonal`。若无，新建 `MessageInternalClient`（HTTP + `internal.api-key`），把 reply 触发的消息发送委托过去。

### 前端 user-web

`project/user/web/src/api/feedback.js`

```js
import request from '@/utils/request'
export const submitFeedback = (data) => request.post('/feedback/submit', data)
```

`project/user/web/src/views/console/MineIndex.vue`

- 新增 `a-input`「联系方式（选填，手机或邮箱）`v-model="feedbackContact"`
- `submitFeedback` 改为：
  ```js
  try {
    await submitFeedback({ type: feedbackType.value, content: feedbackContent.value, contact: feedbackContact.value })
    message.success('反馈已收到，我们会尽快处理')
    feedbackVisible.value = false
    feedbackContent.value = ''
    feedbackContact.value = ''
    feedbackType.value = '功能建议'
  } catch (e) {
    if (e?.code === 117001) message.warning('今日反馈次数已达上限，明天再来')
    else message.error(e?.message || '提交失败')
  }
  ```

### 前端 admin-web

`project/admin/web/src/api/feedback.js`

```js
export const pagePendingFeedbacks = (p) => request.get('/feedbacks', { params: { status: 0, ...p } })
export const pageRepliedFeedbacks = (p) => request.get('/feedbacks', { params: { status: 1, ...p } })
export const getFeedback = (id) => request.get(`/feedbacks/${id}`)
export const replyFeedback = (id, data) => request.post(`/feedbacks/${id}/reply`, data)
```

`project/admin/web/src/views/FeedbackView.vue`：表格 + 抽屉详情 + 回复表单。

路由 + 菜单：现有「用户管理」同级新增「用户反馈」。

## 接口契约

| 方法 | 路径（user-api） | 说明 |
|---|---|---|
| POST | `/api/v1/user/feedback/submit` | 提交反馈，返回 `{id}` |

| 方法 | 路径（admin-api） | 说明 |
|---|---|---|
| GET | `/api/v1/admin/feedbacks?status=0&page=1&size=20` | 分页列表 |
| GET | `/api/v1/admin/feedbacks/{id}` | 详情 |
| POST | `/api/v1/admin/feedbacks/{id}/reply` | 回复，触发消息 |

## 错误处理

| code | 场景 | 处理 |
|---|---|---|
| 117001 | 24h 内 ≥ 5 条 | toast 提示，文案「今日反馈次数已达上限，明天再来」 |
| 117002 | content 为空 | 前端按钮 disabled 拦截；后端防御性抛错 |
| 217001 / 217002 | 管理端 | 后端 JSON 响应，前端 toast |

## 测试

### 后端单测（user-api）

`FeedbackServiceTest`（4 个用例）：
1. `submit_valid_createsRecord` — content 合法，写入一行
2. `submit_dailyLimit_throws` — 模拟 5 条后第 6 次抛 117001
3. `submit_blankContent_throws` — content 空抛 117002
4. `submit_returnsId` — 返回的 `id` 能在 DB 取到

### 后端单测（admin-api）

`AdminFeedbackServiceTest`（3 个用例）：
1. `reply_persistsAndNotifies` — 调 pushPersonal（mock Client）记录 + 入参校验
2. `reply_alreadyReplied_throws` — status=1 时再 reply 抛 217002
3. `detail_missing_returnsNull` — 不存在的 id 抛 217001

### 前端

`npm run build` 通过。

### E2E：`tests/e2e/verify_feedback.py`

1. DB 直接建测试用户 + 登录拿 token
2. 调 `POST /feedback/submit` 6 次：前 5 次 200，第 6 次抛 117001
3. 调管理端 `POST /reply`（用 admin token + content） → 200
4. DB 断言 `status=1, replied_at IS NOT NULL`
5. 断言 `u_message` 出现 `msg_type='feedback', sub_type='reply', target_user_id=用户`
6. 用户调 `GET /api/v1/user/messages` 看到新消息

## 关键文件清单

- 后端 user-api：1 个 SQL + 1 个枚举 + 6 个新 Java + 1 个 Mapper XML + 1 个测试类
- 后端 admin-api：依赖 user-api 的 `MessageInternalClient`（若无则新建），新增 7 个文件 + 1 个测试类
- 前端 user-web：`api/feedback.js` + `MineIndex.vue` 表单增强
- 前端 admin-web：`api/feedback.js` + `FeedbackView.vue` + 路由 + 菜单
- E2E：1 个新 Python 脚本

## 风险与回滚

- admin-api 调 user-api 内部接口：依赖已有 internal client；若无，新建最小客户端（HTTP + `internal.api-key`）
- Flyway 新表失败可回滚（仅新建表，无数据迁移）
- 限频查询无索引会导致慢，但 `idx_user_created` 已加
- 消息发送失败不能阻塞回复：先 update feedback.status，再异步 try-catch 发消息，失败仅打日志

## 实施顺序

1. user-api：迁移 + Entity + Enum + Service + Controller + Test（先绿）
2. admin-api：依赖补全 + Service + Controller + Test
3. user-web：表单加联系方式 + 提交 + 错误提示
4. admin-web：列表 + 详情 + 回复 + 路由
5. E2E 全跑

每步可独立 commit。
