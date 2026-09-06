-- ============================================================
-- 业务域：消息与通知
-- 表数量：5
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '批次号：广播单行/指定人多行共享',
  `msg_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型：announcement-公告 / feature-新功能 / promotion-优惠活动 / generation-生成完成 / membership-会员提醒',
  `scope` tinyint unsigned NOT NULL COMMENT '范围：1-广播（全体），2-个人',
  `target_user_id` bigint unsigned DEFAULT NULL COMMENT '目标用户ID：个人消息填写，广播为NULL',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `summary` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '摘要',
  `link_url` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点击跳转路由，空则前端按类型默认跳转',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `content` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '完整正文,summary 是列表摘要,弹框用',
  `sub_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子类型,如 membership.subscribed / membership.expiring',
  PRIMARY KEY (`id`),
  KEY `idx_message_scope_type` (`scope`,`msg_type`),
  KEY `idx_message_target` (`target_user_id`),
  KEY `idx_message_created` (`created_at`),
  KEY `idx_message_biz_no` (`biz_no`),
  KEY `idx_message_msg_type_biz_no` (`msg_type`,`biz_no`),
  KEY `idx_message_subtype` (`sub_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_message_read` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `message_id` bigint unsigned NOT NULL COMMENT '消息ID',
  `read_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '已读时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_read_user_msg` (`user_id`,`message_id`),
  KEY `idx_message_read_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_reminder_send_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '被提醒用户',
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'message/email',
  `send_date` date NOT NULL COMMENT '发送日期（本地日）',
  `remaining_days` int NOT NULL COMMENT '发送时剩余天数',
  `trigger_type` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'auto/manual',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '1-成功 0-失败',
  `fail_reason` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reminder_user_channel_date_type` (`user_id`,`channel`,`send_date`,`trigger_type`),
  KEY `idx_reminder_send_date` (`send_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reminder_config` (
  `id` bigint unsigned NOT NULL,
  `advance_days` int NOT NULL DEFAULT '7' COMMENT '提前提醒天数 N',
  `notify_hour` tinyint unsigned NOT NULL DEFAULT '9' COMMENT '每天提醒时间点 0-23',
  `notify_channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'message' COMMENT 'message/email/message_email',
  `enabled` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '定时提醒开关',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_message_notify_outbox` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：generation_completed / generation_failed / ...',
  `biz_id` bigint unsigned NOT NULL COMMENT '业务主键（如 task_id）',
  `target_user_id` bigint unsigned NOT NULL COMMENT '目标用户ID',
  `payload` json NOT NULL COMMENT '透传给 user-api notify-completion 的请求体',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-PENDING 1-SENT 2-FAILED',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已重试次数',
  `next_retry_at` datetime(3) NOT NULL COMMENT '下次重试时间',
  `last_error` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后一次失败原因',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `sent_at` datetime(3) DEFAULT NULL COMMENT '成功派发时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_status_next_retry` (`status`,`next_retry_at`),
  KEY `idx_biz_type_id` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
