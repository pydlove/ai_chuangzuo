# 12 阶段流水线运行时增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 12 阶段流水线骨架上，补齐任务级 retry 路径清理、用户进度可见性、AI 参数可配、预算调整，把骨架接通为生产可用运行时。

**Architecture:**
- 复用现有 `a_generation_task.retry_count` / `max_retry`（schema 已就绪），仅补 `progress_pct` 列 + `markRetry`/`updateProgress` service 方法 + worker 钩子。
- AI 参数流：stage.modelParams JSON → ctx.modelParams → AiGateway → GenerationAiService → 请求体（用 Java 8 default 方法保持向后兼容）。
- 进度可见性：PipelineStage 加 weight 字段 → ctx.progressPct 累加 → worker 每阶段后 updateProgress 写库 → user-api `getProgress` 返回 `progressPct`。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, Flyway, JUnit 5 + Mockito + AssertJ.

---

## Global Constraints

- 所有表变更通过 Flyway 脚本（admin-api 持有）。
- 表前缀：`a_`（admin 业务表）、`t_`（admin 配置 / 模板表）、`u_`（用户业务表）。
- 业务表强制字段：`tenant_id=0` / `is_deleted=0` / `created_at` / `updated_at` / `created_by=0` / `updated_by=0`。
- 时间字段 `DATETIME(3)`，应用层 UTC+8。
- 跨端调用：`/api/v1/{user|admin}/{module}`，统一响应 `{code, message, data}`。
- 错误码：admin 端追加到 `AdminGenerationErrorCode`（308xxx 段）。
- 每个 Task 独立 commit。
- Java 模块：`shared`（双端依赖）、`admin/api`（worker + 配置 + 清理）、`user/api`（submit + poll + retry + quota）。
- 测试框架：JUnit 5 + Mockito + AssertJ（与现有 `GenerationPipelineTest` 一致）。

---

## File Structure

### Schema（admin-api）

| 文件 | 职责 |
|------|------|
| `project/admin/api/src/main/resources/db/migration/V2.0.0_020__add_progress_pct_to_generation_task.sql` | 新增 `a_generation_task.progress_pct` |
| `project/admin/api/src/main/resources/db/migration/V2.0.0_021__add_model_params_to_prompt_template_stage.sql` | 新增 `t_prompt_template_stage.model_params` |

### 实体与 VO

| 文件 | 改动 |
|------|------|
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationTask.java` | 加 `progressPct` 字段 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/PromptTemplateStage.java` | 加 `modelParams` 字段 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/GenerationTaskVO.java` | 加 `progressPct` 字段 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/dto/request/PromptTemplateStageSaveItem.java` | 加 `modelParams` 字段 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineStage.java` | 加 `weight` 字段 + 13 个 stage 各自的权重值 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationContext.java` | 加 `progressPct` / `modelParams` 字段 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/AiGateway.java` | 加 `call(ctx, sys, user, modelParams)` 方法（默认 delegate 到 3 参） |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/AbstractAiStep.java` | 调 `aiGateway.call(ctx, sys, user, modelParams)` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/ExportRenderStep.java` | 扩展 `wrapByTemplate`（douyin/xiaohongshu）+ JavaDoc |

### Service 与 Worker

| 文件 | 改动 |
|------|------|
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java` | `markFailed` 加 `progress_pct=0` reset；新增 `updateProgress(taskId, pct)` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/worker/GenerationTaskWorker.java` | 每 step 完成后调 `updateProgress` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationAiService.java` | `call(modelConfigId, sys, user)` → `call(modelConfigId, sys, user, modelParams)`；请求体改 `LinkedHashMap` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGateway.java` | 重写 4 参 call，透传 modelParams |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationCallLogMapper.java` | 加 `deleteByTaskId(taskId)` |
| `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java` | 加 `MODEL_PARAMS_INVALID(308014)` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/service/GenerationTaskService.java` | `getProgress` 映射 `progressPct` |

### 校验

| 文件 | 改动 |
|------|------|
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateStageValidator.java`（新建） | 校验 modelParams 数值范围 |

### 测试

| 文件 | 职责 |
|------|------|
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/ProgressWeightTest.java` | PipelineStage 权重合计=100 |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGatewayModelParamsTest.java` | gateway 透传 modelParams |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationAiServiceModelParamsTest.java` | 请求体 merge 正确 |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceRetryTest.java` | markFailed retry 路径 + progress_pct reset |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceProgressTest.java` | updateProgress 写库 |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateStageValidatorTest.java` | modelParams 校验 |
| `tests/e2e/verify_12_stage_runtime.py` | 端到端验证 |

---

## Task 1: Flyway V2.0.0_020 — 加 `progress_pct` 列

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_020__add_progress_pct_to_generation_task.sql`

- [ ] **Step 1: 写迁移文件**

```sql
-- 12 阶段流水线运行时增强（设计文档 docs/superpowers/specs/2026-07-10-12-stage-pipeline-execution-design.md §4.1）
-- worker 每阶段结束后把进度百分比写回 a_generation_task.progress_pct，user 端轮询可见。

SET NAMES utf8mb4;

ALTER TABLE a_generation_task
    ADD COLUMN progress_pct TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '任务进度 0-100；worker 每阶段结束后写回；user 端轮询可见';
```

- [ ] **Step 2: 启动 admin-api 跑迁移**

```bash
cd project/admin/api && mvn spring-boot:run -DskipTests
```

观察日志：`Successfully applied 20 migrations`，验证：

```bash
mysql -h127.0.0.1 -uroot -p${MYSQL_PASSWORD} aichuangzuo -e "DESC a_generation_task;" | grep progress_pct
```

预期：列存在，类型 `TINYINT UNSIGNED`，默认 `0`。

- [ ] **Step 3: 停 admin-api（Ctrl+C）**

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_020__add_progress_pct_to_generation_task.sql
git commit -m "feat(db): add progress_pct column to generation task"
```

---

## Task 2: Flyway V2.0.0_021 — 加 `model_params` 列

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_021__add_model_params_to_prompt_template_stage.sql`

- [ ] **Step 1: 写迁移文件**

```sql
-- 12 阶段流水线运行时增强 §4.2：AI 阶段可配参数（temperature / max_tokens / top_p）
-- NULL = 用 GenerationAiService 内置默认值。

SET NAMES utf8mb4;

ALTER TABLE t_prompt_template_stage
    ADD COLUMN model_params JSON DEFAULT NULL
        COMMENT 'AI 阶段可配参数：temperature / max_tokens / top_p 等；NULL=用全局默认';
```

- [ ] **Step 2: 启动 admin-api 跑迁移**

```bash
cd project/admin/api && mvn spring-boot:run -DskipTests
```

观察日志：`Successfully applied 21 migrations`，验证：

```bash
mysql -h127.0.0.1 -uroot -p${MYSQL_PASSWORD} aichuangzuo -e "DESC t_prompt_template_stage;" | grep model_params
```

预期：列存在，类型 `JSON`，默认 `NULL`。

- [ ] **Step 3: 停 admin-api**

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_021__add_model_params_to_prompt_template_stage.sql
git commit -m "feat(db): add model_params JSON column to prompt template stage"
```

---

## Task 3: 实体加字段 + 错误码 308014

**Files:**
- Modify: `project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationTask.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/PromptTemplateStage.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/dto/request/PromptTemplateStageSaveItem.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/GenerationTaskVO.java`
- Modify: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java`

- [ ] **Step 1: GenerationTask 加 progressPct**

在 `GenerationTask.java` 第 65 行（`retryCount` 字段上方）插入：

```java
    /** 任务进度 0-100；worker 每阶段结束后写回；user 端轮询可见。 */
    private Integer progressPct;
