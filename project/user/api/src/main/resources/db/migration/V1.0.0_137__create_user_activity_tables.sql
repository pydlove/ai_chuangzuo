CREATE TABLE IF NOT EXISTS u_user_activity (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    last_active_time DATETIME(3) NOT NULL COMMENT '最近一次活跃时间',
    PRIMARY KEY (user_id),
    KEY idx_u_user_activity_last_active_time (last_active_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户活跃时间表（每次活跃 upsert 一行）';

CREATE TABLE IF NOT EXISTS u_daily_active (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    active_date DATE NOT NULL COMMENT '活跃日期',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次记录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_daily_active_user_date (user_id, active_date),
    KEY idx_u_daily_active_active_date (active_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户日活表（每日每用户一行，INSERT IGNORE 幂等）';
