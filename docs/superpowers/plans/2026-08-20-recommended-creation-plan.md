# 按小爱推荐的方式创作（AI 驱动版）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把用户端工作台「按小爱推荐的方式创作」从硬编码向导改造为每步调用 AI、下一步基于上一步结果、生成成功后清除会话的真实创作流程。

**Architecture:** 新增 `u_recommended_creation_session` 表和 `RecommendedCreationService`，通过 `SelfMediaPlanAiService` 调用 `c_ai_prompt` 中的 2 条提示词生成选题与观点；前端 `CreateFlowModal.vue` 按步骤调用后端接口保存选择，最终复用 `GenerationTaskService.submit()` 提交生成任务，`WorkbenchIndex.vue` 展示真实生成记录。

**Tech Stack:** JDK 17, Spring Boot 3.2.5, MyBatis-Plus, MySQL 8, Flyway, Vue 3 + Ant Design Vue + Vite.

## Global Constraints

- 后端代码放在 `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/`。
- 用户端表前缀必须为 `u_`，字段全小写下划线分隔。
- Flyway 脚本版本号不能重复；用户端当前最新 `V1.0.0_105`。
- AI 提示词统一使用 `{{variableName}}` 占位符，通过 `AiPromptRenderService.render(code, variables)` 渲染。
- 所有用户数据查询必须带 `user_id` 过滤。
- 不引入新中间件；复用现有 `a_model_config`、`SelfMediaPlanAiService` 调用 LLM。
- 前端 API 封装在 `project/user/web/src/api/*.js`。
- 单元测试优先；每次任务完成后 commit。
- 不用的代码开发结束后必须删掉（临时 console.log、占位、未用 import、旧模拟逻辑）。

---

## 文件结构

| 文件 | 职责 |
|---|---|
| `V1.0.0_106__create_recommended_creation_session_table.sql` | 创建会话表 |
| `V1.0.0_107__seed_recommended_creation_prompts.sql` | 插入 2 条 AI prompt |
| `RecommendedCreationSession.java` | 会话实体 |
| `RecommendedCreationSessionMapper.java` | 会话数据访问 |
| `RecommendedCreationService.java` / `impl` | 业务逻辑：生成选题、生成观点、提交任务、清除会话 |
| `RecommendedCreationController.java` | 6 个 HTTP 接口 |
| `RecommendedCreation*Request.java` / `VO.java` | DTO/VO |
| `RecommendedCreationServiceImplTest.java` | 服务单元测试 |
| `recommendedCreation.js` | 前端 API 封装 |
| `CreateFlowModal.vue` | 改造为 AI 驱动向导 |
| `WorkbenchIndex.vue` | 移除模拟逻辑，接入真实生成记录 |

---

### Task 1: 创建会话表 Flyway 迁移

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_106__create_recommended_creation_session_table.sql`
- Test: 启动 user-api 后 Flyway 校验通过

**Interfaces:**
- Produces: 表 `u_recommended_creation_session` 可供后续 Entity/Mapper 使用。

- [ ] **Step 1: 编写迁移脚本**

```sql
CREATE TABLE IF NOT EXISTS u_recommended_creation_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    current_step TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '当前步骤：1选题 2观点 3字数 4提示词 5模板',
    topics_json JSON DEFAULT NULL COMMENT 'AI生成的选题列表',
    selected_topic_json JSON DEFAULT NULL COMMENT '用户选中的选题',
    angles_json JSON DEFAULT NULL COMMENT 'AI生成的观点角度列表',
    selected_angles_json JSON DEFAULT NULL COMMENT '用户选中的观点角度（最多3个）',
    word_count INT UNSIGNED DEFAULT NULL COMMENT '用户设置的字数',
    prompt VARCHAR(512) DEFAULT NULL COMMENT '用户选中的创作提示词',
    template VARCHAR(64) DEFAULT NULL COMMENT '用户选中的导出模板key',
    status VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT '会话状态：draft',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_recommended_creation_session_user_id (user_id),
    KEY idx_u_recommended_creation_session_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐创作会话表';
```

- [ ] **Step 2: 本地启动 user-api 验证迁移**

Run: `cd project/user/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`（或项目使用的等价命令）
Expected: 控制台无 Flyway checksum/迁移错误，`u_recommended_creation_session` 表存在。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_106__create_recommended_creation_session_table.sql
git commit -m "feat(db): 创建用户推荐创作会话表"
```

---