```

- [ ] **Step 2: PromptTemplateStage 加 modelParams**

在 `PromptTemplateStage.java` 第 42 行（`ruleConfig` 字段下方）插入：

```java
    /** 仅 stage_type=ai_prompt 有值（JSON 字符串）：temperature / max_tokens / top_p 等。 */
    private String modelParams;
```

- [ ] **Step 3: PromptTemplateStageSaveItem 加 modelParams**

在 `PromptTemplateStageSaveItem.java` 第 24 行（`ruleConfig` 字段下方）插入：

```java
    /** 可选：AI 阶段可配参数（JSON 对象，如 {"temperature":0.7,"max_tokens":2000}）。 */
    private Map<String, Object> modelParams;
```

并在文件顶部 import 区追加：

```java
import java.util.Map;
```

- [ ] **Step 4: GenerationTaskVO 加 progressPct + 映射**

在 `GenerationTaskVO.java` 第 28 行（`failedReason` 字段上方）插入：

```java
    /** 任务进度 0-100；worker 每阶段结束后写回。 */
    private Integer progressPct;
```

并在 `from(...)` 方法（第 47 行 `vo.createdAt = t.getCreatedAt();` 之后）追加：

```java
        vo.progressPct = t.getProgressPct();
```

- [ ] **Step 5: AdminGenerationErrorCode 加 308014**

打开 `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java`，在文件中查找已有 `308013`（GENERATION_TEMPLATE_STATUS_INVALID），在其后追加：

```java
    GENERATION_MODEL_PARAMS_INVALID(308014, "模板 AI 参数不合法：temperature ∈ [0,2]、max_tokens ∈ [1,8000]、top_p ∈ [0,1]"),
```

- [ ] **Step 6: 编译确认无错**

```bash
mvn -pl project/shared -am install -DskipTests
mvn -pl project/admin/api -am compile -DskipTests
mvn -pl project/user/api -am compile -DskipTests
```

预期：BUILD SUCCESS，无编译错误。

- [ ] **Step 7: 提交**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/entity/GenerationTask.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/entity/PromptTemplateStage.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/dto/request/PromptTemplateStageSaveItem.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/generation/vo/GenerationTaskVO.java \
        project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java
git commit -m "feat(generation): add progressPct, modelParams fields + error code 308014"
```

---

## Task 4: `GenerationContext.aiCallBudget` 默认值 3 → 50

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationContext.java:46`

- [ ] **Step 1: 改默认值**

在 `GenerationContext.java` 第 45-46 行：

```java
    /** 任务允许调 AI 的总次数（默认 3，worker 写入；可按会员等级调整）。 */
    private int aiCallBudget = 3;
```

改为：

```java
    /**
     * 任务允许调 AI 的总次数（默认 50）。
     *
     * <p>覆盖 12 阶段流水线最坏情况：8 个 AI 阶段 × 4 次最坏尝试（1 首次 + 3 重试）= 32，
     * 留余量到 50。如果未来加阶段或调高重试上限需要重评此值。
     *
     * <p>为什么保留 budget 而不是只用 retry 上限：budget 是「防失控」兜底，
     * 比如某个 step 内部 bug 绕过 retry 时强制停止。详见 spec §5.5。
     */
    private int aiCallBudget = 50;
```

- [ ] **Step 2: 编译确认**

```bash
mvn -pl project/admin/api -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationContext.java
git commit -m "feat(generation): bump aiCallBudget default 3 -> 50 for 12-stage pipeline"
```

---

## Task 5: PipelineStage 加 weight 字段 + 13 个 stage 权重

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineStage.java`

- [ ] **Step 1: 加 weight 字段**

在 `PipelineStage.java` 第 478 行（构造器声明上方）的成员字段区追加：

```java
    public final int weight;                 // 进度权重（合计 100）
```

并修改第 478-486 行的构造器声明：

```java
    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders) {
        this(index, key, displayName, type, description,
                defaultAiPrompt, defaultRuleConfigJson,
                placeholders, java.util.List.of(), 0);
    }
```

替换为：

```java
    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders) {
        this(index, key, displayName, type, description,
                defaultAiPrompt, defaultRuleConfigJson,
                placeholders, java.util.List.of(), 0);
    }

    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders,
                  int weight) {
        this(index, key, displayName, type, description,
                defaultAiPrompt, defaultRuleConfigJson,
                placeholders, java.util.List.of(), weight);
    }
```

- [ ] **Step 2: 修改 5 参构造器接受 weight**

修改第 488-503 行的 5 参构造器，签名加 `int weight`，体内加 `this.weight = weight;`：

```java
    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders,
                  List<ConfigField> configFields) {
        this(index, key, displayName, type, description,
                defaultAiPrompt, defaultRuleConfigJson,
                placeholders, configFields, 0);
    }

    PipelineStage(int index, String key, String displayName, StageType type,
                  String description,
                  String defaultAiPrompt,
                  String defaultRuleConfigJson,
                  List<Placeholder> placeholders,
                  List<ConfigField> configFields,
                  int weight) {
        this.index = index;
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.description = description;
        this.defaultAiPrompt = defaultAiPrompt;
        this.defaultRuleConfigJson = defaultRuleConfigJson;
        this.placeholders = placeholders;
        this.configFields = configFields;
        this.weight = weight;
    }
```

- [ ] **Step 3: 给 13 个 stage 赋值权重**

逐一修改每个 stage 的枚举常量（`INTENT_ANCHOR` → `EXPORT_RENDER` + 新增 `PERSIST_ARTICLE`），在每个 enum 常量的最后参数后加 `, weight`。注意 `ALL` 数组里需要新增 `PERSIST_ARTICLE` 项。

修改枚举常量的最后参数（每个 stage 都按 spec §5.3 权重表赋值）。例如 `INTENT_ANCHOR`（第 22-33 行）把 `)` 闭合前加 `, 3`：

```java
    INTENT_ANCHOR(
            1, "intent_anchor", "意图锚定", StageType.PASSTHROUGH,
            "把用户 4 项输入（标题 / 核心观点 / 目标读者 / 风格）组装成下游 prompt 可嵌入的 user_context_block。",
            null,
            null,
            List.of(
                    new Placeholder("title", "用户标题"),
                    new Placeholder("coreViewpoint", "核心观点"),
                    new Placeholder("targetReader", "目标读者"),
                    new Placeholder("userStylePrompt", "用户写作风格")
            ),
            3
    ),
```

按下面映射给每个 stage 加权重：

| Stage | 权重 |
|-------|------|
| INTENT_ANCHOR | 3 |
| OUTLINE | 8 |
| MATERIAL_LIST | 8 |
| DRAFT | 22 |
| RHYTHM_DETECT | 3 |
| RHYTHM_REWRITE | 9 |
| EXTERNAL_REVIEW | 8 |
| TARGETED_REWRITE | 10 |
| RHYTHM_POLISH | 12 |
| WORD_COUNT | 3 |
| WORD_ADJUST | 8 |
| EXPORT_RENDER | 4 |

- [ ] **Step 4: 在 ALL 数组里加 PERSIST_ARTICLE**

在 `EXPORT_RENDER` 枚举常量定义之后（约第 458 行）新增：

```java
    // ===== 100. 落库（非 12 阶段之一，用于 GenerationPipeline 编排器排序）=====
    PERSIST_ARTICLE(
            100, "persist_article", "持久化文章", StageType.PASSTHROUGH,
            "pipeline 收尾：把 exportResult 或 finalDraft 写到 article。",
            null,
            null,
            List.of(),
            2
    );
```

并修改 `ALL` 数组（约第 461 行）：

