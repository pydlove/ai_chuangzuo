# 创作模板阶段 2：完整生命周期 + 版本管理

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md 5.16.1 阶段 2：在阶段 1 的「默认模板跑得起来」基础上，扩出完整的生命周期（草稿 / 已发布 / 已下线）、版本管理（每次发布递增版本号）、复制派生，并完善可视化编辑器。

**Architecture:**

- 现状：阶段 1 落地后，`t_prompt_template` 只有 `enabled`（0/1），无草稿/发布/下线状态机，无版本号。
- 改造点：
  - **状态机升级**：在 `t_prompt_template` 增加 `template_status`（0-草稿 / 1-已发布 / 2-已下线），保留 `enabled` 作为兼容字段（`enabled=1 ⇔ template_status=1`）。
  - **版本管理**：新增 `t_prompt_template_version` 表（template_id + version + config_json + version_status），每次发布把当前 12 阶段配置快照为新版本。
  - **复制派生**：新增 `POST /prompt-templates/{id}/actions/clone` 接口，复制源模板的当前草稿（或最新版已发布配置）到新草稿。
  - **编辑器增强**：admin-web 的 `PromptTemplateEditView.vue` 增加占位符高亮、规则阶段表单字段、保存草稿/发布按钮分离。

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL 8, Flyway, Jackson, Vue 3, Ant Design Vue, Playwright.

## 全局约束

- 阶段 1 已经在用的 `enabled` 字段保留但语义改为 `enabled ⇔ template_status=1`，新代码优先用 `template_status`。
- 已发布模板的 12 阶段配置写入 `t_prompt_template_version` 表，运行时优先读最新已发布版本；草稿配置仍存在 `t_prompt_template_stage`（编辑态）。
- 内置模板 `id=1` 仍然不可删除（继承阶段 1 的 `PROMPT_TEMPLATE_BUILTIN_IMMUTABLE`），但允许派生新版本。
- 不引入新的中间件，不破坏现有 `PromptTemplateServiceStageTest` 11 个单测。

---

## 现状盘点（实施前必读）

| 已完成项 | 文件 / 位置 |
|----------|-------------|
| `t_prompt_template` 表 + `enabled` 字段 | `V2.0.0_011` |
| `t_prompt_template_stage`（12 阶段） | `V2.0.0_014` |
| 阶段 1 seed（id=1, enabled=1） | `V2.0.0_017` |
| `PromptTemplateService` CRUD + enable/disable | `service/PromptTemplateService.java` |
| `PromptTemplateAdminController` 8 个端点 | `controller/PromptTemplateAdminController.java` |
| `PromptTemplateAdminVO` 含 `isBuiltin` | 阶段 1 已加 |
| Admin List/Edit 页 | `PromptTemplateListView.vue` / `PromptTemplateEditView.vue` |
| 单测 11/11 通过 | 阶段 1 已写 |

| 缺口 | 本计划要补 |
|------|------------|
| 无 `template_status` 状态机 | Task 1：schema 升级 + enum 化 |
| 无版本管理 | Task 2：`t_prompt_template_version` + 发布快照 |
| 无 clone 端点 | Task 3：`POST /actions/clone` |
| 无草稿/发布分离的 UI | Task 4：Edit 页状态机按钮 |
| 无版本对比 | 不在本期，留阶段 2.5 |
| 无 diff 视图 | 不在本期，design §5.13 Q4 |

---

## File Structure

