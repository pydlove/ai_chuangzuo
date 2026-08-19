
-- 短信配置表，由管理端系统设置-短信配置维护，用户端读取并发送短信
CREATE TABLE IF NOT EXISTS a_sms_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    provider VARCHAR(32) NOT NULL DEFAULT 'aliyun' COMMENT '短信服务商：aliyun',
    access_key_id VARCHAR(255) DEFAULT NULL COMMENT 'AccessKey ID',
    access_key_secret VARCHAR(255) DEFAULT NULL COMMENT '加密后的 AccessKey Secret',
    sign_name VARCHAR(255) DEFAULT NULL COMMENT '短信签名',
    template_code VARCHAR(255) DEFAULT NULL COMMENT '短信模板 Code',
    region_id VARCHAR(64) DEFAULT 'cn-hangzhou' COMMENT '阿里云区域 ID',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用短信：0-否，1-是',
    send_interval_seconds INT UNSIGNED NOT NULL DEFAULT 60 COMMENT '同一手机号两次发送最小间隔（秒）',
    daily_max_per_phone INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '同一手机号每天最多发送次数',
    daily_max_per_ip INT UNSIGNED NOT NULL DEFAULT 50 COMMENT '同一 IP 每天最多发送次数',
    global_daily_max INT UNSIGNED NOT NULL DEFAULT 1000 COMMENT '全站每天最多发送次数',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信配置';

-- 初始化单行默认配置：默认不启用短信，避免未配置密钥时误发送
INSERT INTO a_sms_config (id, provider, enabled, send_interval_seconds, daily_max_per_phone, daily_max_per_ip, global_daily_max)
VALUES (1, 'aliyun', 0, 60, 10, 50, 1000)
ON DUPLICATE KEY UPDATE id = id;
