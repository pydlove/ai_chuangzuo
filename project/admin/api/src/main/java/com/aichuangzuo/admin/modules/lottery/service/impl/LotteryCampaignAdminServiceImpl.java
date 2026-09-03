package com.aichuangzuo.admin.modules.lottery.service.impl;

import com.aichuangzuo.admin.modules.lottery.dto.request.CloneCampaignRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryCampaignQueryRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryCampaignSaveRequest;
import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryPrizeTierSaveRequest;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.admin.modules.lottery.service.LotteryCampaignAdminService;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryCampaignAdminVO;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryPrizeTierAdminVO;
import com.aichuangzuo.shared.enums.error.AdminLotteryErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryCampaignAdminServiceImpl implements LotteryCampaignAdminService {

    private final LotteryCampaignMapper campaignMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;

    @Override
    public PageResult listCampaigns(LotteryCampaignQueryRequest request) {
        Page<LotteryCampaign> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<LotteryCampaign> wrapper = new LambdaQueryWrapper<LotteryCampaign>()
                .eq(LotteryCampaign::getIsDeleted, 0)
                .orderByDesc(LotteryCampaign::getCreatedAt);
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.like(LotteryCampaign::getName, request.getKeyword());
        }
        Page<LotteryCampaign> result = campaignMapper.selectPage(page, wrapper);
        List<LotteryCampaignAdminVO> items = result.getRecords().stream()
                .map(this::buildCampaignVO)
                .collect(Collectors.toList());
        return new PageResult(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public LotteryCampaignAdminVO getCampaign(Long id) {
        LotteryCampaign campaign = campaignMapper.selectById(id);
        if (campaign == null || campaign.getIsDeleted() == 1) {
            throw new BusinessException(AdminLotteryErrorCode.CAMPAIGN_NOT_FOUND);
        }
        return buildCampaignVO(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCampaign(LotteryCampaignSaveRequest request, Long adminUserId) {
        LotteryCampaign entity;
        if (request.getId() != null) {
            entity = campaignMapper.selectById(request.getId());
            if (entity == null || entity.getIsDeleted() == 1) {
                throw new BusinessException(AdminLotteryErrorCode.CAMPAIGN_NOT_FOUND);
            }
            if (entity.getStatus() != null && entity.getStatus() == 3) {
                throw new BusinessException(AdminLotteryErrorCode.CAMPAIGN_CANNOT_UPDATE);
            }
        } else {
            entity = new LotteryCampaign();
            entity.setStatus(0);
            entity.setTenantId(0L);
            entity.setIsDeleted(0);
            entity.setCreatedBy(adminUserId);
        }
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setRules(request.getRules());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setFreeDrawsPerUser(request.getFreeDrawsPerUser());
        entity.setUpdatedBy(adminUserId);
        if (request.getId() != null) {
            campaignMapper.updateById(entity);
        } else {
            campaignMapper.insert(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openCampaign(Long id, Long adminUserId) {
        LotteryCampaign campaign = getAndCheck(id);
        if (campaign.getStatus() != null && campaign.getStatus() != 0 && campaign.getStatus() != 3) {
            throw new BusinessException(AdminLotteryErrorCode.INVALID_CAMPAIGN_STATUS);
        }
        long openCount = campaignMapper.selectCount(
                new LambdaQueryWrapper<LotteryCampaign>()
                        .eq(LotteryCampaign::getIsDeleted, 0)
                        .eq(LotteryCampaign::getStatus, 1)
                        .ne(LotteryCampaign::getId, id));
        if (openCount > 0) {
            throw new BusinessException(AdminLotteryErrorCode.CAMPAIGN_ALREADY_OPEN);
        }
        validateProbabilitySum(id);
        campaign.setStatus(1);
        campaign.setUpdatedBy(adminUserId);
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cloneCampaign(Long sourceId, CloneCampaignRequest request, Long adminUserId) {
        LotteryCampaign source = getAndCheck(sourceId);

        LotteryCampaign copy = new LotteryCampaign();
        BeanUtils.copyProperties(source, copy, "id", "createdAt", "updatedAt", "createdBy", "updatedBy");
        copy.setName(request.getName());
        copy.setStatus(0);
        copy.setTenantId(0L);
        copy.setIsDeleted(0);
        copy.setCreatedBy(adminUserId);
        copy.setUpdatedBy(adminUserId);
        campaignMapper.insert(copy);

        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, sourceId)
                        .eq(LotteryPrizeTier::getIsDeleted, 0));
        for (LotteryPrizeTier tier : tiers) {
            LotteryPrizeTier newTier = new LotteryPrizeTier();
            BeanUtils.copyProperties(tier, newTier, "id", "campaignId", "createdAt", "updatedAt", "createdBy", "updatedBy");
            newTier.setCampaignId(copy.getId());
            newTier.setRemainingWinCount(tier.getMaxWinCount());
            newTier.setStatus(1);
            newTier.setTenantId(0L);
            newTier.setIsDeleted(0);
            newTier.setCreatedBy(adminUserId);
            newTier.setUpdatedBy(adminUserId);
            prizeTierMapper.insert(newTier);
        }

        return copy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeCampaign(Long id, Long adminUserId) {
        LotteryCampaign campaign = getAndCheck(id);
        campaign.setStatus(3);
        campaign.setUpdatedBy(adminUserId);
        campaignMapper.updateById(campaign);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampaign(Long id, Long adminUserId) {
        LotteryCampaign campaign = getAndCheck(id);
        campaign.setIsDeleted(1);
        campaign.setUpdatedBy(adminUserId);
        campaignMapper.updateById(campaign);
    }

    @Override
    public List<LotteryPrizeTierAdminVO> listTiers(Long campaignId) {
        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaignId)
                        .eq(LotteryPrizeTier::getIsDeleted, 0)
                        .orderByAsc(LotteryPrizeTier::getSortOrder)
                        .orderByAsc(LotteryPrizeTier::getPrizeLevel));
        return tiers.stream().map(this::buildTierVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTier(Long campaignId, LotteryPrizeTierSaveRequest request, Long adminUserId) {
        LotteryCampaign campaign = getAndCheck(campaignId);

        LotteryPrizeTier existingSameKey = prizeTierMapper.selectOne(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaignId)
                        .eq(LotteryPrizeTier::getTierKey, request.getTierKey())
                        .eq(LotteryPrizeTier::getIsDeleted, 0));
        if (existingSameKey != null && (request.getId() == null || !existingSameKey.getId().equals(request.getId()))) {
            throw new BusinessException(AdminLotteryErrorCode.TIER_KEY_EXISTS);
        }

        LotteryPrizeTier entity;
        if (request.getId() != null) {
            entity = prizeTierMapper.selectById(request.getId());
            if (entity == null || entity.getIsDeleted() == 1 || !entity.getCampaignId().equals(campaignId)) {
                throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_FOUND);
            }
        } else {
            entity = new LotteryPrizeTier();
            entity.setCampaignId(campaignId);
            entity.setStatus(1);
            entity.setTenantId(0L);
            entity.setIsDeleted(0);
            entity.setCreatedBy(adminUserId);
        }
        entity.setTierKey(request.getTierKey());
        entity.setTierName(request.getTierName());
        entity.setPrizeLevel(request.getPrizeLevel());
        entity.setProbability(request.getProbability());
        syncRemainingWinCount(entity, request.getMaxWinCount());
        entity.setDisplayRemaining(request.getDisplayRemaining() == null ? 0 : request.getDisplayRemaining());
        entity.setDisplayRemainingCount(request.getDisplayRemainingCount());
        entity.setRewardType(request.getRewardType());
        entity.setRewardValueJson(request.getRewardValueJson());
        entity.setCodePrefix(request.getCodePrefix());
        entity.setCodeLength(request.getCodeLength());
        entity.setCodeValidityDays(request.getCodeValidityDays());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setUpdatedBy(adminUserId);

        if (request.getId() != null) {
            prizeTierMapper.updateById(entity);
        } else {
            prizeTierMapper.insert(entity);
        }

        validateProbabilitySum(campaignId);
    }

    private void syncRemainingWinCount(LotteryPrizeTier entity, Integer newMaxWinCount) {
        Integer oldMaxWinCount = entity.getMaxWinCount();
        Integer oldRemaining = entity.getRemainingWinCount();
        entity.setMaxWinCount(newMaxWinCount);
        if (entity.getId() == null) {
            entity.setRemainingWinCount(newMaxWinCount);
            return;
        }
        if (newMaxWinCount == null) {
            entity.setRemainingWinCount(null);
        } else if (oldMaxWinCount == null || oldRemaining == null) {
            entity.setRemainingWinCount(newMaxWinCount);
        } else {
            int diff = newMaxWinCount - oldMaxWinCount;
            entity.setRemainingWinCount(Math.max(0, oldRemaining + diff));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTier(Long campaignId, Long tierId, Long adminUserId) {
        LotteryPrizeTier tier = prizeTierMapper.selectById(tierId);
        if (tier == null || tier.getIsDeleted() == 1 || !tier.getCampaignId().equals(campaignId)) {
            throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_FOUND);
        }
        tier.setIsDeleted(1);
        tier.setUpdatedBy(adminUserId);
        prizeTierMapper.updateById(tier);
    }

    private LotteryCampaign getAndCheck(Long id) {
        LotteryCampaign campaign = campaignMapper.selectById(id);
        if (campaign == null || campaign.getIsDeleted() == 1) {
            throw new BusinessException(AdminLotteryErrorCode.CAMPAIGN_NOT_FOUND);
        }
        return campaign;
    }

    private void validateProbabilitySum(Long campaignId) {
        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaignId)
                        .eq(LotteryPrizeTier::getIsDeleted, 0)
                        .eq(LotteryPrizeTier::getStatus, 1));
        BigDecimal sum = tiers.stream()
                .map(LotteryPrizeTier::getProbability)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(AdminLotteryErrorCode.PROBABILITY_SUM_EXCEEDS);
        }
    }

    private LotteryCampaignAdminVO buildCampaignVO(LotteryCampaign campaign) {
        LotteryCampaignAdminVO vo = new LotteryCampaignAdminVO();
        vo.setId(campaign.getId());
        vo.setName(campaign.getName());
        vo.setDescription(campaign.getDescription());
        vo.setImageUrl(campaign.getImageUrl());
        vo.setRules(campaign.getRules());
        vo.setStartTime(campaign.getStartTime());
        vo.setEndTime(campaign.getEndTime());
        vo.setStatus(campaign.getStatus());
        vo.setFreeDrawsPerUser(campaign.getFreeDrawsPerUser());
        vo.setCreatedAt(campaign.getCreatedAt());
        vo.setUpdatedAt(campaign.getUpdatedAt());
        return vo;
    }

    private LotteryPrizeTierAdminVO buildTierVO(LotteryPrizeTier tier) {
        LotteryPrizeTierAdminVO vo = new LotteryPrizeTierAdminVO();
        vo.setId(tier.getId());
        vo.setCampaignId(tier.getCampaignId());
        vo.setTierKey(tier.getTierKey());
        vo.setTierName(tier.getTierName());
        vo.setPrizeLevel(tier.getPrizeLevel());
        vo.setProbability(tier.getProbability());
        vo.setMaxWinCount(tier.getMaxWinCount());
        vo.setRemainingWinCount(tier.getRemainingWinCount());
        vo.setDisplayRemaining(tier.getDisplayRemaining());
        vo.setDisplayRemainingCount(tier.getDisplayRemainingCount());
        vo.setRewardType(tier.getRewardType());
        vo.setRewardValueJson(tier.getRewardValueJson());
        vo.setCodePrefix(tier.getCodePrefix());
        vo.setCodeLength(tier.getCodeLength());
        vo.setCodeValidityDays(tier.getCodeValidityDays());
        vo.setSortOrder(tier.getSortOrder());
        vo.setStatus(tier.getStatus());
        vo.setCreatedAt(tier.getCreatedAt());
        vo.setUpdatedAt(tier.getUpdatedAt());
        return vo;
    }
}
