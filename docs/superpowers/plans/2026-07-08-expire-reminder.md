# 到期提醒 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在管理端实现"到期提醒"模块，含配置中心、定时任务、单用户手动提醒，以及把"用户管理"重构为一级菜单。

**Architecture:** admin-api 新增 `modules/reminder` 子模块，复用 hotsearch 的 `ThreadPoolTaskScheduler` + DB 单行配置 + 变更事件重排模式。通知下发走"直接读写共享库"：站内信 → admin-api 直接 `INSERT u_message`；邮件 → admin-api 新增 `spring-boot-starter-mail` + `JavaMailSender`，发到用户邮箱。前端用 Ant Design Vue 新建 `ExpireReminderView`，菜单用 `a-sub-menu`。

**Tech Stack:** Spring Boot 3.x + MyBatis-Plus + JDK 17 + Flyway（admin-api 走 `V2.0.0_xxx`）+ ThreadPoolTaskScheduler + JavaMailSender + Vue 3 + Ant Design Vue。

---

## Global Constraints

- **到期时间存储约定**：`membership_expire_at` 存**到期日次日 00:00**（如 7/10 24:00 结束 → 存 `2026-07-11 00:00:00`）。`lastValidDate = expireAt.toLocalDate().minusDays(1)`，`remainingDays = ChronoUnit.DAYS.between(today, lastValidDate)`。
- **命中区间**：`0 ≤ remainingDays ≤ N`，`N` 取自配置。剩余 0 天属于最紧急，必须提醒；剩余 < 0（已过期）不提醒。
- **去重**：`u_reminder_send_log` 唯一键 `(user_id, channel, send_date)`；发送前查当天该用户该渠道是否已有 `status=1` 记录，有则跳过。`message_email` 配置会产生 message、email 两条 log。
- **bizNo 生成**：单用户提醒 `bizNo = "RMD-" + UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase()`，对应 1 条 u_message。
- **错误码段**：26xxxx（hot-search 是 25xxxx，earnings 是后续段）。
- **包路径**：admin-api 下 `com.aichuangzuo.admin.modules.reminder.{entity,mapper,properties,config,event,service,service.impl,job,controller,dto.request,vo,enums,scheduler}`。
- **定时任务**：禁止 `@Scheduled`；统一 `ThreadPoolTaskScheduler` + `CronTrigger` + `@PostConstruct reschedule()` + `@EventListener` 重排。
- **权限**：所有 controller 接口走 `checkSuperAdmin()` 校验。
- **配置默认值**：通过 `ReminderProperties`（绑 `reminder.*`）→ `syncFromProperties()` 在 `@PostConstruct` 时写一行默认配置（id=1）。
- **MAIL 配置**：admin-api `application.yml` 复用 user-api 的 `SPRING_MAIL_*` 环境变量；新增 `spring-boot-starter-mail` 依赖。
- **前端权限**：菜单 + 路由 + 页面（无新角色控制）。

---

## Task File Map

| Task | Create / Modify | Responsibility |
|------|----------------|----------------|
| 1 | 3 SQL migrations | u_user 加 membership_expire_at；建 reminder_config；建 u_reminder_send_log |
| 2 | PlatformUser / AdminUserVO / AdminUserService 接口 / Impl / Controller / DTO | 注册用户页可见/可改会员到期时间 |
| 3 | Reminder entity×2 / Mapper×2 / Properties / SchedulerConfig | reminder 模块骨架 |
| 4 | ErrorCode / Event / Service 接口+Impl / Controller / DTO / VO | reminder_config 读写 + 事件 |
| 5 | Message 实体（admin-api 侧）/ Mapper / ReminderMailService | 通知下发：站内信 + 邮件 |
| 6 | ExpireReminderService 接口+Impl + 测试类 | 剩余天数算法 + 列表 + 单用户提醒 + 去重（TDD） |
| 7 | ExpireReminderJob | cron 任务 + 事件重排 + 调用 service |
| 8 | router / AdminLayout / api/expireReminder.js / ExpireReminderView.vue | 菜单重构 + 到期提醒页 |
| 9 | user.js / UserListView.vue | 注册用户页会员到期展示+编辑 |

---

## Task 1: 数据库迁移（3 个 SQL）

**Files:**
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_005__add_membership_expire_at_to_user.sql`
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_006__create_reminder_config_table.sql`
- Create: `project/admin/api/src/main/resources/db/migration/V2.0.0_007__create_reminder_send_log_table.sql`

**Interfaces:** 产生 `u_user.membership_expire_at`、`reminder_config`、`u_reminder_send_log` 三张表，admin-api 启动 Flyway 会自动跑。

- [ ] **Step 1: 创建 V2.0.0_005（u_user 加到期字段）**

```sql
-- 管理端-到期提醒：u_user 增加会员到期字段
-- 存"到期日次日 00:00"（如 7/10 24:00 结束 → 2026-07-11 00:00:00），NULL=非会员。

ALTER TABLE u_user
    ADD COLUMN membership_expire_at DATETIME NULL COMMENT '会员到期时刻（到期日次日00:00，NULL=非会员）' AFTER user_type;

CREATE INDEX idx_user_membership_expire_at ON u_user (membership_expire_at);
```

- [ ] **Step 2: 创建 V2.0.0_006（reminder_config 单行配置）**

```sql
-- 管理端-到期提醒：单行配置表（id=1）
-- 提前天数 N、每天提醒时间点 0-23、通知形式 message/email/message_email、开关。

CREATE TABLE IF NOT EXISTS reminder_config (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    advance_days INT NOT NULL DEFAULT 7 COMMENT '提前提醒天数 N',
    notify_hour TINYINT UNSIGNED NOT NULL DEFAULT 9 COMMENT '每天提醒时间点 0-23',
    notify_channel VARCHAR(16) NOT NULL DEFAULT 'message' COMMENT 'message/email/message_email',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '定时提醒开关',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到期提醒配置';

INSERT IGNORE INTO reminder_config (id, advance_days, notify_hour, notify_channel, enabled)
VALUES (1, 7, 9, 'message', 1);
```

- [ ] **Step 3: 创建 V2.0.0_007（u_reminder_send_log）**

```sql
-- 管理端-到期提醒：发送记录 + 去重
-- 唯一键 (user_id, channel, send_date) 保证同日同渠道只成功发一次。

CREATE TABLE IF NOT EXISTS u_reminder_send_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '被提醒用户',
    channel VARCHAR(16) NOT NULL COMMENT 'message/email',
    send_date DATE NOT NULL COMMENT '发送日期（本地日）',
    remaining_days INT NOT NULL COMMENT '发送时剩余天数',
    trigger_type VARCHAR(8) NOT NULL COMMENT 'auto/manual',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1-成功 0-失败',
    fail_reason VARCHAR(256) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_reminder_user_channel_date (user_id, channel, send_date),
    KEY idx_reminder_send_date (send_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到期提醒发送记录';
```

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/resources/db/migration/V2.0.0_005__add_membership_expire_at_to_user.sql \
        project/admin/api/src/main/resources/db/migration/V2.0.0_006__create_reminder_config_table.sql \
        project/admin/api/src/main/resources/db/migration/V2.0.0_007__create_reminder_send_log_table.sql
