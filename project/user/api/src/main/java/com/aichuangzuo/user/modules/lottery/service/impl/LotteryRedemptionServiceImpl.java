package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.lottery.entity.LotteryRedemptionCode;
import com.aichuangzuo.user.modules.lottery.entity.UserCoupon;
import com.aichuangzuo.user.modules.lottery.entity.UserMembershipPending;
import com.aichuangzuo.user.modules.lottery.enums.LotteryErrorCode;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.user.modules.lottery.mapper.UserCouponMapper;
import com.aichuangzuo.user.modules.lottery.mapper.UserMembershipPendingMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryRedemptionService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionResultVO;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryRedemptionServiceImpl implements LotteryRedemptionService {

    private static final String BIZ_TYPE_LOTTERY_COIN = "lottery_coin_reward";

    private final LotteryRedemptionCodeMapper redemptionCodeMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserMembershipPendingMapper pendingMembershipMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final CoinRecordService coinRecordService;
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryRedemptionResultVO redeem(Long userId, String code) {
        LotteryRedemptionCode codeEntity = redemptionCodeMapper.selectOne(
                new LambdaQueryWrapper<LotteryRedemptionCode>()
                        .eq(LotteryRedemptionCode::getCode, code));
        if (codeEntity == null) {
            throw new BusinessException(LotteryErrorCode.REDEMPTION_CODE_NOT_FOUND);
        }
        if ("used".equals(codeEntity.getStatus())) {
            throw new BusinessException(LotteryErrorCode.REDEMPTION_CODE_USED);
        }
        if (codeEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(LotteryErrorCode.REDEMPTION_CODE_EXPIRED);
        }

        applyReward(userId, codeEntity);

        codeEntity.setStatus("used");
        codeEntity.setUsedBy(userId);
        codeEntity.setUsedAt(LocalDateTime.now());
        redemptionCodeMapper.updateById(codeEntity);

        log.info("用户兑换奖励 userId={}, code={}", userId, code);

        LotteryRedemptionResultVO vo = new LotteryRedemptionResultVO();
        vo.setRewardType(codeEntity.getRewardType());
        vo.setRewardValueJson(codeEntity.getRewardValueJson());
        vo.setMessage("兑换成功");
        return vo;
    }

    private void applyReward(Long userId, LotteryRedemptionCode code) {
        try {
            JsonNode node = objectMapper.readTree(code.getRewardValueJson());
            switch (code.getRewardType()) {
                case "coin" -> applyCoin(userId, node, code.getId());
                case "membership" -> applyMembership(userId, node, code.getId());
                case "coupon" -> applyCoupon(userId, node, code.getId());
                default -> throw new BusinessException(LotteryErrorCode.INVALID_REWARD_TYPE);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析奖励参数失败 codeId={}", code.getId(), e);
            throw new BusinessException(LotteryErrorCode.INVALID_REWARD_TYPE);
        }
    }

    private void applyCoin(Long userId, JsonNode node, Long codeId) {
        BigDecimal amount = new BigDecimal(node.get("amount").asText());
        coinRecordService.grant(userId, BIZ_TYPE_LOTTERY_COIN, amount, codeId.toString(), "抽奖获得创作币");
    }

    private void applyMembership(Long userId, JsonNode node, Long codeId) {
        String planKey = node.get("plan_key").asText();
        int days;
        if (node.has("days")) {
            days = node.get("days").asInt();
        } else if (node.has("cycle")) {
            MembershipCycle cycle = MembershipCycle.of(node.get("cycle").asText());
            if (cycle == null) {
                throw new BusinessException(LotteryErrorCode.INVALID_REWARD_TYPE);
            }
            days = cycle.getDays();
        } else {
            throw new BusinessException(LotteryErrorCode.INVALID_REWARD_TYPE);
        }

        UserMembership current = userMembershipMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        boolean hasActive = current != null && !current.getExpiresAt().isBefore(today);

        if (!hasActive || planKey.equals(current.getLevel())) {
            membershipService.extendMembership(userId, planKey, days);
        } else {
            UserMembershipPending pending = new UserMembershipPending();
            pending.setUserId(userId);
            pending.setPlanKey(planKey);
            pending.setDays(days);
            pending.setPlannedStartAt(current.getExpiresAt());
            pending.setStatus("pending");
            pending.setSourceCodeId(codeId);
            pendingMembershipMapper.insert(pending);
        }
    }

    private void applyCoupon(Long userId, JsonNode node, Long codeId) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setCouponCode(generateCouponCode(codeId));
        coupon.setCouponType(node.get("coupon_type").asText());
        coupon.setDiscountValue(new BigDecimal(node.get("discount_value").asText()));
        coupon.setApplicableCycle(node.has("applicable_cycle") ? node.get("applicable_cycle").asText() : "all");
        coupon.setApplicablePlan(node.has("applicable_plan") ? node.get("applicable_plan").asText() : "all");
        coupon.setStatus("unused");
        coupon.setValidStart(LocalDateTime.now());
        coupon.setValidEnd(LocalDateTime.now().plusDays(365));
        coupon.setTenantId(0L);
        userCouponMapper.insert(coupon);
    }

    private String generateCouponCode(Long codeId) {
        return "CP" + codeId + System.currentTimeMillis();
    }
}
