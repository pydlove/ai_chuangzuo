# 管理端到期提醒设计

## 背景

管理控制台需要对"会员到期"进行运营干预：定时找出即将到期的用户并下发提醒（站内信 / 邮件），也支持对单个用户手动提醒。同时借此把"用户管理"从单一叶子菜单升级为一级父菜单，下设「注册用户」「到期提醒」两个子页。

当前现状（调研结论）：

- `u_user` 表**没有**任何会员/到期字段；全项目无会员/订单模块，无到期数据源。
- 站内信能力在用户端：`u_message` 表 + `MessageService.pushPersonal(userId, msgType, title, summary, linkUrl)`，`msg_type` 已预留 `membership`。
- 邮件能力仅在用户端：`spring-boot-starter-mail` + `JavaMailSender`；admin-api **没有** mail 依赖与配置。
- 定时任务样板：admin-api 的 hot-search 模块用 `ThreadPoolTaskScheduler` + `CronTrigger` + DB 配置 + 变更事件重排（非 `@Scheduled`）。
- admin-api 与 user-api 连**同一个库** `aichuangzuo`，各自独立 Flyway 历史表（admin 走 `V2.0.0_xxx`）。admin-api 已有 `V2.0.0_004__add_biz_no_to_u_message.sql` 改动共享表的先例。

## 目标

- **菜单重构**：`用户管理` 变为一级 sub-menu，子菜单：`注册用户`（现有 `/console/users` 页面原样归入）、`到期提醒`（新页面 `/console/expire-reminder`）。
- **会员到期字段**：`u_user` 新增 `membership_expire_at`，管理端可在注册用户页手动设置。
- **到期用户查询**：查询"近 N 天到期"的用户，展示剩余天数。
- **定时提醒配置**：提前天数 N、每天提醒时间点（几点）、通知形式（消息 / 邮件 / 消息+邮件）、开关；保存后立即重排定时任务。
- **定时提醒任务**：每天在配置时间点跑，找出 `0 < 剩余天数 ≤ N` 的用户，按配置通知形式下发，去重防止同日重复。
- **手动提醒**：对列表中某个用户点「立即提醒」，按当前配置通知形式即时下发。

## 非目标

- 不实现会员购买 / 订单 / 支付模块；会员到期时间靠管理端手动设置（数据源决策）。
- 不引入 Redis / MQ / 定时任务中间件；沿用 `ThreadPoolTaskScheduler` + DB 配置。
- 不做多档提醒（7/3/1），仅单个 N 天阈值。
- 不做多租户 / 细粒度权限，沿用现有 `SUPER_ADMIN` 校验。
- 不改用户端消息中心的读接口与前端。

## 到期时间算法（核心，务必准确）

- `membership_expire_at` 存**到期日 24:00 结束的那一刻**。会员有效期到 7/10 24点结束 → 存 `2026-07-11 00:00:00`，即"最后有效日" = 7/10。
- **剩余天数（不含今天）= 最后有效日 − 今天**（按日期算，忽略时分秒）：
  - 用 `LocalDate` 相减：`remainingDays = ChronoUnit.DAYS.between(today, lastActiveDate)`
  - `lastActiveDate = membership_expire_at.toLocalDate().minusDays(1)`（因为存的是次日 00:00）
  - 校验：今天 7/8、到期 7/10 24点（存 7/11 00:00）→ lastActiveDate=7/10 → `10 − 8 = 2` ✓
  - 今天 7/10 → 0（今天到期，今晚 24点结束，仍有效）
  - 今天 7/11 → −1（已过期）
- **命中区间统一为 `0 ≤ remainingDays ≤ N`**（定时任务与列表查询一致）：剩余 0 天表示今晚 24点到期，属于最紧急，必须提醒；剩余 < 0（已过期）不再提醒。

> 存储约定统一为"次日 00:00"，避免 `23:59:59` 带来的边界歧义；所有剩余天数计算只走 `LocalDate`，不参与时分秒比较。

## 数据库（admin-api，新增 3 个迁移）

### `V2.0.0_005__add_membership_expire_at_to_user.sql`

```sql
ALTER TABLE u_user
    ADD COLUMN membership_expire_at DATETIME NULL COMMENT '会员到期时刻（到期日次日00:00，NULL=非会员）' AFTER user_type;
```

### `V2.0.0_006__create_reminder_config_table.sql`（单行配置，照抄 hot_search_config 模板）

```sql
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

启动时若 `id=1` 不存在则插入默认值（复用 hot-search 的 `syncFromProperties()` 模式，默认值来自 `ReminderProperties` / yml）。

### `V2.0.0_007__create_reminder_send_log_table.sql`（发送记录 + 去重）

```sql
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

- 唯一键 `(user_id, channel, send_date)` 保证同一用户同一天同一渠道只成功发一次。
- `channel` 按单渠道拆开记录：`message_email` 配置会产生 message、email 两条 log。
- 去重策略：发送前查当天该用户该渠道是否已有 `status=1` 记录；有则跳过。手动提醒同样受这条唯一键约束（同日重复点击不会重发）。

## 架构

### 后端（admin-api，新增 `modules/reminder`）

复用 hot-search 全套模式，包结构参考 `java-package-conventions.md`（`job/` 放定时任务，禁止 `@Scheduled`）：

