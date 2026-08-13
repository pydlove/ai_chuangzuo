SET NAMES utf8mb4;

-- 扩展订单表：手动发放会员时记录时间范围
ALTER TABLE u_order
    ADD COLUMN started_at DATE DEFAULT NULL COMMENT '会员开始日期' AFTER cycle,
    ADD COLUMN expires_at DATE DEFAULT NULL COMMENT '会员结束日期' AFTER started_at;

-- 新增索引
ALTER TABLE u_order ADD INDEX idx_u_order_started_at (started_at);
ALTER TABLE u_order ADD INDEX idx_u_order_expires_at (expires_at);
