-- 升级管理配置表，由管理端系统设置-升级管理维护
CREATE TABLE IF NOT EXISTS a_upgrade_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    script_root_dir VARCHAR(512) NOT NULL DEFAULT '/Users/panyong/aio_project/ai_chuangzuo/scripts' COMMENT '脚本根目录',
    server_ip VARCHAR(64) DEFAULT NULL COMMENT '服务器 IP',
    server_user VARCHAR(64) DEFAULT NULL COMMENT 'SSH 用户名',
    server_password VARCHAR(255) DEFAULT NULL COMMENT 'Jasypt 加密后的 SSH 密码',
    ssh_key_path VARCHAR(512) DEFAULT NULL COMMENT 'SSH 私钥路径',
    command_timeout_seconds INT UNSIGNED NOT NULL DEFAULT 600 COMMENT '脚本执行超时秒数',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='升级管理配置';

-- 初始化单行默认配置
INSERT INTO a_upgrade_config (id, script_root_dir, command_timeout_seconds)
VALUES (1, '/Users/panyong/aio_project/ai_chuangzuo/scripts', 600)
ON DUPLICATE KEY UPDATE id = id;

-- 升级脚本执行日志表
CREATE TABLE IF NOT EXISTS a_upgrade_job_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    script_relative_path VARCHAR(1024) NOT NULL COMMENT '脚本相对根目录路径',
    script_name VARCHAR(255) NOT NULL COMMENT '脚本文件名',
    trigger_type VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT '触发方式：manual',
    run_status VARCHAR(16) NOT NULL DEFAULT 'running' COMMENT '状态：running/success/failed/timeout',
    started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时间',
    finished_at DATETIME(3) DEFAULT NULL COMMENT '结束时间',
    exit_code INT DEFAULT NULL COMMENT '退出码',
    stdout LONGTEXT COMMENT '标准输出（可能被截断）',
    stderr LONGTEXT COMMENT '标准错误（可能被截断）',
    output_truncated TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '输出是否被截断：0-否，1-是',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '执行人ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id),
    KEY idx_status_started (run_status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='升级脚本执行日志';