- `entity/ReminderConfig.java`（`@TableName("reminder_config")`，`@TableId(type=INPUT)`）、`entity/ReminderSendLog.java`
- `mapper/ReminderConfigMapper.java`、`mapper/ReminderSendLogMapper.java`
- `properties/ReminderProperties.java`（绑 yml `reminder.*` 默认值）
- `config/ReminderSchedulerConfig.java`（`ThreadPoolTaskScheduler` Bean）
- `event/ReminderConfigChangedEvent.java`
- `service/ReminderConfigService.java`（`getConfig` / `saveConfig(req, updatedBy)` / `syncFromProperties`，保存后 publish 事件）
- `service/ExpireReminderService.java`：
  - `pageExpiringUsers(advanceDays, page, pageSize)`：查 `membership_expire_at` 非空且 `0 ≤ remainingDays ≤ N` 的用户，返回含剩余天数、最近提醒时间。
  - `remindUser(userId, triggerType)`：按当前配置通知形式对单用户下发（手动/自动共用）。
- `job/ExpireReminderJob.java`：`@PostConstruct` → `reschedule()`；cron 由 `notify_hour` 拼成 `0 0 {hour} * * ?`；`enabled=0` 则不排程；`@EventListener(ReminderConfigChangedEvent)` 重排。job 执行时分页扫描命中用户，逐个调 `remindUser(userId, "auto")`。
- `controller/ExpireReminderController.java`（`/api/v1/admin/expire-reminder`，全走 `checkSuperAdmin`）：
  - `GET /config`、`PUT /config`
  - `GET /users?advanceDays=&page=&pageSize=`（近 N 天到期用户列表）
  - `POST /users/{userId}/remind`（手动提醒单用户）

### 通知下发（直接读写共享库，不跨模块/不 HTTP）

- **站内信**：admin-api 直接 `insert u_message`（`msg_type='membership'`，`scope=2`，`target_user_id=userId`，`biz_no` 按现有约定生成）。为此在 admin-api 侧新增一个轻量 `Message` 实体 + Mapper 指向 `u_message`（只做 insert），不引用 user-api 代码。标题/摘要用固定模板（如"会员即将到期"/"您的会员将于 X 天后到期，请及时续费"）。
- **邮件**：admin-api `pom.xml` 新增 `spring-boot-starter-mail`；`application.yml` 增加 `spring.mail.*`（复用 user-api 同样的 env 变量 `SPRING_MAIL_*`）。新增 `service/ReminderMailService.java` 注入 `JavaMailSender`，`send(to, subject, text)`，到期提醒模板本地构造。

### 前端（admin/web）

- `router/index.js`：新增子路由 `expire-reminder` → 新建 `views/ExpireReminderView.vue`。`/console/users` 路由与 `UserListView.vue` 不变。
- `layouts/AdminLayout.vue`：`用户管理` 从 `a-menu-item` 改为 `a-sub-menu`（图标沿用 `UserOutlined`），内含两个 `a-menu-item`：`注册用户`（`/console/users`）、`到期提醒`（`/console/expire-reminder`）。同步维护 `openKeys`（新增 `/console/users` 父 key）与 `currentMenuName` 面包屑映射。
- `views/ExpireReminderView.vue`：
  - 顶部「提醒配置」卡片：提前天数 N（number）、提醒时间点（0-23 select）、通知形式（message/email/message_email 单选）、开关，保存调 `PUT /config`。
  - 中部「近 N 天到期用户」表格：查询条件可临时改 N；列 = 账号/邮箱/昵称/到期时间/剩余天数/最近提醒时间/操作（「立即提醒」按钮，调 `POST /users/{id}/remind`，成功刷新最近提醒时间）。
- `views/UserListView.vue` 增强：详情 drawer 展示 `membership_expire_at`，并支持手动设置（新增/编辑到期时间的入口 + 后端接口）。手动设置到期时间的接口挂在现有 `AdminUserController`（`PATCH /api/v1/admin/users/{id}/membership` 或复用详情编辑），前端把用户选的"到期日"转换为"次日 00:00"存储。
- `api/`：新增 `expireReminder.js`；`user.js` 增加设置会员到期时间的方法。

## 剩余天数计算的落库查询

列表分页查询需要按剩余天数过滤。用户量不大，采用简单可靠方式：SQL 只按 `membership_expire_at` 时间范围粗筛（`membership_expire_at IS NOT NULL AND membership_expire_at > today00:00 AND membership_expire_at <= (today + N + 1)日 00:00`），再在 Java 侧用 `LocalDate` 精确计算 `remainingDays` 并排序。避免把日期算法写进 SQL 造成边界错误。

- 下界 `today 00:00`：排除已过期（`membership_expire_at <= today00:00` 即最后有效日 < 今天）。
- 上界 `(today + N + 1)日 00:00`：包含"最后有效日 = today+N"的用户（其 expire = today+N+1 00:00）。

## 测试

- **算法单测**（重点）：`ExpireReminderService` 剩余天数计算，覆盖 今天/明天/N天/已过期/存 24点边界 各用例，断言 7/8 对 7/10 24点 = 2 天。
- **命中区间单测**：`0 ≤ remainingDays ≤ N` 命中、`remainingDays = N+1` 不命中、已过期不命中。
- **去重集成测**：同一用户同渠道同日重复 remind，第二次因唯一键跳过，不重复插 `u_message` / 不重复发信。
- **手动触发 E2E/接口测**：`POST /users/{id}/remind` 按 `message_email` 配置产生 1 条 u_message + 1 封邮件（测试用 GreenMail 或断言 mail sender 调用）+ 2 条 send_log。
- **前端**：菜单渲染出「用户管理」父级 + 两个子菜单；到期提醒页配置保存、列表渲染剩余天数、立即提醒按钮可用。
