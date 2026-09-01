CREATE TABLE IF NOT EXISTS u_payment_notify_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '本地订单编号',
    nonce_str VARCHAR(64) DEFAULT NULL COMMENT '第三方通知唯一标识',
    trade_order_id VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
    raw_body TEXT NOT NULL COMMENT '回调原始内容',
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '处理结果：0-失败，1-成功',
    error_msg VARCHAR(512) DEFAULT NULL COMMENT '处理失败原因',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_u_payment_notify_log_order_nonce (order_no, nonce_str),
    KEY idx_u_payment_notify_log_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付异步通知日志';
