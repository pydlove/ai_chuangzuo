# 提示词市场/个人提示词使用计数实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把生成任务成功时的提示词使用计数从 `PersistArticleStep` 同步调用改为 admin 消息 outbox 异步派发，区分市场提示词（计数+收益）与个人提示词（仅计数），失败/停止不计数。

**Architecture:** 复用 `a_message_notify_outbox` + `NotifyOutboxDispatcherJob`，新增 `skill_usage` bizType；admin 在 `markCompleted` 事务内写入 outbox，user-api 内部接口解析来源后分别更新 `u_skill_market` 或 `u_user_skill`。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + MySQL 8 + Mockito；Vue 3 + Ant Design Vue。

## Global Constraints

- 不新增表，复用 `a_message_notify_outbox`、`u_skill_market`、`u_user_skill`、`u_earnings_record`。
- 失败和手动停止的任务不写入 `skill_usage` outbox，自然不计数。
- 个人提示词定义为 `u_user_skill.source_type` 为 `1`（自定义）或 `2`（学习）；系统预设（`source_type = 3`）不计数、不产生收益。
- 所有 DB 迁移遵循 `docs/architecture/mysql-table-conventions.md`。
- 后端单测使用 Mockito；user-api 与 admin-api 分别独立测试。

---

## 文件结构

| 文件 | 责任 |
|---|---|
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/UserSkillService.java` | 新增 `incrementUseCount` 接口 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/UserSkillServiceImpl.java` | 实现 `incrementUseCount` |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/service/SkillUsageRecordService.java` | 新建：按 `skillRef` 解析来源并路由计数 |
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/controller/SkillUsageInternalController.java` | 新建：user-api 内部接口 `/internal/skills/use` |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/message/handler/SkillUsageHandler.java` | 新建：`skill_usage` outbox handler |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/SkillUsageInternalClient.java` | 新建/复用：admin → user-api HTTP 客户端 |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java` | `markCompleted` 中写入 `skill_usage` outbox |
| `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/PersistArticleStep.java` | 移除同步 `SkillMarketInternalClient.recordUsage()` 调用 |
| 前端提示词市场规则文案 | 补充「市场提示词产生收益，自己的提示词只计数」说明 |

---

## Task 1: user-api 增加个人提示词使用次数接口

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/UserSkillService.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/UserSkillServiceImpl.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/service/impl/UserSkillServiceImplTest.java`

**Interfaces:**
- Consumes: `UserSkillMapper`
- Produces: `void incrementUseCount(Long userId, String skillName)`

- [ ] **Step 1: 在接口中新增方法**

在 `UserSkillService.java` 中添加：

```java
    /**
     * 增加指定用户指定名称风格的累计使用次数。
     *
     * @param userId    用户ID
     * @param skillName 风格名称
     */
    void incrementUseCount(Long userId, String skillName);
```

- [ ] **Step 2: 实现方法**

在 `UserSkillServiceImpl.java` 中：

1. 引入 `LambdaUpdateWrapper` 和 `LocalDateTime`（若尚未引入）。
2. 添加实现：

```java
    @Override
    public void incrementUseCount(Long userId, String skillName) {
        if (userId == null || skillName == null || skillName.isBlank()) {
            return;
        }
        UserSkill skill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillName, skillName)
                        .eq(UserSkill::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (skill == null) {
            log.warn("增加风格使用次数失败，风格不存在 userId={} skillName={}", userId, skillName);
            return;
        }
        userSkillMapper.update(null, new LambdaUpdateWrapper<UserSkill>()
                .eq(UserSkill::getId, skill.getId())
                .setSql("use_count = use_count + 1")
                .set(UserSkill::getUpdatedAt, LocalDateTime.now()));
        log.info("增加风格使用次数 userId={} skillName={}", userId, skillName);
    }
```

- [ ] **Step 3: 编写测试**

在 `UserSkillServiceImplTest.java` 中新增：

```java
    @Test
    void incrementUseCount_shouldIncreaseUseCount() {
        Long userId = 1L;
        String skillName = "轻松";
        UserSkill skill = new UserSkill();
        skill.setId(10L);
        skill.setUserId(userId);
        skill.setSkillName(skillName);
        skill.setUseCount(5);

        when(userSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        userSkillService.incrementUseCount(userId, skillName);

        ArgumentCaptor<LambdaUpdateWrapper<UserSkill>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(userSkillMapper).update(isNull(), captor.capture());
        // 断言 SQL 片段包含 use_count + 1
        String sql = captor.getValue().getSqlSet();
        assertTrue(sql.contains("use_count = use_count + 1"));
    }

    @Test
    void incrementUseCount_shouldNoopWhenSkillNotFound() {
        when(userSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        userSkillService.incrementUseCount(1L, "不存在");

        verify(userSkillMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void incrementUseCount_shouldNoopWhenSkillNameBlank() {
        userSkillService.incrementUseCount(1L, "");
        verify(userSkillMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }
```

