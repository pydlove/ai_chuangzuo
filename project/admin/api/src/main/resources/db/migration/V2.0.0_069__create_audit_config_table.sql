CREATE TABLE IF NOT EXISTS a_audit_config (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '固定1',
    retention_days INT NOT NULL DEFAULT 30 COMMENT '日志保留天数',
    cleanup_cron VARCHAR(64) NOT NULL DEFAULT '0 0 3 * * ?',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志配置';

INSERT IGNORE INTO a_audit_config (id, retention_days, cleanup_cron, updated_by)
VALUES (1, 30, '0 0 3 * * ?', 0);
