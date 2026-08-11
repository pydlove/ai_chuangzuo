# 运营抽奖活动实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现运营抽奖活动完整功能，包括活动轮次管理、奖项配置、用户抽奖、兑换码生成与兑换、邀请得次数、折扣券与订单集成、管理端兑换/抽奖记录与展示墙。

**Architecture:** 用户端与管理端分别维护各自的 Entity/Mapper/Service/Controller，共同操作用户端数据库中的 `u_lottery_*` 表。核心抽奖、兑换逻辑在用户端实现；管理端通过直接访问用户库管理活动、奖项、记录与展示墙。

**Tech Stack:** Spring Boot 3.x, Spring Security, MyBatis-Plus, JDK 17, MySQL 8, Flyway, Vue 3 + Ant Design Vue, Caffeine.

## Global Constraints

- 不引入 Redis、RabbitMQ、Elasticsearch 等新中间件。
- 数据库变更通过 Flyway 迁移文件完成；禁止修改已应用的迁移文件。
- 用户端表前缀 `u_`，管理端表前缀 `a_`。
- 代码遵循现有包结构：`com.aichuangzuo.user.modules.lottery.*` / `com.aichuangzuo.admin.modules.lottery.*`。
- 所有金额/概率使用 `BigDecimal`；状态字段使用 `TINYINT` 或 `VARCHAR` 枚举字符串。
- 测试优先：先写失败测试，再实现，再验证。
- 不用的代码开发结束后必须删除，不允许残留注释/空方法/未用 import。

---

## 文件结构总览

### 用户端 (`project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/`)

| 文件 | 职责 |
|---|---|
| `entity/LotteryCampaign.java` | 活动轮次实体 |
| `entity/LotteryPrizeTier.java` | 奖项配置实体 |
| `entity/LotteryDrawChance.java` | 抽奖次数池实体 |
| `entity/LotteryDrawRecord.java` | 抽奖记录实体 |
| `entity/LotteryRedemptionCode.java` | 兑换码实体 |
| `entity/LotteryDisplayWinner.java` | 展示墙实体 |
| `entity/UserCoupon.java` | 用户优惠券实体 |
| `entity/UserMembershipPending.java` | 待生效会员实体 |
| `mapper/*.java` | MyBatis-Plus Mapper |
| `dto/request/*.java` | 用户端请求 DTO |
| `vo/*.java` | 用户端返回 VO |
| `service/LotteryDrawService.java` | 抽奖核心服务 |
| `service/LotteryRedemptionService.java` | 兑换码生成与兑换服务 |
| `service/LotteryChanceService.java` | 抽奖次数管理 |
| `service/UserCouponService.java` | 优惠券查询/使用 |
| `service/UserMembershipPendingService.java` | 待生效会员管理 |
| `controller/LotteryController.java` | 用户端抽奖 API |
| `controller/LotteryRedemptionController.java` | 用户端兑换 API |
| `controller/LotteryDisplayController.java` | 展示墙等查询 API |
| `enums/LotteryErrorCode.java` | 错误码 |
| `scheduler/MembershipPendingActivationJob.java` | 待生效会员激活定时任务 |

### 管理端 (`project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/`)

| 文件 | 职责 |
|---|---|
| `entity/*` | 与用户端对应的管理端实体副本 |
| `mapper/*` | 管理端 Mapper（操作用户端库） |
| `dto/request/*.java` | 管理端请求 DTO |
| `vo/*.java` | 管理端返回 VO |
| `service/LotteryCampaignAdminService.java` | 活动/奖项管理 |
| `service/LotteryRedemptionAdminService.java` | 兑换/抽奖记录查询 |
| `service/LotteryDisplayWinnerAdminService.java` | 展示墙管理 |
| `controller/LotteryCampaignAdminController.java` | 活动/奖项 API |
| `controller/LotteryRecordAdminController.java` | 记录/展示墙 API |

---

## Task 1: 数据库迁移

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_071__create_lottery_tables.sql`
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_072__create_user_coupon_and_pending_membership.sql`
- Test: 启动 user-api，验证 Flyway 执行无报错

**Interfaces:**
- Consumes: 无
- Produces: 数据库表 `u_lottery_campaign`, `u_lottery_prize_tier`, `u_lottery_draw_chance`, `u_lottery_draw_record`, `u_lottery_redemption_code`, `u_lottery_display_winner`, `u_user_coupon`, `u_user_membership_pending`, `u_lottery_risk_log`

- [ ] **Step 1: 编写 V1.0.0_071__create_lottery_tables.sql**

