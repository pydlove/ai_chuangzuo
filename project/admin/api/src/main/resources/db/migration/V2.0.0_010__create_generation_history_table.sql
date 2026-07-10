-- 文章生成历史归档表（cold storage）
-- 由 retention job 把过期的 a_generation_task 任务迁到此表
-- 字段裁剪：去掉 updated_at/updated_by/is_deleted/retention_days/locked_by

CREATE TABLE IF NOT EXISTS a_generation_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '归档表自增主键',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '原 a_generation_task.id',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务唯一编号',
    target_user_id BIGINT UNSIGNED NOT NULL COMMENT '发起任务的用户ID',
    title VARCHAR(256) DEFAULT NULL COMMENT '任务结束时已确定的 title（成功或失败状态都有）',
    input_param JSON NOT NULL COMMENT '输入参数 JSON',
    status TINYINT UNSIGNED NOT NULL COMMENT '终态：2-completed，3-failed',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计重试次数',
    failed_reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因（status=failed 时）',
    word_limit_target INT UNSIGNED NOT NULL COMMENT '用户要求字数',
    model_config_id BIGINT UNSIGNED NOT NULL COMMENT '使用的AI模型配置ID',
    prompt_template_id BIGINT UNSIGNED NOT NULL COMMENT '使用的提示词模板ID',
    created_at DATETIME(3) NOT NULL COMMENT '原任务创建时间',
    completed_at DATETIME(3) DEFAULT NULL COMMENT '完成时间',
    duration_ms BIGINT DEFAULT NULL COMMENT '处理耗时（毫秒）',
    archived_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入档时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_a_gh_task_id (task_id),
    KEY idx_a_gh_user_archived (target_user_id, archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章生成历史归档';
