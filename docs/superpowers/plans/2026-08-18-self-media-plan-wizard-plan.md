# 制定你的自媒体方案（AI 驱动版）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把用户端「制定你的自媒体方案」向导从硬编码选项改造为全流程 AI 驱动，并把最终方案持久化到后端。

**Architecture:** 新增 `u_self_media_plan` 表和 `SelfMediaPlanService`，通过 `AiPromptRenderService` 读取 `c_ai_prompt` 中的 4 条提示词，调用 LLM 生成平台/目标/赛道/人设选项；前端 `OnboardingIndex.vue` 按步骤调用后端接口并保存方案，`WorkbenchIndex.vue` 读取当前方案展示。

**Tech Stack:** JDK 17, Spring Boot 3.2.5, MyBatis-Plus, MySQL 8, Flyway, Vue 3 + Ant Design Vue + Vite.

## Global Constraints

- 后端代码放在 `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/`。
- 共享工具放在 `project/shared/src/main/java/com/aichuangzuo/shared/utils/`。
- 用户端表前缀必须为 `u_`，字段全小写下划线分隔。
- Flyway 脚本版本号不能重复；用户端当前最新 `V1.0.0_085`，管理端当前最新 `V2.0.0_087`。
- AI 提示词统一使用 `{{variableName}}` 占位符，通过 `AiPromptRenderService.render(code, variables)` 渲染。
- 所有用户数据查询必须带 `user_id` 过滤。
- 不引入新中间件；复用现有 `a_model_config` 和 `RestTemplate` 调用 LLM。
- 前端 API 封装在 `project/user/web/src/api/*.js`，composable 在 `project/user/web/src/composables/*.js`。
- 单元测试优先；每次任务完成后 commit。

---

### Task 1: 抽取公共 LLM JSON 解析工具 `LlmJsonParser`