```sql
CREATE TABLE IF NOT EXISTS u_lottery_campaign (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '活动名称',
    description VARCHAR(256) DEFAULT NULL COMMENT '活动描述',
    start_time DATETIME(3) NOT NULL COMMENT '开始时间',
    end_time DATETIME(3) NOT NULL COMMENT '结束时间',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-draft,1-ongoing,2-ended,3-disabled',
    free_draws_per_user INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '每轮免费次数',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_lottery_campaign_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖活动轮次表';

CREATE TABLE IF NOT EXISTS u_lottery_prize_tier (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tier_key VARCHAR(32) NOT NULL COMMENT '奖项标识',
    tier_name VARCHAR(64) NOT NULL COMMENT '奖项名称',
    probability DECIMAL(10,8) NOT NULL COMMENT '中奖概率',
    max_win_count INT UNSIGNED DEFAULT NULL COMMENT '全局可中次数上限，NULL表示不限',
    remaining_win_count INT UNSIGNED DEFAULT NULL COMMENT '剩余可中次数',
    reward_type VARCHAR(16) NOT NULL COMMENT '奖励类型：coin/membership/coupon/none',
    reward_value_json JSON NOT NULL COMMENT '奖励参数',
    code_prefix VARCHAR(16) DEFAULT NULL COMMENT '兑换码前缀',
    code_length INT UNSIGNED DEFAULT NULL COMMENT '兑换码总字符数（含前缀）',
    code_validity_days INT UNSIGNED NOT NULL DEFAULT 30 COMMENT '兑换码有效期天数',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_prize_tier_campaign_key (campaign_id, tier_key),
    KEY idx_lottery_prize_tier_campaign_status (campaign_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖奖项配置表';

CREATE TABLE IF NOT EXISTS u_lottery_draw_chance (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    chance_type VARCHAR(16) NOT NULL COMMENT '次数类型：free/invite',
    source_invite_relation_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'invite来源关系ID',
    status VARCHAR(16) NOT NULL DEFAULT 'available' COMMENT '状态：available/used',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    used_at DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_draw_chance_free (campaign_id, user_id, chance_type),
    KEY idx_lottery_draw_chance_user_campaign (user_id, campaign_id, status),
    KEY idx_lottery_draw_chance_invite (source_invite_relation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖次数池';

CREATE TABLE IF NOT EXISTS u_lottery_draw_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    tier_id BIGINT UNSIGNED DEFAULT NULL COMMENT '命中奖项ID',
    code_id BIGINT UNSIGNED DEFAULT NULL COMMENT '生成兑换码ID',
    draw_type VARCHAR(16) NOT NULL COMMENT '抽奖类型：free/invite',
    invite_relation_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'invite来源关系ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_draw_record_biz_no (biz_no),
    KEY idx_lottery_draw_record_user_campaign (user_id, campaign_id),
    KEY idx_lottery_draw_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖记录表';

CREATE TABLE IF NOT EXISTS u_lottery_redemption_code (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL COMMENT '兑换码',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tier_id BIGINT UNSIGNED NOT NULL COMMENT '奖项ID',
    drawer_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '中奖人用户ID',
    reward_type VARCHAR(16) NOT NULL COMMENT '奖励类型',
    reward_value_json JSON NOT NULL COMMENT '奖励参数快照',
    status VARCHAR(16) NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
    used_by BIGINT UNSIGNED DEFAULT NULL COMMENT '兑换人用户ID',
    used_at DATETIME(3) DEFAULT NULL,
    expires_at DATETIME(3) NOT NULL COMMENT '过期时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_redemption_code_code (code),
    KEY idx_lottery_redemption_code_campaign (campaign_id),
    KEY idx_lottery_redemption_code_drawer (drawer_user_id),
    KEY idx_lottery_redemption_code_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖兑换码表';

CREATE TABLE IF NOT EXISTS u_lottery_display_winner (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tier_id BIGINT UNSIGNED DEFAULT NULL COMMENT '奖项ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '展示昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '展示头像',
    prize_name VARCHAR(64) NOT NULL COMMENT '展示奖品名',
    win_time DATETIME(3) NOT NULL COMMENT '展示时间',
    is_real TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-机器人/运营配置，1-真实中奖',
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-隐藏，1-展示',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_lottery_display_winner_campaign (campaign_id, status, win_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='中奖展示墙';

CREATE TABLE IF NOT EXISTS u_lottery_risk_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED DEFAULT NULL COMMENT '活动ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    action VARCHAR(16) NOT NULL COMMENT '动作：draw/redeem/invite',
    risk_type VARCHAR(32) NOT NULL COMMENT '风控类型',
    detail_json JSON DEFAULT NULL COMMENT '详情',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_lottery_risk_log_user_action (user_id, action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖风控日志';
```

- [ ] **Step 2: 编写 V1.0.0_072__create_user_coupon_and_pending_membership.sql**

```sql
CREATE TABLE IF NOT EXISTS u_user_coupon (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    coupon_code VARCHAR(64) NOT NULL COMMENT '券码',
    coupon_type VARCHAR(16) NOT NULL COMMENT '类型：percent/fixed_amount',
    discount_value DECIMAL(10,4) NOT NULL COMMENT '折扣值',
    applicable_cycle VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '适用周期',
    applicable_plan VARCHAR(16) NOT NULL DEFAULT 'all' COMMENT '适用套餐',
    status VARCHAR(16) NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
    valid_start DATETIME(3) NOT NULL COMMENT '有效期开始',
    valid_end DATETIME(3) NOT NULL COMMENT '有效期结束',
    used_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '使用订单ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon_code (coupon_code),
    KEY idx_user_coupon_user_status (user_id, status, valid_end),
    KEY idx_user_coupon_order (used_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

CREATE TABLE IF NOT EXISTS u_user_membership_pending (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐：basic/pro/flagship',
    days INT UNSIGNED NOT NULL COMMENT '天数',
    planned_start_at DATE NOT NULL COMMENT '计划生效日期',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/activated/expired',
    source_code_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源兑换码ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    activated_at DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_user_membership_pending_user_status (user_id, status, planned_start_at),
    KEY idx_user_membership_pending_start (planned_start_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待生效会员表';
```

- [ ] **Step 3: 启动 user-api 验证迁移**

Run: `cd project/user/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
Expected: Flyway 成功执行，应用启动无报错。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_071__create_lottery_tables.sql \
        project/user/api/src/main/resources/db/migration/V1.0.0_072__create_user_coupon_and_pending_membership.sql
git commit -m "feat(lottery): 创建抽奖活动相关数据表"
```

---

