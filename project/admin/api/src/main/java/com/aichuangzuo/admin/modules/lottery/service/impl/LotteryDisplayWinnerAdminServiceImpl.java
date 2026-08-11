package com.aichuangzuo.admin.modules.lottery.service.impl;

import com.aichuangzuo.admin.modules.lottery.dto.request.LotteryDisplayWinnerSaveRequest;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryDisplayWinner;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDisplayWinnerMapper;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.admin.modules.lottery.service.LotteryDisplayWinnerAdminService;
import com.aichuangzuo.admin.modules.lottery.vo.LotteryDisplayWinnerAdminVO;
import com.aichuangzuo.shared.enums.error.AdminLotteryErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryDisplayWinnerAdminServiceImpl implements LotteryDisplayWinnerAdminService {

    private final LotteryDisplayWinnerMapper displayWinnerMapper;
    private final LotteryCampaignMapper campaignMapper;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public List<LotteryDisplayWinnerAdminVO> listByCampaign(Long campaignId) {
        List<LotteryDisplayWinner> list = displayWinnerMapper.selectList(
                new LambdaQueryWrapper<LotteryDisplayWinner>()
                        .eq(LotteryDisplayWinner::getCampaignId, campaignId)
                        .orderByDesc(LotteryDisplayWinner::getWinTime)
                        .orderByAsc(LotteryDisplayWinner::getSortOrder));
        String campaignName = "";
        if (campaignId != null) {
            LotteryCampaign campaign = campaignMapper.selectById(campaignId);
            campaignName = campaign != null ? campaign.getName() : "";
        }
        final String name = campaignName;
        return list.stream().map(w -> buildVO(w, name)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWinner(LotteryDisplayWinnerSaveRequest request, Long adminUserId) {
        LotteryDisplayWinner entity;
        if (request.getId() != null) {
            entity = displayWinnerMapper.selectById(request.getId());
            if (entity == null) {
                throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_FOUND);
            }
        } else {
            entity = new LotteryDisplayWinner();
            entity.setIsReal(0);
            entity.setStatus(1);
            entity.setTenantId(0L);
        }
        entity.setCampaignId(request.getCampaignId());
        entity.setTierId(request.getTierId());
        entity.setPrizeName(request.getPrizeName());
        if (request.getUserId() != null) {
            PlatformUser user = platformUserMapper.selectById(request.getUserId());
            entity.setUserId(request.getUserId());
            entity.setNickname(user != null && user.getNickname() != null ? user.getNickname() : request.getNickname());
        } else {
            entity.setUserId(null);
            entity.setNickname(request.getNickname());
        }
        entity.setAvatarUrl(null);
        entity.setWinTime(request.getWinTime());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        if (request.getId() != null) {
            displayWinnerMapper.updateById(entity);
        } else {
            displayWinnerMapper.insert(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status, Long adminUserId) {
        LotteryDisplayWinner entity = displayWinnerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_FOUND);
        }
        entity.setStatus(status);
        displayWinnerMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWinner(Long id, Long adminUserId) {
        LotteryDisplayWinner entity = displayWinnerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(AdminLotteryErrorCode.TIER_NOT_FOUND);
        }
        displayWinnerMapper.deleteById(id);
    }

    private LotteryDisplayWinnerAdminVO buildVO(LotteryDisplayWinner winner, String campaignName) {
        LotteryDisplayWinnerAdminVO vo = new LotteryDisplayWinnerAdminVO();
        vo.setId(winner.getId());
        vo.setCampaignId(winner.getCampaignId());
        vo.setCampaignName(campaignName);
        vo.setTierId(winner.getTierId());
        vo.setUserId(winner.getUserId());
        vo.setNickname(winner.getNickname());
        vo.setAvatarUrl(winner.getAvatarUrl());
        vo.setPrizeName(winner.getPrizeName());
        vo.setWinTime(winner.getWinTime());
        vo.setIsReal(winner.getIsReal());
        vo.setSortOrder(winner.getSortOrder());
        vo.setStatus(winner.getStatus());
        vo.setCreatedAt(winner.getCreatedAt());
        vo.setUpdatedAt(winner.getUpdatedAt());
        return vo;
    }
}
