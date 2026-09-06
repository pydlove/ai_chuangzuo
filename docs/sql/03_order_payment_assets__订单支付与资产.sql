-- ============================================================
-- 业务域：订单支付与资产
-- 表数量：11
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE `u_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号：SUB{yyMMdd}{6位随机}',
  `user_id` bigint unsigned NOT NULL COMMENT '下单用户ID',
  `plan_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '套餐：basic/pro/flagship',
  `cycle` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期：month/quarter/year',
  `started_at` date DEFAULT NULL COMMENT '会员开始日期',
  `expires_at` date DEFAULT NULL COMMENT '会员结束日期',
  `amount` decimal(19,4) NOT NULL COMMENT '订单金额',
  `coin_amount` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创作币抵扣数量',
  `coin_discount` decimal(19,4) NOT NULL DEFAULT '0.0000' COMMENT '创作币抵扣金额（元）',
  `coupon_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用的优惠券码',
  `coupon_discount` decimal(19,4) DEFAULT NULL COMMENT '优惠券抵扣金额',
  `total_amount` decimal(19,4) NOT NULL DEFAULT '0.0000' COMMENT '订单名义总金额（现金+创作币抵扣）',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态：0-待支付，1-已支付，2-已退款，3-已取消',
  `paid_at` datetime(3) DEFAULT NULL COMMENT '支付时间',
  `refunded_at` datetime(3) DEFAULT NULL COMMENT '退款时间',
  `refund_reason` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退款原因',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '实际退款金额',
  `admin_remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员操作备注',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '操作管理员ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `payment_method` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '支付方式：xunhupay',
  `third_party_trade_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方交易流水号',
  `third_party_refund_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方退款单号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_order_order_no` (`order_no`),
  KEY `idx_u_order_user_id` (`user_id`),
  KEY `idx_u_order_status` (`status`),
  KEY `idx_u_order_paid_at` (`paid_at`),
  KEY `idx_u_order_created_at` (`created_at`),
  KEY `idx_u_order_started_at` (`started_at`),
  KEY `idx_u_order_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_payment_notify_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '本地订单编号',
  `nonce_str` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方通知唯一标识',
  `trade_order_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '第三方交易流水号',
  `raw_body` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回调原始内容',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '处理结果：0-失败，1-成功',
  `error_msg` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理失败原因',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_payment_notify_log_order_nonce` (`order_no`,`nonce_str`),
  KEY `idx_u_payment_notify_log_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_coupon` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `coupon_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '券码',
  `coupon_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：percent/fixed_amount',
  `discount_value` decimal(10,4) NOT NULL COMMENT '折扣值',
  `applicable_cycle` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'all' COMMENT '适用周期',
  `applicable_plan` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'all' COMMENT '适用套餐',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
  `valid_start` datetime(3) NOT NULL COMMENT '有效期开始',
  `valid_end` datetime(3) NOT NULL COMMENT '有效期结束',
  `used_order_id` bigint unsigned DEFAULT NULL COMMENT '使用订单ID',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon_code` (`coupon_code`),
  KEY `idx_user_coupon_user_status` (`user_id`,`status`,`valid_end`),
  KEY `idx_user_coupon_order` (`used_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_payment_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'xunhupay' COMMENT '支付服务商：xunhupay',
  `app_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '虎皮椒 App ID',
  `app_secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Jasypt 加密后的虎皮椒 App Secret',
  `gateway_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT 'https://api.xunhupay.com/payment/do.html' COMMENT '虎皮椒下单网关地址',
  `refund_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT 'https://api.xunhupay.com/payment/refund.html' COMMENT '虎皮椒退款网关地址',
  `notify_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '虎皮椒异步通知地址',
  `return_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '虎皮椒支付完成回跳地址',
  `enabled` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否启用支付：0-否，1-是',
  `test_mode` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '是否测试模式：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_user_coin_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号',
  `user_id` bigint unsigned NOT NULL COMMENT '所属用户ID',
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型：leaderboard_reward / admin_adjust / invite_reward 等',
  `direction` tinyint unsigned NOT NULL COMMENT '方向：1-收入，2-支出',
  `amount` decimal(19,4) NOT NULL COMMENT '本次金额（始终为正）',
  `balance_after` decimal(19,4) NOT NULL COMMENT '本次入账后余额快照',
  `ref_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务ID',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `biz_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '业务发生时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_user_coin_record_biz_no` (`biz_no`),
  UNIQUE KEY `uk_u_user_coin_record_type_ref` (`biz_type`,`ref_id`),
  KEY `idx_u_user_coin_record_user_time` (`user_id`,`biz_time`),
  KEY `idx_u_user_coin_record_user_type` (`user_id`,`biz_type`),
  KEY `idx_u_user_coin_record_biz_time` (`biz_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_generation_task_refund` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL COMMENT '生成任务ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `benefit_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权益编码',
  `refunded_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '退款时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_generation_task_refund_task_benefit` (`task_id`,`benefit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_earnings_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `type` varchar(32) NOT NULL COMMENT 'USAGE / MILESTONE / LEADERBOARD_REWARD / INVITE_REWARD / OTHER',
  `source_type` varchar(32) DEFAULT NULL COMMENT 'style_market / invite / leaderboard / manual',
  `source_id` varchar(64) DEFAULT NULL COMMENT '上游业务 ID（解耦，不强外键）',
  `plan_key` varchar(32) DEFAULT NULL COMMENT '套餐 key：basic/pro/flagship（邀请奖励适用）',
  `plan_name` varchar(64) DEFAULT NULL COMMENT '套餐显示名（邀请奖励适用）',
  `cycle` varchar(16) DEFAULT NULL COMMENT '周期：month/quarter/year（邀请奖励适用）',
  `order_amount` decimal(10,2) DEFAULT NULL COMMENT '被邀请人订单金额（邀请奖励适用）',
  `commission_rate` decimal(5,4) DEFAULT NULL COMMENT '返佣比例：0.1000=10%, 0.0500=5%（邀请奖励适用）',
  `is_first_purchase` tinyint DEFAULT NULL COMMENT '是否首购：0-续费，1-首购（邀请奖励适用）',
  `title` varchar(128) NOT NULL COMMENT '列表展示标题，如 "「清新」风格被使用"',
  `description` varchar(255) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL DEFAULT '0.00',
  `settlement_month` varchar(7) NOT NULL COMMENT 'YYYY-MM，归属月份（插入时按 created_at 计算）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-未删除，1-已删除',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '结算状态：0-未结算，1-已结算',
  `settled_at` datetime(3) DEFAULT NULL COMMENT '结算时间',
  `biz_no` varchar(64) NOT NULL COMMENT '收益流水号（业务唯一编号）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_earnings_record_biz_no` (`biz_no`),
  KEY `idx_user_status_month` (`user_id`,`settlement_month`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  KEY `idx_u_earnings_record_is_deleted` (`is_deleted`),
  KEY `idx_user_settlement_month` (`user_id`,`settlement_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_withdraw_request` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号',
  `user_id` bigint unsigned NOT NULL COMMENT '申请人用户ID',
  `amount` decimal(19,4) NOT NULL COMMENT '提现创作币数量',
  `account` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收款账号（支付宝）',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收款人真实姓名',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态：1-审核中，2-已通过，3-已拒绝',
  `processed_at` datetime(3) DEFAULT NULL COMMENT '处理时间',
  `processed_by` bigint unsigned DEFAULT NULL COMMENT '处理人ID（管理员）',
  `result_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理结果备注（如拒绝原因）',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '租户ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_withdraw_request_biz_no` (`biz_no`),
  KEY `idx_u_withdraw_request_user_status` (`user_id`,`status`),
  KEY `idx_u_withdraw_request_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_leaderboard_income_submission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号',
  `user_id` bigint unsigned NOT NULL COMMENT '申报用户ID',
  `period_month` char(7) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申报所属月份，格式 YYYY-MM',
  `amount` decimal(19,4) NOT NULL COMMENT '申报金额（元）',
  `platform` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '自媒体平台：wechat / xiaohongshu / douyin / other',
  `screenshot_paths` json NOT NULL COMMENT '收益截图本地路径列表（多张）',
  `audit_status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
  `audited_by` bigint unsigned DEFAULT NULL COMMENT '审核管理员ID',
  `audited_at` datetime(3) DEFAULT NULL COMMENT '审核时间',
  `reject_reason` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_lis_biz_no` (`biz_no`),
  KEY `idx_u_lis_user_status` (`user_id`,`audit_status`),
  KEY `idx_u_lis_status_month` (`audit_status`,`period_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `u_leaderboard_reward_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务唯一编号',
  `leaderboard_type` tinyint unsigned NOT NULL COMMENT '榜单类型：1-创作币榜，2-自媒体收入榜（月度）',
  `period_month` char(7) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '榜单所属月份',
  `rank_no` int unsigned NOT NULL COMMENT '排名 1-10',
  `user_id` bigint unsigned NOT NULL COMMENT '获奖用户ID',
  `amount` decimal(19,4) NOT NULL DEFAULT '100.0000' COMMENT '奖励金额（创作币）',
  `coin_record_biz_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对应 u_user_coin_record.biz_no',
  `granted_by` bigint unsigned NOT NULL COMMENT '发放管理员ID',
  `granted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发放时间',
  `tenant_id` bigint unsigned NOT NULL DEFAULT '0',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` bigint unsigned NOT NULL DEFAULT '0',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_u_lrr_biz_no` (`biz_no`),
  UNIQUE KEY `uk_u_lrr_type_period_user` (`leaderboard_type`,`period_month`,`user_id`) COMMENT '同一榜单同一周期同一用户只发一次',
  KEY `idx_u_lrr_type_period_rank` (`leaderboard_type`,`period_month`,`rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `a_leaderboard_reward_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID，固定为1',
  `reward_top_limit` int unsigned NOT NULL DEFAULT '3' COMMENT '可发放奖励的名次上限，如 TOP 3',
  `reward_amount` decimal(19,4) NOT NULL DEFAULT '500.0000' COMMENT '每名奖励的创作币数量',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `updated_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '更新人ID',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