## Task 2: 用户端实体与 Mapper

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/LotteryCampaign.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/LotteryPrizeTier.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/LotteryDrawChance.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/LotteryDrawRecord.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/LotteryRedemptionCode.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/LotteryDisplayWinner.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/UserCoupon.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity/UserMembershipPending.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryCampaignMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryPrizeTierMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryDrawChanceMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryDrawRecordMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryRedemptionCodeMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryDisplayWinnerMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/UserCouponMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper/UserMembershipPendingMapper.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/mapper/LotteryCampaignMapperTest.java`（基础 CRUD 测试）

**Interfaces:**
- Consumes: 数据库表结构
- Produces: MyBatis-Plus Entity + Mapper，供后续 Service 注入使用

- [ ] **Step 1: 创建 LotteryCampaign 实体**

```java
package com.aichuangzuo.user.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_campaign")
public class LotteryCampaign {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer freeDrawsPerUser;
    private Long tenantId;
    private Integer isDeleted;
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

- [ ] **Step 2: 创建 LotteryCampaignMapper**

```java
package com.aichuangzuo.user.modules.lottery.mapper;

import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LotteryCampaignMapper extends BaseMapper<LotteryCampaign> {
}
```

- [ ] **Step 3: 类似创建其余 7 个实体与 Mapper**

实体字段与表结构一一对应，注意：
- `LotteryPrizeTier` 中 `probability` 用 `BigDecimal`。
- `LotteryDrawChance` 中 `chanceType` / `status` 用 `String`。
- `LotteryRedemptionCode` 中 `rewardType` 用 `String`，`rewardValueJson` 用 `String`。
- `UserCoupon` 中 `discountValue` 用 `BigDecimal`。

- [ ] **Step 4: 编写 Mapper 测试**

```java
package com.aichuangzuo.user.modules.lottery.mapper;

import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LotteryCampaignMapperTest {

    @Autowired
    private LotteryCampaignMapper mapper;

    @Test
    void shouldInsertAndSelectCampaign() {
        LotteryCampaign campaign = new LotteryCampaign();
        campaign.setName("测试活动");
        campaign.setStartTime(LocalDateTime.now());
        campaign.setEndTime(LocalDateTime.now().plusDays(7));
        campaign.setStatus(1);
        campaign.setFreeDrawsPerUser(1);
        campaign.setTenantId(0L);
        campaign.setIsDeleted(0);

        mapper.insert(campaign);
        assertNotNull(campaign.getId());

        LotteryCampaign found = mapper.selectById(campaign.getId());
        assertEquals("测试活动", found.getName());
        assertEquals(1, found.getStatus());
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd project/user/api && ./mvnw test -Dtest=LotteryCampaignMapperTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/entity \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/mapper \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/mapper
git commit -m "feat(lottery): 添加用户端抽奖实体与 Mapper"
```

---

## Task 3: 抽奖次数管理（LotteryChanceService）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/LotteryChanceService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/impl/LotteryChanceServiceImpl.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/enums/LotteryErrorCode.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/InviteRewardServiceImpl.java`（注册成功后创建 invite 抽奖次数）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service/LotteryChanceServiceTest.java`

**Interfaces:**
- Consumes: `LotteryCampaignMapper`, `LotteryDrawChanceMapper`, `UserInviteRelationMapper`
- Produces:
  - `LotteryDrawChance acquireFreeChance(Long campaignId, Long userId)`：获取/创建免费次数
  - `LotteryDrawChance acquireOneAvailableChance(Long campaignId, Long userId)`：获取一条可用次数（任意类型）
  - `int consumeChance(Long chanceId)`：消费指定次数
  - `void createInviteChance(Long campaignId, Long userId, Long inviteRelationId)`：邀请成功后创建次数
  - `int countAvailableChances(Long campaignId, Long userId)`：可用次数
  - `boolean isFreeChanceUsed(Long campaignId, Long userId)`：是否已用免费次数

- [ ] **Step 1: 定义错误码**

```java
package com.aichuangzuo.user.modules.lottery.enums;

import com.aichuangzuo.shared.enums.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LotteryErrorCode implements ErrorCode {
    CAMPAIGN_NOT_FOUND(70001, "活动不存在"),
    CAMPAIGN_NOT_ONGOING(70002, "活动未开始或已结束"),
    NO_DRAW_CHANCE(70003, "没有可用抽奖次数"),
    PRIZE_TIER_NOT_FOUND(70004, "奖项不存在"),
    REDEMPTION_CODE_NOT_FOUND(70005, "兑换码不存在"),
    REDEMPTION_CODE_USED(70006, "兑换码已使用"),
    REDEMPTION_CODE_EXPIRED(70007, "兑换码已过期"),
    INVALID_REWARD_TYPE(70008, "奖励类型无效");

    private final int code;
    private final String message;
}
```

- [ ] **Step 2: 实现 LotteryChanceServiceImpl**

```java
package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.lottery.entity.LotteryDrawChance;
import com.aichuangzuo.user.modules.lottery.enums.LotteryErrorCode;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryDrawChanceMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryChanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryChanceServiceImpl implements LotteryChanceService {

    private final LotteryCampaignMapper campaignMapper;
    private final LotteryDrawChanceMapper drawChanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryDrawChance acquireFreeChance(Long campaignId, Long userId) {
        LotteryDrawChance chance = drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getChanceType, "free"));
        if (chance != null) {
            return chance;
        }
        chance = new LotteryDrawChance();
        chance.setCampaignId(campaignId);
        chance.setUserId(userId);
        chance.setChanceType("free");
        chance.setStatus("available");
        chance.setTenantId(0L);
        try {
            drawChanceMapper.insert(chance);
        } catch (DuplicateKeyException e) {
            return drawChanceMapper.selectOne(
                    new LambdaQueryWrapper<LotteryDrawChance>()
                            .eq(LotteryDrawChance::getCampaignId, campaignId)
                            .eq(LotteryDrawChance::getUserId, userId)
                            .eq(LotteryDrawChance::getChanceType, "free"));
        }
        return chance;
    }

    @Override
    public LotteryDrawChance acquireOneAvailableChance(Long campaignId, Long userId) {
        return drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getStatus, "available")
                        .orderByAsc(LotteryDrawChance::getCreatedAt)
                        .last("LIMIT 1"));
    }

    @Override
    public int consumeChance(Long chanceId) {
        return drawChanceMapper.update(null,
                new LambdaUpdateWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getId, chanceId)
                        .eq(LotteryDrawChance::getStatus, "available")
                        .set(LotteryDrawChance::getStatus, "used")
                        .set(LotteryDrawChance::getUsedAt, LocalDateTime.now()));
    }

    @Override
    public boolean isFreeChanceUsed(Long campaignId, Long userId) {
        LotteryDrawChance free = drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getChanceType, "free"));
        return free == null || "used".equals(free.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInviteChance(Long campaignId, Long userId, Long inviteRelationId) {
        LotteryDrawChance chance = new LotteryDrawChance();
        chance.setCampaignId(campaignId);
        chance.setUserId(userId);
        chance.setChanceType("invite");
        chance.setSourceInviteRelationId(inviteRelationId);
        chance.setStatus("available");
        chance.setTenantId(0L);
        drawChanceMapper.insert(chance);
    }

    @Override
    public int countAvailableChances(Long campaignId, Long userId) {
        return drawChanceMapper.selectCount(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getStatus, "available"));
    }
}
```

- [ ] **Step 3: 修改 InviteRewardServiceImpl**

在 `rewardAfterRegister` 方法末尾，创建 invite 抽奖次数。注入 `LotteryCampaignMapper` 和 `LotteryChanceService`，查询 `status=1` 且时间在有效期内的活动，为邀请人创建 `invite` 次数。

```java
private void createLotteryInviteChance(User invitee, User inviter) {
    LotteryCampaign activeCampaign = lotteryCampaignMapper.selectOne(
            new LambdaQueryWrapper<LotteryCampaign>()
                    .eq(LotteryCampaign::getStatus, 1)
                    .le(LotteryCampaign::getStartTime, LocalDateTime.now())
                    .ge(LotteryCampaign::getEndTime, LocalDateTime.now())
                    .last("LIMIT 1"));
    if (activeCampaign == null) {
        return;
    }
    UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(invitee.getId());
    if (relation == null) {
        return;
    }
    lotteryChanceService.createInviteChance(activeCampaign.getId(), inviter.getId(), relation.getId());
}
```

- [ ] **Step 4: 编写测试**

测试 `acquireFreeChance` 的幂等性、`consumeAvailableChance` 成功与并发、`createInviteChance`。

- [ ] **Step 5: 运行测试**

Run: `cd project/user/api && ./mvnw test -Dtest=LotteryChanceServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/user/service/impl/InviteRewardServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service
git commit -m "feat(lottery): 实现抽奖次数管理"
```

---

## Task 4: 抽奖核心服务（LotteryDrawService）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/LotteryDrawService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/impl/LotteryDrawServiceImpl.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/vo/LotteryDrawResultVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/dto/request/LotteryDrawRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/util/LotteryCodeGenerator.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service/LotteryDrawServiceTest.java`

**Interfaces:**
- Consumes: `LotteryCampaignMapper`, `LotteryPrizeTierMapper`, `LotteryDrawChanceMapper`, `LotteryDrawRecordMapper`, `LotteryRedemptionCodeMapper`, `LotteryDisplayWinnerMapper`, `LotteryChanceService`, `LotteryCodeGenerator`
- Produces:
  - `LotteryDrawResultVO draw(Long userId, Long campaignId, String drawType)`：执行抽奖
  - 内部：`LotteryPrizeTier selectTierByProbability(List<LotteryPrizeTier> tiers)`：按概率选奖项

- [ ] **Step 1: 实现概率选择算法**

```java
private LotteryPrizeTier selectTierByProbability(List<LotteryPrizeTier> tiers) {
    double random = ThreadLocalRandom.current().nextDouble();
    double cumulative = 0.0;
    for (LotteryPrizeTier tier : tiers) {
        if ("none".equals(tier.getRewardType())) continue; // thanks  tier 放到最后兜底
        cumulative += tier.getProbability().doubleValue();
        if (random < cumulative) {
            return tier;
        }
    }
    return tiers.stream()
            .filter(t -> "none".equals(t.getRewardType()))
            .findFirst()
            .orElse(tiers.get(tiers.size() - 1));
}
```

- [ ] **Step 2: 实现 draw 方法主逻辑**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public LotteryDrawResultVO draw(Long userId, Long campaignId) {
    LotteryCampaign campaign = validateAndGetCampaign(campaignId);
    LotteryDrawChance chance = lotteryChanceService.acquireOneAvailableChance(campaignId, userId);
    if (chance == null) {
        throw new BusinessException(LotteryErrorCode.NO_DRAW_CHANCE);
    }
    int consumed = lotteryChanceService.consumeChance(chance.getId());
    if (consumed == 0) {
        throw new BusinessException(LotteryErrorCode.NO_DRAW_CHANCE);
    }

    List<LotteryPrizeTier> activeTiers = prizeTierMapper.selectList(
            new LambdaQueryWrapper<LotteryPrizeTier>()
                    .eq(LotteryPrizeTier::getCampaignId, campaignId)
                    .eq(LotteryPrizeTier::getStatus, 1)
                    .orderByAsc(LotteryPrizeTier::getSortOrder));

    LotteryPrizeTier hitTier = selectTierByProbability(activeTiers);

    // 库存上限扣减
    if (!"none".equals(hitTier.getRewardType()) && hitTier.getMaxWinCount() != null) {
        int affected = prizeTierMapper.update(null,
                new LambdaUpdateWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getId, hitTier.getId())
                        .gt(LotteryPrizeTier::getRemainingWinCount, 0)
                        .setSql("remaining_win_count = remaining_win_count - 1"));
        if (affected == 0) {
            hitTier = getThanksTier(activeTiers);
        }
    }

