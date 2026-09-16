-- 模拟运营：批次、机器人、阶段日志三张表
CREATE TABLE IF NOT EXISTS a_simulation_batch (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(32) NOT NULL COMMENT '批次编号 SIM+日期+序号',
    user_count INT UNSIGNED NOT NULL COMMENT '机器人数',
    plan_key VARCHAR(16) NOT NULL COMMENT '会员套餐 key：basic/pro/flagship',
    plan_name VARCHAR(64) NOT NULL COMMENT '会员套餐名称快照',
    cycle VARCHAR(16) NOT NULL COMMENT '订阅周期：month/quarter/year',
    stage_config JSON NOT NULL COMMENT '阶段配置(概率/范围/提示词范围)',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/RUNNING/COMPLETED/CANCELED',
    total_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总数',
    completed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '完成数',
    failed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '失败数',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟运营批次';

CREATE TABLE IF NOT EXISTS a_simulation_robot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '批次ID',
    seq INT UNSIGNED NOT NULL COMMENT '批次内序号',
    email VARCHAR(128) NOT NULL COMMENT '虚拟邮箱',
    password_encrypted VARCHAR(255) NOT NULL COMMENT 'AES加密后的初始密码',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户端u_user.id',
    invite_code VARCHAR(16) DEFAULT NULL COMMENT '绑定的邀请码',
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT '状态 WAITING/IN_PROGRESS/COMPLETED/FAILED/CANCELED',
    current_stage VARCHAR(16) NOT NULL DEFAULT 'REGISTER' COMMENT '当前阶段',
    next_run_at DATETIME(3) NOT NULL COMMENT '下一阶段允许执行时间',
    context JSON DEFAULT NULL COMMENT '阶段上下文(文章/任务ID等)',
    fail_stage VARCHAR(16) DEFAULT NULL COMMENT '失败阶段',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    started_at DATETIME(3) DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_seq (batch_id, seq),
    KEY idx_next_run (status, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟运营机器人';

CREATE TABLE IF NOT EXISTS a_simulation_robot_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    robot_id BIGINT UNSIGNED NOT NULL COMMENT '机器人ID',
    batch_id BIGINT UNSIGNED NOT NULL COMMENT '批次ID',
    stage VARCHAR(16) NOT NULL COMMENT '阶段',
    status VARCHAR(16) NOT NULL COMMENT '结果 SUCCESS/FAILED/SKIPPED',
    detail JSON DEFAULT NULL COMMENT '明细(任务ID/耗时等)',
    error_msg VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_robot (robot_id),
    KEY idx_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟运营机器人阶段日志';