| 文件 | 职责 |
|------|------|
| `project/admin/api/src/main/resources/db/migration/V2.0.0_018__add_template_status_and_version_table.sql` | **新增** 状态字段 + 版本表 |
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplate.java` | **修改** 加 `templateStatus` 字段 |
| `project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplateVersion.java` | **新增** 版本快照实体 |
| `project/shared/src/main/java/com/aichuangzuo/shared/creative/TemplateStatus.java` | **新增** 状态枚举 |
| `project/admin/api/.../mapper/PromptTemplateVersionMapper.java` | **新增** 版本表 Mapper |
| `project/admin/api/.../service/PromptTemplateService.java` | **修改** 增加 publish/clone + 版本快照逻辑 |
| `project/admin/api/.../controller/PromptTemplateAdminController.java` | **修改** 增加 `/actions/clone` 端点 |
| `project/admin/api/.../dto/request/CloneTemplateRequest.java` | **新增** 克隆请求 DTO |
| `project/admin/api/.../vo/PromptTemplateVersionVO.java` | **新增** 版本摘要 VO |
| `project/admin/web/.../views/PromptTemplateListView.vue` | **修改** 显示状态标签（草稿/已发布/已下线） |
| `project/admin/web/.../views/PromptTemplateEditView.vue` | **修改** 状态机按钮（保存草稿 / 发布 / 下线） |
| `project/admin/api/src/test/.../PromptTemplateServiceStageTest.java` | **修改** 新增 publish/clone 单测 |
| `tests/e2e/verify_template_lifecycle.py` | **新增** E2E：派生 → 编辑 → 发布 → 下线 → 复用历史版本 |

---

## Task 1：状态字段 + 版本表 schema 升级

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_018__add_template_status_and_version_table.sql`

- [ ] **Step 1.1：写迁移**

```sql
-- 创作模板生命周期字段：template_status + latest_published_version
-- 状态机：0-草稿，1-已发布，2-已下线
-- 历史已 enabled=1 的数据自动设为已发布

SET NAMES utf8mb4;

ALTER TABLE t_prompt_template
    ADD COLUMN template_status TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '模板状态：0-草稿，1-已发布，2-已下线',
    ADD COLUMN latest_published_version INT UNSIGNED DEFAULT NULL
        COMMENT '当前最新已发布版本号，未发布则为 NULL';

-- 历史数据：enabled=1 视为已发布
UPDATE t_prompt_template
SET template_status = 1, latest_published_version = 1
WHERE enabled = 1 AND is_deleted = 0;

-- 版本快照表
CREATE TABLE IF NOT EXISTS t_prompt_template_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT UNSIGNED NOT NULL COMMENT '所属模板ID',
    version INT UNSIGNED NOT NULL COMMENT '版本号，从 1 开始自增',
    version_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '版本状态：0-草稿，1-已发布，2-已下线',
    config_json JSON NOT NULL COMMENT '12 阶段配置完整快照',
    change_note VARCHAR(512) DEFAULT NULL COMMENT '本次发布变更说明',
    published_at DATETIME(3) DEFAULT NULL COMMENT '发布时间',
    published_by BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_t_pt_version (template_id, version),
    KEY idx_t_pt_version_status (version_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作模板版本快照';

-- 把已 enabled=1 的模板（id=1 默认 + 其他）补一个 v1 快照
INSERT INTO t_prompt_template_version (template_id, version, version_status, config_json, change_note, published_at, published_by, tenant_id, is_deleted, created_by, updated_by)
SELECT
    t.id,
    1,
    1,
    JSON_OBJECT(
        'stages', JSON_ARRAYAGG(
            JSON_OBJECT(
                'index', s.stage_index,
                'stageKey', s.stage_key,
                'stageType', s.stage_type,
                'aiPrompt', s.ai_prompt,
                'ruleConfig', s.rule_config,
                'enabled', s.enabled
            )
        )
    ),
    '初始回填：阶段 1 落地时的默认配置',
    NOW(3),
    0,
    0,
    0,
    0,
    0
FROM t_prompt_template t
JOIN t_prompt_template_stage s ON s.template_id = t.id
WHERE t.enabled = 1 AND t.is_deleted = 0
GROUP BY t.id;
```

- [ ] **Step 1.2：起 admin-api 让 Flyway 自动跑 V2.0.0_018**

```bash
mvn -pl admin/api -am spring-boot:run
```

观察日志看到 `Successfully applied 18 migrations`，并查：

