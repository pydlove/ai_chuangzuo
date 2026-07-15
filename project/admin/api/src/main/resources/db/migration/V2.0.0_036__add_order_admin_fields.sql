SET NAMES utf8mb4;

-- 扩展订单表：退款、管理员操作字段
ALTER TABLE u_order
    ADD COLUMN refunded_at DATETIME(3) DEFAULT NULL COMMENT '退款时间' AFTER paid_at,
    ADD COLUMN refund_reason VARCHAR(256) DEFAULT NULL COMMENT '退款原因' AFTER refunded_at,
    ADD COLUMN admin_remark VARCHAR(256) DEFAULT NULL COMMENT '管理员操作备注' AFTER refund_reason,
    ADD COLUMN operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作管理员ID' AFTER admin_remark;

-- 修改 status 字段注释（新增枚举值）
ALTER TABLE u_order
    MODIFY COLUMN status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已退款，3-已取消';

-- 新增索引
ALTER TABLE u_order ADD INDEX idx_u_order_status (status);
ALTER TABLE u_order ADD INDEX idx_u_order_paid_at (paid_at);
ALTER TABLE u_order ADD INDEX idx_u_order_created_at (created_at);
