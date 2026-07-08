# 用户端立即订阅（测试支付）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Pricing 页实现「立即订阅」弹框，输入测试支付码 `123456` 后调用后端开通/续期会员；若用户有有效邀请人则发放创作币奖励，并向双方推送消息通知。

**Architecture:** 后端新增 `u_order` 与 `u_user_membership` 两张表，新增 `MembershipController` + `MembershipService` 统一处理支付校验、订单生成、会员续期、邀请奖励、消息通知；前端 `Pricing.vue` 通过 Ant Design Vue 弹框收集支付码，成功后刷新 `localStorage` 会员状态并跳转创作页。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + MySQL 8；Vue 3 + Ant Design Vue + Vue Router 4；Playwright + pymysql E2E。

## Global Constraints

- 测试阶段不接入真实支付，支付码固定为 `123456`。
- 会员周期映射：月 = 30 天，季 = 90 天，年 = 365 天。
- 会员等级由 Pricing 页选择的套餐决定：基础版 / 专业版 / 旗舰版。
- 邀请关系以 `u_user_invite_relation` 表为准，`effective_status = 1` 表示有效。
- 消息通知复用现有 `u_message` + `MessageService.pushPersonal`。
- 所有 DB 迁移必须遵循 `docs/architecture/mysql-table-conventions.md`。
- 金额字段使用 `DECIMAL(19,4)`；状态/类型枚举使用 `TINYINT UNSIGNED`。
- 用户端表强制包含 `user_id` 与 `tenant_id`（默认 0）。
- 后端错误码使用 6 位分段：`1XXYYY`，订单/支付模块编码为 `16`。
- 单元测试使用 JUnit 5 + Spring Boot Test；前端构建命令为 `npm run build`。

---

## File Structure

| 文件 | 职责 |
|---|---|
| `db/migration/V1.0.0_020__add_order_and_membership.sql` | 创建 `u_order` 与 `u_user_membership` 表 |
| `modules/membership/entity/Order.java` | 订单实体 |
| `modules/membership/entity/UserMembership.java` | 用户会员实体 |
| `modules/membership/mapper/OrderMapper.java` | 订单 Mapper |
| `modules/membership/mapper/UserMembershipMapper.java` | 会员 Mapper |
| `modules/membership/enums/MembershipCycle.java` | 会员周期枚举（month/quarter/year → 30/90/365） |
| `modules/membership/enums/MembershipPlan.java` | 会员套餐枚举（basic/pro/flagship → 名称/奖励） |
| `modules/membership/enums/MembershipErrorCode.java` | 会员/支付错误码 |
| `modules/membership/dto/request/SubscribeRequest.java` | 订阅请求 DTO |
| `modules/membership/vo/SubscribeResultVO.java` | 订阅成功响应 VO |
| `modules/membership/vo/MembershipStatusVO.java` | 当前会员状态 VO |
| `modules/membership/service/MembershipService.java` | 会员服务接口 |
| `modules/membership/service/impl/MembershipServiceImpl.java` | 会员服务实现（核心业务逻辑） |
| `modules/membership/controller/MembershipController.java` | 会员相关 HTTP 接口 |
| `modules/membership/service/MembershipServiceTest.java` | Service 层单元测试 |
| `project/user/web/src/api/membership.js` | 前端会员 API 封装 |
| `project/user/web/src/views/Pricing.vue` | 订阅弹框与支付流程 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 会员状态读取与展示（已有逻辑，只需接入 API 刷新） |
| `tests/e2e/verify_subscription.py` | 端到端验证脚本 |

---

### Task 1: Flyway 迁移创建订单与会员表

**Files:**
- Create: `project/user/api/src/main/resources/db/migration/V1.0.0_020__add_order_and_membership.sql`

**Interfaces:**
- Produces: 数据库表 `u_order`、`u_user_membership`，供后续 Entity/Mapper 使用。

- [ ] **Step 1: 编写迁移脚本**

```sql
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号：SUB{yyMMdd}{6位随机}',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
    plan_key VARCHAR(32) NOT NULL COMMENT '套餐：basic/pro/flagship',
    cycle VARCHAR(16) NOT NULL COMMENT '周期：month/quarter/year',
    amount DECIMAL(19,4) NOT NULL COMMENT '订单金额',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付',
    paid_at DATETIME(3) DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_order_order_no (order_no),
    KEY idx_u_order_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户订单表';

CREATE TABLE IF NOT EXISTS u_user_membership (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    level VARCHAR(32) NOT NULL COMMENT '当前等级：basic/pro/flagship',
    started_at DATE NOT NULL COMMENT '本次会员开始日期',
    expires_at DATE NOT NULL COMMENT '会员到期日期',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_user_membership_user_id (user_id),
    KEY idx_u_user_membership_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会员状态表';
```