git commit -m "feat(reminder): 新增到期提醒模块的 3 个数据库迁移"
```

---

## Task 2: 用户会员到期字段 + 编辑接口

**Files:**
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/entity/PlatformUser.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/vo/AdminUserVO.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/AdminUserService.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/service/impl/AdminUserServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/dto/request/AdminUserMembershipRequest.java`
- Modify: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/controller/AdminUserController.java`

**Interfaces:**
- Consumes: 前端 `/console/users` 详情 drawer；前端 `PATCH /api/v1/admin/users/{id}/membership`
- Produces:
  - `AdminUserVO.membershipExpireAt: LocalDateTime`
  - `AdminUserService.updateMembership(Long id, LocalDate expireDate, Long updatedBy)`：把前端选的"到期日"换算成"次日 00:00"存储，传 null/清空时把字段置 NULL

- [ ] **Step 1: 修改 PlatformUser 实体加字段**

在 `PlatformUser.java` 的 `private Integer userType;` 后新增一行：

```java
    private LocalDateTime membershipExpireAt;
```

注意已 `import java.time.LocalDateTime;`，无需新增 import。

- [ ] **Step 2: 修改 AdminUserVO 加字段**

```java
    private LocalDateTime membershipExpireAt;
```

- [ ] **Step 3: 创建 AdminUserMembershipRequest DTO**

`project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/dto/request/AdminUserMembershipRequest.java`：

```java
package com.aichuangzuo.admin.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端修改用户会员到期时间。
 * expireDate 为 null 表示清空（非会员）；否则取"到期日"，后端会自动转成"次日 00:00"存储。
 */
@Data
public class AdminUserMembershipRequest {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;
}
```

- [ ] **Step 4: 在 AdminUserService 接口增加方法**

```java
    void updateMembership(Long id, LocalDate expireDate, Long updatedBy);
```

（`import java.time.LocalDate;`）

- [ ] **Step 5: 在 AdminUserServiceImpl 实现 updateMembership 并在 toAdminUserVO 填字段**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMembership(Long id, LocalDate expireDate, Long updatedBy) {
        PlatformUser user = platformUserMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        user.setMembershipExpireAt(expireDate == null ? null : expireDate.plusDays(1).atStartOfDay());
        platformUserMapper.updateById(user);
    }
```

并在 `toAdminUserVO` 中追加一行：

```java
        vo.setMembershipExpireAt(user.getMembershipExpireAt());
```

（`import java.time.LocalDate;` 加到 import 块）

- [ ] **Step 6: 在 AdminUserController 增加 PATCH 接口**

```java
    @Operation(summary = "设置会员到期时间（null=非会员）")
    @PatchMapping("/{id}/membership")
    public Result<Void> updateMembership(@PathVariable(name = "id") Long id,
                                          @RequestBody AdminUserMembershipRequest request) {
        checkSuperAdmin();
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        adminUserService.updateMembership(id, request.getExpireDate(), adminId);
        return Result.success();
    }
```

- [ ] **Step 7: 启动 admin-api 验证 Flyway**

```bash
cd project/admin/api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

访问 `http://localhost:26060/api/v1/admin/users/1`（需先登录拿到 token）。预期：返回的 JSON 多出 `membershipExpireAt` 字段，初始为 null。

然后 `PATCH /api/v1/admin/users/1/membership` body `{"expireDate":"2026-07-10"}`，预期返回成功，再次 GET 应为 `"2026-07-11T00:00:00"`。

停掉服务。

- [ ] **Step 8: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/user/
git commit -m "feat(user): u_user 增加 membership_expire_at 字段及管理端编辑接口"
```

---

## Task 3: reminder 模块骨架（entity / mapper / properties / scheduler config）

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/entity/ReminderConfig.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/entity/ReminderSendLog.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/mapper/ReminderConfigMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/mapper/ReminderSendLogMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/properties/ReminderProperties.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/config/ReminderSchedulerConfig.java`

**Interfaces:**
- Produces:
  - `ReminderConfigMapper extends BaseMapper<ReminderConfig>`
  - `ReminderSendLogMapper extends BaseMapper<ReminderSendLog>`
  - `ThreadPoolTaskScheduler reminderTaskScheduler()` Bean（poolSize=2, namePrefix=`reminder-`）

- [ ] **Step 1: 创建 ReminderConfig 实体**

```java
package com.aichuangzuo.admin.modules.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 到期提醒配置，对应表 reminder_config。
 * 单行表，固定 id=1。
 */
@Getter
@Setter
@TableName("reminder_config")
public class ReminderConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 提前提醒天数 N（命中区间：0 ≤ remainingDays ≤ N）。 */
    private Integer advanceDays;

    /** 每天提醒时间点 0-23。 */
    private Integer notifyHour;

    /** message / email / message_email。 */
    private String notifyChannel;

    /** 0-关 1-开。 */
    private Integer enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
```

- [ ] **Step 2: 创建 ReminderSendLog 实体**

```java
package com.aichuangzuo.admin.modules.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 到期提醒发送记录，对应表 u_reminder_send_log。
 * 唯一键 (user_id, channel, send_date) 负责去重。
 */
@Getter
@Setter
@TableName("u_reminder_send_log")
public class ReminderSendLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String channel;
    private LocalDate sendDate;
    private Integer remainingDays;
    private String triggerType;
    private Integer status;
    private String failReason;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Mapper 接口**

`ReminderConfigMapper.java`：

```java
package com.aichuangzuo.admin.modules.reminder.mapper;

import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderConfigMapper extends BaseMapper<ReminderConfig> {
}
```

`ReminderSendLogMapper.java`：

```java
package com.aichuangzuo.admin.modules.reminder.mapper;

import com.aichuangzuo.admin.modules.reminder.entity.ReminderSendLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderSendLogMapper extends BaseMapper<ReminderSendLog> {
}
```

- [ ] **Step 4: 创建 ReminderProperties**

```java
package com.aichuangzuo.admin.modules.reminder.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 到期提醒配置默认值，启动时若 DB 不存在则同步进来。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reminder")
public class ReminderProperties {

    /** 提前提醒天数 N。 */
    private Integer advanceDays = 7;

    /** 每天提醒时间点 0-23。 */
    private Integer notifyHour = 9;

    /** message / email / message_email。 */
    private String notifyChannel = "message";

    /** 定时提醒开关。 */
    private boolean enabled = true;
}
```

- [ ] **Step 5: 在 application.yml 增加 reminder 默认值段**

在 `project/admin/api/src/main/resources/application.yml` 末尾追加：

```yaml
reminder:
  enabled: true
  advance-days: 7
  notify-hour: 9
  notify-channel: message
