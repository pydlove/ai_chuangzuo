package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.lottery.entity.UserCoupon;
import com.aichuangzuo.user.modules.lottery.mapper.UserCouponMapper;
import com.aichuangzuo.user.modules.lottery.service.UserCouponService;
import com.aichuangzuo.user.modules.membership.enums.MembershipErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final UserCouponMapper userCouponMapper;

    @Override
    public BigDecimal applyCoupon(Long userId, String couponCode, BigDecimal amount, String planKey, String cycle) {
        if (!StringUtils.hasText(couponCode)) {
            return amount;
        }
        UserCoupon coupon = userCouponMapper.selectOne(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponCode, couponCode)
                        .eq(UserCoupon::getStatus, "unused")
                        .le(UserCoupon::getValidStart, LocalDateTime.now())
                        .ge(UserCoupon::getValidEnd, LocalDateTime.now()));
        if (coupon == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_COUPON);
        }
        if (!isApplicable(coupon, planKey, cycle)) {
            throw new BusinessException(MembershipErrorCode.COUPON_NOT_APPLICABLE);
        }

        if ("percent".equals(coupon.getCouponType())) {
            return amount.multiply(coupon.getDiscountValue())
                    .setScale(2, RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO);
        }
        return amount.subtract(coupon.getDiscountValue())
                .setScale(2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCouponUsed(Long userId, String couponCode, Long orderId) {
        if (!StringUtils.hasText(couponCode)) {
            return;
        }
        userCouponMapper.update(null,
                new LambdaUpdateWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponCode, couponCode)
                        .eq(UserCoupon::getStatus, "unused")
                        .set(UserCoupon::getStatus, "used")
                        .set(UserCoupon::getUsedOrderId, orderId));
    }

    private boolean isApplicable(UserCoupon coupon, String planKey, String cycle) {
        if (coupon.getApplicablePlan() != null && !"all".equals(coupon.getApplicablePlan())
                && !coupon.getApplicablePlan().equals(planKey)) {
            return false;
        }
        if (coupon.getApplicableCycle() != null && !"all".equals(coupon.getApplicableCycle())
                && !coupon.getApplicableCycle().equals(cycle)) {
            return false;
        }
        return true;
    }
}