```sql
SELECT id, template_status, latest_published_version FROM t_prompt_template WHERE id = 1;
-- 期望：template_status=1, latest_published_version=1

SELECT template_id, version, version_status, JSON_LENGTH(config_json, '$.stages') AS stage_count
FROM t_prompt_template_version WHERE template_id = 1;
-- 期望：1 行，version=1, version_status=1, stage_count=12
```

- [ ] **Step 1.3：提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_018__add_template_status_and_version_table.sql
git commit -m "feat(creative-template): add lifecycle state machine and version table"
```

---

## Task 2：实体 + 枚举 + Mapper

**Files:**
- Modify: `project/shared/.../entity/PromptTemplate.java`
- Create: `project/shared/.../entity/PromptTemplateVersion.java`
- Create: `project/shared/.../creative/TemplateStatus.java`
- Create: `project/admin/api/.../mapper/PromptTemplateVersionMapper.java`

- [ ] **Step 2.1：状态枚举**

`TemplateStatus.java`：

```java
package com.aichuangzuo.shared.creative;

import java.util.Arrays;

/**
 * 创作模板状态机。
 *
 * <p>设计文档 §5.14.2：草稿 → 已发布 → 已下线 → 重新发布
 */
public enum TemplateStatus {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    public final int code;
    public final String label;

    TemplateStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TemplateStatus fromCode(Integer code) {
        if (code == null) return DRAFT;
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(DRAFT);
    }
}
```

- [ ] **Step 2.2：实体加字段**

`PromptTemplate.java` 加：

```java
private Integer templateStatus;
private Integer latestPublishedVersion;
```

并在 `toString()` 加上便于调试。

- [ ] **Step 2.3：版本实体**

`PromptTemplateVersion.java`：

```java
package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_prompt_template_version")
public class PromptTemplateVersion extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Integer version;
    private Integer versionStatus;
    /** 12 阶段配置完整 JSON 字符串。 */
    private String configJson;
    private String changeNote;
    private java.time.LocalDateTime publishedAt;
    private Long publishedBy;
    private Long tenantId;
}
```

- [ ] **Step 2.4：Mapper**

`PromptTemplateVersionMapper.java`：

```java
package com.aichuangzuo.admin.modules.generation.mapper;

import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromptTemplateVersionMapper extends BaseMapper<PromptTemplateVersion> {
    /** 取某模板所有版本，按 version 降序。 */
    List<PromptTemplateVersion> selectByTemplateId(@Param("templateId") Long templateId);
    /** 取某模板最新已发布版本。 */
    PromptTemplateVersion selectLatestPublished(@Param("templateId") Long templateId);
}
```

配套 XML `PromptTemplateVersionMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper">

    <select id="selectByTemplateId" resultType="com.aichuangzuo.shared.entity.PromptTemplateVersion">
        SELECT * FROM t_prompt_template_version
        WHERE template_id = #{templateId} AND is_deleted = 0
        ORDER BY version DESC
    </select>

    <select id="selectLatestPublished" resultType="com.aichuangzuo.shared.entity.PromptTemplateVersion">
        SELECT * FROM t_prompt_template_version
        WHERE template_id = #{templateId} AND version_status = 1 AND is_deleted = 0
        ORDER BY version DESC LIMIT 1
    </select>

</mapper>
```

- [ ] **Step 2.5：编译验证**

```bash
mvn -pl shared -am compile -DskipTests
mvn -pl admin/api -am compile -DskipTests
```

- [ ] **Step 2.6：提交**

```bash
git add project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplate.java \
        project/shared/src/main/java/com/aichuangzuo/shared/entity/PromptTemplateVersion.java \
        project/shared/src/main/java/com/aichuangzuo/shared/creative/TemplateStatus.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/mapper/PromptTemplateVersionMapper.java \
        project/admin/api/src/main/resources/mapper/PromptTemplateVersionMapper.xml