```

- [ ] **Step 6: 创建 ReminderSchedulerConfig**

```java
package com.aichuangzuo.admin.modules.reminder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ReminderSchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler reminderTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("reminder-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
cd project/admin/api && mvn -q compile
```

预期 BUILD SUCCESS，无编译错误。

- [ ] **Step 8: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/ \
        project/admin/api/src/main/resources/application.yml
git commit -m "feat(reminder): reminder 模块骨架（entity/mapper/properties/scheduler）"
```

---

## Task 4: 错误码 + 配置变更事件 + ReminderConfigService + Controller

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/enums/AdminReminderErrorCode.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/event/ReminderConfigChangedEvent.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/dto/request/ReminderConfigRequest.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/service/ReminderConfigService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/service/impl/ReminderConfigServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/controller/ReminderConfigController.java`

**Interfaces:**
- Consumes: Task 3 的 Mapper / Properties
- Produces:
  - `ReminderConfigService.getConfig()` / `saveConfig(req, updatedBy)` / `syncFromProperties()`
  - `ReminderConfigController`：`GET /api/v1/admin/expire-reminder/config`、`PUT /api/v1/admin/expire-reminder/config`

- [ ] **Step 1: 创建 AdminReminderErrorCode**

```java
package com.aichuangzuo.admin.modules.reminder.enums;

import com.aichuangzuo.shared.result.ErrorCode;
import lombok.Getter;

/**
 * 到期提醒模块错误码，段位 26xxxx。
 */
@Getter
public enum AdminReminderErrorCode implements ErrorCode {

    CONFIG_NOT_FOUND(260001, "提醒配置不存在"),
    INVALID_NOTIFY_HOUR(260002, "提醒时间点非法（0-23）"),
    INVALID_NOTIFY_CHANNEL(260003, "通知形式非法（message/email/message_email）"),
    INVALID_ADVANCE_DAYS(260004, "提前天数非法（1-90）"),
    TARGET_USER_NOT_FOUND(260005, "目标用户不存在或无会员到期时间");

    private final int code;
    private final String message;

    AdminReminderErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: 创建事件 record**

```java
package com.aichuangzuo.admin.modules.reminder.event;

/**
 * 提醒配置变更事件。
 * 由 ReminderConfigServiceImpl 在 saveConfig 后发布，
 * ExpireReminderJob 监听并 reschedule。
 */
public record ReminderConfigChangedEvent(Long adminId) {
}
```

- [ ] **Step 3: 创建 ReminderConfigRequest DTO**

```java
package com.aichuangzuo.admin.modules.reminder.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReminderConfigRequest {
    @NotNull(message = "提前天数不能为空")
    private Integer advanceDays;

    @NotNull(message = "提醒时间点不能为空")
    private Integer notifyHour;

    @NotNull(message = "通知形式不能为空")
    @Pattern(regexp = "message|email|message_email", message = "通知形式非法")
    private String notifyChannel;

    @NotNull(message = "开关不能为空")
    private Integer enabled;
}
```

- [ ] **Step 4: 创建 ReminderConfigService 接口**

```java
package com.aichuangzuo.admin.modules.reminder.service;

import com.aichuangzuo.admin.modules.reminder.dto.request.ReminderConfigRequest;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;

public interface ReminderConfigService {
    ReminderConfig getConfig();
    ReminderConfig saveConfig(ReminderConfigRequest request, Long updatedBy);
    void syncFromProperties();
}
```

- [ ] **Step 5: 创建 ReminderConfigServiceImpl**

```java
package com.aichuangzuo.admin.modules.reminder.service.impl;

import com.aichuangzuo.admin.modules.reminder.dto.request.ReminderConfigRequest;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.aichuangzuo.admin.modules.reminder.enums.AdminReminderErrorCode;
import com.aichuangzuo.admin.modules.reminder.event.ReminderConfigChangedEvent;
import com.aichuangzuo.admin.modules.reminder.mapper.ReminderConfigMapper;
import com.aichuangzuo.admin.modules.reminder.properties.ReminderProperties;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderConfigServiceImpl implements ReminderConfigService {

    private static final long CONFIG_ID = 1L;

    private final ReminderConfigMapper configMapper;
    private final ReminderProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ReminderConfig getConfig() {
        ReminderConfig cfg = configMapper.selectById(CONFIG_ID);
        if (cfg == null) {
            throw new BusinessException(AdminReminderErrorCode.CONFIG_NOT_FOUND);
        }
        return cfg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReminderConfig saveConfig(ReminderConfigRequest req, Long updatedBy) {
        validate(req);

        ReminderConfig existing = configMapper.selectById(CONFIG_ID);
        ReminderConfig entity = existing == null ? new ReminderConfig() : existing;
        entity.setId(CONFIG_ID);
        entity.setAdvanceDays(req.getAdvanceDays());
        entity.setNotifyHour(req.getNotifyHour());
        entity.setNotifyChannel(req.getNotifyChannel());
        entity.setEnabled(req.getEnabled());
        entity.setUpdatedBy(updatedBy == null ? 0L : updatedBy);

        if (existing == null) {
            entity.setCreatedAt(LocalDateTime.now());
            configMapper.insert(entity);
        } else {
            configMapper.updateById(entity);
        }

        eventPublisher.publishEvent(new ReminderConfigChangedEvent(updatedBy));
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromProperties() {
        ReminderConfig existing = configMapper.selectById(CONFIG_ID);
        if (existing != null) {
            return;
        }
        ReminderConfig entity = new ReminderConfig();
        entity.setId(CONFIG_ID);
        entity.setAdvanceDays(properties.getAdvanceDays());
        entity.setNotifyHour(properties.getNotifyHour());
        entity.setNotifyChannel(properties.getNotifyChannel());
        entity.setEnabled(properties.isEnabled() ? 1 : 0);
        entity.setUpdatedBy(0L);
        configMapper.insert(entity);
        log.info("到期提醒配置默认值已写入 DB");
    }

    private void validate(ReminderConfigRequest req) {
        if (req.getAdvanceDays() == null || req.getAdvanceDays() < 1 || req.getAdvanceDays() > 90) {
            throw new BusinessException(AdminReminderErrorCode.INVALID_ADVANCE_DAYS);
        }
        if (req.getNotifyHour() == null || req.getNotifyHour() < 0 || req.getNotifyHour() > 23) {
            throw new BusinessException(AdminReminderErrorCode.INVALID_NOTIFY_HOUR);
        }
        // notifyChannel 已用 @Pattern 校验
    }
}
```

- [ ] **Step 6: 创建 ReminderConfigController**

```java
package com.aichuangzuo.admin.modules.reminder.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.reminder.dto.request.ReminderConfigRequest;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端到期提醒")
@RestController
@RequestMapping("/api/v1/admin/expire-reminder")
@RequiredArgsConstructor
public class ReminderConfigController {

    private final ReminderConfigService configService;
    private final AdminUserPermissionService permissionService;

    @GetMapping("/config")
    public Result<ReminderConfig> getConfig() {
        checkSuperAdmin();
        return Result.success(configService.getConfig());
    }

    @PutMapping("/config")
    public Result<ReminderConfig> saveConfig(@Valid @RequestBody ReminderConfigRequest request) {
        checkSuperAdmin();
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(configService.saveConfig(request, adminId));
    }

    private void checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
```

- [ ] **Step 7: 编译验证**

```bash
cd project/admin/api && mvn -q compile
```

预期 BUILD SUCCESS。

- [ ] **Step 8: 启动 admin-api 验证默认配置落库**

```bash
cd project/admin/api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后查看日志应包含 `到期提醒配置默认值已写入 DB`。访问 `GET /api/v1/admin/expire-reminder/config`，应返回 `{advanceDays:7, notifyHour:9, notifyChannel:"message", enabled:1, ...}`。

`PUT /config` body `{"advanceDays":3,"notifyHour":10,"notifyChannel":"message_email","enabled":1}`，预期返回更新后的实体并触发日志 `收到配置变更事件`。停服务。

- [ ] **Step 9: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/
git commit -m "feat(reminder): 配置读写 + 事件 + 校验 + 错误码"
```

---

## Task 5: 通知下发层（admin-api 侧站内信 + 邮件）

**Files:**
- Modify: `project/admin/api/pom.xml`
- Modify: `project/admin/api/src/main/resources/application.yml`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/entity/Message.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/mapper/MessageMapper.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/service/ReminderMailService.java`

**Interfaces:**
- Consumes: UserMessage 已有 `bizNo, msgType, scope, targetUserId, title, summary, content, subType, linkUrl` 等字段
- Produces:
  - `ReminderMailService.send(String toEmail, String subject, String text)`：纯文本邮件，使用 `JavaMailSender`
  - admin-api 侧 `Message` 实体（不重名 user-api 的 `Message`；包路径在 reminder 内，只用于 insert）

- [ ] **Step 1: pom.xml 新增 mail starter**

在 `<dependency>` 列表（caffeine 之后）新增：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
```

- [ ] **Step 2: application.yml 补 spring.mail 配置段**

在 `spring:` 下追加（与 datasource 平级）：

```yaml
  mail:
    host: ${SPRING_MAIL_HOST:127.0.0.1}
    port: ${SPRING_MAIL_PORT:3025}
    username: ${SPRING_MAIL_USERNAME:test@local}
    password: ${SPRING_MAIL_PASSWORD:local-dev-no-auth}
    protocol: smtp
    default-encoding: UTF-8
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
      mail.smtp.ssl.enable: false
      mail.smtp.connectiontimeout: 5000
      mail.smtp.timeout: 5000
      mail.smtp.writetimeout: 5000
```

- [ ] **Step 3: 创建 admin-api 侧 Message 实体**

```java
package com.aichuangzuo.admin.modules.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * admin-api 侧 u_message 实体（仅 insert）。
 * 不复用 user-api 的实体是为了避免 admin-api 引入 user-api 模块。
 * 注意：此实体的 @TableField(fill=...) 故意不设 createdAt/updatedAt 等，
 *       由调用方显式赋值，不依赖 SecurityContext 自动填充。
 */
@Getter
@Setter
@TableName("u_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号：单用户提醒固定 bizNo=RMD-uuid16。 */
    private String bizNo;

    /** membership / announcement / ... */
    private String msgType;

    /** 1-广播 2-个人。 */
    private Integer scope;

    private Long targetUserId;

    private String title;
    private String summary;
    private String content;
    private String subType;
    private String linkUrl;

    /** admin-api 单租户，固定 0。 */
    private Long tenantId;

    /** 0=未删除 1=已删除。 */
    private Integer isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
```

- [ ] **Step 4: 创建 MessageMapper**

```java
package com.aichuangzuo.admin.modules.reminder.mapper;

import com.aichuangzuo.admin.modules.reminder.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
```

- [ ] **Step 5: 创建 ReminderMailService**

```java
package com.aichuangzuo.admin.modules.reminder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:test@local}")
    private String mailFrom;

    /**
     * 发送纯文本邮件。失败抛出 RuntimeException，由调用方记 send_log。
     */
    public void send(String toEmail, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(toEmail);
        msg.setSubject(subject);
        msg.setText(text);
        try {
            mailSender.send(msg);
        } catch (MailException ex) {
            log.warn("提醒邮件发送失败 to={}, reason={}", toEmail, ex.getMessage());
            throw ex;
        }
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
cd project/admin/api && mvn -q compile
```

预期 BUILD SUCCESS。

- [ ] **Step 7: 提交**

```bash
git add project/admin/api/pom.xml \
        project/admin/api/src/main/resources/application.yml \
        project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/
git commit -m "feat(reminder): 通知下发层（u_message 实体 + 邮件服务）"
```

---

## Task 6: 核心 ExpireReminderService（含剩余天数算法，TDD）

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/service/ExpireReminderService.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/service/impl/ExpireReminderServiceImpl.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/vo/ExpiringUserVO.java`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/dto/request/ExpiringUserPageQuery.java`
- Create: `project/admin/api/src/test/java/com/aichuangzuo/admin/modules/reminder/service/ExpireReminderServiceTest.java`

**Interfaces:**
- Consumes: `PlatformUserMapper`, `ReminderConfigMapper`, `ReminderSendLogMapper`, `MessageMapper`, `ReminderMailService`, `ReminderConfigService`
- Produces:
  - `ExpiringUserVO(userId, email, nickname, membershipExpireAt, remainingDays, lastRemindedAt, lastReminderChannel)`
  - `ExpiringUserPageQuery(advanceDays, page, pageSize)`
  - `PageResult<ExpiringUserVO> pageExpiringUsers(int advanceDays, int page, int pageSize)`
  - `RemindResult remindUser(Long userId, String triggerType)`：内部按当前配置的 channel 拆 message/email 两条 log；返回 `{userId, sentChannels: List<String>, remainingDays}`

- [ ] **Step 1: 创建 ExpiringUserVO**

```java
package com.aichuangzuo.admin.modules.reminder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpiringUserVO {
    private Long userId;
    private String email;
    private String nickname;
    private LocalDateTime membershipExpireAt;
    private Integer remainingDays;
    private LocalDateTime lastRemindedAt;
    private String lastReminderChannel;
}
```

- [ ] **Step 2: 创建分页请求 DTO**

```java
package com.aichuangzuo.admin.modules.reminder.dto.request;

import lombok.Data;

@Data
public class ExpiringUserPageQuery {
    private Integer advanceDays;
    private Long page = 1L;
    private Long size = 20L;
}
```

- [ ] **Step 3: 创建 ExpireReminderService 接口**

```java
package com.aichuangzuo.admin.modules.reminder.service;

import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.vo.ExpiringUserVO;

import java.util.List;

public interface ExpireReminderService {

    PageResult pageExpiringUsers(ExpiringUserPageQuery query);

    RemindResult remindUser(Long userId, String triggerType);

    /**
     * 计算到期日（次日 00:00 存储）→ 剩余天数（不含今天）。
     * 命中区间 0 ≤ remainingDays ≤ advanceDays。
     */
    int calcRemainingDays(java.time.LocalDateTime expireAt);

    record PageResult(List<ExpiringUserVO> items, long total, long page, long size) {}

    record RemindResult(Long userId, int remainingDays, List<String> sentChannels) {}
}
```

- [ ] **Step 4: 创建测试类（TDD：先写测试）**

```java
package com.aichuangzuo.admin.modules.reminder.service;

import com.aichuangzuo.admin.modules.reminder.service.impl.ExpireReminderServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpireReminderServiceTest {

    private final ExpireReminderServiceImpl service = new ExpireReminderServiceImpl(null, null, null, null, null, null);

    @Test
    void shouldReturn2DaysWhenToday708AndExpireIsNextDay00After710() {
        // 7/10 24:00 结束 → 存 7/11 00:00。今天 7/8，应剩 2 天。
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 11, 0, 0), LocalDate.of(2026, 7, 8));
        assertEquals(2, days);
    }

    @Test
    void shouldReturn0WhenExpireIsNextDay00AfterToday() {
        // 今天 7/10 24:00 结束（存 7/11 00:00），今天 7/10，应剩 0 天。
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 11, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(0, days);
    }

    @Test
    void shouldReturnNegative1WhenAlreadyExpired() {
        // 昨天 7/9 24:00 已结束（存 7/10 00:00），今天 7/10 → 过期
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 10, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(-1, days);
    }

    @Test
    void shouldReturn5WhenNDaysAhead() {
        // 7/15 24:00 结束 → 存 7/16 00:00。今天 7/10 → 剩 6 天
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 16, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(6, days);
    }

    @Test
    void shouldReturn0WhenSameDayMorning() {
        // 7/10 0:01 开始计算 expireAt=7/11 00:00，今天 7/10，仍剩 0 天
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 11, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(0, days);
    }
}
```

注意：`calcRemainingDays` 需要接收 today 参数（便于测试时注入固定日期）。

- [ ] **Step 5: 运行测试，应该全部失败（找不到方法）**

```bash
cd project/admin/api && mvn -q test -Dtest=ExpireReminderServiceTest
```

预期：编译失败（`calcRemainingDays` 方法不存在）。

- [ ] **Step 6: 创建 ExpireReminderServiceImpl（含算法）**

```java
package com.aichuangzuo.admin.modules.reminder.service.impl;

import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.entity.Message;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderSendLog;
import com.aichuangzuo.admin.modules.reminder.enums.AdminReminderErrorCode;
import com.aichuangzuo.admin.modules.reminder.mapper.MessageMapper;
import com.aichuangzuo.admin.modules.reminder.mapper.ReminderConfigMapper;
import com.aichuangzuo.admin.modules.reminder.mapper.ReminderSendLogMapper;
import com.aichuangzuo.admin.modules.reminder.service.ExpireReminderService;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.admin.modules.reminder.service.ReminderMailService;
import com.aichuangzuo.admin.modules.reminder.vo.ExpiringUserVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireReminderServiceImpl implements ExpireReminderService {

    private final PlatformUserMapper userMapper;
    private final ReminderConfigMapper configMapper;
    private final ReminderSendLogMapper sendLogMapper;
    private final MessageMapper messageMapper;
    private final ReminderConfigService configService;
    private final ReminderMailService mailService;

    /**
     * 剩余天数（不含今天）。
     * expireAt 存的是"到期日次日 00:00" → lastValidDate = expireAt.toLocalDate() - 1 day。
     * 命中区间：0 ≤ remainingDays ≤ advanceDays。
     */
    @Override
    public int calcRemainingDays(LocalDateTime expireAt) {
        return calcRemainingDays(expireAt, LocalDate.now());
    }

    /** 同上，便于测试时注入 today。 */
    public int calcRemainingDays(LocalDateTime expireAt, LocalDate today) {
        if (expireAt == null) return Integer.MIN_VALUE;
        LocalDate lastValidDate = expireAt.toLocalDate().minusDays(1);
        return (int) ChronoUnit.DAYS.between(today, lastValidDate);
    }

    @Override
    public PageResult pageExpiringUsers(ExpiringUserPageQuery query) {
        int advanceDays = query.getAdvanceDays() == null
                ? configService.getConfig().getAdvanceDays()
                : query.getAdvanceDays();
        long pageNum = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getSize() == null || query.getSize() < 1 ? 20 : query.getSize();

        LocalDate today = LocalDate.now();
        LocalDateTime lower = today.atStartOfDay();                    // today 00:00 排除
        LocalDateTime upper = today.plusDays(advanceDays + 1L).atStartOfDay(); // 包含 lastValidDate = today+N

        // SQL 只按时间范围粗筛，再 Java 侧计算 remainingDays 排序
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(PlatformUser::getMembershipExpireAt)
                .eq(PlatformUser::getIsDeleted, 0)
                .gt(PlatformUser::getMembershipExpireAt, lower)
                .le(PlatformUser::getMembershipExpireAt, upper);

        Page<PlatformUser> page = userMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<ExpiringUserVO> items = new ArrayList<>();
        for (PlatformUser u : page.getRecords()) {
            int rd = calcRemainingDays(u.getMembershipExpireAt(), today);
            if (rd < 0 || rd > advanceDays) continue;
            ExpiringUserVO vo = new ExpiringUserVO();
            vo.setUserId(u.getId());
            vo.setEmail(u.getEmail());
            vo.setNickname(u.getNickname());
            vo.setMembershipExpireAt(u.getMembershipExpireAt());
            vo.setRemainingDays(rd);
            // 查询最近一次成功提醒
            ReminderSendLog log = sendLogMapper.selectOne(new LambdaQueryWrapper<ReminderSendLog>()
                    .eq(ReminderSendLog::getUserId, u.getId())
                    .eq(ReminderSendLog::getStatus, 1)
                    .orderByDesc(ReminderSendLog::getCreatedAt)
                    .last("LIMIT 1"));
            if (log != null) {
                vo.setLastRemindedAt(log.getCreatedAt());
                vo.setLastReminderChannel(log.getChannel());
            }
            items.add(vo);
        }
        // Java 侧按 remainingDays asc 排序（更紧急的在前）
        items.sort((a, b) -> Integer.compare(a.getRemainingDays(), b.getRemainingDays()));
        return new PageResult(items, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RemindResult remindUser(Long userId, String triggerType) {
        PlatformUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1 || user.getMembershipExpireAt() == null) {
            throw new BusinessException(AdminReminderErrorCode.TARGET_USER_NOT_FOUND);
        }

        ReminderConfig cfg = configService.getConfig();
        int remainingDays = calcRemainingDays(user.getMembershipExpireAt());
        LocalDate today = LocalDate.now();
        String bizNo = "RMD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        List<String> sentChannels = new ArrayList<>();
        String channel = cfg.getNotifyChannel();
        if ("message".equals(channel) || "message_email".equals(channel)) {
            if (sendMessage(user, remainingDays, bizNo, today, triggerType)) {
                sentChannels.add("message");
            }
        }
        if ("email".equals(channel) || "message_email".equals(channel)) {
            if (sendEmail(user, remainingDays, today, triggerType)) {
                sentChannels.add("email");
            }
        }
        return new RemindResult(userId, remainingDays, sentChannels);
    }

    private boolean sendMessage(PlatformUser user, int remainingDays, String bizNo,
                                LocalDate today, String triggerType) {
        if (alreadySent(user.getId(), "message", today)) {
            log.info("用户 {} 今日 message 已发送，跳过", user.getId());
            return false;
        }
        String title = "您的会员即将到期";
        String summary = remainingDays == 0
                ? "您的会员将于今天到期，请及时续费"
                : "您的会员将于 " + remainingDays + " 天后到期，请及时续费";
        Message msg = new Message();
        msg.setBizNo(bizNo);
        msg.setMsgType("membership");
        msg.setSubType("membership.expiring");
        msg.setScope(2);
        msg.setTargetUserId(user.getId());
        msg.setTitle(title);
        msg.setSummary(summary);
        msg.setContent(summary + "\n\n点击「我的会员」查看详情并续费。");
        msg.setLinkUrl("/me/membership");
        msg.setTenantId(0L);
        msg.setIsDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        msg.setCreatedAt(now);
        msg.setUpdatedAt(now);
        msg.setCreatedBy(0L);
        msg.setUpdatedBy(0L);
        messageMapper.insert(msg);
        recordLog(user.getId(), "message", today, remainingDays, triggerType, 1, null);
        return true;
    }

    private boolean sendEmail(PlatformUser user, int remainingDays, LocalDate today, String triggerType) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("用户 {} 无邮箱，跳过邮件提醒", user.getId());
            return false;
        }
        if (alreadySent(user.getId(), "email", today)) {
            log.info("用户 {} 今日 email 已发送，跳过", user.getId());
            return false;
        }
        String subject = "您的爱创作会员即将到期";
        String text = "您好，" + (user.getNickname() == null ? "" : user.getNickname()) + "：\n\n"
                + (remainingDays == 0
                    ? "您的会员将于今天到期，请及时续费以免影响使用。"
                    : "您的会员将于 " + remainingDays + " 天后到期，请及时续费以免影响使用。")
                + "\n\n登录爱创作：https://aichuangzuo.com\n"
                + "本邮件由系统自动发出，请勿直接回复。";
        try {
            mailService.send(user.getEmail(), subject, text);
            recordLog(user.getId(), "email", today, remainingDays, triggerType, 1, null);
            return true;
        } catch (Exception ex) {
            recordLog(user.getId(), "email", today, remainingDays, triggerType, 0, ex.getMessage());
            return false;
        }
    }

    private boolean alreadySent(Long userId, String channel, LocalDate today) {
        Long count = sendLogMapper.selectCount(new LambdaQueryWrapper<ReminderSendLog>()
                .eq(ReminderSendLog::getUserId, userId)
                .eq(ReminderSendLog::getChannel, channel)
                .eq(ReminderSendLog::getSendDate, today)
                .eq(ReminderSendLog::getStatus, 1));
        return count != null && count > 0;
    }

    private void recordLog(Long userId, String channel, LocalDate today, int remainingDays,
                           String triggerType, int status, String failReason) {
        ReminderSendLog log = new ReminderSendLog();
        log.setUserId(userId);
        log.setChannel(channel);
        log.setSendDate(today);
        log.setRemainingDays(remainingDays);
        log.setTriggerType(triggerType);
        log.setStatus(status);
        log.setFailReason(failReason);
        log.setCreatedAt(LocalDateTime.now());
        sendLogMapper.insert(log);
    }
}
```

> 关键点：`PlatformUser` 已有 `private LocalDateTime membershipExpireAt`（Task 2 已加）。

- [ ] **Step 7: 运行测试，验证算法正确**

```bash
cd project/admin/api && mvn -q test -Dtest=ExpireReminderServiceTest
```

预期 5 个测试全部 PASS。

- [ ] **Step 8: 编译验证整个模块**

```bash
cd project/admin/api && mvn -q compile
```

预期 BUILD SUCCESS。

- [ ] **Step 9: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/ \
        project/admin/api/src/test/java/com/aichuangzuo/admin/modules/reminder/
git commit -m "feat(reminder): 核心 service + 剩余天数算法（TDD）"
```

---

## Task 7: 定时任务 ExpireReminderJob

**Files:**
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/job/ExpireReminderJob.java`

**Interfaces:**
- Consumes: Task 6 的 `ExpireReminderService`、`ReminderConfigService`、`ReminderSchedulerConfig.reminderTaskScheduler()`
- Produces:
  - `@PostConstruct init()`：调用 `configService.syncFromProperties()` → `reschedule()`
  - `@EventListener(ReminderConfigChangedEvent) reschedule()`
  - `@PreDestroy shutdown()`
  - `reschedule()`：根据 `notifyHour` 拼 `0 0 {hour} * * ?` cron；`enabled=0` 时不排程
  - `run()`：调 `service.pageExpiringUsers(advanceDays=N, page=1, size=200)`，分页扫所有命中用户逐个 `remindUser(userId, "auto")`

- [ ] **Step 1: 创建 ExpireReminderJob**

```java
package com.aichuangzuo.admin.modules.reminder.job;

import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.event.ReminderConfigChangedEvent;
import com.aichuangzuo.admin.modules.reminder.service.ExpireReminderService;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.admin.modules.reminder.vo.ExpiringUserVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

/**
 * 到期提醒定时任务。
 * 使用 ThreadPoolTaskScheduler 动态重建 Trigger，cron 由配置 notify_hour 拼出。
 * 配置变更通过事件触发 reschedule。
 */
@Slf4j
@Component
public class ExpireReminderJob {

    private static final int SCAN_PAGE_SIZE = 200;

    private final ReminderConfigService configService;
    private final ExpireReminderService reminderService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private volatile ScheduledFuture<?> scheduledFuture;

    public ExpireReminderJob(ReminderConfigService configService,
                             ExpireReminderService reminderService,
                             ThreadPoolTaskScheduler reminderTaskScheduler) {
        this.configService = configService;
        this.reminderService = reminderService;
        this.taskScheduler = reminderTaskScheduler;
    }

    @EventListener
    public void onConfigChanged(ReminderConfigChangedEvent event) {
        log.info("收到提醒配置变更事件，adminId={}，开始 reschedule", event.adminId());
        reschedule();
    }

    @PostConstruct
    public void init() {
        configService.syncFromProperties();
        reschedule();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        var cfg = configService.getConfig();
        if (cfg.getEnabled() == null || cfg.getEnabled() == 0) {
            log.info("到期提醒定时任务已停用");
            return;
        }
        int hour = cfg.getNotifyHour() == null ? 9 : cfg.getNotifyHour();
        String cron = String.format("0 0 %d * * ?", hour);
        try {
            new CronTrigger(cron); // 校验
            scheduledFuture = taskScheduler.schedule(this::run, new CronTrigger(cron));
            log.info("到期提醒定时任务已注册，cron={}", cron);
        } catch (Exception e) {
            log.warn("到期提醒 cron 非法，注册失败: {}", cron);
        }
    }

    /**
     * 定时任务入口：分页扫描命中用户，逐个调 remindUser("auto")。
     */
    public void run() {
        try {
            int advanceDays = configService.getConfig().getAdvanceDays();
            long page = 1;
            while (true) {
                ExpireReminderService.PageResult pr = reminderService.pageExpiringUsers(
                        new ExpiringUserPageQuery() {{
                            setAdvanceDays(advanceDays);
                            setPage(page);
                            setSize((long) SCAN_PAGE_SIZE);
                        }});
                if (pr.items().isEmpty()) break;
                for (ExpiringUserVO vo : pr.items()) {
                    try {
                        reminderService.remindUser(vo.getUserId(), "auto");
                    } catch (Exception e) {
                        log.warn("自动提醒失败 userId={}, reason={}", vo.getUserId(), e.getMessage());
                    }
                }
                if (pr.items().size() < SCAN_PAGE_SIZE) break;
                page++;
                if (page > 100) { // 防御：单次最多 2 万人
                    log.warn("到期提醒扫描超过 100 页，强制结束");
                    break;
                }
            }
            log.info("到期提醒定时扫描完成，advanceDays={}", advanceDays);
        } catch (Exception e) {
            log.error("到期提醒定时任务异常", e);
        }
    }
}
```

> 备注：`{ setAdvanceDays(...); setPage(...); ... }` 是 anonymous-inner-class initializer，等价于 setter 调用；如果不喜欢这种写法可改为先 new 再调 setter，效果一样。

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && mvn -q compile
```

预期 BUILD SUCCESS。

- [ ] **Step 3: 启动 admin-api 验证初始化日志**

```bash
cd project/admin/api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后日志应包含 `到期提醒配置默认值已写入 DB` 与 `到期提醒定时任务已注册，cron=0 0 9 * * ?`（默认 hour=9）。

调用 `PUT /api/v1/admin/expire-reminder/config` body `{"advanceDays":3,"notifyHour":15,"notifyChannel":"message","enabled":1}`，预期日志出现 `收到提醒配置变更事件` 与 `到期提醒定时任务已注册，cron=0 0 15 * * ?`。停服务。

- [ ] **Step 4: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/job/
git commit -m "feat(reminder): 定时任务 + 事件重排"
```

---

## Task 8: 前端 — 菜单重构 + ExpireReminderView + API

**Files:**
- Modify: `project/admin/web/src/router/index.js`
- Modify: `project/admin/web/src/layouts/AdminLayout.vue`
- Create: `project/admin/web/src/views/ExpireReminderView.vue`
- Create: `project/admin/web/src/api/expireReminder.js`

**Interfaces:**
- Routes: 新增 `expire-reminder` → `ExpireReminderView.vue`
- Menu: `用户管理` 变 `a-sub-menu`，含 `注册用户(/console/users)` 和 `到期提醒(/console/expire-reminder)`
- Breadcrumb: `/console/expire-reminder` → "到期提醒"
- API: `getConfig`, `saveConfig`, `listExpiringUsers`, `remindUser`

- [ ] **Step 1: 创建 api/expireReminder.js**

`project/admin/web/src/api/expireReminder.js`：

```javascript
import request from '@/utils/request.js'

export function getReminderConfig() {
  return request.get('/api/v1/admin/expire-reminder/config').then((res) => res.data)
}

export function saveReminderConfig(data) {
  return request.put('/api/v1/admin/expire-reminder/config', data).then((res) => res.data)
}

export function listExpiringUsers(params = {}) {
  return request.get('/api/v1/admin/expire-reminder/users', { params }).then((res) => res.data)
}

export function remindUser(userId) {
  return request.post(`/api/v1/admin/expire-reminder/users/${userId}/remind`).then((res) => res.data)
}
```

- [ ] **Step 2: 在 router/index.js 注册新路由**

在 `messages` 路由块前插入：

```javascript
      {
        path: 'expire-reminder',
        name: 'AdminExpireReminder',
        component: () => import('@/views/ExpireReminderView.vue')
      },
```

- [ ] **Step 3: 改 AdminLayout.vue 菜单为 a-sub-menu**

把 24-29 行（`用户管理` 的 `a-menu-item`）整块替换为：

```html
        <a-sub-menu key="/console/user-management">
          <template #icon>
            <UserOutlined />
          </template>
          <template #title>用户管理</template>
          <a-menu-item key="/console/users">注册用户</a-menu-item>
          <a-menu-item key="/console/expire-reminder">到期提醒</a-menu-item>
        </a-sub-menu>
```

- [ ] **Step 4: 改 AdminLayout.vue 的 openKeys 默认值**

`openKeys` ref 默认值追加 `/console/user-management`：

```javascript
const openKeys = ref(['/console/settings', '/console/hot-search', '/console/leaderboard', '/console/earnings', '/console/style-management', '/console/user-management'])
```

- [ ] **Step 5: 改 AdminLayout.vue 的 currentMenuName**

在 `'return'` 之前插入：

```javascript
  if (route.path === '/console/expire-reminder') return '到期提醒'
```

- [ ] **Step 6: 创建 ExpireReminderView.vue**

`project/admin/web/src/views/ExpireReminderView.vue`：

```vue
<template>
  <div class="expire-reminder">
    <!-- 配置卡片 -->
    <a-card :bordered="false" class="config-card">
      <div class="config-header">
        <h3 class="config-title">提醒配置</h3>
        <p class="config-desc">设置到期提醒的提前天数、提醒时间点、通知形式；保存后定时任务立即重排。</p>
      </div>
      <a-form
        v-if="config"
        :model="config"
        :rules="rules"
        ref="formRef"
        layout="vertical"
        class="config-form"
      >
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="提前天数 N" name="advanceDays">
              <a-input-number v-model:value="config.advanceDays" :min="1" :max="90" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="提醒时间点（0-23）" name="notifyHour">
              <a-select v-model:value="config.notifyHour" style="width: 100%">
                <a-select-option v-for="h in 24" :key="h - 1" :value="h - 1">
                  {{ String(h - 1).padStart(2, '0') }}:00
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="通知形式" name="notifyChannel">
              <a-radio-group v-model:value="config.notifyChannel">
                <a-radio value="message">站内信</a-radio>
                <a-radio value="email">邮件</a-radio>
                <a-radio value="message_email">站内信+邮件</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="定时开关" name="enabled">
              <a-switch
                :checked="config.enabled === 1"
                @update:checked="(v) => (config.enabled = v ? 1 : 0)"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-button type="primary" :loading="saving" @click="handleSaveConfig">保存配置</a-button>
      </a-form>
    </a-card>

    <!-- 列表卡片 -->
    <a-card :bordered="false" class="list-card">
      <div class="list-header">
        <h3 class="list-title">近 {{ advanceDays }} 天到期用户</h3>
        <div class="list-tools">
          <a-input-number v-model:value="advanceDays" :min="1" :max="90" addon-before="提前天数" />
          <a-button type="primary" @click="fetchUsers">刷新</a-button>
        </div>
      </div>
      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{ current: page, pageSize: pageSize, total: total, showSizeChanger: true }"
        row-key="userId"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'remainingDays'">
            <a-tag :color="record.remainingDays <= 1 ? 'red' : record.remainingDays <= 3 ? 'orange' : 'blue'">
              {{ record.remainingDays }} 天
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastRemindedAt'">
            <span v-if="record.lastRemindedAt">{{ record.lastRemindedAt }}（{{ record.lastReminderChannel }}）</span>
            <span v-else>—</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" :loading="reminding[record.userId]" @click="handleRemind(record)">
              立即提醒
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getReminderConfig,
  saveReminderConfig,
  listExpiringUsers,
  remindUser
} from '@/api/expireReminder.js'

const config = ref(null)
const saving = ref(false)
const formRef = ref()

const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const advanceDays = ref(7)
const loading = ref(false)
const reminding = reactive({})

const rules = {
  advanceDays: [{ required: true, type: 'number', min: 1, max: 90, message: '1-90' }],
  notifyHour: [{ required: true, type: 'number', min: 0, max: 23, message: '0-23' }],
  notifyChannel: [{ required: true, message: '请选择' }],
  enabled: [{ required: true, message: '请选择' }]
}

const columns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '到期时间', dataIndex: 'membershipExpireAt', key: 'membershipExpireAt', width: 170 },
  { title: '剩余天数', key: 'remainingDays', width: 90 },
  { title: '最近提醒', key: 'lastRemindedAt', width: 240 },
  { title: '操作', key: 'actions', width: 100 }
]

