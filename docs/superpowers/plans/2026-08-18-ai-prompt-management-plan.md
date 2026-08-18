# AI 提示词管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在管理后台「系统设置」下新增「AI 提示词管理」，把当前代码中硬编码的 3 处 AI 提示词（爆款标题生成、标题优化、文风分析）抽到数据库表 `c_ai_prompt`，支持可视化编辑、`{{variable}}` 变量渲染和必填校验。

**Architecture:** 采用「共享实体 + 各端独立 Mapper + 共享渲染工具」方案：`AiPrompt` 实体放在 `project/shared`，admin-api 提供管理 CRUD 接口，admin/user 两端各自提供渲染服务读取同一张 `c_ai_prompt` 表；变量解析与替换逻辑下沉到 `shared` 的 `AiPromptVariableResolver`，保证两端行为一致。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + JDK 17；Vue 3 + Vite + Ant Design Vue 4 + Pinia；Flyway 迁移。

## Global Constraints

- 后端所有接口以 `/api/v1/admin/` 开头（admin-api）或 `/api/v1/` 开头（user-api）。
- 管理端接口仅允许 `SUPER_ADMIN` 访问。
- 变量占位符统一使用 `{{variableName}}`，大小写敏感。
- 逻辑删除，不物理删除。
- 保存即生效，无审批/发布流程。
- 不做版本历史（第一版）。
- 不改动已有的 13 阶段流水线模板管理（`t_prompt_template_stage`）。
- 用户端和管理端后端共享同一个 MySQL 库，`c_ai_prompt` 为公共配置表。

---

## File Structure

### 后端（admin-api / user-api / shared）

```
project/shared/src/main/java/com/aichuangzuo/shared/
├── entity/AiPrompt.java
├── utils/AiPromptVariableResolver.java
└── vo/AiPromptRendered.java

project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/
├── controller/AiPromptController.java
├── service/AiPromptService.java
├── service/impl/AiPromptServiceImpl.java
├── service/AiPromptRenderService.java
├── service/impl/AiPromptRenderServiceImpl.java
├── entity/  (使用 shared/entity/AiPrompt.java)
├── mapper/AiPromptMapper.java
├── dto/request/AiPromptCreateRequest.java
├── dto/request/AiPromptUpdateRequest.java
├── dto/request/AiPromptQueryRequest.java
├── vo/AiPromptVO.java
└── vo/AiPromptDetailVO.java

project/user/api/src/main/java/com/aichuangzuo/user/modules/aiprompt/
├── service/AiPromptRenderService.java
├── service/impl/AiPromptRenderServiceImpl.java
├── mapper/AiPromptMapper.java
└── entity/  (使用 shared/entity/AiPrompt.java)

project/admin/api/src/test/java/com/aichuangzuo/shared/utils/AiPromptVariableResolverTest.java
```

### 前端（admin-web）

```
project/admin/web/src/
├── api/aiPrompt.js
├── composables/useAiPrompt.js
├── views/AiPromptListView.vue
├── views/AiPromptEditView.vue
├── router/index.js  (修改)
└── layouts/AdminLayout.vue  (修改)
```

### 数据库

```
project/admin/api/src/main/resources/db/migration/V2.0.0_087__create_ai_prompt_table.sql
```

---

### Task 1: Flyway 迁移 - 创建 `c_ai_prompt` 表并初始化 3 条默认提示词

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_087__create_ai_prompt_table.sql`

**Interfaces:**
- Produces: 数据库表 `c_ai_prompt` 及初始数据，供后续实体与 Mapper 使用。

- [ ] **Step 1: 编写建表 + 种子 SQL**

```sql
-- AI 提示词配置表：管理后台统一维护，用户端/管理端运行时读取
CREATE TABLE IF NOT EXISTS c_ai_prompt (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    prompt_code VARCHAR(64) NOT NULL COMMENT '唯一编码',
    prompt_name VARCHAR(128) NOT NULL COMMENT '显示名称',
    module VARCHAR(32) NOT NULL COMMENT '归属端：admin / user',
    category VARCHAR(64) DEFAULT NULL COMMENT '业务分类',
    system_role MEDIUMTEXT COMMENT '系统角色 / AI 身份设定',
    user_prompt MEDIUMTEXT NOT NULL COMMENT '用户提示词主体',
    variable_schema JSON DEFAULT NULL COMMENT '变量元数据：[{name, required, description, example}]',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_c_ai_prompt_code (prompt_code),
    KEY idx_module_category (module, category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 提示词配置表';

-- 管理端：爆款标题/选题生成
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'topic_title_v1',
    '爆款标题/选题生成',
    'admin',
    'topic_title',
    '你是自媒体爆款标题策划。',
    '请生成 {{count}} 条自媒体选题标题，每条包含标题和描述（写作指引）。\n\n生成方向：{{direction}}\n\n支持平台及规则约束（标题与描述必须同时满足）：\n- 微信公众号：禁止诱导分享/关注/转发、低俗、谣言、侵权、虚假宣传、标题党。\n- 小红书：禁止夸张营销、诱导点赞收藏、虚假体验、违禁词、过度美化/对比。\n- 今日头条：禁止标题党、低俗、谣言、侵权、广告法违禁词、无资质医疗/财经建议。\n- 知乎：禁止诱导关注、编故事、不友善、低质营销、无来源事实断言。\n- 百家号：禁止标题党、低俗、抄袭、广告法违禁词、虚假权威背书。\n- 抖音图文：禁止诱导互动（如“双击 666”）、低俗、虚假内容、侵权、未成年人不良引导。\n通用禁区：严禁使用“最”“第一”“绝对”“国家级”等无法证实的极限词；严禁制造焦虑、歧视、攻击、泄露隐私；严禁承诺收益、疗效等无法验证的结果。\n\n标题多样性要求（避免同质化）：\n- 每条标题必须从不同角度切入，避免同义反复或只换关键词。\n- 句式要交错使用：问题型、反差型、场景型、观点型、方法型、故事型、数据型等。\n- 情绪表达要有差异，避免连续使用“震惊”“绝了”“后悔没早点”等同一套爆款模板。\n- 同一生成批次中，任意两条标题的开头 5 个字不能完全相同。\n\n描述要求（必须是写作指引，不是简单总结）：\n- 说明这篇文章大致怎么写，给出 2-5 个核心观点或写作要点。\n- 格式示例：围绕以下观点创作，1、xxx；2、xxxxx；3、xxxx。\n- 每个要点要指出：本部分论证什么、从什么角度展开、给读者带来什么价值。\n- 不要只写“介绍方法”“分析原因”这类空泛说明。\n\n格式要求：\n- 标题 ≤30 字，描述 ≤300 字。\n- 标题和描述中如需引用词语，一律使用中文双引号“”，不要使用单引号。\n\n输出 JSON 结构：\n{"titles": [{"title": "标题文字", "summary": "围绕以下观点创作，1、...；2、...；3、..."}]}\n\n最终输出要求（覆盖以上所有说明，必须严格遵守）：\n1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。\n2. 不要用 ```json 或任何代码围栏包裹。\n3. 第一个字符必须是 {，最后一个字符必须是 }。\n4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。',
    '[{"name":"count","required":true,"description":"生成数量","example":"10"},{"name":"direction","required":true,"description":"生成方向","example":"职场、情感、生活、AI 等热门自媒体赛道"}]',
    1,
    0,
    '管理端 AI 批量生成选题标题'
);

-- 用户端：标题优化
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'title_optimize_v1',
    '标题优化',
    'user',
    'title_optimize',
    '你是一位资深新媒体标题策划专家，深谙各内容平台的推荐机制与用户点击心理。你只输出合法 JSON。',
    '请根据文章标题和正文，为 7 个平台分别拟定 2 条优化标题。\n\n【文章标题】\n{{title}}\n\n【文章正文】\n{{bodyExcerpt}}\n\n【平台与风格要求】\n- wechat（公众号）：引发共鸣或好奇，可带数字/悬念，避免标题党词汇堆砌，30 字以内。\n- xiaohongshu（小红书）：口语化、带 emoji，突出获得感或身份代入，20 字以内。\n- toutiao（今日头条）：信息量大、冲击力强，可适度悬念，30 字以内。\n- baijiahao（百家号）：正式稳重、突出价值点与专业性，30 字以内。\n- zhihu（知乎）：以问句或深度观点句呈现，强调逻辑与干货，35 字以内。\n- douyin（抖音图文）：短平快、情绪强、钩子前置，20 字以内。\n- bilibili（B站专栏）：年轻化、有梗但不低俗，突出兴趣点，30 字以内。\n\n【硬性要求】\n1. 每个平台恰好 2 条标题，风格不可雷同：一条偏痛点/利益驱动，一条偏好奇/情绪驱动。\n2. 标题必须忠于正文内容，不得虚构正文不存在的事实、数据或承诺。\n3. 不得使用“震惊”“不看后悔”等低俗标题党词汇。\n4. 输出 JSON 结构：{"titles":{"wechat":["...","..."],"xiaohongshu":["...","..."],"toutiao":["...","..."],"baijiahao":["...","..."],"zhihu":["...","..."],"douyin":["...","..."],"bilibili":["...","..."]}}\n\n最终输出要求（覆盖以上所有说明，必须严格遵守）：\n  1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。\n  2. 不要用 ```json 或任何代码围栏包裹。\n  3. 第一个字符必须是 {，最后一个字符必须是 }。\n  4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。',
    '[{"name":"title","required":true,"description":"文章标题","example":"原文标题"},{"name":"bodyExcerpt","required":true,"description":"正文摘要","example":"文章正文前 1500 字"}]',
    1,
    0,
    '用户端 AI 标题优化'
);