- [ ] **Step 2: 本地验证 Flyway 迁移**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/api
MYSQL_PASSWORD=123456 ./mvnw spring-boot:run -DskipTests
```

Expected: 日志出现 `Successfully applied 1 migration to schema 'aichuangzuo', now at version v1.0.0.020`。

- [ ] **Step 3: 确认表结构**

Run:
```bash
mysql -uroot -p123456 aichuangzuo -e "SHOW TABLES LIKE 'u_order'; SHOW TABLES LIKE 'u_user_membership';"
```

Expected: 两行输出均显示表名。

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/resources/db/migration/V1.0.0_020__add_order_and_membership.sql
git commit -m "feat(membership): add order and membership tables via Flyway"
```

---

### Task 2: 订单与会员实体及 Mapper

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/entity/Order.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/entity/UserMembership.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/mapper/OrderMapper.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/mapper/UserMembershipMapper.java`

**Interfaces:**
- Produces: `Order` 实体、`UserMembership` 实体、对应 MyBatis-Plus Mapper，供 `MembershipServiceImpl` 注入使用。

- [ ] **Step 1: 创建 Order 实体**

```java
package com.aichuangzuo.user.modules.membership.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订单实体，对应表 u_order。
 */
@Getter
@Setter
@TableName("u_order")
public class Order {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号：SUB{yyMMdd}{6位随机}。 */
    private String orderNo;

    /** 下单用户ID。 */
    private Long userId;

    /** 套餐：basic/pro/flagship。 */
    private String planKey;

    /** 周期：month/quarter/year。 */
    private String cycle;

    /** 订单金额。 */
    private BigDecimal amount;

    /** 状态：0-待支付，1-已支付。 */
    private Integer status;

    /** 支付时间。 */
    private LocalDateTime paidAt;

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

- [ ] **Step 2: 创建 UserMembership 实体**

```java
package com.aichuangzuo.user.modules.membership.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户会员状态实体，对应表 u_user_membership。
 */
@Getter
@Setter
@TableName("u_user_membership")
public class UserMembership {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 当前等级：basic/pro/flagship。 */
    private String level;

    /** 本次会员开始日期。 */
    private LocalDate startedAt;

    /** 会员到期日期。 */
    private LocalDate expiresAt;

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

- [ ] **Step 3: 创建 OrderMapper**

```java
package com.aichuangzuo.user.modules.membership.mapper;

import com.aichuangzuo.user.modules.membership.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户订单 Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
```

- [ ] **Step 4: 创建 UserMembershipMapper**

```java
package com.aichuangzuo.user.modules.membership.mapper;

import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户会员状态 Mapper。
 */
@Mapper
public interface UserMembershipMapper extends BaseMapper<UserMembership> {

    /**
     * 根据用户 ID 查询会员状态。
     *
     * @param userId 用户ID
     * @return 会员状态；未开通返回 null
     */
    @Select("SELECT * FROM u_user_membership WHERE user_id = #{userId} LIMIT 1")
    UserMembership selectByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 5: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/
git commit -m "feat(membership): add order and membership entities plus mappers"
```

---

### Task 3: 会员周期、套餐与错误码枚举

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/enums/MembershipCycle.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/enums/MembershipPlan.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/enums/MembershipErrorCode.java`

**Interfaces:**
- Produces: `MembershipCycle.of(String)`、`MembershipPlan.of(String)`、错误码枚举，供 Service 与 Controller 使用。

- [ ] **Step 1: 创建 MembershipCycle 枚举**

```java
package com.aichuangzuo.user.modules.membership.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 会员周期枚举。
 */
@Getter
public enum MembershipCycle {

    MONTH("month", 30),
    QUARTER("quarter", 90),
    YEAR("year", 365);

    private final String code;
    private final int days;

    MembershipCycle(String code, int days) {
        this.code = code;
        this.days = days;
    }

    /**
     * 根据 code 解析，未匹配返回 null。
     */
    public static MembershipCycle of(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 2: 创建 MembershipPlan 枚举**

```java
package com.aichuangzuo.user.modules.membership.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * 会员套餐枚举。
 */
@Getter
public enum MembershipPlan {

    BASIC("basic", "基础版", new BigDecimal("3")),
    PRO("pro", "专业版", new BigDecimal("5")),
    FLAGSHIP("flagship", "旗舰版", new BigDecimal("10"));

    private final String key;
    private final String displayName;
    private final BigDecimal inviterReward;

    MembershipPlan(String key, String displayName, BigDecimal inviterReward) {
        this.key = key;
        this.displayName = displayName;
        this.inviterReward = inviterReward;
    }

