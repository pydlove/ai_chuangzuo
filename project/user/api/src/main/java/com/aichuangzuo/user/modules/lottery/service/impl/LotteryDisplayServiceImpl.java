package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.user.modules.lottery.entity.LotteryDisplayWinner;
import com.aichuangzuo.user.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.user.modules.lottery.entity.LotteryRedemptionCode;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryDisplayWinnerMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryDisplayService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionCodeVO;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryDisplayServiceImpl implements LotteryDisplayService {

    private final LotteryDisplayWinnerMapper displayWinnerMapper;
    private final LotteryRedemptionCodeMapper redemptionCodeMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;
    private final PlanLookupService planLookupService;
    private final ObjectMapper objectMapper;

    @Override
    public List<LotteryDisplayWinnerVO> listDisplayWinners(Long campaignId, int limit) {
        List<LotteryDisplayWinner> list = displayWinnerMapper.selectList(
                new LambdaQueryWrapper<LotteryDisplayWinner>()
                        .eq(LotteryDisplayWinner::getCampaignId, campaignId)
                        .eq(LotteryDisplayWinner::getStatus, 1)
                        .orderByDesc(LotteryDisplayWinner::getWinTime)
                        .orderByAsc(LotteryDisplayWinner::getSortOrder)
                        .last("LIMIT " + limit));
        return list.stream().map(this::buildDisplayWinnerVO).collect(Collectors.toList());
    }

    @Override
    public List<LotteryRedemptionCodeVO> listMyRedemptionCodes(Long userId) {
        List<LotteryRedemptionCode> list = redemptionCodeMapper.selectList(
                new LambdaQueryWrapper<LotteryRedemptionCode>()
                        .eq(LotteryRedemptionCode::getDrawerUserId, userId)
                        .orderByDesc(LotteryRedemptionCode::getCreatedAt));
        Map<Long, String> tierNameMap = loadTierNameMap(list);
        return list.stream().map(c -> buildRedemptionCodeVO(c, tierNameMap)).collect(Collectors.toList());
    }

    private Map<Long, String> loadTierNameMap(List<LotteryRedemptionCode> codes) {
        List<Long> tierIds = codes.stream()
                .map(LotteryRedemptionCode::getTierId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tierIds)) {
            return Map.of();
        }
        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .in(LotteryPrizeTier::getId, tierIds));
        return tiers.stream().collect(Collectors.toMap(LotteryPrizeTier::getId, LotteryPrizeTier::getTierName, (a, b) -> a));
    }

    private LotteryDisplayWinnerVO buildDisplayWinnerVO(LotteryDisplayWinner winner) {
        LotteryDisplayWinnerVO vo = new LotteryDisplayWinnerVO();
        vo.setId(winner.getId());
        vo.setNickname(maskNickname(winner.getNickname()));
        vo.setAvatarUrl(winner.getAvatarUrl());
        vo.setPrizeName(winner.getPrizeName());
        vo.setWinTime(winner.getWinTime());
        vo.setIsReal(winner.getIsReal());
        return vo;
    }

    private String maskNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "幸运用户";
        }
        if (nickname.length() <= 2) {
            return nickname.charAt(0) + "*";
        }
        return nickname.charAt(0) + "**" + nickname.charAt(nickname.length() - 1);
    }

    private LotteryRedemptionCodeVO buildRedemptionCodeVO(LotteryRedemptionCode code, Map<Long, String> tierNameMap) {
        LotteryRedemptionCodeVO vo = new LotteryRedemptionCodeVO();
        vo.setId(code.getId());
        vo.setCode(code.getCode());
        vo.setTierName(tierNameMap.getOrDefault(code.getTierId(), ""));
        vo.setRewardType(code.getRewardType());
        vo.setRewardSummary(buildRewardSummary(code.getRewardType(), code.getRewardValueJson()));
        vo.setStatus(code.getStatus());
        vo.setExpiresAt(code.getExpiresAt());
        vo.setUsedAt(code.getUsedAt());
        return vo;
    }

    private String buildRewardSummary(String rewardType, String rewardValueJson) {
        if (!StringUtils.hasText(rewardValueJson)) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(rewardValueJson);
            return switch (rewardType) {
                case "coin" -> buildCoinSummary(node);
                case "membership" -> buildMembershipSummary(node);
                case "coupon" -> buildCouponSummary(node);
                case "none" -> "谢谢参与";
                default -> "";
            };
        } catch (Exception e) {
            log.warn("解析奖励参数失败 rewardType={} rewardValueJson={}", rewardType, rewardValueJson, e);
            return "";
        }
    }

    private String buildCoinSummary(JsonNode node) {
        if (!node.has("amount")) {
            return "";
        }
        BigDecimal amount = new BigDecimal(node.get("amount").asText());
        return amount.stripTrailingZeros().toPlainString() + "创作币";
    }

    private String buildMembershipSummary(JsonNode node) {
        if (!node.has("plan_key")) {
            return "";
        }
        String planKey = node.get("plan_key").asText();
        String planName = planLookupService.getDisplayName(planKey);
        if (!planName.endsWith("会员")) {
            planName = planName + "会员";
        }
        if (node.has("days")) {
            int days = node.get("days").asInt();
            return days + "天" + planName;
        }
        if (node.has("cycle")) {
            MembershipCycle cycle = MembershipCycle.of(node.get("cycle").asText());
            if (cycle == null) {
                return "";
            }
            String cycleLabel = switch (cycle) {
                case MONTH -> "1个月";
                case QUARTER -> "1个季度";
                case YEAR -> "1年";
            };
            return cycleLabel + planName;
        }
        return planName;
    }

    private String buildCouponSummary(JsonNode node) {
        if (!node.has("coupon_type") || !node.has("discount_value")) {
            return "";
        }
        String couponType = node.get("coupon_type").asText();
        BigDecimal discountValue = new BigDecimal(node.get("discount_value").asText());
        if ("percent".equals(couponType)) {
            return discountValue.multiply(BigDecimal.TEN).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "折优惠券";
        }
        return "¥" + discountValue.stripTrailingZeros().toPlainString() + "抵扣券";
    }
}
