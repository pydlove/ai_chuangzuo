SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS u_sms_send_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    phone VARCHAR(20) NOT NULL COMMENT '接收手机号',
    scene VARCHAR(32) NOT NULL DEFAULT 'register' COMMENT '发送场景：register-注册，reset_password-忘记密码，bind_phone-绑定/换绑手机号',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
    user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '触发用户ID，0表示未登录',
    send_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '发送状态：0-失败，1-成功',
    fail_reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    response_code VARCHAR(64) DEFAULT NULL COMMENT '服务商返回码',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示默认租户',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_u_sms_send_record_phone (phone),
    KEY idx_u_sms_send_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信发送记录表';
