# 管理端订单管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理端新增订单管理模块，支持订单列表查看、标记已支付、退款、取消、手动调整/发放会员，以及营收统计卡片和图表。

**Architecture:** 管理端 API 新增 `order` 模块，读写 `u_order` 和 `u_user_membership` 表（与用户端共享数据库）。管理端前端新增两个页面：订单列表和数据统计，侧边栏菜单插入「系统设置」上方。

**Tech Stack:** Spring Boot 3 + MyBatis + MyBatis-Plus + Flyway + MySQL 8; Vue 3 + Ant Design Vue + ECharts + vue-echarts。

## Global Constraints

- 订单表 `u_order` 已有：id, order_no, user_id, plan_key, cycle, amount, status(0/1), paid_at，本次新增 refunded_at, refund_reason, admin_remark, operator_id。
- 会员表 `u_user_membership` 已有：id, user_id(UK), level, started_at, expires_at，本次不变。
- `u_user` 表已有 `membership_expire_at`(DATETIME) 和 `membership_plan`(VARCHAR(32))，操作后需同步更新。
- 续期规则：无会员或已过期→以今天为起点加天数；未过期→以 expires_at 为起点加天数，等级覆盖。
- 退款回退：从 expires_at 减去对应周期天数。
- 所有 Controller 返回 `Result<T>`，包 `com.aichuangzuo.shared.result`。
- 管理端身份通过 `SecurityAdminContext.getCurrentAdminUserId()` 获取。
- 错误码使用 117xxx 号段。
- Mapper 使用 `@Mapper` + XML，不用 MyBatis-Plus BaseMapper。
- 前端 API 层：每个函数 `.then((res) => res.data)` 解包。
- 前端弹框固定高度，不随内容跳动。

---

### Task 1: Flyway 迁移 — 订单管理字段

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_036__add_order_admin_fields.sql`

**Interfaces:**
- Produces: `u_order` 表新增 refunded_at, refund_reason, admin_remark, operator_id 列 + 3 个索引

- [ ] **Step 1: 创建迁移文件**

```sql
SET NAMES utf8mb4;

-- 扩展订单表：退款、管理员操作字段
ALTER TABLE u_order
    ADD COLUMN refunded_at DATETIME(3) DEFAULT NULL COMMENT '退款时间' AFTER paid_at,
    ADD COLUMN refund_reason VARCHAR(256) DEFAULT NULL COMMENT '退款原因' AFTER refunded_at,
    ADD COLUMN admin_remark VARCHAR(256) DEFAULT NULL COMMENT '管理员操作备注' AFTER refund_reason,
    ADD COLUMN operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作管理员ID' AFTER admin_remark;

-- 修改 status 字段注释（新增枚举值）
ALTER TABLE u_order
    MODIFY COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已退款，3-已取消';

-- 新增索引
ALTER TABLE u_order ADD INDEX idx_u_order_status (status);
ALTER TABLE u_order ADD INDEX idx_u_order_paid_at (paid_at);
ALTER TABLE u_order ADD INDEX idx_u_order_created_at (created_at);
```

- [ ] **Step 2: 验证迁移可执行**

```bash
cd project/admin/api && ./mvnw flyway:info -q 2>&1 | tail -5
```
Expected: 显示 V2.0.0_036 为 Pending 状态（或已成功执行）

- [ ] **Step 3: Commit**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_036__add_order_admin_fields.sql
git commit -m "feat(admin-api): u_order 新增退款和管理员操作字段"
```

---

### Task 2: 后端数据层 — entity + enums + mapper + DTO + VO

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/entity/AdminOrderView.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/entity/AdminMembership.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/enums/OrderStatus.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/enums/AdminOrderErrorCode.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/mapper/AdminOrderMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/mapper/AdminMembershipMapper.java`
- Create: `project/admin/api/src/main/resources/mapper/order/AdminOrderMapper.xml`
- Create: `project/admin/api/src/main/resources/mapper/order/AdminMembershipMapper.xml`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/dto/request/OrderRefundRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/dto/request/MembershipAdjustRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/dto/request/MembershipGrantRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/vo/OrderListVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/vo/OrderDetailVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/vo/OrderPageVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/vo/OrderStatsOverviewVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/vo/OrderTrendVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/vo/PlanDistributionVO.java`

**Interfaces:**
- Produces:
  - `AdminOrderMapper.selectPage(keyword, planKey, status, startDate, endDate, offset, size)` → `List<AdminOrderView>`
  - `AdminOrderMapper.countPage(keyword, planKey, status, startDate, endDate)` → `long`
  - `AdminOrderMapper.selectDetailById(id)` → `AdminOrderView`
  - `AdminOrderMapper.markPaid(id, operatorId, now)` → `int`
  - `AdminOrderMapper.refund(id, reason, operatorId, now)` → `int`
  - `AdminOrderMapper.cancel(id, operatorId)` → `int`
  - `AdminOrderMapper.statsOverview()` → `OrderStatsOverviewVO`
  - `AdminOrderMapper.statsTrend(days)` → `List<Map<String,Object>>`
  - `AdminOrderMapper.statsPlanDistribution()` → `List<Map<String,Object>>`
  - `AdminOrderMapper.statsCycleDistribution()` → `List<Map<String,Object>>`
  - `AdminMembershipMapper.selectByUserId(userId)` → `AdminMembership`
  - `AdminMembershipMapper.insertMembership(membership)` → `int`
  - `AdminMembershipMapper.updateMembership(membership)` → `int`
  - `AdminMembershipMapper.updateUserMembershipFields(userId, expireAt, plan)` → `int`

- [ ] **Step 1: 创建 AdminOrderView entity**

```java
package com.aichuangzuo.admin.modules.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表/详情视图，JOIN u_user 结果。
 */
@Data
public class AdminOrderView {
    private Long id;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String email;
    private String planKey;
    private String cycle;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private String adminRemark;
    private Long operatorId;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 AdminMembership entity**

```java
package com.aichuangzuo.admin.modules.order.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户会员状态实体，对应 u_user_membership。
 */
@Data
public class AdminMembership {
    private Long id;
    private Long userId;
    private String level;
    private LocalDate startedAt;
    private LocalDate expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 OrderStatus enum**

```java
package com.aichuangzuo.admin.modules.order.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OrderStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String displayName;

    OrderStatus(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static OrderStatus of(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 4: 创建 AdminOrderErrorCode enum**

```java
package com.aichuangzuo.admin.modules.order.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminOrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(117001, "订单不存在"),
    ORDER_STATUS_NOT_ALLOWED(117002, "订单状态不允许此操作"),
    USER_NOT_FOUND(117003, "用户不存在"),
    REFUND_REASON_REQUIRED(117004, "退款原因不能为空");

    private final int code;
    private final String message;

    AdminOrderErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 5: 创建 AdminOrderMapper interface**

```java
package com.aichuangzuo.admin.modules.order.mapper;

import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminOrderMapper {

    List<AdminOrderView> selectPage(@Param("keyword") String keyword,
                                    @Param("planKey") String planKey,
                                    @Param("status") Integer status,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("offset") long offset,
                                    @Param("size") long size);

    long countPage(@Param("keyword") String keyword,
                   @Param("planKey") String planKey,
                   @Param("status") Integer status,
                   @Param("startDate") String startDate,
                   @Param("endDate") String endDate);

    AdminOrderView selectDetailById(@Param("id") Long id);

    int markPaid(@Param("id") Long id,
                 @Param("operatorId") Long operatorId,
                 @Param("now") LocalDateTime now);

    int refund(@Param("id") Long id,
               @Param("reason") String reason,
               @Param("operatorId") Long operatorId,
               @Param("now") LocalDateTime now);

    int cancel(@Param("id") Long id,
               @Param("operatorId") Long operatorId);

    OrderStatsOverviewVO statsOverview();

    List<Map<String, Object>> statsTrend(@Param("days") int days);

    List<Map<String, Object>> statsPlanDistribution();

    List<Map<String, Object>> statsCycleDistribution();
}
```

- [ ] **Step 6: 创建 AdminMembershipMapper interface**

```java
package com.aichuangzuo.admin.modules.order.mapper;

import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminMembershipMapper {

    AdminMembership selectByUserId(@Param("userId") Long userId);

    int insertMembership(AdminMembership membership);

    int updateMembership(AdminMembership membership);

    int updateUserMembershipFields(@Param("userId") Long userId,
                                   @Param("expireAt") LocalDateTime expireAt,
                                   @Param("plan") String plan);

    int userExists(@Param("userId") Long userId);
}
```

- [ ] **Step 7: 创建 AdminOrderMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aichuangzuo.admin.modules.order.mapper.AdminOrderMapper">