git commit -m "feat(creative-template): add TemplateStatus enum, version entity and mapper"
```

---

## Task 3：服务层 publish/clone 逻辑

**Files:**
- Modify: `project/admin/api/.../service/PromptTemplateService.java`
- Create: `project/admin/api/.../dto/request/CloneTemplateRequest.java`
- Create: `project/admin/api/.../vo/PromptTemplateVersionVO.java`

- [ ] **Step 3.1：Clone 请求 DTO**

```java
package com.aichuangzuo.admin.modules.generation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CloneTemplateRequest {
    @NotBlank
    @Size(max = 64)
    private String name;

    /** 可选：复制源模板的某个版本（默认最新已发布）。 */
    private Integer sourceVersion;
}
```

- [ ] **Step 3.2：版本摘要 VO**

```java
package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PromptTemplateVersionVO {
    private Integer version;
    private Integer versionStatus;
    private String changeNote;
    private LocalDateTime publishedAt;
    private Long publishedBy;
}
```

- [ ] **Step 3.3：在 `PromptTemplateService` 加 publish()**

```java
@Transactional
public Long publish(Long id, String changeNote, Long adminUserId) {
    PromptTemplate t = requireById(id);
    if (t.getTemplateStatus() != null && t.getTemplateStatus() == TemplateStatus.OFFLINE.code) {
        // 已下线 → 重新发布：用 latest_published_version + 1
    } else {
        // 草稿 → 首次或新版发布
    }
    int nextVersion = (t.getLatestPublishedVersion() == null ? 0 : t.getLatestPublishedVersion()) + 1;

    // 1. 快照 12 阶段到 version 表
    List<PromptTemplateStage> rows = stageMapper.selectByTemplateId(id);
    String configJson = serializeConfig(rows);
    PromptTemplateVersion v = new PromptTemplateVersion();
    v.setTemplateId(id);
    v.setVersion(nextVersion);
    v.setVersionStatus(TemplateStatus.PUBLISHED.code);
    v.setConfigJson(configJson);
    v.setChangeNote(changeNote);
    v.setPublishedAt(LocalDateTime.now());
    v.setPublishedBy(adminUserId == null ? 0L : adminUserId);
    v.setTenantId(0L);
    v.setIsDeleted(0);
    versionMapper.insert(v);

    // 2. 更新主表
    t.setTemplateStatus(TemplateStatus.PUBLISHED.code);
    t.setEnabled(1);
    t.setLatestPublishedVersion(nextVersion);
    t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
    mapper.updateById(t);

    log.info("admin={} 发布 prompt template id={} version={}", adminUserId, id, nextVersion);
    return (long) nextVersion;
}

