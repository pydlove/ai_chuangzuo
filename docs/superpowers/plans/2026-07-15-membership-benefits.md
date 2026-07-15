# 会员权益系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在用户端落地数据驱动的会员权益系统：3 张表（权益定义/套餐权益值/用量）+ benefit 模块 API（查询/校验/消费）+ 前端 composable，覆盖 Pricing 页承诺的 15 项权益，新增权益只需插数据。

**Architecture:** Flyway 迁移建表并灌入 15 项权益 × 3 套餐初始数据；用户端新增 `benefit` 模块（entity/mapper/service/controller），套餐权益用 Caffeine 缓存（10 分钟 TTL）；前端新增 `useBenefits` composable（模块级 ref 单例），登录后加载，页面按权益值控制功能。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus（BaseMapper + 内联 @Select，无 XML）+ Flyway + MySQL 8 + Caffeine；Vue 3 + composables + Ant Design Vue。

## Global Constraints

- 迁移文件遵循 `docs/architecture/mysql-table-conventions.md`：每列带 COMMENT，`created_at/updated_at DATETIME(3)` 默认 `CURRENT_TIMESTAMP(3)`，`created_by/updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0`，`tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0`，`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`，索引前缀 `uk_`/`idx_` 带表名。
- 用户端 API 模式：entity 用 `@Getter @Setter @TableName`，`@TableId(type = IdType.AUTO)`，审计字段用 `@TableField(fill = FieldFill.*)`；mapper 用 `@Mapper interface XxxMapper extends BaseMapper<Xxx>` + 内联 `@Select`；controller 用 `SecurityUserContext.getCurrentUserId()` 取用户 ID；响应统一 `Result.success(...)`。
- 用户端测试模式：`@SpringBootTest @Transactional @Rollback` 真实 DB 测试（参考 `MessageServiceTest`），不用 Mockito。
- 错误码：模块编码 18（用户端-权益），格式 `1180NN`，枚举实现 `com.aichuangzuo.shared.result.ErrorCode`。
- 前端模式：不用 Pinia；`src/composables/useBenefits.js` 模块级 ref 单例（参考 `useUserProfile.js`）；API 文件 `src/api/benefit.js`，路径相对 `/api/v1/user` baseURL。
- `history_days` 值 -1 表示永久；quota 周期为自然月，格式 `yyyy-MM`。
- 套餐 key：`basic` / `pro` / `flagship`；无会员或已过期时 `planKey = "free"`，无权益。

---

### Task 1: Flyway 迁移 — 3 张表 + 初始数据

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_023__create_benefit_tables.sql`

**Interfaces:**
- Produces: 表 `u_benefit`（15 行种子）、`u_plan_benefit`（45 行种子）、`u_benefit_usage`（空表）。后续 Task 的 entity 字段名与此处列名一一对应（下划线转驼峰）。

- [ ] **Step 1: 编写迁移 SQL**

```sql
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_benefit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL COMMENT '权益编码，如 ai_article_quota',
    name VARCHAR(64) NOT NULL COMMENT '权益名称',
    type VARCHAR(16) NOT NULL COMMENT '类型：boolean/quota/tier',
    description VARCHAR(256) DEFAULT NULL COMMENT '权益描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_benefit_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益定义表';

