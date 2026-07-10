# 用户端「创作」队列化生成实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 用户在创作页点「生成文章」时，任务入数据库队列 → 管理端 worker 异步处理 AI 调用 → 直接产出 article 入 `u_article`；用户端通过轮询查进度，失败可重试。配套管理端「创作提示词」可编辑启用的 prompt 模板。

**Architecture:**
- 两个 Spring Boot 应用共享 MySQL：admin-api 跑 worker（线程池 + 轮询 + `SELECT ... FOR UPDATE SKIP LOCKED`），user-api 提交任务 + 轮询进度 + 退额度。
- 队列表 `a_generation_task`（admin-api 持有 migration + Flyway）。
- 历史归档 `a_generation_history`（admin-api 持有 migration + 定期任务迁移过期任务）。
- 提示词模板 `t_prompt_template`（admin-api 持有 migration + Vue 后台管理）。
- 实体放 shared（`GenerationTask`、`PromptTemplate`），两端都引。
- AI 调用走已有的 `AiProvider`（Kimi / Minimax），model 由 task 记录里的 `model_config_id` 决定，发起任务时 admin 端读 model 配置挑 provider。
- 失败重试：worker 内自动重试 3 次，全失败调用 user-api 内部接口退额度 + 更新 task 状态；用户端点「重新生成」写新 task（不复用旧 task）。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8 (with `FOR UPDATE SKIP LOCKED`), Flyway, Caffeine, Vue 3, Ant Design Vue, Playwright.

---

## 已锁定的设计决策

| 项 | 值 |
|---|---|
| worker 线程数 | `generation.pool-size: 2`（配置可改） |
| admin-api 实例数 | 单实例（不预留横向扩） |
| 任务粒度 | 1 篇 = 1 任务，FIFO 公平（按 `created_at ASC`） |
| 草稿 / 成稿 | 直接生成成 article，**不经过 `u_draft`** |
| 任务失败重试 | worker 自动重试 3 次，仍失败：退额度 + 任务置 failed |
| 重新生成 | 用户端点按钮写**新 task**（不复用旧 task） |
| AI 模型选择 | 用现有 admin 模型配置；task 记 `model_config_id` |
| 字数 | 用户端 ≤ 3000 字；超 `wordCount * 1.1` 截断到 3000，**不重试** |
| 任务保留期 | 提交时按用户会员等级算 `retention_days`：basic 30 / pro +∞ |
| 清理 | cron 每天把过期 task 从 `a_generation_task` 迁到 `a_generation_history`（cold storage） |
| task 写入 | user-api 直接写共享 DB（同 admin-api 读 `u_message` 模式） |
| worker 触发 | 纯轮询，500ms 一次（user-api 不通知） |
| 共享实体位置 | `project/shared/.../entity/GenerationTask.java`、`PromptTemplate.java` |
| quota | 用户权益决定每分钟上限：基础 1 / 专业 5 / 旗舰 10；入队预扣，失败退 |
| lease timeout | 5 分钟（worker claim 后超时回 `queued`） |
| 提示词模板 | 3 段：`base_content` / `user_style_guidance` / `system_prompt_json`；多模板共存但只有 1 个 enabled |

---

## 状态机

```
queued ──claim──► processing ──done──► completed ──retention─► archived(迁入 history)
                  │
                  ├──fail──► retry (auto, 最多 3 次) ──回 queued
                  └──fail──► failed (退额度)
processing ──lease 超时 ──回 queued
```

### AI 输出 JSON Schema（worker 强校验）

```json
{
  "title": "string ≤30",
  "summary": "string ≤80",
  "sections": [
    { "heading": "string", "paragraphs": ["..."] }
  ],
  "imageHints": [
    { "afterSection": 1, "hint": "..." }
  ],
  "meta": { "tone": "informal" }
}
```

- `sections.length` 必须 3-5（不达标重试 1 次，仍不达标 → 业务失败）
- 总字数 = sum(sections.paragraphs) ≤ `wordCount * 1.1`，超出截断到 3000，**不重试**
- JSON 解析失败 → 触发重试 1 次，仍失败 → 业务失败

### 三段拼装发给 AI

```
system:  base_content + "\n\n" + system_prompt_json
user:    render(user_style_guidance) + 用户输入（title / description / platform / wordCount / toneTags）
```

占位符统一用 `{{name}}`，渲染引擎极简正则替换即可。

