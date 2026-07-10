-- 创作运行时配置：worker 线程池、轮询、lease、重试、归档 cron 等
-- 单行（id=1），由 admin 端 GET/PUT 改写；worker 启动时读 + 每 10s 刷新

CREATE TABLE IF NOT EXISTS a_generation_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键（固定 1）',
    pool_size INT UNSIGNED NOT NULL DEFAULT 2 COMMENT 'worker 线程池大小（1-10，重启生效）',
    claim_batch_size INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '每轮单次拉取任务数（1-10）',
    lease_minutes INT UNSIGNED NOT NULL DEFAULT 5 COMMENT 'lease 持续分钟（1-60）',
    max_retry INT UNSIGNED NOT NULL DEFAULT 3 COMMENT '单任务最大重试次数（1-10）',
    poll_interval_ms INT UNSIGNED NOT NULL DEFAULT 500 COMMENT '空轮询间隔 ms（100-5000）',
    retention_cron VARCHAR(64) NOT NULL DEFAULT '0 0 3 * * ?' COMMENT '归档定时任务 cron 表达式',
    worker_id VARCHAR(64) NOT NULL DEFAULT 'worker-1' COMMENT 'worker 实例 ID（单实例约定）',
    remark VARCHAR(256) DEFAULT NULL COMMENT '备注',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID（=0）',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作运行时配置';

-- 插入默认配置行（id=1）
INSERT INTO a_generation_config (id, pool_size, claim_batch_size, lease_minutes, max_retry, poll_interval_ms, retention_cron, worker_id, created_by, updated_by)
VALUES (1, 2, 1, 5, 3, 500, '0 0 3 * * ?', 'worker-1', 0, 0)
ON DUPLICATE KEY UPDATE id = id;
