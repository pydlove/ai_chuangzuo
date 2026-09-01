package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.lottery.service.UserCouponService;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.shared.enums.error.MembershipErrorCode;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.message.MembershipMessageNotifier;
import com.aichuangzuo.user.modules.membership.payment.config.entity.PaymentConfig;
import com.aichuangzuo.user.modules.membership.payment.config.mapper.PaymentConfigMapper;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.client.XunhupayClient;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayNotifyParams;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.dto.XunhupayPaymentResponse;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.entity.PaymentNotifyLog;
import com.aichuangzuo.user.modules.membership.payment.xunhupay.mapper.PaymentNotifyLogMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.service.PaymentService;
import com.aichuangzuo.user.modules.membership.vo.PaymentConfigVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final long CONFIG_ID = 1L;
    private static final String TEST_PAY_CODE = "123456";
    private static final String ORDER_NO_PREFIX = "SUB";
    private static final String PAYMENT_METHOD_XUNHUPAY = "xunhupay";
    private static final BigDecimal COIN_TO_YUAN_RATIO = new BigDecimal("10");
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");
    private static final int EFFECTIVE_STATUS = 1;

    private static final String NEWCOMER_PLAN_KEY = "flagship";
    private static final String NEWCOMER_CYCLE = "year";
    private static final BigDecimal NEWCOMER_EXTRA_DISCOUNT = new BigDecimal("0.8");

    private static final BigDecimal FIRST_PURCHASE_RATE = new BigDecimal("0.10");
    private static final BigDecimal RENEWAL_RATE = new BigDecimal("0.05");
    private static final String COIN_BIZ_TYPE_INVITE_REWARD = "invite_reward";
    private static final String COIN_BIZ_TYPE_SUBSCRIBE_DISCOUNT = "subscribe_coin_discount";

    private final PaymentConfigMapper paymentConfigMapper;
    private final OrderMapper orderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final UserMapper userMapper;
    private final PlanMapper planMapper;
    private final PaymentNotifyLogMapper paymentNotifyLogMapper;

    private final CoinRecordService coinRecordService;
    private final UserCouponService userCouponService;
    private final EarningsService earningsService;
    private final XunhupayClient xunhupayClient;
    private final MembershipMessageNotifier membershipMessageNotifier;

    @Override
    public PaymentConfigVO getPaymentConfig() {
        PaymentConfig config = paymentConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig();
        }
        PaymentConfigVO vo = new PaymentConfigVO();
        vo.setEnabled(1);
        vo.setTestMode(config.getTestMode());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubscribeResultVO createPaymentOrder(Long userId, SubscribeRequest request) {
        PaymentConfig config = getAndValidateConfig();

        MembershipPlan plan = MembershipPlan.of(request.getPlanKey());
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }
        MembershipCycle cycle = MembershipCycle.of(request.getCycle());
        if (cycle == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }

        boolean upgrade = isUpgrade(userId, plan, cycle);
        if (upgrade) {
            validateUpgradeCycle(userId, request.getCycle());
        }

        BigDecimal expectedAmount = resolveExpectedAmount(userId, plan.getKey(), cycle.getCode(), upgrade);
        BigDecimal couponDiscount = calculateCouponDiscount(userId, request.getCouponCode(), expectedAmount, plan.getKey(), cycle.getCode());
        expectedAmount = expectedAmount.subtract(couponDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal requestedCoinAmount = request.getCoinAmount() == null ? BigDecimal.ZERO : request.getCoinAmount();
        validateCoinAmount(requestedCoinAmount);

        BigDecimal coinBalance = coinRecordService.getBalance(userId);
        long maxCoinAmount = calculateMaxCoinAmount(expectedAmount, coinBalance);
        if (requestedCoinAmount.compareTo(BigDecimal.valueOf(maxCoinAmount)) > 0) {
            throw new BusinessException(MembershipErrorCode.INVALID_COIN_AMOUNT);
        }

        BigDecimal coinDiscountYuan = requestedCoinAmount.divide(COIN_TO_YUAN_RATIO, 2, RoundingMode.HALF_UP);
        BigDecimal finalCashAmount = expectedAmount.subtract(coinDiscountYuan).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (request.getAmount() == null ||
                request.getAmount().subtract(finalCashAmount).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessException(MembershipErrorCode.INVALID_AMOUNT);
        }

        Order order = createPendingOrder(userId, plan, cycle, finalCashAmount,
                requestedCoinAmount.longValue(), coinDiscountYuan, request.getCouponCode(), couponDiscount);

        if (isTestMode(config)) {
            if (!StringUtils.hasText(request.getPayCode()) || !TEST_PAY_CODE.equals(request.getPayCode())) {
                throw new BusinessException(MembershipErrorCode.INVALID_PAY_CODE);
            }
            confirmOrder(order.getOrderNo(), null, true);
            return buildSubscribeResult(order, plan, cycle, userId, true);
        }

        String title = buildPaymentTitle(plan, cycle);
        XunhupayPaymentResponse response = xunhupayClient.createPayment(config, order.getOrderNo(), finalCashAmount, title);
        if (response == null || response.getErrcode() == null || response.getErrcode() != 0) {
            log.error("虎皮椒下单失败 orderNo={}, errcode={}, errmsg={}",
                    order.getOrderNo(),
                    response == null ? null : response.getErrcode(),
                    response == null ? null : response.getErrmsg());
            throw new BusinessException(MembershipErrorCode.PAYMENT_GATEWAY_ERROR);
        }

        SubscribeResultVO result = buildSubscribeResult(order, plan, cycle, userId, false);
        result.setPayUrl(response.getUrl());
        result.setPayQrUrl(response.getUrlQrcode());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(String orderNo, String thirdPartyTradeId, boolean fromTestMode) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.warn("订单不存在 orderNo={}", orderNo);
            throw new BusinessException(MembershipErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != null && order.getStatus() == 1) {
            log.info("订单已支付，跳过确认 orderNo={}", orderNo);
            return;
        }

        MembershipPlan plan = MembershipPlan.of(order.getPlanKey());
        MembershipCycle cycle = MembershipCycle.of(order.getCycle());
        if (plan == null || cycle == null) {
            throw new BusinessException(MembershipErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentMethod(PAYMENT_METHOD_XUNHUPAY);
        order.setThirdPartyTradeId(thirdPartyTradeId);
        orderMapper.updateById(order);

        UserMembership membership = activateOrExtendMembership(order.getUserId(), plan, cycle,
                isUpgrade(order.getUserId(), plan, cycle));

        if (order.getCoinAmount() != null && order.getCoinAmount() > 0) {
            spendCoinDiscount(order.getUserId(), order);
        }
        if (order.getCouponDiscount() != null && order.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0
                && StringUtils.hasText(order.getCouponCode())) {
            userCouponService.markCouponUsed(order.getUserId(), order.getCouponCode(), order.getId());
        }

        membershipMessageNotifier.sendSubscriptionNotification(order.getUserId(), plan, membership);
        boolean rewarded = rewardInviter(order.getUserId(), plan, order);

        log.info("订单确认成功 orderNo={}, userId={}, level={}, days={}, fromTestMode={}",
                orderNo, order.getUserId(), plan.getKey(), cycle.getDays(), fromTestMode);

        if (!fromTestMode) {
            // 异步通知里不返回 VO，仅记录日志
            log.info("邀请奖励发放 rewarded={}, userId={}, orderNo={}", rewarded, order.getUserId(), orderNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleXunhupayNotify(XunhupayNotifyParams params, String rawBody) {
        PaymentConfig config = paymentConfigMapper.selectById(CONFIG_ID);
        if (config == null || !StringUtils.hasText(config.getAppSecret())) {
            log.warn("未配置支付密钥，无法处理回调");
            return;
        }

        String orderNo = params.getTradeOrderId();
        String transactionId = params.getTransactionId();

        PaymentNotifyLog logRecord = new PaymentNotifyLog();
        logRecord.setOrderNo(orderNo);
        logRecord.setNonceStr(params.getNonceStr());
        logRecord.setTradeOrderId(transactionId);
        logRecord.setRawBody(rawBody);
        logRecord.setStatus(0);

        if (!xunhupayClient.verifyNotify(params, config.getAppSecret())) {
            log.error("虎皮椒回调签名验证失败 orderNo={}", orderNo);
            logRecord.setErrorMsg("签名验证失败");
            paymentNotifyLogMapper.insert(logRecord);
            return;
        }

        if (!"OD".equals(params.getStatus())) {
            log.warn("虎皮椒回调状态非成功 orderNo={}, status={}", orderNo, params.getStatus());
            logRecord.setErrorMsg("支付状态未成功: " + params.getStatus());
            paymentNotifyLogMapper.insert(logRecord);
            return;
        }

        try {
            confirmOrder(orderNo, transactionId, false);
            logRecord.setStatus(1);
            paymentNotifyLogMapper.insert(logRecord);
        } catch (Exception e) {
            log.error("虎皮椒回调确认订单失败 orderNo={}", orderNo, e);
            logRecord.setErrorMsg(e.getMessage());
            paymentNotifyLogMapper.insert(logRecord);
            throw e;
        }
    }

    private PaymentConfig getAndValidateConfig() {
        PaymentConfig config = paymentConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            throw new BusinessException(MembershipErrorCode.PAYMENT_NOT_ENABLED);
        }
        return config;
    }

    private boolean isTestMode(PaymentConfig config) {
        return config.getTestMode() != null && config.getTestMode() == 1;
    }

    private PaymentConfig defaultConfig() {
        PaymentConfig config = new PaymentConfig();
        config.setId(CONFIG_ID);
        config.setProvider("xunhupay");
        config.setEnabled(1);
        config.setTestMode(1);
        config.setGatewayUrl("https://api.xunhupay.com/payment/do.html");
        return config;
    }

    private String buildPaymentTitle(MembershipPlan plan, MembershipCycle cycle) {
        String planName = switch (plan) {
            case BASIC -> "基础版";
            case PRO -> "专业版";
            case FLAGSHIP -> "旗舰版";
        };
        String cycleName = switch (cycle) {
            case MONTH -> "月度";
            case QUARTER -> "季度";
            case YEAR -> "年度";
        };
        return planName + cycleName + "会员";
    }

    private SubscribeResultVO buildSubscribeResult(Order order, MembershipPlan plan, MembershipCycle cycle,
                                                    Long userId, boolean completed) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        SubscribeResultVO vo = new SubscribeResultVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setLevel(plan.getKey());
        vo.setCycle(cycle.getCode());
        vo.setDays(cycle.getDays());
        vo.setExpiresAt(membership == null ? "" : membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        vo.setCoinAmount(order.getCoinAmount());
        vo.setCoinDiscountYuan(order.getCoinDiscount());
        vo.setCouponDiscount(order.getCouponDiscount());
        vo.setCashAmount(order.getAmount());
        if (completed) {
            boolean rewarded = alreadyRewarded(order.getId());
            vo.setInviterRewarded(rewarded);
            vo.setRewardAmount(rewarded ? calculateInviteReward(order.getTotalAmount(), isFirstPurchase(userId, order.getId())) : BigDecimal.ZERO);
        }
        return vo;
    }

    private Order createPendingOrder(Long userId, MembershipPlan plan, MembershipCycle cycle,
                                      BigDecimal cashAmount, Long coinAmount, BigDecimal coinDiscountYuan,
                                      String couponCode, BigDecimal couponDiscount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlanKey(plan.getKey());
        order.setCycle(cycle.getCode());
        order.setAmount(cashAmount);
        order.setCoinAmount(coinAmount);
        order.setCoinDiscount(coinDiscountYuan);
        order.setCouponCode(couponCode);
        order.setCouponDiscount(couponDiscount);
        order.setTotalAmount(cashAmount.add(coinDiscountYuan).add(couponDiscount == null ? BigDecimal.ZERO : couponDiscount));
        order.setStatus(0);
        order.setTenantId(0L);
        orderMapper.insert(order);
        return order;
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return ORDER_NO_PREFIX + date + random;
    }

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

    private boolean isNewcomerEligible(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership != null && !membership.getExpiresAt().isBefore(LocalDate.now())) {
            return false;
        }
        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(userId);
        return relation == null;
    }

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
        syncUserMembershipFields(userId, newExpiresAt, plan.getKey());
        return membership;
    }

    private void syncUserMembershipFields(Long userId, LocalDate expiresAt, String planKey) {
        LocalDateTime expireDateTime = expiresAt.plusDays(1).atStartOfDay();
        String plan = expiresAt.isBefore(LocalDate.now()) ? null : planKey;
        userMapper.updateMembershipFields(userId, expireDateTime, plan);
    }

    private void spendCoinDiscount(Long userId, Order order) {
        if (order.getCoinAmount() == null || order.getCoinAmount() <= 0) {
            return;
        }
        Long existing = userCoinRecordMapper.selectCount(
                new LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getUserId, userId)
                        .eq(UserCoinRecord::getBizType, COIN_BIZ_TYPE_SUBSCRIBE_DISCOUNT)
                        .eq(UserCoinRecord::getRefId, order.getId().toString()));
        if (existing != null && existing > 0) {
            log.warn("订阅创作币抵扣已扣减，跳过 userId={}, orderId={}", userId, order.getId());
            return;
        }
        String planName = resolvePlanName(order.getPlanKey());
        String remark = String.format("订阅 %s %s 抵扣 %d 创作币",
                planName, order.getCycle(), order.getCoinAmount());
        coinRecordService.spend(userId, COIN_BIZ_TYPE_SUBSCRIBE_DISCOUNT,
                BigDecimal.valueOf(order.getCoinAmount()), order.getId().toString(), remark);
        earningsService.recordCoinDiscountEarnings(userId, order.getId().toString(),
                order.getPlanKey(), planName, order.getCycle(), BigDecimal.valueOf(order.getCoinAmount()));
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
        BigDecimal reward = calculateInviteReward(order.getTotalAmount(), firstPurchase);

        User invitee = userMapper.selectById(userId);
        String inviteeName = invitee == null ? "好友" : (invitee.getNickname() == null ? "好友" : invitee.getNickname());
        String planName = resolvePlanName(plan.getKey());
        String remark = String.format("%s %s %s，邀请奖励 %s 创作币",
                inviteeName, firstPurchase ? "首次购买" : "续费", planName, reward.toPlainString());

        coinRecordService.grant(inviterId, COIN_BIZ_TYPE_INVITE_REWARD, reward, order.getId().toString(), remark);

        String settlementMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        earningsService.recordInviteRewardEarnings(inviterId, userId, plan.getKey(), planName,
                order.getCycle(), order.getTotalAmount(), firstPurchase,
                firstPurchase ? FIRST_PURCHASE_RATE : RENEWAL_RATE, reward, settlementMonth);
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
                        .eq(UserCoinRecord::getBizType, COIN_BIZ_TYPE_INVITE_REWARD));
        return count != null && count > 0;
    }

    private String resolvePlanName(String planKey) {
        return Map.of("basic", "基础版", "pro", "专业版", "flagship", "旗舰版")
                .getOrDefault(planKey, planKey);
    }
}
