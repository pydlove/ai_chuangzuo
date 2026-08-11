package com.aichuangzuo.user.modules.lottery.service;

import java.math.BigDecimal;

public interface UserCouponService {

    BigDecimal applyCoupon(Long userId, String couponCode, BigDecimal amount, String planKey, String cycle);

    void markCouponUsed(Long userId, String couponCode, Long orderId);
}