-- 用户端：文风分析
INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'skill_analyze_v1',
    '文风分析',
    'user',
    'skill_analyze',
    '你是一位资深的中文文体分析师，擅长拆解中文自媒体文章的写作风格，并把风格特征提炼成可直接指导 AI 写作的提示词。',
    '请分析以下参考文章的写作风格，完成两件事：\n\n【文章正文】\n{{text}}\n\n【任务】\n1. 从【语气】【词汇】【句式】【结构】四个维度拆解风格特征。每条特征必须具体、可模仿，禁止空泛形容（不要写「语言优美」，要写「多用15字以内短句，段间留白多」这类可执行描述）。\n2. 从原文中逐字摘录 2 个最能代表该风格的片段。\n\n【输出 JSON 结构】\n{"excerpt1":"原文中最能代表风格的连续片段，不超过120字，必须逐字摘自原文","excerpt2":"另一个代表性片段，不超过80字，必须逐字摘自原文，且不与excerpt1重复","description":"用一句话描述这个提示词适合写什么、风格是什么，不超过100字，不要出现英文双引号","prompt":"不超过1200字的风格提示词"}\n\n其中 prompt 字段严格使用以下模板：\n你是一位中文写手，请模仿以下参考文章的写作风格：\n\n【语气】（人称视角、情感温度、与读者的距离感，1-2句）\n【词汇】（书面/口语倾向、网络用语与语气词的使用习惯，1-2句）\n【句式】（句子长短与节奏、标点习惯、常用修辞，1-2句）\n【结构】（开头方式、段落组织、结尾处理，1-2句）\n\n请在生成新内容时严格遵循以上风格特征。\n\n最终输出要求（覆盖以上所有说明，必须严格遵守）：\n  1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。\n  2. 不要用 ```json 或任何代码围栏包裹。\n  3. 第一个字符必须是 {，最后一个字符必须是 }。\n  4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。\n  5. prompt 字段中若需引用示例词语，必须使用中文直角引号「」，严禁使用英文双引号 "，避免破坏 JSON 格式。',
    '[{"name":"text","required":true,"description":"参考文章正文","example":"不超过1000字的参考文章"}]',
    1,
    0,
    '用户端 AI 文风分析'
);
```

- [ ] **Step 2: 本地启动 admin-api 验证迁移执行**

Run: `cd project/admin/api && mvn spring-boot:run` 或 `./mvnw flyway:info`
Expected: `V2.0.0_086__create_ai_prompt_table.sql` 成功执行，`c_ai_prompt` 表存在且含 3 条记录。

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_087__create_ai_prompt_table.sql
git commit -m "feat(db): create c_ai_prompt table and seed default prompts"
```

---

### Task 2: 共享变量解析器 `AiPromptVariableResolver` + 单元测试

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/utils/AiPromptVariableResolver.java`
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/vo/AiPromptRendered.java`
- Create: `project/admin/api/src/test/java/com/aichuangzuo/shared/utils/AiPromptVariableResolverTest.java`

**Interfaces:**
- Produces:
  - `AiPromptVariableResolver.extractVariables(String)` → `Set<String>`
  - `AiPromptVariableResolver.render(String, Map<String, Object>)` → `String`
  - `AiPromptRendered record(String systemRole, String userPrompt)`

- [ ] **Step 1: 编写失败的单元测试**

```java
package com.aichuangzuo.shared.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AiPromptVariableResolverTest {

    @Test
    void shouldExtractVariables() {
        String text = "请生成 {{count}} 条标题，方向：{{direction}}。";
        Set<String> vars = AiPromptVariableResolver.extractVariables(text);
        assertEquals(Set.of("count", "direction"), vars);
    }

    @Test
    void shouldRenderVariables() {
        String template = "请生成 {{count}} 条标题，方向：{{direction}}。";
        String result = AiPromptVariableResolver.render(template, Map.of("count", 10, "direction", "职场"));
        assertEquals("请生成 10 条标题，方向：职场。", result);
    }

    @Test
    void shouldRenderMissingVariableAsEmptyString() {
        String template = "标题：{{title}}，备注：{{remark}}。";
        String result = AiPromptVariableResolver.render(template, Map.of("title", "A"));
        assertEquals("标题：A，备注：。", result);
    }
}
```

Run: `cd project/admin/api && mvn test -Dtest=AiPromptVariableResolverTest`
Expected: 编译失败，`AiPromptVariableResolver` 不存在。

- [ ] **Step 2: 实现解析器**

```java
package com.aichuangzuo.shared.utils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 提示词变量解析器：统一处理 {{variableName}} 占位符。
 */
public final class AiPromptVariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private AiPromptVariableResolver() {
    }

    /**
     * 从文本中提取所有变量名（去重、保持出现顺序）。
     */
    public static Set<String> extractVariables(String text) {
        Set<String> variables = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return variables;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

    /**
     * 替换模板中的变量。缺失的变量替换为空字符串。
     */
    public static String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null) {
            variables = Map.of();
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables.get(name);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
```