private String serializeConfig(List<PromptTemplateStage> rows) {
    // 用 Jackson ObjectMapper 拼 JSON，结构与设计文档 5.10 一致
    ObjectMapper m = new ObjectMapper();
    List<Map<String, Object>> stages = rows.stream().map(r -> {
        Map<String, Object> m2 = new HashMap<>();
        m2.put("index", r.getStageIndex());
        m2.put("stageKey", r.getStageKey());
        m2.put("stageType", r.getStageType());
        m2.put("aiPrompt", r.getAiPrompt());
        m2.put("ruleConfig", r.getRuleConfig() == null ? null : r.getRuleConfig());
        m2.put("enabled", r.getEnabled());
        return m2;
    }).toList();
    return "{\"stages\":" + safeToJson(m, stages) + "}";
}
```

- [ ] **Step 3.4：在 `PromptTemplateService` 加 offline()**

```java
@Transactional
public void offline(Long id, Long adminUserId) {
    PromptTemplate t = requireById(id);
    if (t.getTemplateStatus() == null || t.getTemplateStatus() != TemplateStatus.PUBLISHED.code) {
        throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_INVALID_STATUS);
    }
    t.setTemplateStatus(TemplateStatus.OFFLINE.code);
    t.setEnabled(0);
    t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
    mapper.updateById(t);
    log.info("admin={} 下线 prompt template id={}", adminUserId, id);
}
```

- [ ] **Step 3.5：在 `PromptTemplateService` 加 clone()**

```java
@Transactional
public Long clone(Long sourceId, CloneTemplateRequest req, Long adminUserId) {
    PromptTemplate src = requireById(sourceId);
    // 1. 插新模板
    PromptTemplate copy = new PromptTemplate();
    BeanUtils.copyProperties(src, copy, "id", "enabled", "latestPublishedVersion", "createdAt", "updatedAt");
    copy.setName(req.getName());
    copy.setTemplateStatus(TemplateStatus.DRAFT.code);
    copy.setEnabled(0);
    copy.setLatestPublishedVersion(null);
    copy.setCreatedBy(adminUserId == null ? 0L : adminUserId);
    copy.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
    mapper.insert(copy);

    // 2. 复制 stage（来自源模板草稿或指定版本的快照）
    List<PromptTemplateStage> srcStages;
    if (req.getSourceVersion() != null) {
        PromptTemplateVersion v = versionMapper.selectByTemplateId(sourceId).stream()
                .filter(x -> x.getVersion() != null && x.getVersion() == req.getSourceVersion())
                .findFirst().orElseThrow(() -> new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND));
        srcStages = parseStagesFromConfig(v.getConfigJson());
    } else {
        srcStages = stageMapper.selectByTemplateId(sourceId);
    }
    for (PromptTemplateStage srcStage : srcStages) {
        PromptTemplateStage newStage = new PromptTemplateStage();
        BeanUtils.copyProperties(srcStage, newStage, "id", "templateId", "createdAt", "updatedAt");
        newStage.setTemplateId(copy.getId());
        stageMapper.insert(newStage);
    }
    log.info("admin={} 克隆 prompt template id={} → newId={} stages={}",
            adminUserId, sourceId, copy.getId(), srcStages.size());
    return copy.getId();
}
```

- [ ] **Step 3.6：在 `create()` 改成默认 DRAFT 状态**

```java
t.setTemplateStatus(TemplateStatus.DRAFT.code);
t.setEnabled(0);
t.setLatestPublishedVersion(null);
```

- [ ] **Step 3.7：在 `toVo()` 输出状态标签**

```java
TemplateStatus st = TemplateStatus.fromCode(t.getTemplateStatus());
vo.setTemplateStatus(st.code);
vo.setTemplateStatusLabel(st.label);
```

并在 `PromptTemplateAdminVO` 加 `templateStatus` / `templateStatusLabel` / `latestPublishedVersion` 三个字段。

- [ ] **Step 3.8：编译**

```bash
mvn -pl admin/api -am compile -DskipTests
```

- [ ] **Step 3.9：提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateService.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/dto/request/CloneTemplateRequest.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/PromptTemplateVersionVO.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/vo/PromptTemplateAdminVO.java
git commit -m "feat(creative-template): add publish/offline/clone service methods and status VO"
```

---

## Task 4：Controller 新端点 + AdminGenerationErrorCode 补码

**Files:**
- Modify: `project/admin/api/.../controller/PromptTemplateAdminController.java`
- Modify: `project/shared/.../enums/error/AdminGenerationErrorCode.java`

- [ ] **Step 4.1：补错误码**

```java
PROMPT_TEMPLATE_INVALID_STATUS(308013, "模板状态不允许该操作"),
```

- [ ] **Step 4.2：Controller 加 publish/clone/versions 端点**

