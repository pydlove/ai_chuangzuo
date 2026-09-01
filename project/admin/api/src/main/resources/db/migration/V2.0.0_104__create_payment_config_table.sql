-- 支付配置表，由管理端系统设置-支付设置维护
CREATE TABLE IF NOT EXISTS a_payment_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    provider VARCHAR(32) NOT NULL DEFAULT 'xunhupay' COMMENT '支付服务商：xunhupay',
    app_id VARCHAR(255) DEFAULT NULL COMMENT '虎皮椒 App ID',
    app_secret VARCHAR(255) DEFAULT NULL COMMENT 'Jasypt 加密后的虎皮椒 App Secret',
    gateway_url VARCHAR(512) DEFAULT 'https://api.xunhupay.com/payment/do.html' COMMENT '虎皮椒下单网关地址',
    notify_url VARCHAR(512) DEFAULT NULL COMMENT '虎皮椒异步通知地址',
    return_url VARCHAR(512) DEFAULT NULL COMMENT '虎皮椒支付完成回跳地址',
    enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用支付：0-否，1-是',
    test_mode TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否测试模式：0-否，1-是',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    created_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    updated_by BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新人ID',
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付配置';

-- 初始化单行默认配置：默认不启用支付，且开启测试模式，避免未配置密钥时误调用真实网关
INSERT INTO a_payment_config (id, provider, enabled, test_mode, gateway_url)
VALUES (1, 'xunhupay', 0, 1, 'https://api.xunhupay.com/payment/do.html')
ON DUPLICATE KEY UPDATE id = id;
