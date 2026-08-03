CREATE TABLE IF NOT EXISTS hot_search_crawl_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trigger_type VARCHAR(16) NOT NULL COMMENT '触发方式：AUTO 定时 / MANUAL 手动',
    started_at DATETIME(3) NOT NULL COMMENT '开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功平台数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败平台数',
    total_fetched INT NOT NULL DEFAULT 0 COMMENT '抓取总条数',
    status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS / PARTIAL / FAILED',
    results_json TEXT COMMENT '各平台抓取结果 JSON',
    error_msg TEXT COMMENT '整体异常信息',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '手动触发人，定时任务为 0',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_started_at (started_at DESC),
    INDEX idx_trigger_type (trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜抓取执行日志';