- [ ] **Step 4: 运行测试**

```bash
cd project/user/api
mvn test -Dtest=UserSkillServiceImplTest -q
```

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/UserSkillService.java
 git add project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/service/impl/UserSkillServiceImpl.java
 git add project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/service/impl/UserSkillServiceImplTest.java
git commit -m "$(cat <<'EOF'
feat(skill): 增加个人提示词使用次数接口

UserSkillService 新增 incrementUseCount，供异步计数调用。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: user-api 新建 SkillUsageRecordService 统一处理提示词使用计数

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/service/SkillUsageRecordService.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/market/service/SkillUsageRecordServiceTest.java`

**Interfaces:**
- Consumes: `SkillMarketMapper`, `UserSkillMapper`, `SkillMarketUsageService`, `UserSkillService`
- Produces: `void record(String skillRef, Long userId)`

- [ ] **Step 1: 新建服务类**

```java
package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 提示词使用统一计数服务。
 *
 * <p>按 skillRef 判定来源：市场提示词产生收益，个人提示词只计 use_count，系统预设不计。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillUsageRecordService {

    private static final int SOURCE_TYPE_SYSTEM_PRESET = 3;

    private final SkillMarketMapper skillMarketMapper;
    private final UserSkillMapper userSkillMapper;
    private final SkillMarketUsageService skillMarketUsageService;
    private final UserSkillService userSkillService;

    /**
     * 记录一次提示词使用。
     *
     * @param skillRef 提示词标识：市场为 biz_no，个人/系统预设为 skill_name
     * @param userId   使用者用户ID
     */
    public void record(String skillRef, Long userId) {
        if (skillRef == null || skillRef.isBlank() || userId == null) {
            return;
        }

        // 1. 优先判定市场提示词（biz_no 匹配）
        SkillMarket market = skillMarketMapper.selectOne(
                new LambdaQueryWrapper<SkillMarket>()
                        .eq(SkillMarket::getBizNo, skillRef)
                        .eq(SkillMarket::getAuditStatus, 1)
                        .eq(SkillMarket::getEnableStatus, 1)
                        .eq(SkillMarket::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (market != null) {
            skillMarketUsageService.recordUsage(skillRef, userId);
            return;
        }

        // 2. 再判定个人提示词（用户自己的自定义或学习风格）
        UserSkill userSkill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillName, skillRef)
                        .eq(UserSkill::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (userSkill != null) {
            if (SOURCE_TYPE_SYSTEM_PRESET == userSkill.getSourceType()) {
                log.info("系统预设风格不计数 userId={} skillName={}", userId, skillRef);
                return;
            }
            userSkillService.incrementUseCount(userId, skillRef);
            return;
        }

        log.warn("未找到对应提示词来源，不计数 userId={} skillRef={}", userId, skillRef);
    }
}
```

- [ ] **Step 2: 编写测试**

```java
package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillUsageRecordServiceTest {

    @Mock
    private SkillMarketMapper skillMarketMapper;
    @Mock
    private UserSkillMapper userSkillMapper;
    @Mock
    private SkillMarketUsageService skillMarketUsageService;
    @Mock
    private UserSkillService userSkillService;

    @InjectMocks
    private SkillUsageRecordService service;

    @Test
    void record_shouldCountMarketSkillAndEarnings() {
        SkillMarket market = new SkillMarket();
        market.setBizNo("SK123");
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(market);

        service.record("SK123", 1L);

        verify(skillMarketUsageService).recordUsage("SK123", 1L);
        verify(userSkillMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void record_shouldCountUserSkillOnly() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        UserSkill skill = new UserSkill();
        skill.setSkillName("轻松");
        skill.setSourceType(1);
        when(userSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        service.record("轻松", 1L);

        verify(skillMarketUsageService, never()).recordUsage(any(), any());
        verify(userSkillService).incrementUseCount(1L, "轻松");
    }

    @Test
    void record_shouldNoopForSystemPreset() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        UserSkill skill = new UserSkill();
        skill.setSkillName("正式");
        skill.setSourceType(3);
        when(userSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        service.record("正式", 1L);

        verify(skillMarketUsageService, never()).recordUsage(any(), any());
        verify(userSkillService, never()).incrementUseCount(any(), any());
    }

    @Test
    void record_shouldNoopWhenSkillRefBlank() {
        service.record("", 1L);
        verify(skillMarketMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
cd project/user/api
mvn test -Dtest=SkillUsageRecordServiceTest -q
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/service/SkillUsageRecordService.java
 git add project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/market/service/SkillUsageRecordServiceTest.java
git commit -m "$(cat <<'EOF'
feat(skill): 统一提示词使用计数服务

SkillUsageRecordService 按来源路由：市场提示词计数+收益，
个人提示词只计 use_count，系统预设不计数。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: user-api 新增 `/internal/skills/use` 内部接口

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/controller/SkillUsageInternalController.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/market/controller/SkillUsageInternalControllerTest.java`

**Interfaces:**
- Consumes: `SkillUsageRecordService`
- Produces: `POST /api/v1/user/internal/skills/use` 内部接口

- [ ] **Step 1: 新建 Controller**

```java
package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.skill.market.service.SkillUsageRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端内部接口：供管理端 outbox dispatcher 调用，记录提示词使用。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/internal/skills")
@RequiredArgsConstructor
public class SkillUsageInternalController {

    private final SkillUsageRecordService skillUsageRecordService;

    @PostMapping("/use")
    public Result<Void> recordUsage(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String skillRef = asString(payload.get("skillRef"));
        if (userId == null || skillRef == null || skillRef.isBlank()) {
            log.warn("记录提示词使用入参缺失 taskId={} userId={} skillRef={}", taskId, userId, skillRef);
            return Result.success();
        }
        skillUsageRecordService.record(skillRef, userId);
        return Result.success();
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
```

- [ ] **Step 2: 编写测试**

```java
package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.skill.market.service.SkillUsageRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkillUsageInternalControllerTest {

    @Mock
    private SkillUsageRecordService skillUsageRecordService;

    @InjectMocks
    private SkillUsageInternalController controller;

    @Test
    void recordUsage_shouldCallService() {
        Map<String, Object> payload = Map.of(
                "taskId", 100L,
                "userId", 10L,
                "skillRef", "SK123"
        );

        Result<Void> result = controller.recordUsage(payload);

        assertEquals(0, result.getCode());
        verify(skillUsageRecordService).record("SK123", 10L);
    }

    @Test
    void recordUsage_shouldNoopWhenSkillRefMissing() {
        Map<String, Object> payload = Map.of("taskId", 100L, "userId", 10L);

        Result<Void> result = controller.recordUsage(payload);

        assertEquals(0, result.getCode());
        verify(skillUsageRecordService, never()).record(any(), any());
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
cd project/user/api
mvn test -Dtest=SkillUsageInternalControllerTest -q
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/skill/market/controller/SkillUsageInternalController.java
 git add project/user/api/src/test/java/com/aichuangzuo/user/modules/skill/market/controller/SkillUsageInternalControllerTest.java
git commit -m "$(cat <<'EOF'
feat(skill): 新增提示词使用计数内部接口

POST /api/v1/user/internal/skills/use 供 admin outbox dispatcher 调用。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: admin 新增 `SkillUsageHandler` 与 HTTP 客户端

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/message/handler/SkillUsageHandler.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/SkillUsageInternalClient.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/message/handler/SkillUsageHandlerTest.java`

**Interfaces:**
- Consumes: `NotifyOutbox` row, `user.api.base-url`, `user.api.internal-key`
- Produces: `String bizType() = "skill_usage"`, `void dispatch(NotifyOutbox row)`

- [ ] **Step 1: 新建 HTTP 客户端**

```java
package com.aichuangzuo.admin.modules.generation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin → User 内部 HTTP 客户端：记录提示词使用。
 *
 * <p>调用 user-api 的 {@code /api/v1/user/internal/skills/use}。
 */
@Slf4j
@Service
public class SkillUsageInternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalKey;

    public SkillUsageInternalClient(@Value("${user.api.base-url:}") String baseUrl,
                                    @Value("${user.api.internal-key:}") String internalKey) {
        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.restTemplate = new RestTemplate();
    }

    public void recordUsage(Long taskId, Long userId, String skillRef) {
        if (userId == null || skillRef == null || skillRef.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey == null ? "" : internalKey);

        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("userId", userId);
        body.put("skillRef", skillRef);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/v1/user/internal/skills/use",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response == null || !Integer.valueOf(0).equals(response.get("code"))) {
                log.warn("记录提示词使用失败 user-api 响应: {}", response);
            } else {
                log.info("记录提示词使用完成 taskId={} userId={} skillRef={}", taskId, userId, skillRef);
            }
        } catch (RestClientException e) {
            log.warn("调用 user-api 记录提示词使用失败：{}", e.getMessage());
            throw e;
        }
    }
}
```

- [ ] **Step 2: 新建 Handler**

```java
package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.generation.service.SkillUsageInternalClient;
import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 提示词使用计数 outbox 处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillUsageHandler implements MessageNotifyHandler {

    public static final String BIZ_TYPE = "skill_usage";

    private final SkillUsageInternalClient usageClient;
    private final ObjectMapper objectMapper;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void dispatch(NotifyOutbox row) {
        Map<String, Object> payload = parse(row.getPayload());
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String skillRef = asString(payload.get("skillRef"));

        if (userId == null || skillRef == null || skillRef.isBlank()) {
            throw new IllegalArgumentException(
                    "skill_usage payload 缺失必填字段：userId/skillRef");
        }
        usageClient.recordUsage(taskId, userId, skillRef);
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 skill_usage payload 失败: " + e.getMessage(), e);
        }
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
```

- [ ] **Step 3: 编写 Handler 测试**

```java
package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.generation.service.SkillUsageInternalClient;
import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkillUsageHandlerTest {

    @Mock
    private SkillUsageInternalClient usageClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SkillUsageHandler handler;

    @Test
    void dispatch_shouldCallClient() throws Exception {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload(objectMapper.writeValueAsString(Map.of(
                "taskId", 100L,
                "userId", 10L,
                "skillRef", "SK123"
        )));

        handler.dispatch(row);

        verify(usageClient).recordUsage(100L, 10L, "SK123");
    }

    @Test
    void dispatch_shouldThrowWhenSkillRefMissing() throws Exception {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload(objectMapper.writeValueAsString(Map.of("userId", 10L)));

        assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd project/admin/api
mvn test -Dtest=SkillUsageHandlerTest -q
```

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/message/handler/SkillUsageHandler.java
 git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/SkillUsageInternalClient.java
 git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/message/handler/SkillUsageHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(admin): 新增 skill_usage outbox handler 与内部客户端

SkillUsageHandler 将 skill_usage outbox 派发到 user-api。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: admin `GenerationTaskService.markCompleted` 写入 `skill_usage` outbox

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceTest.java`

**Interfaces:**
- Consumes: `NotifyOutboxService`, `ObjectMapper`
- Produces: `skill_usage` outbox 写入

- [ ] **Step 1: 注入 ObjectMapper**

在 `GenerationTaskService.java` 中增加：

```java
    private final ObjectMapper objectMapper;
```

构造函数通过 `@RequiredArgsConstructor` 自动生成，因此只需加字段。

- [ ] **Step 2: 修改 `markCompleted` 方法**

在 `markCompleted(Long taskId, String articleBizNo, String expectedLockedBy, Map<String, Object> notifyPayload)` 中，更新 task 后、写入 `generation_completed` outbox 前，增加：

```java
        if (notifyPayload != null) {
            insertSkillUsageOutbox(task);
            notifyOutboxService.insertPending("generation_completed", taskId, task.getTargetUserId(), notifyPayload);
        }
```

原代码：

```java
        if (notifyPayload != null) {
            notifyOutboxService.insertPending("generation_completed", taskId, task.getTargetUserId(), notifyPayload);
        }
```

替换为上述逻辑。

- [ ] **Step 3: 新增辅助方法**

在 `GenerationTaskService.java` 中添加：

```java
    private void insertSkillUsageOutbox(GenerationTask task) {
        if (task.getInputParam() == null || task.getInputParam().isBlank()) {
            return;
        }
        try {
            Map<String, Object> input = objectMapper.readValue(task.getInputParam(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            String skillRef = input.get("skillRef") == null ? null : input.get("skillRef").toString();
            if (skillRef == null || skillRef.isBlank()) {
                return;
            }
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("taskId", task.getId());
            payload.put("userId", task.getTargetUserId());
            payload.put("skillRef", skillRef);
            notifyOutboxService.insertPending("skill_usage", task.getId(), task.getTargetUserId(), payload);
        } catch (Exception e) {
            log.warn("task={} 解析 inputParam 提取 skillRef 失败: {}", task.getId(), e.getMessage());
        }
    }
```

- [ ] **Step 4: 更新测试**

在 `GenerationTaskServiceTest.java` 中新增：

```java
    @Mock
    private ObjectMapper objectMapper;
```

修改现有 `markCompleted_shouldSetStatusAndCompletedAt` 测试（因为该测试调用的是无 notifyPayload 的重载，不会触发 outbox，所以只需确保不报错即可）。

新增测试：

```java
    @Test
    void markCompleted_shouldInsertSkillUsageOutboxWhenSkillRefPresent() throws Exception {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setTargetUserId(10L);
        task.setInputParam("{\"skillRef\":\"SK123\"}");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(objectMapper.readValue(eq(task.getInputParam()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("skillRef", "SK123"));

        Map<String, Object> notifyPayload = Map.of("taskId", 1L, "userId", 10L);
        taskService.markCompleted(1L, "ART123", null, notifyPayload);

        verify(notifyOutboxService).insertPending(eq("skill_usage"), eq(1L), eq(10L), any(Map.class));
        verify(notifyOutboxService).insertPending("generation_completed", 1L, 10L, notifyPayload);
    }

    @Test
    void markCompleted_shouldNotInsertSkillUsageOutboxWhenSkillRefMissing() throws Exception {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setTargetUserId(10L);
        task.setInputParam("{}");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(objectMapper.readValue(eq(task.getInputParam()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of());

        Map<String, Object> notifyPayload = Map.of("taskId", 1L, "userId", 10L);
        taskService.markCompleted(1L, "ART123", null, notifyPayload);

        verify(notifyOutboxService, never()).insertPending(eq("skill_usage"), any(), any(), any(Map.class));
    }
```

- [ ] **Step 5: 运行测试**

```bash
cd project/admin/api
mvn test -Dtest=GenerationTaskServiceTest -q
```

Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskService.java
 git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/GenerationTaskServiceTest.java
git commit -m "$(cat <<'EOF'
feat(admin): 生成成功时写入 skill_usage outbox

markCompleted 事务内同时写入 skill_usage 与 generation_completed outbox。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: 移除 `PersistArticleStep` 中的同步市场提示词使用调用

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/PersistArticleStep.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/PersistArticleStepTest.java`

**Interfaces:**
- Consumes: `ArticleWriteInternalClient`
- Produces: 仅持久化 article，不再调用市场提示词使用接口

- [ ] **Step 1: 修改 `PersistArticleStep`**

1. 移除 `SkillMarketInternalClient` 字段与构造参数。
2. 移除 `process` 方法中关于 `skillRef` 的同步调用代码块。

修改后的类核心代码：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PersistArticleStep implements GenerationStep {

    private final ArticleWriteInternalClient articleClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int stageIndex() { return 100; }

    @Override
    public String name() { return "persist-article"; }

    @Override
    public boolean enabled(GenerationContext ctx) {
        return ctx.getFinalDraftJson() != null && !ctx.getFinalDraftJson().isBlank();
    }

    @Override
    public StepResult process(GenerationContext ctx) {
        GenerationTask task = ctx.getTask();
        if (task == null) {
            throw new RuntimeException("ctx.task 为空，无法持久化 article");
        }
        Map<String, Object> in = ctx.getInput() == null ? Map.of() : ctx.getInput();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("userId", task.getTargetUserId());
        payload.put("title", in.get("title"));
        payload.put("body", ctx.getExportResult() == null ? null
                : PipelineUtils.normalizeQuotes(ctx.getExportResult().getRenderedDocument()));
        payload.put("summary", in.get("description"));
        payload.put("wordCount", resolveWordCount(ctx));
        payload.put("platform", in.get("platform"));
        payload.put("skill", in.get("skillRef"));
        payload.put("template", in.get("template"));
        payload.put("description", PipelineUtils.normalizeQuotes(ctx.getPublishDescription()));
        payload.put("tags", ctx.getPublishTags());
        payload.put("inputParam", task.getInputParam());
        payload.put("wordLimitTarget", task.getWordLimitTarget() == null ? 1500 : task.getWordLimitTarget());

        String articleBizNo = articleClient.saveArticle(payload);
        ctx.setArticleBizNo(articleBizNo);
        log.info("article 持久化完成 task={} articleBizNo={}", task.getId(), articleBizNo);

        return StepResult.CONTINUE;
    }
    // resolveWordCount / countWordsFromFinalDraft / countChars / isPunctuation 保持不变
}
```

- [ ] **Step 2: 更新测试**

在 `PersistArticleStepTest.java` 中：

1. 移除 `SkillMarketInternalClient` mock 与构造参数。
2. 更新 `PersistArticleStep step = new PersistArticleStep(articleClient);`。
3. 保持现有 article 持久化断言不变。

- [ ] **Step 3: 运行测试**

```bash
cd project/admin/api
mvn test -Dtest=PersistArticleStepTest -q
```

Expected: PASS

- [ ] **Step 4: 删除已弃用的 `SkillMarketInternalClient`**

确认 `SkillMarketInternalClient` 已无其他引用后删除：

```bash
rm project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/SkillMarketInternalClient.java
```

- [ ] **Step 5: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/SkillMarketInternalClient.java
 git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/PersistArticleStep.java
 git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/pipeline/steps/PersistArticleStepTest.java
git commit -m "$(cat <<'EOF'
refactor(admin): PersistArticleStep 不再同步记录市场提示词使用

计数已下沉到 markCompleted 后的 outbox 异步流程。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: 前端提示词市场规则文案更新

**Files:**
- Modify: 提示词市场规则弹框/帮助页面（如 `project/user/web/src/views/console/SkillMarketIndex.vue` 或相关规则组件）

**Interfaces:**
- Consumes: 现有规则弹框组件
- Produces: 更新的规则文案

- [ ] **Step 1: 定位规则弹框**

在 `SkillMarketIndex.vue` 或相关组件中找到「规则说明」弹框内容区域。

- [ ] **Step 2: 添加/更新规则条目**

在规则列表中增加或替换为：

```html
<li>使用提示词市场上的提示词生成文章并成功后，该提示词的累计使用次数与本月使用次数会增加，创作者可获得对应创作币收益。</li>
<li>使用自己创建或学习获得的提示词生成文章并成功后，仅累计该提示词的使用次数，不产生收益。</li>
<li>生成失败或手动停止的任务不计入使用次数与收益。</li>
```

- [ ] **Step 3: 本地验证**

启动前端 dev server：

```bash
cd project/user/web
npm run dev
```

打开提示词市场页面，点击「规则说明」/「收益规则」，确认三条规则展示正确。

- [ ] **Step 4: 提交**

```bash
git add project/user/web/src/views/console/SkillMarketIndex.vue
git commit -m "$(cat <<'EOF'
feat(web): 提示词市场规则说明补充收益与计数规则

说明市场提示词产生收益、个人提示词只计数、失败/停止不计数。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: 运行全量相关测试

**Files:**
- 无新增文件

- [ ] **Step 1: user-api 相关测试**

```bash
cd project/user/api
mvn test -Dtest=UserSkillServiceImplTest,SkillUsageRecordServiceTest,SkillUsageInternalControllerTest,GenerationTaskServiceTest -q
```

Expected: PASS

- [ ] **Step 2: admin-api 相关测试**

```bash
cd project/admin/api
mvn test -Dtest=SkillUsageHandlerTest,GenerationTaskServiceTest,PersistArticleStepTest -q
```

Expected: PASS

- [ ] **Step 3: 提交（如测试通过）**

如全量通过且此前已分任务提交，本步骤可不单独提交。

---

## Self-Review

### Spec Coverage

| 设计文档要求 | 对应 Task |
|---|---|
| 市场提示词生成成功后计数+收益 | Task 2, Task 5 |
| 个人提示词生成成功后只计数 | Task 1, Task 2, Task 5 |
| 系统预设不计数 | Task 2 |
| 失败/停止不计数 | Task 5（不写 outbox） |
| 异步 outbox 队列 | Task 4, Task 5 |
| 移除同步调用 | Task 6 |
| 前端规则文案 | Task 7 |

### Placeholder Scan

- 无 TBD / TODO。
- 所有步骤含具体代码、命令与预期结果。
- 测试代码完整。

### Type Consistency

- `incrementUseCount(Long userId, String skillName)` 在接口与实现中一致。
- `SkillUsageRecordService.record(String, Long)` 与 Controller 调用一致。
- outbox payload 字段 `taskId/userId/skillRef` 在 admin 与 user-api 中一致。

---

## 执行方式

Plan complete and saved to `docs/superpowers/plans/2026-07-30-prompt-market-usage-counting-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