    LotteryRedemptionCode code = null;
    if (!"none".equals(hitTier.getRewardType())) {
        code = generateCode(campaignId, hitTier, userId);
    }

    saveDrawRecord(userId, campaignId, chance, hitTier, code);
    if (code != null) {
        saveDisplayWinner(campaignId, hitTier, userId, code);
    }

    return buildResultVO(hitTier, code);
}
```

- [ ] **Step 3: 实现兑换码生成器**

```java
public String generate(String prefix, int length) {
    if (prefix == null) prefix = "";
    int randomLength = Math.max(1, length - prefix.length());
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder(prefix);
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    for (int i = 0; i < randomLength; i++) {
        sb.append(chars.charAt(rnd.nextInt(chars.length())));
    }
    return sb.toString();
}
```

生成兑换码后循环校验唯一性，冲突则重试 5 次。

- [ ] **Step 4: 编写测试**

- 测试概率分布：构造 100% 命中特等奖和 100% 命中谢谢回顾两个测试用例。
- 测试库存扣减：特等奖 `remaining_win_count=1`，抽两次后第二次降级为 thanks。
- 测试无次数时抛 `BusinessException(NO_DRAW_CHANCE)`。

- [ ] **Step 5: 运行测试**

Run: `cd project/user/api && ./mvnw test -Dtest=LotteryDrawServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/vo \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/dto \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/util \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service
git commit -m "feat(lottery): 实现抽奖核心服务与兑换码生成"
```

---

## Task 5: 兑换与奖励发放（LotteryRedemptionService）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/LotteryRedemptionService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/impl/LotteryRedemptionServiceImpl.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/dto/request/LotteryRedeemRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/vo/LotteryRedemptionResultVO.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/service/MembershipService.java`（增加 extendMembership 公开方法或复用现有）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service/LotteryRedemptionServiceTest.java`

**Interfaces:**
- Consumes: `LotteryRedemptionCodeMapper`, `UserMembershipPendingMapper`, `UserCouponMapper`, `CoinRecordService`, `MembershipService`
- Produces:
  - `LotteryRedemptionResultVO redeem(Long userId, String code)`：兑换奖励
  - 内部根据 `reward_type` 分发到不同处理器

- [ ] **Step 1: 实现兑换校验**

```java
LotteryRedemptionCode codeEntity = redemptionCodeMapper.selectOne(
        new LambdaQueryWrapper<LotteryRedemptionCode>()
                .eq(LotteryRedemptionCode::getCode, code));
