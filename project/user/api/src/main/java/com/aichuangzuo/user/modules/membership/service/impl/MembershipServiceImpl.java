package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.lottery.service.UserCouponService;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribePreviewRequest;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.dto.request.UpgradePreviewRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.shared.enums.error.MembershipErrorCode;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.service.PaymentService;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribePreviewVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.aichuangzuo.user.modules.membership.vo.UpgradePreviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 会员服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final String NEWCOMER_PLAN_KEY = "flagship";
    private static final String NEWCOMER_CYCLE = "year";
    private static final BigDecimal NEWCOMER_EXTRA_DISCOUNT = new BigDecimal("0.8");
    private static final BigDecimal COIN_TO_YUAN_RATIO = new BigDecimal("10");
    private static final int EFFECTIVE_STATUS = 1;

    private final OrderMapper orderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final CoinRecordService coinRecordService;
    private final PlanLookupService planLookupService;
    private final PlanMapper planMapper;
    private final UserCouponService userCouponService;
    private final PaymentService paymentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscribeResultVO subscribe(Long userId, SubscribeRequest request) {
        return paymentService.createPaymentOrder(userId, request);
    }

    /**
     * 计算本次订阅应付金额：新人首冲享折扣；升级套餐可用当前订阅剩余价值抵扣。
     */
    private BigDecimal resolveExpectedAmount(Long userId, String planKey, String cycleCode, boolean upgrade) {
        Plan plan = planMapper.selectOne(new LambdaQueryWrapper<Plan>()
                .eq(Plan::getPlanKey, planKey)
                .eq(Plan::getStatus, EFFECTIVE_STATUS));
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }

        BigDecimal basePrice = resolveCyclePrice(plan, cycleCode);

        boolean eligibleForNewcomer = NEWCOMER_PLAN_KEY.equals(planKey)
                && NEWCOMER_CYCLE.equals(cycleCode)
                && isNewcomerEligible(userId);
        if (eligibleForNewcomer) {
            return basePrice.multiply(NEWCOMER_EXTRA_DISCOUNT)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (upgrade) {
            BigDecimal credit = calculateCredit(userId);
            return basePrice.subtract(credit).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }

    private BigDecimal calculateCouponDiscount(Long userId, String couponCode, BigDecimal amount, String planKey, String cycle) {
        if (!StringUtils.hasText(couponCode)) {
            return BigDecimal.ZERO;
        }
        BigDecimal afterCoupon = userCouponService.applyCoupon(userId, couponCode, amount, planKey, cycle);
        return amount.subtract(afterCoupon).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateCoinAmount(BigDecimal coinAmount) {
        if (coinAmount == null) {
            return;
        }
        if (coinAmount.compareTo(BigDecimal.ZERO) < 0 || coinAmount.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(MembershipErrorCode.INVALID_COIN_AMOUNT);
        }
    }

    private long calculateMaxCoinAmount(BigDecimal cashAmount, BigDecimal coinBalance) {
        long maxByCash = cashAmount.multiply(COIN_TO_YUAN_RATIO).setScale(0, RoundingMode.FLOOR).longValue();
        long maxByBalance = coinBalance.setScale(0, RoundingMode.FLOOR).longValue();
        return Math.min(Math.max(maxByCash, 0), Math.max(maxByBalance, 0));
    }

    private BigDecimal resolveCyclePrice(Plan plan, String cycleCode) {
        BigDecimal basePrice;
        if ("month".equals(cycleCode)) {
            basePrice = plan.getPriceMonthly();
        } else if ("quarter".equals(cycleCode)) {
            basePrice = plan.getPriceQuarter();
        } else if ("year".equals(cycleCode)) {
            basePrice = plan.getPriceYear();
        } else {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }
        if (basePrice == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }
        return basePrice;
    }

    private boolean isNewcomerEligible(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership != null && !membership.getExpiresAt().isBefore(LocalDate.now())) {
            return false;
        }
        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(userId);
        return relation == null;
    }

    /**
     * 判断目标套餐是否高于当前有效套餐（升级）。
     * 套餐等级更高，或同套餐但周期更长，均视为升级。
     */
    private boolean isUpgrade(Long userId, MembershipPlan targetPlan, MembershipCycle targetCycle) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            return false;
        }
        MembershipPlan currentPlan = MembershipPlan.of(membership.getLevel());
        if (currentPlan == null) {
            return false;
        }
        if (targetPlan.getRank() > currentPlan.getRank()) {
            return true;
        }
        if (targetPlan.getRank() == currentPlan.getRank()) {
            MembershipCycle currentCycle = MembershipCycle.of(getCurrentMembershipCycle(userId));
            return currentCycle != null && targetCycle.getRank() > currentCycle.getRank();
        }
        return false;
    }

    /**
     * 计算当前有效订阅的剩余价值：优先按最近一次已支付订单的金额/周期折算日单价；
     * 无订单时按当前套餐月价/30 天作为兜底日单价。
     */
    private BigDecimal calculateCredit(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            return BigDecimal.ZERO;
        }
        return calculateCredit(userId, membership);
    }

    private BigDecimal calculateCredit(Long userId, UserMembership membership) {
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), membership.getExpiresAt());
        if (remainingDays <= 0) {
            return BigDecimal.ZERO;
        }

        Order latestOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getPlanKey, membership.getLevel())
                        .eq(Order::getStatus, 1)
                        .orderByDesc(Order::getPaidAt)
                        .last("LIMIT 1")
        );

        BigDecimal dailyRate;
        if (latestOrder != null && latestOrder.getAmount() != null) {
            MembershipCycle cycle = MembershipCycle.of(latestOrder.getCycle());
            int cycleDays = cycle != null ? cycle.getDays() : 30;
            dailyRate = latestOrder.getAmount().divide(BigDecimal.valueOf(cycleDays), 10, RoundingMode.HALF_UP);
        } else {
            Plan plan = planMapper.selectOne(new LambdaQueryWrapper<Plan>()
                    .eq(Plan::getPlanKey, membership.getLevel())
                    .eq(Plan::getStatus, EFFECTIVE_STATUS));
            BigDecimal monthly = plan != null && plan.getPriceMonthly() != null
                    ? plan.getPriceMonthly() : BigDecimal.ZERO;
            dailyRate = monthly.divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
        }

        return dailyRate.multiply(BigDecimal.valueOf(remainingDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public MembershipStatusVO getMyMembership(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        MembershipStatusVO vo = new MembershipStatusVO();
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            vo.setHasMembership(false);
            return vo;
        }

        vo.setHasMembership(true);
        vo.setLevel(membership.getLevel());
        vo.setLevelName(planLookupService.getDisplayName(membership.getLevel()));
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));

        Order latestOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getPlanKey, membership.getLevel())
                        .eq(Order::getStatus, 1)
                        .orderByDesc(Order::getPaidAt)
                        .last("LIMIT 1")
        );
        if (latestOrder != null && latestOrder.getCycle() != null) {
            vo.setCycle(latestOrder.getCycle());
        }
        return vo;
    }

    private String getCurrentMembershipCycle(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            return null;
        }
        Order latestOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getPlanKey, membership.getLevel())
                        .eq(Order::getStatus, 1)
                        .orderByDesc(Order::getPaidAt)
                        .last("LIMIT 1")
        );
        return latestOrder == null ? null : latestOrder.getCycle();
    }

    private void validateUpgradeCycle(Long userId, String requestCycle) {
        String currentCycleCode = getCurrentMembershipCycle(userId);
        if (currentCycleCode == null) {
            return;
        }
        MembershipCycle currentCycle = MembershipCycle.of(currentCycleCode);
        MembershipCycle targetCycle = MembershipCycle.of(requestCycle);
        if (currentCycle != null && targetCycle != null && targetCycle.getRank() < currentCycle.getRank()) {
            throw new BusinessException(MembershipErrorCode.UPGRADE_CYCLE_MISMATCH);
        }
    }

    @Override
    public UpgradePreviewVO previewUpgrade(Long userId, UpgradePreviewRequest request) {
        MembershipPlan targetPlan = MembershipPlan.of(request.getPlanKey());
        if (targetPlan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }
        MembershipCycle targetCycle = MembershipCycle.of(request.getCycle());
        if (targetCycle == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }

        validateUpgradeCycle(userId, request.getCycle());

        Plan plan = planMapper.selectOne(new LambdaQueryWrapper<Plan>()
                .eq(Plan::getPlanKey, targetPlan.getKey())
                .eq(Plan::getStatus, EFFECTIVE_STATUS));
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }

        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        boolean hasActive = membership != null && !membership.getExpiresAt().isBefore(LocalDate.now());
        MembershipPlan currentPlan = hasActive ? MembershipPlan.of(membership.getLevel()) : null;
        boolean upgrade = isUpgrade(userId, targetPlan, targetCycle);

        BigDecimal originalPrice = resolveCyclePrice(plan, targetCycle.getCode());
        BigDecimal creditAmount = upgrade ? calculateCredit(userId, membership) : BigDecimal.ZERO;
        BigDecimal finalPrice = originalPrice.subtract(creditAmount).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        UpgradePreviewVO vo = new UpgradePreviewVO();
        vo.setHasMembership(hasActive);
        if (hasActive) {
            vo.setCurrentPlanKey(currentPlan.getKey());
            vo.setCurrentPlanName(planLookupService.getDisplayName(currentPlan.getKey()));
            vo.setCurrentExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
            vo.setRemainingDays((int) ChronoUnit.DAYS.between(LocalDate.now(), membership.getExpiresAt()));
        }
        vo.setUpgrade(upgrade);
        vo.setTargetPlanKey(targetPlan.getKey());
        vo.setTargetPlanName(planLookupService.getDisplayName(targetPlan.getKey()));
        vo.setTargetCycle(targetCycle.getCode());
        vo.setOriginalPrice(originalPrice);
        vo.setCreditAmount(creditAmount);
        vo.setFinalPrice(finalPrice);
        vo.setTargetDays(targetCycle.getDays());
        vo.setNewExpiresAt(LocalDate.now().plusDays(targetCycle.getDays())
                .format(DateTimeFormatter.ISO_LOCAL_DATE));

        BigDecimal coinBalance = coinRecordService.getBalance(userId);
        vo.setCoinBalance(coinBalance);
        vo.setMaxCoinAmount(calculateMaxCoinAmount(finalPrice, coinBalance));
        vo.setCoinToYuanRatio(COIN_TO_YUAN_RATIO.intValue());
        return vo;
    }

    @Override
    public SubscribePreviewVO previewSubscribe(Long userId, SubscribePreviewRequest request) {
        MembershipPlan targetPlan = MembershipPlan.of(request.getPlanKey());
        if (targetPlan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }
        MembershipCycle targetCycle = MembershipCycle.of(request.getCycle());
        if (targetCycle == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }

        Plan plan = planMapper.selectOne(new LambdaQueryWrapper<Plan>()
                .eq(Plan::getPlanKey, targetPlan.getKey())
                .eq(Plan::getStatus, EFFECTIVE_STATUS));
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }

        boolean upgrade = isUpgrade(userId, targetPlan, targetCycle);
        if (upgrade) {
            validateUpgradeCycle(userId, request.getCycle());
        }
        BigDecimal originalPrice = resolveCyclePrice(plan, targetCycle.getCode());
        BigDecimal expectedAmount = resolveExpectedAmount(userId, targetPlan.getKey(), targetCycle.getCode(), upgrade);
        BigDecimal creditAmount = upgrade ? calculateCredit(userId) : BigDecimal.ZERO;
        BigDecimal couponDiscount = calculateCouponDiscount(userId, request.getCouponCode(), expectedAmount, targetPlan.getKey(), targetCycle.getCode());
        BigDecimal finalPrice = expectedAmount.subtract(couponDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        SubscribePreviewVO vo = new SubscribePreviewVO();
        vo.setPlanKey(targetPlan.getKey());
        vo.setCycle(targetCycle.getCode());
        vo.setOriginalPrice(originalPrice);
        vo.setCreditAmount(creditAmount);
        vo.setCouponDiscount(couponDiscount);
        vo.setFinalPrice(finalPrice);

        BigDecimal coinBalance = coinRecordService.getBalance(userId);
        vo.setCoinBalance(coinBalance);
        vo.setMaxCoinAmount(calculateMaxCoinAmount(expectedAmount, coinBalance));
        vo.setCoinToYuanRatio(COIN_TO_YUAN_RATIO.intValue());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extendMembership(Long userId, String level, long days) {
        LocalDate today = LocalDate.now();
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        LocalDate baseDate = today;
        if (membership != null && !membership.getExpiresAt().isBefore(today)) {
            baseDate = membership.getExpiresAt();
        }
        LocalDate newExpiresAt = baseDate.plusDays(days);

        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setLevel(level);
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membership.setTenantId(0L);
            userMembershipMapper.insert(membership);
        } else {
            membership.setLevel(level);
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            userMembershipMapper.updateById(membership);
        }
        log.info("会员延长 userId={}, level={}, days={}, expiresAt={}", userId, level, days, newExpiresAt);
    }
}
