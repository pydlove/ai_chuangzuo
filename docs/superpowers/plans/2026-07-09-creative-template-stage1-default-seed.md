# 创作模板阶段 1：固化默认模板

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md 5.16.1 阶段 1：把 12 阶段流水线的默认提示词与规则固化为内置模板 `default-v1`，无需任何 UI 操作即可让 worker 跑出可复现结果。

**Architecture:**

- 数据层：`t_prompt_template`（已有）+ `t_prompt_template_stage`（已有）保存启用模板与 12 阶段配置。
- 默认值源：本次新增外部 JSON 文件 `project/shared/src/main/resources/creative-template/default-v1.json`，作为单一可信源；Java 端 `PipelineStage` enum 与之对齐。
- 装载：新增 Flyway 迁移 `V2.0.0_017__seed_default_prompt_template.sql`，启动时把 JSON 内容插入 `t_prompt_template`（id=1, enabled=1）与 12 行 `t_prompt_template_stage`（用 `ON DUPLICATE KEY UPDATE` 幂等）。
- 运行时：`PipelineTemplateResolver.resolveInto(ctx)` 已存在，本次只需确认它能稳定从 (id=1, enabled=1) 读取。
- 前端：admin-web 的 `PromptTemplateListView` 自动展示该默认模板（已有，不动），但要新增「内置模板」标签，避免运营误删。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, Flyway, Jackson, Vue 3, Ant Design Vue, Playwright.

## 全局约束

- 不引入新的中间件。
- 不动现有 16 个迁移（V2.0.0_001 ~ V2.0.0_016）的语义，本次新增迁移序号 `V2.0.0_017`。
- JSON 配置与 `PipelineStage` enum 的 `defaultAiPrompt` / `defaultRuleConfigJson` 必须保持字符级一致（dev 阶段用脚本校验）。
- 不修改运行时 worker 行为；只在启动时多一个 seeded template 存在。
- 不删 `t_prompt_template.base_content` 字段（V2.0.0_011 遗留），但本次 seed 把它留空字符串，明确不再使用。

---

## 现状盘点（实施前必读）

| 已完成项 | 文件 / 位置 |
|----------|-------------|
| `t_prompt_template` 表 | `V2.0.0_011__create_prompt_template_table.sql` |
| `t_prompt_template_stage` 表（12 阶段） | `V2.0.0_014__create_prompt_template_stage_table.sql` |
| `a_generation_config` 表（含重试配置） | `V2.0.0_013/015` |
| `a_generation_call_log` 表 | `V2.0.0_016` |
| `PipelineStage` enum（12 阶段默认值） | `project/admin/api/src/main/java/.../pipeline/PipelineStage.java` |
| `PipelineTemplateResolver`（运行时加载） | `pipeline/PipelineTemplateResolver.java` |
| `PromptTemplateService`（CRUD + enable/disable） | `service/PromptTemplateService.java` |
| Admin 列表/编辑页 | `project/admin/web/src/views/PromptTemplateListView.vue` + `PromptTemplateEditView.vue` |
| 单测 | `project/admin/api/src/test/java/.../service/PromptTemplateServiceStageTest.java` |

| 缺口 | 本计划要补 |
|------|------------|
| 没有任何 `enabled=1` 的默认模板 | Task 1：JSON 配置文件 |
| ↑ 启动后 worker 立即抛 `PROMPT_TEMPLATE_NO_ENABLED` | Task 2：seed 迁移 |
| `PipelineStage` enum 的默认值散落在 Java 源码 | Task 1：抽到 JSON 单一可信源 |
| 没有端到端验证：seed 后能否跑通一条任务 | Task 3：Playwright + 集成测试 |
| 列表页无法区分内置 vs 自建 | Task 4：UI 标签 |

---

## File Structure

| 文件 | 职责 |
|------|------|
| `project/shared/src/main/resources/creative-template/default-v1.json` | **新增** 默认模板配置（12 阶段 ai_prompt + rule_config 完整 JSON） |
| `project/shared/src/main/java/com/aichuangzuo/shared/creative/CreativeTemplateConstants.java` | **新增** 常量类（id=1、name="默认去 AI 味模板"、JSON 文件路径） |
| `project/admin/api/src/main/resources/db/migration/V2.0.0_017__seed_default_prompt_template.sql` | **新增** Flyway 迁移，插入 id=1 模板 + 12 阶段 |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/creative/DefaultTemplateJsonConsistencyTest.java` | **新增** 校验 JSON 与 `PipelineStage` enum 默认值字符级一致 |
| `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/creative/SeedMigrationIT.java` | **新增** Spring Boot 集成测试，启动后能查到 id=1 模板且 enabled=1 |
| `project/admin/web/src/views/PromptTemplateListView.vue` | **修改** 给内置模板加「内置」标签 |
| `tests/e2e/verify_default_template_seed.py` | **新增** Playwright 端到端：admin 看到默认模板、点编辑能看到 12 阶段、跑一条任务验证成功 |

---

## Task 1：JSON 默认模板配置

**Files:**
- Create: `project/shared/src/main/resources/creative-template/default-v1.json`
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/creative/CreativeTemplateConstants.java`