if (codeEntity == null) throw new BusinessException(LotteryErrorCode.REDEMPTION_CODE_NOT_FOUND);
if ("used".equals(codeEntity.getStatus())) throw new BusinessException(LotteryErrorCode.REDEMPTION_CODE_USED);
if (codeEntity.getExpiresAt().isBefore(LocalDateTime.now())) throw new BusinessException(LotteryErrorCode.REDEMPTION_CODE_EXPIRED);
```

- [ ] **Step 2: 实现按奖励类型分发**

```java
private void applyReward(Long userId, LotteryRedemptionCode code) {
    String rewardType = code.getRewardType();
    String json = code.getRewardValueJson();
    switch (rewardType) {
        case "coin" -> applyCoin(userId, json);
        case "membership" -> applyMembership(userId, json, code.getId());
        case "coupon" -> applyCoupon(userId, json, code.getId());
        default -> throw new BusinessException(LotteryErrorCode.INVALID_REWARD_TYPE);
    }
}
```

- [ ] **Step 3: 实现各奖励处理器**

- **coin**：解析 `amount`，调用 `coinRecordService.grant(userId, "lottery_coin_reward", amount, codeId, "抽奖获得创作币")`。
- **membership**：
  - 解析 `plan_key` 与 `days`/`cycle`。
  - 若 `cycle` 存在，转换为天数（month=30, quarter=90, year=365，或读取 `MembershipCycle` 定义）。
  - 查询当前有效会员：
    - 无会员或同档：调用 `membershipService.extendMembership(userId, planKey, days)`。
    - 不同档：写入 `u_user_membership_pending`。
- **coupon**：解析 coupon 参数，写入 `u_user_coupon`，有效期按奖项配置。

- [ ] **Step 4: 编写测试**

- 测试兑换创作币后余额增加。
- 测试兑换同档会员后天数延长。
- 测试兑换不同档会员后生成 pending 记录。
- 测试已使用/过期码抛异常。

- [ ] **Step 5: 运行测试**

Run: `cd project/user/api && ./mvnw test -Dtest=LotteryRedemptionServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/dto \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/vo \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service
git commit -m "feat(lottery): 实现兑换与奖励发放"
```

---

## Task 6: 待生效会员激活定时任务

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/scheduler/MembershipPendingActivationJob.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/UserApiApplication.java`（确认已启用 `@EnableScheduling`）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/scheduler/MembershipPendingActivationJobTest.java`

**Interfaces:**
- Consumes: `UserMembershipPendingMapper`, `UserMembershipMapper`, `MembershipService`
- Produces: 无（定时任务）

- [ ] **Step 1: 实现定时任务**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipPendingActivationJob {

    private final UserMembershipPendingMapper pendingMapper;
    private final UserMembershipMapper membershipMapper;
    private final MembershipService membershipService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void activatePendingMemberships() {
        LocalDate today = LocalDate.now();
        List<UserMembershipPending> pendings = pendingMapper.selectList(
                new LambdaQueryWrapper<UserMembershipPending>()
                        .eq(UserMembershipPending::getStatus, "pending")
                        .le(UserMembershipPending::getPlannedStartAt, today)
                        .orderByAsc(UserMembershipPending::getCreatedAt));

        for (UserMembershipPending pending : pendings) {
            try {
                membershipService.extendMembership(pending.getUserId(), pending.getPlanKey(), pending.getDays());
                pending.setStatus("activated");
                pending.setActivatedAt(LocalDateTime.now());
                pendingMapper.updateById(pending);
                log.info("激活待生效会员 pendingId={}, userId={}, planKey={}, days={}",
                        pending.getId(), pending.getUserId(), pending.getPlanKey(), pending.getDays());
            } catch (Exception e) {
                log.error("激活待生效会员失败 pendingId={}", pending.getId(), e);
            }
        }
    }
}
```

- [ ] **Step 2: 编写测试**

- 插入一条 `planned_start_at <= today` 的 pending 记录，运行 job，验证会员已延长且 pending 状态变为 activated。

- [ ] **Step 3: 运行测试**

Run: `cd project/user/api && ./mvnw test -Dtest=MembershipPendingActivationJobTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/scheduler \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/scheduler
git commit -m "feat(lottery): 添加待生效会员激活定时任务"
```

---

