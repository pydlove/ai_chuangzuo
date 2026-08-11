ALTER TABLE u_order
    ADD COLUMN coupon_code VARCHAR(64) DEFAULT NULL COMMENT '使用的优惠券码' AFTER coin_discount,
    ADD COLUMN coupon_discount DECIMAL(19,4) DEFAULT NULL COMMENT '优惠券抵扣金额' AFTER coupon_code;