```java
@PostMapping("/{id}/actions/publish")
public R<Long> publish(@PathVariable Long id,
                       @RequestBody(required = false) PublishTemplateRequest req,
                       @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminUserId) {
    String changeNote = req == null ? null : req.getChangeNote();
    return R.ok(templateService.publish(id, changeNote, adminUserId));
}

@PostMapping("/{id}/actions/offline")
public R<Void> offline(@PathVariable Long id,
                       @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminUserId) {
    templateService.offline(id, adminUserId);
    return R.ok();
}

@PostMapping("/{id}/actions/clone")
public R<Long> clone(@PathVariable Long id,
                     @RequestBody @Valid CloneTemplateRequest req,
                     @RequestHeader(value = "X-Admin-User-Id", required = false) Long adminUserId) {
    return R.ok(templateService.clone(id, req, adminUserId));
}

@GetMapping("/{id}/versions")
public R<List<PromptTemplateVersionVO>> versions(@PathVariable Long id) {
    return R.ok(templateService.listVersions(id));
}
```

`PublishTemplateRequest.java`：

```java
@Data
public class PublishTemplateRequest {
    private String changeNote;
}
```

`PromptTemplateService` 加 `listVersions`：

```java
public List<PromptTemplateVersionVO> listVersions(Long id) {
    requireById(id);
    return versionMapper.selectByTemplateId(id).stream().map(v -> {
        PromptTemplateVersionVO vo = new PromptTemplateVersionVO();
        BeanUtils.copyProperties(v, vo);
        return vo;
    }).toList();
}
```

- [ ] **Step 4.3：编译**

```bash
mvn -pl admin/api -am compile -DskipTests
```

- [ ] **Step 4.4：提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/controller/PromptTemplateAdminController.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/dto/request/PublishTemplateRequest.java \
        project/shared/src/main/java/com/aichuangzuo/shared/enums/error/AdminGenerationErrorCode.java \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateService.java
git commit -m "feat(creative-template): add publish/clone/versions controller endpoints"
```

---

## Task 5：Admin UI 状态机按钮 + 状态标签

**Files:**
- Modify: `project/admin/web/.../views/PromptTemplateListView.vue`
- Modify: `project/admin/web/.../views/PromptTemplateEditView.vue`
- Modify: `project/admin/web/.../composables/usePromptTemplate.js`（如需新增方法）

- [ ] **Step 5.1：List 视图加状态列**

在 `columns` 数组里加：

```js
{ title: '状态', key: 'templateStatus', width: 100 },
```

模板里：

```vue
<template v-if="column.key === 'templateStatus'">
  <a-tag v-if="record.templateStatus === 1" color="green">{{ record.templateStatusLabel }}</a-tag>
  <a-tag v-else-if="record.templateStatus === 2" color="default">{{ record.templateStatusLabel }}</a-tag>
  <a-tag v-else color="orange">{{ record.templateStatusLabel }}</a-tag>
</template>
```

并把操作列里现有的「启用/停用」按钮按状态调整：草稿显示「编辑后发布」入口，已发布显示「下线」入口，已下线显示「重新发布」。

- [ ] **Step 5.2：Edit 视图加底部状态机操作栏**

```vue
<div class="state-actions">
  <a-button @click="saveDraft">保存草稿</a-button>
  <a-button type="primary" @click="publish">发布</a-button>
  <a-button v-if="record.templateStatus === 1" danger @click="offline">下线</a-button>
</div>
```

- [ ] **Step 5.3：usePromptTemplate.js 加方法**

```js
const handlePublish = async (id, changeNote) => {
  await api.post(`/prompt-templates/${id}/actions/publish`, { changeNote })
  message.success('已发布')
  await fetch()
}
const handleOffline = async (id) => {
  await api.post(`/prompt-templates/${id}/actions/offline`, null, { headers: { 'X-Admin-User-Id': currentUserId } })
  message.success('已下线')
  await fetch()
}
const handleClone = async (id, name) => {
  const resp = await api.post(`/prompt-templates/${id}/actions/clone`, { name })
  message.success('已克隆，新模板 ID: ' + resp.data.data)
  return resp.data.data
}
```

- [ ] **Step 5.4：手测**

1. 起 admin-web：访问 `/console/prompt-templates`，默认模板应显示「已发布」绿色标签。
2. 点「新建模板」→ 输入名称 → 存为草稿 → 列表显示「草稿」橙色标签。
3. 编辑草稿 → 点「发布」→ 列表变「已发布」。
4. 点「下线」→ 变「已下线」灰色。
5. 点「重新发布」→ 变「已发布」，version 升到 2。
6. 在默认模板行点「克隆」→ 输入「默认-副本」→ 列表新增一条「草稿」，id 不同于 1。

- [ ] **Step 5.5：提交**

```bash
git add project/admin/web/src/views/PromptTemplateListView.vue \
        project/admin/web/src/views/PromptTemplateEditView.vue \
        project/admin/web/src/composables/usePromptTemplate.js