const fetchConfig = async () => {
  config.value = await getReminderConfig()
  if (config.value) advanceDays.value = config.value.advanceDays
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await listExpiringUsers({ advanceDays: advanceDays.value, page: page.value, pageSize: pageSize.value })
    items.value = res.items || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSaveConfig = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const saved = await saveReminderConfig(config.value)
    config.value = saved
    message.success('配置已保存，定时任务已重排')
    fetchUsers()
  } finally {
    saving.value = false
  }
}

const handleRemind = async (record) => {
  reminding[record.userId] = true
  try {
    await remindUser(record.userId)
    message.success(`已提醒：${record.email}`)
    fetchUsers()
  } finally {
    reminding[record.userId] = false
  }
}

const handleTableChange = (pagination) => {
  page.value = pagination.current
  pageSize.value = pagination.pageSize
  fetchUsers()
}

onMounted(() => {
  fetchConfig()
  fetchUsers()
})
</script>

<style scoped>
.expire-reminder {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card,
.list-card {
  border-radius: 8px;
}

.config-header,
.list-header {
  margin-bottom: 16px;
}

.config-title,
.list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.config-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.list-tools {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
```

> 注意：列表接口后端字段命名是 `page`/`size`，与前端 `pageSize` 不同。Task 9 在后端补 `ExpireReminderController` 时会用 `@RequestParam` 接收 `advanceDays/page/pageSize` 三个参数。

- [ ] **Step 7: 前端编译验证**

```bash
cd project/admin/web && npm run build
```

预期 BUILD SUCCESS，无 import / 语法错误。

- [ ] **Step 8: 提交**

```bash
git add project/admin/web/src/router/index.js \
        project/admin/web/src/layouts/AdminLayout.vue \
        project/admin/web/src/api/expireReminder.js \
        project/admin/web/src/views/ExpireReminderView.vue
git commit -m "feat(reminder-web): 菜单重构 + 到期提醒页 + API"
```

---

## Task 9: 前端 — 注册用户页会员到期展示 + 编辑 + 后端 ExpireReminderController

**Files:**
- Modify: `project/admin/web/src/api/user.js`
- Modify: `project/admin/web/src/views/UserListView.vue`
- Create: `project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/controller/ExpireReminderController.java`

**Interfaces:**
- API: `updateUserMembership(id, expireDate)` → `PATCH /api/v1/admin/users/{id}/membership` body `{"expireDate": "yyyy-MM-dd" | null}`
- Controller: `GET /users?advanceDays=&page=&pageSize=` / `POST /users/{userId}/remind`

- [ ] **Step 1: 创建 ExpireReminderController（后端）**

`project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/controller/ExpireReminderController.java`：

```java
package com.aichuangzuo.admin.modules.reminder.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.service.ExpireReminderService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端到期提醒-用户列表与手动提醒")
@RestController
@RequestMapping("/api/v1/admin/expire-reminder")
@RequiredArgsConstructor
public class ExpireReminderController {

    private final ExpireReminderService reminderService;
    private final AdminUserPermissionService permissionService;

    @GetMapping("/users")
    public Result<ExpireReminderService.PageResult> listExpiringUsers(
            @RequestParam(name = "advanceDays", required = false) Integer advanceDays,
            @RequestParam(name = "page", defaultValue = "1") Long page,
            @RequestParam(name = "pageSize", defaultValue = "20") Long pageSize) {
        checkSuperAdmin();
        ExpiringUserPageQuery query = new ExpiringUserPageQuery();
        query.setAdvanceDays(advanceDays);
        query.setPage(page);
        query.setSize(pageSize);
        return Result.success(reminderService.pageExpiringUsers(query));
    }

    @PostMapping("/users/{userId}/remind")
    public Result<ExpireReminderService.RemindResult> remind(@PathVariable("userId") Long userId) {
        checkSuperAdmin();
        return Result.success(reminderService.remindUser(userId, "manual"));
    }

    private void checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd project/admin/api && mvn -q compile
```

预期 BUILD SUCCESS。

- [ ] **Step 3: api/user.js 增加设置到期时间方法**

在文件末尾追加：

```javascript
export function updateUserMembership(id, expireDate) {
  return request.patch(`/api/v1/admin/users/${id}/membership`, { expireDate }).then((res) => res.data)
}
```

- [ ] **Step 4: UserListView.vue 详情 drawer 加会员到期展示 + 编辑入口**

在 `<a-descriptions-item label="邀请码">` 之后插入：

```html
        <a-descriptions-item label="会员到期">
          <template v-if="editingMembership">
            <a-space>
              <a-date-picker
                v-model:value="membershipDate"
                value-format="YYYY-MM-DD"
                placeholder="选择到期日（清空=非会员）"
              />
              <a-button type="link" size="small" @click="confirmMembership">保存</a-button>
              <a-button type="link" size="small" @click="cancelMembership">取消</a-button>
            </a-space>
          </template>
          <template v-else>
            <span>{{ detailUser?.membershipExpireAt || '非会员' }}</span>
            <a-button type="link" size="small" @click="startEditMembership">
              {{ detailUser?.membershipExpireAt ? '修改' : '设置' }}
            </a-button>
            <a-button
              v-if="detailUser?.membershipExpireAt"
              type="link"
              size="small"
              @click="clearMembership"
            >
              清空
            </a-button>
          </template>
        </a-descriptions-item>
```

- [ ] **Step 5: UserListView.vue 增加编辑状态 + 方法**

在 `<script setup>` 末尾、`onMounted` 之前追加：

```javascript
import { updateUserMembership } from '@/api/user.js'

const editingMembership = ref(false)
const membershipDate = ref(null)

const startEditMembership = () => {
  membershipDate.value = detailUser.value?.membershipExpireAt
    ? detailUser.value.membershipExpireAt.substring(0, 10)
    : null
  editingMembership.value = true
}

const cancelMembership = () => {
  editingMembership.value = false
  membershipDate.value = null
}

const confirmMembership = async () => {
  if (!detailUser.value) return
  await updateUserMembership(detailUser.value.id, membershipDate.value || null)
  message.success('会员到期时间已更新')
  editingMembership.value = false
  // 重新拉详情
  detailUser.value = await getUser(detailUser.value.id)
  fetchUsers()
}

const clearMembership = async () => {
  if (!detailUser.value) return
  await updateUserMembership(detailUser.value.id, null)
  message.success('已清空会员到期时间')
  detailUser.value = await getUser(detailUser.value.id)
  fetchUsers()
}
```

注意：上面 `import { updateUserMembership } from '@/api/user.js'` 应与文件顶部的 `import { getUser } from '@/api/user.js'` 合并：

```javascript
import { getUser, updateUserMembership } from '@/api/user.js'
```

同时在 script setup 顶部 import `message`：

```javascript
import { message } from 'ant-design-vue'
```

- [ ] **Step 6: 前端编译验证**

```bash
cd project/admin/web && npm run build
```

预期 BUILD SUCCESS。

- [ ] **Step 7: 端到端联调（手动）**

启动 admin-api：
```bash
cd project/admin/api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
启动前端：
```bash
cd project/admin/web && npm run dev
```

验证清单：
1. 登录管理端，左侧菜单应出现「用户管理」父菜单，点击展开有「注册用户」「到期提醒」。
2. 点击「注册用户」→ 列表加载；点击某用户「查看详情」→ drawer 显示「会员到期」字段，点击「设置」选择日期保存，详情刷新显示新值。
3. 点击「到期提醒」→ 配置卡片显示当前配置（默认 7/9/message/开）；列表卡片显示当前 advanceDays=7 命中用户。
4. 点列表中某用户「立即提醒」→ 提示成功，刷新该行「最近提醒」显示当前时间戳。
5. 修改配置中通知形式为 `email` 并保存 → 日志应出现「收到提醒配置变更事件」「到期提醒定时任务已注册」。

- [ ] **Step 8: 提交**

```bash
git add project/admin/api/src/main/java/com/aichuangzuo/admin/modules/reminder/controller/ExpireReminderController.java \
        project/admin/web/src/api/user.js \
        project/admin/web/src/views/UserListView.vue
git commit -m "feat(reminder): 注册用户会员到期编辑 + 提醒列表/手动接口"
```

---

## Self-Review Checklist

1. **Spec coverage**：
   - 菜单重构 ✓ Task 8
   - 会员到期字段 + 手动设置 ✓ Task 2、Task 9
   - 提醒配置（提前天数/时间点/通知形式/开关）✓ Task 4
   - 定时任务（cron 拼装 + 事件重排 + 开关）✓ Task 7
   - 列表查询 + 剩余天数展示 ✓ Task 6 + Task 8
   - 手动提醒 ✓ Task 6 + Task 9
   - 通知下发（站内信 + 邮件）✓ Task 5
   - 去重（唯一键）✓ Task 1 + Task 6
   - 算法准确（0 ≤ remainingDays ≤ N，存"次日 00:00"）✓ Task 6 + 单测

2. **Placeholder scan**：无 TBD/TODO/同 X。代码块全部为可粘贴的完整代码。

3. **Type consistency**：
   - `ExpireReminderService.calcRemainingDays(LocalDateTime)` 与测试一致
   - `ReminderConfig.getNotifyChannel()` 返回 `String`，与 `ReminderConfigRequest` 的 `String` 一致
   - `PageResult.page/size` 在前端用 `page/pageSize` 接收，Controller 显式映射 ✓

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-08-expire-reminder.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?