**Files:**
- Create: `project/shared/src/main/java/com/aichuangzuo/shared/utils/LlmJsonParser.java`
- Modify: `project/shared/pom.xml`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/SkillAnalyzeServiceImpl.java`
- Test: `project/shared/src/test/java/com/aichuangzuo/shared/utils/LlmJsonParserTest.java`

**Interfaces:**
- Consumes: `ObjectMapper`（调用方传入，避免 shared 依赖 Spring 注入）。
- Produces:
  - `String stripCodeFence(String text)`
  - `String extractJsonObject(String text)`
  - `String fixUnescapedQuotesInField(String text, String fieldName)`
  - `JsonNode parseLenient(ObjectMapper mapper, String raw) throws JsonProcessingException`

- [ ] **Step 1: 给 `shared` 模块增加 jackson + junit 依赖**

在 `project/shared/pom.xml` 的 `<dependencies>` 中新增：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 编写 `LlmJsonParser`**

```java
package com.aichuangzuo.shared.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class LlmJsonParser {

    private LlmJsonParser() {}

    public static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String s = text.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            } else {
                s = s.substring(3);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.strip();
    }

    public static String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    public static String fixUnescapedQuotesInField(String text, String fieldName) {
        if (text == null || fieldName == null) {
            return text;
        }
        String promptKey = "\"" + fieldName + "\"";
        int keyIdx = text.indexOf(promptKey);
        if (keyIdx < 0) {
            return text;
        }
        int valueStart = text.indexOf('"', keyIdx + promptKey.length());
        if (valueStart < 0) {
            return text;
        }
        valueStart++;
        int valueEnd = -1;
        for (int i = text.length() - 1; i >= valueStart; i--) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                int j = i + 1;
                while (j < text.length() && Character.isWhitespace(text.charAt(j))) {
                    j++;
                }
                if (j >= text.length() || text.charAt(j) == ',' || text.charAt(j) == '}') {
                    valueEnd = i;
                    break;
                }
            }
        }
        if (valueEnd <= valueStart) {
            return text;
        }
        String promptValue = text.substring(valueStart, valueEnd);
        StringBuilder fixed = new StringBuilder();
        for (int i = 0; i < promptValue.length(); i++) {
            char c = promptValue.charAt(i);
            if (c == '"' && (i == 0 || promptValue.charAt(i - 1) != '\\')) {
                fixed.append('\\');
            }
            fixed.append(c);
        }
        return text.substring(0, valueStart) + fixed + text.substring(valueEnd);
    }

    public static JsonNode parseLenient(ObjectMapper mapper, String raw) throws JsonProcessingException {
        String cleaned = stripCodeFence(raw);
        try {
            return mapper.readTree(cleaned);
        } catch (Exception ignored1) {
            String extracted = extractJsonObject(cleaned);
            if (extracted != null && !extracted.equals(cleaned)) {
                try {
                    return mapper.readTree(extracted);
                } catch (Exception ignored2) {
                    // continue
                }
            }
            String fixed = fixUnescapedQuotesInField(cleaned, "prompt");
            if (!fixed.equals(cleaned)) {
                try {
                    return mapper.readTree(fixed);
                } catch (Exception ignored3) {
                    // continue
                }
            }
            throw new JsonProcessingException("无法解析 LLM 返回的 JSON") {};
        }
    }
}
```

- [ ] **Step 3: 改造 `SkillAnalyzeServiceImpl` 复用该工具**

把 `SkillAnalyzeServiceImpl` 中私有方法 `stripCodeFence`、`extractJsonObject`、`fixUnescapedQuotesInPrompt` 删除，并在 `parseJson` 中改为：

```java
private JsonNode parseJson(String raw) {
    try {
        return LlmJsonParser.parseLenient(lenientObjectMapper, raw);
    } catch (Exception e) {
        log.warn("AI 风格分析结果解析失败 resp={}", abbreviate(raw, 2000));
        throw new BusinessException(SkillErrorCode.SKILL_ANALYZE_FAILED);
    }
}
```

保留 `lenientObjectMapper` 的创建逻辑。

- [ ] **Step 4: 编写单元测试**

```java
package com.aichuangzuo.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmJsonParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void stripCodeFence_removesMarkdownFence() {
        assertEquals("{\"a\":1}", LlmJsonParser.stripCodeFence("```json\n{\"a\":1}\n```"));
    }

    @Test
    void extractJsonObject_pullsObjectFromSurroundingText() {
        String result = LlmJsonParser.extractJsonObject("好的，结果是：{\"a\":1}，请查收。");
        assertEquals("{\"a\":1}", result);
    }

    @Test
    void parseLenient_fixesUnescapedQuotesInPromptField() throws Exception {
        String raw = "{\"prompt\":\"他说 \"你好\"\"}";
        assertEquals("他说 \"你好\"", LlmJsonParser.parseLenient(mapper, raw).path("prompt").asText());
    }

    @Test
    void parseLenient_throwsOnInvalidJson() {
        assertThrows(Exception.class, () -> LlmJsonParser.parseLenient(mapper, "这不是 JSON"));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/shared
mvn test -Dtest=LlmJsonParserTest
```

Expected: 4 tests pass.

- [ ] **Step 6: Commit**

```bash
git add project/shared/pom.xml \
        project/shared/src/main/java/com/aichuangzuo/shared/utils/LlmJsonParser.java \
        project/shared/src/test/java/com/aichuangzuo/shared/utils/LlmJsonParserTest.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/SkillAnalyzeServiceImpl.java
git commit -m "refactor(shared): 抽取 LLM JSON 解析工具 LlmJsonParser 并复用于文风分析"
```

---

### Task 2: 创建方案表、实体、Mapper 和 Flyway 迁移

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/entity/SelfMediaPlan.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/mapper/SelfMediaPlanMapper.java`
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_086__create_self_media_plan_table.sql`

**Interfaces:**
- Produces: `SelfMediaPlan` 实体、`SelfMediaPlanMapper` 可查询/保存。

- [ ] **Step 1: 编写 Flyway 迁移脚本**

`project/user/api/src/main/resources/db/migration/V1.0.0_086__create_self_media_plan_table.sql`：

```sql
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_self_media_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    platform_key VARCHAR(64) NOT NULL COMMENT '主攻平台key，如 xiaohongshu',
    platform_name VARCHAR(128) NOT NULL COMMENT '主攻平台显示名，如 小红书',
    goal VARCHAR(128) NOT NULL COMMENT '用户选择的核心目标',
    background VARCHAR(128) DEFAULT NULL COMMENT '用户职业/经验领域',
    has_product TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有可变现产品/服务：0-否，1-是',
    product_desc VARCHAR(512) DEFAULT NULL COMMENT '可变现产品/服务描述',
    niche_key VARCHAR(64) NOT NULL COMMENT '细分赛道key',
    niche_name VARCHAR(128) NOT NULL COMMENT '细分赛道显示名',
    persona_key VARCHAR(64) NOT NULL COMMENT '人设key',
    persona_name VARCHAR(128) NOT NULL COMMENT '人设显示名',
    content_pillars_json JSON NOT NULL COMMENT '内容支柱比例 [{"name":"...","percent":60},...]',
    recommendation_context_json JSON DEFAULT NULL COMMENT 'AI推荐问卷上下文',
    is_recommended_by_ai TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '平台是否由AI推荐：0-否，1-是',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_self_media_plan_user_id (user_id),
    KEY idx_u_self_media_plan_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自媒体运营方案表';
```

- [ ] **Step 2: 编写实体类**

```java
package com.aichuangzuo.user.modules.selfmedia.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_self_media_plan")
public class SelfMediaPlan extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String platformKey;
    private String platformName;
    private String goal;
    private String background;
    private Integer hasProduct;
    private String productDesc;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private String contentPillarsJson;
    private String recommendationContextJson;
    private Integer isRecommendedByAi;
    private Long tenantId;
    private Integer isDeleted;
}
```

- [ ] **Step 3: 编写 Mapper**

```java
package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SelfMediaPlanMapper extends BaseMapper<SelfMediaPlan> {

    @Select("SELECT * FROM u_self_media_plan WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    SelfMediaPlan selectByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/entity/SelfMediaPlan.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/mapper/SelfMediaPlanMapper.java \
        project/user/api/src/main/resources/db/migration/V1.0.0_086__create_self_media_plan_table.sql
git commit -m "feat(user): 新增 u_self_media_plan 表及实体 Mapper"
```

---

### Task 3: 新增业务错误码

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/enums/SelfMediaPlanErrorCode.java`

**Interfaces:**
- Produces: `SelfMediaPlanErrorCode` 枚举，供后续 Service 抛出。

- [ ] **Step 1: 编写枚举**

```java
package com.aichuangzuo.user.modules.selfmedia.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum SelfMediaPlanErrorCode implements ErrorCode {

    SELF_MEDIA_PLAN_NOT_FOUND(113001, "运营方案不存在"),
    SELF_MEDIA_PLAN_AI_FAILED(113002, "AI 推荐失败，请重试"),
    SELF_MEDIA_PLAN_PLATFORM_REQUIRED(113003, "请选择自媒体平台"),
    SELF_MEDIA_PLAN_GOAL_REQUIRED(113004, "请选择运营目标"),
    SELF_MEDIA_PLAN_NICHE_REQUIRED(113005, "请选择细分赛道"),
    SELF_MEDIA_PLAN_PERSONA_REQUIRED(113006, "请选择人设定位");

    private final int code;
    private final String message;

    SelfMediaPlanErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/enums/SelfMediaPlanErrorCode.java
git commit -m "feat(user): 新增自媒体方案错误码"
```

---

### Task 4: 定义 DTO 与 VO

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/SelfMediaRecommendationContext.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/request/RecommendPlatformRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/request/RecommendGoalsRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/request/RecommendNichesRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/request/RecommendPersonasRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto/request/SavePlanRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/PillarVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/GoalOptionVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/NicheOptionVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/PersonaOptionVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/RecommendPlatformResultVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/RecommendPersonasResultVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo/SelfMediaPlanVO.java`

**Interfaces:**
- Produces: 请求/响应对象，字段与 API 设计文档一致。

- [ ] **Step 1: 编写公共上下文 DTO**

```java
package com.aichuangzuo.user.modules.selfmedia.dto;

import lombok.Data;

@Data
public class SelfMediaRecommendationContext {
    private String workType;
    private String timePerWeek;
    private String incomeGoal;
    private String breakEvenPeriod;
    private String contentType;
    private String audience;
    private String identity;
    private String onCamera;
    private String note;
}
```

- [ ] **Step 2: 编写推荐请求 DTO**

```java
package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

@Data
public class RecommendPlatformRequest {
    private SelfMediaRecommendationContext context;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

@Data
public class RecommendGoalsRequest {
    private String platformKey;
    private String background;
    private SelfMediaRecommendationContext context;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

@Data
public class RecommendNichesRequest {
    private String platformKey;
    private String goal;
    private String background;
    private Boolean hasProduct;
    private String productDesc;
    private SelfMediaRecommendationContext context;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

@Data
public class RecommendPersonasRequest {
    private String platformKey;
    private String goal;
    private String background;
    private String nicheKey;
    private String nicheName;
    private SelfMediaRecommendationContext context;
}
```

- [ ] **Step 3: 编写保存请求 DTO**

```java
package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import lombok.Data;

import java.util.List;

@Data
public class SavePlanRequest {
    private String platformKey;
    private String platformName;
    private String goal;
    private String background;
    private Boolean hasProduct;
    private String productDesc;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private List<PillarVO> pillars;
    private Boolean isRecommendedByAI;
    private SelfMediaRecommendationContext recommendationContext;
}
```

- [ ] **Step 4: 编写 VO**

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

@Data
public class PillarVO {
    private String name;
    private Integer percent;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

@Data
public class GoalOptionVO {
    private String key;
    private String name;
    private String description;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

@Data
public class NicheOptionVO {
    private String key;
    private String name;
    private String audience;
    private String monetization;
    private String riskLabel;
    private String riskColor;
    private Integer caseCount;
    private String reason;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

@Data
public class PersonaOptionVO {
    private String key;
    private String name;
    private String desc;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

@Data
public class RecommendPlatformResultVO {
    private String platformKey;
    private String platformName;
    private String reason;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecommendPersonasResultVO {
    private List<PersonaOptionVO> personas;
    private List<PillarVO> defaultPillars;
}
```

```java
package com.aichuangzuo.user.modules.selfmedia.vo;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

import java.util.List;

@Data
public class SelfMediaPlanVO {
    private String platformKey;
    private String platformName;
    private String goal;
    private String background;
    private Boolean hasProduct;
    private String productDesc;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private List<PillarVO> pillars;
    private Boolean isRecommendedByAI;
    private SelfMediaRecommendationContext recommendationContext;
}
```

- [ ] **Step 5: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/dto \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/vo
git commit -m "feat(user): 新增自媒体方案 DTO 与 VO"
```

---

### Task 5: 实现 LLM 调用器 `SelfMediaPlanAiService`

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanAiService.java`

**Interfaces:**
- Consumes: `AiPromptRenderService`、`ArticleModelConfigMapper`、`ObjectMapper`、`RestTemplate`、配置 `user.model.api-key-secret`。
- Produces: `JsonNode callPrompt(String promptCode, Map<String, Object> variables)`。

- [ ] **Step 1: 编写调用器**

```java
package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.aichuangzuo.shared.utils.LlmJsonParser;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.user.modules.article.dto.ActiveModelConfig;
import com.aichuangzuo.user.modules.article.mapper.ArticleModelConfigMapper;
import com.aichuangzuo.user.modules.selfmedia.enums.SelfMediaPlanErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SelfMediaPlanAiService {

    private final ArticleModelConfigMapper modelConfigMapper;
    private final AiPromptRenderService aiPromptRenderService;
    private final String apiKeySecret;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public SelfMediaPlanAiService(ArticleModelConfigMapper modelConfigMapper,
                                  AiPromptRenderService aiPromptRenderService,
                                  @Value("${user.model.api-key-secret}") String apiKeySecret,
                                  ObjectMapper objectMapper,
                                  RestTemplate restTemplate) {
        this.modelConfigMapper = modelConfigMapper;
        this.aiPromptRenderService = aiPromptRenderService;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public JsonNode callPrompt(String promptCode, Map<String, Object> variables) {
        AiPromptRendered rendered = aiPromptRenderService.render(promptCode, variables);
        ActiveModelConfig cfg = modelConfigMapper.selectActive();
        if (cfg == null) {
            log.warn("自媒体方案 AI 调用失败：无 active 模型配置");
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("自媒体方案 AI api key 解密失败", e);
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }

        String url = resolveUrl(cfg);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", rendered.systemRole()),
                Map.of("role", "user", "content", rendered.userPrompt())
        ));
        body.put("temperature", 0.5);
        body.put("max_tokens", 4096);
        body.put("top_p", 1.0);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            String content = extractContent(response.getBody(), cfg.getProviderType());
            return LlmJsonParser.parseLenient(objectMapper, content);
        } catch (RestClientException e) {
            log.warn("自媒体方案 AI 调用失败 provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        } catch (Exception e) {
            log.warn("自媒体方案 AI 响应解析失败", e);
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
    }

    private String resolveUrl(ActiveModelConfig cfg) {
        String base = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().trim().replaceAll("/+$", "");
        int schemeEnd = base.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = base.indexOf('/', schemeEnd + 3);
            if (pathStart > 0) base = base.substring(0, pathStart);
        }
        String suffix = "minimax".equalsIgnoreCase(cfg.getProviderType())
                ? "/v1/text/chatcompletion_v2"
                : "/v1/chat/completions";
        return base + suffix;
    }

    private String extractContent(String responseBody, String providerType) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
            }
        } catch (Exception e) {
            log.warn("自媒体方案 AI 响应提取失败", e);
        }
        throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanAiService.java
git commit -m "feat(user): 新增自媒体方案 LLM 调用器"
```

---

### Task 6: 实现业务 Service 与 Controller

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/impl/SelfMediaPlanServiceImpl.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/controller/SelfMediaPlanController.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanServiceImplTest.java`

**Interfaces:**
- Consumes: `SelfMediaPlanAiService`、`SelfMediaPlanMapper`、`PlatformMapper`、`ObjectMapper`。
- Produces:
  - `SelfMediaPlanVO getCurrentPlan(Long userId)`
  - `SelfMediaPlanVO savePlan(Long userId, SavePlanRequest request)`
  - `RecommendPlatformResultVO recommendPlatform(Long userId, RecommendPlatformRequest request)`
  - `List<GoalOptionVO> recommendGoals(Long userId, RecommendGoalsRequest request)`
  - `List<NicheOptionVO> recommendNiches(Long userId, RecommendNichesRequest request)`
  - `RecommendPersonasResultVO recommendPersonas(Long userId, RecommendPersonasRequest request)`

- [ ] **Step 1: 编写 Service 接口**

```java
package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.vo.*;

import java.util.List;

public interface SelfMediaPlanService {
    SelfMediaPlanVO getCurrentPlan(Long userId);
    SelfMediaPlanVO savePlan(Long userId, SavePlanRequest request);
    RecommendPlatformResultVO recommendPlatform(Long userId, RecommendPlatformRequest request);
    List<GoalOptionVO> recommendGoals(Long userId, RecommendGoalsRequest request);
    List<NicheOptionVO> recommendNiches(Long userId, RecommendNichesRequest request);
    RecommendPersonasResultVO recommendPersonas(Long userId, RecommendPersonasRequest request);
}
```

- [ ] **Step 2: 编写 Service 实现**

下面是核心骨架（省略 import，按需补充）：

```java
package com.aichuangzuo.user.modules.selfmedia.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.platform.mapper.PlatformMapper;
import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.enums.SelfMediaPlanErrorCode;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfMediaPlanServiceImpl implements SelfMediaPlanService {

    private final SelfMediaPlanAiService aiService;
    private final SelfMediaPlanMapper planMapper;
    private final PlatformMapper platformMapper;
    private final ObjectMapper objectMapper;

    @Override
    public SelfMediaPlanVO getCurrentPlan(Long userId) {
        SelfMediaPlan plan = planMapper.selectByUserId(userId);
        return plan == null ? null : toVO(plan);
    }

    @Override
    public SelfMediaPlanVO savePlan(Long userId, SavePlanRequest request) {
        validateSave(request);
        SelfMediaPlan existing = planMapper.selectByUserId(userId);
        SelfMediaPlan plan = existing == null ? new SelfMediaPlan() : existing;
        plan.setUserId(userId);
        plan.setPlatformKey(request.getPlatformKey());
        plan.setPlatformName(request.getPlatformName());
        plan.setGoal(request.getGoal());
        plan.setBackground(request.getBackground());
        plan.setHasProduct(Boolean.TRUE.equals(request.getHasProduct()) ? 1 : 0);
        plan.setProductDesc(request.getProductDesc());
        plan.setNicheKey(request.getNicheKey());
        plan.setNicheName(request.getNicheName());
        plan.setPersonaKey(request.getPersonaKey());
        plan.setPersonaName(request.getPersonaName());
        plan.setIsRecommendedByAI(Boolean.TRUE.equals(request.getIsRecommendedByAI()) ? 1 : 0);
        plan.setContentPillarsJson(toJson(request.getPillars()));
        plan.setRecommendationContextJson(toJson(request.getRecommendationContext()));
        plan.setTenantId(0L);
        plan.setIsDeleted(0);
        if (existing == null) {
            planMapper.insert(plan);
        } else {
            planMapper.updateById(plan);
        }
        return getCurrentPlan(userId);
    }

    @Override
    public RecommendPlatformResultVO recommendPlatform(Long userId, RecommendPlatformRequest request) {
        SelfMediaRecommendationContext ctx = request.getContext();
        List<com.aichuangzuo.shared.entity.Platform> platforms = platformMapper.selectList(
                new LambdaQueryWrapper<com.aichuangzuo.shared.entity.Platform>()
                        .eq(com.aichuangzuo.shared.entity.Platform::getStatus, 1)
                        .orderByAsc(com.aichuangzuo.shared.entity.Platform::getSortOrder));
        String platformsJson = toJson(platforms.stream().map(p -> Map.of(
                "platformKey", p.getPlatformKey(),
                "platformName", p.getPlatformName(),
                "tagline", defaultString(p.getTagline()),
                "contentForm", jsonListToString(p.getContentFormJson()),
                "monetization", jsonListToString(p.getMonetizationJson()),
                "bestFor", defaultString(p.getBestFor())
        )).collect(Collectors.toList()));

        Map<String, Object> vars = contextVars(ctx);
        vars.put("platformsJson", platformsJson);
        JsonNode root = aiService.callPrompt("self_media_recommend_platform_v1", vars);
        String platformKey = root.path("platformKey").asText("");
        com.aichuangzuo.shared.entity.Platform chosen = platforms.stream()
                .filter(p -> p.getPlatformKey().equals(platformKey))
                .findFirst()
                .orElse(platforms.isEmpty() ? null : platforms.get(0));
        if (chosen == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_AI_FAILED);
        }
        RecommendPlatformResultVO vo = new RecommendPlatformResultVO();
        vo.setPlatformKey(chosen.getPlatformKey());
        vo.setPlatformName(chosen.getPlatformName());
        vo.setReason(root.path("reason").asText(""));
        return vo;
    }

    @Override
    public List<GoalOptionVO> recommendGoals(Long userId, RecommendGoalsRequest request) {
        com.aichuangzuo.shared.entity.Platform platform = requirePlatform(request.getPlatformKey());
        Map<String, Object> vars = platformVars(platform);
        vars.put("background", defaultString(request.getBackground()));
        vars.putAll(contextVars(request.getContext()));
        JsonNode root = aiService.callPrompt("self_media_recommend_goals_v1", vars);
        return parseGoals(root.path("goals"));
    }

    @Override
    public List<NicheOptionVO> recommendNiches(Long userId, RecommendNichesRequest request) {
        com.aichuangzuo.shared.entity.Platform platform = requirePlatform(request.getPlatformKey());
        Map<String, Object> vars = platformVars(platform);
        vars.put("goal", defaultString(request.getGoal()));
        vars.put("background", defaultString(request.getBackground()));
        vars.put("hasProduct", Boolean.TRUE.equals(request.getHasProduct()) ? "有" : "没有");
        vars.put("productDesc", defaultString(request.getProductDesc()));
        vars.putAll(contextVars(request.getContext()));
        JsonNode root = aiService.callPrompt("self_media_recommend_niches_v1", vars);
        return parseNiches(root.path("niches"));
    }

    @Override
    public RecommendPersonasResultVO recommendPersonas(Long userId, RecommendPersonasRequest request) {
        com.aichuangzuo.shared.entity.Platform platform = requirePlatform(request.getPlatformKey());
        Map<String, Object> vars = platformVars(platform);
        vars.put("goal", defaultString(request.getGoal()));
        vars.put("background", defaultString(request.getBackground()));
        vars.put("nicheKey", defaultString(request.getNicheKey()));
        vars.put("nicheName", defaultString(request.getNicheName()));
        vars.putAll(contextVars(request.getContext()));
        JsonNode root = aiService.callPrompt("self_media_recommend_personas_v1", vars);
        RecommendPersonasResultVO vo = new RecommendPersonasResultVO();
        vo.setPersonas(parsePersonas(root.path("personas")));
        vo.setDefaultPillars(parsePillars(root.path("defaultPillars")));
        return vo;
    }

    // ---------- private helpers ----------

    private void validateSave(SavePlanRequest request) {
        if (StringUtils.isBlank(request.getPlatformKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PLATFORM_REQUIRED);
        }
        if (StringUtils.isBlank(request.getGoal())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_GOAL_REQUIRED);
        }
        if (StringUtils.isBlank(request.getNicheKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_NICHE_REQUIRED);
        }
        if (StringUtils.isBlank(request.getPersonaKey())) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PERSONA_REQUIRED);
        }
    }

    private com.aichuangzuo.shared.entity.Platform requirePlatform(String platformKey) {
        com.aichuangzuo.shared.entity.Platform p = platformMapper.selectOne(
                new LambdaQueryWrapper<com.aichuangzuo.shared.entity.Platform>()
                        .eq(com.aichuangzuo.shared.entity.Platform::getPlatformKey, platformKey)
                        .eq(com.aichuangzuo.shared.entity.Platform::getStatus, 1));
        if (p == null) {
            throw new BusinessException(SelfMediaPlanErrorCode.SELF_MEDIA_PLAN_PLATFORM_REQUIRED);
        }
        return p;
    }

    private Map<String, Object> platformVars(com.aichuangzuo.shared.entity.Platform p) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("platformKey", p.getPlatformKey());
        vars.put("platformName", p.getPlatformName());
        vars.put("platformTagline", defaultString(p.getTagline()));
        vars.put("platformContentForm", jsonListToString(p.getContentFormJson()));
        vars.put("platformMonetization", jsonListToString(p.getMonetizationJson()));
        vars.put("platformBestFor", defaultString(p.getBestFor()));
        return vars;
    }

    private Map<String, Object> contextVars(SelfMediaRecommendationContext ctx) {
        Map<String, Object> vars = new LinkedHashMap<>();
        if (ctx == null) {
            vars.put("workType", "");
            vars.put("timePerWeek", "");
            vars.put("incomeGoal", "");
            vars.put("breakEvenPeriod", "");
            vars.put("contentType", "");
            vars.put("audience", "");
            vars.put("identity", "");
            vars.put("onCamera", "");
            vars.put("note", "");
            return vars;
        }
        vars.put("workType", defaultString(ctx.getWorkType()));
        vars.put("timePerWeek", defaultString(ctx.getTimePerWeek()));
        vars.put("incomeGoal", defaultString(ctx.getIncomeGoal()));
        vars.put("breakEvenPeriod", defaultString(ctx.getBreakEvenPeriod()));
        vars.put("contentType", defaultString(ctx.getContentType()));
        vars.put("audience", defaultString(ctx.getAudience()));
        vars.put("identity", defaultString(ctx.getIdentity()));
        vars.put("onCamera", defaultString(ctx.getOnCamera()));
        vars.put("note", defaultString(ctx.getNote()));
        return vars;
    }

    private String defaultString(String s) {
        return s == null ? "" : s;
    }

    private String jsonListToString(String json) {
        if (StringUtils.isBlank(json)) return "";
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return String.join("、", list);
        } catch (Exception e) {
            return json;
        }
    }

    @SneakyThrows
    private String toJson(Object value) {
        if (value == null) return null;
        return objectMapper.writeValueAsString(value);
    }

    private SelfMediaPlanVO toVO(SelfMediaPlan plan) {
        SelfMediaPlanVO vo = new SelfMediaPlanVO();
        vo.setPlatformKey(plan.getPlatformKey());
        vo.setPlatformName(plan.getPlatformName());
        vo.setGoal(plan.getGoal());
        vo.setBackground(plan.getBackground());
        vo.setHasProduct(Integer.valueOf(1).equals(plan.getHasProduct()));
        vo.setProductDesc(plan.getProductDesc());
        vo.setNicheKey(plan.getNicheKey());
        vo.setNicheName(plan.getNicheName());
        vo.setPersonaKey(plan.getPersonaKey());
        vo.setPersonaName(plan.getPersonaName());
        vo.setIsRecommendedByAI(Integer.valueOf(1).equals(plan.getIsRecommendedByAi()));
        vo.setPillars(parsePillarsJson(plan.getContentPillarsJson()));
        vo.setRecommendationContext(parseContextJson(plan.getRecommendationContextJson()));
        return vo;
    }

    private List<PillarVO> parsePillarsJson(String json) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PillarVO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private SelfMediaRecommendationContext parseContextJson(String json) {
        if (StringUtils.isBlank(json)) return new SelfMediaRecommendationContext();
        try {
            return objectMapper.readValue(json, SelfMediaRecommendationContext.class);
        } catch (Exception e) {
            return new SelfMediaRecommendationContext();
        }
    }

    private List<GoalOptionVO> parseGoals(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    GoalOptionVO vo = new GoalOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setName(n.path("name").asText(""));
                    vo.setDescription(n.path("description").asText(""));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<NicheOptionVO> parseNiches(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    NicheOptionVO vo = new NicheOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setName(n.path("name").asText(""));
                    vo.setAudience(n.path("audience").asText(""));
                    vo.setMonetization(n.path("monetization").asText(""));
                    vo.setRiskLabel(n.path("riskLabel").asText(""));
                    vo.setRiskColor(n.path("riskColor").asText(""));
                    vo.setCaseCount(n.path("caseCount").asInt(0));
                    vo.setReason(n.path("reason").asText(""));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<PersonaOptionVO> parsePersonas(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    PersonaOptionVO vo = new PersonaOptionVO();
                    vo.setKey(n.path("key").asText(""));
                    vo.setName(n.path("name").asText(""));
                    vo.setDesc(n.path("desc").asText(""));
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<PillarVO> parsePillars(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    PillarVO vo = new PillarVO();
                    vo.setName(n.path("name").asText(""));
                    vo.setPercent(n.path("percent").asInt(0));
                    return vo;
                }).collect(Collectors.toList());
    }
}
```

- [ ] **Step 3: 编写 Controller**

```java
package com.aichuangzuo.user.modules.selfmedia.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户端-自媒体方案")
@RestController
@RequestMapping("/api/v1/user/self-media-plans")
@RequiredArgsConstructor
public class SelfMediaPlanController {

