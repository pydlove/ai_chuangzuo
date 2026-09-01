SET NAMES utf8mb4;

-- 支付配置表新增退款网关地址
ALTER TABLE a_payment_config
    ADD COLUMN refund_url VARCHAR(512) DEFAULT 'https://api.xunhupay.com/payment/refund.html' COMMENT '虎皮椒退款网关地址' AFTER gateway_url;

-- 初始化现有配置的退款地址
UPDATE a_payment_config
SET refund_url = 'https://api.xunhupay.com/payment/refund.html'
WHERE refund_url IS NULL OR refund_url = '';
