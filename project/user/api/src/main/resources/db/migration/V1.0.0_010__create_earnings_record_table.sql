CREATE TABLE IF NOT EXISTS u_earnings_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    type VARCHAR(32) NOT NULL
        COMMENT 'USAGE / MILESTONE / LEADERBOARD_REWARD / INVITE_REWARD / OTHER',
    source_type VARCHAR(32) DEFAULT NULL
        COMMENT 'style_market / invite / leaderboard / manual',
    source_id VARCHAR(64) DEFAULT NULL
        COMMENT '上游业务 ID（解耦，不强外键）',
    title VARCHAR(128) NOT NULL
        COMMENT '列表展示标题，如 "「清新」风格被使用"',
    description VARCHAR(255) DEFAULT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 0
        COMMENT '0=未结算, 1=已结算',
    settlement_month VARCHAR(7) NOT NULL
        COMMENT 'YYYY-MM，归属月份（插入时按 created_at 计算）',
    settled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status_month (user_id, status, settlement_month),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='用户收益流水表（通用账本）';
