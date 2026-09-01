ALTER TABLE u_order
    ADD COLUMN payment_method VARCHAR(32) DEFAULT NULL COMMENT '支付方式：xunhupay',
    ADD COLUMN third_party_trade_id VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号';
