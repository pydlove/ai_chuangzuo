SET NAMES utf8mb4;

-- 套餐首月价格：仅月付周期 + 用户首次购买（无任何成功支付订单）时生效；空表示不启用首月优惠
ALTER TABLE u_plan
    ADD COLUMN first_month_price DECIMAL(10,2) DEFAULT NULL COMMENT '首月价格（仅月付周期首次购买生效，空表示不启用）' AFTER price_monthly;