---

## Global Constraints

- 所有表变更通过 Flyway 脚本执行，表前缀：`a_`（admin 业务表）、`t_`（admin 配置 / 模板表）、`u_`（用户业务表）。
- admin 业务表强制字段：`tenant_id`、`is_deleted`、`created_at`、`updated_at`、`created_by`、`updated_by`、`biz_no`。
- 时间字段使用 `DATETIME(3)`，应用层 UTC+8（与 `BaseEntity` 约定一致）。
- 用户端 / 管理端业务代码隔离，禁止互相依赖 `service`；跨端调用统一走 `user-api` 的 `/api/v1/user/internal/**` 内部接口（已存在 `InternalKeyAuthenticationFilter`）。
- 接口规范：`/api/v1/{user|admin}/{module}`，统一响应 `{code, message, data}`。
- 错误码：用户端追加到 `UserAuthErrorCode` 或新 `UserGenerationErrorCode`；管理端追加到 `AdminGenerationErrorCode`。
- 每个 Task 必须有可独立验证的测试或命令，每个 Task 结束 commit。

---

## File Structure

### 数据库迁移（admin-api 持有）

| 文件 | 职责 |
|---|---|
| `project/admin/api/src/main/resources/db/migration/V2.0.0_009__create_generation_task_table.sql` | `a_generation_task` 队列表 + 索引（FIFO、用户、状态） |
| `project/admin/api/src/main/resources/db/migration/V2.0.0_010__create_generation_history_table.sql` | `a_generation_history` cold storage |
| `project/admin/api/src/main/resources/db/migration/V2.0.0_011__create_prompt_template_table.sql` | `t_prompt_template` 提示词模板表 + 唯一启用索引 |
| `project/admin/api/src/main/resources/db/migration/V2.0.0_012__add_quota_per_minute_to_membership.sql` | `u_user.coin_balance_quota_per_minute` 限流字段（按用户权益在入队时查询权益，不一定需要单独字段，写在 user-api 已有权益配置）— **取消，本期复用权益配置枚举** |

### 共享实体（两端依赖）

