CREATE TABLE IF NOT EXISTS a_scheduled_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_key VARCHAR(128) NOT NULL COMMENT '任务唯一标识',
    task_name VARCHAR(128) NOT NULL COMMENT '任务名称',
    description VARCHAR(512) DEFAULT NULL COMMENT '业务说明',
    module VARCHAR(32) NOT NULL COMMENT '所属模块：admin-管理端，user-用户端',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发类型：cron-表达式触发，fixed_delay-固定间隔',
    expression VARCHAR(128) DEFAULT NULL COMMENT '触发表达式：cron 或 fixedDelay 毫秒值',
    bean_name VARCHAR(128) NOT NULL COMMENT 'Spring Bean 名称',
    method_name VARCHAR(128) NOT NULL COMMENT '方法名',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    last_run_at DATETIME(3) DEFAULT NULL COMMENT '上次执行时间',
    last_run_status VARCHAR(32) DEFAULT NULL COMMENT '上次执行状态：success-成功，failed-失败，running-执行中',
    last_run_message VARCHAR(512) DEFAULT NULL COMMENT '上次执行结果摘要',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_scheduled_task_task_key (task_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务元数据表';

CREATE TABLE IF NOT EXISTS a_scheduled_task_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '任务ID',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发方式：auto-自动，manual-手动',
    started_at DATETIME(3) NOT NULL COMMENT '开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    run_status VARCHAR(32) NOT NULL COMMENT '执行状态：success-成功，failed-失败',
    message VARCHAR(1024) DEFAULT NULL COMMENT '结果摘要或异常信息',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '触发人ID，auto 为 0',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_a_scheduled_task_log_task_id (task_id),
    KEY idx_a_scheduled_task_log_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行日志表';