    <resultMap id="orderMap" type="com.aichuangzuo.admin.modules.order.entity.AdminOrderView">
        <id property="id" column="id"/>
        <result property="orderNo" column="order_no"/>
        <result property="userId" column="user_id"/>
        <result property="nickname" column="nickname"/>
        <result property="email" column="email"/>
        <result property="planKey" column="plan_key"/>
        <result property="cycle" column="cycle"/>
        <result property="amount" column="amount"/>
        <result property="status" column="status"/>
        <result property="paidAt" column="paid_at"/>
        <result property="refundedAt" column="refunded_at"/>
        <result property="refundReason" column="refund_reason"/>
        <result property="adminRemark" column="admin_remark"/>
        <result property="operatorId" column="operator_id"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <sql id="selectCols">
        SELECT o.id, o.order_no, o.user_id,
               u.nickname, u.email,
               o.plan_key, o.cycle, o.amount, o.status,
               o.paid_at, o.refunded_at, o.refund_reason,
               o.admin_remark, o.operator_id, o.created_at
        FROM u_order o
        LEFT JOIN u_user u ON u.id = o.user_id AND u.is_deleted = 0
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
            AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.email LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="planKey != null and planKey != ''">
            AND o.plan_key = #{planKey}
        </if>
        <if test="status != null">
            AND o.status = #{status}
        </if>
        <if test="startDate != null and startDate != ''">
            AND o.created_at &gt;= CONCAT(#{startDate}, ' 00:00:00')
        </if>
        <if test="endDate != null and endDate != ''">
            AND o.created_at &lt;= CONCAT(#{endDate}, ' 23:59:59')
        </if>
    </sql>

    <select id="selectPage" resultMap="orderMap">
        <include refid="selectCols"/>
        ORDER BY o.created_at DESC
        LIMIT #{offset}, #{size}
    </select>

    <select id="countPage" resultType="long">
        SELECT COUNT(*)
        FROM u_order o
        LEFT JOIN u_user u ON u.id = o.user_id AND u.is_deleted = 0
        WHERE 1 = 1
        <if test="keyword != null and keyword != ''">
            AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.email LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="planKey != null and planKey != ''">
            AND o.plan_key = #{planKey}
        </if>
        <if test="status != null">
            AND o.status = #{status}
        </if>
        <if test="startDate != null and startDate != ''">
            AND o.created_at &gt;= CONCAT(#{startDate}, ' 00:00:00')
        </if>
        <if test="endDate != null and endDate != ''">
            AND o.created_at &lt;= CONCAT(#{endDate}, ' 23:59:59')
        </if>
    </select>

    <select id="selectDetailById" resultMap="orderMap">
        <include refid="selectCols"/>
        AND o.id = #{id}
        LIMIT 1
    </select>

    <update id="markPaid">
        UPDATE u_order
        SET status = 1,
            paid_at = #{now},
            operator_id = #{operatorId},
            updated_at = #{now}
        WHERE id = #{id} AND status = 0
    </update>

    <update id="refund">
        UPDATE u_order
        SET status = 2,
            refunded_at = #{now},
            refund_reason = #{reason},
            operator_id = #{operatorId},
            updated_at = #{now}
        WHERE id = #{id} AND status = 1
    </update>

    <update id="cancel">
        UPDATE u_order
        SET status = 3,
            operator_id = #{operatorId},
            updated_at = NOW(3)
        WHERE id = #{id} AND status = 0
    </update>

    <select id="statsOverview" resultType="com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO">
        SELECT
            (SELECT COUNT(*) FROM u_order WHERE status = 1 AND DATE(paid_at) = CURDATE()) AS todayOrderCount,
            (SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1 AND DATE(paid_at) = CURDATE()) AS todayRevenue,
            (SELECT COUNT(*) FROM u_order WHERE status = 1 AND DATE_FORMAT(paid_at, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')) AS monthOrderCount,
            (SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1 AND DATE_FORMAT(paid_at, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')) AS monthRevenue,
            (SELECT COUNT(*) FROM u_order WHERE status = 1) AS totalOrderCount,
            (SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1) AS totalRevenue
    </select>

    <select id="statsTrend" resultType="java.util.LinkedHashMap">
        SELECT DATE_FORMAT(d.dt, '%m-%d') AS dateLabel,
               COALESCE(stats.revenue, 0) AS revenue,
               COALESCE(stats.orderCount, 0) AS orderCount
        FROM (
            SELECT DATE_SUB(CURDATE(), INTERVAL n DAY) AS dt
            FROM (
                SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
                UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
                UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
                UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
                UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
            ) nums
            WHERE n &lt; #{days}
        ) d
        LEFT JOIN (
            SELECT DATE(paid_at) AS payDate,
                   SUM(amount) AS revenue,
                   COUNT(*) AS orderCount
            FROM u_order
            WHERE status = 1
              AND paid_at &gt;= DATE_SUB(CURDATE(), INTERVAL #{days} - 1 DAY)
            GROUP BY DATE(paid_at)
        ) stats ON stats.payDate = d.dt
        ORDER BY d.dt ASC
    </select>

    <select id="statsPlanDistribution" resultType="java.util.LinkedHashMap">
        SELECT plan_key AS planKey,
               COUNT(*) AS count,
               COALESCE(SUM(amount), 0) AS revenue
        FROM u_order
        WHERE status = 1
        GROUP BY plan_key
        ORDER BY count DESC
    </select>

    <select id="statsCycleDistribution" resultType="java.util.LinkedHashMap">
        SELECT cycle,
               COUNT(*) AS count
        FROM u_order
        WHERE status = 1
        GROUP BY cycle
        ORDER BY count DESC
    </select>
</mapper>
```

- [ ] **Step 8: 创建 AdminMembershipMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper">

    <resultMap id="membershipMap" type="com.aichuangzuo.admin.modules.order.entity.AdminMembership">
        <id property="id" column="id"/>
        <result property="userId" column="user_id"/>
        <result property="level" column="level"/>
        <result property="startedAt" column="started_at"/>
        <result property="expiresAt" column="expires_at"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="selectByUserId" resultMap="membershipMap">
        SELECT id, user_id, level, started_at, expires_at, created_at, updated_at
        FROM u_user_membership
        WHERE user_id = #{userId}
        LIMIT 1
    </select>

    <insert id="insertMembership" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO u_user_membership (user_id, level, started_at, expires_at, tenant_id)
        VALUES (#{userId}, #{level}, #{startedAt}, #{expiresAt}, 0)
    </insert>

    <update id="updateMembership">
        UPDATE u_user_membership
        SET level = #{level},
            started_at = #{startedAt},
            expires_at = #{expiresAt}
        WHERE id = #{id}
    </update>

    <update id="updateUserMembershipFields">
        UPDATE u_user
        SET membership_expire_at = #{expireAt},
            membership_plan = #{plan}
        WHERE id = #{userId} AND is_deleted = 0
    </update>

    <select id="userExists" resultType="int">
        SELECT COUNT(*) FROM u_user WHERE id = #{userId} AND is_deleted = 0
    </select>
</mapper>
```

- [ ] **Step 9: 创建 DTO 文件**

`OrderRefundRequest.java`:
```java
package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderRefundRequest {
    @NotBlank(message = "退款原因不能为空")
    private String reason;
}
```

`MembershipAdjustRequest.java`:
```java
package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembershipAdjustRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "会员等级不能为空")
    private String level;
    @NotNull(message = "到期时间不能为空")
    private LocalDate expiresAt;
    private String remark;
}
```

`MembershipGrantRequest.java`:
```java
package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembershipGrantRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "套餐不能为空")
    private String planKey;
    @NotBlank(message = "周期不能为空")
    private String cycle;
    private String remark;
}
```

- [ ] **Step 10: 创建 VO 文件**

`OrderListVO.java`:
```java
package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String email;
    private String planKey;
    private String planName;
    private String cycle;
    private String cycleName;
    private BigDecimal amount;
    private Integer status;
    private String statusName;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
```

`OrderDetailVO.java`:
```java
package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String email;
    private String planKey;
    private String planName;
    private String cycle;
    private String cycleName;
    private BigDecimal amount;
    private Integer status;
    private String statusName;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private String adminRemark;
    private LocalDateTime createdAt;
}
```

`OrderPageVO.java`:
```java
package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderPageVO {
    private List<OrderListVO> list;
    private long total;
}
```

`OrderStatsOverviewVO.java`:
```java
package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatsOverviewVO {
    private Long todayOrderCount;
    private BigDecimal todayRevenue;
    private Long monthOrderCount;
    private BigDecimal monthRevenue;
    private Long totalOrderCount;
    private BigDecimal totalRevenue;
}
```

`OrderTrendVO.java`:
```java
package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderTrendVO {
    private List<String> dates;
    private List<BigDecimal> revenues;
    private List<Long> orderCounts;
}
```

`PlanDistributionVO.java`:
```java
package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanDistributionVO {
    private List<PlanItem> plans;
    private List<CycleItem> cycles;

    @Data
    public static class PlanItem {
        private String planKey;
        private String planName;
        private Long count;
        private BigDecimal revenue;
    }

    @Data
    public static class CycleItem {
        private String cycle;
        private String cycleName;
        private Long count;
    }
}
```

- [ ] **Step 11: 编译验证**

```bash
cd project/admin/api && ./mvnw compile -q 2>&1 | tail -10
```
Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/
git add project/admin/api/src/main/resources/mapper/order/
git commit -m "feat(admin-api): 订单模块数据层 — entity/mapper/DTO/VO"
```

---

### Task 3: 后端 Service 层 — 业务逻辑 + 单元测试

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/service/AdminOrderService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/service/impl/AdminOrderServiceImpl.java`
- Test: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/order/service/AdminOrderServiceTest.java`

**Interfaces:**
- Consumes: `AdminOrderMapper`, `AdminMembershipMapper` (from Task 2)
- Produces:
  - `AdminOrderService.listOrders(keyword, planKey, status, startDate, endDate, page, pageSize)` → `OrderPageVO`
  - `AdminOrderService.getOrderDetail(id)` → `OrderDetailVO`
  - `AdminOrderService.markPaid(id, operatorId)` → `void`
  - `AdminOrderService.refund(id, reason, operatorId)` → `void`
  - `AdminOrderService.cancel(id, operatorId)` → `void`
  - `AdminOrderService.adjustMembership(request, operatorId)` → `void`
  - `AdminOrderService.grantMembership(request, operatorId)` → `void`
  - `AdminOrderService.getStatsOverview()` → `OrderStatsOverviewVO`
  - `AdminOrderService.getStatsTrend(days)` → `OrderTrendVO`
  - `AdminOrderService.getPlanDistribution()` → `PlanDistributionVO`

- [ ] **Step 1: 写失败的单元测试**

```java
package com.aichuangzuo.admin.modules.order.service;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.enums.AdminOrderErrorCode;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.order.mapper.AdminOrderMapper;
import com.aichuangzuo.admin.modules.order.service.impl.AdminOrderServiceImpl;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private AdminOrderMapper orderMapper;

    @Mock
    private AdminMembershipMapper membershipMapper;

    @InjectMocks
    private AdminOrderServiceImpl orderService;

    private AdminOrderView paidOrder;
    private AdminOrderView pendingOrder;

    @BeforeEach
    void setUp() {
        paidOrder = new AdminOrderView();
        paidOrder.setId(1L);
        paidOrder.setOrderNo("SUB260715000001");
        paidOrder.setUserId(5L);
        paidOrder.setPlanKey("pro");
        paidOrder.setCycle("month");
        paidOrder.setAmount(new BigDecimal("59.90"));
        paidOrder.setStatus(1);
        paidOrder.setPaidAt(LocalDateTime.now());

        pendingOrder = new AdminOrderView();
        pendingOrder.setId(2L);
        pendingOrder.setOrderNo("SUB260715000002");
        pendingOrder.setUserId(5L);
        pendingOrder.setPlanKey("basic");
        pendingOrder.setCycle("quarter");
        pendingOrder.setAmount(new BigDecimal("80.70"));
        pendingOrder.setStatus(0);
    }

    // ── markPaid ──

    @Test
    void markPaid_orderNotFound_throws() {
        when(orderMapper.selectDetailById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.markPaid(99L, 1L));
        assertEquals(AdminOrderErrorCode.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void markPaid_alreadyPaid_throws() {
        when(orderMapper.selectDetailById(1L)).thenReturn(paidOrder);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.markPaid(1L, 1L));
        assertEquals(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void markPaid_pendingOrder_success() {
        when(orderMapper.selectDetailById(2L)).thenReturn(pendingOrder);
        when(orderMapper.markPaid(eq(2L), eq(1L), any())).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(null);
        when(membershipMapper.insertMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.markPaid(2L, 1L));
        verify(orderMapper).markPaid(eq(2L), eq(1L), any());
        verify(membershipMapper).insertMembership(argThat(m ->
                "basic".equals(m.getLevel()) && m.getUserId() == 5L));
    }

    // ── refund ──

    @Test
    void refund_orderNotPaid_throws() {
        when(orderMapper.selectDetailById(2L)).thenReturn(pendingOrder);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.refund(2L, "不想要了", 1L));
        assertEquals(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void refund_paidOrder_success() {
        AdminMembership membership = new AdminMembership();
        membership.setId(1L);
        membership.setUserId(5L);
        membership.setLevel("pro");
        membership.setStartedAt(LocalDate.now().minusDays(5));
        membership.setExpiresAt(LocalDate.now().plusDays(25));

        when(orderMapper.selectDetailById(1L)).thenReturn(paidOrder);
        when(orderMapper.refund(eq(1L), eq("不想要了"), eq(1L), any())).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(membership);
        when(membershipMapper.updateMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.refund(1L, "不想要了", 1L));
        // pro/month = 30 days, 25 - 30 = -5 → expiresAt should be in the past
        verify(membershipMapper).updateMembership(argThat(m ->
                m.getExpiresAt().isBefore(LocalDate.now())));
    }

    // ── cancel ──

    @Test
    void cancel_pendingOrder_success() {
        when(orderMapper.selectDetailById(2L)).thenReturn(pendingOrder);
        when(orderMapper.cancel(2L, 1L)).thenReturn(1);

        assertDoesNotThrow(() -> orderService.cancel(2L, 1L));
        verify(orderMapper).cancel(2L, 1L);
    }

    @Test
    void cancel_paidOrder_throws() {
        when(orderMapper.selectDetailById(1L)).thenReturn(paidOrder);
        assertThrows(BusinessException.class, () -> orderService.cancel(1L, 1L));
    }

    // ── adjustMembership ──

    @Test
    void adjustMembership_userNotFound_throws() {
        MembershipAdjustRequest req = new MembershipAdjustRequest();
        req.setUserId(99L);
        req.setLevel("pro");
        req.setExpiresAt(LocalDate.now().plusDays(30));

        when(membershipMapper.userExists(99L)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.adjustMembership(req, 1L));
        assertEquals(AdminOrderErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void adjustMembership_existingMembership_updates() {
        MembershipAdjustRequest req = new MembershipAdjustRequest();
        req.setUserId(5L);
        req.setLevel("flagship");
        req.setExpiresAt(LocalDate.now().plusDays(365));

        AdminMembership existing = new AdminMembership();
        existing.setId(1L);
        existing.setUserId(5L);
        existing.setLevel("basic");

        when(membershipMapper.userExists(5L)).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(existing);
        when(membershipMapper.updateMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.adjustMembership(req, 1L));
        verify(membershipMapper).updateMembership(argThat(m ->
                "flagship".equals(m.getLevel())));
    }

    // ── grantMembership ──

    @Test
    void grantMembership_createsZeroOrderAndActivates() {
        MembershipGrantRequest req = new MembershipGrantRequest();
        req.setUserId(5L);
        req.setPlanKey("pro");
        req.setCycle("month");
        req.setRemark("活动赠送");

        when(membershipMapper.userExists(5L)).thenReturn(1);
        when(membershipMapper.selectByUserId(5L)).thenReturn(null);
        when(membershipMapper.insertMembership(any())).thenReturn(1);
        when(membershipMapper.updateUserMembershipFields(eq(5L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> orderService.grantMembership(req, 1L));
        verify(orderMapper).markPaid(anyLong(), eq(1L), any());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd project/admin/api && ./mvnw test -Dtest=AdminOrderServiceTest -q 2>&1 | tail -20
```
Expected: 编译失败或测试失败（service 尚未实现）

- [ ] **Step 3: 创建 AdminOrderService interface**

```java
package com.aichuangzuo.admin.modules.order.service;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.vo.OrderDetailVO;
import com.aichuangzuo.admin.modules.order.vo.OrderPageVO;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.OrderTrendVO;
import com.aichuangzuo.admin.modules.order.vo.PlanDistributionVO;

public interface AdminOrderService {

    OrderPageVO listOrders(String keyword, String planKey, Integer status,
                           String startDate, String endDate, int page, int pageSize);

    OrderDetailVO getOrderDetail(Long id);

    void markPaid(Long id, Long operatorId);

    void refund(Long id, String reason, Long operatorId);

    void cancel(Long id, Long operatorId);

    void adjustMembership(MembershipAdjustRequest request, Long operatorId);

    void grantMembership(MembershipGrantRequest request, Long operatorId);

    OrderStatsOverviewVO getStatsOverview();

    OrderTrendVO getStatsTrend(int days);

    PlanDistributionVO getPlanDistribution();
}
```

- [ ] **Step 4: 创建 AdminOrderServiceImpl**

```java
package com.aichuangzuo.admin.modules.order.service.impl;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.enums.AdminOrderErrorCode;
import com.aichuangzuo.admin.modules.order.enums.OrderStatus;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.order.mapper.AdminOrderMapper;
import com.aichuangzuo.admin.modules.order.service.AdminOrderService;
import com.aichuangzuo.admin.modules.order.vo.*;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final Map<String, String> PLAN_NAMES = Map.of(
            "basic", "基础版", "pro", "专业版", "flagship", "旗舰版");
    private static final Map<String, String> CYCLE_NAMES = Map.of(
            "month", "月付", "quarter", "季付", "year", "年付");
    private static final Map<String, Integer> CYCLE_DAYS = Map.of(
            "month", 30, "quarter", 90, "year", 365);

    private final AdminOrderMapper orderMapper;
    private final AdminMembershipMapper membershipMapper;

    @Override
    public OrderPageVO listOrders(String keyword, String planKey, Integer status,
                                  String startDate, String endDate, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        List<AdminOrderView> rows = orderMapper.selectPage(keyword, planKey, status, startDate, endDate, offset, pageSize);
        long total = orderMapper.countPage(keyword, planKey, status, startDate, endDate);

        List<OrderListVO> list = rows.stream().map(this::toListVO).toList();
        OrderPageVO vo = new OrderPageVO();
        vo.setList(list);
        vo.setTotal(total);
        return vo;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        AdminOrderView row = orderMapper.selectDetailById(id);
        if (row == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        return toDetailVO(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long id, Long operatorId) {
        AdminOrderView order = orderMapper.selectDetailById(id);
        if (order == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        orderMapper.markPaid(id, operatorId, now);
        activateOrExtendMembership(order.getUserId(), order.getPlanKey(), order.getCycle());

        log.info("管理员标记订单已支付 orderId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id, String reason, Long operatorId) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(AdminOrderErrorCode.REFUND_REASON_REQUIRED);
        }
        AdminOrderView order = orderMapper.selectDetailById(id);
        if (order == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        orderMapper.refund(id, reason, operatorId, now);

        // 回退会员时长
        AdminMembership membership = membershipMapper.selectByUserId(order.getUserId());
        if (membership != null) {
            int days = CYCLE_DAYS.getOrDefault(order.getCycle(), 30);
            LocalDate newExpiresAt = membership.getExpiresAt().minusDays(days);
            membership.setExpiresAt(newExpiresAt);
            membershipMapper.updateMembership(membership);
            syncUserMembershipFields(order.getUserId(), newExpiresAt, order.getCycle());
        }

        log.info("管理员退款 orderId={}, operatorId={}, reason={}", id, operatorId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long operatorId) {
        AdminOrderView order = orderMapper.selectDetailById(id);
        if (order == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        orderMapper.cancel(id, operatorId);
        log.info("管理员取消订单 orderId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustMembership(MembershipAdjustRequest request, Long operatorId) {
        if (membershipMapper.userExists(request.getUserId()) == 0) {
            throw new BusinessException(AdminOrderErrorCode.USER_NOT_FOUND);
        }

        AdminMembership membership = membershipMapper.selectByUserId(request.getUserId());
        if (membership == null) {
            membership = new AdminMembership();
            membership.setUserId(request.getUserId());
            membership.setLevel(request.getLevel());
            membership.setStartedAt(LocalDate.now());
            membership.setExpiresAt(request.getExpiresAt());
            membershipMapper.insertMembership(membership);
        } else {
            membership.setLevel(request.getLevel());
            membership.setStartedAt(LocalDate.now());
            membership.setExpiresAt(request.getExpiresAt());
            membershipMapper.updateMembership(membership);
        }

        syncUserMembershipFields(request.getUserId(), request.getExpiresAt(), null);
        log.info("管理员调整会员 userId={}, level={}, expiresAt={}, operatorId={}",
                request.getUserId(), request.getLevel(), request.getExpiresAt(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMembership(MembershipGrantRequest request, Long operatorId) {
        if (membershipMapper.userExists(request.getUserId()) == 0) {
            throw new BusinessException(AdminOrderErrorCode.USER_NOT_FOUND);
        }

        // 创建 0 元已支付订单
        LocalDateTime now = LocalDateTime.now();
        String remark = request.getRemark() != null ? "手动发放：" + request.getRemark() : "手动发放";

        // 直接通过 mapper 插入（需要一个 insert 方法）
        // 这里复用 markPaid 的方式：先创建 pending 订单，再标记已支付
        // 但为了简洁，我们直接在 service 里用 SQL 插入
        // 实际上需要一个 AdminOrder 实体和 insert 方法
        // 为了简化，使用 orderMapper 的一个自定义 insert
        // 但当前 mapper 没有 insert 方法，需要添加

        // 简化方案：直接激活/延长会员，订单通过 markPaid 流程走
        // 需要先在 u_order 插入一条记录，然后标记已支付
        // 这里我们添加一个 insertGrantOrder 方法到 mapper

        // 激活/延长会员
        activateOrExtendMembership(request.getUserId(), request.getPlanKey(), request.getCycle());

        log.info("管理员发放会员 userId={}, planKey={}, cycle={}, operatorId={}",
                request.getUserId(), request.getPlanKey(), request.getCycle(), operatorId);
    }

    @Override
    public OrderStatsOverviewVO getStatsOverview() {
        return orderMapper.statsOverview();
    }

    @Override
    public OrderTrendVO getStatsTrend(int days) {
        if (days != 7 && days != 30) {
            days = 7;
        }
        List<Map<String, Object>> rows = orderMapper.statsTrend(days);
        OrderTrendVO vo = new OrderTrendVO();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            dates.add((String) row.get("dateLabel"));
            revenues.add((BigDecimal) row.get("revenue"));
            orderCounts.add(((Number) row.get("orderCount")).longValue());
        }
        vo.setDates(dates);
        vo.setRevenues(revenues);
        vo.setOrderCounts(orderCounts);
        return vo;
    }

    @Override
    public PlanDistributionVO getPlanDistribution() {
        List<Map<String, Object>> planRows = orderMapper.statsPlanDistribution();
        List<Map<String, Object>> cycleRows = orderMapper.statsCycleDistribution();

        PlanDistributionVO vo = new PlanDistributionVO();

        List<PlanDistributionVO.PlanItem> plans = new ArrayList<>();
        for (Map<String, Object> row : planRows) {
            PlanDistributionVO.PlanItem item = new PlanDistributionVO.PlanItem();
            String key = (String) row.get("planKey");
            item.setPlanKey(key);
            item.setPlanName(PLAN_NAMES.getOrDefault(key, key));
            item.setCount(((Number) row.get("count")).longValue());
            item.setRevenue((BigDecimal) row.get("revenue"));
            plans.add(item);
        }
        vo.setPlans(plans);

        List<PlanDistributionVO.CycleItem> cycles = new ArrayList<>();
        for (Map<String, Object> row : cycleRows) {
            PlanDistributionVO.CycleItem item = new PlanDistributionVO.CycleItem();
            String code = (String) row.get("cycle");
            item.setCycle(code);
            item.setCycleName(CYCLE_NAMES.getOrDefault(code, code));
            item.setCount(((Number) row.get("count")).longValue());
            cycles.add(item);
        }
        vo.setCycles(cycles);

        return vo;
    }

    // ── private helpers ──

    private void activateOrExtendMembership(Long userId, String planKey, String cycle) {
        AdminMembership membership = membershipMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        int days = CYCLE_DAYS.getOrDefault(cycle, 30);

        LocalDate baseDate = today;
        if (membership != null && membership.getExpiresAt().isAfter(today.minusDays(1))) {
            baseDate = membership.getExpiresAt();
        }
        LocalDate newExpiresAt = baseDate.plusDays(days);

        if (membership == null) {
            membership = new AdminMembership();
            membership.setUserId(userId);
            membership.setLevel(planKey);
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membershipMapper.insertMembership(membership);
        } else {
            membership.setLevel(planKey);
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membershipMapper.updateMembership(membership);
        }

        syncUserMembershipFields(userId, newExpiresAt, cycle);
    }

    private void syncUserMembershipFields(Long userId, LocalDate expiresAt, String cycle) {
        LocalDateTime expireDateTime = expiresAt.atTime(LocalTime.MAX);
        membershipMapper.updateUserMembershipFields(userId, expireDateTime, cycle);
    }

    private OrderListVO toListVO(AdminOrderView row) {
        OrderListVO vo = new OrderListVO();
        vo.setId(row.getId());
        vo.setOrderNo(row.getOrderNo());
        vo.setUserId(row.getUserId());
        vo.setNickname(row.getNickname());
        vo.setEmail(row.getEmail());
        vo.setPlanKey(row.getPlanKey());
        vo.setPlanName(PLAN_NAMES.getOrDefault(row.getPlanKey(), row.getPlanKey()));
        vo.setCycle(row.getCycle());
        vo.setCycleName(CYCLE_NAMES.getOrDefault(row.getCycle(), row.getCycle()));
        vo.setAmount(row.getAmount());
        vo.setStatus(row.getStatus());
        OrderStatus os = OrderStatus.of(row.getStatus());
        vo.setStatusName(os != null ? os.getDisplayName() : "未知");
        vo.setPaidAt(row.getPaidAt());
        vo.setRefundedAt(row.getRefundedAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private OrderDetailVO toDetailVO(AdminOrderView row) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(row.getId());
        vo.setOrderNo(row.getOrderNo());
        vo.setUserId(row.getUserId());
        vo.setNickname(row.getNickname());
        vo.setEmail(row.getEmail());
        vo.setPlanKey(row.getPlanKey());
        vo.setPlanName(PLAN_NAMES.getOrDefault(row.getPlanKey(), row.getPlanKey()));
        vo.setCycle(row.getCycle());
        vo.setCycleName(CYCLE_NAMES.getOrDefault(row.getCycle(), row.getCycle()));
        vo.setAmount(row.getAmount());
        vo.setStatus(row.getStatus());
        OrderStatus os = OrderStatus.of(row.getStatus());
        vo.setStatusName(os != null ? os.getDisplayName() : "未知");
        vo.setPaidAt(row.getPaidAt());
        vo.setRefundedAt(row.getRefundedAt());
        vo.setRefundReason(row.getRefundReason());
        vo.setAdminRemark(row.getAdminRemark());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
```

Wait — the `grantMembership` method needs to create an order record. I need to add an `insertGrantOrder` method to `AdminOrderMapper`. Let me add that to the mapper and XML.

Add to `AdminOrderMapper.java`:
```java
int insertGrantOrder(@Param("orderNo") String orderNo,
                     @Param("userId") Long userId,
                     @Param("planKey") String planKey,
                     @Param("cycle") String cycle,
                     @Param("adminRemark") String adminRemark,
                     @Param("operatorId") Long operatorId,
                     @Param("now") LocalDateTime now);
```

Add to `AdminOrderMapper.xml`:
```xml
<insert id="insertGrantOrder">
    INSERT INTO u_order (order_no, user_id, plan_key, cycle, amount, status, paid_at, admin_remark, operator_id, tenant_id)
    VALUES (#{orderNo}, #{userId}, #{planKey}, #{cycle}, 0, 1, #{now}, #{adminRemark}, #{operatorId}, 0)
</insert>
```

Then fix `grantMembership` in the service impl:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void grantMembership(MembershipGrantRequest request, Long operatorId) {
    if (membershipMapper.userExists(request.getUserId()) == 0) {
        throw new BusinessException(AdminOrderErrorCode.USER_NOT_FOUND);
    }

    LocalDateTime now = LocalDateTime.now();
    String remark = request.getRemark() != null ? "手动发放：" + request.getRemark() : "手动发放";
    String orderNo = generateOrderNo();

    orderMapper.insertGrantOrder(orderNo, request.getUserId(), request.getPlanKey(),
            request.getCycle(), remark, operatorId, now);
    activateOrExtendMembership(request.getUserId(), request.getPlanKey(), request.getCycle());

    log.info("管理员发放会员 userId={}, planKey={}, cycle={}, operatorId={}",
            request.getUserId(), request.getPlanKey(), request.getCycle(), operatorId);
}

private String generateOrderNo() {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
    String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    return "SUB" + date + random;
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd project/admin/api && ./mvnw test -Dtest=AdminOrderServiceTest -q 2>&1 | tail -20
```
Expected: 全部测试 PASS

- [ ] **Step 6: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/service/
git add project/admin/api/src/test/java/com/aichuangzuo/admin/modules/order/
git commit -m "feat(admin-api): 订单模块 service 层 — 标记支付/退款/取消/调整/发放会员"
```

---

### Task 4: 后端 Controller 层

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/controller/AdminOrderController.java`

**Interfaces:**
- Consumes: `AdminOrderService` (from Task 3)
- Produces: 10 个 REST 端点

- [ ] **Step 1: 创建 AdminOrderController**

```java
package com.aichuangzuo.admin.modules.order.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.dto.request.OrderRefundRequest;
import com.aichuangzuo.admin.modules.order.service.AdminOrderService;
import com.aichuangzuo.admin.modules.order.vo.OrderDetailVO;
import com.aichuangzuo.admin.modules.order.vo.OrderPageVO;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.OrderTrendVO;
import com.aichuangzuo.admin.modules.order.vo.PlanDistributionVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端订单管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService orderService;

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<OrderPageVO> listOrders(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "planKey", required = false) String planKey,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return Result.success(orderService.listOrders(keyword, planKey, status, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable(name = "id") Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @Operation(summary = "标记已支付")
    @PostMapping("/orders/{id}/mark-paid")
    public Result<Void> markPaid(@PathVariable(name = "id") Long id) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.markPaid(id, adminId);
        return Result.success();
    }

    @Operation(summary = "退款")
    @PostMapping("/orders/{id}/refund")
    public Result<Void> refund(@PathVariable(name = "id") Long id,
                               @Valid @RequestBody OrderRefundRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.refund(id, request.getReason(), adminId);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable(name = "id") Long id) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.cancel(id, adminId);
        return Result.success();
    }

    @Operation(summary = "手动调整会员")
    @PostMapping("/membership/adjust")
    public Result<Void> adjustMembership(@Valid @RequestBody MembershipAdjustRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.adjustMembership(request, adminId);
        return Result.success();
    }

    @Operation(summary = "手动发放会员")
    @PostMapping("/membership/grant")
    public Result<Void> grantMembership(@Valid @RequestBody MembershipGrantRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.grantMembership(request, adminId);
        return Result.success();
    }

    @Operation(summary = "统计概览")
    @GetMapping("/orders/stats/overview")
    public Result<OrderStatsOverviewVO> statsOverview() {
        return Result.success(orderService.getStatsOverview());
    }

    @Operation(summary = "收入趋势")
    @GetMapping("/orders/stats/trend")
    public Result<OrderTrendVO> statsTrend(@RequestParam(name = "days", defaultValue = "7") int days) {
        return Result.success(orderService.getStatsTrend(days));
    }

    @Operation(summary = "套餐分布")
    @GetMapping("/orders/stats/plan-distribution")
    public Result<PlanDistributionVO> planDistribution() {
        return Result.success(orderService.getPlanDistribution());
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && ./mvnw compile -q 2>&1 | tail -10
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 运行全部订单模块测试**

```bash
cd project/admin/api && ./mvnw test -Dtest="AdminOrder*" -q 2>&1 | tail -20
```
Expected: 全部 PASS

- [ ] **Step 4: Commit**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/order/controller/
git commit -m "feat(admin-api): 订单管理 REST 端点"
```

---

### Task 5: 前端基础设施 — API 层 + 侧边栏 + 路由

**Files:**
- Create: `project/admin/web/src/api/order.js`
- Modify: `project/admin/web/src/layouts/AdminLayout.vue` — 插入菜单 + 添加路由匹配 + 添加面包屑名称
- Modify: `project/admin/web/src/router/index.js` — 新增 2 条路由

**Interfaces:**
- Produces:
  - `getOrderList(params)` → `{ list, total }`
  - `getOrderDetail(id)` → `OrderDetailVO`
  - `markOrderPaid(id)` → `void`
  - `refundOrder(id, data)` → `void`
  - `cancelOrder(id)` → `void`
  - `adjustMembership(data)` → `void`
  - `grantMembership(data)` → `void`
  - `getOrderStatsOverview()` → `OrderStatsOverviewVO`
  - `getOrderTrend(days)` → `OrderTrendVO`
  - `getPlanDistribution()` → `PlanDistributionVO`

- [ ] **Step 1: 创建 API 层 `src/api/order.js`**

```js
import request from '@/utils/request.js'

export function getOrderList(params = {}) {
  return request.get('/api/v1/admin/orders', { params }).then((res) => res.data)
}

export function getOrderDetail(id) {
  return request.get(`/api/v1/admin/orders/${id}`).then((res) => res.data)
}

export function markOrderPaid(id) {
  return request.post(`/api/v1/admin/orders/${id}/mark-paid`).then((res) => res.data)
}

export function refundOrder(id, data) {
  return request.post(`/api/v1/admin/orders/${id}/refund`, data).then((res) => res.data)
}

export function cancelOrder(id) {
  return request.post(`/api/v1/admin/orders/${id}/cancel`).then((res) => res.data)
}

export function adjustMembership(data) {
  return request.post('/api/v1/admin/membership/adjust', data).then((res) => res.data)
}

export function grantMembership(data) {
  return request.post('/api/v1/admin/membership/grant', data).then((res) => res.data)
}

export function getOrderStatsOverview() {
  return request.get('/api/v1/admin/orders/stats/overview').then((res) => res.data)
}

export function getOrderTrend(days = 7) {
  return request.get('/api/v1/admin/orders/stats/trend', { params: { days } }).then((res) => res.data)
}

export function getPlanDistribution() {
  return request.get('/api/v1/admin/orders/stats/plan-distribution').then((res) => res.data)
}
```

- [ ] **Step 2: 修改 AdminLayout.vue — 添加菜单**

在 `<a-menu-item key="/console/feedbacks">` 和 `<a-sub-menu key="/console/settings">` 之间插入：

```html
<a-sub-menu key="/console/orders">
  <template #icon>
    <ShoppingCartOutlined />
  </template>
  <template #title>订单管理</template>
  <a-menu-item key="/console/orders/list">订单列表</a-menu-item>
  <a-menu-item key="/console/orders/stats">数据统计</a-menu-item>
</a-sub-menu>
```

在 `<script setup>` 的 import 行添加 `ShoppingCartOutlined`：
```js
import { UserOutlined, AuditOutlined, AppstoreOutlined, SettingOutlined, ApiOutlined, FireOutlined, TrophyOutlined, DollarOutlined, BookOutlined, ReadOutlined, MessageOutlined, CommentOutlined, FileTextOutlined, ExperimentOutlined, UnorderedListOutlined, SlidersOutlined, PictureOutlined, ShoppingCartOutlined } from '@ant-design/icons-vue'
```

在 `parentMenuKey` computed 中添加订单路由匹配（在 `return null` 前）：
```js
if (p.startsWith('/console/orders/')) return '/console/orders'
```

在 `currentMenuName` computed 中添加（在 `return ''` 前）：
```js
if (route.path === '/console/orders/list') return '订单列表'
if (route.path === '/console/orders/stats') return '数据统计'
```

- [ ] **Step 3: 修改 router/index.js — 添加路由**

在 `home-banner` 路由后添加：

```js
{
  path: 'orders/list',
  name: 'AdminOrderList',
  component: () => import('@/views/OrderListView.vue')
},
{
  path: 'orders/stats',
  name: 'AdminOrderStats',
  component: () => import('@/views/OrderStatsView.vue')
}
```

- [ ] **Step 4: Commit**

```bash
git add project/admin/web/src/api/order.js project/admin/web/src/layouts/AdminLayout.vue project/admin/web/src/router/index.js
git commit -m "feat(admin-web): 订单管理菜单 + API 层 + 路由"
```

---

### Task 6: 前端订单列表页 — OrderListView.vue

**Files:**
- Create: `project/admin/web/src/views/OrderListView.vue`

**Interfaces:**
- Consumes: `getOrderList`, `getOrderDetail`, `markOrderPaid`, `refundOrder`, `cancelOrder`, `adjustMembership`, `grantMembership` (from Task 5)
- Produces: 订单列表页面，路由 `/console/orders/list`

- [ ] **Step 1: 创建 OrderListView.vue**

```vue
<template>
  <a-card :bordered="false" class="order-admin">
    <div class="page-header">
      <h3 class="page-title">订单列表</h3>
      <p class="page-desc">查看和管理用户订阅订单</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-input
        v-model:value="keyword"
        placeholder="搜索昵称或邮箱"
        style="width: 200px"
        allow-clear
        @press-enter="handleSearch"
      />
      <a-select v-model:value="planKey" placeholder="套餐" style="width: 120px" allow-clear>
        <a-select-option value="basic">基础版</a-select-option>
        <a-select-option value="pro">专业版</a-select-option>
        <a-select-option value="flagship">旗舰版</a-select-option>
      </a-select>
      <a-select v-model:value="statusFilter" placeholder="状态" style="width: 120px" allow-clear>
        <a-select-option :value="0">待支付</a-select-option>
        <a-select-option :value="1">已支付</a-select-option>
        <a-select-option :value="2">已退款</a-select-option>
        <a-select-option :value="3">已取消</a-select-option>
      </a-select>
      <a-range-picker v-model:value="dateRange" style="width: 240px" />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button @click="handleReset">重置</a-button>
      <div style="flex: 1" />
      <a-button type="primary" ghost @click="openGrantModal">手动发放会员</a-button>
      <a-button ghost @click="openAdjustModal">手动调整会员</a-button>
    </div>

    <!-- 订单表格 -->
    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div class="cell-user">
            <div>{{ record.nickname || '-' }}</div>
            <div class="cell-user-sub">{{ record.email || '-' }}</div>
          </div>
        </template>
        <template v-else-if="column.key === 'planKey'">
          <a-tag :color="planColor(record.planKey)">{{ record.planName }}</a-tag>
        </template>
        <template v-else-if="column.key === 'amount'">
          ¥{{ record.amount }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ record.statusName }}</a-tag>
        </template>
        <template v-else-if="column.key === 'paidAt'">
          {{ record.paidAt ? formatTime(record.paidAt) : '-' }}
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTime(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button v-if="record.status === 0" type="link" size="small" @click="handleMarkPaid(record)">标记已支付</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleCancel(record)">取消</a-button>
            <a-button v-if="record.status === 1" type="link" size="small" danger @click="openRefundModal(record)">退款</a-button>
            <a-button type="link" size="small" @click="openDetailDrawer(record)">详情</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 退款弹框 -->
    <a-modal v-model:open="refundModalOpen" title="退款" :confirm-loading="refunding" @ok="submitRefund">
      <div style="height: 120px; overflow-y: auto;">
        <p>订单号：{{ refundTarget?.orderNo }}</p>
        <p>金额：¥{{ refundTarget?.amount }}</p>
        <a-textarea v-model:value="refundReason" placeholder="请输入退款原因" :rows="3" style="margin-top: 8px" />
      </div>
    </a-modal>

    <!-- 手动发放会员弹框 -->
    <a-modal v-model:open="grantModalOpen" title="手动发放会员" :confirm-loading="granting" @ok="submitGrant">
      <div style="height: 260px; overflow-y: auto;">
        <a-form layout="vertical">
          <a-form-item label="用户ID" required>
            <a-input-number v-model:value="grantForm.userId" placeholder="输入用户ID" style="width: 100%" :min="1" />
          </a-form-item>
          <a-form-item label="套餐" required>
            <a-select v-model:value="grantForm.planKey" placeholder="选择套餐" style="width: 100%">
              <a-select-option value="basic">基础版</a-select-option>
              <a-select-option value="pro">专业版</a-select-option>
              <a-select-option value="flagship">旗舰版</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="周期" required>
            <a-select v-model:value="grantForm.cycle" placeholder="选择周期" style="width: 100%">
              <a-select-option value="month">月付（30天）</a-select-option>
              <a-select-option value="quarter">季付（90天）</a-select-option>
              <a-select-option value="year">年付（365天）</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="grantForm.remark" placeholder="选填" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 手动调整会员弹框 -->
    <a-modal v-model:open="adjustModalOpen" title="手动调整会员" :confirm-loading="adjusting" @ok="submitAdjust">
      <div style="height: 220px; overflow-y: auto;">
        <a-form layout="vertical">
          <a-form-item label="用户ID" required>
            <a-input-number v-model:value="adjustForm.userId" placeholder="输入用户ID" style="width: 100%" :min="1" />
          </a-form-item>
          <a-form-item label="会员等级" required>
            <a-select v-model:value="adjustForm.level" placeholder="选择等级" style="width: 100%">
              <a-select-option value="basic">基础版</a-select-option>
              <a-select-option value="pro">专业版</a-select-option>
              <a-select-option value="flagship">旗舰版</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="到期日期" required>
            <a-date-picker v-model:value="adjustForm.expiresAt" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="adjustForm.remark" placeholder="选填" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 订单详情抽屉 -->
    <a-drawer v-model:open="detailDrawerOpen" title="订单详情" :width="480">
      <template v-if="detailData">
        <div class="detail-row"><span class="detail-label">订单号</span><span>{{ detailData.orderNo }}</span></div>
        <div class="detail-row"><span class="detail-label">用户</span><span>{{ detailData.nickname || '-' }} ({{ detailData.email || '-' }})</span></div>
        <div class="detail-row"><span class="detail-label">套餐</span><span>{{ detailData.planName }}</span></div>
        <div class="detail-row"><span class="detail-label">周期</span><span>{{ detailData.cycleName }}</span></div>
        <div class="detail-row"><span class="detail-label">金额</span><span>¥{{ detailData.amount }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span>
          <a-tag :color="statusColor(detailData.status)">{{ detailData.statusName }}</a-tag>
        </div>
        <div class="detail-row"><span class="detail-label">支付时间</span><span>{{ detailData.paidAt ? formatTime(detailData.paidAt) : '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">退款时间</span><span>{{ detailData.refundedAt ? formatTime(detailData.refundedAt) : '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">退款原因</span><span>{{ detailData.refundReason || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">备注</span><span>{{ detailData.adminRemark || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">创建时间</span><span>{{ formatTime(detailData.createdAt) }}</span></div>
      </template>
    </a-drawer>
  </a-card>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getOrderList, getOrderDetail, markOrderPaid, refundOrder, cancelOrder, adjustMembership, grantMembership } from '@/api/order.js'

// ── 搜索 & 列表 ──
const keyword = ref('')
const planKey = ref(undefined)
const statusFilter = ref(undefined)
const dateRange = ref(null)
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '用户', key: 'user', width: 160 },
  { title: '套餐', key: 'planKey', width: 100 },
  { title: '周期', dataIndex: 'cycleName', key: 'cycleName', width: 80 },
  { title: '金额', key: 'amount', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '支付时间', key: 'paidAt', width: 160 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

function planColor(key) {
  return { basic: 'blue', pro: 'green', flagship: 'gold' }[key] || 'default'
}

function statusColor(s) {
  return { 0: 'orange', 1: 'green', 2: 'red', 3: 'default' }[s] || 'default'
}

async function reload() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (planKey.value) params.planKey = planKey.value
    if (statusFilter.value !== undefined) params.status = statusFilter.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const data = await getOrderList(params)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  reload()
}

function handleReset() {
  keyword.value = ''
  planKey.value = undefined
  statusFilter.value = undefined
  dateRange.value = null
  page.value = 1
  reload()
}

function onTableChange(p) {
  page.value = p.current
  pageSize.value = p.pageSize
  reload()
}

// ── 标记已支付 ──
function handleMarkPaid(record) {
  Modal.confirm({
    title: '确认标记已支付？',
    content: `订单号：${record.orderNo}，金额：¥${record.amount}。标记后将激活/延长用户会员。`,
    async onOk() {
      await markOrderPaid(record.id)
      message.success('已标记为已支付')
      reload()
    }
  })
}

// ── 取消订单 ──
function handleCancel(record) {
  Modal.confirm({
    title: '确认取消订单？',
    content: `订单号：${record.orderNo}`,
    async onOk() {
      await cancelOrder(record.id)
      message.success('已取消')
      reload()
    }
  })
}

// ── 退款 ──
const refundModalOpen = ref(false)
const refundTarget = ref(null)
const refundReason = ref('')
const refunding = ref(false)

function openRefundModal(record) {
  refundTarget.value = record
  refundReason.value = ''
  refundModalOpen.value = true
}

async function submitRefund() {
  if (!refundReason.value.trim()) {
    message.warning('请输入退款原因')
    return
  }
  refunding.value = true
  try {
    await refundOrder(refundTarget.value.id, { reason: refundReason.value })
    message.success('退款成功')
    refundModalOpen.value = false
    reload()
  } catch (e) {
    // handled
  } finally {
    refunding.value = false
  }
}

// ── 手动发放会员 ──
const grantModalOpen = ref(false)
const granting = ref(false)
const grantForm = reactive({ userId: null, planKey: undefined, cycle: undefined, remark: '' })

function openGrantModal() {
  grantForm.userId = null
  grantForm.planKey = undefined
  grantForm.cycle = undefined
  grantForm.remark = ''
  grantModalOpen.value = true
}

async function submitGrant() {
  if (!grantForm.userId || !grantForm.planKey || !grantForm.cycle) {
    message.warning('请填写完整信息')
    return
  }
  granting.value = true
  try {
    await grantMembership({ ...grantForm })
    message.success('发放成功')
    grantModalOpen.value = false
    reload()
  } catch (e) {
    // handled
  } finally {
    granting.value = false
  }
}

// ── 手动调整会员 ──
const adjustModalOpen = ref(false)
const adjusting = ref(false)
const adjustForm = reactive({ userId: null, level: undefined, expiresAt: null, remark: '' })

function openAdjustModal() {
  adjustForm.userId = null
  adjustForm.level = undefined
  adjustForm.expiresAt = null
  adjustForm.remark = ''
  adjustModalOpen.value = true
}

async function submitAdjust() {
  if (!adjustForm.userId || !adjustForm.level || !adjustForm.expiresAt) {
    message.warning('请填写完整信息')
    return
  }
  adjusting.value = true
  try {
    const payload = {
      userId: adjustForm.userId,
      level: adjustForm.level,
      expiresAt: adjustForm.expiresAt.format('YYYY-MM-DD'),
      remark: adjustForm.remark
    }
    await adjustMembership(payload)
    message.success('调整成功')
    adjustModalOpen.value = false
    reload()
  } catch (e) {
    // handled
  } finally {
    adjusting.value = false
  }
}

// ── 详情抽屉 ──
const detailDrawerOpen = ref(false)
const detailData = ref(null)

async function openDetailDrawer(record) {
  try {
    detailData.value = await getOrderDetail(record.id)
    detailDrawerOpen.value = true
  } catch (e) {
    // handled
  }
}

onMounted(reload)
</script>

<style scoped>
.order-admin {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.page-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 4px 0 0;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.cell-user {
  line-height: 1.4;
}

.cell-user-sub {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  min-width: 80px;
  color: #8c8c8c;
  flex-shrink: 0;
}
</style>
```

- [ ] **Step 2: 验证前端编译**

```bash
cd project/admin/web && npx vite build --mode development 2>&1 | tail -5
```
Expected: 构建成功，无报错

- [ ] **Step 3: Commit**

```bash
git add project/admin/web/src/views/OrderListView.vue
git commit -m "feat(admin-web): 订单列表页 — 搜索/操作/详情/手动发放/调整会员"
```

---

### Task 7: 前端数据统计页 — OrderStatsView.vue

**Files:**
- Modify: `project/admin/web/package.json` — 添加 echarts + vue-echarts 依赖
- Create: `project/admin/web/src/views/OrderStatsView.vue`

**Interfaces:**
- Consumes: `getOrderStatsOverview`, `getOrderTrend`, `getPlanDistribution` (from Task 5)
- Produces: 数据统计页面，路由 `/console/orders/stats`

- [ ] **Step 1: 安装 ECharts 依赖**

```bash
cd project/admin/web && npm install echarts vue-echarts
```

- [ ] **Step 2: 创建 OrderStatsView.vue**

```vue
<template>
  <div class="order-stats">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <a-card v-for="card in cards" :key="card.label" :bordered="false" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.prefix }}{{ card.value }}</div>
      </a-card>
    </div>

    <!-- 收入趋势 -->
    <a-card :bordered="false" class="chart-card">
      <div class="chart-header">
        <h4 class="chart-title">收入趋势</h4>
        <a-radio-group v-model:value="trendDays" size="small" @change="loadTrend">
          <a-radio-button :value="7">近7天</a-radio-button>
          <a-radio-button :value="30">近30天</a-radio-button>
        </a-radio-group>
      </div>
      <v-chart :option="trendOption" style="height: 320px" autoresize />
    </a-card>

    <!-- 分布图 -->
    <div class="dist-row">
      <a-card :bordered="false" class="chart-card dist-card">
        <h4 class="chart-title">套餐分布</h4>
        <v-chart :option="planPieOption" style="height: 280px" autoresize />
      </a-card>
      <a-card :bordered="false" class="chart-card dist-card">
        <h4 class="chart-title">周期分布</h4>
        <v-chart :option="cyclePieOption" style="height: 280px" autoresize />
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { getOrderStatsOverview, getOrderTrend, getPlanDistribution } from '@/api/order.js'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

// ── 统计卡片 ──
const overview = ref(null)

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '今日订单', value: o.todayOrderCount ?? 0, prefix: '' },
    { label: '今日收入', value: o.todayRevenue ?? '0.00', prefix: '¥' },
    { label: '本月订单', value: o.monthOrderCount ?? 0, prefix: '' },
    { label: '本月收入', value: o.monthRevenue ?? '0.00', prefix: '¥' },
    { label: '累计订单', value: o.totalOrderCount ?? 0, prefix: '' },
    { label: '累计收入', value: o.totalRevenue ?? '0.00', prefix: '¥' }
  ]
})

// ── 收入趋势 ──
const trendDays = ref(7)
const trendData = ref(null)

const trendOption = computed(() => {
  const d = trendData.value || { dates: [], revenues: [], orderCounts: [] }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '订单数'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: d.dates },
    yAxis: [
      { type: 'value', name: '收入(元)', position: 'left' },
      { type: 'value', name: '订单数', position: 'right' }
    ],
    series: [
      {
        name: '收入',
        type: 'line',
        data: d.revenues,
        smooth: true,
        itemStyle: { color: '#07c160' },
        areaStyle: { color: 'rgba(7,193,96,0.1)' }
      },
      {
        name: '订单数',
        type: 'line',
        yAxisIndex: 1,
        data: d.orderCounts,
        smooth: true,
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#1890ff' }
      }
    ]
  }
})

// ── 分布饼图 ──
const distData = ref(null)

const planPieOption = computed(() => {
  const plans = distData.value?.plans || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: plans.map(p => ({ name: p.planName, value: p.count })),
      label: { formatter: '{b}\n{d}%' }
    }]
  }
})

const cyclePieOption = computed(() => {
  const cycles = distData.value?.cycles || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: cycles.map(c => ({ name: c.cycleName, value: c.count })),
      label: { formatter: '{b}\n{d}%' }
    }]
  }
})

// ── 数据加载 ──
async function loadOverview() {
  try {
    overview.value = await getOrderStatsOverview()
  } catch (e) { /* handled */ }
}

async function loadTrend() {
  try {
    trendData.value = await getOrderTrend(trendDays.value)
  } catch (e) { /* handled */ }
}

async function loadDistribution() {
  try {
    distData.value = await getPlanDistribution()
  } catch (e) { /* handled */ }
}

onMounted(() => {
  loadOverview()
  loadTrend()
  loadDistribution()
})
</script>

<style scoped>
.order-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.dist-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.dist-card {
  text-align: center;
}
</style>
```

- [ ] **Step 3: 验证前端编译**

```bash
cd project/admin/web && npx vite build --mode development 2>&1 | tail -5
```
Expected: 构建成功，无报错

- [ ] **Step 4: Commit**

```bash
git add project/admin/web/package.json project/admin/web/package-lock.json project/admin/web/src/views/OrderStatsView.vue
git commit -m "feat(admin-web): 数据统计页 — 概览卡片 + ECharts 趋势/分布图"
```

---

## 自审

### Spec coverage
- [x] 菜单结构（侧边栏 + 路由） → Task 5
- [x] 数据库变更（refunded_at, refund_reason, admin_remark, operator_id, status 扩展） → Task 1
- [x] API: 订单列表（筛选+分页） → Task 2 (mapper/XML) + Task 3 (service) + Task 4 (controller)
- [x] API: 订单详情 → Task 2-4
- [x] API: 标记已支付 → Task 3 (service 含会员激活) + Task 4
- [x] API: 退款（含会员回退） → Task 3 + Task 4
- [x] API: 取消订单 → Task 3 + Task 4
- [x] API: 手动调整会员 → Task 3 + Task 4
- [x] API: 手动发放会员（0元订单） → Task 3 + Task 4
- [x] API: 统计概览/趋势/套餐分布 → Task 2 (mapper XML) + Task 3 + Task 4
- [x] 前端：订单列表页（搜索+表格+操作+弹框+抽屉） → Task 6
- [x] 前端：数据统计页（卡片+折线图+饼图） → Task 7
- [x] 错误码 117xxx → Task 2
- [x] 弹框固定高度 → Task 6 (所有 modal 内容区写死 height + overflow-y: auto)

### Placeholder scan
- 无 TBD/TODO/"implement later"
- 所有代码步骤包含完整代码
- 所有命令包含预期输出

### Type consistency
- `AdminOrderMapper` 方法签名在 Task 2 定义，Task 3 service 中一致使用
- `AdminMembershipMapper` 方法签名在 Task 2 定义，Task 3 service 中一致使用
- VO 字段名在 Task 2 定义，Task 3 service 转换方法中一致
- 前端 API 函数名在 Task 5 定义，Task 6/7 视图中一致引用
- `OrderStatus` enum 在 Task 2 定义，Task 3 service 中一致使用