git commit -m "feat(creative-template): add lifecycle state UI actions"
```

---

## Task 6：单测覆盖 publish/clone/offline

**Files:**
- Modify: `project/admin/api/src/test/.../service/PromptTemplateServiceStageTest.java`

- [ ] **Step 6.1：补 Mock 和 import**

```java
@Mock private PromptTemplateVersionMapper versionMapper;
```

import：

```java
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.admin.modules.generation.dto.request.CloneTemplateRequest;
```

- [ ] **Step 6.2：新增测试**

```java
@Test
void publish_shouldCreateVersionAndSetStatus() {
    PromptTemplate exist = sampleTemplate(1L);
    exist.setLatestPublishedVersion(null);
    when(templateMapper.selectById(1L)).thenReturn(exist);
    when(stageMapper.selectByTemplateId(1L)).thenReturn(new ArrayList<>());

    Long version = service.publish(1L, "首次发布", 99L);

    assertEquals(Long.valueOf(1L), version);
    verify(versionMapper, times(1)).insert(any(PromptTemplateVersion.class));
    assertEquals(TemplateStatus.PUBLISHED.code, exist.getTemplateStatus());
    assertEquals(1, exist.getLatestPublishedVersion());
}

@Test
void publish_shouldIncrementVersion() {
    PromptTemplate exist = sampleTemplate(2L);
    exist.setLatestPublishedVersion(3);
    when(templateMapper.selectById(2L)).thenReturn(exist);
    when(stageMapper.selectByTemplateId(2L)).thenReturn(new ArrayList<>());

    Long version = service.publish(2L, "第 4 次", 1L);

    assertEquals(Long.valueOf(4L), version);
    assertEquals(4, exist.getLatestPublishedVersion());
}

@Test
void offline_shouldSetStatusToOffline() {
    PromptTemplate exist = sampleTemplate(1L);
    exist.setTemplateStatus(TemplateStatus.PUBLISHED.code);
    exist.setEnabled(1);
    when(templateMapper.selectById(1L)).thenReturn(exist);

    service.offline(1L, 1L);

    assertEquals(TemplateStatus.OFFLINE.code, exist.getTemplateStatus());
    assertEquals(0, exist.getEnabled());
}

@Test
void offline_shouldRejectNonPublishedTemplate() {
    PromptTemplate exist = sampleTemplate(1L);
    exist.setTemplateStatus(TemplateStatus.DRAFT.code);
    when(templateMapper.selectById(1L)).thenReturn(exist);

    assertThrows(BusinessException.class, () -> service.offline(1L, 1L));
}

@Test
void clone_shouldCreateDraftWithCopiedStages() {
    PromptTemplate src = sampleTemplate(1L);
    when(templateMapper.selectById(1L)).thenReturn(src);
    org.mockito.Mockito.doAnswer((inv) -> {
        PromptTemplate t = inv.getArgument(0);
        t.setId(100L);
        return 1;
    }).when(templateMapper).insert(any(PromptTemplate.class));

    List<PromptTemplateStage> srcStages = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(i);
        s.setStageKey("k" + i);
        s.setStageType("ai_prompt");
        s.setAiPrompt("p" + i);
        srcStages.add(s);
    }
    when(stageMapper.selectByTemplateId(1L)).thenReturn(srcStages);

    CloneTemplateRequest req = new CloneTemplateRequest();
    req.setName("默认-副本");
    Long newId = service.clone(1L, req, 99L);

    assertEquals(Long.valueOf(100L), newId);
    verify(stageMapper, times(3)).insert(any(PromptTemplateStage.class));
    assertEquals(TemplateStatus.DRAFT.code, src.getTemplateStatus() == null
            ? TemplateStatus.DRAFT.code : TemplateStatus.fromCode(src.getTemplateStatus()).code);
}
```

- [ ] **Step 6.3：跑测试**

```bash
mvn -pl admin/api -am test -Dtest=PromptTemplateServiceStageTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: Tests run: 16+, Failures: 0。

