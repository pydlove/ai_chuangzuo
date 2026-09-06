-- ============================================================
-- 业务域：用户与账号
-- 表数量：11
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户唯一编号',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `phone_verified` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '手机是否验证：0-否，1-是',
  `password_hash` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码哈希（BCrypt）',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人简介',
  `gender` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '性别：0-保密，1-男，2-女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `location` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所在地',
  `occupation` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职业',
  `real_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(18) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号',
  `real_name_verified` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否实名认证：0-否，1-是',
  `default_skill_json` json DEFAULT NULL COMMENT '默认 skill 引用，JSON {source, bizNo, id?, name, prompt, scope}',
  `invite_code` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个人邀请码',
  `coin_balance` decimal(19,4) NOT NULL DEFAULT '0.0000' COMMENT '创作币余额（正为可用）',
  `user_status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `user_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '用户类型：0-机器人，1-真实用户',
  `membership_expire_at` datetime DEFAULT NULL COMMENT '会员到期时刻（到期日次日00:00，NULL=非会员）',
  `membership_plan` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会员套餐：monthly/quarterly/yearly 等，NULL=无套餐',
  `email_verified` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '邮箱是否验证：0-否，1-是',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_user_biz_no` (`biz_no`),
  UNIQUE KEY `uk_u_user_phone` (`phone`),
  UNIQUE KEY `uk_u_user_email` (`email`,((case when (`is_deleted` = 0) then 0 else NULL end))),
  UNIQUE KEY `uk_u_user_invite_code` (`invite_code`,((case when (`is_deleted` = 0) then 0 else NULL end))),
  KEY `idx_u_user_type` (`user_type`),
  KEY `idx_user_membership_expire_at` (`membership_expire_at`),
  KEY `idx_user_membership_plan` (`membership_plan`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_login_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '用户ID，0表示未登录',
  `login_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '类型：1-密码登录，2-注册登录',
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User-Agent',
  `login_status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：0-失败，1-成功',
  `fail_reason` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_u_user_login_log_user_id` (`user_id`),
  KEY `idx_u_user_login_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_audit_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `action_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型，如 article/create',
  `module` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块，如 article, skill',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_uri` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_params` text COLLATE utf8mb4_unicode_ci COMMENT 'URL参数，截断1024',
  `request_body` text COLLATE utf8mb4_unicode_ci COMMENT '请求体，截断2048并脱敏',
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status_code` int DEFAULT NULL,
  `error_msg` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_ms` int unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_u_user_audit_log_user_id_created_at` (`user_id`,`created_at`),
  KEY `idx_u_user_audit_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_invite_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inviter_id` bigint unsigned NOT NULL COMMENT '邀请人ID',
  `invitee_id` bigint unsigned NOT NULL COMMENT '被邀请人ID',
  `invite_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邀请码',
  `source_type` tinyint unsigned NOT NULL DEFAULT '2' COMMENT '来源：1-链接，2-手动填写',
  `effective_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-待验证，1-有效，2-无效',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_user_invite_relation_invitee_id` (`invitee_id`),
  KEY `idx_u_user_invite_relation_inviter_id` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_ip_register_limit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端IP',
  `register_count` int unsigned NOT NULL DEFAULT '0' COMMENT '累计成功注册数',
  `is_blocked` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否永久封禁：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_ip_register_limit_client_ip` (`client_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_qr_login_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '二维码唯一标识',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-待扫描 1-已扫描 2-已授权 3-已取消 4-已过期',
  `scanner_user_id` bigint unsigned DEFAULT NULL COMMENT '扫描者用户ID',
  `scanner_nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扫描者昵称',
  `client_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者IP',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者User-Agent',
  `expired_at` datetime(3) NOT NULL COMMENT '过期时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否 1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_qr_login_session_qr_code` (`qr_code`),
  KEY `idx_u_qr_login_session_status_expired` (`status`,`expired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_wechat_bind` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `openid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '微信openid',
  `unionid` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信unionid',
  `app_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '微信公众号appid',
  `nickname` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信昵称',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信头像',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid_app` (`openid`,`app_id`),
  UNIQUE KEY `uk_user_app` (`user_id`,`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_wechat_bind_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bind_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '绑定码',
  `openid` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信openid',
  `app_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '微信公众号appid',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0待绑定 1已激活 2已绑定',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind_code` (`bind_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_wechat_bind_qr_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_str` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '二维码场景值',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0待扫描 1已扫描 2已绑定',
  `openid` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扫描用户openid',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_str` (`scene_str`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_feedback` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '提交人',
  `type` varchar(32) NOT NULL COMMENT '反馈类型',
  `content` varchar(2000) NOT NULL COMMENT '反馈正文',
  `star_rating` tinyint unsigned DEFAULT NULL COMMENT '评价星级 1-5',
  `reply_content` varchar(2000) DEFAULT NULL COMMENT '管理员回复',
  `reply_admin_id` bigint unsigned DEFAULT NULL COMMENT '回复管理员',
  `replied_at` datetime(3) DEFAULT NULL COMMENT '回复时间',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '0 待回复 / 1 已回复',
  `is_show_on_homepage` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否展示到首页：0-否，1-是',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_u_feedback_user_created` (`user_id`,`created_at`),
  KEY `idx_u_feedback_status_created` (`status`,`created_at`),
  KEY `idx_u_feedback_show_homepage` (`is_show_on_homepage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_share_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scene_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享场景：lottery-抽奖活动，invite-邀请有礼',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '配置标题（管理端展示用）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分享文案，支持占位符',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，越小越靠前',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_by` bigint NOT NULL DEFAULT '0' COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_key` (`scene_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