    /**
     * 根据 key 解析，未匹配返回 null。
     */
    public static MembershipPlan of(String key) {
        if (key == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(p -> p.key.equals(key))
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 3: 创建 MembershipErrorCode 枚举**

```java
package com.aichuangzuo.user.modules.membership.enums;

import com.aichuangzuo.shared.result.ErrorCode;

/**
 * 会员/支付模块错误码，模块编码 16。
 */
public enum MembershipErrorCode implements ErrorCode {

    INVALID_PAY_CODE(116001, "支付码错误"),
    INVALID_PLAN_KEY(116002, "套餐不存在"),
    INVALID_CYCLE(116003, "订阅周期不存在");

    private final int code;
    private final String message;

    MembershipErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/enums/
git commit -m "feat(membership): add cycle, plan and error code enums"
```

---

### Task 4: MembershipService 接口与核心业务实现

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/service/MembershipService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/service/impl/MembershipServiceImpl.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/dto/request/SubscribeRequest.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/vo/SubscribeResultVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/vo/MembershipStatusVO.java`

**Interfaces:**
- Consumes: `OrderMapper`、`UserMembershipMapper`、`UserInviteRelationMapper`、`UserMapper`、`UserCoinRecordMapper`、`MessageService`、`CoinRecordService`。
- Produces:
  - `MembershipService.subscribe(Long userId, SubscribeRequest request) → SubscribeResultVO`
  - `MembershipService.getMyMembership(Long userId) → MembershipStatusVO`

- [ ] **Step 1: 创建 SubscribeRequest DTO**

```java
package com.aichuangzuo.user.modules.membership.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 立即订阅请求。
 */
@Data
public class SubscribeRequest {

    /** 套餐：basic / pro / flagship。 */
    @NotBlank(message = "套餐不能为空")
    private String planKey;

    /** 周期：month / quarter / year。 */
    @NotBlank(message = "订阅周期不能为空")
    private String cycle;

    /** 测试支付码。 */
    @NotBlank(message = "支付码不能为空")
    private String payCode;

    /** 订单金额（取自 Pricing 页当前价格）。 */
    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须大于 0")
    private BigDecimal amount;
}
```

- [ ] **Step 2: 创建 SubscribeResultVO**

```java
package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订阅成功响应。
 */
@Data
public class SubscribeResultVO {

    /** 订单编号。 */
    private String orderNo;

    /** 开通等级。 */
    private String level;

    /** 增加天数。 */
    private int days;

    /** 到期日期 yyyy-MM-dd。 */
    private String expiresAt;

    /** 是否已给邀请人发放奖励。 */
    private boolean inviterRewarded;

    /** 奖励金额（创作币）。 */
    private BigDecimal rewardAmount;
}
```

- [ ] **Step 3: 创建 MembershipStatusVO**

```java
package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

/**
 * 当前会员状态响应。
 */
@Data
public class MembershipStatusVO {

    /** 是否拥有有效会员。 */
    private boolean hasMembership;

    /** 等级 key：basic / pro / flagship。 */
    private String level;

    /** 等级显示名。 */
    private String levelName;

    /** 到期日期 yyyy-MM-dd。 */
    private String expiresAt;
}
```

- [ ] **Step 4: 创建 MembershipService 接口**

```java
package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;

/**
 * 会员服务。
 */
public interface MembershipService {

    /**
     * 立即订阅（测试支付）。
     *
     * @param userId  当前用户ID
     * @param request 订阅请求
     * @return 订阅结果
     */
    SubscribeResultVO subscribe(Long userId, SubscribeRequest request);

    /**
     * 查询当前用户会员状态。
     *
     * @param userId 用户ID
     * @return 会员状态
     */
    MembershipStatusVO getMyMembership(Long userId);
}
```

- [ ] **Step 5: 创建 MembershipServiceImpl**

```java
package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.enums.MembershipErrorCode;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 会员服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final String TEST_PAY_CODE = "123456";
    private static final String ORDER_NO_PREFIX = "SUB";
    private static final String COIN_BIZ_TYPE_INVITE_REWARD = "invite_reward";
    private static final int EFFECTIVE_STATUS = 1;

    private final OrderMapper orderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final CoinRecordService coinRecordService;

    @Override
    @Transactional
    public SubscribeResultVO subscribe(Long userId, SubscribeRequest request) {
        validatePayCode(request.getPayCode());

        MembershipPlan plan = MembershipPlan.of(request.getPlanKey());
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }

        MembershipCycle cycle = MembershipCycle.of(request.getCycle());
        if (cycle == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }

        Order order = createPaidOrder(userId, plan, cycle, request.getAmount());
        UserMembership membership = activateOrExtendMembership(userId, plan, cycle);

        sendSubscriptionNotification(userId, plan, membership);
        boolean rewarded = rewardInviter(userId, plan, order);

        log.info("会员订阅成功 userId={}, orderNo={}, level={}, days={}",
                userId, order.getOrderNo(), plan.getKey(), cycle.getDays());

        SubscribeResultVO vo = new SubscribeResultVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setLevel(plan.getKey());
        vo.setDays(cycle.getDays());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        vo.setInviterRewarded(rewarded);
        vo.setRewardAmount(rewarded ? plan.getInviterReward() : BigDecimal.ZERO);
        return vo;
    }

    @Override
    public MembershipStatusVO getMyMembership(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        MembershipStatusVO vo = new MembershipStatusVO();
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            vo.setHasMembership(false);
            return vo;
        }

        MembershipPlan plan = MembershipPlan.of(membership.getLevel());
        vo.setHasMembership(true);
        vo.setLevel(membership.getLevel());
        vo.setLevelName(plan == null ? membership.getLevel() : plan.getDisplayName());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return vo;
    }

