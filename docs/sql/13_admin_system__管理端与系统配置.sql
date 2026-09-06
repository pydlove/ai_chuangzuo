-- ============================================================
-- 业务域：管理端与系统配置
-- 表数量：22
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `a_admin_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
  `password_hash` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码哈希（BCrypt）',
  `real_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `last_login_at` datetime(3) DEFAULT NULL COMMENT '最后登录时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_admin_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_admin_user_role_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_user_id` bigint unsigned NOT NULL COMMENT '管理员ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_admin_user_role_rel` (`admin_user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_role_permission_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_role_permission_rel` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码',
  `permission_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `resource_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '资源类型：1-菜单，2-按钮，3-接口',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父权限ID，0表示顶级',
  `sort_order` int unsigned NOT NULL DEFAULT '0' COMMENT '排序',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_admin_login_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_user_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '管理员ID，0表示未登录',
  `login_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '类型：1-密码登录',
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User-Agent',
  `login_status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-失败，1-成功',
  `fail_reason` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_a_admin_login_log_admin_user_id` (`admin_user_id`),
  KEY `idx_a_admin_login_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_access_control` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '规则类型：1-IP，2-账号',
  `list_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '名单类型：1-黑名单，2-白名单',
  `rule_value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则值：IP地址或账号标识（用户ID/邮箱）',
  `rule_status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_access_control_rule` (`rule_type`,`list_type`,`rule_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_audit_config` (
  `id` bigint unsigned NOT NULL COMMENT '固定1',
  `retention_days` int NOT NULL DEFAULT '30' COMMENT '日志保留天数',
  `cleanup_cron` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0 0 3 * * ?',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_rate_limit_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `is_login_rate_limit_enabled` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否启用登录限流：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `deleted_at` datetime(3) DEFAULT NULL COMMENT '删除时间，NULL表示未删除',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `nickname_check_daily_limit` int NOT NULL DEFAULT '10' COMMENT '平台账号检测每日次数上限',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_scheduled_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务唯一标识',
  `task_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务说明',
  `module` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属模块：admin-管理端，user-用户端',
  `trigger_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发类型：cron-表达式触发，fixed_delay-固定间隔',
  `expression` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '触发表达式：cron 或 fixedDelay 毫秒值',
  `bean_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Spring Bean 名称',
  `method_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方法名',
  `enabled` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否启用：0-否，1-是',
  `last_run_at` datetime(3) DEFAULT NULL COMMENT '上次执行时间',
  `last_run_status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上次执行状态：success-成功，failed-失败，running-执行中',
  `last_run_message` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上次执行结果摘要',
  `sort_order` int unsigned NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_a_scheduled_task_task_key` (`task_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_scheduled_task_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint unsigned NOT NULL COMMENT '任务ID',
  `trigger_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发方式：auto-自动，manual-手动',
  `started_at` datetime(3) NOT NULL COMMENT '开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `run_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行状态：success-成功，failed-失败',
  `message` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果摘要或异常信息',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '触发人ID，auto 为 0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_a_scheduled_task_log_task_id` (`task_id`),
  KEY `idx_a_scheduled_task_log_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_scheduled_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务唯一标识',
  `task_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务说明',
  `module` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属模块：user-用户端',
  `trigger_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发类型：cron-表达式触发，fixed_delay-固定间隔',
  `expression` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '触发表达式：cron 或 fixedDelay 毫秒值',
  `bean_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Spring Bean 名称',
  `method_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方法名',
  `enabled` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否启用：0-否，1-是',
  `last_run_at` datetime(3) DEFAULT NULL COMMENT '上次执行时间',
  `last_run_status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上次执行状态：success-成功，failed-失败，running-执行中',
  `last_run_message` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上次执行结果摘要',
  `sort_order` int unsigned NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_scheduled_task_task_key` (`task_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_scheduled_task_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint unsigned NOT NULL COMMENT '任务ID',
  `trigger_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发方式：auto-自动，manual-手动',
  `started_at` datetime(3) NOT NULL COMMENT '开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `run_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行状态：success-成功，failed-失败',
  `message` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果摘要或异常信息',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '触发人ID，auto 为 0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_u_scheduled_task_log_task_id` (`task_id`),
  KEY `idx_u_scheduled_task_log_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_sms_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'aliyun' COMMENT '短信服务商：aliyun',
  `access_key_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AccessKey ID',
  `access_key_secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '加密后的 AccessKey Secret',
  `sign_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '短信签名',
  `template_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '短信模板 Code',
  `region_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'cn-hangzhou' COMMENT '阿里云区域 ID',
  `enabled` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否启用短信：0-否，1-是',
  `send_interval_seconds` int unsigned NOT NULL DEFAULT '60' COMMENT '同一手机号两次发送最小间隔（秒）',
  `daily_max_per_phone` int unsigned NOT NULL DEFAULT '10' COMMENT '同一手机号每天最多发送次数',
  `daily_max_per_ip` int unsigned NOT NULL DEFAULT '50' COMMENT '同一 IP 每天最多发送次数',
  `global_daily_max` int unsigned NOT NULL DEFAULT '1000' COMMENT '全站每天最多发送次数',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_upgrade_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `script_root_dir` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '/Users/panyong/aio_project/ai_chuangzuo/scripts' COMMENT '脚本根目录',
  `server_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '服务器 IP',
  `server_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SSH 用户名',
  `server_password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Jasypt 加密后的 SSH 密码',
  `ssh_key_path` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SSH 私钥路径',
  `command_timeout_seconds` int unsigned NOT NULL DEFAULT '600' COMMENT '脚本执行超时秒数',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_upgrade_job_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `script_relative_path` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '脚本相对根目录路径',
  `script_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '脚本文件名',
  `trigger_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'manual' COMMENT '触发方式：manual',
  `run_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'running' COMMENT '状态：running/success/failed/timeout',
  `started_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `exit_code` int DEFAULT NULL COMMENT '退出码',
  `stdout` longtext COLLATE utf8mb4_unicode_ci COMMENT '标准输出（可能被截断）',
  `stderr` longtext COLLATE utf8mb4_unicode_ci COMMENT '标准错误（可能被截断）',
  `output_truncated` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '输出是否被截断：0-否，1-是',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '执行人ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_status_started` (`run_status`,`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_wechat_official_account_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，固定为 1',
  `app_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '公众号 AppID',
  `app_secret` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '公众号 AppSecret（Jasypt 加密）',
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '服务器配置 Token',
  `plaintext_mode` tinyint NOT NULL DEFAULT '1' COMMENT '是否明文模式：0-否 1-是',
  `enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否启用：0-否 1-是',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
  `updated_by` bigint NOT NULL DEFAULT '0' COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_home_banner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `image_url` varchar(512) NOT NULL COMMENT '图片 URL',
  `link_url` varchar(512) NOT NULL DEFAULT '' COMMENT '点击跳转链接',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重，小在前',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '卡片标题',
  `subtitle` varchar(256) NOT NULL DEFAULT '' COMMENT '卡片副标题',
  `cta_text` varchar(64) NOT NULL DEFAULT '' COMMENT 'CTA 按钮文字',
  `theme` varchar(16) NOT NULL DEFAULT 'light' COMMENT '主题: light/dark/auto',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0 禁用 / 1 启用',
  `start_time` datetime(3) DEFAULT NULL COMMENT '上线时间, NULL=立即上线',
  `end_time` datetime(3) DEFAULT NULL COMMENT '下线时间, NULL=永久',
  PRIMARY KEY (`id`),
  KEY `idx_a_home_banner_sort` (`sort`),
  KEY `idx_a_home_banner_status_sort` (`status`,`sort`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_testimonial` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `avatar_url` varchar(512) NOT NULL DEFAULT '' COMMENT '头像 URL',
  `name` varchar(64) NOT NULL COMMENT '评价人姓名',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '评价人身份/职位',
  `star_rating` tinyint unsigned NOT NULL DEFAULT '5' COMMENT '星级：1-5',
  `review_text` varchar(2048) NOT NULL COMMENT '评价内容',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重，小在前',
  `is_enabled` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人 ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人 ID',
  PRIMARY KEY (`id`),
  KEY `idx_a_testimonial_sort` (`sort`),
  KEY `idx_a_testimonial_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_export_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `template_key` varchar(64) NOT NULL COMMENT '模板标识，如 wechat / xiaohongshu-list',
  `name` varchar(128) NOT NULL COMMENT '模板名称',
  `platform` varchar(32) NOT NULL DEFAULT 'general' COMMENT '平台：wechat/xiaohongshu/toutiao/baijiahao/zhihu/douyin/general',
  `description` varchar(256) DEFAULT NULL COMMENT '模板描述',
  `bg_color` varchar(16) NOT NULL DEFAULT '#fff' COMMENT '卡片背景色',
  `text_color` varchar(16) NOT NULL DEFAULT '#1a1a1a' COMMENT '卡片文字颜色',
  `visual_style_json` json DEFAULT NULL COMMENT '视觉预设 JSON（标题/小标题/正文/callout 样式）',
  `signature_text` varchar(128) DEFAULT NULL COMMENT '签名文本，如 — 完 —',
  `signature_position` varchar(8) NOT NULL DEFAULT 'end' COMMENT '签名位置：start/end',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `tier` varchar(16) NOT NULL DEFAULT 'basic' COMMENT '所需套餐：basic/pro/flagship',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_export_template_key` (`template_key`),
  KEY `idx_export_template_platform` (`platform`,`status`,`is_deleted`),
  KEY `idx_export_template_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_export_template_param` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `param_key` varchar(64) NOT NULL COMMENT '参数 key，对应 visual_style_json 里的字段名（如 titleColor）',
  `display_label` varchar(64) NOT NULL COMMENT 'admin 端显示名（如「标题颜色」）',
  `field_type` varchar(16) NOT NULL COMMENT '控件类型：color/number/text/select/border',
  `group_label` varchar(32) NOT NULL COMMENT '分组：标题/Meta/正文/小标题/高亮块/整体/排版',
  `default_value` varchar(255) DEFAULT NULL COMMENT '新建模板时填入 visual_style_json 的默认值',
  `options_json` varchar(1024) DEFAULT NULL COMMENT 'select 类型的可选项 JSON 数组',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_export_template_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `c_platform` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台唯一键，如 wechat / xiaohongshu',
  `platform_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台显示名，如 公众号',
  `description` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台简介',
  `recommend_words` int unsigned NOT NULL DEFAULT '0' COMMENT '推荐字数',
  `trait` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台风格/特征描述',
  `word_count_presets_json` json DEFAULT NULL COMMENT '平台专属字数档位配置 JSON',
  `sort_order` int unsigned NOT NULL DEFAULT '0' COMMENT '排序，越小越靠前',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `is_default` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否默认选中：0-否，1-是',
  `icon_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台图标 URL',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID，0表示系统或未知',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID，0表示系统或未知',
  `deleted_at` datetime(3) DEFAULT NULL COMMENT '删除时间，NULL表示未删除',
  `tagline` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '一句话卖点，如「图文种草社区，女性用户多，适合分享生活经验」',
  `content_form_json` json DEFAULT NULL COMMENT '内容形式，如 ["图文笔记","短视频"]',
  `monetization_json` json DEFAULT NULL COMMENT '主要收益，如 ["品牌广告","带货分佣"]',
  `threshold` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变现门槛',
  `best_for` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适合谁',
  `reason` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提示/推荐理由',
  `monetization_ease` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变现难度，如 中等',
  `time_to_income` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预计周期，如 2-4个月',
  `income_range` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收入空间，如 几千~几万/月',
  `difficulty` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '运营难度，如 中',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_c_platform_platform_key` (`platform_key`),
  KEY `idx_c_platform_status_sort_order` (`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