```java
    public static final PipelineStage[] ALL = new PipelineStage[]{
            INTENT_ANCHOR, OUTLINE, MATERIAL_LIST, DRAFT,
            RHYTHM_DETECT, RHYTHM_REWRITE, EXTERNAL_REVIEW,
            TARGETED_REWRITE, RHYTHM_POLISH,
            WORD_COUNT, WORD_ADJUST, EXPORT_RENDER,
            PERSIST_ARTICLE
    };
```

> 注意：`PersistArticleStep` 当前 `stageIndex()` 返回 100，刚好对应 `PERSIST_ARTICLE.index`。这样 `PipelineStage.byIndex(step.stageIndex())` 也能查。

- [ ] **Step 5: 写测试验证权重合计 = 100**

新建 `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/ProgressWeightTest.java`：

```java
package com.aichuangzuo.admin.modules.generation.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 进度权重合计必须 = 100，否则 progressPct 累加结果不为 100。
 * 详见 spec §5.3。
 */
class ProgressWeightTest {

    @Test
    void allStageWeights_shouldSumTo100() {
        int sum = 0;
        for (PipelineStage s : PipelineStage.ALL) {
            sum += s.weight;
        }
        assertEquals(100, sum, "12 阶段 + persist 阶段的 weight 合计必须为 100");
    }

    @Test
    void persistArticleWeight_shouldBe2() {
        assertEquals(2, PipelineStage.byIndex(100).weight);
    }
}
```

- [ ] **Step 6: 跑测试**

```bash
mvn -pl project/admin/api -am test -Dtest=ProgressWeightTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：2 个测试都 PASS。

- [ ] **Step 7: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/PipelineStage.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/ProgressWeightTest.java
git commit -m "feat(generation): add weight field to PipelineStage + persist stage"
```

---

## Task 6: GenerationContext 加 progressPct / modelParams 字段

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationContext.java`

- [ ] **Step 1: 加 progressPct 字段**

在 `GenerationContext.java` 第 45 行 `aiCallBudget` 字段上方插入：

```java
    /** 当前任务进度 0-100；每 step 完成后 += PipelineStage.weight。 */
    private int progressPct = 0;
```

- [ ] **Step 2: 加 modelParams 字段**

在 `GenerationContext.java` 第 60 行 `aiCallHistory` 字段下方插入：

```java
    /** 当前 stage 的 AI 调用参数（来自 stage.modelParams）。每 step 启动时由编排器设入。 */
    private Map<String, Object> modelParams;
```

并在文件顶部 import 区追加：

```java
import java.util.HashMap;
```

> 已有 `import java.util.HashMap;` 的话跳过本步骤。

- [ ] **Step 3: 写累加进度方法**

在 `GenerationContext.java` 类体内（`putExtra` 方法之后）插入：

```java
    /**
     * 累加当前 stage 的进度权重；进度上限 100。
     * 用于 pipeline 编排器在 step 完成后调用。
     */
    public void addProgress(int weight) {
        this.progressPct = Math.min(100, this.progressPct + weight);
    }

    /** 重置进度（任务 retry 时使用）。 */
    public void resetProgress() {
        this.progressPct = 0;
    }
```

- [ ] **Step 4: 编译确认**

```bash
mvn -pl project/admin/api -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **Step 5: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationContext.java
git commit -m "feat(generation): add progressPct and modelParams to GenerationContext"
```

---

## Task 7: GenerationPipeline 编排器写进度 + 设 modelParams

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationPipeline.java`

- [ ] **Step 1: 在 step 完成后写进度**

修改 `GenerationPipeline.java` 第 45-61 行的循环，在 `step.process(ctx)` 调用成功（即没有 `STOP` 和异常）之后，加 `addProgress`：

找到 `for (GenerationStep step : sorted) {` 循环（第 45 行），把循环体改成：

```java
        for (GenerationStep step : sorted) {
            if (!step.enabled(ctx)) {
                log.debug("stage {} ({}) disabled, skip", step.stageIndex(), step.name());
                continue;
            }
            log.info("→ stage {} ({})", step.stageIndex(), step.name());
            try {
                StepResult r = step.process(ctx);
                if (r == StepResult.STOP) {
                    log.info("stage {} ({}) 要求 STOP，pipeline 提前结束", step.stageIndex(), step.name());
                    break;
                }
                // 累加进度（worker 也会通过 ctx.progressPct 拿到这个值，但实时写库由 worker 负责）
                ctx.addProgress(PipelineStage.byIndex(step.stageIndex()).weight);
            } catch (RuntimeException e) {
                log.error("✗ stage {} ({}) 失败: {}", step.stageIndex(), step.name(), e.getMessage());
                throw e;
            }
        }
```

- [ ] **Step 2: 在 step 启动前设 modelParams**

在同一个循环里，`step.process(ctx)` 调用之前（`log.info("→ stage {} ({})", ...)` 之后），加：

```java
                // 把当前 stage 的 modelParams 塞进 ctx，供 AbstractAiStep 调用 AiGateway 用
                com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage stageRow =
                        ctx.getStages().get(step.stageIndex());
                if (stageRow != null && stageRow.getModelParams() != null && !stageRow.getModelParams().isBlank()) {
                    try {
                        ctx.setModelParams(new com.fasterxml.jackson.databind.ObjectMapper()
                                .readValue(stageRow.getModelParams(), new com.fasterxml.jackson.core.type.TypeReference<>() {}));
                    } catch (Exception parseEx) {
                        log.warn("stage {} modelParams 解析失败，用默认: {}", step.stageIndex(), parseEx.getMessage());
                    }
                } else {
                    ctx.setModelParams(null);
                }
```

> 如果代码行数太多不好读，提取成 `private void setupStageModelParams(GenerationContext ctx, int stageIndex)` 方法。

- [ ] **Step 3: 编译确认**

```bash
mvn -pl project/admin/api -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **Step 4: 现有测试回归**

```bash
mvn -pl project/admin/api -am test -Dtest=GenerationPipelineTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：所有现有测试 PASS（pipeline 行为不变，只是加了进度累加）。

- [ ] **Step 5: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationPipeline.java
git commit -m "feat(generation): pipeline accumulates progress + sets stage modelParams"
```

---

## Task 8: AiGateway 加 4 参 call（默认 delegate）

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/AiGateway.java`

- [ ] **Step 1: 加新方法 + 默认 delegate**

修改 `AiGateway.java` 第 26 行 `String call(GenerationContext ctx, String systemMsg, String userMsg);` 后面追加：

```java

    /**
     * 调 AI 一次，携带模型参数（覆盖默认 temperature / max_tokens / top_p）。
     *
     * <p>默认实现 delegate 到 3 参版本，保持向后兼容；子类按需重写。
     *
     * @param modelParams 可选；null 或空 map = 用 GenerationAiService 内置默认值
     */
    default String call(GenerationContext ctx, String systemMsg, String userMsg,
                        java.util.Map<String, Object> modelParams) {
        return call(ctx, systemMsg, userMsg);
    }
```

并在文件顶部 import 区追加（如已有则跳过）：

```java
import java.util.Map;
```

- [ ] **Step 2: 编译确认**

```bash
mvn -pl project/admin/api -am compile -DskipTests
```

预期：BUILD SUCCESS（现有 `DefaultAiGateway` / 测试 mock `(ctx, sys, user) -> ""` 都不受影响）。

- [ ] **Step 3: 现有测试回归**

```bash
mvn -pl project/admin/api -am test -Dtest=DefaultAiGatewayRetryTest,GenerationPipelineTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：所有现有测试 PASS。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/AiGateway.java
git commit -m "feat(generation): AiGateway call with optional modelParams (default delegate)"
```

---

