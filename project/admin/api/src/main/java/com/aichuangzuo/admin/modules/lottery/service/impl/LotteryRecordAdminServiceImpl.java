package com.aichuangzuo.admin.modules.lottery.service.impl;

import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDrawRecordQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryRedemptionCodeQueryRequest;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryDrawChance;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryDrawRecord;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryRedemptionCode;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDrawChanceMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDrawRecordMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.admin.modules.lottery.service.LotteryRecordAdminService;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDrawRecordAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryRedemptionCodeAdminVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryRecordAdminServiceImpl implements LotteryRecordAdminService {

    private final LotteryRedemptionCodeMapper redemptionCodeMapper;
    private final LotteryDrawRecordMapper drawRecordMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;
    private final LotteryCampaignMapper campaignMapper;
    private final LotteryDrawChanceMapper drawChanceMapper;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public PageResult<LotteryRedemptionCodeAdminVO> listRedemptionCodes(LotteryRedemptionCodeQueryRequest request) {
        Page<LotteryRedemptionCode> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<LotteryRedemptionCode> wrapper = new LambdaQueryWrapper<LotteryRedemptionCode>()
                .orderByDesc(LotteryRedemptionCode::getCreatedAt);
        if (request.getCampaignId() != null) {
            wrapper.eq(LotteryRedemptionCode::getCampaignId, request.getCampaignId());
        }
        if (request.getTierId() != null) {
            wrapper.eq(LotteryRedemptionCode::getTierId, request.getTierId());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            wrapper.eq(LotteryRedemptionCode::getStatus, request.getStatus());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(LotteryRedemptionCode::getCreatedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(LotteryRedemptionCode::getCreatedAt, request.getEndTime());
        }
        if (request.getUserKeyword() != null && !request.getUserKeyword().isBlank()) {
            String keyword = request.getUserKeyword().trim();
            LambdaQueryWrapper<PlatformUser> userWrapper = new LambdaQueryWrapper<PlatformUser>()
                    .like(PlatformUser::getNickname, keyword)
                    .or()
                    .like(PlatformUser::getEmail, keyword);
            List<Long> matchedUserIds = platformUserMapper.selectList(userWrapper)
                    .stream()
                    .map(PlatformUser::getId)
                    .distinct()
                    .collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return new PageResult<>(List.<LotteryRedemptionCodeAdminVO>of(), 0L, page.getCurrent(), page.getSize());
            }
            wrapper.in(LotteryRedemptionCode::getDrawerUserId, matchedUserIds);
        }
        Page<LotteryRedemptionCode> result = redemptionCodeMapper.selectPage(page, wrapper);
        List<Long> campaignIds = collectCampaignIds(result.getRecords());
        Map<Long, String> tierNameMap = tierNameMap(campaignIds);
        Map<Long, String> campaignNameMap = campaignNameMap(campaignIds);
        Map<Long, PlatformUser> userMap = userMap(collectUserIds(result.getRecords()));
        List<LotteryRedemptionCodeAdminVO> items = result.getRecords().stream()
                .map(r -> buildRedemptionCodeVO(r, tierNameMap, campaignNameMap, userMap))
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<LotteryDrawRecordAdminVO> listDrawRecords(LotteryDrawRecordQueryRequest request) {
        List<Long> matchedUserIds = matchUserIdsByEmailOrNickname(request.getEmail(), request.getNickname());
        Page<LotteryDrawRecord> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<LotteryDrawRecord> wrapper = new LambdaQueryWrapper<LotteryDrawRecord>()
                .orderByDesc(LotteryDrawRecord::getCreatedAt);
        if (request.getDrawType() != null && !request.getDrawType().isBlank()) {
            wrapper.eq(LotteryDrawRecord::getDrawType, request.getDrawType());
        }
        if (matchedUserIds != null) {
            if (matchedUserIds.isEmpty()) {
                return new PageResult<>(List.of(), 0L, page.getCurrent(), page.getSize());
            }
            wrapper.in(LotteryDrawRecord::getUserId, matchedUserIds);
        }
        if (request.getStartTime() != null) {
            wrapper.ge(LotteryDrawRecord::getCreatedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(LotteryDrawRecord::getCreatedAt, request.getEndTime());
        }
        Page<LotteryDrawRecord> result = drawRecordMapper.selectPage(page, wrapper);
        List<Long> campaignIds = result.getRecords().stream()
                .map(LotteryDrawRecord::getCampaignId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> tierNameMap = tierNameMap(campaignIds);
        Map<Long, String> campaignNameMap = campaignNameMap(campaignIds);
        Map<Long, PlatformUser> userMap = userMap(result.getRecords().stream()
                .map(LotteryDrawRecord::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        List<LotteryDrawRecordAdminVO> items = result.getRecords().stream()
                .map(r -> buildDrawRecordVO(r, tierNameMap, campaignNameMap, userMap))
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void resetDrawChance(Long campaignId, Long userId) {
        LotteryDrawChance chance = new LotteryDrawChance();
        chance.setCampaignId(campaignId);
        chance.setUserId(userId);
        chance.setChanceType("invite");
        chance.setStatus("available");
        chance.setTenantId(0L);
        drawChanceMapper.insert(chance);
    }

    private Map<Long, String> tierNameMap(List<Long> campaignIds) {
        if (campaignIds.isEmpty()) {
            return Map.of();
        }
        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .in(LotteryPrizeTier::getCampaignId, campaignIds)
                        .eq(LotteryPrizeTier::getIsDeleted, 0));
        return tiers.stream().collect(Collectors.toMap(LotteryPrizeTier::getId, LotteryPrizeTier::getTierName));
    }

    private Map<Long, String> campaignNameMap(List<Long> campaignIds) {
        if (campaignIds.isEmpty()) {
            return Map.of();
        }
        List<LotteryCampaign> campaigns = campaignMapper.selectList(
                new LambdaQueryWrapper<LotteryCampaign>()
                        .in(LotteryCampaign::getId, campaignIds));
        return campaigns.stream().collect(Collectors.toMap(LotteryCampaign::getId, LotteryCampaign::getName));
    }

    private Map<Long, PlatformUser> userMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<PlatformUser> users = platformUserMapper.selectList(
                new LambdaQueryWrapper<PlatformUser>()
                        .in(PlatformUser::getId, userIds));
        return users.stream().collect(Collectors.toMap(PlatformUser::getId, u -> u));
    }

    private List<Long> matchUserIdsByEmailOrNickname(String email, String nickname) {
        boolean hasCondition = (email != null && !email.isBlank())
                || (nickname != null && !nickname.isBlank());
        if (!hasCondition) {
            return null;
        }
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<PlatformUser>();
        if (email != null && !email.isBlank()) {
            wrapper.like(PlatformUser::getEmail, email);
        }
        if (nickname != null && !nickname.isBlank()) {
            if (email != null && !email.isBlank()) {
                wrapper.or();
            }
            wrapper.like(PlatformUser::getNickname, nickname);
        }
        List<PlatformUser> users = platformUserMapper.selectList(wrapper);
        return users.stream().map(PlatformUser::getId).distinct().collect(Collectors.toList());
    }

    private List<Long> collectCampaignIds(List<LotteryRedemptionCode> records) {
        return records.stream()
                .map(LotteryRedemptionCode::getCampaignId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private LotteryRedemptionCodeAdminVO buildRedemptionCodeVO(LotteryRedemptionCode code, Map<Long, String> tierNameMap, Map<Long, String> campaignNameMap, Map<Long, PlatformUser> userMap) {
        LotteryRedemptionCodeAdminVO vo = new LotteryRedemptionCodeAdminVO();
        vo.setId(code.getId());
        vo.setCode(code.getCode());
        vo.setCampaignId(code.getCampaignId());
        vo.setCampaignName(campaignNameMap.getOrDefault(code.getCampaignId(), ""));
        vo.setTierId(code.getTierId());
        vo.setTierName(tierNameMap.getOrDefault(code.getTierId(), ""));
        vo.setDrawerUserId(code.getDrawerUserId());
        PlatformUser user = userMap.get(code.getDrawerUserId());
        vo.setUserDisplay(user != null ? formatUserDisplay(user) : String.valueOf(code.getDrawerUserId()));
        vo.setRewardType(code.getRewardType());
        vo.setRewardValueJson(code.getRewardValueJson());
        vo.setRewardContent(formatRewardContent(code.getRewardType(), code.getRewardValueJson()));
        vo.setStatus(code.getStatus());
        vo.setUsedBy(code.getUsedBy());
        vo.setUsedAt(code.getUsedAt());
        vo.setExpiresAt(code.getExpiresAt());
        vo.setCreatedAt(code.getCreatedAt());
        return vo;
    }

    private LotteryDrawRecordAdminVO buildDrawRecordVO(LotteryDrawRecord record, Map<Long, String> tierNameMap, Map<Long, String> campaignNameMap, Map<Long, PlatformUser> userMap) {
        LotteryDrawRecordAdminVO vo = new LotteryDrawRecordAdminVO();
        vo.setId(record.getId());
        vo.setBizNo(record.getBizNo());
        vo.setCampaignId(record.getCampaignId());
        vo.setCampaignName(campaignNameMap.getOrDefault(record.getCampaignId(), ""));
        vo.setUserId(record.getUserId());
        PlatformUser user = userMap.get(record.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setEmail(user.getEmail());
        }
        vo.setTierId(record.getTierId());
        vo.setTierName(tierNameMap.getOrDefault(record.getTierId(), ""));
        vo.setCodeId(record.getCodeId());
        vo.setDrawType(record.getDrawType());
        vo.setInviteRelationId(record.getInviteRelationId());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private List<Long> collectUserIds(List<LotteryRedemptionCode> records) {
        return records.stream()
                .map(LotteryRedemptionCode::getDrawerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String formatUserDisplay(PlatformUser user) {
        if (user == null) {
            return "";
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getNickname() != null && !user.getNickname().isBlank()
                    ? user.getEmail() + " / " + user.getNickname()
                    : user.getEmail();
        }
        return user.getNickname() != null ? user.getNickname() : "";
    }

    private String formatRewardContent(String rewardType, String rewardValueJson) {
        if (rewardType == null) {
            return "";
        }
        try {
            Map<String, Object> params = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    rewardValueJson != null ? rewardValueJson : "{}", Map.class);
            switch (rewardType) {
                case "coin" -> {
                    Object amount = params.get("amount");
                    return amount != null ? amount + " 创作币" : "创作币";
                }
                case "membership" -> {
                    Object planKey = params.get("plan_key");
                    Object days = params.get("days");
                    Object cycle = params.get("cycle");
                    String plan = planKey != null ? String.valueOf(planKey) : "会员";
                    if (days != null) {
                        return plan + " 会员 " + days + " 天";
                    }
                    if (cycle != null) {
                        String cycleText = switch (String.valueOf(cycle)) {
                            case "month" -> "1 个月";
                            case "quarter" -> "1 个季度";
                            case "year" -> "1 年";
                            default -> String.valueOf(cycle);
                        };
                        return plan + " 会员 " + cycleText;
                    }
                    return plan + " 会员";
                }
                case "coupon" -> {
                    Object couponType = params.get("coupon_type");
                    Object discountValue = params.get("discount_value");
                    if ("percent".equals(couponType) && discountValue != null) {
                        return discountValue + " 折券";
                    }
                    if ("fixed".equals(couponType) && discountValue != null) {
                        return "抵扣 " + discountValue + " 元";
                    }
                    return "折扣券";
                }
                case "none" -> {
                    return "谢谢回顾";
                }
                default -> {
                    return "奖励";
                }
            }
        } catch (Exception e) {
            return "奖励";
        }
    }
}
