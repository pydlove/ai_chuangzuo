package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.dto.request.UpgradePreviewRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.enums.MembershipErrorCode;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.aichuangzuo.user.modules.membership.vo.UpgradePreviewVO;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 会员服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final String TEST_PAY_CODE = "123456";
    private static final String ORDER_NO_PREFIX = "SUB";
    private static final String COIN_BIZ_TYPE_INVITE_REWARD = "invite_reward";
    private static final int EFFECTIVE_STATUS = 1;

    private static final BigDecimal FIRST_PURCHASE_RATE = new BigDecimal("0.10");
    private static final BigDecimal RENEWAL_RATE = new BigDecimal("0.05");
    private static final BigDecimal COIN_TO_YUAN_RATIO = new BigDecimal("10");

    private static final String NEWCOMER_PLAN_KEY = "flagship";
    private static final String NEWCOMER_CYCLE = "year";
    private static final BigDecimal NEWCOMER_EXTRA_DISCOUNT = new BigDecimal("0.8");
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final OrderMapper orderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final CoinRecordService coinRecordService;
    private final EarningsService earningsService;
    private final PlanLookupService planLookupService;
    private final PlanMapper planMapper;

    @Override
    @Transactional
    public SubscribeResultVO subscribe(Long userId, SubscribeRequest request) {
        validatePayCode(request.getPayCode());

        MembershipPlan plan = MembershipPlan.of(request.getPlanKey());
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }

        MembershipCycle cycle = MembershipCycle.of(request.getCycle());
        if (cycle == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }

        boolean upgrade = isUpgrade(userId, plan);
        if (upgrade) {
            validateUpgradeCycle(userId, request.getCycle());
        }
        BigDecimal expectedAmount = resolveExpectedAmount(userId, plan.getKey(), cycle.getCode(), upgrade);
        if (request.getAmount() == null ||
                request.getAmount().subtract(expectedAmount).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessException(MembershipErrorCode.INVALID_AMOUNT);
        }

        Order order = createPaidOrder(userId, plan, cycle, expectedAmount);
        UserMembership membership = activateOrExtendMembership(userId, plan, cycle, upgrade);

        sendSubscriptionNotification(userId, plan, membership);
        boolean rewarded = rewardInviter(userId, plan, order);

        log.info("会员订阅成功 userId={}, orderNo={}, level={}, days={}",
                userId, order.getOrderNo(), plan.getKey(), cycle.getDays());

        SubscribeResultVO vo = new SubscribeResultVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setLevel(plan.getKey());
        vo.setDays(cycle.getDays());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        vo.setInviterRewarded(rewarded);
        vo.setRewardAmount(rewarded ? calculateInviteReward(order.getAmount(), isFirstPurchase(userId, order.getId())) : BigDecimal.ZERO);
        return vo;
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
     */
    private boolean isUpgrade(Long userId, MembershipPlan targetPlan) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            return false;
        }
        MembershipPlan currentPlan = MembershipPlan.of(membership.getLevel());
        return currentPlan != null && targetPlan.getRank() > currentPlan.getRank();
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
        String currentCycle = getCurrentMembershipCycle(userId);
        if (currentCycle != null && !currentCycle.equals(requestCycle)) {
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
        boolean upgrade = currentPlan != null && targetPlan.getRank() > currentPlan.getRank();

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

    private void validatePayCode(String payCode) {
        if (!TEST_PAY_CODE.equals(payCode)) {
            throw new BusinessException(MembershipErrorCode.INVALID_PAY_CODE);
        }
    }

    private Order createPaidOrder(Long userId, MembershipPlan plan, MembershipCycle cycle, BigDecimal amount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlanKey(plan.getKey());
        order.setCycle(cycle.getCode());
        order.setAmount(amount);
        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        order.setTenantId(0L);
        orderMapper.insert(order);
        return order;
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return ORDER_NO_PREFIX + date + random;
    }

    private UserMembership activateOrExtendMembership(Long userId, MembershipPlan plan, MembershipCycle cycle, boolean upgrade) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate baseDate = today;
        if (!upgrade && membership != null && membership.getExpiresAt().isAfter(today.minusDays(1))) {
            baseDate = membership.getExpiresAt();
        }
        LocalDate newExpiresAt = baseDate.plusDays(cycle.getDays());

        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setLevel(plan.getKey());
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membership.setTenantId(0L);
            userMembershipMapper.insert(membership);
        } else {
            membership.setLevel(plan.getKey());
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            userMembershipMapper.updateById(membership);
        }
        return membership;
    }

    private void sendSubscriptionNotification(Long userId, MembershipPlan plan, UserMembership membership) {
        String levelName = planLookupService.getDisplayName(plan.getKey());
        String expiresAt = membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String summary = String.format("您已成功开通 %s，有效期至 %s", levelName, expiresAt);
        String content = String.format(
                "亲爱的用户：\n\n您的 %s 会员已成功开通，有效期至 %s。\n\n感谢您对爱创作的支持！",
                levelName, expiresAt);

        messageService.pushPersonal(
                userId,
                "membership",
                "订阅成功",
                summary,
                null,
                content,
                MessageSubType.SUBSCRIBED.getCode());
    }

    private boolean rewardInviter(Long userId, MembershipPlan plan, Order order) {
        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(userId);
        if (relation == null || !Integer.valueOf(EFFECTIVE_STATUS).equals(relation.getEffectiveStatus())) {
            return false;
        }

        Long inviterId = relation.getInviterId();
        if (alreadyRewarded(order.getId())) {
            log.warn("邀请奖励已发放，跳过 userId={}, orderId={}", userId, order.getId());
            return false;
        }

        boolean firstPurchase = isFirstPurchase(userId, order.getId());
        BigDecimal rate = firstPurchase ? FIRST_PURCHASE_RATE : RENEWAL_RATE;
        BigDecimal reward = order.getAmount().multiply(rate).multiply(COIN_TO_YUAN_RATIO).setScale(2, RoundingMode.HALF_UP);

        User invitee = userMapper.selectById(userId);
        String inviteeName = invitee == null ? "好友" : (invitee.getNickname() == null ? "好友" : invitee.getNickname());
        String planName = planLookupService.getDisplayName(plan.getKey());
        String remark = String.format("%s %s %s，邀请奖励 %s 创作币",
                inviteeName, firstPurchase ? "首次购买" : "续费", planName, reward.toPlainString());

        coinRecordService.grant(inviterId, COIN_BIZ_TYPE_INVITE_REWARD, reward, order.getId().toString(), remark);

        String settlementMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        earningsService.recordInviteRewardEarnings(inviterId, userId, plan.getKey(), planName,
                order.getCycle(), order.getAmount(), firstPurchase, rate, reward, settlementMonth);
        return true;
    }

    private boolean isFirstPurchase(Long userId, Long currentOrderId) {
        Long paidCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, 1)
                        .ne(Order::getId, currentOrderId)
        );
        return paidCount == null || paidCount == 0;
    }

    private BigDecimal calculateInviteReward(BigDecimal orderAmount, boolean firstPurchase) {
        BigDecimal rate = firstPurchase ? FIRST_PURCHASE_RATE : RENEWAL_RATE;
        return orderAmount.multiply(rate).multiply(COIN_TO_YUAN_RATIO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean alreadyRewarded(Long orderId) {
        Long count = userCoinRecordMapper.selectCount(
                new LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getRefId, orderId.toString())
                        .eq(UserCoinRecord::getBizType, COIN_BIZ_TYPE_INVITE_REWARD)
        );
        return count != null && count > 0;
    }
}