### Task 2: 插入 AI Prompt 种子数据

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_107__seed_recommended_creation_prompts.sql`
- Test: 启动后查询 `c_ai_prompt` 存在 2 条记录

**Interfaces:**
- Produces: prompt_code `recommend_creation_topics_v1` 和 `recommend_creation_angles_v1`。

- [ ] **Step 1: 编写迁移脚本**

```sql
INSERT INTO c_ai_prompt (prompt_code, category, name, system_role, user_prompt, description, is_enabled, version, tenant_id, created_by, updated_by)
VALUES (
    'recommend_creation_topics_v1',
    'recommended_creation',
    '小爱推荐创作选题',
    '你是一位资深的自媒体选题顾问，擅长根据账号定位生成低粉高赞、差异化的今日创作选题。请严格按 JSON 格式返回，不要输出额外说明。',
    '用户运营方案如下：\n- 主攻平台：{{platform}}\n- 细分赛道：{{niche}}\n- 人设定位：{{persona}}\n- 内容支柱：{{pillars}}\n\n请基于以上方案，推荐 6 个适合今日创作的选题。要求：\n1. 选题贴合赛道和人设，有爆款潜质。\n2. 给出同质化风险等级（low/medium/high）及标签。\n3. 给出参考案例数量和推荐切入角度。\n\n返回 JSON 数组，每个元素包含字段：id、title、risk、riskLabel、caseCount、recommendedAngle。',
    '基于运营方案生成今日创作选题',
    1, 1, 0, 0, 0
),
(
    'recommend_creation_angles_v1',
    'recommended_creation',
    '小爱推荐创作观点',
    '你是一位爆款文章角度策划，擅长为一个选题生成多个可组合使用的差异化观点。请严格按 JSON 格式返回，不要输出额外说明。',
    '用户运营方案如下：\n- 主攻平台：{{platform}}\n- 细分赛道：{{niche}}\n- 人设定位：{{persona}}\n- 内容支柱：{{pillars}}\n\n今日选题：{{topicTitle}}\n\n请围绕该选题生成 7 个观点/切入角度，用户可从中选择 1-3 个组合使用。每个角度要具体、可执行、有网感。\n\n返回 JSON 数组，每个元素包含字段：id、text。',
    '基于选题生成差异化观点角度',
    1, 1, 0, 0, 0
);
```

- [ ] **Step 2: 本地启动 user-api 验证**

Run: `mysql -u root -p ai_chuangzuo_user -e "SELECT prompt_code FROM c_ai_prompt WHERE category='recommended_creation';"`
Expected: 返回 `recommend_creation_topics_v1` 和 `recommend_creation_angles_v1`。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_107__seed_recommended_creation_prompts.sql
git commit -m "feat(db): 插入小爱推荐创作 prompt 种子"
```

---

### Task 3: 会话实体与 Mapper

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/entity/RecommendedCreationSession.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/mapper/RecommendedCreationSessionMapper.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/recommendedcreation/mapper/RecommendedCreationSessionMapperTest.java`（可选，若项目无 Mapper 测试传统可跳过）

**Interfaces:**
- Produces:
  - `RecommendedCreationSession` 实体，字段与表一一对应。
  - `RecommendedCreationSessionMapper.selectByUserId(Long userId)` 方法。

- [ ] **Step 1: 编写实体**

```java
package com.aichuangzuo.user.modules.recommendedcreation.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_recommended_creation_session")
public class RecommendedCreationSession extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer currentStep;
    private String topicsJson;
    private String selectedTopicJson;
    private String anglesJson;
    private String selectedAnglesJson;
    private Integer wordCount;
    private String prompt;
    private String template;
    private String status;
    private Long tenantId;
}
```

- [ ] **Step 2: 编写 Mapper**

```java
package com.aichuangzuo.user.modules.recommendedcreation.mapper;

