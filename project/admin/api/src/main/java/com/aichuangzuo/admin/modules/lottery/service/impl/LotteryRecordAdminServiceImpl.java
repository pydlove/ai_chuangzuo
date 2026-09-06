package com.aichuangzuo.admin.modules.lottery.service.impl;

import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDrawRecordQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryManualGrantRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryRedemptionCodeQueryRequest;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryDisplayWinner;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryDrawChance;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryDrawRecord;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryRedemptionCode;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDisplayWinnerMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDrawChanceMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDrawRecordMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.admin.modules.lottery.service.LotteryRecordAdminService;
import com.aichuangzuo.admin.modules.lottery.util.LotteryCodeGenerator;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDrawRecordAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryRedemptionCodeAdminVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.enums.error.AdminLotteryErrorCode;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryRecordAdminServiceImpl implements LotteryRecordAdminService {

    private final LotteryRedemptionCodeMapper redemptionCodeMapper;
    private final LotteryDrawRecordMapper drawRecordMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;
    private final LotteryCampaignMapper campaignMapper;
    private final LotteryDrawChanceMapper drawChanceMapper;
    private final LotteryDisplayWinnerMapper displayWinnerMapper;
    private final PlatformUserMapper platformUserMapper;
    private final LotteryCodeGenerator codeGenerator;

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
        Map<Long, LotteryRedemptionCode> codeMap = codeMap(result.getRecords().stream()
                .map(LotteryDrawRecord::getCodeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        List<LotteryDrawRecordAdminVO> items = result.getRecords().stream()
                .map(r -> buildDrawRecordVO(r, tierNameMap, campaignNameMap, userMap, codeMap))
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualGrant(LotteryManualGrantRequest request) {
        LotteryCampaign campaign = campaignMapper.selectById(request.getCampaignId());
        if (campaign == null) {
            throw new BusinessException(AdminLotteryErrorCode.CAMPAIGN_NOT_FOUND);
        }
        LotteryPrizeTier tier = prizeTierMapper.selectById(request.getTierId());
        if (tier == null || !Objects.equals(tier.getCampaignId(), request.getCampaignId())
                || tier.getIsDeleted() != null && tier.getIsDeleted() == 1) {
            throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_FOUND);
        }
        if ("none".equals(tier.getRewardType())) {
            throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_GRANTABLE);
        }
        PlatformUser user = platformUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }

        // 条件扣减库存，扣不到说明发完了（与用户端抽奖同一套扣减逻辑）
        if (tier.getMaxWinCount() != null) {
            int affected = prizeTierMapper.update(null,
                    new LambdaUpdateWrapper<LotteryPrizeTier>()
                            .eq(LotteryPrizeTier::getId, tier.getId())
                            .gt(LotteryPrizeTier::getRemainingWinCount, 0)
                            .setSql("remaining_win_count = remaining_win_count - 1"));
            if (affected == 0) {
                throw new BusinessException(AdminLotteryErrorCode.TIER_STOCK_EMPTY);
            }
        }

        int codeLength = tier.getCodeLength() != null ? tier.getCodeLength() : 12;
        int validityDays = tier.getCodeValidityDays() != null ? tier.getCodeValidityDays() : 30;

        LotteryRedemptionCode code = new LotteryRedemptionCode();
        code.setCode(codeGenerator.generate(tier.getCodePrefix(), codeLength));
        code.setCampaignId(campaign.getId());
        code.setTierId(tier.getId());
        code.setDrawerUserId(user.getId());
        code.setRewardType(tier.getRewardType());
        code.setRewardValueJson(tier.getRewardValueJson());
        code.setStatus("unused");
        code.setExpiresAt(LocalDateTime.now().plusDays(validityDays));
        code.setTenantId(0L);
        redemptionCodeMapper.insert(code);

        LotteryDrawRecord record = new LotteryDrawRecord();
        record.setBizNo("LD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        record.setCampaignId(campaign.getId());
        record.setUserId(user.getId());
        record.setTierId(tier.getId());
        record.setCodeId(code.getId());
        record.setDrawType("manual");
        record.setTenantId(0L);
        drawRecordMapper.insert(record);

        LotteryDisplayWinner winner = new LotteryDisplayWinner();
        winner.setCampaignId(campaign.getId());
        winner.setTierId(tier.getId());
        winner.setUserId(user.getId());
        winner.setNickname(user.getNickname());
        winner.setAvatarUrl(user.getAvatarUrl());
        winner.setPrizeName(tier.getTierName());
        winner.setWinTime(LocalDateTime.now());
        winner.setIsReal(1);
        winner.setSortOrder(0);
        winner.setStatus(1);
        winner.setCodeId(code.getId());
        winner.setTenantId(0L);
        displayWinnerMapper.insert(winner);

        log.info("管理员人工发奖, campaignId={}, tierId={}, userId={}, code={}",
                campaign.getId(), tier.getId(), user.getId(), code.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteManualGrant(Long recordId) {
        LotteryDrawRecord record = requireManualRecord(recordId);
        LotteryRedemptionCode code = record.getCodeId() != null
                ? redemptionCodeMapper.selectById(record.getCodeId()) : null;
        if (code != null && "used".equals(code.getStatus())) {
            throw new BusinessException(AdminLotteryErrorCode.RECORD_CODE_USED);
        }

        // 释放奖项额度（不超过上限）
        LotteryPrizeTier tier = prizeTierMapper.selectById(record.getTierId());
        if (tier != null && tier.getMaxWinCount() != null) {
            prizeTierMapper.update(null,
                    new LambdaUpdateWrapper<LotteryPrizeTier>()
                            .eq(LotteryPrizeTier::getId, tier.getId())
                            .lt(LotteryPrizeTier::getRemainingWinCount, tier.getMaxWinCount())
                            .setSql("remaining_win_count = remaining_win_count + 1"));
        }

        if (code != null) {
            redemptionCodeMapper.deleteById(code.getId());
        }
        LotteryDisplayWinner winner = findDisplayWinner(record, code);
        if (winner != null) {
            displayWinnerMapper.deleteById(winner.getId());
        }
        drawRecordMapper.deleteById(record.getId());

        log.info("管理员删除人工发奖记录, recordId={}, campaignId={}, tierId={}, userId={}, releaseStock={}",
                record.getId(), record.getCampaignId(), record.getTierId(), record.getUserId(),
                tier != null && tier.getMaxWinCount() != null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeManualGrantUser(Long recordId, Long newUserId) {
        LotteryDrawRecord record = requireManualRecord(recordId);
        LotteryRedemptionCode code = record.getCodeId() != null
                ? redemptionCodeMapper.selectById(record.getCodeId()) : null;
        if (code == null) {
            throw new BusinessException(AdminLotteryErrorCode.RECORD_CODE_NOT_UNUSED);
        }
        if ("used".equals(code.getStatus())) {
            throw new BusinessException(AdminLotteryErrorCode.RECORD_CODE_USED);
        }
        if (!"unused".equals(code.getStatus())) {
            throw new BusinessException(AdminLotteryErrorCode.RECORD_CODE_NOT_UNUSED);
        }
        PlatformUser user = platformUserMapper.selectById(newUserId);
        if (user == null) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }

        Long oldUserId = record.getUserId();
        // 先按旧记录定位展示墙（历史数据按 活动+奖项+用户 兜底匹配，需在改 userId 之前查）
        LotteryDisplayWinner winner = findDisplayWinner(record, code);

        record.setUserId(user.getId());
        drawRecordMapper.updateById(record);

        code.setDrawerUserId(user.getId());
        redemptionCodeMapper.updateById(code);

        if (winner != null) {
            winner.setUserId(user.getId());
            winner.setNickname(user.getNickname());
            winner.setAvatarUrl(user.getAvatarUrl());
            displayWinnerMapper.updateById(winner);
        }

        log.info("管理员修改人工发奖获奖人, recordId={}, campaignId={}, tierId={}, oldUserId={}, newUserId={}",
                record.getId(), record.getCampaignId(), record.getTierId(), oldUserId, user.getId());
    }

    private LotteryDrawRecord requireManualRecord(Long recordId) {
        LotteryDrawRecord record = drawRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(AdminLotteryErrorCode.RECORD_NOT_FOUND);
        }
        if (!"manual".equals(record.getDrawType())) {
            throw new BusinessException(AdminLotteryErrorCode.RECORD_NOT_MANUAL);
        }
        return record;
    }

    private LotteryDisplayWinner findDisplayWinner(LotteryDrawRecord record, LotteryRedemptionCode code) {
        if (code != null && code.getId() != null) {
            LotteryDisplayWinner matched = displayWinnerMapper.selectOne(
                    new LambdaQueryWrapper<LotteryDisplayWinner>()
                            .eq(LotteryDisplayWinner::getCodeId, code.getId())
                            .last("LIMIT 1"));
            if (matched != null) {
                return matched;
            }
        }
        // 兼容迁移前的历史数据：按活动+奖项+用户匹配最近一条真实中奖展示
        return displayWinnerMapper.selectOne(
                new LambdaQueryWrapper<LotteryDisplayWinner>()
                        .eq(LotteryDisplayWinner::getCampaignId, record.getCampaignId())
                        .eq(LotteryDisplayWinner::getTierId, record.getTierId())
                        .eq(LotteryDisplayWinner::getUserId, record.getUserId())
                        .eq(LotteryDisplayWinner::getIsReal, 1)
                        .orderByDesc(LotteryDisplayWinner::getId)
                        .last("LIMIT 1"));
    }

    @Override
    public void resetDrawChance(Long campaignId, Long userId) {
        drawChanceMapper.update(null,
                new UpdateWrapper<LotteryDrawChance>()
                        .eq("campaign_id", campaignId)
                        .eq("user_id", userId)
                        .eq("status", "used")
                        .set("status", "available")
                        .set("used_at", null));
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

    private Map<Long, LotteryRedemptionCode> codeMap(List<Long> codeIds) {
        if (codeIds.isEmpty()) {
            return Map.of();
        }
        List<LotteryRedemptionCode> codes = redemptionCodeMapper.selectList(
                new LambdaQueryWrapper<LotteryRedemptionCode>()
                        .in(LotteryRedemptionCode::getId, codeIds));
        return codes.stream().collect(Collectors.toMap(LotteryRedemptionCode::getId, c -> c));
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

    private LotteryDrawRecordAdminVO buildDrawRecordVO(LotteryDrawRecord record, Map<Long, String> tierNameMap, Map<Long, String> campaignNameMap, Map<Long, PlatformUser> userMap, Map<Long, LotteryRedemptionCode> codeMap) {
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
        LotteryRedemptionCode code = codeMap.get(record.getCodeId());
        if (code != null) {
            vo.setCode(code.getCode());
            vo.setCodeStatus(code.getStatus());
        }
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
