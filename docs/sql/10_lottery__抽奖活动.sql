-- ============================================================
-- 业务域：抽奖活动
-- 表数量：7
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_lottery_campaign` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '活动描述',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '宣传图URL',
  `rules` text COLLATE utf8mb4_unicode_ci COMMENT '活动规则（可填写奖项说明等）',
  `start_time` datetime(3) NOT NULL COMMENT '开始时间',
  `end_time` datetime(3) NOT NULL COMMENT '结束时间',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-draft,1-ongoing,2-ended,3-disabled',
  `free_draws_per_user` int unsigned NOT NULL DEFAULT '1' COMMENT '每轮免费次数',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_lottery_campaign_status_time` (`status`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_lottery_display_winner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `campaign_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `tier_id` bigint unsigned DEFAULT NULL COMMENT '奖项ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '用户ID',
  `code_id` bigint unsigned DEFAULT NULL COMMENT '关联兑换码ID（人工发奖/真实中奖生成）',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '展示昵称',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '展示头像',
  `prize_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '展示奖品名',
  `win_time` datetime(3) NOT NULL COMMENT '展示时间',
  `is_real` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '0-机器人/运营配置，1-真实中奖',
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '0-隐藏，1-展示',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_lottery_display_winner_campaign` (`campaign_id`,`status`,`win_time`),
  KEY `idx_lottery_display_winner_code` (`code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_lottery_draw_chance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `campaign_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `chance_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '次数类型：free/invite',
  `source_invite_relation_id` bigint unsigned DEFAULT NULL COMMENT 'invite来源关系ID',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'available' COMMENT '状态：available/used',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `used_at` datetime(3) DEFAULT NULL,
  `uk_lottery_draw_chance_source` bigint unsigned GENERATED ALWAYS AS (coalesce(`source_invite_relation_id`,0)) STORED COMMENT '唯一键辅助列：free 为 0，invite 为 source_invite_relation_id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lottery_draw_chance` (`campaign_id`,`user_id`,`chance_type`,`uk_lottery_draw_chance_source`),
  KEY `idx_lottery_draw_chance_user_campaign` (`user_id`,`campaign_id`,`status`),
  KEY `idx_lottery_draw_chance_invite` (`source_invite_relation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_lottery_draw_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号',
  `campaign_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `tier_id` bigint unsigned DEFAULT NULL COMMENT '命中奖项ID',
  `code_id` bigint unsigned DEFAULT NULL COMMENT '生成兑换码ID',
  `draw_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '抽奖类型：free/invite',
  `invite_relation_id` bigint unsigned DEFAULT NULL COMMENT 'invite来源关系ID',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lottery_draw_record_biz_no` (`biz_no`),
  KEY `idx_lottery_draw_record_user_campaign` (`user_id`,`campaign_id`),
  KEY `idx_lottery_draw_record_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_lottery_prize_tier` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `campaign_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `tier_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '奖项标识',
  `tier_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '奖项名称',
  `prize_level` tinyint unsigned NOT NULL DEFAULT '6' COMMENT '奖项等级：1-特等奖，2-一等奖，3-二等奖，4-三等奖，5-四等奖，6-五等奖；数值越小越靠前',
  `probability` decimal(10,8) NOT NULL COMMENT '中奖概率',
  `max_win_count` int unsigned DEFAULT NULL COMMENT '全局可中次数上限，NULL表示不限',
  `remaining_win_count` int unsigned DEFAULT NULL COMMENT '剩余可中次数',
  `display_remaining` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否在前端显示剩余数量：0-不显示，1-显示',
  `display_remaining_count` int unsigned DEFAULT NULL COMMENT '手动设置的显示剩余数量，NULL 表示使用真实剩余数量',
  `reward_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '奖励类型：coin/membership/coupon/none',
  `reward_value_json` json NOT NULL COMMENT '奖励参数',
  `code_prefix` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '兑换码前缀',
  `code_length` int unsigned DEFAULT NULL COMMENT '兑换码总字符数（含前缀）',
  `code_validity_days` int unsigned NOT NULL DEFAULT '30' COMMENT '兑换码有效期天数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lottery_prize_tier_campaign_key` (`campaign_id`,`tier_key`),
  KEY `idx_lottery_prize_tier_campaign_status` (`campaign_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_lottery_redemption_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '兑换码',
  `campaign_id` bigint unsigned NOT NULL COMMENT '活动ID',
  `tier_id` bigint unsigned NOT NULL COMMENT '奖项ID',
  `drawer_user_id` bigint unsigned DEFAULT NULL COMMENT '中奖人用户ID',
  `reward_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '奖励类型',
  `reward_value_json` json NOT NULL COMMENT '奖励参数快照',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
  `used_by` bigint unsigned DEFAULT NULL COMMENT '兑换人用户ID',
  `used_at` datetime(3) DEFAULT NULL,
  `expires_at` datetime(3) NOT NULL COMMENT '过期时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lottery_redemption_code_code` (`code`),
  KEY `idx_lottery_redemption_code_campaign` (`campaign_id`),
  KEY `idx_lottery_redemption_code_drawer` (`drawer_user_id`),
  KEY `idx_lottery_redemption_code_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_lottery_risk_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `campaign_id` bigint unsigned DEFAULT NULL COMMENT '活动ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `action` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作：draw/redeem/invite',
  `risk_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '风控类型',
  `detail_json` json DEFAULT NULL COMMENT '详情',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_lottery_risk_log_user_action` (`user_id`,`action`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