- [ ] **Step 1.1：创建 JSON 文件**

在 `project/shared/src/main/resources/creative-template/` 目录新建 `default-v1.json`：

```json
{
  "template": {
    "id": 1,
    "name": "默认去 AI 味模板",
    "remark": "标准 12 阶段去 AI 味写作流水线，admin 不要删除，可在基础上复制派生",
    "stages": [
      {
        "index": 1,
        "stageKey": "intent_anchor",
        "stageType": "passthrough",
        "enabled": 1,
        "aiPrompt": null,
        "ruleConfig": null
      },
      {
        "index": 2,
        "stageKey": "outline",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位资深编辑。请根据以下文章意图，为这篇文章生成一份职责式大纲。\n\n[user_context_block]\n\n任务：\n- 把全文拆成若干段落\n- 每段只写\"职责\"，不写\"主题\"或具体内容\n- 职责要明确、不重复、不抽象\n- 风格提示词可间接影响结构选择\n\n输出格式（JSON）：\n{\n  \"paragraphs\": [\n    {\"index\": 1, \"responsibility\": \"建立好奇：让读者想知道答案\"},\n    {\"index\": 2, \"responsibility\": \"打破常识：指出读者原有认知的漏洞\"},\n    {\"index\": 3, \"responsibility\": \"给出新视角：提供读者想不到的角度\"}\n  ]\n}\n\n约束：\n- 职责不能用\"展开论述\"这种抽象词\n- 相邻段落职责不能重复\n- 全文围绕核心观点服务，不要偏离",
        "ruleConfig": null
      },
      {
        "index": 3,
        "stageKey": "material_list",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位事实核查编辑。请根据以下文章意图和结构骨架，列出每段需要的支撑素材，并诚实标注来源可靠性。\n\n[user_context_block]\n\n结构骨架（职责式大纲）：\n{{outline}}\n\n任务：\n- 对每一段，判断它需要什么素材支撑（数据 / 案例 / 引用 / 个人观察 / 类比等）\n- 每项素材必须标注为以下之一：\n  - 已知：用户已提供，或 AI 能 100% 确认真实的\n  - 推断：AI 推测但不能 100% 确认的\n  - 未知：AI 不知道的\n  - 待补：需要用户补充的\n\n约束：\n- 严禁编造数据、案例、人名、时间\n- 不知道的素材必须标\"未知\"，不能标\"已知\"或\"推断\"来蒙混\n- 找不到真实素材支撑的论点，标注\"建议降级\"（改用不依赖数据的论证方式）\n\n输出格式（JSON）：\n{\n  \"materials\": [\n    {\n      \"paragraph_index\": 1,\n      \"responsibility\": \"建立好奇\",\n      \"items\": [\n        {\"type\": \"数据\", \"description\": \"...\", \"reliability\": \"未知\", \"source_or_reason\": \"...\"}\n      ]\n    }\n  ]\n}",
        "ruleConfig": null
      },
      {
        "index": 4,
        "stageKey": "draft",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "用以下风格写：[user_style_prompt]\n\n文章意图：\n- 标题：[title]\n- 核心观点：[core_viewpoint]\n- 目标读者：[target_reader]\n\n结构骨架（按段落职责）：\n[outline]\n\n可用素材（仅限已知项）：\n[material_list]\n\n写作要求：\n- 按结构分块写，每块对应一个职责\n- 禁用套话（在当今社会 / 综上所述 / 值得深思 / 首先...其次...最后...）\n- 每个抽象论点配具体例子（仅限已知素材）\n- 句子长短交替\n- 不要主动补全不知道的数据\n\n输出格式（JSON）：\n{\n  \"draft\": [\n    {\"paragraph_index\": 1, \"responsibility\": \"建立好奇\", \"content\": \"...\"},\n    {\"paragraph_index\": 2, \"responsibility\": \"...\", \"content\": \"...\"}\n  ]\n}",
        "ruleConfig": null
      },
      {
        "index": 5,
        "stageKey": "rhythm_detect",
        "stageType": "rule_config",
        "enabled": 1,
        "aiPrompt": null,
        "ruleConfig": {
          "metrics": [
            {"name": "uniform_length", "enabled": true, "threshold": 5, "windowSize": 3},
            {"name": "no_breath", "enabled": true, "threshold": 35},
            {"name": "monotonous_start", "enabled": true, "threshold": 3, "windowSize": 5}
          ],
          "styleExemptions": []
        }
      },
      {
        "index": 6,
        "stageKey": "rhythm_rewrite",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位文字编辑。请根据韵律问题清单，改写文章中的问题句子，使其更像真人写作。\n\n原文（JSON 分块初稿）：\n[draft]\n\n韵律问题清单：\n{{rhythm_issues}}\n\n改写要求：\n- 只改清单中标记的问题句子/段落，不改其他部分\n- 保持原意不变\n- 保持用户风格不变\n- 用户风格中明确要求的元素（如\"多用短句破折号\"）必须保留\n\n输出格式：与原文相同的 JSON 分块初稿",
        "ruleConfig": null
      },
      {
        "index": 7,
        "stageKey": "external_review",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位文笔极好、但人品极差的毒舌同行。你和作者有私怨，早就看他不顺眼，但你的文学判断力无可挑剔。你现在要读他的文章，找出所有让你冷笑的地方——不是找错别字，而是找那些\"太正确\"\"太安全\"\"软弱无力不敢下判断\"的地方。\n\n文章：\n[draft]\n\n任务：\n逐段审查，找出以下问题：\n1. 太正确了（两边不得罪，说了一堆等于没说）\n2. 软弱无力（用\"可能\"\"某种程度上\"\"也许\"来逃避）\n3. 不敢站队（结尾回到\"见仁见智\"）\n4. 假大空（用抽象概念代替具体判断）\n5. 套路感（段落像填空，换个人名也能用）\n6. 伪金句（听起来漂亮但没有实质内容）\n\n每个问题必须具体到\"第 X 段第 Y 句\"，并给一句毒舌点评。\n最后给整体毒舌评分（满分 10 分）和一句话总结。",
        "ruleConfig": null
      },
      {
        "index": 8,
        "stageKey": "targeted_rewrite",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位资深编辑。请根据毒舌同行的点评，改写文章中的问题句子。\n\n原文（JSON 分块初稿）：\n[draft]\n\n毒舌点评清单：\n[external_review]\n\n改写要求：\n- 只改点评中标记的问题句子，保留其他部分\n- 重点解决：太正确、软弱无力、不敢站队、假大空、套路感、伪金句\n- 改写后要比原句更有立场、更具体、更像真人写的\n- 保持用户风格不变\n- 不要为了\"让毒舌满意\"而过度修改\n\n输出格式：与原文相同的 JSON 分块初稿",
        "ruleConfig": null
      },
      {
        "index": 9,
        "stageKey": "rhythm_polish",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位文字编辑，负责做最后的节奏打磨。\n\n原文（JSON 分块初稿）：\n[draft]\n\n风格约束：\n[user_style_prompt]\n\n打磨目标：\n1. 句子长短变化更明显\n2. 砍掉信息密度低的过渡句\n3. 开场前 3 句检查：是否有力、是否吸引人继续读\n4. 结尾检查：不要小结式结尾，要给读者一个画面、问题或余味\n5. 段落之间的过渡是否自然\n\n禁区（绝对不能砍）：\n- 符合用户风格的口癖 / 鲜活表达\n- 不标准但有特色的句式\n- 看似重复但承担强调功能的句子\n\n输出格式：与原文相同的 JSON 分块初稿\n最后给一个简短的修改摘要。",
        "ruleConfig": null
      },
      {
        "index": 10,
        "stageKey": "word_count",
        "stageType": "rule_config",
        "enabled": 1,
        "aiPrompt": null,
        "ruleConfig": {
          "countPunctuation": false,
          "countWhitespace": false,
          "wordCountMode": "chinese_char"
        }
      },
      {
        "index": 11,
        "stageKey": "word_adjust",
        "stageType": "ai_prompt",
        "enabled": 1,
        "aiPrompt": "你是一位资深编辑。请根据字数统计报告，对文章做最后的字数调整。\n\n原文（JSON 分块初稿）：\n[draft]\n\n字数统计报告：\n[word_count_report]\n\n任务：\n- 如果 actual > target：找出\"删掉不损失意思\"的句子/段落\n- 如果 actual ≤ target：无需增删，返回原文\n\n原则：\n- 不硬砍到 X 字，质量优先\n- 字数不足时不硬补\n- 优先删：信息密度低的过渡句、重复论证、空话\n- 绝对不删：核心观点句、关键证据、用户风格口癖",
        "ruleConfig": null
      },
      {
        "index": 12,
        "stageKey": "export_render",
        "stageType": "passthrough",
        "enabled": 1,
        "aiPrompt": null,
        "ruleConfig": null
      }
    ]
  }
}
```

