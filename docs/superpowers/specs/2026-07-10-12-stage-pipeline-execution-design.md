# 12 阶段流水线运行时增强设计

> 日期：2026-07-10
> 状态：设计稿（待审核）
> 范围：在现有 12 阶段流水线基础上，补齐任务级 retry、AI 参数可配、用户进度可见性、导出样式分工
> 不改：模板管理 UI、12 阶段结构、阶段级 fallback 策略（按用户确认一律不引入）

---

## 1. 背景

12 阶段流水线的代码骨架已全部落地（见 `project/admin/api/.../generation/pipeline/`）：
- `GenerationPipeline` 编排器
- 13 个 step bean（含 `PersistArticleStep`）
- `PipelineStage` 枚举持默认值 + 占位符 + 表单字段
- `DefaultAiGateway` 带阶段内 retry + budget + 调用留痕
- `PipelineTemplateResolver` 支持任务锁定的 (templateId, version)
- 已存在测试：`GenerationPipelineTest` / `PipelineTemplateResolverTest` / `DefaultAiGatewayRetryTest` / 3 个 step 测试

但用户提出 7 条明确诉求 + 委托 Claude 补缺的若干项，需要把现有骨架"接通"成生产可用的运行时。本文逐项给出改动方案。

---

## 2. 用户诉求（已锁定）

| # | 诉求 | 决策 |
|---|------|------|
| 1 | 12 阶段串行，每阶段 3 次重试 | 沿用现有 `a_generation_config.llm_retry_max_attempts`（默认 3），无需改代码 |
| 2 | 无阶段级 fallback，重试耗尽 = 整任务失败 | 沿用现状（任何 step 异常 → worker markFailed），无需改代码 |
| 3 | Claude 补充 | 见 §6 |
| 4 | 加任务级 retry + 可配置 | **新增改动**：wire worker 用现有 `retry_count` / `max_retry` 字段 |
| 5 | 导出样式（前端已有几十套） | **新增决策**：双层方案（后端轻量渲染 + 前端权威渲染） |
| 6 | 用户只看百分比 | **新增改动**：每阶段权重 → ctx.progressPct → 写库 → user-api 暴露 |
| 7 | AI 参数放创作设置 | **新增改动**：`t_prompt_template_stage` 加 `model_params JSON` 列 |
| 8 | Claude 补充 | 见 §6 |

---

## 3. 决策回顾：导出样式双层方案

**后端 stage 12 职责**：从 templateId 提取 platform → 拼 markdown（保留段落结构 + 平台化 wrapper）→ 落 article.body。

**前端职责**：拿到 article.body 后，用现有 `templatePresets.getTemplateStyles(platform)` 做最终视觉导出（弹框预览、复制、卡片图、emoji/标签插入）。

**理由**：
- 不重复实现前端已有的 30 套模板
- article.body 落库用 markdown 通用格式
- 用户导出体验不变（前端继续用模板库）

**代码动作**：
- `ExportRenderStep` 现有实现基本对，仅需补：
  - `wrapByTemplate()` 增加几个常用 platform（douyin 卡片图位、xiaohongshu emoji 排版）
  - 把"前端有几十套"的事实写进 JavaDoc，避免后续误改
- 前端不动（已存在）

---

## 4. 数据库变更

### 4.1 `a_generation_task` 加 `progress_pct` 列

```sql
-- V2.0.0_020__add_progress_pct_to_generation_task.sql
ALTER TABLE a_generation_task
    ADD COLUMN progress_pct TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '任务进度百分比 0-100；worker 每阶段结束后写回；user 端轮询可见';
```

**回填**：老任务 `progress_pct=0`，语义为"未开始"。

### 4.2 `t_prompt_template_stage` 加 `model_params` 列

```sql
-- V2.0.0_021__add_model_params_to_prompt_template_stage.sql
ALTER TABLE t_prompt_template_stage
    ADD COLUMN model_params JSON DEFAULT NULL
        COMMENT 'AI 阶段可配参数：temperature / max_tokens / top_p 等；NULL=用全局默认';
```

