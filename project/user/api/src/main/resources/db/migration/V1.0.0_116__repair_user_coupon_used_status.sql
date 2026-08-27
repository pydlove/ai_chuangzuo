-- 修复历史数据：已产生订单的优惠券状态仍为 unused 的情况
UPDATE u_user_coupon
SET status = 'used'
WHERE used_order_id IS NOT NULL
  AND status != 'used';
