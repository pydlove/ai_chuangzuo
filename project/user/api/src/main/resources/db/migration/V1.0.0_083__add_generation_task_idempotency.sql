ALTER TABLE u_article
    ADD COLUMN task_id BIGINT UNSIGNED NULL COMMENT '关联生成任务ID',
    ADD UNIQUE KEY uk_u_article_task_id (task_id);

CREATE TABLE IF NOT EXISTS u_generation_task_refund (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT UNSIGNED NOT NULL COMMENT '生成任务ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    benefit_code VARCHAR(64) NOT NULL COMMENT '权益编码',
    refunded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '退款时间',
    UNIQUE KEY uk_u_generation_task_refund_task_benefit (task_id, benefit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务额度退款记录';
