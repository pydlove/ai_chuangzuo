SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_qr_login_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    qr_code VARCHAR(64) NOT NULL COMMENT '二维码唯一标识',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待扫描 1-已扫描 2-已授权 3-已取消 4-已过期',
    scanner_user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '扫描者用户ID',
    scanner_nickname VARCHAR(64) DEFAULT NULL COMMENT '扫描者昵称',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '创建者IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '创建者User-Agent',
    expired_at DATETIME(3) NOT NULL COMMENT '过期时间',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_qr_login_session_qr_code (qr_code),
    KEY idx_u_qr_login_session_status_expired (status, expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二维码登录会话表';