**示例值**（JSON）：
```json
{"temperature": 0.7, "max_tokens": 2000, "top_p": 0.95}
```

**校验**：每条 AI 阶段保存时校验 `temperature ∈ [0, 2]`、`max_tokens ∈ [1, 8000]`，超出拒绝保存（错误码 308014，新加）。

### 4.3 无需新增的列

- 任务级 retry：`a_generation_task.retry_count` / `max_retry`（V2.0.0_009）+ `a_generation_config.max_retry`（V2.0.0_013）已存在，只是 worker 没 wire。

---

## 5. 核心改动

### 5.1 任务级 retry（诉求 #4）

**当前问题**：`GenerationTaskWorker.processOne()` catch 异常后直接 `markFailed` + 退币，`retry_count` 从未递增。

**改动**：在 catch 分支判断 `retry_count < max_retry`：
- 是：`retry_count++`、重置 `status=queued`、清空 `locked_at/locked_by/lease_until`、清空 `ai_call_history`、`progress_pct=0`、保留 `failed_reason`（让运营看到最近一次原因）
- 否：原逻辑（markFailed + 退币）

**关键约束**：
- 用户端不感知：retry 是后台行为，用户端轮询仍看到 processing（中间步骤进度保留），最终看到 completed 或 failed。
- 不退币：retry 不算"用户失败"，配额不动；只在最终失败时退。
- 退币前提：`retry_count` 到达上限后 `markFailed` 才退（现状已经这样）。

**代码入口**：`GenerationTaskWorker.processOne()`。

**新增 service 方法**（`GenerationTaskService`）：
- `markRetry(Long taskId, String lastError)`：事务内更新 retry_count + 清 claim 字段 + 写 failed_reason

**单测**：
- retry_count 0/1/2 走 retry，3 走 markFailed
- retry 路径不退币
- retry 后 lease_until 清空，下一轮 claim 能抢到

---

### 5.2 AI 参数可配（诉求 #7）

**优先级链**：`stage.model_params > 全局默认（temperature=0.7）`

**改动**：
1. `PromptTemplateStage` 实体加 `String modelParams` 字段
2. `AbstractAiStep.process()` 在调 `aiGateway.call(ctx, sys, user)` 前，从 ctx 拿 modelParams，**作为额外参数传入** AiGateway
3. `AiGateway.call()` 签名扩展：新增 `Map<String, Object> modelParams` 参数（向后兼容：缺省 = null = 用全局默认）
4. `DefaultAiGateway.call()` 把 params 透传给 `GenerationAiService.call()`
5. `GenerationAiService.call()` 签名扩展：把 modelParams merge 进请求 body（覆盖默认 temperature）
6. `GenerationAiService` JSON 请求体改用 `LinkedHashMap`（避免 `Map.of()` 不允许 null 值）

**字段支持**：
- `temperature`（Double）：默认 0.7
- `max_tokens`（Integer）：默认 2000
- `top_p`（Double）：默认 1.0
- 其它未知字段：忽略（仅 log warn，不报错）

**代码入口**：
- `AbstractAiStep.process()` 提取 modelParams
- `AiGateway` 接口签名变更（admin 模块内的接口）

**单测**：
- stage.modelParams=null → 默认值
- stage.modelParams={temperature:0.3} → 请求体 temperature=0.3
- stage.modelParams={temperature:0.3,max_tokens:1500} → 两个都生效
- 未知字段 → 忽略，不报错

---

### 5.3 用户进度百分比（诉求 #6）

**设计原则**：用户只看 0-100 数字，不暴露任何阶段名称 / 内部状态。

**阶段权重表**（合计 100）：

| Stage | 名称 | 权重 | 说明 |
|-------|------|------|------|
| 1 | intent-anchor | 3 | passthrough，纯组装，快 |
| 2 | outline | 8 | AI |
| 3 | material-list | 8 | AI |
| 4 | draft | 22 | 主战场，最重 |
| 5 | rhythm-detect | 3 | 规则 |
| 6 | rhythm-rewrite | 9 | AI |
| 7 | external-review | 8 | AI（毒舌） |
| 8 | targeted-rewrite | 10 | AI |
| 9 | rhythm-polish | 12 | AI（最终打磨） |
| 10 | word-count | 3 | 规则 |
| 11 | word-adjust | 8 | AI（仅超字数时） |
| 12 | export-render | 4 | 规则 |
| 100 | persist-article | 2 | 落库 |

