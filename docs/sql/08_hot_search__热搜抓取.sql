-- ============================================================
-- 业务域：热搜抓取
-- 表数量：4
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `hot_search_config` (
  `id` bigint unsigned NOT NULL,
  `cron` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint unsigned NOT NULL DEFAULT '1',
  `top_n` int NOT NULL DEFAULT '50',
  `connect_timeout_millis` int NOT NULL DEFAULT '5000',
  `read_timeout_millis` int NOT NULL DEFAULT '10000',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `hot_search_crawl_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `trigger_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发方式：AUTO 定时 / MANUAL 手动',
  `started_at` datetime(3) NOT NULL COMMENT '开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功平台数',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '失败平台数',
  `total_fetched` int NOT NULL DEFAULT '0' COMMENT '抓取总条数',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS / PARTIAL / FAILED',
  `results_json` text COLLATE utf8mb4_unicode_ci COMMENT '各平台抓取结果 JSON',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '整体异常信息',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '手动触发人，定时任务为 0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_started_at` (`started_at` DESC),
  KEY `idx_trigger_type` (`trigger_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `hot_search_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台编码',
  `rank_num` int NOT NULL COMMENT '排名',
  `title` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '热搜标题',
  `hot_value` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '热度值字符串',
  `url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转链接',
  `search_count` bigint DEFAULT NULL COMMENT '搜索量数字',
  `snapshot_date` date NOT NULL COMMENT '快照日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hot_search_daily_platform_date_rank` (`platform_code`,`snapshot_date`,`rank_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `hot_search_platform` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台编码：douyin、toutiao、bilibili、weibo、baidu',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台名称',
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台图标 URL',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '展示排序',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-否，1-是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hot_search_platform_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