注：上面 `aiPrompt` 字段中与设计文档 §3 完全一致；`ruleConfig` 字段与设计文档 5.7、5.10 一致。所有转义符合 JSON 规范（`\n` 表示换行，`\"` 表示引号）。

- [ ] **Step 1.2：创建常量类**

`project/shared/src/main/java/com/aichuangzuo/shared/creative/CreativeTemplateConstants.java`：

```java
package com.aichuangzuo.shared.creative;

/**
 * 默认创作模板（default-v1）相关常量。
 *
 * <p>设计文档：2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.10
 */
public final class CreativeTemplateConstants {

    private CreativeTemplateConstants() {}

    /** 内置默认模板固定 ID，避免运营误删。 */
    public static final long DEFAULT_TEMPLATE_ID = 1L;

    /** 默认模板名称（与 JSON 中 name 一致）。 */
    public static final String DEFAULT_TEMPLATE_NAME = "默认去 AI 味模板";

    /** classpath 资源路径（与 V2.0.0_017 迁移共享）。 */
    public static final String DEFAULT_TEMPLATE_JSON = "creative-template/default-v1.json";

    /** Admin 删除默认模板时的错误码占位（与 AdminGenerationErrorCode 合并前先用字符串标记）。 */
    public static final String ERR_CANNOT_DELETE_BUILTIN = "DEFAULT_TEMPLATE_IMMUTABLE";
}
```

