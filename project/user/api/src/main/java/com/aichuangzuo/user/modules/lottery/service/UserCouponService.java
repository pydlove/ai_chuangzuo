package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

public interface UserCouponService {

    List<UserCouponVO> listUserCoupons(Long userId);

    BigDecimal applyCoupon(Long userId, String couponCode, BigDecimal amount, String planKey, String cycle);

    void markCouponUsed(Long userId, String couponCode, Long orderId);
}
