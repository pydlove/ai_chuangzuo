CREATE TABLE IF NOT EXISTS u_leaderboard_income_submission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '申报用户ID',
    period_month CHAR(7) NOT NULL COMMENT '申报所属月份，格式 YYYY-MM',
    amount DECIMAL(19,4) NOT NULL COMMENT '申报金额（元）',
    platform VARCHAR(64) DEFAULT NULL COMMENT '自媒体平台：wechat / xiaohongshu / douyin / other',
    screenshot_paths JSON NOT NULL COMMENT '收益截图本地路径列表（多张）',
    audit_status TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
    audited_by BIGINT UNSIGNED DEFAULT NULL COMMENT '审核管理员ID',
    audited_at DATETIME(3) DEFAULT NULL COMMENT '审核时间',
    reject_reason VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_lis_biz_no (biz_no),
    KEY idx_u_lis_user_status (user_id, audit_status),
    KEY idx_u_lis_status_month (audit_status, period_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自媒体收入申报记录';

CREATE TABLE IF NOT EXISTS u_leaderboard_reward_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    leaderboard_type TINYINT UNSIGNED NOT NULL
        COMMENT '榜单类型：1-创作币榜，2-自媒体收入榜（月度）',
    period_month CHAR(7) NOT NULL COMMENT '榜单所属月份',
    rank_no INT UNSIGNED NOT NULL COMMENT '排名 1-10',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '获奖用户ID',
    amount DECIMAL(19,4) NOT NULL DEFAULT 100.0000 COMMENT '奖励金额（创作币）',
    coin_record_biz_no VARCHAR(64) DEFAULT NULL COMMENT '对应 u_user_coin_record.biz_no',
    granted_by BIGINT UNSIGNED NOT NULL COMMENT '发放管理员ID',
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发放时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_lrr_biz_no (biz_no),
    UNIQUE KEY uk_u_lrr_type_period_user (leaderboard_type, period_month, user_id)
        COMMENT '同一榜单同一周期同一用户只发一次',
    KEY idx_u_lrr_type_period_rank (leaderboard_type, period_month, rank_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='榜单奖励发放记录';
