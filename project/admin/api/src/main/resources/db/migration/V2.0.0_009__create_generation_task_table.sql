-- 文章生成队列表：跨端共享，user-api 提交，admin-api worker 处理
-- 表前缀：a_（admin 业务表）

CREATE TABLE IF NOT EXISTS a_generation_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号（对外暴露，如 GA20260709xxxx）',
    target_user_id BIGINT UNSIGNED NOT NULL COMMENT '发起任务的用户ID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-queued，1-processing，2-completed，3-failed',
    model_config_id BIGINT UNSIGNED NOT NULL COMMENT '使用的AI模型配置ID',
    prompt_template_id BIGINT UNSIGNED NOT NULL COMMENT '使用的提示词模板ID（提交时快照）',
    input_param JSON NOT NULL COMMENT '输入参数：title/description/platform/wordCount/styleRef/toneTags',
    word_limit_target INT UNSIGNED NOT NULL DEFAULT 3000 COMMENT '用户要求字数（≤3000）',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry INT UNSIGNED NOT NULL DEFAULT 3 COMMENT '最大重试次数，默认3',
    locked_at DATETIME(3) DEFAULT NULL COMMENT 'worker 锁定（开始处理）时间',
    locked_by VARCHAR(64) DEFAULT NULL COMMENT 'worker 实例ID，当前单实例固定 worker-1',
    lease_until DATETIME(3) DEFAULT NULL COMMENT 'lease 超时时刻（locked_at + 5分钟）',
    failed_reason VARCHAR(512) DEFAULT NULL COMMENT '最后一次失败原因',
    completed_at DATETIME(3) DEFAULT NULL COMMENT '完成时间（成功或最终失败）',
    retention_days INT DEFAULT NULL COMMENT '保留天数：基础 30，pro/旗舰 null=永久',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID（=0）',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_gt_biz_no (biz_no),
    -- FIFO 拉取 + 用户隔离
    KEY idx_a_gt_user_created (target_user_id, created_at),
    -- worker 扫候选任务 / lease 回收
    KEY idx_a_gt_status_lease (status, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章生成队列表';
