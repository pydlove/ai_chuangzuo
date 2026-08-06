SET NAMES utf8mb4;

ALTER TABLE u_order
    ADD COLUMN coin_amount BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创作币抵扣数量' AFTER amount,
    ADD COLUMN coin_discount DECIMAL(19,4) NOT NULL DEFAULT 0.0000 COMMENT '创作币抵扣金额（元）' AFTER coin_amount,
    ADD COLUMN total_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000 COMMENT '订单名义总金额（现金+创作币抵扣）' AFTER coin_discount;

UPDATE u_order SET total_amount = amount;