```java
package com.aichuangzuo.shared.vo;

/**
 * 渲染后的 AI 提示词。
 */
public record AiPromptRendered(String systemRole, String userPrompt) {
}
```

- [ ] **Step 3: 运行测试**

Run: `cd project/admin/api && mvn test -Dtest=AiPromptVariableResolverTest`
Expected: 3 个测试全部通过。

- [ ] **Step 4: Commit**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/utils/AiPromptVariableResolver.java project/shared/src/main/java/com/aichuangzuo/shared/vo/AiPromptRendered.java project/admin/api/src/test/java/com/aichuangzuo/shared/utils/AiPromptVariableResolverTest.java
git commit -m "feat(shared): add AI prompt variable resolver and rendered value object"
```

---

### Task 3: 新增错误码枚举

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminAiPromptErrorCode.java`

**Interfaces:**
- Produces: `AdminAiPromptErrorCode` 供 admin 渲染服务和管理接口使用。

- [ ] **Step 1: 创建错误码枚举**

```java
package com.aichuangzuo.shared.enums.error;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 管理端 AI 提示词配置错误码（2401xx）。
 */
@Getter
public enum AdminAiPromptErrorCode implements ErrorCode {

    AI_PROMPT_NOT_FOUND(240101, "提示词配置不存在"),
    AI_PROMPT_DISABLED(240102, "提示词配置已停用"),
    AI_PROMPT_VARIABLE_MISSING(240103, "提示词必填变量缺失"),
    AI_PROMPT_CODE_EXISTS(240104, "提示词编码已存在"),
    AI_PROMPT_RENDER_ERROR(240105, "提示词渲染异常");

    private final int code;
    private final String message;

    AdminAiPromptErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd project/shared && mvn compile`
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminAiPromptErrorCode.java
git commit -m "feat(shared): add admin ai prompt error codes"
```

---

### Task 4: 共享实体 `AiPrompt`

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/entity/AiPrompt.java`

**Interfaces:**
- Produces: `AiPrompt` 实体类，供 admin-api 和 user-api 的 Mapper 共用。

- [ ] **Step 1: 创建实体**

```java
package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 提示词配置实体，对应表 {@code c_ai_prompt}。
 *
 * <p>管理端维护，用户端/管理端运行时读取。</p>
 */
@Getter
@Setter
@TableName("c_ai_prompt")
public class AiPrompt extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String promptCode;
    private String promptName;
    private String module;
    private String category;
    private String systemRole;
    private String userPrompt;
    private String variableSchema;
    private Integer status;
    private Integer sortOrder;
    private String description;
    private Long tenantId;
}
```

- [ ] **Step 2: 编译验证**

Run: `cd project/shared && mvn compile`
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/entity/AiPrompt.java
git commit -m "feat(shared): add AiPrompt entity for c_ai_prompt"
```

---

### Task 5: Admin 后端 CRUD（Controller / Service / Mapper / DTO / VO）

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/mapper/AiPromptMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/service/AiPromptService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/service/impl/AiPromptServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/controller/AiPromptController.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/dto/request/AiPromptCreateRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/dto/request/AiPromptUpdateRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/dto/request/AiPromptQueryRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/vo/AiPromptVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/vo/AiPromptDetailVO.java`

**Interfaces:**
- Consumes: `AiPrompt` 实体（Task 4）、`AdminAiPromptErrorCode`（Task 3）。
- Produces:
  - `AiPromptService.list(AiPromptQueryRequest)` → `PageResult`
  - `AiPromptService.get(Long)` → `AiPromptDetailVO`
  - `AiPromptService.create(AiPromptCreateRequest)` → `Long`
  - `AiPromptService.update(Long, AiPromptUpdateRequest)` → `void`
  - `AiPromptService.delete(Long)` → `void`
  - `AiPromptService.enable(Long)` / `disable(Long)` → `void`

- [ ] **Step 1: 创建 DTO 与 VO**

```java
// project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/dto/request/AiPromptCreateRequest.java
package com.aichuangzuo.admin.modules.aiprompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiPromptCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String promptCode;

    @NotBlank
    @Size(max = 128)
    private String promptName;

    @NotBlank
    private String module;

    @Size(max = 64)
    private String category;

    private String systemRole;

    @NotBlank
    private String userPrompt;

    private List<AiPromptVariableRequest> variableSchema;

    @NotNull
    private Integer status;

    private Integer sortOrder;

    @Size(max = 500)
    private String description;

    @Data
    public static class AiPromptVariableRequest {
        private String name;
        private Boolean required;
        private String description;
        private String example;
    }
}
```

```java
// project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/dto/request/AiPromptUpdateRequest.java
package com.aichuangzuo.admin.modules.aiprompt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiPromptUpdateRequest {

    @NotBlank
    @Size(max = 128)
    private String promptName;

    @NotBlank
    private String module;

    @Size(max = 64)
    private String category;

    private String systemRole;

    @NotBlank
    private String userPrompt;

    private List<AiPromptCreateRequest.AiPromptVariableRequest> variableSchema;

    @NotNull
    private Integer status;

    private Integer sortOrder;

    @Size(max = 500)
    private String description;
}
```

```java
// project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/dto/request/AiPromptQueryRequest.java
package com.aichuangzuo.admin.modules.aiprompt.dto.request;

import lombok.Data;

@Data
public class AiPromptQueryRequest {

    private String module;
    private String category;
    private Integer status;
    private String keyword;
    private Long page = 1L;
    private Long pageSize = 20L;
}
```

```java
// project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/vo/AiPromptVO.java
package com.aichuangzuo.admin.modules.aiprompt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiPromptVO {

    private Long id;
    private String promptCode;
    private String promptName;
    private String module;
    private String category;
    private Integer status;
    private LocalDateTime updatedAt;
}
```

```java
// project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/vo/AiPromptDetailVO.java
package com.aichuangzuo.admin.modules.aiprompt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiPromptDetailVO {

    private Long id;
    private String promptCode;
    private String promptName;
    private String module;
    private String category;
    private String systemRole;
    private String userPrompt;
    private List<AiPromptVariableVO> variableSchema;
    private Integer status;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class AiPromptVariableVO {
        private String name;
        private Boolean required;
        private String description;
        private String example;
    }
}
```

- [ ] **Step 2: 创建 Mapper**

```java
package com.aichuangzuo.admin.modules.aiprompt.mapper;

import com.aichuangzuo.shared.entity.AiPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiPromptMapper extends BaseMapper<AiPrompt> {

    /**
     * 按编码查询未删除记录（不校验状态，由业务层处理）。
     */
    @Select("SELECT * FROM c_ai_prompt WHERE prompt_code = #{code} AND is_deleted = 0 LIMIT 1")
    AiPrompt selectByCode(@Param("code") String code);
}
```

- [ ] **Step 3: 创建 Service 接口与实现**

```java
package com.aichuangzuo.admin.modules.aiprompt.service;

import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptCreateRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptQueryRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptUpdateRequest;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptDetailVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptVO;

import java.util.List;

public interface AiPromptService {

    PageResult list(AiPromptQueryRequest request);
    AiPromptDetailVO get(Long id);
    Long create(AiPromptCreateRequest request);
    void update(Long id, AiPromptUpdateRequest request);
    void delete(Long id);
    void enable(Long id);
    void disable(Long id);

    record PageResult(List<AiPromptVO> list, long total, long page, long pageSize) {}
}
```