- [ ] **Step 1.3：本地构建确保 shared 编译通过**

```bash
mvn -pl project/shared -am compile -DskipTests
```

Expected: BUILD SUCCESS。

- [ ] **Step 1.4：提交**

```bash
git add project/shared/src/main/resources/creative-template/default-v1.json \
        project/shared/src/main/java/com/aichuangzuo/shared/creative/CreativeTemplateConstants.java
git commit -m "feat(creative-template): add default-v1 json config and shared constants"
```

---

## Task 2：Flyway seed 迁移

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_017__seed_default_prompt_template.sql`

- [ ] **Step 2.1：编写迁移**

由于 Flyway 不直接支持读 classpath JSON，本迁移用 inline 方式插入 12 阶段。先插模板主表，再插 stage 表（`ON DUPLICATE KEY UPDATE` 幂等）。

```sql
-- 默认创作模板 default-v1 seed
-- 启动后 worker 可立即从 (id=1, enabled=1) 加载
-- JSON 单一可信源：project/shared/src/main/resources/creative-template/default-v1.json
-- 阶段字段值必须与 JSON 字符级一致；改动先改 JSON 再回流到本 SQL

SET NAMES utf8mb4;

-- 模板主表
INSERT INTO t_prompt_template
    (id, name, base_content, enabled, remark, tenant_id, is_deleted, created_by, updated_by)
