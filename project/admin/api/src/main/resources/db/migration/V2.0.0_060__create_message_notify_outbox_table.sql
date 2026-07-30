-- 消息通知 outbox：admin 端各业务模块把要推给用户的通知写进来，
-- 由 NotifyOutboxDispatcherJob（Spring Scheduler）异步派发到 user-api。
-- 用 DB + 轮询替代独立消息队列，符合项目"无 Redis/RabbitMQ"约束。

CREATE TABLE IF NOT EXISTS a_message_notify_outbox (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：generation_completed / generation_failed / ...',
    biz_id BIGINT UNSIGNED NOT NULL COMMENT '业务主键（如 task_id）',
    target_user_id BIGINT UNSIGNED NOT NULL COMMENT '目标用户ID',
    payload JSON NOT NULL COMMENT '透传给 user-api notify-completion 的请求体',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-PENDING 1-SENT 2-FAILED',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
    next_retry_at DATETIME(3) NOT NULL COMMENT '下次重试时间',
    last_error VARCHAR(512) DEFAULT NULL COMMENT '最后一次失败原因',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    sent_at DATETIME(3) DEFAULT NULL COMMENT '成功派发时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    -- 派发器按状态 + 下次重试时间扫表（支持跳过行锁未抢到的记录）
    KEY idx_status_next_retry (status, next_retry_at),
    -- 业务幂等/排重用
    KEY idx_biz_type_id (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知 outbox';