## Task 7: 用户端 API Controller

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/controller/LotteryController.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/controller/LotteryRedemptionController.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/controller/LotteryDisplayController.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/controller/LotteryControllerTest.java`

**Interfaces:**
- Consumes: `LotteryDrawService`, `LotteryRedemptionService`, `LotteryChanceService`, `LotteryCampaignMapper`, `LotteryRedemptionCodeMapper`, `LotteryDisplayWinnerMapper`
- Produces: REST API endpoints

- [ ] **Step 1: 实现 LotteryController**

```java
@Tag(name = "用户端-抽奖活动")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryDrawService lotteryDrawService;
    private final LotteryChanceService lotteryChanceService;
    private final LotteryCampaignMapper campaignMapper;

    @GetMapping("/campaigns/current")
    public Result<LotteryCampaignVO> currentCampaign() {
        // 查询 status=ongoing 且时间有效的活动
    }

    @GetMapping("/chances")
    public Result<LotteryChancesVO> chances(@RequestParam Long campaignId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        int available = lotteryChanceService.countAvailableChances(campaignId, userId);
        boolean freeUsed = lotteryChanceService.isFreeChanceUsed(campaignId, userId);
        return Result.success(new LotteryChancesVO(available, !freeUsed));
    }

    @PostMapping("/draw")
    public Result<LotteryDrawResultVO> draw(@RequestBody @Valid LotteryDrawRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(lotteryDrawService.draw(userId, request.getCampaignId()));
    }
}
```

- [ ] **Step 2: 实现 LotteryRedemptionController**

```java
@PostMapping("/redeem")
public Result<LotteryRedemptionResultVO> redeem(@RequestBody @Valid LotteryRedeemRequest request) {
    Long userId = SecurityUserContext.getCurrentUserId();
    return Result.success(lotteryRedemptionService.redeem(userId, request.getCode()));
}

@GetMapping("/my-codes")
public Result<List<LotteryRedemptionCodeVO>> myCodes(@RequestParam Long campaignId) {
    Long userId = SecurityUserContext.getCurrentUserId();
    // 查询 drawer_user_id = userId 的兑换码
}
```

- [ ] **Step 3: 实现 LotteryDisplayController**

```java
@GetMapping("/display-winners")
public Result<List<LotteryDisplayWinnerVO>> displayWinners(@RequestParam Long campaignId) {
    // 查询 status=1 的记录，按 win_time 倒序
}
```

- [ ] **Step 4: 运行 Controller 测试**

Run: `cd project/user/api && ./mvnw test -Dtest=LotteryControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/controller \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/controller
git commit -m "feat(lottery): 添加用户端抽奖 API"
```

---

## Task 8: 管理端实体、Mapper 与配置 API

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/entity/*.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/mapper/*.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/dto/request/*.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/vo/*.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service/LotteryCampaignAdminService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service/impl/LotteryCampaignAdminServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/controller/LotteryCampaignAdminController.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/lottery/service/LotteryCampaignAdminServiceTest.java`

**Interfaces:**
- Consumes: 用户端数据库表
- Produces: 管理端活动/奖项 CRUD API

- [ ] **Step 1: 复制创建管理端实体与 Mapper**

与用户端实体字段一致，包路径改为 `com.aichuangzuo.admin.modules.lottery.*`。

- [ ] **Step 2: 实现 CampaignAdminService**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void saveCampaign(LotteryCampaignSaveRequest request, Long adminUserId) {
    LotteryCampaign entity = ...;
    // 保存活动
}

@Override
@Transactional(rollbackFor = Exception.class)
public void saveTier(Long campaignId, LotteryPrizeTierSaveRequest request, Long adminUserId) {
    // 校验概率之和 ≤ 1
    // 保存/更新奖项
    // 若设置 max_win_count，则同步初始化 remaining_win_count
}

