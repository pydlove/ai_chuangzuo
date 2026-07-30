# 提示词市场/个人提示词使用计数设计

**日期**: 2026-07-30
**状态**: 已确认，待实现
**关联模块**: `project/user/api`、`project/admin/api`

---

## 1. 背景与目标

用户端创作流程选择提示词后生成文章时，需要按提示词来源分别处理使用计数：

1. **市场提示词**：生成成功后累计使用次数并产生收益。
2. **自己的提示词**（自定义/学习）：生成成功后只累计使用次数，不产生收益。
3. **系统预设提示词**：不计数、不产生收益。

同时要求：

- 失败和手动停止的任务不计入使用次数与收益。
- 计数走与消息通知相同的异步 outbox 队列，避免阻塞生成 pipeline。
- 在提示词市场收益规则中明确说明上述差异。

---

## 2. 设计决策

### 2.1 计数时机：成功时异步计数

采用「生成成功后才计数」的语义：

- 在 admin 的 `GenerationTaskService.markCompleted` 中，与 `generation_completed` 通知 outbox 同一事务内写入 `skill_usage` outbox。
- `markFailed` 与用户手动停止不写入 `skill_usage` outbox。

因此失败/停止时根本不会产生计数事件，天然满足「失败和停止应回滚」的要求，无需额外补偿状态机。

### 2.2 异步队列复用现有消息 outbox

复用 `a_message_notify_outbox` 表与 `NotifyOutboxDispatcherJob`：

- 新增 `biz_type = "skill_usage"`。
- 新增 `SkillUsageHandler` 将计数请求派发到 user-api 内部接口。
- user-api 内部接口根据 `skillRef` 判断来源并分别计数。

### 2.3 移除 pipeline 中的同步调用

当前 `PersistArticleStep` 同步调用 `SkillMarketInternalClient.recordUsage()`，改为：

- 在 `PersistArticleStep` 中仅负责 article 持久化。
- 计数逻辑下沉到 `markCompleted` 后的 outbox 派发流程。

---

## 3. 数据模型

无需新增表。复用现有表：

| 表 | 用途 |
|---|---|
| `a_message_notify_outbox` | admin 异步计数事件队列 |
| `u_skill_market` | 市场提示词累计/月度使用次数与收益 |
| `u_user_skill` | 个人提示词累计使用次数 `use_count` |
| `u_earnings_record` | 市场提示词产生的 `USAGE` 收益记录 |

---

## 4. 来源判定规则

user-api 内部接口收到 `{ taskId, userId, skillRef }` 后按以下顺序判定：

1. **市场提示词**：`skillRef` 匹配 `u_skill_market.biz_no` 且 `audit_status = 1`、`enable_status = 1`、`is_deleted = 0`。
2. **个人提示词**：`skillRef` 匹配当前用户的 `u_user_skill.skill_name` 且 `source_type` 为 `1`（自定义）或 `2`（学习），`is_deleted = 0`。
3. **系统预设**：`skillRef` 匹配 `u_user_skill.source_type = 3`。不作计数与收益处理。
4. 未命中任何来源：记录 warn 日志，不抛异常，避免阻塞 outbox 派发。

---

## 5. 计数行为

### 5.1 市场提示词

调用现有 `SkillMarketUsageService.recordUsage(bizNo, consumerUserId)`：

- `u_skill_market.total_uses + 1`
- `u_skill_market.monthly_uses + 1`
- `u_skill_market.monthly_earnings + price`
- 插入一条 `u_earnings_record`，`type = USAGE`，`source_type = skill_market`，`status = 0`（未结算）

### 5.2 个人提示词

执行：

- `u_user_skill.use_count + 1`
- 不写 `u_earnings_record`

### 5.3 系统预设

直接返回，无状态变更。

---

## 6. 接口变更

### 6.1 admin 内部：新增 handler

新增 `com.aichuangzuo.admin.modules.message.handler.SkillUsageHandler`：

- `bizType()` 返回 `"skill_usage"`。
- `dispatch(NotifyOutbox row)` 解析 payload，调用 user-api 内部接口。

### 6.2 user-api 内部接口

在 `/api/v1/user/internal/market-skills` 下扩展，或新增 `/api/v1/user/internal/skills/use`：

```
POST /api/v1/user/internal/skills/use
Content-Type: application/json
X-Internal-Key: {internal-key}

{
  "taskId": 123,
  "userId": 456,
  "skillRef": "SKxxxxxxxx"
}
```

返回 `Result<Void>`。user-api 内部完成来源判定与计数。

---

## 7. 关键代码改动点

| 文件 | 改动 |
|---|---|
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java` | `markCompleted` 中增加 `skill_usage` outbox 写入 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/PersistArticleStep.java` | 移除 `SkillMarketInternalClient.recordUsage()` 同步调用 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/message/handler/SkillUsageHandler.java` | 新增 handler |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/controller/SkillMarketInternalController.java` 或新建 controller | 新增/扩展内部接口，统一处理提示词使用计数 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/UserSkillServiceImpl.java` 或相关 service | 新增个人提示词 use_count 自增方法 |
| 前端提示词市场规则文案 | 补充「市场提示词产生收益，自己的提示词只计数」说明 |

---

## 8. 幂等与重试

- outbox 派发失败后按现有策略指数退避，最多 5 次。
- `SkillMarketUsageService.recordUsage` 当前无去重。task 与 article 一一对应，且 `markCompleted` 只执行一次，正常不会重复。如后续需要强幂等，可在 `u_earnings_record` 增加 `task_id` 或基于 `taskId` 去重；本次不引入。
- 个人提示词 `use_count` 自增本身幂等语义宽松，允许任务重试时多次累加；如需精确，可基于 `taskId` 做去重表。本次不引入。

---

## 9. 失败/停止处理

| 场景 | 是否写入 skill_usage outbox | 结果 |
|---|---|---|
| 生成成功 | 是 | 异步计数 |
| 生成失败（admin worker markFailed） | 否 | 不计数，文章额度退还 |
| 用户手动停止 | 否 | 不计数，文章额度退还 |
| outbox 派发失败达最大重试 | 记录 FAILED，人工介入 | 不阻塞主流程 |

---

## 10. 前端收益规则文案

在提示词市场规则弹框/帮助页面增加：

> 1. 使用提示词市场上的提示词生成文章并成功后，该提示词的累计使用次数与本月使用次数会增加，创作者可获得对应创作币收益。
> 2. 使用自己创建或学习获得的提示词生成文章并成功后，仅累计该提示词的使用次数，不产生收益。
> 3. 生成失败或手动停止的任务不计入使用次数与收益。

---

## 11. 测试计划

1. 使用市场提示词生成文章成功，确认 `u_skill_market.total_uses/monthly_uses` 增加、`u_earnings_record` 出现 `USAGE` 记录。
2. 使用自定义提示词生成文章成功，确认 `u_user_skill.use_count` 增加，无收益记录。
3. 生成失败任务，确认市场/个人提示词计数均无增加。
4. 用户手动停止任务，确认计数无增加。
5. 验证 `a_message_notify_outbox` 中存在 `skill_usage` 记录且最终被派发为 `SENT`。
6. 验证前端提示词市场规则文案已更新。

---

## 12. 不在本次范围

- 强幂等去重表（如后续重复计数问题出现再补）。
- 管理端收益规则配置页面。
- 系统预设提示词的使用统计。