    private void validatePayCode(String payCode) {
        if (!TEST_PAY_CODE.equals(payCode)) {
            throw new BusinessException(MembershipErrorCode.INVALID_PAY_CODE);
        }
    }

    private Order createPaidOrder(Long userId, MembershipPlan plan, MembershipCycle cycle, BigDecimal amount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlanKey(plan.getKey());
        order.setCycle(cycle.getCode());
        order.setAmount(amount);
        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        order.setTenantId(0L);
        orderMapper.insert(order);
        return order;
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return ORDER_NO_PREFIX + date + random;
    }

    private UserMembership activateOrExtendMembership(Long userId, MembershipPlan plan, MembershipCycle cycle) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate baseDate = today;
        if (membership != null && membership.getExpiresAt().isAfter(today.minusDays(1))) {
            baseDate = membership.getExpiresAt();
        }
        LocalDate newExpiresAt = baseDate.plusDays(cycle.getDays());

        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setLevel(plan.getKey());
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membership.setTenantId(0L);
            userMembershipMapper.insert(membership);
        } else {
            membership.setLevel(plan.getKey());
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            userMembershipMapper.updateById(membership);
        }
        return membership;
    }

    private void sendSubscriptionNotification(Long userId, MembershipPlan plan, UserMembership membership) {
        String levelName = plan.getDisplayName();
        String expiresAt = membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String summary = String.format("您已成功开通 %s，有效期至 %s", levelName, expiresAt);
        String content = String.format(
                "亲爱的用户：\n\n您的 %s 会员已成功开通，有效期至 %s。\n\n感谢您对爱创作的支持！",
                levelName, expiresAt);

        messageService.pushPersonal(
                userId,
                "membership",
                "订阅成功",
                summary,
                null,
                content,
                MessageSubType.SUBSCRIBED.getCode());
    }

    private boolean rewardInviter(Long userId, MembershipPlan plan, Order order) {
        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(userId);
        if (relation == null || !Integer.valueOf(EFFECTIVE_STATUS).equals(relation.getEffectiveStatus())) {
            return false;
        }

        Long inviterId = relation.getInviterId();
        if (alreadyRewarded(order.getId())) {
            log.warn("邀请奖励已发放，跳过 userId={}, orderId={}", userId, order.getId());
            return false;
        }

        User invitee = userMapper.selectById(userId);
        String inviteeName = invitee == null ? "好友" : (invitee.getNickname() == null ? "好友" : invitee.getNickname());
        String remark = String.format("%s 订阅 %s，邀请奖励", inviteeName, plan.getDisplayName());

        coinRecordService.grant(inviterId, COIN_BIZ_TYPE_INVITE_REWARD, plan.getInviterReward(), order.getId().toString(), remark);

        String summary = String.format("好友 %s 订阅 %s，您获得 %s 创作币", inviteeName, plan.getDisplayName(), plan.getInviterReward().toPlainString());
        String content = String.format(
                "恭喜您！\n\n您邀请的好友 %s 成功订阅 %s，系统已向您发放 %s 创作币奖励。\n\n感谢您的分享！",
                inviteeName, plan.getDisplayName(), plan.getInviterReward().toPlainString());

        messageService.pushPersonal(inviterId, "reward", "邀请奖励到账", summary, null, content, null);
        return true;
    }

    private boolean alreadyRewarded(Long orderId) {
        Long count = userCoinRecordMapper.selectCount(
                new LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getRefId, orderId.toString())
                        .eq(UserCoinRecord::getBizType, COIN_BIZ_TYPE_INVITE_REWARD)
        );
        return count != null && count > 0;
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/service/
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/dto/
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/vo/
git commit -m "feat(membership): add membership service with subscribe and query"
```

---

### Task 5: MembershipController

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/controller/MembershipController.java`

**Interfaces:**
- Consumes: `MembershipService`。
- Produces: `POST /api/v1/user/membership/subscribe` 与 `GET /api/v1/user/membership/me`。

- [ ] **Step 1: 创建 MembershipController**

```java
package com.aichuangzuo.user.modules.membership.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端会员订阅接口。
 */
@RestController
@RequestMapping("/api/v1/user/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    /**
     * 立即订阅（测试支付）。
     */
    @PostMapping("/subscribe")
    public Result<SubscribeResultVO> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(membershipService.subscribe(userId, request));
    }

    /**
     * 查询当前用户会员状态。
     */
    @GetMapping("/me")
    public Result<MembershipStatusVO> getMyMembership() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(membershipService.getMyMembership(userId));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/membership/controller/MembershipController.java
git commit -m "feat(membership): add membership controller"
```

---

### Task 6: MembershipService 单元测试

**Files:**
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/membership/service/MembershipServiceTest.java`

**Interfaces:**
- Consumes: `MembershipService`、`OrderMapper`、`UserMembershipMapper`、`UserInviteRelationMapper`、`UserMapper`、`UserCoinRecordMapper`、`MessageService`、`CoinRecordService`。

- [ ] **Step 1: 编写测试类**

```java
package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class MembershipServiceTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void subscribe_invalidPayCode_throwsBusinessException() {
        User user = createUser("sub-pay-code@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "000000", new BigDecimal("503.2"));

        try {
            membershipService.subscribe(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("支付码错误"));
            return;
        }
        throw new AssertionError("应抛出支付码错误异常");
    }

    @Test
    void subscribe_newMembership_createsMembershipAndOrder() {
        User user = createUser("sub-new@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));

        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        assertNotNull(result.getOrderNo());
        assertTrue(result.getOrderNo().startsWith("SUB"));
        assertEquals("pro", result.getLevel());
        assertEquals(365, result.getDays());
        assertNotNull(result.getExpiresAt());
        assertFalse(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(BigDecimal.ZERO));

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertNotNull(membership);
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(365), membership.getExpiresAt());
    }

    @Test
    void subscribe_extendMembership_extendsFromExistingExpiry() {
        User user = createUser("sub-extend@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        SubscribeRequest request = buildRequest("pro", "quarter", "123456", new BigDecimal("161.7"));
        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(10).plusDays(90), membership.getExpiresAt());
        assertEquals(90, result.getDays());
    }

    @Test
    void subscribe_withInviter_grantsRewardAndSendsNotification() {
        User inviter = createUser("sub-inviter@test.com");
        User invitee = createUser("sub-invitee@test.com");
        createInviteRelation(inviter, invitee);

        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));
        SubscribeResultVO result = membershipService.subscribe(invitee.getId(), request);