CREATE TABLE IF NOT EXISTS u_plan_benefit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐：basic/pro/flagship',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码，关联 u_benefit.code',
    benefit_value VARCHAR(128) NOT NULL COMMENT '权益值：boolean 存 true/false，quota 存数字，tier 存等级标识',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_plan_benefit_plan_code (plan_key, benefit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐权益值表';

CREATE TABLE IF NOT EXISTS u_benefit_usage (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码',
    period VARCHAR(16) NOT NULL COMMENT '周期标识，月度格式 yyyy-MM',
    used_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已用量',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_benefit_usage_user_code_period (user_id, benefit_code, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益用量表';

INSERT INTO u_benefit (code, name, type, description, sort_order) VALUES
('ai_article_quota', 'AI 文章生成', 'quota', '每月可生成的文章篇数', 1),
('export_word', '导出 Word', 'boolean', '导出 Word 文档', 2),
('copy_text', '复制正文', 'boolean', '复制文章正文', 3),
('ai_topic', 'AI 选题灵感', 'boolean', 'AI 选题灵感推荐', 4),
('ai_title_optimize', 'AI 标题优化', 'boolean', 'AI 标题优化建议', 5),
('online_edit', '在线编辑', 'boolean', '文章在线编辑器', 6),
('style_custom', '写作风格定制', 'tier', 'none-不可用/preset-预设风格/custom-自定义风格', 7),
('seo_keywords', 'SEO 关键词建议', 'boolean', 'SEO 关键词优化建议', 8),
('template_access', '文章模板', 'tier', 'basic_8-基础 8 款/all_20-全部 20 款/all_custom-全部+自定义', 9),
('sticker_quota', '贴图生成', 'quota', '每月可生成的贴图数量', 10),
('batch_generate', '批量生成/改写', 'boolean', '批量生成与批量改写', 11),
('batch_export', '批量导出', 'boolean', '批量导出文章', 12),
('history_days', '历史记录', 'quota', '历史记录保留天数，-1 表示永久', 13),
('queue_priority', '生成队列优先级', 'tier', 'standard-标准/priority-优先/express-极速', 14),
('queue_max_tasks', '队列任务数', 'quota', '同时在队列中的最大任务数', 15);

INSERT INTO u_plan_benefit (plan_key, benefit_code, benefit_value) VALUES
('basic', 'ai_article_quota', '30'), ('pro', 'ai_article_quota', '100'), ('flagship', 'ai_article_quota', '300'),
('basic', 'export_word', 'true'), ('pro', 'export_word', 'true'), ('flagship', 'export_word', 'true'),
('basic', 'copy_text', 'true'), ('pro', 'copy_text', 'true'), ('flagship', 'copy_text', 'true'),
('basic', 'ai_topic', 'true'), ('pro', 'ai_topic', 'true'), ('flagship', 'ai_topic', 'true'),
('basic', 'ai_title_optimize', 'false'), ('pro', 'ai_title_optimize', 'true'), ('flagship', 'ai_title_optimize', 'true'),
('basic', 'online_edit', 'false'), ('pro', 'online_edit', 'true'), ('flagship', 'online_edit', 'true'),
('basic', 'style_custom', 'none'), ('pro', 'style_custom', 'preset'), ('flagship', 'style_custom', 'custom'),
('basic', 'seo_keywords', 'false'), ('pro', 'seo_keywords', 'false'), ('flagship', 'seo_keywords', 'true'),
('basic', 'template_access', 'basic_8'), ('pro', 'template_access', 'all_20'), ('flagship', 'template_access', 'all_custom'),
('basic', 'sticker_quota', '5'), ('pro', 'sticker_quota', '30'), ('flagship', 'sticker_quota', '100'),
('basic', 'batch_generate', 'false'), ('pro', 'batch_generate', 'false'), ('flagship', 'batch_generate', 'true'),
('basic', 'batch_export', 'false'), ('pro', 'batch_export', 'false'), ('flagship', 'batch_export', 'true'),
('basic', 'history_days', '30'), ('pro', 'history_days', '-1'), ('flagship', 'history_days', '-1'),
('basic', 'queue_priority', 'standard'), ('pro', 'queue_priority', 'priority'), ('flagship', 'queue_priority', 'express'),
('basic', 'queue_max_tasks', '1'), ('pro', 'queue_max_tasks', '5'), ('flagship', 'queue_max_tasks', '10');
```

- [ ] **Step 2: 启动应用验证迁移执行**

Run: `cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn spring-boot:run`（启动后 Ctrl+C 停掉即可；若本地已有运行实例，也可直接查库）
Expected: 日志出现 `Successfully applied 1 migration` 含 `V1.0.0 023`；或在 MySQL 执行 `SELECT COUNT(*) FROM u_benefit;` 得 15，`SELECT COUNT(*) FROM u_plan_benefit;` 得 45

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_023__create_benefit_tables.sql
git commit -m "feat(user-api): 会员权益三张表 + 15 项权益初始数据"
```

---

### Task 2: benefit 模块数据层 — entity / mapper / enums / VO

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/entity/Benefit.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/entity/PlanBenefit.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/entity/BenefitUsage.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/mapper/BenefitMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/mapper/PlanBenefitMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/mapper/BenefitUsageMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/enums/BenefitErrorCode.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/vo/UserBenefitVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/vo/BenefitCheckVO.java`

**Interfaces:**
- Consumes: Task 1 的 3 张表
- Produces: `BenefitMapper.selectByCode(String)`、`PlanBenefitMapper.selectByPlanKey(String)`、`BenefitUsageMapper.selectByUserAndCodeAndPeriod(Long, String, String)`、`BenefitUsageMapper.incrementIfBelowLimit(Long, String, String, int)`；错误码 `BENEFIT_NOT_FOUND(118001)`、`BENEFIT_NOT_SUPPORTED(118002)`、`QUOTA_EXHAUSTED(118003)`、`NOT_QUOTA_BENEFIT(118004)`

- [ ] **Step 1: 编写 3 个 entity**

`Benefit.java`：

```java
package com.aichuangzuo.user.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权益定义实体，对应表 u_benefit。
 */
@Getter
@Setter
@TableName("u_benefit")
public class Benefit {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权益编码，如 ai_article_quota。 */
    private String code;

    /** 权益名称。 */
    private String name;

    /** 类型：boolean/quota/tier。 */
    private String type;

    /** 权益描述。 */
    private String description;

    /** 排序号。 */
    private Integer sortOrder;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
```

`PlanBenefit.java`：

```java
package com.aichuangzuo.user.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 套餐权益值实体，对应表 u_plan_benefit。
 */
@Getter
@Setter
@TableName("u_plan_benefit")
public class PlanBenefit {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐：basic/pro/flagship。 */
    private String planKey;

    /** 权益编码，关联 u_benefit.code。 */
    private String benefitCode;

    /** 权益值：boolean 存 true/false，quota 存数字，tier 存等级标识。 */
    private String benefitValue;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
```

`BenefitUsage.java`：

```java
package com.aichuangzuo.user.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权益用量实体，对应表 u_benefit_usage。
 */
@Getter
@Setter
@TableName("u_benefit_usage")
public class BenefitUsage {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 权益编码。 */
    private String benefitCode;

    /** 周期标识，月度格式 yyyy-MM。 */
    private String period;

    /** 已用量。 */
    private Integer usedCount;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
```

- [ ] **Step 2: 编写 3 个 mapper**

`BenefitMapper.java`：

```java
package com.aichuangzuo.user.modules.benefit.mapper;

import com.aichuangzuo.user.modules.benefit.entity.Benefit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 权益定义 Mapper。
 */
@Mapper
public interface BenefitMapper extends BaseMapper<Benefit> {

    /**
     * 根据编码查询启用中的权益。
     *
     * @param code 权益编码
     * @return 权益定义；不存在或已停用返回 null
     */
    @Select("SELECT * FROM u_benefit WHERE code = #{code} AND status = 1 LIMIT 1")
    Benefit selectByCode(@Param("code") String code);
}
```

`PlanBenefitMapper.java`：

```java
package com.aichuangzuo.user.modules.benefit.mapper;

import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 套餐权益值 Mapper。
 */
@Mapper
public interface PlanBenefitMapper extends BaseMapper<PlanBenefit> {

    /**
     * 查询某套餐的全部权益值。
     *
     * @param planKey 套餐 key
     * @return 该套餐的权益值列表
     */
    @Select("SELECT * FROM u_plan_benefit WHERE plan_key = #{planKey}")
    List<PlanBenefit> selectByPlanKey(@Param("planKey") String planKey);
}
```

`BenefitUsageMapper.java`：

```java
package com.aichuangzuo.user.modules.benefit.mapper;

import com.aichuangzuo.user.modules.benefit.entity.BenefitUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 权益用量 Mapper。
 */
@Mapper
public interface BenefitUsageMapper extends BaseMapper<BenefitUsage> {

    /**
     * 查询用户某权益当前周期的用量记录。
     *
     * @param userId 用户ID
     * @param benefitCode 权益编码
     * @param period 周期标识
     * @return 用量记录；未消费过返回 null
     */
    @Select("SELECT * FROM u_benefit_usage WHERE user_id = #{userId} AND benefit_code = #{benefitCode} AND period = #{period} LIMIT 1")
    BenefitUsage selectByUserAndCodeAndPeriod(@Param("userId") Long userId,
                                              @Param("benefitCode") String benefitCode,
                                              @Param("period") String period);

    /**
     * 原子地将用量 +1，仅在未超限时生效（防并发超额）。
     *
     * @param userId 用户ID
     * @param benefitCode 权益编码
     * @param period 周期标识
     * @param limit 额度上限
     * @return 受影响行数；0 表示记录不存在或已达上限
     */
    @Update("UPDATE u_benefit_usage SET used_count = used_count + 1 " +
            "WHERE user_id = #{userId} AND benefit_code = #{benefitCode} AND period = #{period} AND used_count < #{limit}")
    int incrementIfBelowLimit(@Param("userId") Long userId,
                              @Param("benefitCode") String benefitCode,
                              @Param("period") String period,
                              @Param("limit") int limit);
}
```

- [ ] **Step 3: 编写错误码枚举**

`BenefitErrorCode.java`：

```java
package com.aichuangzuo.user.modules.benefit.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 权益模块错误码，模块编码 18。
 */
public enum BenefitErrorCode implements ErrorCode {

    BENEFIT_NOT_FOUND(118001, "权益不存在"),
    BENEFIT_NOT_SUPPORTED(118002, "当前套餐不支持此功能"),
    QUOTA_EXHAUSTED(118003, "额度已用完"),
    NOT_QUOTA_BENEFIT(118004, "仅配额类权益可消费");

    private final int code;
    private final String message;

    BenefitErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 4: 编写 2 个 VO**

`UserBenefitVO.java`（`/me` 响应）：

```java
package com.aichuangzuo.user.modules.benefit.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 当前用户权益视图。
 */
@Getter
@Setter
public class UserBenefitVO {

    /** 套餐 key；无会员时为 free。 */
    private String planKey;

    /** 套餐名称；无会员时为 免费版。 */
    private String planName;

    /** 会员到期日期（yyyy-MM-dd）；无会员时为 null。 */
    private String expiresAt;

    /** 权益列表。 */
    private List<BenefitItem> benefits;

    /**
     * 单项权益。
     */
    @Getter
    @Setter
    public static class BenefitItem {

        /** 权益编码。 */
        private String code;

        /** 权益名称。 */
        private String name;

        /** 类型：boolean/quota/tier。 */
        private String type;

        /** 权益值。 */
        private String value;

        /** 已用量（仅 quota 类型有值）。 */
        private Integer used;

        /** 剩余额度（仅 quota 类型有值）。 */
        private Integer remaining;
    }
}
```

`BenefitCheckVO.java`（`/check`、`/consume` 响应）：

```java
package com.aichuangzuo.user.modules.benefit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 权益校验/消费结果视图。
 */
@Getter
@Setter
public class BenefitCheckVO {

    /** 是否放行。 */
    private Boolean allowed;

    /** 权益编码。 */
    private String code;

    /** 类型：boolean/quota/tier。 */
    private String type;

    /** 权益值。 */
    private String value;

    /** 已用量（仅 quota 类型有值）。 */
    private Integer used;

    /** 剩余额度（仅 quota 类型有值）。 */
    private Integer remaining;

    /** 提示信息（不放行时给出原因）。 */
    private String message;
}
```

- [ ] **Step 5: 编译验证**

Run: `cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/
git commit -m "feat(user-api): benefit 模块数据层（entity/mapper/enums/VO）"
```

---

### Task 3: BenefitService + 单元测试

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/service/BenefitService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/service/impl/BenefitServiceImpl.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/config/CaffeineConfig.java:44-53`（注册 planBenefits 自定义缓存，TTL 10 分钟）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/benefit/service/BenefitServiceTest.java`

**Interfaces:**
- Consumes: Task 2 的 mapper/VO/错误码；`UserMembershipMapper.selectByUserId(Long)`（已存在于 membership 模块）；`MembershipPlan.of(String)` 取套餐显示名
- Produces: `BenefitService.getMyBenefits(Long userId) → UserBenefitVO`、`BenefitService.check(Long userId, String code) → BenefitCheckVO`、`BenefitService.consume(Long userId, String code) → BenefitCheckVO`。Task 4 controller 直接调用这 3 个方法。

- [ ] **Step 1: 编写失败的测试**

`BenefitServiceTest.java`：

```java
package com.aichuangzuo.user.modules.benefit.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.benefit.entity.BenefitUsage;
import com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitUsageMapper;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class BenefitServiceTest {

    @Autowired
    private BenefitService benefitService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private BenefitUsageMapper benefitUsageMapper;

    // ── getMyBenefits ──

    @Test
    void getMyBenefits_noMembership_returnsFreeWithEmptyBenefits() {
        User user = createUser("benefit-free@test.com");

        UserBenefitVO vo = benefitService.getMyBenefits(user.getId());

        assertEquals("free", vo.getPlanKey());
        assertEquals("免费版", vo.getPlanName());
        assertNull(vo.getExpiresAt());
        assertNotNull(vo.getBenefits());
        assertTrue(vo.getBenefits().isEmpty());
    }

    @Test
    void getMyBenefits_expiredMembership_returnsFree() {
        User user = createUser("benefit-expired@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().minusDays(1));

        UserBenefitVO vo = benefitService.getMyBenefits(user.getId());

        assertEquals("free", vo.getPlanKey());
        assertTrue(vo.getBenefits().isEmpty());
    }

    @Test
    void getMyBenefits_proMembership_returns15BenefitsWithQuotaUsage() {
        User user = createUser("benefit-pro@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        UserBenefitVO vo = benefitService.getMyBenefits(user.getId());

        assertEquals("pro", vo.getPlanKey());
        assertEquals("专业版", vo.getPlanName());
        assertEquals(LocalDate.now().plusDays(30).toString(), vo.getExpiresAt());
        assertEquals(15, vo.getBenefits().size());

        UserBenefitVO.BenefitItem quota = vo.getBenefits().stream()
                .filter(b -> "ai_article_quota".equals(b.getCode()))
                .findFirst().orElseThrow();
        assertEquals("quota", quota.getType());
        assertEquals("100", quota.getValue());
        assertEquals(0, quota.getUsed());
        assertEquals(100, quota.getRemaining());

        UserBenefitVO.BenefitItem bool = vo.getBenefits().stream()
                .filter(b -> "ai_title_optimize".equals(b.getCode()))
                .findFirst().orElseThrow();
        assertEquals("boolean", bool.getType());
        assertEquals("true", bool.getValue());
        assertNull(bool.getUsed());
    }

    // ── check ──

    @Test
    void check_unknownCode_throwsNotFound() {
        User user = createUser("benefit-check-unknown@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.check(user.getId(), "not_exist_code"));
        assertEquals(BenefitErrorCode.BENEFIT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void check_booleanAllowed_returnsTrue() {
        User user = createUser("benefit-check-bool@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_title_optimize");

        assertTrue(vo.getAllowed());
        assertEquals("boolean", vo.getType());
        assertEquals("true", vo.getValue());
    }

    @Test
    void check_booleanDenied_returnsFalseWithMessage() {
        User user = createUser("benefit-check-denied@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_title_optimize");

        assertFalse(vo.getAllowed());
        assertEquals("false", vo.getValue());
        assertNotNull(vo.getMessage());
    }

    @Test
    void check_freeUser_returnsNotSupported() {
        User user = createUser("benefit-check-free@test.com");

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_title_optimize");

        assertFalse(vo.getAllowed());
        assertEquals(BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getMessage(), vo.getMessage());
    }

    @Test
    void check_quota_returnsUsedAndRemaining() {
        User user = createUser("benefit-check-quota@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 28);

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_article_quota");

        assertTrue(vo.getAllowed());
        assertEquals(28, vo.getUsed());
        assertEquals(72, vo.getRemaining());
    }

    @Test
    void check_tier_returnsValueForBusinessDecision() {
        User user = createUser("benefit-check-tier@test.com");
        createMembership(user.getId(), "flagship", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.check(user.getId(), "queue_priority");

        assertTrue(vo.getAllowed());
        assertEquals("tier", vo.getType());
        assertEquals("express", vo.getValue());
    }

    // ── consume ──

    @Test
    void consume_quotaFirstTime_insertsRowAndReturnsRemaining() {
        User user = createUser("benefit-consume-first@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.consume(user.getId(), "ai_article_quota");

        assertTrue(vo.getAllowed());
        assertEquals(1, vo.getUsed());
        assertEquals(29, vo.getRemaining());
    }

    @Test
    void consume_quotaIncrementsExistingRow() {
        User user = createUser("benefit-consume-incr@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 10);

        BenefitCheckVO vo = benefitService.consume(user.getId(), "ai_article_quota");

        assertTrue(vo.getAllowed());
        assertEquals(11, vo.getUsed());
        assertEquals(19, vo.getRemaining());
    }

    @Test
    void consume_quotaExhausted_throws() {
        User user = createUser("benefit-consume-full@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.consume(user.getId(), "ai_article_quota"));
        assertEquals(BenefitErrorCode.QUOTA_EXHAUSTED.getCode(), ex.getCode());
    }

    @Test
    void consume_freeUser_throwsNotSupported() {
        User user = createUser("benefit-consume-free@test.com");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.consume(user.getId(), "ai_article_quota"));
        assertEquals(BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getCode(), ex.getCode());
    }

    @Test
    void consume_booleanBenefit_throwsNotQuota() {
        User user = createUser("benefit-consume-bool@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.consume(user.getId(), "ai_title_optimize"));
        assertEquals(BenefitErrorCode.NOT_QUOTA_BENEFIT.getCode(), ex.getCode());
    }

    // ── helpers ──

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);
        return user;
    }

    private void createMembership(Long userId, String level, LocalDate expiresAt) {
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setLevel(level);
        membership.setStartedAt(LocalDate.now());
        membership.setExpiresAt(expiresAt);
        membership.setTenantId(0L);
        userMembershipMapper.insert(membership);
    }

    private void createUsage(Long userId, String code, int usedCount) {
        BenefitUsage usage = new BenefitUsage();
        usage.setUserId(userId);
        usage.setBenefitCode(code);
        usage.setPeriod(java.time.YearMonth.now().toString());
        usage.setUsedCount(usedCount);
        usage.setTenantId(0L);
        benefitUsageMapper.insert(usage);
    }
}
```

> 注：`createUser` 与 `MessageServiceTest` 保持一致（bizNo/inviteCode 用 nanoTime 保证唯一）。`User` 上无 `nickname`/`status` 字段，不要加。

- [ ] **Step 2: 运行测试确认编译失败（BenefitService 不存在）**

Run: `cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn test -Dtest=BenefitServiceTest -q`
Expected: 编译错误 `cannot find symbol: class BenefitService`

- [ ] **Step 3: 修改 CaffeineConfig 注册 planBenefits 缓存（TTL 10 分钟）**

修改 `cacheManager()` 方法：

```java
    @Primary
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        // 套餐权益缓存：10 分钟 TTL（权益配置极少变更）
        manager.registerCustomCache("planBenefits",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());
        return manager;
    }
```

- [ ] **Step 4: 编写 BenefitService 接口**

```java
package com.aichuangzuo.user.modules.benefit.service;

import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;

/**
 * 会员权益服务。
 */
public interface BenefitService {

    /**
     * 查询当前用户的套餐与全部权益（quota 类含已用量/剩余额度）。
     *
     * @param userId 用户ID
     * @return 用户权益视图；无会员时 planKey=free 且权益列表为空
     */
    UserBenefitVO getMyBenefits(Long userId);

    /**
     * 校验单项权益是否可用（quota 类只读不写）。
     *
     * @param userId 用户ID
     * @param code 权益编码
     * @return 校验结果
     */
    BenefitCheckVO check(Long userId, String code);

    /**
     * 消费一次配额（仅 quota 类），成功返回最新剩余额度。
     *
     * @param userId 用户ID
     * @param code 权益编码
     * @return 消费结果
     */
    BenefitCheckVO consume(Long userId, String code);
}
```

- [ ] **Step 5: 编写 BenefitServiceImpl**

```java
package com.aichuangzuo.user.modules.benefit.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.entity.Benefit;
import com.aichuangzuo.user.modules.benefit.entity.BenefitUsage;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitMapper;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitUsageMapper;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员权益服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitServiceImpl implements BenefitService {

    private static final String FREE_PLAN_KEY = "free";
    private static final String FREE_PLAN_NAME = "免费版";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_QUOTA = "quota";

    private final BenefitMapper benefitMapper;
    private final PlanBenefitMapper planBenefitMapper;
    private final BenefitUsageMapper benefitUsageMapper;
    private final UserMembershipMapper userMembershipMapper;

    @Override
    public UserBenefitVO getMyBenefits(Long userId) {
        String planKey = currentPlanKey(userId);
        UserBenefitVO vo = new UserBenefitVO();
        vo.setPlanKey(planKey);

        if (FREE_PLAN_KEY.equals(planKey)) {
            vo.setPlanName(FREE_PLAN_NAME);
            vo.setBenefits(new ArrayList<>());
            return vo;
        }

        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        MembershipPlan plan = MembershipPlan.of(planKey);
        vo.setPlanName(plan == null ? planKey : plan.getDisplayName());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));

        Map<String, Benefit> benefitMap = loadActiveBenefitMap();
        String period = currentPeriod();
        List<UserBenefitVO.BenefitItem> items = new ArrayList<>();
        for (PlanBenefit pb : listPlanBenefits(planKey)) {
            Benefit benefit = benefitMap.get(pb.getBenefitCode());
            if (benefit == null) {
                continue;
            }
            UserBenefitVO.BenefitItem item = new UserBenefitVO.BenefitItem();
            item.setCode(benefit.getCode());
            item.setName(benefit.getName());
            item.setType(benefit.getType());
            item.setValue(pb.getBenefitValue());
            if (TYPE_QUOTA.equals(benefit.getType())) {
                int limit = parseInt(pb.getBenefitValue(), 0);
                int used = currentUsed(userId, benefit.getCode(), period);
                item.setUsed(used);
                item.setRemaining(Math.max(limit - used, 0));
            }
            items.add(item);
        }
        vo.setBenefits(items);
        return vo;
    }

    @Override
    public BenefitCheckVO check(Long userId, String code) {
        Benefit benefit = requireBenefit(code);
        String planKey = currentPlanKey(userId);

        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setCode(code);
        vo.setType(benefit.getType());

        PlanBenefit planBenefit = findPlanBenefit(planKey, code);
        if (planBenefit == null) {
            deny(vo, null, BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getMessage());
            return vo;
        }
        vo.setValue(planBenefit.getBenefitValue());

        if (TYPE_BOOLEAN.equals(benefit.getType())) {
            if (Boolean.parseBoolean(planBenefit.getBenefitValue())) {
                vo.setAllowed(true);
            } else {
                deny(vo, planBenefit.getBenefitValue(), "当前套餐不支持此功能，请升级");
            }
            return vo;
        }

        if (TYPE_QUOTA.equals(benefit.getType())) {
            int limit = parseInt(planBenefit.getBenefitValue(), 0);
            int used = currentUsed(userId, code, currentPeriod());
            vo.setUsed(used);
            vo.setRemaining(Math.max(limit - used, 0));
            if (used < limit) {
                vo.setAllowed(true);
            } else {
                deny(vo, planBenefit.getBenefitValue(), BenefitErrorCode.QUOTA_EXHAUSTED.getMessage());
            }
            return vo;
        }

        // tier 类型：放行，值交给业务逻辑自行判断
        vo.setAllowed(true);
        return vo;
    }

    @Override
    public BenefitCheckVO consume(Long userId, String code) {
        Benefit benefit = requireBenefit(code);
        if (!TYPE_QUOTA.equals(benefit.getType())) {
            throw new BusinessException(BenefitErrorCode.NOT_QUOTA_BENEFIT);
        }

        String planKey = currentPlanKey(userId);
        PlanBenefit planBenefit = findPlanBenefit(planKey, code);
        if (planBenefit == null) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_SUPPORTED);
        }

        int limit = parseInt(planBenefit.getBenefitValue(), 0);
        String period = currentPeriod();

        int updated = benefitUsageMapper.incrementIfBelowLimit(userId, code, period, limit);
        if (updated == 0) {
            BenefitUsage existing = benefitUsageMapper.selectByUserAndCodeAndPeriod(userId, code, period);
            if (existing != null || limit < 1) {
                throw new BusinessException(BenefitErrorCode.QUOTA_EXHAUSTED);
            }
            // 本期首次消费：插入用量记录；并发插入冲突时退回到原子 +1
            BenefitUsage usage = new BenefitUsage();
            usage.setUserId(userId);
            usage.setBenefitCode(code);
            usage.setPeriod(period);
            usage.setUsedCount(1);
            usage.setTenantId(0L);
            try {
                benefitUsageMapper.insert(usage);
            } catch (DuplicateKeyException e) {
                if (benefitUsageMapper.incrementIfBelowLimit(userId, code, period, limit) == 0) {
                    throw new BusinessException(BenefitErrorCode.QUOTA_EXHAUSTED);
                }
            }
        }

        BenefitUsage current = benefitUsageMapper.selectByUserAndCodeAndPeriod(userId, code, period);
        int used = current == null ? 0 : current.getUsedCount();

        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(true);
        vo.setCode(code);
        vo.setType(benefit.getType());
        vo.setValue(planBenefit.getBenefitValue());
        vo.setUsed(used);
        vo.setRemaining(Math.max(limit - used, 0));
        return vo;
    }

    /**
     * 查询套餐的全部权益值（带缓存，key 为 planKey）。
     */
    @Cacheable(cacheNames = "planBenefits", key = "#planKey")
    public List<PlanBenefit> listPlanBenefits(String planKey) {
        return planBenefitMapper.selectByPlanKey(planKey);
    }

    // ── private helpers ──

    /**
     * 当前有效套餐 key；无会员或已过期返回 free。
     */
    private String currentPlanKey(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            return FREE_PLAN_KEY;
        }
        return membership.getLevel();
    }

    private Benefit requireBenefit(String code) {
        Benefit benefit = benefitMapper.selectByCode(code);
        if (benefit == null) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_FOUND);
        }
        return benefit;
    }

    private PlanBenefit findPlanBenefit(String planKey, String code) {
        if (FREE_PLAN_KEY.equals(planKey)) {
            return null;
        }
        return listPlanBenefits(planKey).stream()
                .filter(pb -> code.equals(pb.getBenefitCode()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Benefit> loadActiveBenefitMap() {
        return benefitMapper.selectList(null).stream()
                .filter(b -> Integer.valueOf(1).equals(b.getStatus()))
                .collect(Collectors.toMap(Benefit::getCode, Function.identity()));
    }

    private int currentUsed(Long userId, String code, String period) {
        BenefitUsage usage = benefitUsageMapper.selectByUserAndCodeAndPeriod(userId, code, period);
        return usage == null ? 0 : usage.getUsedCount();
    }

    private String currentPeriod() {
        return YearMonth.now().toString();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void deny(BenefitCheckVO vo, String value, String message) {
        vo.setAllowed(false);
        vo.setValue(value);
        vo.setMessage(message);
    }
}
```

> 注意：`listPlanBenefits` 必须是 `public` 且通过代理调用才走缓存——同类内部 `this.listPlanBenefits(...)` 调用不经过 Spring AOP 代理时缓存不生效。此处可接受（功能正确，仅少一层缓存）；如需严格缓存，后续把缓存方法拆到独立 Bean。本计划保持简单，不在此拆分。

> `MembershipPlan.of(String)` 与 `getDisplayName()` 参考 `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/enums/MembershipPlan.java`，已实现并被 `MembershipServiceImpl` 使用。

- [ ] **Step 6: 运行测试确认全部通过**

Run: `cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn test -Dtest=BenefitServiceTest -q`
Expected: `Tests run: 14, Failures: 0, Errors: 0`

- [ ] **Step 7: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/service/ project/user/api/src/main/java/com/aichuangzuo/user/config/CaffeineConfig.java project/user/api/src/test/java/com/aichuangzuo/user/modules/benefit/
git commit -m "feat(user-api): 权益查询/校验/消费服务 + planBenefits Caffeine 缓存"
```

---

### Task 4: BenefitController

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/controller/BenefitController.java`

**Interfaces:**
- Consumes: Task 3 的 `BenefitService` 三个方法
- Produces: `GET /api/v1/user/benefits/me`、`POST /api/v1/user/benefits/check/{code}`、`POST /api/v1/user/benefits/consume/{code}`，前端 Task 5 按此路径封装 API

- [ ] **Step 1: 编写 Controller**

```java
package com.aichuangzuo.user.modules.benefit.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端会员权益接口。
 */
@RestController
@RequestMapping("/api/v1/user/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    /**
     * 查询当前用户权益。
     */
    @GetMapping("/me")
    public Result<UserBenefitVO> getMyBenefits() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(benefitService.getMyBenefits(userId));
    }

    /**
     * 校验单项权益。
     */
    @PostMapping("/check/{code}")
    public Result<BenefitCheckVO> check(@PathVariable String code) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(benefitService.check(userId, code));
    }

    /**
     * 消费一次配额（quota 类专用）。
     */
    @PostMapping("/consume/{code}")
    public Result<BenefitCheckVO> consume(@PathVariable String code) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(benefitService.consume(userId, code));
    }
}
```

- [ ] **Step 2: 编译 + 跑 benefit 全部测试**

Run: `cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api && /Users/panyong/software/maven/apache-maven-3.9.11/bin/mvn test -Dtest='BenefitServiceTest' -q`
Expected: BUILD SUCCESS，测试全绿

- [ ] **Step 3: 手动验证接口（可选但推荐）**

启动应用后用登录态 token 调 `GET http://localhost:8080/api/v1/user/benefits/me`，确认返回套餐与 15 项权益。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/benefit/controller/
git commit -m "feat(user-api): 权益查询/校验/消费接口"
```

---

### Task 5: 前端 useBenefits composable + API 封装 + ConsoleLayout 加载

**Files:**
- Create: `project/user/web/src/api/benefit.js`
- Create: `project/user/web/src/composables/useBenefits.js`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`（onMounted 中加载权益，约 line 2513-2518）

**Interfaces:**
- Consumes: Task 4 的 3 个端点
- Produces: `useBenefits()` 返回 `{ benefits, planKey, planName, loaded, loadBenefits, hasBenefit, benefitValue, benefitRemaining }`：
  - `benefits` — ref，code → item 的 Map 对象
  - `hasBenefit(code) → boolean` — boolean 类权益是否开通
  - `benefitValue(code) → string|null` — 原始权益值
  - `benefitRemaining(code) → number` — quota 类剩余额度（非 quota 返回 0）

- [ ] **Step 1: 编写 API 封装**

`src/api/benefit.js`：

```js
import request from '@/utils/request'

export function getMyBenefits() {
  return request.get('/benefits/me')
}

export function checkBenefit(code) {
  return request.post(`/benefits/check/${code}`)
}

export function consumeBenefit(code) {
  return request.post(`/benefits/consume/${code}`)
}
```

- [ ] **Step 2: 编写 useBenefits composable**

`src/composables/useBenefits.js`：

```js
import { ref } from 'vue'
import { getMyBenefits } from '@/api/benefit'

// 模块级 ref：单例模式，整个 console 共享一份权益数据（同 useUserProfile）。
const benefits = ref({})
const planKey = ref('free')
const planName = ref('免费版')
const expiresAt = ref('')
const loaded = ref(false)

/**
 * 会员权益 composable。
 * 登录后调用 loadBenefits() 加载；页面用 hasBenefit/benefitValue/benefitRemaining 控制功能。
 */
export function useBenefits() {

  async function loadBenefits() {
    try {
      const res = await getMyBenefits()
      const data = res.data || res
      planKey.value = data.planKey || 'free'
      planName.value = data.planName || '免费版'
      expiresAt.value = data.expiresAt || ''
      const map = {}
      for (const item of data.benefits || []) {
        map[item.code] = item
      }
      benefits.value = map
      loaded.value = true
    } catch (e) {
      // 加载失败按无权益处理，不打扰页面
      benefits.value = {}
      planKey.value = 'free'
    }
  }

  /** boolean 类权益是否开通。 */
  function hasBenefit(code) {
    const item = benefits.value[code]
    return !!item && item.value === 'true'
  }

  /** 原始权益值（tier/quota 类用）。 */
  function benefitValue(code) {
    const item = benefits.value[code]
    return item ? item.value : null
  }

  /** quota 类剩余额度。 */
  function benefitRemaining(code) {
    const item = benefits.value[code]
    return item && item.remaining != null ? item.remaining : 0
  }

  return {
    benefits,
    planKey,
    planName,
    expiresAt,
    loaded,
    loadBenefits,
    hasBenefit,
    benefitValue,
    benefitRemaining
  }
}
```

- [ ] **Step 3: ConsoleLayout onMounted 中加载权益**

在 `ConsoleLayout.vue` script 顶部 import 区（约 line 1162 附近）添加：

```js
import { useBenefits } from '@/composables/useBenefits'
```

在 setup 内（`const userProfile = useUserProfile()` 附近）添加：

```js
const { loadBenefits } = useBenefits()
```

在 `onMounted`（约 line 2513，与 `userProfile.loadProfile()` 并列）添加：

```js
onMounted(async () => {
  // ...已有逻辑保持不动
  userProfile.loadProfile()
  loadBenefits()
  // ...
})
```

- [ ] **Step 4: 前端构建验证**

Run: `cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && npx vite build`
Expected: 构建成功无报错

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/api/benefit.js project/user/web/src/composables/useBenefits.js project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(user-web): useBenefits composable + 登录后加载会员权益"
```

---

## Self-Review 记录

- **Spec 覆盖**：3 张表（Task 1）、15 项权益种子（Task 1）、缓存 10 分钟（Task 3 Step 3）、3 个 API（Task 4）、错误码 118001-118003（Task 2）、前端 composable（Task 5）、扩展方式=纯 INSERT（Task 1 模式即证明）。
- **Spec 偏差**（已确认可接受）：
  1. spec 写"Pinia store"，实际按代码库惯例用 composable（无 stores 目录）。
  2. `/me` 响应的 quota 项额外带 `used`/`remaining`，供前端直接显示剩余额度，免 N 次 check 调用。
  3. 新增错误码 118004（仅配额类可消费），spec 只列了 3 个；非 quota 调 consume 需要明确错误。
  4. `listPlanBenefits` 同类内部调用缓存不生效的取舍已在 Task 3 Step 5 注释说明。
- **类型一致**：`UserBenefitVO.BenefitItem`、`BenefitCheckVO` 字段在 Task 2 定义、Task 3 填充、Task 5 消费，命名一致（code/name/type/value/used/remaining）。
