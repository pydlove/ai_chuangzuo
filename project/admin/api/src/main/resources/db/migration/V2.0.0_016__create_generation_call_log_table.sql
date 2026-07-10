-- 创作 AI 调用日志：每次 worker 跑完流水线后批量落库
-- 记录每次 AI 调用的 stage / 尝试次数 / 成功 / 错误 / 耗时 / 输入输出摘要
-- 任务结束（成功 or 失败）后才写，crash 中途丢失记录

CREATE TABLE IF NOT EXISTS a_generation_call_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '所属任务 ID（a_generation_task.id）',
    stage_index INT UNSIGNED NOT NULL COMMENT '阶段序号 1-12',
    stage_name VARCHAR(32) NOT NULL COMMENT '阶段稳定标识符：outline / draft / ...',
    attempt INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '第几次尝试（1=首次，2=第 1 次重试，...）',
    success TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否成功',
    error VARCHAR(512) DEFAULT NULL COMMENT '失败原因（成功时为 null）',
    duration_ms INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次尝试耗时（ms）',
    called_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '调用开始时间',
    user_msg_preview VARCHAR(256) DEFAULT NULL COMMENT 'userMsg 前 200 字（避免存全文）',
    response_preview VARCHAR(256) DEFAULT NULL COMMENT 'AI 返回前 200 字（成功时）',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID（=0）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_log_task (task_id, called_at),
    KEY idx_log_stage (task_id, stage_index, attempt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='创作 AI 调用日志';
