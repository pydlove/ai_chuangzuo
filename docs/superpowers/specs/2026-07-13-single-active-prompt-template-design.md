# 创作模板单一生效 + 用户端创作直连 设计

日期：2026-07-13
状态：已通过设计评审，待编写实施计划

## 1. 背景与问题

管理端"创作提示词"管理一套 12 阶段 AI 写作流水线模板（`t_prompt_template` + `t_prompt_template_stage`，一个模板 = 12 条阶段配置，约 7000+ 字 prompt）。当前存在三个问题：

1. **"唯一生效"从未真正强制**。代码注释写着"runtime 仅允许 1 个 enabled=1"，但数据库里现有 3 条"已发布"记录（id=1 默认去 AI 味模板、id=2 测试-小红书模板、id=4 阶段3-E2E），且 enabled 全为 1。`enabled`（旧字段）与 `template_status`（现行字段）双轨并存，语义重叠。
2. **用户端可以让用户挑模板**。`/console/ai-generate` 页的"创作模板"下拉列出所有已发布模板任用户选择，与"平台只运营一套写作配方"的产品意图相悖。
3. **`/console/ai-generate` 页本身不是产品需要**。它是一次开发过程的产物（路由 + 菜单"AI 创作" + `GenerationQueueIndex.vue`），用户从未要求。创作主路径应该是：创作页（`/console/create`）填标题/要求 → 点"生成文章" → 直接按已发布模板的 12 阶段执行。

## 2. 目标

- 管理端任意时刻最多 1 条"已发布"创作模板，发布即唯一生效。
- 用户端创作页点"生成文章"直接提交生成任务，后端自动按唯一已发布模板的最新版本执行 12 阶段，用户无模板选择环节。
- 移除 `/console/ai-generate` 页及其全部关联代码。
- 运营改提示词（编辑→重新发布）和开发加阶段（枚举加一项）两条扩展路径保持简单。

## 3. 核心决策（评审结论）

| 决策点 | 结论 | 备选（已否决） |
|---|---|---|
| 生效模型 | 发布 = 唯一生效：发布 A 时事务内自动下线其他；`template_status=PUBLISHED` 全表最多 1 条；删除旧 `enabled` 字段 | 发布与生效分离（多一层语义）；只收用户端（管理端双轨保留） |
| 用户端模板选择 | 完全移除：不传 templateId，后端自动锁定唯一已发布模板 | 只读展示当前模板名 |
| `/console/ai-generate` | 整个移除（路由/菜单/页面/引用） | 保留 |
| 加阶段方式 | 枚举集中扩展：`PipelineStage` 加一项，管理端编辑页自动渲染 | 全数据驱动（管理端 UI 加"新增阶段"按钮，改动大） |

## 4. 核心不变量

`t_prompt_template` 表中 `template_status=1`（已发布）的记录任意时刻最多 1 条，由发布事务保证（先全表下线再发布目标，同事务）。历史任务已通过 `template_id + template_version` 锁定版本快照，不受后续发布影响。

## 5. 数据流

```
管理端 创作提示词页                用户端 创作页 (/console/create)
┌─────────────────────┐          ┌──────────────────────────────┐
│ 编辑 12 阶段 prompt  │          │ 填标题/要求 → 点"生成文章"     │
│ 点"发布"（二次确认） │          │        ↓ POST /generation-tasks
│   ↓ 事务：           │          │   （不带 templateId）         │
│ ① 全表下线           │          │        ↓ user-api 自动锁定     │
│ ② 目标=已发布+v+1    │          │   唯一已发布模板最新版本       │
│ ③ 写版本快照         │          │        ↓                     │
└─────────────────────┘          │ 右侧队列面板轮询显示进度       │
         │ 共享库                 │ （queued→generating→完成）     │
   t_prompt_template ◄──────────│ 完成条目点击 → 我的作品         │
   （最多 1 条 PUBLISHED）        └──────────────────────────────┘
         │
         ↓ admin-api worker 捡任务 → 按锁定 templateId+version 快照跑 12 阶段 → 落 u_article
```

## 6. 管理端改动（admin-api / admin-web）

### 6.1 发布唯一化

`PromptTemplateService.publish`：事务内三步——① `UPDATE t_prompt_template SET template_status=2 WHERE template_status=1`（下线当前已发布）；② 目标模板置 `template_status=1`、`latest_published_version+1`；③ 写 `t_prompt_template_version` 快照（现有逻辑保留）。

### 6.2 删除 enabled 双轨

- 新迁移 `V2.0.0_024__drop_template_enabled_and_enforce_single_published.sql`（admin-api）：
  - 数据修正：保留 id 最小（内置默认模板）的已发布记录，其余 `template_status=1` 的置为 `2`；
  - `ALTER TABLE t_prompt_template DROP COLUMN enabled`。