```java
package com.aichuangzuo.admin.modules.aiprompt.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptCreateRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptQueryRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptUpdateRequest;
import com.aichuangzuo.admin.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptService;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptDetailVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptVO;
import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AiPromptVariableResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptServiceImpl implements AiPromptService {

    private final AiPromptMapper aiPromptMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult list(AiPromptQueryRequest request) {
        long page = Math.max(1, request.getPage() == null ? 1 : request.getPage());
        long pageSize = Math.min(Math.max(1, request.getPageSize() == null ? 20 : request.getPageSize()), 100);
        String keyword = StringUtils.trimToEmpty(request.getKeyword());

        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPrompt::getIsDeleted, 0);
        wrapper.eq(StringUtils.isNotBlank(request.getModule()), AiPrompt::getModule, request.getModule());
        wrapper.eq(StringUtils.isNotBlank(request.getCategory()), AiPrompt::getCategory, request.getCategory());
        wrapper.eq(request.getStatus() != null, AiPrompt::getStatus, request.getStatus());
        wrapper.and(StringUtils.isNotBlank(keyword), w -> w
                .like(AiPrompt::getPromptCode, keyword)
                .or()
                .like(AiPrompt::getPromptName, keyword));
        wrapper.orderByDesc(AiPrompt::getUpdatedAt);

        Page<AiPrompt> pageParam = new Page<>(page, pageSize);
        Page<AiPrompt> result = aiPromptMapper.selectPage(pageParam, wrapper);

        return new PageResult(
                result.getRecords().stream().map(this::toListVo).toList(),
                result.getTotal(),
                page,
                pageSize
        );
    }

    @Override
    public AiPromptDetailVO get(Long id) {
        AiPrompt entity = requireById(id);
        return toDetailVo(entity);
    }

    @Override
    public Long create(AiPromptCreateRequest request) {
        checkCodeUnique(request.getPromptCode(), null);

        AiPrompt entity = new AiPrompt();
        entity.setPromptCode(request.getPromptCode().trim());
        entity.setPromptName(request.getPromptName().trim());
        entity.setModule(request.getModule().trim());
        entity.setCategory(StringUtils.trimToNull(request.getCategory()));
        entity.setSystemRole(StringUtils.trimToNull(request.getSystemRole()));
        entity.setUserPrompt(request.getUserPrompt().trim());
        entity.setVariableSchema(toJson(request.getVariableSchema()));
        entity.setStatus(request.getStatus());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setDescription(StringUtils.trimToNull(request.getDescription()));
        entity.setTenantId(0L);
        entity.setIsDeleted(0);
        entity.setCreatedBy(currentAdminIdOrZero());
        entity.setUpdatedBy(entity.getCreatedBy());

        aiPromptMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, AiPromptUpdateRequest request) {
        AiPrompt entity = requireById(id);

        entity.setPromptName(request.getPromptName().trim());
        entity.setModule(request.getModule().trim());
        entity.setCategory(StringUtils.trimToNull(request.getCategory()));
        entity.setSystemRole(StringUtils.trimToNull(request.getSystemRole()));
        entity.setUserPrompt(request.getUserPrompt().trim());
        entity.setVariableSchema(toJson(request.getVariableSchema()));
        entity.setStatus(request.getStatus());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setDescription(StringUtils.trimToNull(request.getDescription()));
        entity.setUpdatedBy(currentAdminIdOrZero());

        aiPromptMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        AiPrompt entity = requireById(id);
        entity.setIsDeleted(1);
        entity.setUpdatedBy(currentAdminIdOrZero());
        aiPromptMapper.updateById(entity);
    }

    @Override
    public void enable(Long id) {
        updateStatus(id, 1);
    }

    @Override
    public void disable(Long id) {
        updateStatus(id, 0);
    }

    private void updateStatus(Long id, int status) {
        AiPrompt entity = requireById(id);
        entity.setStatus(status);
        entity.setUpdatedBy(currentAdminIdOrZero());
        aiPromptMapper.updateById(entity);
    }

    private AiPrompt requireById(Long id) {
        AiPrompt entity = aiPromptMapper.selectById(id);
        if (entity == null || entity.getIsDeleted() != null && entity.getIsDeleted() == 1) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_NOT_FOUND);
        }
        return entity;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPrompt::getPromptCode, code.trim());
        wrapper.eq(AiPrompt::getIsDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(AiPrompt::getId, excludeId);
        }
        if (aiPromptMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_CODE_EXISTS);
        }
    }

    private AiPromptVO toListVo(AiPrompt entity) {
        AiPromptVO vo = new AiPromptVO();
        vo.setId(entity.getId());
        vo.setPromptCode(entity.getPromptCode());
        vo.setPromptName(entity.getPromptName());
        vo.setModule(entity.getModule());
        vo.setCategory(entity.getCategory());
        vo.setStatus(entity.getStatus());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AiPromptDetailVO toDetailVo(AiPrompt entity) {
        AiPromptDetailVO vo = new AiPromptDetailVO();
        vo.setId(entity.getId());
        vo.setPromptCode(entity.getPromptCode());
        vo.setPromptName(entity.getPromptName());
        vo.setModule(entity.getModule());
        vo.setCategory(entity.getCategory());
        vo.setSystemRole(entity.getSystemRole());
        vo.setUserPrompt(entity.getUserPrompt());
        vo.setVariableSchema(parseVariableSchema(entity.getVariableSchema()));
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    @SneakyThrows
    private String toJson(List<AiPromptCreateRequest.AiPromptVariableRequest> variables) {
        if (variables == null) {
            return null;
        }
        return objectMapper.writeValueAsString(variables);
    }

    @SneakyThrows
    private List<AiPromptDetailVO.AiPromptVariableVO> parseVariableSchema(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private Long currentAdminIdOrZero() {
        Long id = SecurityAdminContext.getCurrentAdminUserId();
        return id == null ? 0L : id;
    }
}
```

- [ ] **Step 4: 创建 Controller**

```java
package com.aichuangzuo.admin.modules.aiprompt.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptCreateRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptQueryRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptUpdateRequest;
import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptService;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptDetailVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptVO;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端 AI 提示词管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai-prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService aiPromptService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询提示词列表")
    @GetMapping
    public Result<AiPromptService.PageResult> list(@ModelAttribute AiPromptQueryRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询 AI 提示词列表, adminUserId={}", adminUserId);
        return Result.success(aiPromptService.list(request));
    }

    @Operation(summary = "查看提示词详情")
    @GetMapping("/{id}")
    public Result<AiPromptDetailVO> get(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看 AI 提示词详情, adminUserId={}, id={}", adminUserId, id);
        return Result.success(aiPromptService.get(id));
    }

    @Operation(summary = "新增提示词")
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody AiPromptCreateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员新增 AI 提示词, adminUserId={}, promptCode={}", adminUserId, request.getPromptCode());
        Long id = aiPromptService.create(request);
        return Result.success(Map.of("id", id));
    }

    @Operation(summary = "编辑提示词")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody AiPromptUpdateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员编辑 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除提示词")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用提示词")
    @PostMapping("/{id}/actions/enable")
    public Result<Void> enable(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员启用 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.enable(id);
        return Result.success();
    }

    @Operation(summary = "停用提示词")
    @PostMapping("/{id}/actions/disable")
    public Result<Void> disable(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员停用 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.disable(id);
        return Result.success();
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
```