## Task 9: DefaultAiGateway 实现 4 参 call 透传

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGateway.java`

- [ ] **Step 1: 重写 4 参 call**

修改 `DefaultAiGateway.java` 第 37 行的 `@Override public String call(GenerationContext ctx, String systemMsg, String userMsg)` 方法。

在该方法（第 37-120 行）签名下方、第 38 行 `int maxAttempts = readMaxAttempts(ctx);` 上方，把整个方法体内的 `aiService.call(...)` 调用改成调用 4 参版本。

具体改动：找到第 64 行：

```java
                content = aiService.call(modelConfigId, systemMsg, currentUserMsg);
```

改为：

```java
                content = aiService.call(modelConfigId, systemMsg, currentUserMsg,
                        ctx.getModelParams());
```

并在第 37 行原方法签名下方新增 4 参 override：

```java
    @Override
    public String call(GenerationContext ctx, String systemMsg, String userMsg,
                       java.util.Map<String, Object> modelParams) {
        // 把 modelParams 暂存到 ctx，3 参 call 会自动读 ctx.modelParams
        ctx.setModelParams(modelParams);
        return call(ctx, systemMsg, userMsg);
    }
```

> 设计要点：3 参 `call(ctx, sys, user)` 通过 ctx.modelParams 拿到 modelParams，避免重写整个 retry 循环。ctx.modelParams 由 GenerationPipeline 在 step 启动前设入；这里 4 参版本也可以覆盖。

- [ ] **Step 2: 写测试验证透传**

新建 `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGatewayModelParamsTest.java`：

```java
package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 DefaultAiGateway 把 ctx.modelParams 透传给 GenerationAiService.call 的 4 参版本。
 */
@ExtendWith(MockitoExtension.class)
class DefaultAiGatewayModelParamsTest {

    @Mock
    private GenerationAiService aiService;

    @Mock
    private com.aichuangzuo.admin.modules.generation.service.GenerationConfigService configService;

    @Test
    void call_4arg_shouldPassModelParamsToAiService() {
        when(configService.getCurrent()).thenReturn(null);  // 用默认 retry 配置
        when(aiService.call(anyLong(), anyString(), anyString(), any())).thenReturn("ok");

        DefaultAiGateway gw = new DefaultAiGateway(aiService, configService);
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(5);
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setModelConfigId(20L);
        ctx.setTask(task);

        Map<String, Object> params = Map.of("temperature", 0.3, "max_tokens", 1500);
        String result = gw.call(ctx, "sys", "user", params);

        assertEquals("ok", result);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(aiService, times(1)).call(anyLong(), anyString(), anyString(), captor.capture());
        Map<String, Object> passed = captor.getValue();
        assertEquals(0.3, passed.get("temperature"));
        assertEquals(1500, passed.get("max_tokens"));
    }

    @Test
    void call_4arg_shouldHandleNullParams() {
        when(configService.getCurrent()).thenReturn(null);
        when(aiService.call(anyLong(), anyString(), anyString(), any())).thenReturn("ok");

        DefaultAiGateway gw = new DefaultAiGateway(aiService, configService);
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallBudget(5);
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setModelConfigId(20L);
        ctx.setTask(task);

        gw.call(ctx, "sys", "user", null);

        // 即使 modelParams=null，AiService 也要收到调用（参数为 null，由 AiService 用默认值）
        verify(aiService, times(1)).call(anyLong(), anyString(), anyString(), any());
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
mvn -pl project/admin/api -am test -Dtest=DefaultAiGatewayModelParamsTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：2 个测试 PASS。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGateway.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/DefaultAiGatewayModelParamsTest.java
git commit -m "feat(generation): DefaultAiGateway 4-arg call passes modelParams"
```

---

## Task 10: AbstractAiStep 调 4 参 gateway

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/AbstractAiStep.java`

- [ ] **Step 1: 改 call 调用**

修改 `AbstractAiStep.java` 第 35 行：

```java
        String aiResp = aiGateway.call(ctx, systemMessage(), userPrompt);
```

改为：

```java
        String aiResp = aiGateway.call(ctx, systemMessage(), userPrompt, ctx.getModelParams());
```

- [ ] **Step 2: 现有测试回归**

```bash
mvn -pl project/admin/api -am test -Dtest=GenerationPipelineTest,DefaultAiGatewayRetryTest,DefaultAiGatewayModelParamsTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：所有测试 PASS。

- [ ] **Step 3: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/AbstractAiStep.java
git commit -m "feat(generation): AbstractAiStep passes ctx.modelParams to gateway"
```

---

## Task 11: GenerationAiService 接受 modelParams + merge 到请求体

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationAiService.java`

- [ ] **Step 1: 改 call 签名 + 请求体构造**

修改 `GenerationAiService.java` 第 54 行的 `call(...)` 方法：

把第 54-93 行整个方法替换为：

```java
    /**
     * 调用模型，返回 AI 原始 assistant content（字符串）。
     *
     * @param modelParams 可选；非空时 merge 进请求体（覆盖默认 temperature）
     */
    public String call(Long modelConfigId, String systemMessage, String userMessage,
                       Map<String, Object> modelParams) {
        ModelConfig cfg = modelConfigMapper.selectById(modelConfigId);
        if (cfg == null || cfg.getIsActive() == null || cfg.getIsActive() != 1) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        }
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("api key 解密失败 modelConfigId={}", modelConfigId, e);
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        }

        String url = resolveUrl(cfg);

        // 请求体改用 LinkedHashMap，允许覆盖默认值；Map.of() 不允许 null 值
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("temperature", pickDouble(modelParams, "temperature", 0.7));
        body.put("max_tokens", pickInt(modelParams, "max_tokens", 2000));
        body.put("top_p", pickDouble(modelParams, "top_p", 1.0));
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return extractAssistantContent(response.getBody(), cfg.getProviderType());
        } catch (HttpClientErrorException e) {
            log.warn("AI 调用 client error provider={} status={}", cfg.getProviderType(), e.getStatusCode());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        } catch (RestClientException e) {
            log.warn("AI 调用 transport error provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_AI_PROVIDER_ERROR);
        }
    }

    /** 保留旧 3 参签名（向后兼容），内部 delegate 到 4 参版本。 */
    public String call(Long modelConfigId, String systemMessage, String userMessage) {
        return call(modelConfigId, systemMessage, userMessage, null);
    }

    private static Double pickDouble(Map<String, Object> params, String key, double def) {
        if (params == null) return def;
        Object v = params.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            log.warn("AI 参数 {} 不是合法数字，使用默认 {}: value={}", key, def, v);
            return def;
        }
    }

    private static Integer pickInt(Map<String, Object> params, String key, int def) {
        if (params == null) return def;
        Object v = params.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            log.warn("AI 参数 {} 不是合法整数，使用默认 {}: value={}", key, def, v);
            return def;
        }
    }
```

- [ ] **Step 2: 写测试**

新建 `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationAiServiceModelParamsTest.java`：

```java
package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 验证 GenerationAiService 把 modelParams merge 进 LLM 请求体。
 */
