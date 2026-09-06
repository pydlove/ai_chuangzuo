-- ============================================================
-- 业务域：创作学院
-- 表数量：3
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `t_article` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint unsigned NOT NULL,
  `title` varchar(128) NOT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `cover_image_url` varchar(512) NOT NULL DEFAULT '' COMMENT '封面图 URL',
  `content_type` varchar(16) NOT NULL COMMENT 'markdown 或 rich_text',
  `content` longtext NOT NULL,
  `status` varchar(16) NOT NULL COMMENT 'draft 或 published',
  `sort` int NOT NULL DEFAULT '0',
  `is_recommended` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否推荐 0=否 1=是',
  `is_free` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否免费：1=免费，0=付费',
  `required_plan_key` varchar(32) DEFAULT NULL COMMENT '最低所需套餐 key：basic/pro/flagship，仅付费时有值',
  `author_id` bigint unsigned DEFAULT NULL,
  `published_at` datetime(3) DEFAULT NULL,
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_cat_status_deleted` (`category_id`,`status`,`is_deleted`),
  KEY `idx_sort` (`sort`),
  KEY `idx_status_deleted` (`status`,`is_deleted`),
  KEY `idx_recommended_status_deleted` (`is_recommended`,`status`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_article_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` bigint unsigned DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `sort` int NOT NULL DEFAULT '0',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_parent_deleted` (`parent_id`,`is_deleted`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_learn_banner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `image_url` varchar(512) NOT NULL COMMENT '图片 URL',
  `link_url` varchar(512) NOT NULL DEFAULT '' COMMENT '点击跳转链接',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重，小在前',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