- [ ] **Step 5: 编译并启动 admin-api 验证接口**

Run: `cd project/admin/api && mvn compile`
Expected: 编译成功。

Run: `cd project/admin/api && mvn spring-boot:run`
Expected: 启动成功，无 Flyway checksum 错误。

Test via Knife4j: `http://localhost:8080/admin/doc.html`（或对应端口），验证 `GET /api/v1/admin/ai-prompts` 返回 3 条默认数据。

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/
git commit -m "feat(admin): add AI prompt CRUD API"
```

---

### Task 6: Admin 渲染服务 `AiPromptRenderService`

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/service/AiPromptRenderService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/service/impl/AiPromptRenderServiceImpl.java`

**Interfaces:**
- Consumes: `AiPromptMapper`、shared `AiPromptVariableResolver`、`AdminAiPromptErrorCode`。
- Produces: `AiPromptRendered render(String promptCode, Map<String, Object> variables)`。

- [ ] **Step 1: 创建渲染 Service 接口与实现**

```java
package com.aichuangzuo.admin.modules.aiprompt.service;

import com.aichuangzuo.shared.vo.AiPromptRendered;

import java.util.Map;

public interface AiPromptRenderService {

    AiPromptRendered render(String promptCode, Map<String, Object> variables);
}
```

```java
package com.aichuangzuo.admin.modules.aiprompt.service.impl;

import com.aichuangzuo.admin.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AiPromptVariableResolver;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptRenderServiceImpl implements AiPromptRenderService {

    private final AiPromptMapper aiPromptMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiPromptRendered render(String promptCode, Map<String, Object> variables) {
        if (StringUtils.isBlank(promptCode)) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_RENDER_ERROR);
        }

        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPrompt::getPromptCode, promptCode.trim());
        wrapper.eq(AiPrompt::getStatus, 1);
        wrapper.eq(AiPrompt::getIsDeleted, 0);
        AiPrompt prompt = aiPromptMapper.selectOne(wrapper);

        if (prompt == null) {
            log.warn("AI 提示词不存在或已停用, promptCode={}", promptCode);
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_NOT_FOUND);
        }

        return renderInternal(prompt, variables == null ? Map.of() : variables);
    }

    private AiPromptRendered renderInternal(AiPrompt prompt, Map<String, Object> variables) {
        Set<String> requiredVariables = parseRequiredVariables(prompt.getVariableSchema());
        Set<String> missing = new LinkedHashSet<>();
        for (String name : requiredVariables) {
            Object value = variables.get(name);
            if (value == null || StringUtils.isBlank(value.toString())) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            log.warn("AI 提示词必填变量缺失, promptCode={}, missing={}", prompt.getPromptCode(), missing);
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_VARIABLE_MISSING);
        }

        String systemRole = AiPromptVariableResolver.render(prompt.getSystemRole(), variables);
        String userPrompt = AiPromptVariableResolver.render(prompt.getUserPrompt(), variables);
        return new AiPromptRendered(systemRole, userPrompt);
    }

    @SneakyThrows
    private Set<String> parseRequiredVariables(String json) {
        if (StringUtils.isBlank(json)) {
            return Set.of();
        }
        List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
        Set<String> required = new LinkedHashSet<>();
        for (Map<String, Object> item : list) {
            Boolean requiredFlag = (Boolean) item.get("required");
            if (Boolean.TRUE.equals(requiredFlag)) {
                String name = (String) item.get("name");
                if (StringUtils.isNotBlank(name)) {
                    required.add(name);
                }
            }
        }
        return required;
    }
}
```

- [ ] **Step 2: 编写渲染服务单元测试**

Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/aiprompt/service/AiPromptRenderServiceTest.java`

```java
package com.aichuangzuo.admin.modules.aiprompt.service;

import com.aichuangzuo.admin.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.admin.modules.aiprompt.service.impl.AiPromptRenderServiceImpl;
import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiPromptRenderServiceTest {

    private final AiPromptMapper mapper = mock(AiPromptMapper.class);
    private final AiPromptRenderService service = new AiPromptRenderServiceImpl(mapper, new ObjectMapper());

    @Test
    void shouldRenderPrompt() {
        AiPrompt prompt = new AiPrompt();
        prompt.setPromptCode("test_v1");
        prompt.setStatus(1);
        prompt.setIsDeleted(0);
        prompt.setSystemRole("你是 {{role}}。");
        prompt.setUserPrompt("请生成 {{count}} 条。");
        prompt.setVariableSchema("[{\"name\":\"role\",\"required\":true},{\"name\":\"count\",\"required\":true}]");

        when(mapper.selectOne(any())).thenReturn(prompt);

        AiPromptRendered rendered = service.render("test_v1", Map.of("role", "专家", "count", 5));

        assertEquals("你是 专家。", rendered.systemRole());
        assertEquals("请生成 5 条。", rendered.userPrompt());
    }

    @Test
    void shouldThrowWhenRequiredVariableMissing() {
        AiPrompt prompt = new AiPrompt();
        prompt.setPromptCode("test_v1");
        prompt.setStatus(1);
        prompt.setIsDeleted(0);
        prompt.setUserPrompt("请生成 {{count}} 条。");
        prompt.setVariableSchema("[{\"name\":\"count\",\"required\":true}]");

        when(mapper.selectOne(any())).thenReturn(prompt);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.render("test_v1", Map.of()));
        assertEquals(AdminAiPromptErrorCode.AI_PROMPT_VARIABLE_MISSING.getCode(), ex.getErrorCode().getCode());
    }
}
```

Run: `cd project/admin/api && mvn test -Dtest=AiPromptRenderServiceTest`
Expected: 测试通过。

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/service/AiPromptRenderService.java project/admin/api/src/main/java/com/aichuangzuo/admin/modules/aiprompt/service/impl/AiPromptRenderServiceImpl.java project/admin/api/src/test/java/com/aichuangzuo/admin/modules/aiprompt/service/AiPromptRenderServiceTest.java
git commit -m "feat(admin): add AI prompt render service"
```

---

### Task 7: User 端渲染服务 `AiPromptRenderService`

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/aiprompt/mapper/AiPromptMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/aiprompt/service/AiPromptRenderService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/aiprompt/service/impl/AiPromptRenderServiceImpl.java`

**Interfaces:**
- Consumes: shared `AiPromptVariableResolver`、`AdminAiPromptErrorCode`。
- Produces: `AiPromptRendered render(String promptCode, Map<String, Object> variables)`。

- [ ] **Step 1: 创建 User 端 Mapper**

```java
package com.aichuangzuo.user.modules.aiprompt.mapper;

import com.aichuangzuo.shared.entity.AiPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiPromptMapper extends BaseMapper<AiPrompt> {

    @Select("SELECT * FROM c_ai_prompt WHERE prompt_code = #{code} AND status = 1 AND is_deleted = 0 LIMIT 1")
    AiPrompt selectActiveByCode(@Param("code") String code);
}
```

- [ ] **Step 2: 创建 User 端渲染 Service（与 admin 端逻辑一致，但使用 user 端 Mapper）**

```java
package com.aichuangzuo.user.modules.aiprompt.service;

import com.aichuangzuo.shared.vo.AiPromptRendered;

import java.util.Map;

public interface AiPromptRenderService {

