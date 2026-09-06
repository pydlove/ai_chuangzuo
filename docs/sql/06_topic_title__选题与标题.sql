-- ============================================================
-- 业务域：选题与标题
-- 表数量：5
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_topic_title` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `summary` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题概要（写作方向）',
  `direction` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '生成时用的方向提示词（追溯用）',
  `use_count` int unsigned NOT NULL DEFAULT '0' COMMENT '全站累计使用次数',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID，0表示系统',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID，0表示系统',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_u_topic_title_deleted_id` (`is_deleted`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_topic_title_usage` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `title_id` bigint unsigned NOT NULL COMMENT '标题ID，关联 u_topic_title.id',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID，0表示系统',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID，0表示系统',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_topic_usage_user_title` (`user_id`,`title_id`),
  KEY `idx_u_topic_usage_title` (`title_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_topic_title_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '0=queued,1=processing,2=completed,3=failed',
  `count` int NOT NULL COMMENT '请求生成数量',
  `direction` varchar(1024) DEFAULT NULL COMMENT '方向提示词（可选）',
  `generated_count` int NOT NULL DEFAULT '0' COMMENT '实际入库条数',
  `failed_reason` varchar(512) DEFAULT NULL COMMENT '失败原因（status=3 时填写）',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `started_at` datetime(3) DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_topic_title_task_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_recommended_creation_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `current_step` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '当前步骤：1选题 2观点 3字数 4提示词 5模板',
  `topics_json` json DEFAULT NULL COMMENT 'AI生成的选题列表',
  `selected_topic_json` json DEFAULT NULL COMMENT '用户选中的选题',
  `angles_json` json DEFAULT NULL COMMENT 'AI生成的观点角度列表',
  `selected_angles_json` json DEFAULT NULL COMMENT '用户选中的观点角度（最多3个）',
  `word_count` int unsigned DEFAULT NULL COMMENT '用户设置的字数',
  `prompt` text COLLATE utf8mb4_unicode_ci COMMENT '用户选中的创作提示词',
  `skill_ref` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '选中的提示词市场 skill 业务编号，用于结算创作币收益',
  `template` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户选中的导出模板key',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT '会话状态：draft',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_recommended_creation_session_user_id` (`user_id`),
  KEY `idx_u_recommended_creation_session_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_recommended_creation_topic_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '已推荐选题标题',
  `session_id` bigint unsigned NOT NULL COMMENT '关联的推荐创作会话ID',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  KEY `idx_u_recommended_creation_topic_history_user_created` (`user_id`,`created_at`),
  KEY `idx_u_recommended_creation_topic_history_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
