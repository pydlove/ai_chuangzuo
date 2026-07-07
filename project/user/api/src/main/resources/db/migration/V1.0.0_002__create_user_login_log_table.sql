SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_user_login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID，0表示未登录',
    login_type TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型：1-密码登录，2-注册登录',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    login_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    fail_reason VARCHAR(256) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_u_user_login_log_user_id (user_id),
    KEY idx_u_user_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';