    AiPromptRendered render(String promptCode, Map<String, Object> variables);
}
```

```java
package com.aichuangzuo.user.modules.aiprompt.service.impl;

import com.aichuangzuo.shared.entity.AiPrompt;
import com.aichuangzuo.shared.enums.error.AdminAiPromptErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AiPromptVariableResolver;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.mapper.AiPromptMapper;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptRenderServiceImpl implements AiPromptRenderService {

    private final AiPromptMapper aiPromptMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiPromptRendered render(String promptCode, Map<String, Object> variables) {
        if (StringUtils.isBlank(promptCode)) {
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_RENDER_ERROR);
        }
        AiPrompt prompt = aiPromptMapper.selectActiveByCode(promptCode.trim());
        if (prompt == null) {
            log.warn("AI 提示词不存在或已停用, promptCode={}", promptCode);
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_NOT_FOUND);
        }
        return renderInternal(prompt, variables == null ? Map.of() : variables);
    }

    private AiPromptRendered renderInternal(AiPrompt prompt, Map<String, Object> variables) {
        Set<String> requiredVariables = parseRequiredVariables(prompt.getVariableSchema());
        Set<String> missing = new LinkedHashSet<>();
        for (String name : requiredVariables) {
            Object value = variables.get(name);
            if (value == null || StringUtils.isBlank(value.toString())) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            log.warn("AI 提示词必填变量缺失, promptCode={}, missing={}", prompt.getPromptCode(), missing);
            throw new BusinessException(AdminAiPromptErrorCode.AI_PROMPT_VARIABLE_MISSING);
        }

        String systemRole = AiPromptVariableResolver.render(prompt.getSystemRole(), variables);
        String userPrompt = AiPromptVariableResolver.render(prompt.getUserPrompt(), variables);
        return new AiPromptRendered(systemRole, userPrompt);
    }

    @SneakyThrows
    private Set<String> parseRequiredVariables(String json) {
        if (StringUtils.isBlank(json)) {
            return Set.of();
        }
        List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
        Set<String> required = new LinkedHashSet<>();
        for (Map<String, Object> item : list) {
            Boolean requiredFlag = (Boolean) item.get("required");
            if (Boolean.TRUE.equals(requiredFlag)) {
                String name = (String) item.get("name");
                if (StringUtils.isNotBlank(name)) {
                    required.add(name);
                }
            }
        }
        return required;
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd project/user/api && mvn compile`
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/aiprompt/
git commit -m "feat(user): add AI prompt render service"
```

---

### Task 8: 重构 `TopicTitleService` 使用提示词配置

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/topictitle/service/TopicTitleService.java`
- Modify: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/topictitle/service/TopicTitleServiceTest.java`（更新测试以兼容新依赖）

**Interfaces:**
- Consumes: `AiPromptRenderService.render("topic_title_v1", Map.of("count", count, "direction", direction))`。
- Produces: 原有 `runGeneration` 行为不变，但 prompt 从配置读取。

- [ ] **Step 1: 注入渲染服务并替换 prompt 构建**

在 `TopicTitleService` 中：

1. 添加依赖：

```java
import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.shared.vo.AiPromptRendered;
```

2. 添加字段：

```java
private final AiPromptRenderService aiPromptRenderService;
```

3. 删除常量：

```java
// 删除以下常量
// private static final String SYSTEM_MESSAGE = "你是自媒体爆款标题策划。";
// private static final String STRICT_OUTPUT_RULES = "...";
```

4. 删除 `buildUserMessage` 方法。

5. 修改 `runGeneration` 方法：

```java
// 原调用
AiCallResult result = generationAiService.call(cfg.getId(), SYSTEM_MESSAGE,
        buildUserMessage(count, direction), null);

// 替换为
String directionText = (direction == null || direction.isBlank())
        ? "不限，覆盖职场、情感、生活、AI 等热门自媒体赛道" : direction.trim();
AiPromptRendered prompt = aiPromptRenderService.render("topic_title_v1",
        Map.of("count", count, "direction", directionText));
AiCallResult result = generationAiService.call(cfg.getId(), prompt.systemRole(),
        prompt.userPrompt(), null);
```

- [ ] **Step 2: 更新单元测试**

如果 `TopicTitleServiceTest` 中 mock 了 `SYSTEM_MESSAGE` 或 `buildUserMessage`，需要改为 mock `AiPromptRenderService`。读取测试文件后按需调整：注入 `AiPromptRenderService` mock，并 stub `render("topic_title_v1", ...)` 返回固定的 systemRole + userPrompt。

Run: `cd project/admin/api && mvn test -Dtest=TopicTitleServiceTest`
Expected: 测试通过。

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/topictitle/service/TopicTitleService.java project/admin/api/src/test/java/com/aichuangzuo/admin/modules/topictitle/service/TopicTitleServiceTest.java
git commit -m "refactor(admin): TopicTitleService use ai prompt config"
```

---

### Task 9: 重构 `TitleOptimizeServiceImpl` 使用提示词配置

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/impl/TitleOptimizeServiceImpl.java`

**Interfaces:**
- Consumes: `AiPromptRenderService.render("title_optimize_v1", Map.of("title", article.getTitle(), "bodyExcerpt", excerpt(article.getBody())))`。
- Produces: 原有 `optimize` 行为不变。

- [ ] **Step 1: 注入渲染服务并替换 prompt**

在 `TitleOptimizeServiceImpl` 中：

1. 添加依赖：

```java
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
```

2. 删除常量：

```java
// 删除以下常量
// private static final String SYSTEM_MESSAGE = "...";
// private static final String USER_PROMPT_TEMPLATE = "...";
```

3. 添加字段：

```java
private final AiPromptRenderService aiPromptRenderService;
```

4. 修改 `optimize` 方法中的调用：

```java
// 原调用
String userPrompt = String.format(USER_PROMPT_TEMPLATE, article.getTitle(), excerpt(article.getBody()));
String aiResp = aiService.call(SYSTEM_MESSAGE, userPrompt);

// 替换为
AiPromptRendered prompt = aiPromptRenderService.render("title_optimize_v1",
        Map.of("title", article.getTitle(), "bodyExcerpt", excerpt(article.getBody())));
String aiResp = aiService.call(prompt.systemRole(), prompt.userPrompt());
```

- [ ] **Step 2: 编译验证**

Run: `cd project/user/api && mvn compile`
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/impl/TitleOptimizeServiceImpl.java
git commit -m "refactor(user): TitleOptimizeService use ai prompt config"
```

---

### Task 10: 重构 `SkillAnalyzeServiceImpl` 使用提示词配置

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/SkillAnalyzeServiceImpl.java`

**Interfaces:**
- Consumes: `AiPromptRenderService.render("skill_analyze_v1", Map.of("text", text))`。
- Produces: 原有 `analyze` 行为不变。

- [ ] **Step 1: 注入渲染服务并替换 prompt**

在 `SkillAnalyzeServiceImpl` 中：

1. 添加依赖：

```java
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
```

2. 删除常量：

```java
// 删除以下常量
// private static final String SYSTEM_MESSAGE = "...";
// private static final String USER_PROMPT_TEMPLATE = "...";
```

3. 添加字段：

```java
private final AiPromptRenderService aiPromptRenderService;
```

4. 修改 `analyze` 方法中的调用：

```java
// 原调用
String aiResp = aiService.call(SYSTEM_MESSAGE, USER_PROMPT_TEMPLATE.replace("%s", text));

// 替换为
AiPromptRendered prompt = aiPromptRenderService.render("skill_analyze_v1", Map.of("text", text));
String aiResp = aiService.call(prompt.systemRole(), prompt.userPrompt());
```

- [ ] **Step 2: 编译验证**

Run: `cd project/user/api && mvn compile`
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/SkillAnalyzeServiceImpl.java
git commit -m "refactor(user): SkillAnalyzeService use ai prompt config"
```

---

### Task 11: 前端实现（API / Composable / 页面 / 路由 / 菜单）

**Files:**
- Create: `project/admin/web/src/api/aiPrompt.js`
- Create: `project/admin/web/src/composables/useAiPrompt.js`
- Create: `project/admin/web/src/views/AiPromptListView.vue`
- Create: `project/admin/web/src/views/AiPromptEditView.vue`
- Modify: `project/admin/web/src/router/index.js`
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`

**Interfaces:**
- Consumes: `/api/v1/admin/ai-prompts` 接口（Task 5）。
- Produces: 管理后台「系统设置 → AI 提示词管理」列表页与编辑页。

- [ ] **Step 1: 创建 API 封装**

```js
// project/admin/web/src/api/aiPrompt.js
import request from '@/utils/request.js'

export function listAiPrompts(params) {
  return request.get('/ai-prompts', { params }).then((res) => res.data)
}

export function getAiPrompt(id) {
  return request.get(`/ai-prompts/${id}`).then((res) => res.data)
}

export function createAiPrompt(data) {
  return request.post('/ai-prompts', data)
}

export function updateAiPrompt(id, data) {
  return request.put(`/ai-prompts/${id}`, data)
}

export function deleteAiPrompt(id) {
  return request.delete(`/ai-prompts/${id}`)
}

export function enableAiPrompt(id) {
  return request.post(`/ai-prompts/${id}/actions/enable`)
}

export function disableAiPrompt(id) {
  return request.post(`/ai-prompts/${id}/actions/disable`)
}
```

- [ ] **Step 2: 创建 Composable**

```js
// project/admin/web/src/composables/useAiPrompt.js
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listAiPrompts,
  getAiPrompt,
  createAiPrompt,
  updateAiPrompt,
  deleteAiPrompt,
  enableAiPrompt,
  disableAiPrompt
} from '@/api/aiPrompt.js'

export function useAiPrompt() {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)

  const fetchList = async (params = {}) => {
    loading.value = true
    try {
      const res = await listAiPrompts({
        page: page.value,
        pageSize: pageSize.value,
        ...params
      })
      list.value = res.list || []
      total.value = res.total || 0
    } catch (e) {
      message.error(e.message || '加载失败')
    } finally {
      loading.value = false
    }
  }

  const getDetail = async (id) => {
    return await getAiPrompt(id)
  }

  const save = async (id, data) => {
    if (id) {
      await updateAiPrompt(id, data)
      message.success('更新成功')
    } else {
      await createAiPrompt(data)
      message.success('创建成功')
    }
  }

  const remove = async (id) => {
    await deleteAiPrompt(id)
    message.success('删除成功')
  }

  const toggleStatus = async (record) => {
    if (record.status === 1) {
      await disableAiPrompt(record.id)
      message.success('已停用')
    } else {
      await enableAiPrompt(record.id)
      message.success('已启用')
    }
  }

  return {
    list,
    total,
    loading,
    page,
    pageSize,
    fetchList,
    getDetail,
    save,
    remove,
    toggleStatus
  }
}
```

- [ ] **Step 3: 创建列表页**

```vue
<!-- project/admin/web/src/views/AiPromptListView.vue -->
<template>
  <div class="ai-prompt-list">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">AI 提示词管理</h3>
        <p class="page-desc">
          统一管理代码中硬编码的 AI 提示词。支持变量占位符
          <code>{{variableName}}</code>，非变量内容可直接在线编辑。
        </p>
      </div>

      <div class="toolbar">
        <a-select
          v-model:value="query.module"
          placeholder="归属端"
          allow-clear
          style="width: 120px"
          @change="handleSearch"
        >
          <a-select-option value="admin">管理端</a-select-option>
          <a-select-option value="user">用户端</a-select-option>
        </a-select>
        <a-input
          v-model:value="query.keyword"
          placeholder="编码/名称"
          allow-clear
          style="width: 220px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="gotoCreate">
          <template #icon><PlusOutlined /></template>
          新建提示词
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 1" color="green">启用</a-tag>
            <a-tag v-else color="default">停用</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="gotoEdit(record.id)">编辑</a-button>
            <a-button type="link" size="small" @click="onToggleStatus(record)">
              {{ record.status === 1 ? '停用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确定删除此提示词？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="onDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <div class="pagination">
        <a-pagination
          v-model:current="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useAiPrompt } from '@/composables/useAiPrompt.js'

const router = useRouter()
const {
  list, total, loading, page, pageSize,
  fetchList, remove, toggleStatus
} = useAiPrompt()

const query = reactive({
  module: undefined,
  keyword: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '编码', dataIndex: 'promptCode', key: 'promptCode', width: 180 },
  { title: '名称', dataIndex: 'promptName', key: 'promptName', width: 200 },
  { title: '端', dataIndex: 'module', key: 'module', width: 100 },
  { title: '分类', dataIndex: 'category', key: 'category', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'actions', fixed: 'right', width: 200 }
]

const handleSearch = () => {
  page.value = 1
  fetchList(query)
}

const handleReset = () => {
  query.module = undefined
  query.keyword = ''
  page.value = 1
  pageSize.value = 20
  fetchList(query)
}

const handlePageChange = () => {
  fetchList(query)
}

const gotoCreate = () => router.push('/console/ai-prompts/new')
const gotoEdit = (id) => router.push(`/console/ai-prompts/${id}`)

const onToggleStatus = async (record) => {
  try {
    await toggleStatus(record)
    await fetchList(query)
  } catch (e) {
    message.error(e.message || '操作失败')
  }
}

const onDelete = async (id) => {
  try {
    await remove(id)
    await fetchList(query)
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

onMounted(() => fetchList(query))
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
.page-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
```

- [ ] **Step 4: 创建编辑页**

```vue
<!-- project/admin/web/src/views/AiPromptEditView.vue -->
<template>
  <div class="ai-prompt-edit">
    <a-card :bordered="false">
      <a-page-header
        :title="editingId ? '编辑提示词' : '新建提示词'"
        :sub-title="editingId ? `ID #${editingId}` : ''"
        @back="goBack"
      />

      <a-spin :spinning="loading">
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          style="max-width: 900px; margin-top: 16px"
        >
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="提示词编码" name="promptCode" :rules="[{ required: true, message: '请输入编码' }]">
                <a-input v-model:value="form.promptCode" :disabled="!!editingId" placeholder="如 topic_title_v1" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="显示名称" name="promptName" :rules="[{ required: true, message: '请输入名称' }]">
                <a-input v-model:value="form.promptName" placeholder="如 爆款标题生成" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :xs="24" :md="8">
              <a-form-item label="归属端" name="module" :rules="[{ required: true, message: '请选择归属端' }]">
                <a-select v-model:value="form.module" placeholder="请选择">
                  <a-select-option value="admin">管理端</a-select-option>
                  <a-select-option value="user">用户端</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item label="业务分类" name="category">
                <a-input v-model:value="form.category" placeholder="如 topic_title" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item label="状态" name="status" :rules="[{ required: true, message: '请选择状态' }]">
                <a-select v-model:value="form.status">
                  <a-select-option :value="1">启用</a-select-option>
                  <a-select-option :value="0">停用</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="系统角色（system role）" name="systemRole">
            <a-textarea v-model:value="form.systemRole" :rows="4" placeholder="AI 身份设定" />
          </a-form-item>

          <a-form-item label="用户提示词（user prompt）" name="userPrompt" :rules="[{ required: true, message: '请输入用户提示词' }]">
            <a-textarea v-model:value="form.userPrompt" :rows="16" placeholder="支持 {{variable}} 变量占位符" />
          </a-form-item>

          <a-form-item label="变量定义">
            <a-table
              :columns="variableColumns"
              :data-source="form.variableSchema"
              :pagination="false"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'name'">
                  <a-input v-model:value="record.name" placeholder="变量名" />
                </template>
                <template v-else-if="column.key === 'required'">
                  <a-checkbox v-model:checked="record.required">必填</a-checkbox>
                </template>
                <template v-else-if="column.key === 'description'">
                  <a-input v-model:value="record.description" placeholder="描述" />
                </template>
                <template v-else-if="column.key === 'example'">
                  <a-input v-model:value="record.example" placeholder="示例值" />
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" danger size="small" @click="removeVariable(index)">删除</a-button>
                </template>
              </template>
            </a-table>
            <a-button type="dashed" size="small" style="margin-top: 8px" @click="addVariable">
              + 添加变量
            </a-button>
            <a-button type="link" size="small" style="margin-top: 8px" @click="syncVariables">
              从提示词中自动解析变量
            </a-button>
          </a-form-item>

          <a-form-item label="备注" name="description">
            <a-input v-model:value="form.description" placeholder="可选" />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
              <a-button @click="goBack">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAiPrompt } from '@/composables/useAiPrompt.js'