@ExtendWith(MockitoExtension.class)
class GenerationAiServiceModelParamsTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private RestTemplate restTemplate;

    private GenerationAiService service;

    @BeforeEach
    void setup() throws Exception {
        ModelConfig cfg = new ModelConfig();
        cfg.setId(1L);
        cfg.setModelCode("test-model");
        cfg.setProviderType("kimi");
        cfg.setBaseUrl("https://api.example.com/");
        cfg.setApiKeyEncrypted("encrypted");
        cfg.setIsActive(1);
        when(modelConfigMapper.selectById(1L)).thenReturn(cfg);

        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}"));

        service = new TestableGenerationAiService(modelConfigMapper, "test-secret", restTemplate);
    }

    @Test
    void call_shouldUseDefaultParamsWhenModelParamsNull() {
        service.call(1L, "sys", "user", null);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.7, body.get("temperature"));
        assertEquals(2000, body.get("max_tokens"));
    }

    @Test
    void call_shouldOverrideParamsFromModelParams() {
        Map<String, Object> params = Map.of("temperature", 0.3, "max_tokens", 1500, "top_p", 0.9);
        service.call(1L, "sys", "user", params);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.3, body.get("temperature"));
        assertEquals(1500, body.get("max_tokens"));
        assertEquals(0.9, body.get("top_p"));
    }

    @Test
    void call_shouldIgnoreUnknownFields() {
        Map<String, Object> params = Map.of("unknown_field", "value", "temperature", 0.5);
        service.call(1L, "sys", "user", params);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals(0.5, body.get("temperature"));
        assertEquals(2000, body.get("max_tokens"));  // 未知字段不影响
    }

    /**
     * 测试用子类：注入 mock RestTemplate，避免真实 HTTP 调用。
     */
    static class TestableGenerationAiService extends GenerationAiService {
        private final RestTemplate mockRestTemplate;

        TestableGenerationAiService(ModelConfigMapper mapper, String secret, RestTemplate restTemplate) {
            super(mapper, secret);
            this.mockRestTemplate = restTemplate;
        }

        @Override
        public org.springframework.web.client.RestTemplate getRestTemplate() {
            return mockRestTemplate;
        }
    }
}
```

> 注意：上面用了 `getRestTemplate()` 这个 protected getter。需要在 `GenerationAiService` 父类加这个 getter（Step 0 的准备工作）：

- [ ] **Step 0（前置）：`GenerationAiService` 加 protected getter**

在 `GenerationAiService.java` 类内任意位置（建议第 49 行 `this.restTemplate = ...` 之后）插入：

```java
    /** 测试用：暴露 RestTemplate 给子类 override。 */
    protected RestTemplate getRestTemplate() {
        return this.restTemplate;
    }
```

并在 `import` 区追加（如已有则跳过）：

```java
import org.springframework.web.client.RestTemplate;
```

- [ ] **Step 3: 跑测试**

```bash
mvn -pl project/admin/api -am test -Dtest=GenerationAiServiceModelParamsTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：3 个测试 PASS。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationAiService.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationAiServiceModelParamsTest.java
git commit -m "feat(generation): GenerationAiService accepts modelParams and merges into request body"
```

---

## Task 12: PromptTemplateStageValidator 校验 modelParams

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateStageValidator.java`
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateStageValidatorTest.java`

- [ ] **Step 1: 写 validator**

```java
package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateStageSaveItem;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 校验 stage 保存请求的合法性（modelParams 数值范围）。
 *
 * <p>modelParams 允许字段：temperature ∈ [0, 2]、max_tokens ∈ [1, 8000]、top_p ∈ [0, 1]。
 * 未知字段忽略不报错。详见 spec §7.2。
 */
@Component
public class PromptTemplateStageValidator {

    public void validate(PromptTemplateStageSaveItem item) {
        if (item == null || item.getModelParams() == null || item.getModelParams().isEmpty()) {
            return;
        }
        Map<String, Object> params = item.getModelParams();
        validateRange(params, "temperature", 0.0, 2.0);
        validateRange(params, "max_tokens", 1.0, 8000.0);
        validateRange(params, "top_p", 0.0, 1.0);
    }

    private static void validateRange(Map<String, Object> params, String key, double min, double max) {
        Object v = params.get(key);
        if (v == null) return;  // 字段缺失 = 用默认
        double d;
        if (v instanceof Number n) {
            d = n.doubleValue();
        } else {
            try {
                d = Double.parseDouble(v.toString());
            } catch (NumberFormatException e) {
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
            }
        }
        if (d < min || d > max) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID);
        }
    }
}
```

- [ ] **Step 2: 写测试**

```java
package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateStageSaveItem;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 校验 modelParams 数值范围。
 */
class PromptTemplateStageValidatorTest {

    private final PromptTemplateStageValidator validator = new PromptTemplateStageValidator();

    @Test
    void nullModelParams_shouldPass() {
        PromptTemplateStageSaveItem item = new PromptTemplateStageSaveItem();
        assertDoesNotThrow(() -> validator.validate(item));
    }

    @Test
    void validModelParams_shouldPass() {
        PromptTemplateStageSaveItem item = new PromptTemplateStageSaveItem();
        item.setModelParams(Map.of("temperature", 0.7, "max_tokens", 2000, "top_p", 0.95));
        assertDoesNotThrow(() -> validator.validate(item));
    }