    private final SelfMediaPlanService planService;

    @GetMapping("/current")
    public Result<SelfMediaPlanVO> getCurrentPlan() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.getCurrentPlan(userId));
    }

    @PostMapping
    public Result<SelfMediaPlanVO> savePlan(@RequestBody SavePlanRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.savePlan(userId, request));
    }

    @PostMapping("/actions/recommend-platform")
    public Result<RecommendPlatformResultVO> recommendPlatform(@RequestBody RecommendPlatformRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.recommendPlatform(userId, request));
    }

    @PostMapping("/actions/recommend-goals")
    public Result<List<GoalOptionVO>> recommendGoals(@RequestBody RecommendGoalsRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.recommendGoals(userId, request));
    }

    @PostMapping("/actions/recommend-niches")
    public Result<List<NicheOptionVO>> recommendNiches(@RequestBody RecommendNichesRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.recommendNiches(userId, request));
    }

    @PostMapping("/actions/recommend-personas")
    public Result<RecommendPersonasResultVO> recommendPersonas(@RequestBody RecommendPersonasRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.recommendPersonas(userId, request));
    }
}
```

- [ ] **Step 4: 编写 Service 单元测试（mock AI 调用器）**

```java
package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.SavePlanRequest;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
import com.aichuangzuo.user.modules.selfmedia.service.impl.SelfMediaPlanServiceImpl;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelfMediaPlanServiceImplTest {

    private final SelfMediaPlanAiService aiService = mock(SelfMediaPlanAiService.class);
    private final SelfMediaPlanMapper planMapper = mock(SelfMediaPlanMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SelfMediaPlanServiceImpl service() {
        return new SelfMediaPlanServiceImpl(aiService, planMapper, null, objectMapper);
    }

    @Test
    void savePlan_shouldUpsertAndReturnVo() {
        SavePlanRequest req = new SavePlanRequest();
        req.setPlatformKey("xiaohongshu");
        req.setPlatformName("小红书");
        req.setGoal("靠生活经验变现");
        req.setBackground("职场/管理");
        req.setHasProduct(false);
        req.setNicheKey("zhichangzhuanxing");
        req.setNicheName("35+ 职场转型");
        req.setPersonaKey("experiencer");
        req.setPersonaName("实战记录者");
        PillarVO p = new PillarVO();
        p.setName("干货复盘");
        p.setPercent(60);
        req.setPillars(List.of(p));

        when(planMapper.selectByUserId(1L)).thenReturn(null);

        SelfMediaPlanVO vo = service().savePlan(1L, req);

        assertEquals("xiaohongshu", vo.getPlatformKey());
        assertEquals("实战记录者", vo.getPersonaName());
        verify(planMapper).insert(any(SelfMediaPlan.class));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn test -Dtest=SelfMediaPlanServiceImplTest
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/controller \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanServiceImplTest.java
git commit -m "feat(user): 实现自媒体方案 Service 与 Controller"
```

---

### Task 7: 初始化 AI 提示词种子数据

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_088__seed_self_media_plan_prompts.sql`

**Interfaces:**
- Produces: 4 条 `c_ai_prompt` 记录，用户端后端可按 code 读取。

- [ ] **Step 1: 编写迁移脚本**

迁移脚本向 `c_ai_prompt` 插入 4 条提示词。因 SQL 较长，这里给出结构，实际内容复制设计文档「AI 提示词设计」章节的 `system_role` 和 `user_prompt`，并把 `{{variableName}}` 原样保留。

```sql
SET NAMES utf8mb4;

INSERT INTO c_ai_prompt (
    prompt_code, prompt_name, module, category, system_role, user_prompt, variable_schema, status, sort_order, description
) VALUES (
    'self_media_recommend_platform_v1',
    '自媒体方案-平台推荐',
    'user',
    'self_media_plan',
    '你是一位自媒体平台匹配顾问。你只输出合法 JSON，不输出任何解释、免责声明或 markdown 代码围栏。',
    '请根据用户画像，从以下平台中推荐最合适的一个平台，并说明理由。...（变量：{{platformsJson}}、{{workType}} 等）',
    '[{"name":"workType","required":true,...},{"name":"platformsJson","required":true,...}]',
    1, 0, '自媒体方案：根据问卷推荐平台'
);

-- 同理插入 self_media_recommend_goals_v1、self_media_recommend_niches_v1、self_media_recommend_personas_v1
```

完整 prompt 文本见设计文档《AI 提示词设计》章节，直接复制到 SQL 的 `user_prompt` 字段即可。

- [ ] **Step 2: Commit**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_088__seed_self_media_plan_prompts.sql
git commit -m "feat(admin): 初始化自媒体方案 AI 提示词"
```

---

### Task 8: 前端 API 与 Composable

**Files:**
- Create: `project/user/web/src/api/selfMediaPlan.js`
- Create: `project/user/web/src/composables/useSelfMediaPlan.js`

**Interfaces:**
- Produces: `fetchCurrentPlan`、`recommendPlatform`、`recommendGoals`、`recommendNiches`、`recommendPersonas`、`savePlan`。

- [ ] **Step 1: 编写 API 封装**

```js
import request from '@/utils/request.js'

const BASE = '/self-media-plans'

export function fetchCurrentPlan() {
  return request.get(`${BASE}/current`).then((res) => res.data)
}

export function recommendPlatform(data) {
  return request.post(`${BASE}/actions/recommend-platform`, data).then((res) => res.data)
}

export function recommendGoals(data) {
  return request.post(`${BASE}/actions/recommend-goals`, data).then((res) => res.data)
}

export function recommendNiches(data) {
  return request.post(`${BASE}/actions/recommend-niches`, data).then((res) => res.data)
}

export function recommendPersonas(data) {
  return request.post(`${BASE}/actions/recommend-personas`, data).then((res) => res.data)
}

export function savePlan(data) {
  return request.post(BASE, data).then((res) => res.data)
}
```

- [ ] **Step 2: 编写 Composable**

```js
import { ref } from 'vue'
import {
  fetchCurrentPlan as apiFetchCurrentPlan,
  savePlan as apiSavePlan,
  recommendPlatform as apiRecommendPlatform,
  recommendGoals as apiRecommendGoals,
  recommendNiches as apiRecommendNiches,
  recommendPersonas as apiRecommendPersonas
} from '@/api/selfMediaPlan.js'

export function useSelfMediaPlan() {
  const plan = ref(null)
  const loading = ref(false)

  async function loadPlan() {
    loading.value = true
    try {
      plan.value = await apiFetchCurrentPlan()
    } finally {
      loading.value = false
    }
  }

  async function save(data) {
    plan.value = await apiSavePlan(data)
    return plan.value
  }

  return {
    plan,
    loading,
    loadPlan,
    save,
    recommendPlatform: apiRecommendPlatform,
    recommendGoals: apiRecommendGoals,
    recommendNiches: apiRecommendNiches,
    recommendPersonas: apiRecommendPersonas
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/api/selfMediaPlan.js \
        project/user/web/src/composables/useSelfMediaPlan.js
git commit -m "feat(user-web): 新增自媒体方案 API 与 composable"
```

---

### Task 9: 改造 `OnboardingIndex.vue` 调用后端 AI 接口

**Files:**
- Modify: `project/user/web/src/views/console/OnboardingIndex.vue`

**Interfaces:**
- Consumes: `useSelfMediaPlan`、`fetchPlatforms`。

- [ ] **Step 1: 替换常量数据为接口返回**

在 `<script setup>` 顶部引入：

```js
import { useSelfMediaPlan } from '@/composables/useSelfMediaPlan.js'

const { recommendPlatform, recommendGoals, recommendNiches, recommendPersonas, save } = useSelfMediaPlan()
```

删除 `goals`、`backgrounds` 硬编码数组（保留 `backgrounds` 作为 UI 选项源）和 `nichePool`、`personas` 硬编码数组。新增响应式变量：

```js
const goalOptions = ref([])
const nicheOptions = ref([])
const personaOptions = ref([])
const recommendLoading = ref(false)
```

- [ ] **Step 2: AI 推荐平台改为调用后端**

把 `runRecommend` 函数替换为：

```js
async function runRecommend() {
  if (!canRecommend.value) return
  recommendLoading.value = true
  recommendResult.value = null
  try {
    const result = await recommendPlatform({ context: buildContext(recommendForm) })
    recommendResult.value = {
      platform: platforms.value.find((p) => p.key === result.platformKey) || platforms.value[0],
      reason: result.reason
    }
  } finally {
    recommendLoading.value = false
  }
}

function buildContext(source) {
  return {
    workType: source.workType || '',
    timePerWeek: source.timePerWeek || '',
    incomeGoal: source.incomeGoal || '',
    breakEvenPeriod: source.breakEvenPeriod || '',
    contentType: source.contentType || '',
    audience: source.audience || '',
    identity: source.identity || '',
    onCamera: source.onCamera || '',
    note: source.note || ''
  }
}
```

- [ ] **Step 3: Step 2 获取 AI 目标推荐**

在 Step 2 模板中新增按钮：

```html
<a-button
  type="primary"
  :loading="goalsLoading"
  :disabled="!form.background"
  @click="loadGoals"
>
  获取 AI 目标推荐
</a-button>
```

实现：

```js
const goalsLoading = ref(false)

async function loadGoals() {
  if (!form.platform || !form.background) return
  goalsLoading.value = true
  try {
    goalOptions.value = await recommendGoals({
      platformKey: form.platform,
      background: form.background,
      context: buildContext({ ...recommendForm, timePerWeek: form.timePerWeek })
    })
  } finally {
    goalsLoading.value = false
  }
}
```

渲染目标选项时使用 `goalOptions.value`：

```html
<button
  v-for="g in goalOptions"
  :key="g.key"
  :class="['option-btn', { selected: form.goal === g.name }]"
  @click="form.goal = g.name"
>
  {{ g.name }}
</button>
```

- [ ] **Step 4: Step 3 获取 AI 赛道推荐**

在进入 Step 3 时（或点击下一步时）调用：

```js
async function loadNiches() {
  nicheOptions.value = await recommendNiches({
    platformKey: form.platform,
    goal: form.goal,
    background: form.background,
    hasProduct: form.hasProduct,
    productDesc: form.productDesc,
    context: buildContext({ ...recommendForm, timePerWeek: form.timePerWeek })
  })
  selectedNiche.value = nicheOptions.value[0]?.key || ''
}
```

把 `next()` 中 `step.value === 2` 的逻辑改为调用 `loadNiches()`。

- [ ] **Step 5: Step 4 获取 AI 人设推荐**

```js
async function loadPersonas() {
  const niche = nicheOptions.value.find((n) => n.key === selectedNiche.value)
  const result = await recommendPersonas({
    platformKey: form.platform,
    goal: form.goal,
    background: form.background,
    nicheKey: niche?.key,
    nicheName: niche?.name,
    context: buildContext({ ...recommendForm, timePerWeek: form.timePerWeek })
  })
  personaOptions.value = result.personas
  if (result.defaultPillars?.length) {
    pillars.splice(0, pillars.length, ...result.defaultPillars)
  }
  selectedPersona.value = personaOptions.value[0]?.key || ''
}
```

把 `next()` 中 `step.value === 3` 的逻辑改为调用 `loadPersonas()`。

- [ ] **Step 6: Step 5 保存方案**

把 `confirm()` 改为：

```js
async function confirm() {
  const niche = nicheOptions.value.find((n) => n.key === selectedNiche.value)
  const persona = personaOptions.value.find((p) => p.key === selectedPersona.value)
  await save({
    platformKey: form.platform,
    platformName: platforms.value.find((p) => p.key === form.platform)?.name,
    goal: form.goal,
    background: form.background,
    hasProduct: form.hasProduct,
    productDesc: form.productDesc,
    nicheKey: niche?.key,
    nicheName: niche?.name,
    personaKey: persona?.key,
    personaName: persona?.name,
    pillars: pillars.map((p) => ({ name: p.name, percent: p.percent })),
    isRecommendedByAI: form.recommendedByAI,
    recommendationContext: buildContext({ ...recommendForm, timePerWeek: form.timePerWeek })
  })
  message.success('自媒体方案已生成')
  localStorage.setItem('aichuangzuo_onboarding_done', '1')
  router.push('/console/workbench')
}
```

- [ ] **Step 7: Commit**

```bash
git add project/user/web/src/views/console/OnboardingIndex.vue
git commit -m "feat(user-web): 自媒体方案向导对接后端 AI 接口"
```

---

### Task 10: 改造 `WorkbenchIndex.vue` 读取后端方案

**Files:**
- Modify: `project/user/web/src/views/console/WorkbenchIndex.vue`

**Interfaces:**
- Consumes: `useSelfMediaPlan`。

- [ ] **Step 1: 引入 composable 并把 plan 改为响应式**

```js
import { useSelfMediaPlan } from '@/composables/useSelfMediaPlan.js'

const { plan: currentPlan, loadPlan } = useSelfMediaPlan()

onMounted(async () => {
  todayDone.value = localStorage.getItem(todayKey.value) === '1'
  await loadPlan()
})
```

- [ ] **Step 2: 把模板中硬编码 `plan.xxx` 改为 `currentPlan.value.xxx`**

例如：

```html
<span class="plan-value plan-platform">{{ currentPlan.value?.platform }}</span>
```

如果 `currentPlan.value` 为 null，显示占位文案「未制定」。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/views/console/WorkbenchIndex.vue
git commit -m "feat(user-web): 工作台从后端读取自媒体方案"
```

---

### Task 11: 补充测试

**Files:**
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/selfmedia/controller/SelfMediaPlanControllerTest.java`
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanAiServiceTest.java`（可选，mock RestTemplate）

- [ ] **Step 1: Controller 测试**

```java
package com.aichuangzuo.user.modules.selfmedia.controller;

import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SelfMediaPlanController.class)
class SelfMediaPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SelfMediaPlanService planService;

    @Test
    void getCurrentPlan_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/user/self-media-plans/current")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: 运行全量用户端测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn test
```

Expected: 无新增失败。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/selfmedia
git commit -m "test(user): 补充自媒体方案 Controller 测试"
```

---

### Task 12: 联调与验证

**Files:** 无新增文件。

- [ ] **Step 1: 启动用户端后端**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
mvn spring-boot:run
```

- [ ] **Step 2: 启动用户端前端**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run dev
```

- [ ] **Step 3: 手动验证清单**

1. 登录后访问 `/console/onboarding`。
2. 选择「AI 推荐」并填写问卷，返回平台推荐结果。
3. 选择平台 → 填写背景 → 点击「获取 AI 目标推荐」→ 选择目标。
4. 点击下一步 → 返回赛道选项 → 选择赛道。
5. 点击下一步 → 返回人设选项和默认支柱 → 选择人设。
6. 点击确认 → 保存成功 → 跳转工作台。
7. 工作台「运营方案」卡片显示保存的平台/赛道/人设/目标/支柱。
8. 管理后台修改 `c_ai_prompt` 中任意提示词后，重新进入向导步骤，输出按新提示词生效。

- [ ] **Step 4: Commit 任何修复**

联调中的修复按问题单独 commit，例如：

```bash
git commit -m "fix(user): 修复自媒体方案 JSON 字段解析空指针"
```

---

## 计划自检

1. **Spec 覆盖：** 数据库表、4 条 AI 提示词、6 个后端接口、前端向导改造、工作台读取均有对应任务。
2. **Placeholder 扫描：** 无 TBD/TODO；Task 7 的 SQL 明确提示复制设计文档完整 prompt 文本。
3. **类型一致性：** `PillarVO`、`GoalOptionVO` 等字段与 API 设计文档、Service 解析逻辑一致；`SavePlanRequest` 与 `SelfMediaPlan` 字段映射一致。

---

## 风险与回退

- 若 LLM 输出 JSON 不稳定，`SelfMediaPlanAiService` 已走 `LlmJsonParser` 做围栏剥离和对象提取；仍失败时返回 113002。
- 若前端某一步 AI 接口失败，显示错误提示并允许重试；第一版不强制本地硬编码兜底。
- 若 `c_ai_prompt` 缺失某条提示词，`AiPromptRenderService` 会抛 240101，前端提示联系管理员。