        assertTrue(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(new BigDecimal("5")));

        Long rewardCount = userCoinRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord>()
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getUserId, inviter.getId())
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getBizType, "invite_reward")
        );
        assertEquals(1L, rewardCount);
    }

    @Test
    void getMyMembership_expired_returnsFalse() {
        User user = createUser("sub-expired@test.com");
        UserMembership membership = new UserMembership();
        membership.setUserId(user.getId());
        membership.setLevel("pro");
        membership.setStartedAt(LocalDate.now().minusDays(400));
        membership.setExpiresAt(LocalDate.now().minusDays(10));
        membership.setTenantId(0L);
        userMembershipMapper.insert(membership);

        MembershipStatusVO vo = membershipService.getMyMembership(user.getId());

        assertFalse(vo.isHasMembership());
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        user.setCoinBalance(BigDecimal.ZERO);
        userMapper.insert(user);
        return user;
    }

    private void createInviteRelation(User inviter, User invitee) {
        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviter.getInviteCode());
        relation.setSourceType(2);
        relation.setEffectiveStatus(1);
        userInviteRelationMapper.insert(relation);
    }

    private SubscribeRequest buildRequest(String planKey, String cycle, String payCode, BigDecimal amount) {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanKey(planKey);
        request.setCycle(cycle);
        request.setPayCode(payCode);
        request.setAmount(amount);
        return request;
    }
}
```

- [ ] **Step 2: 运行测试**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl user/api test -Dtest=MembershipServiceTest -DfailIfNoTests=false
```

Expected: 5 个用例全部 PASS。

- [ ] **Step 3: Commit**

```bash
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/membership/service/MembershipServiceTest.java
git commit -m "test(membership): add MembershipService tests"
```

---

### Task 7: 前端会员 API 封装

**Files:**
- Create: `project/user/web/src/api/membership.js`

**Interfaces:**
- Produces: `subscribe(data)`、`getMyMembership()`，供 `Pricing.vue` 与 `ConsoleLayout.vue` 调用。

- [ ] **Step 1: 创建 membership.js**

```js
import request from '@/utils/request'

export function subscribe(data) {
  return request.post('/membership/subscribe', data)
}

export function getMyMembership() {
  return request.get('/membership/me')
}
```

- [ ] **Step 2: Commit**

```bash
git add project/user/web/src/api/membership.js
git commit -m "feat(membership-web): add membership API client"
```

---

### Task 8: Pricing.vue 订阅弹框

**Files:**
- Modify: `project/user/web/src/views/Pricing.vue`

**Interfaces:**
- Consumes: `subscribe` from `@/api/membership`。
- Produces: 点击「立即订阅」弹出支付码输入框，输入 `123456` 后调用 API，成功后写入 `localStorage` 并跳转 `/console/create`。

- [ ] **Step 1: 修改 script setup 导入与状态**

在 `<script setup>` 顶部新增：

```js
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { subscribe } from '@/api/membership'
import NavBar from '@/components/layout/NavBar.vue'
```

将 `import { ref } from 'vue'` 替换为上述合并后的导入。并在 `const ctaLabel = '开始创作'` 之后新增：