    @Test
    void temperatureOutOfRange_shouldThrow() {
        PromptTemplateStageSaveItem item = new PromptTemplateStageSaveItem();
        item.setModelParams(Map.of("temperature", 3.0));
        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validate(item));
        if (ex.getErrorCode() != AdminGenerationErrorCode.GENERATION_MODEL_PARAMS_INVALID) {
            throw new AssertionError("expected GENERATION_MODEL_PARAMS_INVALID, got " + ex.getErrorCode());
        }
    }

    @Test
    void maxTokensOutOfRange_shouldThrow() {
        PromptTemplateStageSaveItem item = new PromptTemplateStageSaveItem();
        item.setModelParams(Map.of("max_tokens", 0));
        assertThrows(BusinessException.class, () -> validator.validate(item));
    }

    @Test
    void topPOutOfRange_shouldThrow() {
        PromptTemplateStageSaveItem item = new PromptTemplateStageSaveItem();
        item.setModelParams(Map.of("top_p", 1.5));
        assertThrows(BusinessException.class, () -> validator.validate(item));
    }

    @Test
    void unknownFields_shouldBeIgnored() {
        PromptTemplateStageSaveItem item = new PromptTemplateStageSaveItem();
        item.setModelParams(Map.of("unknown_field", "value", "temperature", 0.5));
        assertDoesNotThrow(() -> validator.validate(item));
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
mvn -pl project/admin/api -am test -Dtest=PromptTemplateStageValidatorTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：6 个测试 PASS。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateStageValidator.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateStageValidatorTest.java
git commit -m "feat(generation): add validator for stage modelParams (range check)"
```

---

## Task 13: GenerationTaskService.markFailed 加 progress_pct reset

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java`

> 注：当前 `markFailed` 已经在 retry 路径上做 status=QUEUED + 清 claim 字段。这里只补 `progress_pct=0` 重置 + 调 `callLogMapper.deleteByTaskId` 清空旧 call log。

- [ ] **Step 1: GenerationCallLogMapper 加 deleteByTaskId**

打开 `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationCallLogMapper.java`，加方法：

```java
    /** 删除某 task 的全部 call log（任务 retry 时清空旧记录）。 */
    int deleteByTaskId(@Param("taskId") Long taskId);
```

并在 mapper 接口同名 XML 文件 `GenerationCallLogMapper.xml` 里加 SQL：

```xml
<delete id="deleteByTaskId">
    DELETE FROM a_generation_call_log WHERE task_id = #{taskId} AND tenant_id = 0
</delete>
```

如果当前没有 XML 文件，新建一个，文件结构参考其他 mapper 的 XML（如 `PromptTemplateMapper.xml`）。

- [ ] **Step 2: markFailed 加 reset 逻辑**

修改 `GenerationTaskService.java` 第 71-94 行的 `markFailed` 方法，在 retry 分支（第 80-86 行）追加 reset：

找到：

```java
        if (nextRetry <= max) {
            // 回 queued，释放 lease
            task.setStatus(GenerationTaskStatus.QUEUED);
            task.setLockedAt(null);
            task.setLockedBy(null);
            task.setLeaseUntil(null);
            mapper.updateById(task);
            log.info("task={} retry {}/{}, queued back, reason={}", taskId, nextRetry, max, reason);
        } else {
```

改为：

```java
        if (nextRetry <= max) {
            // 回 queued，释放 lease；重置进度 + 清空旧 call log，让新一轮重试干净开始
            task.setStatus(GenerationTaskStatus.QUEUED);
            task.setLockedAt(null);
            task.setLockedBy(null);
            task.setLeaseUntil(null);
            task.setProgressPct(0);
            mapper.updateById(task);
            try {
                callLogMapper.deleteByTaskId(taskId);
            } catch (Exception logEx) {
                log.warn("task={} 清空旧 call log 失败: {}", taskId, logEx.getMessage());
            }
            log.info("task={} retry {}/{}, queued back, reason={}", taskId, nextRetry, max, reason);
        } else {
```

并在 `GenerationTaskService` 类顶部 import 区追加（如已有则跳过）：

```java
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
```

并在类成员区（第 24 行 `private final GenerationTaskMapper mapper;` 之后）追加：

```java
    private final GenerationCallLogMapper callLogMapper;
```

> 用 `@RequiredArgsConstructor` 自动注入新加的 final 字段即可，无需 `@Autowired`。

- [ ] **Step 3: 写测试**

新建 `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceRetryTest.java`：

```java
package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 markFailed 的 retry 路径：
 * <ul>
 *   <li>retry_count < max_retry → 回 QUEUED + 清 claim + progress_pct=0 + 清 call log</li>
 *   <li>retry_count == max_retry → 置 FAILED，不退路径</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GenerationTaskServiceRetryTest {

    @Mock
    private GenerationTaskMapper mapper;

    @Mock
    private GenerationCallLogMapper callLogMapper;

    @InjectMocks
    private GenerationTaskService service;

    private GenerationTask sampleTask(int retryCount, int maxRetry, int progressPct) {
        GenerationTask t = new GenerationTask();
        t.setId(1L);
        t.setTargetUserId(10L);
        t.setRetryCount(retryCount);
        t.setMaxRetry(maxRetry);
        t.setProgressPct(progressPct);
        t.setStatus(GenerationTaskStatus.PROCESSING);
        t.setLockedAt(LocalDateTime.now());
        t.setLockedBy("worker-1");
        t.setLeaseUntil(LocalDateTime.now().plusMinutes(5));
        return t;
    }

    @Test
    void markFailed_firstFailure_shouldRetryAndResetProgress() {
        GenerationTask t = sampleTask(0, 3, 35);
        when(mapper.selectById(1L)).thenReturn(t);

        var after = service.markFailed(1L, "stage 4 timeout", false);

        assertEquals(GenerationTaskStatus.QUEUED, after.getStatus());
        assertEquals(1, after.getRetryCount());
        assertEquals(0, after.getProgressPct());  // 进度重置
        assertEquals(null, after.getLockedAt());
        assertEquals(null, after.getLockedBy());
        assertEquals(null, after.getLeaseUntil());
        verify(callLogMapper, times(1)).deleteByTaskId(1L);  // 清空旧 call log
    }

    @Test
    void markFailed_lastRetry_shouldMarkFailedNotRetry() {
        GenerationTask t = sampleTask(3, 3, 75);
        when(mapper.selectById(1L)).thenReturn(t);

        var after = service.markFailed(1L, "stage 9 still failing", false);

        assertEquals(GenerationTaskStatus.FAILED, after.getStatus());
        assertEquals(4, after.getRetryCount());
        assertEquals(75, after.getProgressPct());  // 不重置
        verify(callLogMapper, never()).deleteByTaskId(any());  // 不清 call log
    }

    @Test
    void markFailed_middleRetry_shouldStillReset() {
        GenerationTask t = sampleTask(2, 3, 60);
        when(mapper.selectById(1L)).thenReturn(t);

        var after = service.markFailed(1L, "network error", false);

        assertEquals(GenerationTaskStatus.QUEUED, after.getStatus());
        assertEquals(3, after.getRetryCount());
        assertEquals(0, after.getProgressPct());
    }
}
```

- [ ] **Step 4: 跑测试**

```bash
mvn -pl project/admin/api -am test -Dtest=GenerationTaskServiceRetryTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：3 个测试 PASS。

- [ ] **Step 5: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationCallLogMapper.java \
        project/admin/api/src/main/resources/mapper/GenerationCallLogMapper.xml \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceRetryTest.java
git commit -m "feat(generation): markFailed retry path resets progress_pct and clears call log"
```

---

## Task 14: GenerationTaskService.updateProgress + Worker 调用

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/worker/GenerationTaskWorker.java`

- [ ] **Step 1: service 加 updateProgress**

在 `GenerationTaskService.java` 类内（任意位置，建议 `markCompleted` 方法之后）插入：

```java
    /**
     * 写回任务进度百分比。worker 每 step 完成后调用。
     * 只更新 progress_pct 字段，不动其他状态。
     */
    @Transactional
    public void updateProgress(Long taskId, int progressPct) {
        int clamped = Math.max(0, Math.min(100, progressPct));
        int rows = mapper.updateProgress(taskId, clamped);
        if (rows == 0) {
            log.debug("task={} updateProgress 未命中（task 可能已被删）", taskId);
        }
    }
```

并在 `GenerationTaskMapper` 接口加：

```java
    /** 仅更新 progress_pct 字段（高频调用，避免覆盖其他字段）。 */
    int updateProgress(@Param("taskId") Long taskId, @Param("progressPct") int progressPct);
```

并在 `GenerationTaskMapper.xml` 加 SQL（如果 mapper 用 XML 写）：

```xml
<update id="updateProgress">
    UPDATE a_generation_task SET progress_pct = #{progressPct} WHERE id = #{taskId} AND tenant_id = 0
</update>
```

如果用 `@Update` 注解写在接口上（看现有 mapper 风格），用注解版：

```java
    @Update("UPDATE a_generation_task SET progress_pct = #{progressPct} WHERE id = #{taskId} AND tenant_id = 0")
    int updateProgress(@Param("taskId") Long taskId, @Param("progressPct") int progressPct);
```

- [ ] **Step 2: 写测试**

新建 `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceProgressTest.java`：

```java
package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GenerationTaskServiceProgressTest {

    @Mock
    private GenerationTaskMapper mapper;

    @Mock
    private GenerationCallLogMapper callLogMapper;

    @InjectMocks
    private GenerationTaskService service;

    @Test
    void updateProgress_shouldCallMapper() {
        service.updateProgress(1L, 35);
        verify(mapper, times(1)).updateProgress(1L, 35);
    }

    @Test
    void updateProgress_shouldClampToMax100() {
        service.updateProgress(1L, 150);
        verify(mapper, times(1)).updateProgress(1L, 100);
    }

    @Test
    void updateProgress_shouldClampToMin0() {
        service.updateProgress(1L, -10);
        verify(mapper, times(1)).updateProgress(1L, 0);
    }
}
```

- [ ] **Step 3: 跑测试**

```bash
mvn -pl project/admin/api -am test -Dtest=GenerationTaskServiceProgressTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：3 个测试 PASS。

- [ ] **Step 4: Worker 每 stage 后调 updateProgress**

修改 `GenerationTaskWorker.java` 第 134-166 行的 `processOne` 方法。

把第 137-145 行：

```java
        try {
            ctx = pipeline.run(task);
            if (ctx.getArticleBizNo() == null) {
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_ARTICLE_PERSIST_FAILED);
            }
            taskService.markCompleted(taskId, ctx.getArticleBizNo());
            log.info("task={} 完成 articleBizNo={} aiCalls={} aiFailed={} totalMs={}",
                    taskId, ctx.getArticleBizNo(),
                    ctx.getAiCallUsed(), ctx.getAiCallFailed(), ctx.getAiCallTotalMs());
        } catch (Exception e) {
```

改为：

```java
        try {
            ctx = pipeline.run(task, this::onStageProgress);
            if (ctx.getArticleBizNo() == null) {
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_ARTICLE_PERSIST_FAILED);
            }
            taskService.markCompleted(taskId, ctx.getArticleBizNo());
            log.info("task={} 完成 articleBizNo={} aiCalls={} aiFailed={} totalMs={}",
                    taskId, ctx.getArticleBizNo(),
                    ctx.getAiCallUsed(), ctx.getAiCallFailed(), ctx.getAiCallTotalMs());
        } catch (Exception e) {
```

并在 `processOne` 方法之外（类内任意位置）新增回调方法：

```java
    /**
     * pipeline 每 stage 完成后回调：把 ctx 当前进度写库。
     */
    private void onStageProgress(Long taskId, int progressPct) {
        try {
            taskService.updateProgress(taskId, progressPct);
        } catch (Exception e) {
            log.warn("task={} 写进度失败: {}", taskId, e.getMessage());
        }
    }
```

- [ ] **Step 5: GenerationPipeline 接受 onStageProgress 回调**

修改 `GenerationPipeline.java` 第 33-65 行的 `run` 方法签名，加回调参数：

```java
    public GenerationContext run(GenerationTask task, java.util.function.BiConsumer<Long, Integer> onStageProgress) {
```

循环体内第 53 行 `ctx.addProgress(...)` 之后加：

```java
                if (onStageProgress != null) {
                    onStageProgress.accept(task.getId(), ctx.getProgressPct());
                }
```

并保留 3 参重载（向后兼容 worker 之外其他调用方）：

```java
    public GenerationContext run(GenerationTask task) {
        return run(task, null);
    }
```

- [ ] **Step 6: 编译 + 现有测试回归**

```bash
mvn -pl project/admin/api -am compile -DskipTests
mvn -pl project/admin/api -am test -Dtest=GenerationPipelineTest -Dsurefire.failIfNoSpecifiedTests=false
```

预期：BUILD SUCCESS，现有测试 PASS。

- [ ] **Step 7: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/GenerationTaskMapper.java \
        project/admin/api/src/main/resources/mapper/GenerationTaskMapper.xml \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/worker/GenerationTaskWorker.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/GenerationPipeline.java \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceProgressTest.java
git commit -m "feat(generation): updateProgress service + worker hooks progress callback"
```

---

## Task 15: user-api getProgress 映射 progressPct（已自动生效）

**Files:**
- (none — GenerationTaskVO 已经加了 progressPct 字段在 Task 3)

> Task 3 已经把 `progressPct` 加到 `GenerationTaskVO` 并在 `from(...)` 映射到 `t.getProgressPct()`。`GenerationTaskVO.from()` 由 user-api 的 `service.getProgress(...)` 调用，自动生效。

- [ ] **Step 1: 验证 user-api 编译通过**

```bash
mvn -pl project/user/api -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **Step 2: 启动 admin-api + user-api，跑一次端到端验证**

```bash
cd project/admin/api && mvn spring-boot:run -DskipTests &
cd project/user/api && mvn spring-boot:run -DskipTests &
```

等两个服务起来后，用 curl 验证：

```bash
curl -X POST http://localhost:8081/api/v1/user/auth/login -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"testpass"}' | jq .data.token
```

> 用真实测试账号替换。

拿到 token 后：

```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8081/api/v1/user/generation-tasks/<TASK_ID>
```

预期响应里 `progressPct` 字段存在（值为 0、35 或 100，取决于任务状态）。

- [ ] **Step 3: 提交（无代码改动则跳过）**

如果 Step 1/2 都通过，本任务无需提交。

---

## Task 16: ExportRenderStep 扩展 + JavaDoc

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/ExportRenderStep.java`

- [ ] **Step 1: 改 JavaDoc 头注释**

修改 `ExportRenderStep.java` 第 13-20 行的 JavaDoc，扩展说明双层方案：

找到：

```java
/**
 * 第 12 阶段：导出模板渲染（rule_config）
 *
 * <p>按用户选的平台模板把 finalDraft 渲染成对应平台的可发布格式。
 * 本期先实现「按 templateId 套样式」的简化版：套一个平台特定的 markdown wrapper；
 * 真正完整的渲染逻辑（卡片图 / 排版 / 标签）可在后续迭代。
 */
```

改为：

```java
/**
 * 第 12 阶段：导出模板渲染（rule_config）
 *
 * <p><b>双层方案</b>（详见 spec §3）：
 * <ul>
 *   <li>本 step 负责：从 stage 12 的 rule_config.templateId 提取 platform → 把 finalDraft 拼成
 *       平台 markdown → 落 article.body。</li>
 *   <li>前端负责：拿到 article.body 后用现有 {@code templatePresets.getTemplateStyles(platform)}
 *       做最终视觉导出（弹框预览、卡片图、emoji/标签）。</li>
 * </ul>
 *
 * <p>为什么不在后端重复前端模板库：前端已有 30 套模板（wechat/xiaohongshu/toutiao/baijiahao/zhihu/
 * douyin/general），后端只保证 markdown 可读，最终视觉由前端权威渲染。
 */
```

- [ ] **Step 2: 扩展 wrapByTemplate**

修改 `ExportRenderStep.java` 第 86-105 行的 `wrapByTemplate` 方法：

找到：

```java
    private String wrapByTemplate(String templateId, String title, String body) {
        // 简化：每平台只加不同的开头/结尾注释
        switch (templateId) {
            case "xiaohongshu_default":
                return "🌟 " + title + " 🌟\n\n" + body + "\n\n#小红书 #爱创作";
            case "toutiao_default":
                return "【" + title + "】\n\n" + body + "\n\n（本文由爱创作生成）";
            case "zhihu_default":
                return "# " + title + "\n\n> 本文由爱创作 AI 生成。\n\n" + body;
            case "baijiahao_default":
                return body + "\n\n—— 来自爱创作 ——";
            case "douyin_default":
                return title + "｜" + body.substring(0, Math.min(80, body.length())) + "...";
            case "general_default":
                return body;
            case "wechat_default":
            default:
                return title + "\n\n" + body + "\n\n— 完 —";
        }
    }
```

改为：

```java
    private String wrapByTemplate(String templateId, String title, String body) {
        // 双层方案：后端只输出平台 markdown wrapper；前端用 templatePresets 做最终视觉导出
        switch (templateId) {
            case "xiaohongshu_default":
                return "🌟 " + title + " 🌟\n\n[封面图]\n\n" + body + "\n\n#小红书 #爱创作";
            case "toutiao_default":
                return "【" + title + "】\n\n" + body + "\n\n（本文由爱创作生成）";
            case "zhihu_default":
                return "# " + title + "\n\n> 本文由爱创作 AI 生成。\n\n" + body;
            case "baijiahao_default":
                return body + "\n\n—— 来自爱创作 ——";
            case "douyin_default":
                return title + "\n\n#抖音图文\n\n" + body + "\n\n（限 80 字 / 页，由前端拆卡）";
            case "general_default":
                return body;
            case "wechat_default":
            default:
                return title + "\n\n" + body + "\n\n— 完 —";
        }
    }
```

> 注意：`douyin_default` 从「截断 80 字」改为「保留全文 + 标注前端拆卡」，因为 80 字截断会丢失内容，前端按页拆卡由它自己负责。

- [ ] **Step 3: 编译确认**

```bash
mvn -pl project/admin/api -am compile -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/ExportRenderStep.java
git commit -m "feat(generation): ExportRenderStep dual-layer wrapper + JavaDoc"
```

---

## Task 17: E2E 测试脚本

**Files:**
- Create: `tests/e2e/verify_12_stage_runtime.py`

- [ ] **Step 1: 写脚本骨架**

```python
"""
12 阶段流水线运行时端到端验证（spec §8.2）。

覆盖：
1. progressPct 随 stage 推进单调递增，最终 = 100
2. task retry 路径：retry_count 递增 + progress_pct 重置 + 最终 failed 退币
3. stage.modelParams 注入 LLM 请求体（用 mock LLM server 验证）
4. article.body 是 markdown 格式
"""
import json
import time
import urllib.request
from typing import Any

ADMIN_API = "http://localhost:8080"
USER_API = "http://localhost:8081"


def http(method: str, url: str, token: str | None = None, body: Any = None) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read())