| 文件 | 职责 |
|---|---|
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationTask.java` | extends `BaseEntity`；字段 `biz_no, target_user_id, status, model_config_id, prompt_template_id, input_param (JSON), word_limit_target, retry_count, max_retry, locked_at, locked_by, failed_reason, completed_at, retention_days, lease_until` |
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplate.java` | extends `BaseEntity`；字段 `name, base_content, user_style_guidance, system_prompt_json, enabled, remark` |
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationHistory.java` | extends `BaseEntity`；从 task 表裁剪：见上「我定的 #14 / #11」字段表 |
| `project/shared/src/main/java/com/aichuangzuo/shared/enums/GenerationTaskStatus.java` | 枚举 `QUEUED / PROCESSING / COMPLETED / FAILED` |
| `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java` | 管理端错误码 |
| `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserGenerationErrorCode.java` | 用户端错误码 |

### admin-api：worker + template + retention

| 文件 | 职责 |
|---|---|
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/GenerationTaskEntity.java` | extends `GenerationTask`，加 `@TableName("a_generation_task")`，可做字段别名 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/PromptTemplateEntity.java` | 同上，`t_prompt_template` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationTaskMapper.java` | `BaseMapper`，注入 `claimBatch(int limit, Instant leaseUntil)` 用 `FOR UPDATE SKIP LOCKED` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationTaskMapper.xml` | 同上的 XML |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/PromptTemplateMapper.java` | 同上 + `findEnabled()` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/PromptTemplateMapper.xml` | 同上 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationHistoryMapper.java` | history 表 mapper |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateService.java` + `impl` | CRUD；启用流程：事务内先全表 `enabled=0` 再设当前 `enabled=1` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateRenderService.java` | `{{name}}` 占位符渲染 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java` + `impl` | `claimNext()` 调 Mapper，`markProcessing` / `markCompleted` / `markFailed` / `releaseLeaseExpired` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationExecutor.java` | 单任务执行：选 enabled 模板 → 渲染 → 调 AI provider → 解析 JSON → 校验 → 调 user-api 内部接口保存 article → 退 / 扣额度 → 落库 article + 更新 task |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/ArticleWriteClient.java` | 内部 HTTP 客户端调 `user-api /internal/articles` 保存 article |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/QuotaRefundClient.java` | 内部 HTTP 客户端调 `user-api /internal/coin-records/refund` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/worker/GenerationTaskWorker.java` | `@PostConstruct` 起线程池，循环：claim → 提交执行 → lease 检查 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/job/GenerationRetentionJob.java` | `@Scheduled` 每天 03:00，按 `retention_days` + `created_at` 把过期任务迁到 history |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/controller/PromptTemplateAdminController.java` | 管理端 CRUD API |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/dto/request/*.java` | SaveRequest / ListRequest / EnableRequest |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/*.java` | AdminTemplateVO、AdminTemplatePageVO |
| `project/admin/api/src/main/resources/application.yml` | 加 `generation:` 配置块 |
| `project/admin/api/pom.xml` | 不变（HTTP client 用 Spring RestTemplate 或 WebClient） |

### user-api：submit + poll + retry + quota

| 文件 | 职责 |
|---|---|
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/entity/GenerationTaskEntity.java` | extends `GenerationTask`，`@TableName("a_generation_task")` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/mapper/GenerationTaskMapper.java` | `BaseMapper` + `insert()`（带 `MybatisPlusMetaObjectHandler` 需要的字段） |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/GenerationTaskService.java` + `impl` | `submit()` 限流 + 预扣额度 + 插队；`progress(taskId)`；`retry(taskId)` 写新 task；`listMine(page)` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/GenerationSubmitValidator.java` | 校验输入参数 + 字数 + 用户权益 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/RateLimiter.java` | Caffeine + 每用户每分钟计数；权益基础 1 / 专业 5 / 旗舰 10 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/QuotaPreDeductService.java` | 调用现有 coin 服务扣额度 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/controller/GenerationTaskController.java` | 用户端 5 个端点 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/controller/GenerationTaskInternalController.java` | 内部接口：admin-api worker 调：保存 article、退额度 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/dto/request/*.java` | SubmitRequest、RetryRequest |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/*.java` | GenerationTaskVO、GenerationTaskPageVO |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/ArticleSaveInternalService.java` | 给 internal controller 调，纯内部用，写 `u_article` |

### admin-web：创作提示词管理

| 文件 | 职责 |
|---|---|
| `project/admin/web/src/api/promptTemplate.js` | API 封装 |
| `project/admin/web/src/composables/usePromptTemplate.js` | 列表 + 启用 + 编辑 |
| `project/admin/web/src/views/PromptTemplateView.vue` | 列表页 + 启用按钮 |
| `project/admin/web/src/views/PromptTemplateEditView.vue` | 编辑三段 + 实时预览渲染效果 |
| `project/admin/web/src/router/index.js` | 加 `/console/prompt-templates` 路由 |
| `project/admin/web/src/layouts/AdminLayout.vue` | 系统设置 → 加「创作提示词」菜单项 |
| `project/admin/web/src/components/PromptTemplateRenderer.vue` | 占位符可视化：列出可用占位符 + 选中插入 |

### user-web：创作页接入队列

| 文件 | 职责 |
|---|---|
| `project/user/web/src/api/generation.js` | API 封装（submit / progress / retry） |
| `project/user/web/src/composables/useGenerationTask.js` | 轮询进度 composable |
| `project/user/web/src/views/console/CreateIndex.vue` | 改：点「生成」调 submit → 弹生成队列弹框 → 显示进度 / 失败重试 |
| `project/user/web/src/views/console/GenerationQueueIndex.vue`（新建或复用 WorksIndex） | 用户生成历史任务列表（status=queued/processing/failed/completed 聚合视图） |
| `project/user/web/src/components/GenerationProgressModal.vue` | 显示进度 + 「重新生成」按钮 |

### 测试

| 文件 | 职责 |
|---|---|
| `project/shared/src/test/java/.../entity/BaseEntityTest.java` | 占位符渲染器单测 |
| `project/admin/api/src/test/java/.../modules/generation/service/PromptTemplateRenderServiceTest.java` | `{{name}}` 渲染 |
| `project/admin/api/src/test/java/.../modules/generation/service/GenerationExecutorTest.java` | 模拟 AI 调返回 JSON，校验落 article |
| `project/admin/api/src/test/java/.../modules/generation/worker/GenerationTaskWorkerTest.java` | claim → 多线程不重复 + lease 超时回收 |
| `project/admin/api/src/test/java/.../modules/generation/job/GenerationRetentionJobTest.java` | 过期任务正确迁移 |
| `project/user/api/src/test/java/.../modules/generation/service/RateLimiterTest.java` | 限流配额正确 |
| `project/user/api/src/test/java/.../modules/generation/service/GenerationTaskServiceTest.java` | submit → 预扣；retry 写新 task |
| `tests/e2e/verify_generation_queue.py` | Playwright 跨端联调：admin 启用模板 + user 提交 → 跑完 → 用户端看到 article |

---

## 实施顺序

### Phase 1：基础设施（DB + 共享实体）

1. Flyway 3 个 migration（admin-api）
2. shared 实体 + 枚举 + 错误码（先编译，确保两端能引到）
3. admin/user 各自的 mapper 镜像

### Phase 2：admin worker 内核

4. `GenerationTaskService`（claim / complete / fail / lease 回收）
5. `GenerationTaskWorker`（线程池 + 轮询）
6. `GenerationExecutor`（AI 调用 + JSON 校验 + 落 article）
7. `ArticleWriteClient` / `QuotaRefundClient`（内部 HTTP）
8. user-api `GenerationTaskInternalController`（暴露保存 article / 退额度）

### Phase 3：用户端提交 / 查询

9. user-api `RateLimiter` + `QuotaPreDeductService`
10. user-api `GenerationTaskService` + controller
11. user-web 接入 + 进度弹框

### Phase 4：admin prompt template 管理

12. admin-api `PromptTemplateService` + CRUD controller
13. admin-web 列表 / 编辑 / 启用界面

### Phase 5：清理 / 测试 / 联调

14. `GenerationRetentionJob`（每日凌晨迁移）
15. 单测 + E2E 脚本

---

## 关键决策点

| 决策 | 锁定 |
|---|---|
| 实体归属 | `shared/`（双端都要写） |
| migration 归属 | `admin-api/src/main/resources/db/migration/`（admin 拥有 worker + 配置 + 清理） |
| 跨端调用 | user-api 暴露内部接口 `/api/v1/user/internal/**` 给 admin-api worker 调（写 article、退额度） |
| AI provider 选型 | `GenerationExecutor` 按 `task.model_config_id` 查 admin 模型配置 → 拿 provider 名 → 选 `KimiProviderClient` / `MinimaxProviderClient` |
| 启用模板唯一性 | 程序层事务内先全表 enabled=0 再 UPDATE |
| 限流在 user-api | 每分钟 quota 来自 `MembershipLevel`（已有权益配置），不用新增字段 |
| 历史归档字段裁剪 | 砍 `tenant_id / updated_at / updated_by / is_deleted / retention_days / locked_by`，加 `duration_ms` + `archived_at` |

---

## 验证

### 编译

```bash
mvn -pl project/shared -am install -DskipTests
mvn -pl project/admin/api -am compile -DskipTests
mvn -pl project/user/api -am compile -DskipTests
```

### 单测

```bash
mvn -pl project/admin/api test -Dtest='Generation*Test,Prompt*Test'
mvn -pl project/user/api test -Dtest='Generation*Test,RateLimit*Test'
```

### 端到端

```bash
# 启动 admin-api + user-api + user-web（端口参考现有 scripts/local/start.sh）
python3 tests/e2e/verify_generation_queue.py
```

E2E 覆盖：

1. admin 端登录 → 进入「创作提示词」→ 创建模板 → 启用
2. user 端登录 → 进入创作页 → 点「生成文章」→ 弹框显示「排队中」
3. 等待 ≤ 30s → 弹框切到「已完成」→ 「查看作品」跳到 article 详情
4. 验证 `u_article` 已落库（user_id、body 含 AI 输出正文）
5. 验证 `a_generation_task.status='completed'`
6. 验证额度：扣减正确（成功才扣，不预扣退还）
7. 手动制造失败（关掉网络或换无效 model_config）→ 用户端显示「生成失败」+「重新生成」按钮
8. 「重新生成」→ 写新 task（不是复用旧 task），旧 task 保留 status=failed
9. 限流：连点 11 次「生成」后第 11 次报「操作过于频繁，请稍候」
10. 归档：把 retention_days 改成 0 → 等 retention job 跑 → 任务进 `a_generation_history` 不在 `a_generation_task`
