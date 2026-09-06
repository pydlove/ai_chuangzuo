-- ============================================================
-- 业务域：会员与权益
-- 表数量：7
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_user_membership` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `level` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '当前等级：basic/pro/flagship',
  `started_at` date NOT NULL COMMENT '本次会员开始日期',
  `expires_at` date NOT NULL COMMENT '会员到期日期',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_user_membership_user_id` (`user_id`),
  KEY `idx_u_user_membership_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_membership_pending` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `plan_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐：basic/pro/flagship',
  `days` int unsigned NOT NULL COMMENT '天数',
  `planned_start_at` date NOT NULL COMMENT '计划生效日期',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending/activated/expired',
  `source_code_id` bigint unsigned DEFAULT NULL COMMENT '来源兑换码ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `activated_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_membership_pending_user_status` (`user_id`,`status`,`planned_start_at`),
  KEY `idx_user_membership_pending_start` (`planned_start_at`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐 key：basic/pro/flagship',
  `display_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐显示名，如 基础版',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '前台展示顺序',
  `recommended` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否推荐套餐：0-否，1-是',
  `price_monthly` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '月度原价（无折扣时同步）',
  `price_quarter` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '季度价',
  `price_year` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '年度价',
  `original_monthly` decimal(10,2) DEFAULT NULL COMMENT '月度划线价',
  `original_quarter` decimal(10,2) DEFAULT NULL COMMENT '季度划线价',
  `original_year` decimal(10,2) DEFAULT NULL COMMENT '年度划线价',
  `articles_monthly` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '月度文章数展示文案',
  `articles_quarter` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '季度文章数展示文案',
  `articles_year` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年度文章数展示文案',
  `savings_year` decimal(10,2) DEFAULT NULL COMMENT '年度立省金额',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_plan_key` (`plan_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_plan_benefit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐：basic/pro/flagship',
  `benefit_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益编码，关联 u_benefit.code',
  `benefit_value` varchar(2048) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益值：boolean 存 true/false，quota 存数字，tier 存等级标识；template_access 存逗号分隔的 template_key',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_plan_benefit_plan_code` (`plan_key`,`benefit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_benefit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益编码，如 ai_article_quota',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益名称',
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：boolean/quota/tier/lifetime',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权益描述',
  `display_label` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对比表行标签/默认名称',
  `card_value_tpl` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '价格卡值模板，含 {value} 占位',
  `value_label_json` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'value→显示文本 JSON 覆盖',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_benefit_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_benefit_usage` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `benefit_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益编码',
  `period` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期标识：月度格式 yyyy-MM，lifetime 类型权益为 lifetime',
  `used_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已用量',
  `pre_used_count` int NOT NULL DEFAULT '0' COMMENT '预扣额度数',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_benefit_usage_user_code_period` (`user_id`,`benefit_code`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_experience_token` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次号',
  `token` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '体验令牌',
  `plan_key` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pro' COMMENT '套餐类型：basic/pro/flagship',
  `membership_days` int unsigned NOT NULL DEFAULT '30' COMMENT '赠送会员天数',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-未使用 1-已使用 2-过期',
  `used_by_user_id` bigint unsigned DEFAULT NULL COMMENT '使用人用户ID',
  `used_at` datetime(3) DEFAULT NULL COMMENT '使用时间',
  `expires_at` datetime(3) DEFAULT NULL COMMENT '令牌有效期',
  `created_by` bigint unsigned NOT NULL COMMENT '创建管理员ID',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  `used_by_user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用人昵称',
  `used_by_user_email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用人邮箱',
  `used_by_user_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用人手机号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_experience_token` (`token`),
  KEY `idx_experience_token_batch` (`batch_id`),
  KEY `idx_experience_token_status` (`status`,`expires_at`),
  KEY `idx_experience_token_used_by` (`used_by_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