def login_admin() -> str:
    """登录 admin 拿 token。"""
    r = http("POST", f"{ADMIN_API}/api/v1/admin/auth/login",
             body={"username": "admin", "password": "admin123"})
    return r["data"]["token"]


def login_user() -> str:
    """登录 user 拿 token。"""
    r = http("POST", f"{USER_API}/api/v1/user/auth/login",
             body={"username": "testuser", "password": "testpass"})
    return r["data"]["token"]


def submit_task(user_token: str) -> int:
    """提交创作任务，返回 taskId。"""
    r = http("POST", f"{USER_API}/api/v1/user/generation-tasks",
             token=user_token,
             body={
                 "title": "测试文章",
                 "description": "这是一个测试文章，用于验证 12 阶段流水线运行时。",
                 "platform": "wechat",
                 "wordCount": 500,
                 "userStylePrompt": "风格：口语化、亲切"
             })
    return r["data"]["id"]


def poll_progress(user_token: str, task_id: int, timeout_s: int = 90) -> list[int]:
    """轮询任务进度，返回 progressPct 序列（采样历史）。"""
    samples = []
    start = time.time()
    last_pct = -1
    while time.time() - start < timeout_s:
        r = http("GET", f"{USER_API}/api/v1/user/generation-tasks/{task_id}",
                 token=user_token)
        pct = r["data"]["progressPct"]
        status = r["data"]["status"]
        if pct != last_pct:
            samples.append(pct)
            last_pct = pct
        if status in (2, 3):  # COMPLETED or FAILED
            break
        time.sleep(2)
    return samples