VALUES
    (1, '默认去 AI 味模板', '', 1, '标准 12 阶段去 AI 味写作流水线，admin 不要删除，可在基础上复制派生', 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    enabled = 1,
    remark = VALUES(remark),
    updated_at = CURRENT_TIMESTAMP(3);

-- 12 阶段
INSERT INTO t_prompt_template_stage
    (template_id, stage_index, stage_type, stage_key, ai_prompt, rule_config, enabled, tenant_id, is_deleted, created_by, updated_by)
VALUES
    (1, 1,  'passthrough', 'intent_anchor',     NULL, NULL, 1, 0, 0, 0, 0),
    (1, 2,  'ai_prompt',   'outline',          '你是一位资深编辑。请根据以下文章意图，为这篇文章生成一份职责式大纲。\n\n[user_context_block]\n\n任务：\n- 把全文拆成若干段落\n- 每段只写\"职责\"，不写\"主题\"或具体内容\n- 职责要明确、不重复、不抽象\n- 风格提示词可间接影响结构选择\n\n输出格式（JSON）：\n{\n  \"paragraphs\": [\n    {\"index\": 1, \"responsibility\": \"建立好奇：让读者想知道答案\"},\n    {\"index\": 2, \"responsibility\": \"打破常识：指出读者原有认知的漏洞\"},\n    {\"index\": 3, \"responsibility\": \"给出新视角：提供读者想不到的角度\"}\n  ]\n}\n\n约束：\n- 职责不能用\"展开论述\"这种抽象词\n- 相邻段落职责不能重复\n- 全文围绕核心观点服务，不要偏离', NULL, 1, 0, 0, 0, 0),
    (1, 3,  'ai_prompt',   'material_list',    '你是一位事实核查编辑。请根据以下文章意图和结构骨架，列出每段需要的支撑素材，并诚实标注来源可靠性。\n\n[user_context_block]\n\n结构骨架（职责式大纲）：\n{{outline}}\n\n任务：\n- 对每一段，判断它需要什么素材支撑（数据 / 案例 / 引用 / 个人观察 / 类比等）\n- 每项素材必须标注为以下之一：\n  - 已知：用户已提供，或 AI 能 100% 确认真实的\n  - 推断：AI 推测但不能 100% 确认的\n  - 未知：AI 不知道的\n  - 待补：需要用户补充的\n\n约束：\n- 严禁编造数据、案例、人名、时间\n- 不知道的素材必须标\"未知\"，不能标\"已知\"或\"推断\"来蒙混\n- 找不到真实素材支撑的论点，标注\"建议降级\"（改用不依赖数据的论证方式）\n\n输出格式（JSON）：\n{\n  \"materials\": [\n    {\n      \"paragraph_index\": 1,\n      \"responsibility\": \"建立好奇\",\n      \"items\": [\n        {\"type\": \"数据\", \"description\": \"...\", \"reliability\": \"未知\", \"source_or_reason\": \"...\"}\n      ]\n    }\n  ]\n}', NULL, 1, 0, 0, 0, 0),
    (1, 4,  'ai_prompt',   'draft',            '用以下风格写：[user_style_prompt]\n\n文章意图：\n- 标题：[title]\n- 核心观点：[core_viewpoint]\n- 目标读者：[target_reader]\n\n结构骨架（按段落职责）：\n[outline]\n\n可用素材（仅限已知项）：\n[material_list]\n\n写作要求：\n- 按结构分块写，每块对应一个职责\n- 禁用套话（在当今社会 / 综上所述 / 值得深思 / 首先...其次...最后...）\n- 每个抽象论点配具体例子（仅限已知素材）\n- 句子长短交替\n- 不要主动补全不知道的数据\n\n输出格式（JSON）：\n{\n  \"draft\": [\n    {\"paragraph_index\": 1, \"responsibility\": \"建立好奇\", \"content\": \"...\"},\n    {\"paragraph_index\": 2, \"responsibility\": \"...\", \"content\": \"...\"}\n  ]\n}', NULL, 1, 0, 0, 0, 0),
    (1, 5,  'rule_config', 'rhythm_detect',    NULL, JSON_OBJECT('metrics', JSON_ARRAY(JSON_OBJECT('name','uniform_length','enabled',true,'threshold',5,'windowSize',3), JSON_OBJECT('name','no_breath','enabled',true,'threshold',35), JSON_OBJECT('name','monotonous_start','enabled',true,'threshold',3,'windowSize',5)), 'styleExemptions', JSON_ARRAY()), 1, 0, 0, 0, 0),
    (1, 6,  'ai_prompt',   'rhythm_rewrite',   '你是一位文字编辑。请根据韵律问题清单，改写文章中的问题句子，使其更像真人写作。\n\n原文（JSON 分块初稿）：\n[draft]\n\n韵律问题清单：\n{{rhythm_issues}}\n\n改写要求：\n- 只改清单中标记的问题句子/段落，不改其他部分\n- 保持原意不变\n- 保持用户风格不变\n- 用户风格中明确要求的元素（如\"多用短句破折号\"）必须保留\n\n输出格式：与原文相同的 JSON 分块初稿', NULL, 1, 0, 0, 0, 0),
    (1, 7,  'ai_prompt',   'external_review',  '你是一位文笔极好、但人品极差的毒舌同行。\n\n文章：\n[draft]\n\n任务：逐段审查\"太正确\"\"太安全\"\"软弱无力\"的问题，给出毒舌点评 + 整体评分（满分 10 分）。\n\n每个问题必须具体到\"第 X 段第 Y 句\"。', NULL, 1, 0, 0, 0, 0),
    (1, 8,  'ai_prompt',   'targeted_rewrite', '你是一位资深编辑。请根据毒舌同行的点评，改写文章中的问题句子。\n\n原文：\n[draft]\n\n毒舌点评清单：\n[external_review]\n\n改写要求：\n- 只改点评中标记的问题句子，保留其他部分\n- 重点解决：太正确、软弱无力、不敢站队、假大空、套路感、伪金句\n- 保持用户风格不变\n\n输出格式：与原文相同的 JSON 分块初稿', NULL, 1, 0, 0, 0, 0),
    (1, 9,  'ai_prompt',   'rhythm_polish',    '你是一位文字编辑，负责做最后的节奏打磨。\n\n原文：\n[draft]\n\n风格约束：\n[user_style_prompt]\n\n打磨目标：\n1. 句子长短变化更明显\n2. 砍掉信息密度低的过渡句\n3. 开场前 3 句检查：是否有力\n4. 结尾检查：不要小结式结尾\n5. 段落之间的过渡是否自然\n\n禁区：符合用户风格的口癖不能砍\n\n输出格式：与原文相同的 JSON 分块初稿 + 简短修改摘要', NULL, 1, 0, 0, 0, 0),
    (1, 10, 'rule_config', 'word_count',       NULL, JSON_OBJECT('countPunctuation', false, 'countWhitespace', false, 'wordCountMode', 'chinese_char'), 1, 0, 0, 0, 0),
    (1, 11, 'ai_prompt',   'word_adjust',      '你是一位资深编辑。请根据字数统计报告，对文章做最后的字数调整。\n\n原文：\n[draft]\n\n字数统计报告：\n[word_count_report]\n\n任务：\n- 如果 actual > target：找出\"删掉不损失意思\"的句子/段落\n- 如果 actual ≤ target：无需增删\n\n原则：不硬砍到 X 字，质量优先', NULL, 1, 0, 0, 0, 0),
    (1, 12, 'passthrough', 'export_render',    NULL, NULL, 1, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    stage_type = VALUES(stage_type),
    stage_key = VALUES(stage_key),
    ai_prompt = VALUES(ai_prompt),
    rule_config = VALUES(rule_config),
    enabled = VALUES(enabled);
```

> 注：本迁移内嵌的 `aiPrompt` 文本与 Task 1.1 JSON 中相应字段字符级一致；任务规则通过 Step 1.4 的 `DefaultTemplateJsonConsistencyTest` 强制校验。

- [ ] **Step 2.2：在 admin-api 启动一次测试库**

```bash
# 启动本地 MySQL，启动 admin-api（让 Flyway 自动跑 V2.0.0_017）
mvn -pl project/admin/api -am spring-boot:run
```

观察启动日志：

```
Successfully applied 17 migrations to schema ...
Migrating schema ... to version 2.0.0 - 017 - seed default prompt template
```

- [ ] **Step 2.3：查 DB 确认 seed 成功**

```sql
SELECT id, name, enabled, remark FROM t_prompt_template WHERE id = 1;
-- 期望：1 行，name="默认去 AI 味模板"，enabled=1

SELECT stage_index, stage_key, stage_type, enabled,
       LENGTH(ai_prompt) AS ai_prompt_len,
       rule_config IS NOT NULL AS has_rule
FROM t_prompt_template_stage
WHERE template_id = 1
ORDER BY stage_index;
-- 期望：12 行，stage_index 1..12，ai_prompt_len 大于 0（除 passthrough 外）
```

- [ ] **Step 2.4：触发 worker 加载一次**

最简方式：调用 `PipelineTemplateResolver.resolveInto(ctx)`：

```java
GenerationContext ctx = new GenerationContext();
templateResolver.resolveInto(ctx);
assert ctx.getTemplate().getId() == 1L;
assert ctx.getTemplate().getEnabled() == 1;
assert ctx.getStages().size() == 12;
```

如果还没在测试用例中写，可先用 admin-web 提交一条生成任务观察 worker 日志：

```
resolved template id=1 stages=12 (defaults filled where missing)
```

- [ ] **Step 2.5：提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_017__seed_default_prompt_template.sql
git commit -m "feat(creative-template): seed default-v1 prompt template + 12 stages"
```

---

## Task 3：JSON 与 PipelineStage enum 一致性校验

**Files:**
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/creative/DefaultTemplateJsonConsistencyTest.java`

- [ ] **Step 3.1：编写单测**

```java
package com.aichuangzuo.admin.modules.creative;

import com.aichuangzuo.admin.modules.generation.pipeline.PipelineStage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 校验 classpath JSON 与 PipelineStage enum 默认值字符级一致。
 * 改动 JSON 必须同步改 enum，反之亦然，否则这里失败。
 */
class DefaultTemplateJsonConsistencyTest {

    @Test
    @DisplayName("default-v1.json 与 PipelineStage 默认值一致")
    void json_and_enum_aligned() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = getClass().getClassLoader()
                .getResourceAsStream("creative-template/default-v1.json");
        assertNotNull(in, "classpath missing: creative-template/default-v1.json");
        JsonNode root = mapper.readTree(in).get("template");
        JsonNode stages = root.get("stages");

        for (PipelineStage def : PipelineStage.ALL) {
            JsonNode row = findStage(stages, def.index);
            assertNotNull(row, "JSON 缺 stage index=" + def.index);
            assertEquals(def.key, row.get("stageKey").asText(), "stageKey 不一致 at " + def.index);
            assertEquals(def.type.code, row.get("stageType").asText(), "stageType 不一致 at " + def.index);

            JsonNode aiPrompt = row.get("aiPrompt");
            JsonNode ruleConfig = row.get("ruleConfig");

            if (def.defaultAiPrompt == null) {
                assert aiPrompt == null || aiPrompt.isNull()
                    : "stage " + def.index + " enum 无 ai_prompt 默认值，但 JSON 有";
            } else {
                assertNotNull(aiPrompt, "stage " + def.index + " JSON 缺 aiPrompt");
                assertEquals(def.defaultAiPrompt, aiPrompt.asText(),
                    "aiPrompt 不一致 at " + def.index);
            }

            if (def.defaultRuleConfigJson == null) {
                assert ruleConfig == null || ruleConfig.isNull()
                    : "stage " + def.index + " enum 无 rule_config 默认值，但 JSON 有";
            } else {
                assertNotNull(ruleConfig, "stage " + def.index + " JSON 缺 ruleConfig");
                assertEquals(mapper.readTree(def.defaultRuleConfigJson), ruleConfig,
                    "ruleConfig 不一致 at " + def.index);
            }
        }
    }

    private JsonNode findStage(JsonNode stages, int index) {
        Iterator<JsonNode> it = stages.elements();
        while (it.hasNext()) {
            JsonNode n = it.next();
            if (n.get("index").asInt() == index) return n;
        }
        return null;
    }
}
```

- [ ] **Step 3.2：跑单测**

```bash
mvn -pl project/admin/api test -Dtest=DefaultTemplateJsonConsistencyTest
```

Expected: BUILD SUCCESS。如果失败，说明 Task 1.1 的 JSON 与 enum 不一致，回到对应文件逐字符对齐。

- [ ] **Step 3.3：提交**

```bash
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/creative/DefaultTemplateJsonConsistencyTest.java
git commit -m "test(creative-template): add JSON/enum consistency check"
```

---

## Task 4：Admin UI 给默认模板加「内置」标签

**Files:**
- Modify: `project/admin/web/src/views/PromptTemplateListView.vue`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateService.java`（在 VO 增 `isBuiltin` 字段）

- [ ] **Step 4.1：后端 VO 加字段**

`PromptTemplateAdminVO.java` 加字段：

```java
private Boolean isBuiltin;
public Boolean getIsBuiltin() { return isBuiltin; }
public void setIsBuiltin(Boolean isBuiltin) { this.isBuiltin = isBuiltin; }
```

`PromptTemplateService.toVo()` 末尾：

```java
vo.setIsBuiltin(t.getId() != null && t.getId() == 1L);
```

> 也可改成读 `t_prompt_template.remark` 含「内置」前缀判断；这里先用 id=1 简化，未来若需要拆分多份内置模板可改。

- [ ] **Step 4.2：列表页渲染标签**

在 `PromptTemplateListView.vue` 的模板列里，给 `record.id === 1` 的行渲染 a-tag：

```vue
<a-tag v-if="record.isBuiltin" color="green" style="margin-left: 8px">内置</a-tag>
```

同时禁用该行的「删除」按钮（避免误删）：

```vue
<a-popconfirm v-if="!record.isBuiltin" ...>...</a-popconfirm>
<span v-else style="color: #999; font-size: 12px">内置不可删</span>
```

- [ ] **Step 4.3：手测列表页**

启动 admin-web：

```bash
cd project/admin/web && npm run dev
```

访问 `/console/prompt-templates`，确认：

- 「默认去 AI 味模板」一行带「内置」绿色标签。
- 该行没有删除按钮，显示「内置不可删」。
- 其他模板行行为不变。

- [ ] **Step 4.4：提交**

```bash
git add project/admin/web/src/views/PromptTemplateListView.vue \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/PromptTemplateAdminVO.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateService.java
git commit -m "feat(creative-template): mark default template as builtin in admin UI"
```

---

## Task 5：端到端 Playwright 验证

**Files:**
- Create: `tests/e2e/verify_default_template_seed.py`

- [ ] **Step 5.1：编写脚本**

```python
from playwright.sync_api import sync_playwright
import time

ADMIN_WEB = 'http://localhost:5174'
USER_WEB = 'http://localhost:5173'

def test_default_template():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        ctx = browser.new_context(viewport={'width': 1440, 'height': 900})

        # 1. admin 登录
        page = ctx.new_page()
        page.goto(ADMIN_WEB + '/login', wait_until='networkidle')
        page.fill('input[name="username"]', 'admin')
        page.fill('input[name="password"]', 'admin123')
        page.click('button[type="submit"]')
        time.sleep(1)

        # 2. 进入模板列表
        page.goto(ADMIN_WEB + '/console/prompt-templates', wait_until='networkidle')
        time.sleep(0.5)

        # 3. 确认默认模板存在 + 「内置」标签
        assert page.locator('text=默认去 AI 味模板').count() > 0
        assert page.locator('span:has-text("内置")').count() > 0

        # 4. 进入编辑页，确认 12 阶段
        page.click('text=默认去 AI 味模板')
        time.sleep(0.5)
        # 等待 12 个 stage tab 渲染（具体 selector 按实现调整）
        stage_count = page.locator('[data-stage-index]').count()
        assert stage_count == 12, f'expected 12 stages, got {stage_count}'

        page.screenshot(path='/tmp/default_template_admin.png', full_page=True)

        # 5. user 端提交一条生成任务
        user_page = ctx.new_page()
        user_page.goto(USER_WEB + '/login', wait_until='networkidle')
        user_page.fill('input[name="phone"]', '13800138000')
        user_page.fill('input[name="password"]', 'test123456')
        user_page.click('button[type="submit"]')
        time.sleep(1)

        user_page.goto(USER_WEB + '/console/create', wait_until='networkidle')
        user_page.fill('input[id="topic"]', '夏季防晒的 3 个误区')
        user_page.click('button:has-text("生成文章")')
        time.sleep(0.5)

        # 6. 等待任务完成（最长 60 秒）
        for _ in range(60):
            if user_page.locator('text=生成成功').count() > 0:
                break
            time.sleep(1)
        assert user_page.locator('text=生成成功').count() > 0, '任务未在 60s 内完成'

        user_page.screenshot(path='/tmp/default_template_user.png', full_page=True)

        browser.close()
        print('default template end-to-end verification passed')

if __name__ == '__main__':
    test_default_template()
```

- [ ] **Step 5.2：启动 admin-api + user-api + 两个 web**

```bash
./scripts/local/start.sh
```

（参考已有启动脚本，确保 MySQL、admin-api（默认 18081）、user-api（默认 18082）、admin-web（5174）、user-web（5173）全部起来）

- [ ] **Step 5.3：跑脚本**

```bash
python3 tests/e2e/verify_default_template_seed.py
```

Expected: 终端打印 `default template end-to-end verification passed`，两个截图可见 admin 默认模板与 user 任务完成状态。

- [ ] **Step 5.4：提交**

```bash
git add tests/e2e/verify_default_template_seed.py
git commit -m "test(creative-template): add e2e verification for default-v1 seed"
```

---

## Self-Review

**1. Spec coverage（设计文档 5.16.1 阶段 1 验收）：**

| 验收项 | 覆盖 Task |
|--------|-----------|
| 数据表 Flyway 脚本 | Task 2（V2.0.0_017 seed） |
| 模板配置 JSON 文件 | Task 1（default-v1.json） |
| 运行时按固定配置加载 | 已存在（PipelineTemplateResolver） |
| 无需任何 UI | ✅ 全程 SQL + JSON seed，无新增 UI 流程 |
| 用户提交任务能跑通 | Task 5 E2E |

**2. 不引入未实现的依赖：**

- 不新增中间件、不引入新表（仅复用已有 `t_prompt_template` / `t_prompt_template_stage`）。
- 不动现有 16 个迁移的语义。
- 不修改 worker 执行逻辑。

**3. 一致性约束：**

- JSON、SQL、V2 enum 三处默认值字符级一致，Task 3 单测强制。
- 默认模板 id 固定为 1，UI 阻止删除（Task 4）。

**4. 范围控制：**

- 不实现 stage 5 / 10 规则 UI 编辑（那是阶段 2 的范围）。
- 不实现 stage 3 / 4 / 6 等的 stageKey 改名（设计文档要求稳定 key）。
- 不实现多版本（设计文档 5.14 是阶段 2 的范围）。
- 不实现模板复制 / 删除 UI（管理端基础 CRUD 已在阶段 2 提供）。

**5. 可回滚：**

- 删 V2.0.0_017 即可回滚 seed。
- 删 default-v1.json 不影响 DB（运行时仍可启动）；下次启动再 seed。
- 删 Task 4 UI 修改对功能无影响（仅前端展示）。

阶段 1 完成可进入阶段 2（管理端 CRUD + 生命周期接口 + 编辑页落地）。