```js
const router = useRouter()

const modalVisible = ref(false)
const selectedPlan = ref(null)
const payCode = ref('')
const subscribeLoading = ref(false)

const planKeyToName = {
  basic: '基础版',
  pro: '专业版',
  flagship: '旗舰版'
}

const cycleLabel = {
  month: '月度',
  quarter: '季度',
  year: '年度'
}

const handleSubscribe = (plan) => {
  selectedPlan.value = plan
  payCode.value = ''
  modalVisible.value = true
}

const handlePay = async () => {
  if (!payCode.value || payCode.value.length !== 6) {
    message.warning('请输入 6 位支付码')
    return
  }

  const plan = selectedPlan.value
  const cycle = activeCycle.value
  const price = plan[cycle === 'month' ? 'monthly' : cycle]

  subscribeLoading.value = true
  try {
    const res = await subscribe({
      planKey: plan.key,
      cycle,
      payCode: payCode.value,
      amount: price.current
    })
    const data = res.data
    message.success('订阅成功')
    localStorage.setItem('aichuangzuo_membership', JSON.stringify({
      level: planKeyToName[data.level] || plan.name,
      expiresAt: data.expiresAt
    }))
    modalVisible.value = false
    router.push('/console/create')
  } catch (err) {
    message.error(err.message || '订阅失败，请重试')
  } finally {
    subscribeLoading.value = false
  }
}
```

- [ ] **Step 2: 为「立即订阅」按钮绑定点击事件**

将第 48-50 行：

```vue
<button class="plan-btn" :class="{ primary: plan.recommended }">
  立即订阅
</button>
```

替换为：

```vue
<button
  class="plan-btn"
  :class="{ primary: plan.recommended }"
  @click="handleSubscribe(plan)"
>
  立即订阅
</button>
```

- [ ] **Step 3: 在模板末尾新增订阅弹框**

在 `</template>` 之前新增：

```vue
<a-modal
  v-model:open="modalVisible"
  :title="`确认订阅 ${selectedPlan ? selectedPlan.name : ''}`"
  :width="420"
  centered
  class="subscribe-modal"
  @ok="handlePay"
  :confirm-loading="subscribeLoading"
>
  <div class="subscribe-pay-panel">
    <p class="subscribe-pay-tip">
      测试阶段，请输入支付码 <strong>123456</strong> 完成订阅。
    </p>
    <a-input
      v-model:value="payCode"
      placeholder="请输入 6 位支付码"
      maxlength="6"
      size="large"
      @pressEnter="handlePay"
    />
  </div>
</a-modal>
```

- [ ] **Step 4: 在 style 块中新增弹框样式**

在 `</style>` 之前新增：

```css
.subscribe-pay-panel {
  padding: 8px 0 16px;
}

.subscribe-pay-tip {
  color: #595959;
  font-size: 14px;
  margin-bottom: 16px;
}

.subscribe-pay-tip strong {
  color: #FF2442;
}

body[data-theme="dark"] .subscribe-pay-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .subscribe-pay-tip strong {
  color: #ff4d6f;
}
```

- [ ] **Step 5: 运行前端构建**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run build
```

Expected: `build completed` 无 ERROR。

- [ ] **Step 6: Commit**

```bash
git add project/user/web/src/views/Pricing.vue
git commit -m "feat(membership-web): add subscribe modal and payment flow on Pricing"
```

---

### Task 9: ConsoleLayout.vue 会员状态 API 刷新

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`

**Interfaces:**
- Consumes: `getMyMembership` from `@/api/membership`。
- Produces: 页面加载时优先读取 `localStorage`，同时异步调用 `/api/v1/user/membership/me` 刷新真实会员状态并更新本地缓存。

- [ ] **Step 1: 导入 API**

在 `<script setup>` 顶部找到 `import { getMessages, markMessageRead, markAllMessagesRead } from '@/api/message'`，在该行同一导入块中新增：

```js
import { getMyMembership } from '@/api/membership'
```

- [ ] **Step 2: 新增刷新会员状态函数**

在 `extendMembership` 函数之后（约第 1603 行）新增：

```js
const refreshMembershipFromApi = async () => {
  try {
    const res = await getMyMembership()
    const data = res.data
    if (data && data.hasMembership) {
      membershipLevel.value = data.levelName || data.level
      membershipExpiry.value = data.expiresAt
      hasMembership.value = true
      localStorage.setItem(MEMBERSHIP_KEY, JSON.stringify({
        level: membershipLevel.value,
        expiresAt: membershipExpiry.value
      }))
    }
  } catch (err) {
    // 静默失败，继续使用本地缓存
  }
}
```

- [ ] **Step 3: 在 onMounted 调用刷新**

在 `onMounted` 钩子中 `loadMembership()` 调用之后新增 `refreshMembershipFromApi()`。若现有代码类似：

```js
onMounted(() => {
  loadMembership()
  // ...
})
```

改为：

```js
onMounted(() => {
  loadMembership()
  refreshMembershipFromApi()
  // ...
})
```

- [ ] **Step 4: 运行前端构建**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run build
```

Expected: 构建通过。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(membership-web): refresh membership status from API in console layout"
```

---

### Task 10: E2E 验证脚本

**Files:**
- Create: `tests/e2e/verify_subscription.py`