- 同步删除：`PromptTemplate.enabled` 字段、`PromptTemplateAdminController` 的 `POST /{id}/enable` 与 `POST /{id}/disable`、`PromptTemplateService.enable/disable/findEnabled`、Mapper 的 `selectEnabled`（接口 + XML）。
- 注意：阶段表 `t_prompt_template_stage.enabled`（阶段级开关）是另一字段，**保留不动**。
- `PipelineTemplateResolver` fallback（老任务无锁定版本，L62-64）：从 `findEnabled()` 改为查唯一 `template_status=1` 的 `findPublished()`。

### 6.3 管理端 UI

- 列表页"发布"按钮加二次确认："发布后将自动下线当前已发布模板，确认？"
- 已发布行展示绿色"生效中"tag；其余行不展示生效状态。
- 12 阶段编辑页不动。

## 7. 用户端改动（user-web / user-api）

### 7.1 移除 /console/ai-generate

- 删 `project/user/web/src/views/console/GenerationQueueIndex.vue`；
- 删路由（`router/index.js` L58-60）与 `ConsoleLayout.vue` navItems 中"AI 创作"项（L1254）；
- 全仓 grep `/console/ai-generate` 清零：创作页"查看更多"×2 改指 `/console/works`，`handleGenerate` 跳转改为直接提交（见 7.2），e2e 脚本如有引用同步处理。

### 7.2 创作页直接提交

- `handleGenerate`：校验标题/要求后 `POST /api/v1/user/generation-tasks`（标题/要求/平台/风格/字数，不带 templateId），成功 toast"已加入生成队列"，停留创作页；
- 右侧"生成队列"面板（已是后端轮询 5s）自动出现新任务并推进状态；
- 面板已完成条目点击 → `/console/works`；
- 队列条数限制等现有前端校验保留。

### 7.3 死代码清理

- `CreateIndex.vue`：`loadAvailableTemplates` / `selectedTemplateId` / `availableTemplates` 及 `listPromptTemplates` import（当前已是无人使用的加载逻辑）；
- `api/generation.js`：`listPromptTemplates`；
- user-api：`PromptTemplateQueryController`、`PromptTemplateQueryService`、`PromptTemplateQueryServiceTest`（无其他调用方，grep 确认后删）；
- `GenerationSubmitRequest` 去掉 `templateId` 字段。

### 7.4 提交时锁定模板

`GenerationTaskService`：不再读前端 templateId；改用 `UserPromptTemplateMapper` 新增 `selectPublished()`（`template_status=1 AND is_deleted=0 LIMIT 1`）取唯一已发布模板，锁定 `id + latestPublishedVersion` 写入任务行。查不到 → 业务错误"创作模板未发布，请联系管理员"。

## 8. 生成链路（改动点加粗）

```
创作页 POST /generation-tasks（无 templateId）
  → user-api GenerationTaskService
      【改】findPublished() 取唯一已发布模板，锁定 id+version 写入任务
  → admin-api worker 捡任务（现有）
  → PipelineTemplateResolver 按锁定 templateId+version 取 12 阶段快照
      【改】fallback 从 enabled=1 改为唯一 PUBLISHED
  → GenerationPipeline 逐阶段执行 → 完成落 u_article
```

## 9. 扩展路径

**改提示词**（已通，无需开发）：管理端编辑阶段 prompt → 重新发布 → 版本号 +1 → 新任务用新版本，老任务按锁定版本跑完。

**加阶段**（枚举集中扩展）：

1. `PipelineStage` 加一项枚举（key/类型/默认 prompt/占位符/进度权重，一处写完）；
2. AI 类阶段：通用 AI 执行步自动生效；规则类阶段：补一个 `GenerationStep` Bean；
3. 管理端编辑页从 `PipelineStage.ALL` 渲染，新阶段自动出现；
4. 老模板调 `init-stages` 补齐新阶段行；不补也能跑（resolver 有枚举默认值兜底）。

## 10. 错误处理

- 无已发布模板时提交生成 → 业务错误码 + 提示"创作模板未发布，请联系管理员"；
- 发布事务失败 → 整体回滚，不出现 0 条或 2 条已发布的中间态；
- 下线当前唯一已发布模板 → 允许（管理端显式操作），此后用户端提交生成按上一条报错。

## 11. 测试

- admin-api 单测：publish 唯一性（发布 B 后 A 自动下线、全表 PUBLISHED=1）；
- user-api 单测：提交时不传 templateId → 锁定唯一已发布模板版本；无已发布模板 → 业务错误；
- 前端：`npm run build` 通过 + 创作页手动验证 提交→面板进度→作品落库；
- e2e：grep 确认 `/console/ai-generate` 与 `listPromptTemplates` 无残留引用。

## 12. 风险与边界

- 存量数据违反新不变量（3 条已发布）：由 V2.0.0_024 迁移修正，部署即收敛；
- 老任务（templateId/version 为 NULL）走 resolver fallback 到当前唯一已发布模板，行为与现状一致（语义从 enabled 换成 PUBLISHED，迁移后两者指向同一条）；
- worker 在 admin-api、提交在 user-api，两端共用 `t_prompt_template`，本次改动不涉及跨服务通信；
- 版本快照（`config_json`）是阶段配置的不可变拷贝，模板后续编辑不影响在途任务。
