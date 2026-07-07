CREATE TABLE IF NOT EXISTS hot_search_config (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    cron VARCHAR(64) NOT NULL,
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1,
    top_n INT NOT NULL DEFAULT 50,
    connect_timeout_millis INT NOT NULL DEFAULT 5000,
    read_timeout_millis INT NOT NULL DEFAULT 10000,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜抓取配置';

INSERT IGNORE INTO hot_search_config (id, cron, enabled, top_n, connect_timeout_millis, read_timeout_millis)
VALUES (1, '0 0 2 * * ?', 1, 50, 5000, 10000);
