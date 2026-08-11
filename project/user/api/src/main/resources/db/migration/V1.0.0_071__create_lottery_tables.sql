CREATE TABLE IF NOT EXISTS u_lottery_campaign (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '活动名称',
    description VARCHAR(256) DEFAULT NULL COMMENT '活动描述',
    start_time DATETIME(3) NOT NULL COMMENT '开始时间',
    end_time DATETIME(3) NOT NULL COMMENT '结束时间',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-draft,1-ongoing,2-ended,3-disabled',
    free_draws_per_user INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '每轮免费次数',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_lottery_campaign_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖活动轮次表';

CREATE TABLE IF NOT EXISTS u_lottery_prize_tier (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tier_key VARCHAR(32) NOT NULL COMMENT '奖项标识',
    tier_name VARCHAR(64) NOT NULL COMMENT '奖项名称',
    probability DECIMAL(10,8) NOT NULL COMMENT '中奖概率',
    max_win_count INT UNSIGNED DEFAULT NULL COMMENT '全局可中次数上限，NULL表示不限',
    remaining_win_count INT UNSIGNED DEFAULT NULL COMMENT '剩余可中次数',
    reward_type VARCHAR(16) NOT NULL COMMENT '奖励类型：coin/membership/coupon/none',
    reward_value_json JSON NOT NULL COMMENT '奖励参数',
    code_prefix VARCHAR(16) DEFAULT NULL COMMENT '兑换码前缀',
    code_length INT UNSIGNED DEFAULT NULL COMMENT '兑换码总字符数（含前缀）',
    code_validity_days INT UNSIGNED NOT NULL DEFAULT 30 COMMENT '兑换码有效期天数',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_prize_tier_campaign_key (campaign_id, tier_key),
    KEY idx_lottery_prize_tier_campaign_status (campaign_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖奖项配置表';

CREATE TABLE IF NOT EXISTS u_lottery_draw_chance (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    chance_type VARCHAR(16) NOT NULL COMMENT '次数类型：free/invite',
    source_invite_relation_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'invite来源关系ID',
    status VARCHAR(16) NOT NULL DEFAULT 'available' COMMENT '状态：available/used',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    used_at DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_draw_chance_free (campaign_id, user_id, chance_type),
    KEY idx_lottery_draw_chance_user_campaign (user_id, campaign_id, status),
    KEY idx_lottery_draw_chance_invite (source_invite_relation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖次数池';

CREATE TABLE IF NOT EXISTS u_lottery_draw_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    tier_id BIGINT UNSIGNED DEFAULT NULL COMMENT '命中奖项ID',
    code_id BIGINT UNSIGNED DEFAULT NULL COMMENT '生成兑换码ID',
    draw_type VARCHAR(16) NOT NULL COMMENT '抽奖类型：free/invite',
    invite_relation_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'invite来源关系ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_draw_record_biz_no (biz_no),
    KEY idx_lottery_draw_record_user_campaign (user_id, campaign_id),
    KEY idx_lottery_draw_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖记录表';

CREATE TABLE IF NOT EXISTS u_lottery_redemption_code (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(64) NOT NULL COMMENT '兑换码',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tier_id BIGINT UNSIGNED NOT NULL COMMENT '奖项ID',
    drawer_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '中奖人用户ID',
    reward_type VARCHAR(16) NOT NULL COMMENT '奖励类型',
    reward_value_json JSON NOT NULL COMMENT '奖励参数快照',
    status VARCHAR(16) NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
    used_by BIGINT UNSIGNED DEFAULT NULL COMMENT '兑换人用户ID',
    used_at DATETIME(3) DEFAULT NULL,
    expires_at DATETIME(3) NOT NULL COMMENT '过期时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_redemption_code_code (code),
    KEY idx_lottery_redemption_code_campaign (campaign_id),
    KEY idx_lottery_redemption_code_drawer (drawer_user_id),
    KEY idx_lottery_redemption_code_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖兑换码表';

CREATE TABLE IF NOT EXISTS u_lottery_display_winner (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tier_id BIGINT UNSIGNED DEFAULT NULL COMMENT '奖项ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '展示昵称',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '展示头像',
    prize_name VARCHAR(64) NOT NULL COMMENT '展示奖品名',
    win_time DATETIME(3) NOT NULL COMMENT '展示时间',
    is_real TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-机器人/运营配置，1-真实中奖',
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0-隐藏，1-展示',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_lottery_display_winner_campaign (campaign_id, status, win_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='中奖展示墙';

CREATE TABLE IF NOT EXISTS u_lottery_risk_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    campaign_id BIGINT UNSIGNED DEFAULT NULL COMMENT '活动ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    action VARCHAR(16) NOT NULL COMMENT '动作：draw/redeem/invite',
    risk_type VARCHAR(32) NOT NULL COMMENT '风控类型',
    detail_json JSON DEFAULT NULL COMMENT '详情',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_lottery_risk_log_user_action (user_id, action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖风控日志';