const route = useRoute()
const router = useRouter()
const { getDetail, save } = useAiPrompt()

const editingId = computed(() => {
  const id = route.params.id
  if (!id || id === 'new') return null
  return Number(id)
})

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  promptCode: '',
  promptName: '',
  module: 'admin',
  category: '',
  systemRole: '',
  userPrompt: '',
  variableSchema: [],
  status: 1,
  sortOrder: 0,
  description: ''
})

const rules = {
  promptCode: [{ required: true, message: '请输入提示词编码', trigger: 'blur' }],
  promptName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  module: [{ required: true, message: '请选择归属端', trigger: 'change' }],
  userPrompt: [{ required: true, message: '请输入用户提示词', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const variableColumns = [
  { title: '变量名', key: 'name', width: 160 },
  { title: '必填', key: 'required', width: 80 },
  { title: '描述', key: 'description' },
  { title: '示例值', key: 'example', width: 180 },
  { title: '操作', key: 'actions', width: 80 }
]

const addVariable = () => {
  form.variableSchema.push({ name: '', required: false, description: '', example: '' })
}

const removeVariable = (index) => {
  form.variableSchema.splice(index, 1)
}

const syncVariables = () => {
  const text = (form.systemRole || '') + (form.userPrompt || '')
  const matches = text.match(/\{\{(\w+)\}\}/g) || []
  const names = [...new Set(matches.map((m) => m.replace(/[{}]/g, '')))]
  const existing = new Map(form.variableSchema.map((v) => [v.name, v]))
  form.variableSchema = names.map((name) => {
    if (existing.has(name)) {
      return existing.get(name)
    }
    return { name, required: true, description: '', example: '' }
  })
}

const loadDetail = async () => {
  if (!editingId.value) return
  loading.value = true
  try {
    const data = await getDetail(editingId.value)
    Object.assign(form, {
      promptCode: data.promptCode,
      promptName: data.promptName,
      module: data.module,
      category: data.category || '',
      systemRole: data.systemRole || '',
      userPrompt: data.userPrompt || '',
      variableSchema: data.variableSchema || [],
      status: data.status,
      sortOrder: data.sortOrder || 0,
      description: data.description || ''
    })
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      promptCode: form.promptCode,
      promptName: form.promptName,
      module: form.module,
      category: form.category || undefined,
      systemRole: form.systemRole || undefined,
      userPrompt: form.userPrompt,
      variableSchema: form.variableSchema.filter((v) => v.name?.trim()),
      status: form.status,
      sortOrder: form.sortOrder,
      description: form.description || undefined
    }
    await save(editingId.value, payload)
    goBack()
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push('/console/ai-prompts')

onMounted(() => loadDetail())
</script>
```

- [ ] **Step 5: 修改路由**

在 `project/admin/web/src/router/index.js` 的 `/console` children 中新增：

```js
{
  path: 'ai-prompts',
  name: 'AdminAiPromptList',
  component: () => import('@/views/AiPromptListView.vue')
},
{
  path: 'ai-prompts/new',
  name: 'AdminAiPromptCreate',
  component: () => import('@/views/AiPromptEditView.vue')
},
{
  path: 'ai-prompts/:id',
  name: 'AdminAiPromptEdit',
  component: () => import('@/views/AiPromptEditView.vue')
}
```

- [ ] **Step 6: 修改菜单**

在 `project/admin/web/src/layouts/AdminLayout.vue` 的「系统设置」分组中新增：

```js
{ key: '/console/ai-prompts', title: 'AI 提示词管理', matchPrefix: '/console/ai-prompts' }
```

- [ ] **Step 7: 启动前端并验证页面**

Run: `cd project/admin/web && npm run dev`
Expected: 访问 `/console/ai-prompts`，列表展示 3 条默认提示词；点击进入编辑，可修改并保存。

- [ ] **Step 8: Commit**

```bash
git add project/admin/web/src/api/aiPrompt.js project/admin/web/src/composables/useAiPrompt.js project/admin/web/src/views/AiPromptListView.vue project/admin/web/src/views/AiPromptEditView.vue project/admin/web/src/router/index.js project/admin/web/src/layouts/AdminLayout.vue
git commit -m "feat(admin-web): add AI prompt management UI"
```

---

## Self-Review

### 1. Spec coverage

| Spec 要求 | 对应 Task |
|-----------|----------|
| 新增 `c_ai_prompt` 表 | Task 1 |
| 初始化 topic_title/title_optimize/skill_analyze 三条记录 | Task 1 |
| 统一 `{{variableName}}` 变量 | Task 2 |
| 必填变量校验 | Task 6 / Task 7 |
| 管理后台 CRUD API | Task 5 |
| 保存即生效 | Task 5 / Task 11 |
| 管理后台页面 | Task 11 |
| 重构 TopicTitleService | Task 8 |
| 重构 TitleOptimizeServiceImpl | Task 9 |
| 重构 SkillAnalyzeServiceImpl | Task 10 |

### 2. Placeholder scan

- 无 "TBD" / "TODO" / "implement later"。
- 所有步骤均包含实际代码或明确命令。
- 无 "add appropriate error handling" 等模糊表述。

### 3. Type consistency

- `AiPromptRendered` 统一使用 `systemRole()` / `userPrompt()`。
- `AiPromptService.PageResult` 字段为 `list, total, page, pageSize`。
- 前端 composable 返回的字段与视图使用一致。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-18-ai-prompt-management-plan.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using `executing-plans`, batch execution with checkpoints.

Which approach would you like?
