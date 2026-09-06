-- ============================================================
-- 业务域：自媒体运营方案
-- 表数量：5
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_self_media_plan` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `platform_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主攻平台key，如 xiaohongshu',
  `platform_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主攻平台显示名，如 小红书',
  `niche_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '细分赛道key',
  `niche_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '细分赛道显示名',
  `persona_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人设key',
  `persona_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人设显示名',
  `content_pillars_json` json NOT NULL COMMENT '内容支柱比例 [{"name":"...","percent":60},...]',
  `recommendation_context_json` json DEFAULT NULL COMMENT 'AI推荐问卷上下文',
  `answers_json` json DEFAULT NULL COMMENT '用户对平台问题的答案 [{"questionKey":"...","answer":"..."}]',
  `question_prompt_code` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成问题时使用的 prompt code',
  `is_recommended_by_ai` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '平台是否由AI推荐：0-否，1-是',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_self_media_plan_user_id` (`user_id`),
  KEY `idx_u_self_media_plan_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_self_media_plan_niche` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `platform_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台key，如 xiaohongshu',
  `answer_snapshot_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '答案快照 SHA256',
  `answer_snapshot_json` json NOT NULL COMMENT '答案快照 JSON',
  `niche_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '赛道标识',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '赛道名称',
  `audience` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标人群',
  `monetization` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变现方式',
  `risk_label` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '风险标签',
  `risk_color` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '风险颜色',
  `case_count` int NOT NULL DEFAULT '0' COMMENT '案例数',
  `reason` text COLLATE utf8mb4_unicode_ci COMMENT '推荐理由',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_platform_hash_niche` (`user_id`,`platform_key`,`answer_snapshot_hash`,`niche_key`),
  KEY `idx_user_platform_hash` (`user_id`,`platform_key`,`answer_snapshot_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_self_media_plan_persona` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `platform_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台key，如 xiaohongshu',
  `answer_snapshot_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '答案快照 SHA256',
  `niche_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '赛道标识',
  `persona_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人设标识',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人设名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '人设描述',
  `default_pillars_json` json DEFAULT NULL COMMENT '默认内容支柱 [{"name":"...","percent":60}]',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_platform_hash_niche_persona` (`user_id`,`platform_key`,`answer_snapshot_hash`,`niche_key`,`persona_key`),
  KEY `idx_user_platform_hash_niche` (`user_id`,`platform_key`,`answer_snapshot_hash`,`niche_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_self_media_plan_publish_guide` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `plan_id` bigint unsigned NOT NULL COMMENT '自媒体方案ID',
  `plan_updated_at` datetime(3) NOT NULL COMMENT '生成时方案更新时间',
  `plan_content_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '运营方案内容 SHA-256 哈希',
  `main_platform` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主发平台',
  `main_platform_json` json NOT NULL COMMENT '主平台发布计划',
  `cold_start_json` json NOT NULL COMMENT '冷启动策略',
  `reposts_json` json NOT NULL COMMENT '一文多发计划列表',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_platform_hash` (`user_id`,`main_platform`,`plan_content_hash`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_self_media_plan_question` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `platform_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台key，如 xiaohongshu',
  `prompt_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '生成问题时使用的 prompt code',
  `question_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题标识',
  `question_text` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题文本',
  `options_json` json NOT NULL COMMENT '选项列表 [{"key":"...","label":"..."}]',
  `is_required` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否必填：0-否，1-是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `allow_other` tinyint NOT NULL DEFAULT '0' COMMENT '是否支持其他选项填写：0否 1是',
  `other_max_length` int NOT NULL DEFAULT '0' COMMENT '其他选项最大输入长度，allow_other=1时生效',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_platform_question` (`user_id`,`platform_key`,`question_key`),
  KEY `idx_user_platform` (`user_id`,`platform_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