- [ ] **Step 6.4：提交**

```bash
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/generation/service/PromptTemplateServiceStageTest.java
git commit -m "test(creative-template): add publish/offline/clone unit tests"
```

---

## Task 7：E2E 验证生命周期

**Files:**
- Create: `tests/e2e/verify_template_lifecycle.py`

- [ ] **Step 7.1：写脚本**

脚本结构：
1. admin 登录 → 克隆默认模板 → 拿到新草稿 id
2. 编辑新草稿 → 改第 4 阶段 ai_prompt → 保存草稿
3. 发布新草稿 → 验证 version=1, template_status=published
4. 重新发布 → 验证 version=2
5. 下线 → 验证 template_status=offline
6. 重新发布 → 验证 template_status=published, version=3
7. 删除新模板 → 验证软删除
8. user 端跑一条任务 → 验证仍然用默认模板（id=1）工作

- [ ] **Step 7.2：运行**

```bash
./scripts/local/start-all.sh   # 或现有启动脚本
python3 tests/e2e/verify_template_lifecycle.py
```

- [ ] **Step 7.3：提交**

```bash
git add tests/e2e/verify_template_lifecycle.py
git commit -m "test(creative-template): add lifecycle E2E verification"
```

---

## Self-Review

**1. Spec coverage（设计文档 5.14 + 5.16.1 阶段 2 验收）：**

| 验收项 | 覆盖 Task |
|--------|-----------|
| 草稿 / 已发布 / 已下线 状态机 | Task 1 + 3 |
| 版本管理（每次发布递增） | Task 2 + 3 |
| 复制派生 | Task 3 |
| UI 状态机按钮 | Task 5 |
| 内置模板保护仍生效 | 继承阶段 1 的 PROMPT_TEMPLATE_BUILTIN_IMMUTABLE |
| 单测覆盖 | Task 6（16+ 测试） |

**2. 不破坏阶段 1：**

- `enabled` 字段保留语义兼容（`enabled ⇔ template_status=PUBLISHED`）。
- 阶段 1 的 11 个单测不动，运行时仍然能 fallback 到 enum 默认。
- 默认模板 id=1 行为不变（仍然 enabled=1 + 12 阶段）。

**3. 范围控制（不做）：**

- 不实现版本 diff UI（design §5.13 Q4 留待后续）
- 不实现多模板同时发布（仅 1 个 enabled 沿用现有约束）
- 不实现版本回滚（design §5.14 Q3 待讨论）

**4. 可回滚：**

- V2.0.0_018 是纯 ALTER + 新表，删迁移文件即可回滚。
- UI 状态机按钮是 Vue 模板条件渲染，回滚到「启用/停用」单按钮即可。

**5. 风险：**

- `JSON_OBJECT(...)` 在 MySQL 5.7 不支持，但项目用 MySQL 8.x，安全。
- `serializeConfig` 是 inline 字符串拼接，简单粗暴；后续可换 Jackson `writeValueAsString`。
- 阶段 1 的 seed migration 没有插入 12 阶段行，靠 `PipelineTemplateResolver` 兜底；阶段 2 回填脚本会扫 stage 表生成 v1 快照，需要保证 12 阶段行已存在（编辑页点「初始化 12 阶段」后会存在）。

阶段 2 完成可进入阶段 3（用户端模板选择 + 生成任务侧消费）。