import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecommendedCreationSessionMapper extends BaseMapper<RecommendedCreationSession> {

    @Select("SELECT * FROM u_recommended_creation_session WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    RecommendedCreationSession selectByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 3: 编译验证**

Run: `cd project/user/api && ./mvnw compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/
git commit -m "feat(recommended-creation): 新增会话实体与 Mapper"
```

---

### Task 4: 定义 DTO / VO

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/dto/request/GenerateAnglesRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/dto/request/UpdateSessionRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/vo/RecommendedCreationSessionVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/vo/TopicOptionVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/vo/AngleOptionVO.java`

**Interfaces:**
- Produces: 后续 Service / Controller 使用的类型。

- [ ] **Step 1: 编写请求 DTO**

```java
package com.aichuangzuo.user.modules.recommendedcreation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateAnglesRequest {

    @NotBlank(message = "选题ID不能为空")
    private String topicId;
}
```

```java
package com.aichuangzuo.user.modules.recommendedcreation.dto.request;

import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSessionRequest {

    @NotNull(message = "当前步骤不能为空")
    @Min(1)
    @Max(5)
    private Integer currentStep;

    @Min(100)
    @Max(3000)
    private Integer wordCount;

    @Size(max = 512)
    private String prompt;

    @Size(max = 64)
    private String template;

    private List<AngleOptionVO> selectedAngles;
}
```

- [ ] **Step 2: 编写 VO**

```java
package com.aichuangzuo.user.modules.recommendedcreation.vo;

import lombok.Data;
import java.util.List;

@Data
public class RecommendedCreationSessionVO {
    private Integer currentStep;
    private List<TopicOptionVO> topics;
    private TopicOptionVO selectedTopic;
    private List<AngleOptionVO> angles;
    private List<AngleOptionVO> selectedAngles;
    private Integer wordCount;
    private String prompt;
    private String template;
}
```

```java
package com.aichuangzuo.user.modules.recommendedcreation.vo;

import lombok.Data;

@Data
public class TopicOptionVO {
    private String id;
    private String title;
    private String risk;
    private String riskLabel;
    private Integer caseCount;
    private String recommendedAngle;
}
```

```java
package com.aichuangzuo.user.modules.recommendedcreation.vo;

import lombok.Data;

@Data
public class AngleOptionVO {
    private String id;
    private String text;
}
```

- [ ] **Step 3: 编译验证**

Run: `cd project/user/api && ./mvnw compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/dto project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/vo
git commit -m "feat(recommended-creation): 新增 DTO 与 VO"
```

---

### Task 5: 业务 Service 实现

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/service/RecommendedCreationService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/service/impl/RecommendedCreationServiceImpl.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/selfmedia/service/SelfMediaPlanService.java`（仅添加 `getCurrentPlan` 已存在，无需修改）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/recommendedcreation/service/RecommendedCreationServiceImplTest.java`

**Interfaces:**
- Consumes:
  - `SelfMediaPlanAiService.callPrompt(String promptCode, Map<String, Object> variables)` → `JsonNode`
  - `SelfMediaPlanService.getCurrentPlan(Long userId)` → `SelfMediaPlanVO`
  - `GenerationTaskService.submit(GenerationSubmitRequest req, Long userId)` → `GenerationTaskVO`
- Produces:
  - `RecommendedCreationSessionVO getSession(Long userId)`
  - `List<TopicOptionVO> generateTopics(Long userId)`
  - `List<AngleOptionVO> generateAngles(Long userId, String topicId)`
  - `void updateSession(Long userId, UpdateSessionRequest request)`
  - `GenerationTaskVO submitGeneration(Long userId)`
  - `void clearSession(Long userId)`

- [ ] **Step 1: 编写 Service 接口**

```java
package com.aichuangzuo.user.modules.recommendedcreation.service;

import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.RecommendedCreationSessionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;

import java.util.List;

public interface RecommendedCreationService {

    RecommendedCreationSessionVO getSession(Long userId);

    List<TopicOptionVO> generateTopics(Long userId);

    List<AngleOptionVO> generateAngles(Long userId, String topicId);

    void updateSession(Long userId, UpdateSessionRequest request);

    GenerationTaskVO submitGeneration(Long userId);

    void clearSession(Long userId);
}
```

- [ ] **Step 2: 编写 Service 实现（骨架）**

```java
package com.aichuangzuo.user.modules.recommendedcreation.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationSession;
import com.aichuangzuo.user.modules.recommendedcreation.mapper.RecommendedCreationSessionMapper;
import com.aichuangzuo.user.modules.recommendedcreation.service.RecommendedCreationService;
import com.aichuangzuo.user.modules.recommendedcreation.vo.*;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendedCreationServiceImpl implements RecommendedCreationService {

    private static final String PROMPT_TOPICS = "recommend_creation_topics_v1";
    private static final String PROMPT_ANGLES = "recommend_creation_angles_v1";

    private final SelfMediaPlanAiService aiService;
    private final SelfMediaPlanService planService;
    private final GenerationTaskService generationTaskService;
    private final RecommendedCreationSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendedCreationSessionVO getSession(Long userId) {
        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session == null) {
            return null;
        }
        return toVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TopicOptionVO> generateTopics(Long userId) {
        SelfMediaPlanVO plan = requirePlan(userId);
        Map<String, Object> vars = planVars(plan);
        JsonNode root = aiService.callPrompt(PROMPT_TOPICS, vars);
        List<TopicOptionVO> topics = parseTopics(root.path("topics").isMissingNode() ? root : root.path("topics"));

        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session == null) {
            session = newSession(userId);
        }
        session.setCurrentStep(1);
        session.setTopicsJson(toJson(topics));
        session.setSelectedTopicJson(null);
        session.setAnglesJson(null);
        session.setSelectedAnglesJson(null);
        save(session);
        return topics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AngleOptionVO> generateAngles(Long userId, String topicId) {
        RecommendedCreationSession session = requireSession(userId);
        TopicOptionVO topic = findTopic(session, topicId);
        session.setSelectedTopicJson(toJson(topic));

        SelfMediaPlanVO plan = requirePlan(userId);
        Map<String, Object> vars = planVars(plan);
        vars.put("topicTitle", topic.getTitle());

        JsonNode root = aiService.callPrompt(PROMPT_ANGLES, vars);
        List<AngleOptionVO> angles = parseAngles(root.path("angles").isMissingNode() ? root : root.path("angles"));

        session.setCurrentStep(2);
        session.setAnglesJson(toJson(angles));
        session.setSelectedAnglesJson(null);
        save(session);
        return angles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSession(Long userId, UpdateSessionRequest request) {
        RecommendedCreationSession session = requireSession(userId);
        session.setCurrentStep(request.getCurrentStep());
        if (request.getWordCount() != null) {
            session.setWordCount(request.getWordCount());
        }
        if (request.getPrompt() != null) {
            session.setPrompt(request.getPrompt());
        }
        if (request.getTemplate() != null) {
            session.setTemplate(request.getTemplate());
        }
        if (request.getSelectedAngles() != null) {
            session.setSelectedAnglesJson(toJson(request.getSelectedAngles()));
        }
        save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerationTaskVO submitGeneration(Long userId) {
        RecommendedCreationSession session = requireSession(userId);
        TopicOptionVO topic = fromJson(session.getSelectedTopicJson(), TopicOptionVO.class);
        List<AngleOptionVO> angles = fromJsonList(session.getSelectedAnglesJson(), AngleOptionVO.class);
        if (topic == null || angles == null || angles.isEmpty()) {
            throw new BusinessException("请先完成选题和观点选择");
        }
        if (session.getWordCount() == null || StringUtils.isBlank(session.getPrompt()) || StringUtils.isBlank(session.getTemplate())) {
            throw new BusinessException("请先完成字数、提示词和模板选择");
        }

        StringBuilder desc = new StringBuilder();
        desc.append("选题：").append(topic.getTitle()).append("\n");
        desc.append("观点：");
        for (int i = 0; i < angles.size(); i++) {
            desc.append(angles.get(i).getText());
            if (i < angles.size() - 1) desc.append("；");
        }
        desc.append("\n创作要求：").append(session.getPrompt());

        GenerationSubmitRequest req = new GenerationSubmitRequest();
        req.setTitle(topic.getTitle());
        req.setDescription(desc.toString());
        // 模板 key 约定为 "平台-样式"，如 xiaohongshu-default；若后续模板 key 变更需同步调整平台解析逻辑
        req.setPlatform(session.getTemplate().split("-")[0]);
        req.setWordCount(session.getWordCount());
        req.setTemplate(session.getTemplate());

        GenerationTaskVO task = generationTaskService.submit(req, userId);
        sessionMapper.deleteById(session.getId());
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearSession(Long userId) {
        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session != null) {
            sessionMapper.deleteById(session.getId());
        }
    }

    // ---- helpers ----

    private SelfMediaPlanVO requirePlan(Long userId) {
        SelfMediaPlanVO plan = planService.getCurrentPlan(userId);
        if (plan == null) {
            throw new BusinessException("请先制定自媒体运营方案");
        }
        return plan;
    }

    private RecommendedCreationSession requireSession(Long userId) {
        RecommendedCreationSession session = sessionMapper.selectByUserId(userId);
        if (session == null) {
            throw new BusinessException("创作会话不存在，请重新开始");
        }
        return session;
    }

    private RecommendedCreationSession newSession(Long userId) {
        RecommendedCreationSession s = new RecommendedCreationSession();
        s.setUserId(userId);
        s.setCurrentStep(1);
        s.setStatus("draft");
        s.setTenantId(0L);
        return s;
    }

    private void save(RecommendedCreationSession session) {
        if (session.getId() == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }
    }

    private Map<String, Object> planVars(SelfMediaPlanVO plan) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("platform", plan.getPlatformName());
        vars.put("niche", plan.getNicheName());
        vars.put("persona", plan.getPersonaName());
        vars.put("pillars", formatPillars(plan.getPillars()));
        return vars;
    }

    private String formatPillars(List<com.aichuangzuo.user.modules.selfmedia.vo.PillarVO> pillars) {
        if (pillars == null || pillars.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pillars.size(); i++) {
            sb.append(pillars.get(i).getName()).append(" ").append(pillars.get(i).getPercent()).append("%");
            if (i < pillars.size() - 1) sb.append("，");
        }
        return sb.toString();
    }

    private TopicOptionVO findTopic(RecommendedCreationSession session, String topicId) {
        List<TopicOptionVO> topics = fromJsonList(session.getTopicsJson(), TopicOptionVO.class);
        if (topics == null) {
            throw new BusinessException("选题列表为空，请重新生成选题");
        }
        return topics.stream()
                .filter(t -> topicId.equals(t.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("选题不存在"));
    }

    private List<TopicOptionVO> parseTopics(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isArray()) {
            log.warn("AI 选题返回格式异常");
            return List.of();
        }
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    TopicOptionVO vo = new TopicOptionVO();
                    vo.setId(n.path("id").asText(""));
                    vo.setTitle(n.path("title").asText(""));
                    vo.setRisk(n.path("risk").asText("medium"));
                    vo.setRiskLabel(n.path("riskLabel").asText(""));
                    vo.setCaseCount(n.path("caseCount").asInt(0));
                    vo.setRecommendedAngle(n.path("recommendedAngle").asText(""));
                    return vo;
                })
                .toList();
    }

    private List<AngleOptionVO> parseAngles(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isArray()) {
            log.warn("AI 观点返回格式异常");
            return List.of();
        }
        return StreamSupport.stream(node.spliterator(), false)
                .map(n -> {
                    AngleOptionVO vo = new AngleOptionVO();
                    vo.setId(n.path("id").asText(""));
                    vo.setText(n.path("text").asText(""));
                    return vo;
                })
                .toList();
    }

    private RecommendedCreationSessionVO toVO(RecommendedCreationSession session) {
        RecommendedCreationSessionVO vo = new RecommendedCreationSessionVO();
        vo.setCurrentStep(session.getCurrentStep());
        vo.setTopics(fromJsonList(session.getTopicsJson(), TopicOptionVO.class));
        vo.setSelectedTopic(fromJson(session.getSelectedTopicJson(), TopicOptionVO.class));
        vo.setAngles(fromJsonList(session.getAnglesJson(), AngleOptionVO.class));
        vo.setSelectedAngles(fromJsonList(session.getSelectedAnglesJson(), AngleOptionVO.class));
        vo.setWordCount(session.getWordCount());
        vo.setPrompt(session.getPrompt());
        vo.setTemplate(session.getTemplate());
        return vo;
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    private <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) return null;
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    private <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) return List.of();
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}
```

- [ ] **Step 3: 处理缺失的 import 和编译**

注意：`StreamSupport` 需要 `java.util.stream.StreamSupport`；`BusinessException` 在 `com.aichuangzuo.shared.exception`。

Run: `cd project/user/api && ./mvnw compile -q`
Expected: BUILD SUCCESS。

- [ ] **Step 4: 编写单元测试**

```java
package com.aichuangzuo.user.modules.recommendedcreation.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationSession;
import com.aichuangzuo.user.modules.recommendedcreation.mapper.RecommendedCreationSessionMapper;
import com.aichuangzuo.user.modules.recommendedcreation.service.impl.RecommendedCreationServiceImpl;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecommendedCreationServiceImplTest {

    private final SelfMediaPlanAiService aiService = mock(SelfMediaPlanAiService.class);
    private final SelfMediaPlanService planService = mock(SelfMediaPlanService.class);
    private final GenerationTaskService generationTaskService = mock(GenerationTaskService.class);
    private final RecommendedCreationSessionMapper sessionMapper = mock(RecommendedCreationSessionMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RecommendedCreationServiceImpl service() {
        return new RecommendedCreationServiceImpl(aiService, planService, generationTaskService, sessionMapper, objectMapper);
    }

    private SelfMediaPlanVO mockPlan() {
        SelfMediaPlanVO plan = new SelfMediaPlanVO();
        plan.setPlatformName("小红书");
        plan.setNicheName("35+ 职场转型");
        plan.setPersonaName("实战记录者");
        PillarVO p = new PillarVO();
        p.setName("干货复盘");
        p.setPercent(60);
        plan.setPillars(List.of(p));
        return plan;
    }

    @Test
    void generateTopics_shouldSaveSession() throws Exception {
        when(planService.getCurrentPlan(1L)).thenReturn(mockPlan());
        JsonNode root = objectMapper.readTree("{\"topics\":[{\"id\":\"t1\",\"title\":\"选题1\",\"risk\":\"low\",\"riskLabel\":\"低\",\"caseCount\":5,\"recommendedAngle\":\"角度\"}]}");
        when(aiService.callPrompt(any(), any())).thenReturn(root);
        when(sessionMapper.selectByUserId(1L)).thenReturn(null);

        List<TopicOptionVO> topics = service().generateTopics(1L);

        assertEquals(1, topics.size());
        ArgumentCaptor<RecommendedCreationSession> captor = ArgumentCaptor.forClass(RecommendedCreationSession.class);
        verify(sessionMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getCurrentStep());
        assertTrue(captor.getValue().getTopicsJson().contains("选题1"));
    }

    @Test
    void generateAngles_shouldSaveSelectedTopicAndAngles() throws Exception {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setTopicsJson(objectMapper.writeValueAsString(List.of(new TopicOptionVO() {{ setId("t1"); setTitle("选题1"); }})));
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);
        when(planService.getCurrentPlan(1L)).thenReturn(mockPlan());
        JsonNode root = objectMapper.readTree("{\"angles\":[{\"id\":\"a1\",\"text\":\"观点1\"}]}");
        when(aiService.callPrompt(any(), any())).thenReturn(root);

        List<AngleOptionVO> angles = service().generateAngles(1L, "t1");

        assertEquals(1, angles.size());
        assertEquals(2, session.getCurrentStep());
        assertTrue(session.getSelectedTopicJson().contains("选题1"));
    }

    @Test
    void submitGeneration_shouldCallGenerationAndDeleteSession() throws Exception {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setCurrentStep(5);
        session.setSelectedTopicJson(objectMapper.writeValueAsString(new TopicOptionVO() {{ setId("t1"); setTitle("选题1"); }}));
        session.setSelectedAnglesJson(objectMapper.writeValueAsString(List.of(new AngleOptionVO() {{ setId("a1"); setText("观点1"); }})));
        session.setWordCount(1500);
        session.setPrompt("提示词");
        session.setTemplate("xiaohongshu-default");
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);
        when(generationTaskService.submit(any(GenerationSubmitRequest.class), eq(1L))).thenReturn(new GenerationTaskVO());

        service().submitGeneration(1L);

        ArgumentCaptor<GenerationSubmitRequest> captor = ArgumentCaptor.forClass(GenerationSubmitRequest.class);
        verify(generationTaskService).submit(captor.capture(), eq(1L));
        assertEquals("选题1", captor.getValue().getTitle());
        assertEquals("xiaohongshu", captor.getValue().getPlatform());
        assertEquals(1500, captor.getValue().getWordCount());
        verify(sessionMapper).deleteById(1L);
    }

    @Test
    void submitGeneration_shouldThrowWhenSessionIncomplete() {
        RecommendedCreationSession session = new RecommendedCreationSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setCurrentStep(2);
        when(sessionMapper.selectByUserId(1L)).thenReturn(session);

        assertThrows(BusinessException.class, () -> service().submitGeneration(1L));
    }
}
```

- [ ] **Step 5: 运行单元测试**

Run: `cd project/user/api && ./mvnw test -Dtest=RecommendedCreationServiceImplTest -q`
Expected: BUILD SUCCESS，4 个测试全部通过。

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/ project/user/api/src/test/java/com/aichuangzuo/user/modules/recommendedcreation/
git commit -m "feat(recommended-creation): 新增业务 Service 及单元测试"
```

