CREATE TABLE IF NOT EXISTS u_user_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    action_type VARCHAR(64) DEFAULT NULL COMMENT '操作类型，如 article/create',
    module VARCHAR(64) DEFAULT NULL COMMENT '模块，如 article, skill',
    request_method VARCHAR(10) NOT NULL,
    request_uri VARCHAR(512) NOT NULL,
    request_params TEXT DEFAULT NULL COMMENT 'URL参数，截断1024',
    request_body TEXT DEFAULT NULL COMMENT '请求体，截断2048并脱敏',
    client_ip VARCHAR(45) DEFAULT NULL,
    user_agent VARCHAR(512) DEFAULT NULL,
    status_code INT DEFAULT NULL,
    error_msg VARCHAR(512) DEFAULT NULL,
    duration_ms INT UNSIGNED DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_u_user_audit_log_user_id_created_at (user_id, created_at),
    KEY idx_u_user_audit_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户操作审计日志表';