**Interfaces:**
- Consumes: 用户端前端 `http://localhost:22345`、用户端 API `http://localhost:25050`、MySQL `aichuangzuo` 数据库。
- Produces: 截图 `tests/e2e/screenshots/subscription_success.png`、`subscription_inviter_reward.png`；断言结果输出。

- [ ] **Step 1: 编写 E2E 脚本**

```python
#!/usr/bin/env python3
"""验证用户端立即订阅（测试支付）流程。

准备数据（DB 直连）:
  - 注册测试用户 A（有邀请人 B）
  - 注册测试用户 C（无邀请人）

验证流程:
  - 用户 C: Pricing 页选择专业版/年度 → 弹框输入 123456 → 订阅成功 → 跳转 /console/create
  - 用户 A: 同上，订阅成功后邀请人 B 获得创作币奖励并收到消息通知

用法:
  python3 tests/e2e/verify_subscription.py
"""
import sys
from datetime import datetime, timedelta
from pathlib import Path

import pymysql
import requests
from playwright.sync_api import sync_playwright

BASE_URL = 'http://localhost:22345'
API_URL = 'http://localhost:25050'
SCREENSHOT_DIR = Path(__file__).resolve().parent / 'screenshots'

DB_HOST = 'localhost'
DB_PORT = 3306
DB_USER = 'root'
DB_PASSWORD = '123456'
DB_NAME = 'aichuangzuo'


def db_conn():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )


def setup_test_users():
    """准备测试用户：inviter、invitee、standalone。"""
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    inviter_email = f'sub_inviter_{timestamp}@example.com'
    invitee_email = f'sub_invitee_{timestamp}@example.com'
    standalone_email = f'sub_standalone_{timestamp}@example.com'
    password = 'Test1234!'

    for email in (inviter_email, invitee_email, standalone_email):
        requests.post(
            f'{API_URL}/api/v1/user/auth/register',
            json={'email': email, 'password': password, 'confirmPassword': password, 'inviteCode': ''}
        )

    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute('SELECT id, invite_code FROM u_user WHERE email IN (%s, %s, %s)', (inviter_email, invitee_email, standalone_email))
            rows = c.fetchall()
            user_map = {r['email']: (r['id'], r['invite_code']) for r in rows}

            inviter_id = user_map[inviter_email][0]
            invitee_id = user_map[invitee_email][0]
            c.execute(
                """INSERT INTO u_user_invite_relation
                   (inviter_id, invitee_id, invite_code, source_type, effective_status)
                   VALUES (%s, %s, %s, 2, 1)""",
                (inviter_id, invitee_id, user_map[inviter_email][1])
            )
        conn.commit()

    return {
        'inviter': {'email': inviter_email, 'password': password, 'id': inviter_id},
        'invitee': {'email': invitee_email, 'password': password, 'id': invitee_id},
        'standalone': {'email': standalone_email, 'password': password, 'id': user_map[standalone_email][0]}
    }


def get_token(email, password):
    resp = requests.post(
        f'{API_URL}/api/v1/user/auth/login',
        json={'email': email, 'password': password}
    )
    resp.raise_for_status()
    return resp.json()['data']['accessToken']


def run_browser_subscription(email, password, label, screenshot_name):
    token = get_token(email, password)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, slow_mo=80)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})
        page.goto(f'{BASE_URL}/login')
        page.wait_for_selector('.login-card', timeout=10000)
        page.evaluate(
            f"""
            window.localStorage.setItem('aichuangzuo_access_token', '{token}');
            window.localStorage.setItem('aichuangzuo_refresh_token', '{token}');
            """
        )

        page.goto(f'{BASE_URL}/pricing')
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)

        # 切换到年度
        page.locator('.toggle-btn', has_text='年度').click()
        page.wait_for_timeout(500)

        # 点击专业版「立即订阅」
        pro_card = page.locator('.pricing-card', has_text='专业版').first
        pro_card.locator('button:has-text("立即订阅")').click()
        page.wait_for_selector('.subscribe-modal', timeout=5000)
        page.wait_for_timeout(500)

        page.locator('.subscribe-modal input').fill('123456')
        page.locator('.subscribe-modal .ant-btn-primary').click()

        page.wait_for_url('**/console/create', timeout=10000)
        page.wait_for_timeout(1000)

        page.screenshot(path=str(SCREENSHOT_DIR / screenshot_name), full_page=True)
        print(f'[SCREENSHOT] {screenshot_name} saved')

        result = page.url.endswith('/console/create')
        browser.close()
        return result


def assert_membership(user_id, expected_level, min_days):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                'SELECT level, expires_at FROM u_user_membership WHERE user_id = %s',
                (user_id,)
            )
            row = c.fetchone()
            assert row is not None, f'user_id={user_id} 无会员记录'
            assert row['level'] == expected_level, f'期望等级 {expected_level}，实际 {row["level"]}'
            expiry = row['expires_at']
            assert expiry >= datetime.now().date() + timedelta(days=min_days - 1), f'到期日 {expiry} 不足 {min_days} 天'


def assert_inviter_reward(inviter_id, invitee_id):
    with db_conn() as conn:
        with conn.cursor() as c:
            c.execute(
                """SELECT COUNT(*) AS cnt FROM u_user_coin_record
                   WHERE user_id = %s AND biz_type = 'invite_reward' AND ref_id IN
                   (SELECT id FROM u_order WHERE user_id = %s)""",
                (inviter_id, invitee_id)
            )
            row = c.fetchone()
            assert row['cnt'] == 1, f'邀请人奖励记录应为 1，实际 {row["cnt"]}'

            c.execute(
                """SELECT COUNT(*) AS cnt FROM u_message
                   WHERE msg_type = 'reward' AND target_user_id = %s""",
                (inviter_id,)
            )
            row = c.fetchone()
            assert row['cnt'] >= 1, '邀请人未收到奖励到账通知'


def main():
    users = setup_test_users()
    results = []

    results.append((
        '无邀请人用户订阅成功并跳转创作页',
        run_browser_subscription(
            users['standalone']['email'],
            users['standalone']['password'],
            'standalone',
            'subscription_success.png'
        )
    ))
    assert_membership(users['standalone']['id'], 'pro', 365)

    results.append((
        '有邀请人用户订阅成功并给邀请人发放奖励',
        run_browser_subscription(
            users['invitee']['email'],
            users['invitee']['password'],
            'invitee',
            'subscription_inviter_reward.png'
        )
    ))
    assert_membership(users['invitee']['id'], 'pro', 365)
    assert_inviter_reward(users['inviter']['id'], users['invitee']['id'])

    print('\n=== 立即订阅 E2E 验证 ===')
    all_ok = True
    for name, ok in results:
        status = 'PASS' if ok else 'FAIL'
        print(f'{status}  {name}')
        if not ok:
            all_ok = False
    print()
    return 0 if all_ok else 1


if __name__ == '__main__':
    sys.exit(main())
```