---

### Task 6: Controller 接口

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/controller/RecommendedCreationController.java`
- Test: 用 curl / Postman / Swagger 验证接口

**Interfaces:**
- Consumes: `RecommendedCreationService` 的全部方法。
- Produces: `/api/v1/user/recommended-creation/*` 6 个端点。

- [ ] **Step 1: 编写 Controller**

```java
package com.aichuangzuo.user.modules.recommendedcreation.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.GenerateAnglesRequest;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.service.RecommendedCreationService;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.RecommendedCreationSessionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户端-小爱推荐创作")
@RestController
@RequestMapping("/api/v1/user/recommended-creation")
@RequiredArgsConstructor
public class RecommendedCreationController {

    private final RecommendedCreationService recommendedCreationService;

    @GetMapping("/session")
    public Result<RecommendedCreationSessionVO> getSession() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(recommendedCreationService.getSession(userId));
    }

    @PostMapping("/topics")
    public Result<List<TopicOptionVO>> generateTopics() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(recommendedCreationService.generateTopics(userId));
    }

    @PostMapping("/angles")
    public Result<List<AngleOptionVO>> generateAngles(@Valid @RequestBody GenerateAnglesRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(recommendedCreationService.generateAngles(userId, request.getTopicId()));
    }

    @PatchMapping("/session")
    public Result<Void> updateSession(@Valid @RequestBody UpdateSessionRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        recommendedCreationService.updateSession(userId, request);
        return Result.success();
    }

    @PostMapping("/submit")
    public Result<GenerationTaskVO> submitGeneration() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(recommendedCreationService.submitGeneration(userId));
    }

    @DeleteMapping("/session")
    public Result<Void> clearSession() {
        Long userId = SecurityUserContext.getCurrentUserId();
        recommendedCreationService.clearSession(userId);
        return Result.success();
    }
}
```

- [ ] **Step 2: 编译并启动服务**

Run: `cd project/user/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
Expected: 启动成功，Swagger 中可见 `用户端-小爱推荐创作` 6 个接口。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/recommendedcreation/controller/RecommendedCreationController.java
git commit -m "feat(recommended-creation): 新增 Controller 接口"
```

---

### Task 7: 前端 API 封装

**Files:**
- Create: `project/user/web/src/api/recommendedCreation.js`
- Test: 在浏览器控制台或临时页面调用验证

**Interfaces:**
- Produces: 6 个前端 API 函数供 `CreateFlowModal.vue` 使用。

- [ ] **Step 1: 编写 API 文件**

```javascript
import request from '@/utils/request'

export function getRecommendedCreationSession() {
  return request.get('/recommended-creation/session').then((res) => res.data)
}

export function generateRecommendedTopics() {
  return request.post('/recommended-creation/topics').then((res) => res.data)
}

export function generateRecommendedAngles(topicId) {
  return request.post('/recommended-creation/angles', { topicId }).then((res) => res.data)
}

export function updateRecommendedSession(data) {
  return request.patch('/recommended-creation/session', data).then((res) => res.data)
}

export function submitRecommendedGeneration() {
  return request.post('/recommended-creation/submit').then((res) => res.data)
}

export function clearRecommendedSession() {
  return request.delete('/recommended-creation/session').then((res) => res.data)
}
```

- [ ] **Step 2: Commit**

```bash
git add project/user/web/src/api/recommendedCreation.js
git commit -m "feat(recommended-creation): 前端 API 封装"
```

---

### Task 8: 改造 CreateFlowModal.vue

**Files:**
- Modify: `project/user/web/src/views/console/create/CreateFlowModal.vue`
- Test: 启动前端 dev server，手动走通 5 步流程

**Interfaces:**
- Consumes: `recommendedCreation.js` 的 6 个函数。
- Produces: 弹框打开时恢复会话或生成选题；每步选择后调用后端；最终 `emit('success', task)`。

- [ ] **Step 1: 替换硬编码数据为 API 驱动**

主要改造点（不提供完整文件，按以下逻辑修改）：

1. 引入 API：

```javascript
import {
  getRecommendedCreationSession,
  generateRecommendedTopics,
  generateRecommendedAngles,
  updateRecommendedSession,
  submitRecommendedGeneration
} from '@/api/recommendedCreation.js'
```

2. 打开弹框时：

```javascript
async function init() {
  reset()
  loading.value = true
  try {
    const session = await getRecommendedCreationSession()
    if (session) {
      restoreSession(session)
    } else {
      const data = await generateRecommendedTopics()
      topicOptions.value = data.topics || []
    }
  } catch (err) {
    message.error(err?.message || '加载失败')
  } finally {
    loading.value = false
  }
}
```

3. 第 1 步选中选题后，进入第 2 步时调用 `generateRecommendedAngles`：

```javascript
async function goToAngles() {
  if (!flowData.selectedTopic) return
  loading.value = true
  try {
    const data = await generateRecommendedAngles(flowData.selectedTopic.id)
    generatedAngleList.value = data.angles || []
    flowData.step = 2
  } catch (err) {
    message.error(err?.message || '生成观点失败')
  } finally {
    loading.value = false
  }
}
```

4. 第 2 步→第 3 步保存 `selectedAngles`：

```javascript
await updateRecommendedSession({
  currentStep: 3,
  selectedAngles: selectedAngleObjects // 数组
})
```

5. 第 3/4/5 步分别调用 `updateRecommendedSession` 保存 `wordCount`、`prompt`、`template`。

6. 第 5 步点击生成：

```javascript
async function finish() {
  loading.value = true
  try {
    const task = await submitRecommendedGeneration()
    message.success('已加入生成队列')
    emit('success', task)
    close()
  } catch (err) {
    message.error(err?.message || '提交失败')
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 2: 删除不再使用的硬编码数组和模拟逻辑**

删除 `topicOptions` 硬编码、`generatedAngleList` 硬编码 watch、`minePrompts`/`learnPrompts`/`favoritePrompts`/`systemPrompts` 等常量若仍用于提示词展示则保留，但改为从后端或本地常量均可；本任务重点删除模拟数据。

- [ ] **Step 3: 启动前端并验证**

Run: `cd project/user/web && npm run dev`（或项目等价命令）
Expected:
- 打开工作台 → 按小爱推荐创作 → 展示 loading → 出现 AI 生成的选题。
- 选中选题 → 出现 AI 生成的观点。
- 选择字数/提示词/模板 → 点击生成 → 关闭弹框。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/create/CreateFlowModal.vue
git commit -m "feat(recommended-creation): CreateFlowModal 接入后端 AI 流程"
```

---

### Task 9: 改造 WorkbenchIndex.vue

**Files:**
- Modify: `project/user/web/src/views/console/WorkbenchIndex.vue`
- Test: 生成成功后工作台生成记录列表出现新任务

**Interfaces:**
- Consumes: `CreateFlowModal` 的 `success` 事件返回的 `task`。
- Produces: 调用 `loadGenerationRecords()` 刷新列表，标记今日完成。

- [ ] **Step 1: 修改 onCreateStart**

```javascript
function onCreateStart(task) {
  setTodayDone()
  loadGenerationRecords()
  if (task?.id) {
    message.success('文章生成任务已创建')
  }
}
```

- [ ] **Step 2: 删除 simulateGeneration 和 createGenerationRecord 模拟逻辑**

删除以下函数：
- `createGenerationRecord(payload)`
- `simulateGeneration(record)`

若 `createGenerationRecord` 仍被 `onFreeCreateSuccess` 引用，则一并改为只调用 `loadGenerationRecords()`。

- [ ] **Step 3: 验证**

生成成功后，工作台「生成记录」卡片应出现新的「排队中/生成中」记录。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/WorkbenchIndex.vue
git commit -m "feat(workbench): 移除模拟生成逻辑，接入真实生成记录"
```

---

### Task 10: 端到端验证

**Files:**
- Create: `tests/e2e/verify_recommended_creation.py`（参考 `verify_template.py` 风格）
- 或手动浏览器验证

**验证清单：**

- [ ] 无会话时打开「按小爱推荐的方式创作」，自动调用 AI 生成选题。
- [ ] 选题生成失败时显示「重新生成」按钮，不闪退。
- [ ] 选中选题后生成观点，观点基于选题内容。
- [ ] 刷新页面后恢复到当前步骤和已选数据。
- [ ] 选择字数/提示词/模板后点击生成，成功加入生成队列。
- [ ] 生成成功后再次打开弹框，重新从第 1 步开始。
- [ ] 自由创作功能不受影响。

- [ ] **Step 1: 编写 Playwright 验证脚本（可选）**

若团队要求自动化验证，新增：

```python
# tests/e2e/verify_recommended_creation.py
# 启动前端 dev server 后运行，验证弹框渲染和选题列表非空
```

- [ ] **Step 2: Commit**

```bash
git add tests/e2e/verify_recommended_creation.py
git commit -m "test(e2e): 小爱推荐创作端到端验证"
```

---

## Self-Review Checklist

- [ ] Spec coverage：数据库表、2 条 prompt、6 个接口、前端改造、生成成功后清除会话均已覆盖。
- [ ] Placeholder scan：无 TBD/TODO，所有代码块为可直接运行的骨架。
- [ ] Type consistency：`UpdateSessionRequest` 与 Service 中字段名一致（含 `selectedAngles`）；`TopicOptionVO` / `AngleOptionVO` 与前端原型字段一致。
- [ ] 无未引用代码：`simulateGeneration` 等旧模拟逻辑需在 Task 9 删除。
- [ ] 跨模块依赖合法：`recommendedcreation` → `selfmedia` 同端横向依赖，符合规范。

---

## 执行方式

计划完成后，两种方式可选：

1. **Subagent-Driven（推荐）**：每个 Task 派独立 subagent，中间 review。
2. **Inline Execution**：在当前会话按 Task 顺序直接执行。