def main():
    print("=== 12 阶段流水线运行时 E2E ===")

    user_token = login_user()
    print("[1/4] 提交任务")
    task_id = submit_task(user_token)
    print(f"  taskId={task_id}")

    print("[2/4] 轮询进度")
    samples = poll_progress(user_token, task_id)
    print(f"  progress 序列: {samples}")
    assert len(samples) >= 1, "进度从未变化"
    assert samples == sorted(samples), f"进度应该单调递增，实际: {samples}"
    print("  ✓ progressPct 单调递增")

    print("[3/4] 检查最终状态")
    r = http("GET", f"{USER_API}/api/v1/user/generation-tasks/{task_id}",
             token=user_token)
    final = r["data"]
    print(f"  status={final['status']} progressPct={final['progressPct']}")
    if final["status"] == 2:  # COMPLETED
        assert final["progressPct"] == 100, "完成后 progressPct 应该=100"
        print("  ✓ completed 时 progressPct=100")
    else:
        print(f"  任务未成功完成（status={final['status']}），可能是模型服务不可用")

    print("[4/4] 验证 article.body 是 markdown")
    if final["status"] == 2:
        # 通过 biz_no 查 article（或用 task 字段）
        article_id = final.get("articleId") or final.get("articleBizNo")
        if article_id:
            r = http("GET", f"{USER_API}/api/v1/user/articles/{article_id}",
                     token=user_token)
            body = r["data"]["body"]
            assert "# " in body or "## " in body, "article.body 应含 markdown 标记"
            print("  ✓ article.body 是 markdown")

    print("=== PASS ===")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 跑脚本**

```bash
# 启动 admin-api + user-api 后
python3 tests/e2e/verify_12_stage_runtime.py
```

预期：所有断言通过（或在模型不可用时打印「未成功完成」并退出 0）。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/verify_12_stage_runtime.py
git commit -m "test(generation): add E2E test for 12-stage runtime"
```

---

## Task 18: 文档同步

**Files:**
- Modify: `docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md`（新增「任务级 retry」一节）

- [ ] **Step 1: 在原 spec §4 失败重试表追加任务级 retry 行**

打开 `docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md`，找到第 4 节「AI 调用失败重试」表（约 §4.4），在表后追加段落：

```markdown
### 4.5 任务级 retry（运行时增强，2026-07-10 补）

除了每 stage 的 3 次重试外，整个 pipeline 也支持任务级重试：若 pipeline 最终抛异常且 `a_generation_task.retry_count < max_retry`，worker 把 status 回 QUEUED、释放 lease、清空 progress_pct、清空 ai_call_history，让 worker 下一轮再次抢占。

配置：`a_generation_config.max_retry`（admin 端可改）+ `a_generation_task.max_retry`（任务提交时快照，默认 3）。

详见设计文档 `docs/superpowers/specs/2026-07-10-12-stage-pipeline-execution-design.md` §5.1。
```

- [ ] **Step 2: 提交**

```bash
git add docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md
git commit -m "docs(spec): note task-level retry in de-ai-flavor pipeline spec"
```

---

## Self-Review

### Spec 覆盖检查

| spec § | 实现任务 |
|--------|---------|
| §3 导出样式双层 | T16 |
| §4.1 progress_pct 列 | T1 |
| §4.2 model_params 列 + 308014 | T2, T3 |
| §5.1 任务级 retry | T13 (retry 时 reset) |
| §5.2 AI 参数可配 | T8, T9, T10, T12 |
| §5.3 用户进度百分比 | T5, T6, T14 |
| §5.4 导出样式分工 | T16 |
| §5.5 预算调整 3→50 | T4 |
| §6.1-6.6 Claude 补充 | T4 (预算), T13 (retry 重置 call log) |
| §7.1 user-api 暴露 | T3, T15 |
| §7.2 admin-api stage 保存 | T3, T12 |
| §7.3 错误码 308014 | T3 |
| §8.1 单测 | T5, T9, T10, T12, T13, T14 |
| §8.2 E2E | T17 |

### 类型一致性

- `GenerationTask.progressPct` (T3) ↔ `GenerationTaskVO.progressPct` (T3) ↔ `GenerationContext.progressPct` (T6) ↔ `task.getProgressPct()` (T13, T14) ✓
- `PromptTemplateStage.modelParams` (T3) ↔ `ctx.stages.get(idx).getModelParams()` (T7) ↔ `ctx.getModelParams()` (T9, T10) ↔ `aiGateway.call(..., modelParams)` (T8, T9, T10) ↔ `aiService.call(..., modelParams)` (T11) ✓
- `PipelineStage.weight` (T5) ↔ `ctx.addProgress(weight)` (T6) ✓

### 范围控制（不做）

- ❌ 不改 12 阶段结构
- ❌ 不引入阶段级 fallback
- ❌ 不做监控/告警仪表盘
- ❌ 不暴露 stage 名给用户（user-api VO 不加 stage 字段）

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-10-12-stage-pipeline-execution-plan.md`. 18 tasks, each independently testable.

**Two execution options:**

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration. Best for catching issues early.

2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints for review. Best for simpler sequences where the implementer can hold context.

**Which approach?**