**进度计算**：每 step 完成后 `ctx.progressPct += stage.weight`，最大值 100。

**持久化**：worker 每 step 结束后调用 `GenerationTaskService.updateProgress(taskId, progressPct)` 写库。

**user-api 暴露**：现有进度查询端点（`/api/v1/user/generation-tasks/{id}/progress`）的响应里增加 `progressPct` 字段。

**前端 UI 改动**：
- 用户端"生成中"弹框显示百分比进度条（参考前端的 loading.html 中的样式）
- 不用显示阶段名 / 阶段细节

**单测**：
- 12 阶段跑完最终 progressPct=100
- 单 stage 失败时不更新 progressPct（避免错误状态显示高百分比）
- 任务 retry 时 progressPct 重置为 0

---

### 5.4 导出样式分工（诉求 #5）

详见 §3。本节代码动作：

**`ExportRenderStep.wrapByTemplate()` 扩展**：
- `douyin_default`：保留当前截断 80 字行为 + 加 `#抖音图文` 标签
- `xiaohongshu_default`：保留 emoji wrapper + 加封面图占位 `[封面图]`
- 其它平台：保持现状
- JavaDoc 增补"前端用 templatePresets 做最终渲染，后端只保证 markdown 可读"

**前端不动**。

---

### 5.5 预算调整（Claude 补充 §6.1）

**问题**：当前 `GenerationContext.aiCallBudget = 3`，但 12 阶段有 8 个 AI 阶段，每个最多 3 次重试 → 理论上限 24 次。默认 3 必然提前耗尽。

**改动**：
- `GenerationContext.aiCallBudget` 默认值从 3 → **50**（覆盖 8 阶段 × 4 最坏尝试 = 32 + 余量）
- 这个值不进 `a_generation_config` 表（避免 config 表膨胀），保持代码内常量
- 注释里说明：50 是经验值，未来如果加阶段或调高重试上限需要重评

**为什么不删 budget**：
- budget 是"防失控"的兜底（比如模型异常导致无限循环）
- 单纯依赖阶段内 retry 不够（某个 step 内部逻辑 bug 可能绕过 retry）
- 50 的上限足够 12 阶段正常完成，又能在异常时强制停止

---

## 6. Claude 补充项（诉求 #3 + #8）

### 6.1 预算调整（见 §5.5）

### 6.2 任务 retry 重置 ai_call_history

任务 retry 时清空 `ctx.aiCallHistory`，让新一轮重试是干净的开始（不与上一轮混淆）。

**代码动作**：`GenerationTaskService.markRetry()` 同步清空 call log 表里该 task 的旧记录（避免监控污染）。

### 6.3 AI 调用参数级联

`temperature` / `max_tokens` / `top_p` 的优先级：
1. stage.model_params 中显式值（最高）
2. `GenerationAiService.call()` 内置默认值（temperature=0.7, max_tokens=2000, top_p=1.0）

> 注意：这里的"默认值"指 `GenerationAiService` 请求体的兜底，与 §5.5 的 `aiCallBudget`（AI 调用次数上限）是两个不同概念，不要混淆。

不支持 task 级 model_params（避免过度灵活）。如果未来需要再扩展。

### 6.4 任务进度持久化频率

每阶段结束都写库（最多 12 次写，单任务开销可忽略），让用户轮询看到实时进度。不做节流（节流反而让进度跳变）。

### 6.5 阶段测试覆盖

新增单测覆盖改动点（§5 各小节已列），不全量补 10 个缺失 step 测试（避免本次范围膨胀）。

### 6.6 文档同步

更新 `docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md` 第 4 节失败重试表，新增"任务级 retry"行（按 `a_generation_config.max_retry` 配置）。

