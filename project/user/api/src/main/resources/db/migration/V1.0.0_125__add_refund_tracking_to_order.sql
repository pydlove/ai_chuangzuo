SET NAMES utf8mb4;

-- 订单表新增退款跟踪字段
ALTER TABLE u_order
    ADD COLUMN third_party_refund_id VARCHAR(128) DEFAULT NULL COMMENT '第三方退款单号' AFTER third_party_trade_id,
    ADD COLUMN refund_amount DECIMAL(10, 2) DEFAULT NULL COMMENT '实际退款金额' AFTER refund_reason;