- [ ] **Step 2: 运行 E2E 验证**

前置条件：用户端 API 与前端 dev server 已启动（`npm run dev` 或生产构建产物已部署）。

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_subscription.py
```

Expected: 2 个断言全部 PASS，生成两张截图。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_subscription.py
git commit -m "test(membership): add subscription E2E verification"
```

---

### Task 11: 全链路验证与清理

**Files:**
- N/A（运行验证命令）

- [ ] **Step 1: 运行后端 MembershipServiceTest**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
MYSQL_PASSWORD=123456 mvn -pl user/api test -Dtest=MembershipServiceTest -DfailIfNoTests=false
```

Expected: 5 tests PASS。

- [ ] **Step 2: 运行前端构建**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web
npm run build
```

Expected: 构建成功。

- [ ] **Step 3: 运行 E2E**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_subscription.py
```

Expected: 全部 PASS。

- [ ] **Step 4: 检查未引用代码**

Run:
```bash
cd /Users/panyong/aio_project/ai_chuangzuo
grep -R "MembershipService\|subscribe\|/membership/subscribe\|getMyMembership" --include="*.java" --include="*.vue" --include="*.js" project/user | grep -v "target/"
```

Expected: 所有新增类和方法均有调用方，无孤立代码。

- [ ] **Step 5: Commit any final fixes**

若验证过程中有修复，逐条 commit；无修复则跳过。

---

## Self-Review

### 1. Spec coverage

| 需求点 | 对应任务 |
|---|---|
| `u_order` 表 | Task 1 |
| `u_user_membership` 表 | Task 1 |
| 支付码 `123456` | Task 4 |
| 周期 30/90/365 天 | Task 3 + Task 4 |
| 等级 basic/pro/flagship | Task 3 + Task 4 |
| 邀请人奖励 basic=3/pro=5/flagship=10 | Task 3 + Task 4 |
| 订阅成功通知 | Task 4 |
| 邀请奖励通知 | Task 4 |
| 奖励幂等（ref_id 去重） | Task 4 |
| `POST /api/v1/user/membership/subscribe` | Task 5 |
| `GET /api/v1/user/membership/me` | Task 5 |
| Pricing.vue 弹框输入支付码 | Task 8 |
| 成功后跳转 `/console/create` | Task 8 |
| ConsoleLayout 读取会员状态 | Task 9 |
| 后端单元测试 | Task 6 |
| E2E 验证 | Task 10 |

### 2. Placeholder scan

- 无 TBD/TODO。
- 所有代码块包含完整实现。
- 测试类包含具体断言。

### 3. Type consistency

- `SubscribeRequest` 字段与 `MembershipService.subscribe` 签名一致。
- `SubscribeResultVO` 与 API 契约字段一致。
- `MembershipStatusVO` 与 API 契约字段一致。
- `MembershipPlan` / `MembershipCycle` 枚举 key 与 DTO/Entity 字段一致。
- `Order.status` 使用 `Integer` 与 Entity 一致。
- `ref_id` 使用 `order.getId().toString()` 与 `UserCoinRecord.refId` 类型一致。

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-08-subscription-test-payment.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