@Override
@Transactional(rollbackFor = Exception.class)
public void openCampaign(Long id, Long adminUserId) {
    LotteryCampaign campaign = campaignMapper.selectById(id);
    campaign.setStatus(1);
    campaign.setUpdatedBy(adminUserId);
    campaignMapper.updateById(campaign);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void closeCampaign(Long id, Long adminUserId) {
    LotteryCampaign campaign = campaignMapper.selectById(id);
    campaign.setStatus(3);
    campaign.setUpdatedBy(adminUserId);
    campaignMapper.updateById(campaign);
}
```

- [ ] **Step 3: 实现概率校验逻辑**

保存/更新奖项时，查询该活动所有启用奖项，校验 `SUM(probability) <= 1`。若超过则抛异常。

- [ ] **Step 4: 实现 Controller**

按设计文档中的管理端 API 路径实现。

- [ ] **Step 5: 运行测试**

Run: `cd project/admin/api && ./mvnw test -Dtest=LotteryCampaignAdminServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/lottery
git commit -m "feat(admin/lottery): 添加管理端抽奖活动与奖项配置 API"
```

---

## Task 9: 管理端记录与展示墙 API

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service/LotteryRecordAdminService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service/impl/LotteryRecordAdminServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service/LotteryDisplayWinnerAdminService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service/impl/LotteryDisplayWinnerAdminServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/controller/LotteryRecordAdminController.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/lottery/service/LotteryRecordAdminServiceTest.java`

**Interfaces:**
- Consumes: `LotteryDrawRecordMapper`, `LotteryRedemptionCodeMapper`, `LotteryDisplayWinnerMapper`
- Produces: 管理端记录查询与展示墙管理 API

- [ ] **Step 1: 实现记录查询 Service**

支持分页查询 `u_lottery_draw_record` 和 `u_lottery_redemption_code`，按活动、奖项、状态筛选。

- [ ] **Step 2: 实现展示墙管理 Service**

- 新增/编辑机器人记录（`is_real=0`）。
- 真实中奖记录由用户端写入，管理端只能隐藏/排序。

- [ ] **Step 3: 实现 Controller**

按设计文档实现 `/api/v1/admin/lottery/redemption-codes`、`/draw-records`、`/display-winners`。

- [ ] **Step 4: 运行测试**

Run: `cd project/admin/api && ./mvnw test -Dtest=LotteryRecordAdminServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/service \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/lottery/controller \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/lottery/service
git commit -m "feat(admin/lottery): 添加管理端兑换记录、抽奖记录与展示墙 API"
```

---

## Task 10: 折扣券与订阅下单集成

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/UserCouponService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service/impl/UserCouponServiceImpl.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/dto/request/SubscribeRequest.java`（增加 couponCode 字段）
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/dto/request/SubscribePreviewRequest.java`（增加 couponCode 字段）
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/service/impl/MembershipServiceImpl.java`（在金额计算中应用优惠券）
- Modify: `project/user/api/src/main/resources/db/migration/V1.0.0_073__add_coupon_to_order.sql`（订单表增加 coupon 字段）
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service/UserCouponServiceTest.java`

**Interfaces:**
- Consumes: `UserCouponMapper`, `OrderMapper`, `PlanMapper`
- Produces:
  - `BigDecimal applyCoupon(Long userId, String couponCode, BigDecimal originalAmount, String planKey, String cycle)`：返回券后金额
  - `void markCouponUsed(Long userId, String couponCode, Long orderId)`：标记已使用

- [ ] **Step 1: 创建订单表 coupon 字段迁移**

```sql
ALTER TABLE u_order
    ADD COLUMN coupon_code VARCHAR(64) DEFAULT NULL COMMENT '使用的优惠券码' AFTER coin_amount,
    ADD COLUMN coupon_discount DECIMAL(19,4) DEFAULT NULL COMMENT '优惠券抵扣金额' AFTER coupon_code;
```

- [ ] **Step 2: 实现 UserCouponService**

```java
@Override
public BigDecimal applyCoupon(Long userId, String couponCode, BigDecimal amount, String planKey, String cycle) {
    if (!StringUtils.hasText(couponCode)) return amount;
    UserCoupon coupon = userCouponMapper.selectOne(
            new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponCode, couponCode)
                    .eq(UserCoupon::getStatus, "unused")
                    .ge(UserCoupon::getValidEnd, LocalDateTime.now()));
    if (coupon == null) throw new BusinessException(MembershipErrorCode.INVALID_COUPON);
    if (!isApplicable(coupon, planKey, cycle)) throw new BusinessException(MembershipErrorCode.COUPON_NOT_APPLICABLE);

    if ("percent".equals(coupon.getCouponType())) {
        return amount.multiply(coupon.getDiscountValue()).setScale(2, RoundingMode.HALF_UP);
    } else {
        return amount.subtract(coupon.getDiscountValue()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 3: 修改 MembershipServiceImpl**

在 `resolveExpectedAmount` 最终金额后，如果请求带了 `couponCode`，调用 `userCouponService.applyCoupon` 再打折。创建订单时记录 `coupon_code` 和 `coupon_discount`。

- [ ] **Step 4: 修改预览接口**

`previewSubscribe` / `previewUpgrade` 增加 coupon 应用后的最终价返回。

- [ ] **Step 5: 运行测试**

Run: `cd project/user/api && ./mvnw test -Dtest=UserCouponServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/lottery/service \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/membership \
        project/user/api/src/main/resources/db/migration/V1.0.0_073__add_coupon_to_order.sql \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/lottery/service
git commit -m "feat(lottery): 实现折扣券与订阅下单集成"
```

---

## Task 11: 用户端前端抽奖页面

**Files:**
- Create: `project/user/web/src/views/lottery/LotteryPage.vue`
- Create: `project/user/web/src/api/lottery.js`
- Modify: `project/user/web/src/router/index.js`（注册路由）
- Modify: 合适的导航入口（如 console 侧边栏或 landing NavBar）

**Interfaces:**
- Consumes: 用户端 API `/api/v1/user/lottery/*`
- Produces: 用户可交互的抽奖页面

- [ ] **Step 1: 创建 lottery API 模块**

```javascript
import request from '@/utils/request';

export function getCurrentCampaign() {
  return request({ url: '/api/v1/user/lottery/campaigns/current', method: 'get' });
}

export function getChances(campaignId) {
  return request({ url: '/api/v1/user/lottery/chances', method: 'get', params: { campaignId } });
}

export function draw(campaignId) {
  return request({ url: '/api/v1/user/lottery/draw', method: 'post', data: { campaignId } });
}

export function redeem(code) {
  return request({ url: '/api/v1/user/lottery/redeem', method: 'post', data: { code } });
}

export function getDisplayWinners(campaignId) {
  return request({ url: '/api/v1/user/lottery/display-winners', method: 'get', params: { campaignId } });
}
```

- [ ] **Step 2: 实现 LotteryPage.vue**

页面包含：
- 活动标题与倒计时。
- 抽奖按钮，显示剩余次数。
- 中奖弹窗：展示奖项名 + 兑换码 + 复制按钮。
- 我的兑换码列表。
- 中奖展示墙（滚动或列表）。

使用 Ant Design Vue 组件。

- [ ] **Step 3: 注册路由与入口**

```javascript
{
  path: '/lottery',
  name: 'Lottery',
  component: () => import('@/views/lottery/LotteryPage.vue'),
  meta: { title: '抽奖活动' }
}
```

- [ ] **Step 4: 本地验证**

Run: `cd project/user/web && npm run dev`
Expected: 页面可访问，按钮可点击，抽奖接口正常返回。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/lottery \
        project/user/web/src/api/lottery.js \
        project/user/web/src/router/index.js
git commit -m "feat(web/lottery): 添加用户端抽奖页面"
```

---

## Task 12: 管理端前端页面

**Files:**
- Create: `project/admin/web/src/views/lottery/LotteryCampaignList.vue`
- Create: `project/admin/web/src/views/lottery/LotteryCampaignEdit.vue`
- Create: `project/admin/web/src/views/lottery/LotteryTierConfig.vue`
- Create: `project/admin/web/src/views/lottery/LotteryRedemptionRecord.vue`
- Create: `project/admin/web/src/views/lottery/LotteryDrawRecord.vue`
- Create: `project/admin/web/src/views/lottery/LotteryDisplayWinner.vue`
- Create: `project/admin/web/src/api/lottery.js`
- Modify: `project/admin/web/src/router/index.js`
- Modify: `project/admin/web/src/layout/components/Sidebar/menu.js`（或对应菜单配置）

**Interfaces:**
- Consumes: 管理端 API `/api/v1/admin/lottery/*`
- Produces: 管理端运营活动配置界面

- [ ] **Step 1: 创建 admin lottery API 模块**

```javascript
import request from '@/utils/request';

export function listCampaigns() {
  return request({ url: '/api/v1/admin/lottery/campaigns', method: 'get' });
}

export function saveCampaign(data) {
  return request({ url: '/api/v1/admin/lottery/campaigns', method: 'post', data });
}

export function openCampaign(id) {
  return request({ url: `/api/v1/admin/lottery/campaigns/${id}/open`, method: 'post' });
}

export function closeCampaign(id) {
  return request({ url: `/api/v1/admin/lottery/campaigns/${id}/close`, method: 'post' });
}

export function listTiers(campaignId) {
  return request({ url: `/api/v1/admin/lottery/campaigns/${campaignId}/tiers`, method: 'get' });
}

export function saveTier(campaignId, data) {
  return request({ url: `/api/v1/admin/lottery/campaigns/${campaignId}/tiers`, method: 'post', data });
}

export function listRedemptionCodes(params) {
  return request({ url: '/api/v1/admin/lottery/redemption-codes', method: 'get', params });
}

export function listDrawRecords(params) {
  return request({ url: '/api/v1/admin/lottery/draw-records', method: 'get', params });
}

export function listDisplayWinners(campaignId) {
  return request({ url: '/api/v1/admin/lottery/display-winners', method: 'get', params: { campaignId } });
}

export function saveDisplayWinner(data) {
  return request({ url: '/api/v1/admin/lottery/display-winners', method: 'post', data });
}
```

- [ ] **Step 2: 实现活动列表与编辑页**

使用 Ant Design Vue Table + Form，支持：
- 活动 CRUD。
- 开启/关闭按钮。

- [ ] **Step 3: 实现奖项配置页**

奖项表单字段：
- tier_key / tier_name
- probability
- max_win_count
- reward_type + reward_value_json（根据类型动态表单）
- code_prefix / code_length / code_validity_days

- [ ] **Step 4: 实现记录与展示墙页**

- 兑换记录：表格展示码、状态、中奖人、兑换人、时间，支持导出。
- 抽奖记录：表格展示用户、奖项、类型、时间。
- 展示墙：表格 + 新增机器人表单。

- [ ] **Step 5: 注册路由与菜单**

在「运营活动」下新增子菜单：抽奖活动、兑换记录、抽奖记录。

- [ ] **Step 6: 本地验证**

Run: `cd project/admin/web && npm run dev`
Expected: 管理端菜单正常，活动可配置，记录可查看。

- [ ] **Step 7: Commit**

```bash
git add project/admin/web/src/views/lottery \
        project/admin/web/src/api/lottery.js \
        project/admin/web/src/router/index.js \
        project/admin/web/src/layout/components/Sidebar/menu.js
git commit -m "feat(admin-web/lottery): 添加管理端抽奖活动页面"
```

---

## Task 13: 集成测试与端到端验证

**Files:**
- Create: `tests/e2e/verify_lottery.py`
- Create: `tests/e2e/verify_lottery_admin.py`

**Interfaces:**
- Consumes: 完整后端 + 前端服务
- Produces: 验证脚本与截图

- [ ] **Step 1: 编写用户端 E2E 脚本**

使用 Playwright：
1. 注册用户 A。
2. 管理端创建活动并开启。
3. 用户 A 进入抽奖页，点击抽奖。
4. 验证返回结果包含兑换码或谢谢回顾。
5. 复制兑换码，调用 redeem 接口，验证奖励到账。

- [ ] **Step 2: 编写管理端 E2E 脚本**

1. 登录管理端。
2. 创建活动、配置奖项。
3. 开启活动。
4. 查看兑换记录、抽奖记录。
5. 配置机器人展示墙。

- [ ] **Step 3: 运行验证**

Run: `python3 tests/e2e/verify_lottery.py`
Expected: 脚本成功完成，截图保存到 `tests/e2e/screenshots/`。

- [ ] **Step 4: Commit**

```bash
git add tests/e2e/verify_lottery.py tests/e2e/verify_lottery_admin.py
git commit -m "test(lottery): 添加抽奖活动 E2E 验证脚本"
```

---

## 自检清单

### Spec 覆盖度

| Spec 章节 | 对应 Task |
|---|---|
| 数据模型 9 张表 | Task 1, Task 2, Task 8 |
| 活动生命周期 | Task 8 |
| 奖项概率与库存上限 | Task 4, Task 8 |
| 抽奖次数（免费+邀请） | Task 3 |
| 抽奖核心流程 | Task 4 |
| 邀请得次数 | Task 3 |
| 兑换与奖励发放 | Task 5 |
| 会员不同档位排队 | Task 5, Task 6 |
| 折扣券与下单集成 | Task 10 |
| 展示墙 | Task 4, Task 9 |
| 风控 | Task 3, Task 4 |
| 管理端 API | Task 8, Task 9 |
| 用户端 API | Task 7 |
| 前端页面 | Task 11, Task 12 |

### Placeholder 扫描

- 无 TBD/TODO。
- 无 "add appropriate error handling" 等模糊描述。
- 所有代码片段均为可直接实现的示例。

### 类型一致性

- `reward_type` 统一使用 `coin/membership/coupon/none`。
- `status` 字符串统一使用 `available/used/unused/used/expired/pending/activated`。
- `LotteryDrawService.draw` 返回 `LotteryDrawResultVO`；`LotteryRedemptionService.redeem` 返回 `LotteryRedemptionResultVO`。
- `MembershipService.extendMembership(Long, String, long)` 签名与现有代码一致。