---

## 7. API 变更

### 7.1 user-api：进度查询

`GET /api/v1/user/generation-tasks/{id}`（现有端点，service 方法 `getProgress(id, userId)` 已存在）

响应对象 `GenerationTaskVO` 新增字段：

```java
/** 任务进度百分比 0-100；worker 每阶段结束后写回；user 端轮询可见。 */
private Integer progressPct;
```

并在 `GenerationTaskVO.from(...)` 中映射 `t.getProgressPct()`。

**关键**：响应里**不暴露** stage 名 / stage index（`GenerationTaskVO` 现状已无 stage 字段，仅加 progressPct 一个数字）。

### 7.2 admin-api：模板 stage 保存

`PUT /api/v1/admin/prompt-templates/{id}`（现有端点）请求体中 `stages[]` 的每个元素增加可选 `modelParams`：

```json
{
  "name": "默认去 AI 味模板",
  "stages": [
    {
      "stageIndex": 4,
      "aiPrompt": "...",
      "modelParams": {"temperature": 0.7, "max_tokens": 2000}
    }
  ]
}
```

DTO `PromptTemplateStageSaveItem` 加可选字段 `private Map<String, Object> modelParams;`。

后端校验（保存时）：`temperature ∈ [0, 2]`、`max_tokens ∈ [1, 8000]`、`top_p ∈ [0, 1]`。任一字段越界 → 拒绝保存，错误码 308014。

### 7.3 错误码

| 错误码 | 含义 |
|--------|------|
| `308014` | model_params 不合法（admin 端） |

追加到 `AdminGenerationErrorCode`。

---

## 8. 测试计划

### 8.1 单元测试

- `GenerationTaskServiceMarkRetryTest`：retry 路径不退币、retry_count 递增、claim 字段清空
- `AbstractAiStepModelParamsTest`：3 个 case（null / 部分 / 含未知字段）
- `GenerationAiServiceModelParamsTest`：请求体正确 merge
- `GenerationContextProgressTest`：12 阶段权重累加 = 100、failure 时不更新

### 8.2 端到端

`tests/e2e/verify_12_stage_runtime.py`（新建）：
1. 提交任务 → 查 progressPct 随时间增长 → 最终 completed 时 progressPct=100
2. 关掉 model_config（模拟 AI 失败）→ 观察 retry_count 增长 → 最终 failed → 退币
3. 改 stage.modelParams.temperature=0.3 → 新建任务 → 检查 LLM 请求体（用 mock server 验证）
4. 验证 article.body 是 markdown 而不是空的

### 8.3 回归

- 现有 `GenerationPipelineTest` 不动（不破基础流程）
- 现有 `DefaultAiGatewayRetryTest` 不动
- 现有 `PipelineTemplateResolverTest` 不动

---

## 9. 范围外（不做）

- ❌ 不改 12 阶段结构（保持现状）
- ❌ 不引入阶段级 fallback（用户明确不要）
- ❌ 不做监控/告警仪表盘（spec §5.17 是更大范围）
- ❌ 不做"用户主动取消任务"
- ❌ 不改模板管理 UI（阶段 1/2/3 已落地）
- ❌ 不补全 10 个缺失 step 测试（避免膨胀）
- ❌ 不暴露 stage 名给用户（安全要求）

---

## 10. 实施步骤（高层）

后续 writing-plans skill 会拆 task-by-task：

1. Flyway V2.0.0_020：加 `progress_pct`
2. Flyway V2.0.0_021：加 `model_params` 列 + 错误码 308014
3. 实体层：`GenerationTask` 加字段、`PromptTemplateStage` 加字段
4. 任务级 retry：service 方法 + worker 调用
5. AI 参数可配：抽象到 AiGateway → 透传到 GenerationAiService
6. 进度百分比：ctx 字段 + worker 写库 + user-api 暴露
7. ExportRenderStep JavaDoc + 平台 wrapper 扩展
8. 预算默认值调整
9. 单测 + E2E
10. 文档同步更新

每步独立 commit。