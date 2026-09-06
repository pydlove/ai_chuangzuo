-- ============================================================
-- 业务域：约稿征稿
-- 表数量：2
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_commission_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务唯一编号',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '稿件需求描述',
  `min_word_count` int unsigned NOT NULL COMMENT '最小字数',
  `max_word_count` int unsigned NOT NULL COMMENT '最大字数',
  `skill_hint` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '期望风格提示',
  `reward_coin` decimal(19,4) NOT NULL COMMENT '每篇采纳稿件奖励创作币',
  `needed_count` int unsigned NOT NULL COMMENT '计划采纳稿件数量',
  `adopted_count` int unsigned NOT NULL DEFAULT '0' COMMENT '已采纳稿件数量',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-招募中，1-已截止待采纳，2-已完成，3-已流局',
  `deadline_at` datetime(3) NOT NULL COMMENT '投稿截止时间',
  `selection_deadline_at` datetime(3) NOT NULL COMMENT '评选截止时间',
  `published_by` bigint unsigned NOT NULL COMMENT '发布管理员ID',
  `completed_at` datetime(3) DEFAULT NULL COMMENT '完成或流局时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID（=0）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `deleted_at` datetime(3) DEFAULT NULL COMMENT '删除时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_commission_task_no` (`task_no`),
  KEY `idx_u_commission_task_status_deadline` (`status`,`deadline_at`),
  KEY `idx_u_commission_task_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_commission_submission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint unsigned NOT NULL COMMENT '约稿任务ID',
  `submitter_id` bigint unsigned NOT NULL COMMENT '投稿用户ID',
  `article_biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台生成文章业务编号',
  `article_title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '投稿时文章标题快照',
  `article_body` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '投稿时文章正文快照',
  `word_count` int unsigned NOT NULL COMMENT '投稿时文章字数快照',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-投稿中，1-已采纳，2-未采纳，3-已撤回',
  `reward_coin` decimal(19,4) DEFAULT NULL COMMENT '实际发放创作币',
  `coin_record_biz_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创作币流水业务编号',
  `adopted_at` datetime(3) DEFAULT NULL COMMENT '采纳时间',
  `withdrawn_at` datetime(3) DEFAULT NULL COMMENT '撤回时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID（=0）',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `deleted_at` datetime(3) DEFAULT NULL COMMENT '删除时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_commission_submission_article` (`task_id`,`article_biz_no`),
  UNIQUE KEY `uk_u_commission_submission_coin_biz` (`coin_record_biz_no`),
  KEY `idx_u_commission_submission_task_status` (`task_id`,`status`),
  KEY `idx_u_commission_submission_user_created` (`submitter_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
