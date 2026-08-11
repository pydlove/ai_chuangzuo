package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.lottery.entity.*;
import com.aichuangzuo.user.modules.lottery.enums.LotteryErrorCode;
import com.aichuangzuo.user.modules.lottery.mapper.*;
import com.aichuangzuo.user.modules.lottery.service.LotteryChanceService;
import com.aichuangzuo.user.modules.lottery.service.LotteryDrawService;
import com.aichuangzuo.user.modules.lottery.util.LotteryCodeGenerator;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDrawResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryDrawServiceImpl implements LotteryDrawService {

    private final LotteryCampaignMapper campaignMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;
    private final LotteryDrawChanceMapper drawChanceMapper;
    private final LotteryDrawRecordMapper drawRecordMapper;
    private final LotteryRedemptionCodeMapper redemptionCodeMapper;
    private final LotteryDisplayWinnerMapper displayWinnerMapper;
    private final LotteryChanceService lotteryChanceService;
    private final LotteryCodeGenerator codeGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryDrawResultVO draw(Long userId, Long campaignId) {
        LotteryCampaign campaign = validateCampaign(campaignId);

        LotteryDrawChance chance = lotteryChanceService.acquireOneAvailableChance(campaignId, userId);
        if (chance == null) {
            throw new BusinessException(LotteryErrorCode.NO_DRAW_CHANCE);
        }
        int consumed = lotteryChanceService.consumeChance(chance.getId());
        if (consumed == 0) {
            throw new BusinessException(LotteryErrorCode.NO_DRAW_CHANCE);
        }

        List<LotteryPrizeTier> activeTiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaignId)
                        .eq(LotteryPrizeTier::getStatus, 1)
                        .orderByAsc(LotteryPrizeTier::getSortOrder));

        if (activeTiers.isEmpty()) {
            throw new BusinessException(LotteryErrorCode.PRIZE_TIER_NOT_FOUND);
        }

        LotteryPrizeTier hitTier = selectTierByProbability(activeTiers);

        // 库存上限扣减
        if (!"none".equals(hitTier.getRewardType()) && hitTier.getMaxWinCount() != null) {
            int affected = prizeTierMapper.update(null,
                    new LambdaUpdateWrapper<LotteryPrizeTier>()
                            .eq(LotteryPrizeTier::getId, hitTier.getId())
                            .gt(LotteryPrizeTier::getRemainingWinCount, 0)
                            .setSql("remaining_win_count = remaining_win_count - 1"));
            if (affected == 0) {
                hitTier = findThanksTier(activeTiers);
            }
        }

        LotteryRedemptionCode code = null;
        if (hitTier != null && !"none".equals(hitTier.getRewardType())) {
            code = createRedemptionCode(campaignId, hitTier, userId);
        }

        saveDrawRecord(userId, campaignId, chance, hitTier, code);
        if (code != null) {
            saveDisplayWinner(campaignId, hitTier, userId, code);
        }

        log.info("用户抽奖 userId={}, campaignId={}, tierId={}, tierName={}",
                userId, campaignId, hitTier.getId(), hitTier.getTierName());

        return buildResultVO(hitTier, code);
    }

    private LotteryCampaign validateCampaign(Long campaignId) {
        LotteryCampaign campaign = campaignMapper.selectById(campaignId);
        if (campaign == null) {
            throw new BusinessException(LotteryErrorCode.CAMPAIGN_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStatus() == null || campaign.getStatus() != 1
                || campaign.getStartTime().isAfter(now)
                || campaign.getEndTime().isBefore(now)) {
            throw new BusinessException(LotteryErrorCode.CAMPAIGN_NOT_ONGOING);
        }
        return campaign;
    }

    private LotteryPrizeTier selectTierByProbability(List<LotteryPrizeTier> tiers) {
        double random = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        for (LotteryPrizeTier tier : tiers) {
            if ("none".equals(tier.getRewardType())) {
                continue;
            }
            cumulative += tier.getProbability().doubleValue();
            if (random < cumulative) {
                return tier;
            }
        }
        LotteryPrizeTier thanks = findThanksTier(tiers);
        return thanks != null ? thanks : tiers.get(tiers.size() - 1);
    }

    private LotteryPrizeTier findThanksTier(List<LotteryPrizeTier> tiers) {
        return tiers.stream()
                .filter(t -> "none".equals(t.getRewardType()))
                .findFirst()
                .orElse(null);
    }

    private LotteryRedemptionCode createRedemptionCode(Long campaignId, LotteryPrizeTier tier, Long userId) {
        String code = codeGenerator.generate(tier.getCodePrefix(), tier.getCodeLength());
        LotteryRedemptionCode entity = new LotteryRedemptionCode();
        entity.setCode(code);
        entity.setCampaignId(campaignId);
        entity.setTierId(tier.getId());
        entity.setDrawerUserId(userId);
        entity.setRewardType(tier.getRewardType());
        entity.setRewardValueJson(tier.getRewardValueJson());
        entity.setStatus("unused");
        entity.setExpiresAt(LocalDateTime.now().plusDays(tier.getCodeValidityDays()));
        entity.setTenantId(0L);
        redemptionCodeMapper.insert(entity);
        return entity;
    }

    private void saveDrawRecord(Long userId, Long campaignId, LotteryDrawChance chance,
                                LotteryPrizeTier tier, LotteryRedemptionCode code) {
        LotteryDrawRecord record = new LotteryDrawRecord();
        record.setBizNo(generateBizNo());
        record.setCampaignId(campaignId);
        record.setUserId(userId);
        record.setTierId(tier.getId());
        record.setCodeId(code != null ? code.getId() : null);
        record.setDrawType(chance.getChanceType());
        record.setInviteRelationId(chance.getSourceInviteRelationId());
        record.setTenantId(0L);
        drawRecordMapper.insert(record);
    }

    private void saveDisplayWinner(Long campaignId, LotteryPrizeTier tier, Long userId,
                                   LotteryRedemptionCode code) {
        LotteryDisplayWinner winner = new LotteryDisplayWinner();
        winner.setCampaignId(campaignId);
        winner.setTierId(tier.getId());
        winner.setUserId(userId);
        winner.setPrizeName(tier.getTierName());
        winner.setWinTime(LocalDateTime.now());
        winner.setIsReal(1);
        winner.setSortOrder(0);
        winner.setStatus(1);
        winner.setTenantId(0L);
        displayWinnerMapper.insert(winner);
    }

    private LotteryDrawResultVO buildResultVO(LotteryPrizeTier tier, LotteryRedemptionCode code) {
        LotteryDrawResultVO vo = new LotteryDrawResultVO();
        vo.setTierId(tier.getId());
        vo.setTierName(tier.getTierName());
        vo.setRewardType(tier.getRewardType());
        vo.setRewardValueJson(tier.getRewardValueJson());
        if (code != null) {
            vo.setCode(code.getCode());
            vo.setCodeExpiresAt(code.getExpiresAt());
        }
        if ("none".equals(tier.getRewardType())) {
            vo.setMessage("谢谢回顾");
        }
        return vo;
    }

    private String generateBizNo() {
        return "LD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
