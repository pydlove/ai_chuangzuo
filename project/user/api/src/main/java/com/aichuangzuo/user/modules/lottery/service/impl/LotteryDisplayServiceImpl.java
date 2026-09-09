package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.shared.enums.DeletedFlagEnum;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.user.modules.lottery.entity.LotteryDisplayWinner;
import com.aichuangzuo.user.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.user.modules.lottery.entity.LotteryRedemptionCode;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryDisplayWinnerMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryDisplayService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerPageVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionCodeVO;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryDisplayServiceImpl implements LotteryDisplayService {

    private final LotteryCampaignMapper campaignMapper;
    private final LotteryDisplayWinnerMapper displayWinnerMapper;
    private final LotteryRedemptionCodeMapper redemptionCodeMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;
    private final PlanLookupService planLookupService;
    private final UserMapper userMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final ObjectMapper objectMapper;

    @Override
    public LotteryCampaign getCurrentCampaign() {
        return campaignMapper.selectOne(
                new LambdaQueryWrapper<LotteryCampaign>()
                        .eq(LotteryCampaign::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                        .eq(LotteryCampaign::getStatus, 1)
                        .le(LotteryCampaign::getStartTime, LocalDateTime.now())
                        .ge(LotteryCampaign::getEndTime, LocalDateTime.now())
                        .orderByDesc(LotteryCampaign::getStartTime)
                        .last("LIMIT 1"));
    }

    @Override
    public LotteryCampaign getCampaignById(Long campaignId) {
        return campaignMapper.selectById(campaignId);
    }

    @Override
    public List<LotteryPrizeTier> listActiveTiersByCampaignId(Long campaignId) {
        return prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaignId)
                        .eq(LotteryPrizeTier::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                        .eq(LotteryPrizeTier::getStatus, 1)
                        .orderByAsc(LotteryPrizeTier::getSortOrder)
                        .orderByAsc(LotteryPrizeTier::getPrizeLevel));
    }

    private Map<Long, Integer> loadTierLevelMap(List<LotteryDisplayWinner> winners) {
        List<Long> tierIds = winners.stream()
                .map(LotteryDisplayWinner::getTierId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tierIds)) {
            return Map.of();
        }
        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .in(LotteryPrizeTier::getId, tierIds)
                        .isNotNull(LotteryPrizeTier::getPrizeLevel));
        return tiers.stream().collect(Collectors.toMap(LotteryPrizeTier::getId, LotteryPrizeTier::getPrizeLevel, (a, b) -> a));
    }

    @Override
    public LotteryDisplayWinnerPageVO listDisplayWinnersPage(Long campaignId, int page, int pageSize) {
        Page<LotteryDisplayWinner> mpPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<LotteryDisplayWinner> wrapper = new LambdaQueryWrapper<LotteryDisplayWinner>()
                .eq(LotteryDisplayWinner::getCampaignId, campaignId)
                .eq(LotteryDisplayWinner::getStatus, 1)
                .orderByDesc(LotteryDisplayWinner::getWinTime)
                .orderByAsc(LotteryDisplayWinner::getSortOrder);
        Page<LotteryDisplayWinner> result = displayWinnerMapper.selectPage(mpPage, wrapper);
        Map<Long, Integer> tierLevelMap = loadTierLevelMap(result.getRecords());
        Map<Long, String> tierNameMap = loadWinnerTierNameMap(result.getRecords());
        Map<Long, User> currentUserMap = loadCurrentUserMap(result.getRecords());
        Map<Long, String> memberLevelMap = loadMemberLevelMap(result.getRecords());
        LotteryDisplayWinnerPageVO vo = new LotteryDisplayWinnerPageVO();
        vo.setList(result.getRecords().stream().map(w -> buildDisplayWinnerVO(w, tierLevelMap, tierNameMap, currentUserMap, memberLevelMap)).collect(Collectors.toList()));
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public List<LotteryDisplayWinnerVO> listGrandWinners(Long campaignId) {
        List<LotteryPrizeTier> grandTiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaignId)
                        .eq(LotteryPrizeTier::getIsDeleted, DeletedFlagEnum.NOT_DELETED.getCode())
                        .in(LotteryPrizeTier::getPrizeLevel, 1, 2, 3));
        if (CollectionUtils.isEmpty(grandTiers)) {
            return List.of();
        }
        Map<Long, Integer> tierLevelMap = grandTiers.stream()
                .collect(Collectors.toMap(LotteryPrizeTier::getId, LotteryPrizeTier::getPrizeLevel, (a, b) -> a));
        List<Long> tierIds = List.copyOf(tierLevelMap.keySet());
        List<LotteryDisplayWinner> winners = displayWinnerMapper.selectList(
                new LambdaQueryWrapper<LotteryDisplayWinner>()
                        .eq(LotteryDisplayWinner::getCampaignId, campaignId)
                        .eq(LotteryDisplayWinner::getStatus, 1)
                        .in(LotteryDisplayWinner::getTierId, tierIds)
                        .orderByAsc(LotteryDisplayWinner::getSortOrder)
                        .orderByDesc(LotteryDisplayWinner::getWinTime));
        Map<Long, User> currentUserMap = loadCurrentUserMap(winners);
        Map<Long, String> memberLevelMap = loadMemberLevelMap(winners);
        Map<Long, String> tierNameMap = grandTiers.stream()
                .collect(Collectors.toMap(LotteryPrizeTier::getId, LotteryPrizeTier::getTierName, (a, b) -> a));
        return winners.stream()
                .map(w -> buildDisplayWinnerVO(w, tierLevelMap, tierNameMap, currentUserMap, memberLevelMap))
                .sorted(Comparator.comparing((LotteryDisplayWinnerVO w) -> w.getPrizeLevel() != null ? w.getPrizeLevel() : 99)
                        .thenComparing((LotteryDisplayWinnerVO w) -> w.getWinTime(), Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
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
        return queryTierNameMap(tierIds);
    }

    /**
     * 查询奖项当前名称，避免展示墙停留在配置修改前的快照。
     *
     * @return tierId -> 当前奖项名称
     */
    private Map<Long, String> loadWinnerTierNameMap(List<LotteryDisplayWinner> winners) {
        List<Long> tierIds = winners.stream()
                .map(LotteryDisplayWinner::getTierId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return queryTierNameMap(tierIds);
    }

    private Map<Long, String> queryTierNameMap(List<Long> tierIds) {
        if (CollectionUtils.isEmpty(tierIds)) {
            return Map.of();
        }
        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .in(LotteryPrizeTier::getId, tierIds));
        return tiers.stream().collect(Collectors.toMap(LotteryPrizeTier::getId, LotteryPrizeTier::getTierName, (a, b) -> a));
    }

    /**
     * 查询中奖用户当前资料，避免展示墙停留在抽奖时刻的快照。
     *
     * @return userId -> 用户实体（仅含当前头像与昵称非空的记录）
     */
    private Map<Long, User> loadCurrentUserMap(List<LotteryDisplayWinner> winners) {
        List<Long> userIds = winners.stream()
                .map(LotteryDisplayWinner::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(u -> (u.getAvatarUrl() != null && !u.getAvatarUrl().isBlank())
                        || (u.getNickname() != null && !u.getNickname().isBlank()))
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 兼容旧版与管理端配置的头像 URL。
     *
     * <p>旧数据可能是 /uploads/...，管理端运营配置的头像可能是 /api/v1/admin/uploads/...，
     * 统一改写为用户端可访问的 /api/v1/user/uploads/...。
     */
    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        if (avatarUrl.startsWith("/uploads/")) {
            return "/api/v1/user" + avatarUrl;
        }
        if (avatarUrl.startsWith("/api/v1/admin/uploads/")) {
            return "/api/v1/user/uploads/" + avatarUrl.substring("/api/v1/admin/uploads/".length());
        }
        return avatarUrl;
    }

    /**
     * 批量查询中奖用户当前有效会员等级（basic/pro/flagship），用于头像 V 标展示。
     *
     * @return userId -> 会员等级；非会员或已过期不放入 Map
     */
    private Map<Long, String> loadMemberLevelMap(List<LotteryDisplayWinner> winners) {
        List<Long> userIds = winners.stream()
                .map(LotteryDisplayWinner::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            return Map.of();
        }
        return userMembershipMapper.selectList(
                        new LambdaQueryWrapper<UserMembership>()
                                .in(UserMembership::getUserId, userIds)
                                .ge(UserMembership::getExpiresAt, LocalDate.now()))
                .stream()
                .collect(Collectors.toMap(UserMembership::getUserId, UserMembership::getLevel, (a, b) -> a));
    }

    private LotteryDisplayWinnerVO buildDisplayWinnerVO(LotteryDisplayWinner winner, Map<Long, Integer> tierLevelMap,
                                                        Map<Long, String> tierNameMap, Map<Long, User> currentUserMap,
                                                        Map<Long, String> memberLevelMap) {
        LotteryDisplayWinnerVO vo = new LotteryDisplayWinnerVO();
        vo.setId(winner.getId());
        User currentUser = winner.getUserId() != null ? currentUserMap.get(winner.getUserId()) : null;
        String currentNickname = currentUser != null && currentUser.getNickname() != null && !currentUser.getNickname().isBlank()
                ? currentUser.getNickname() : winner.getNickname();
        vo.setNickname(maskNickname(currentNickname));
        String currentAvatar = currentUser != null && currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isBlank()
                ? normalizeAvatarUrl(currentUser.getAvatarUrl()) : null;
        vo.setAvatarUrl(currentAvatar != null ? currentAvatar : normalizeAvatarUrl(winner.getAvatarUrl()));
        String currentPrizeName = winner.getTierId() != null ? tierNameMap.get(winner.getTierId()) : null;
        vo.setPrizeName(currentPrizeName != null && !currentPrizeName.isBlank() ? currentPrizeName : winner.getPrizeName());
        vo.setPrizeLevel(tierLevelMap.get(winner.getTierId()));
        vo.setWinTime(winner.getWinTime());
        vo.setIsReal(winner.getIsReal());
        vo.setMemberLevel(winner.getUserId() != null ? memberLevelMap.get(winner.getUserId()) : null);
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
