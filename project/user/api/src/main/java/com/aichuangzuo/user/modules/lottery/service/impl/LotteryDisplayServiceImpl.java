package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.user.modules.lottery.entity.LotteryDisplayWinner;
import com.aichuangzuo.user.modules.lottery.entity.LotteryRedemptionCode;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryDisplayWinnerMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryDisplayService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionCodeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryDisplayServiceImpl implements LotteryDisplayService {

    private final LotteryDisplayWinnerMapper displayWinnerMapper;
    private final LotteryRedemptionCodeMapper redemptionCodeMapper;

    @Override
    public List<LotteryDisplayWinnerVO> listDisplayWinners(Long campaignId, int limit) {
        List<LotteryDisplayWinner> list = displayWinnerMapper.selectList(
                new LambdaQueryWrapper<LotteryDisplayWinner>()
                        .eq(LotteryDisplayWinner::getCampaignId, campaignId)
                        .eq(LotteryDisplayWinner::getStatus, 1)
                        .orderByAsc(LotteryDisplayWinner::getSortOrder)
                        .orderByDesc(LotteryDisplayWinner::getCreatedAt)
                        .last("LIMIT " + limit));
        return list.stream().map(this::buildDisplayWinnerVO).collect(Collectors.toList());
    }

    @Override
    public List<LotteryRedemptionCodeVO> listMyRedemptionCodes(Long userId) {
        List<LotteryRedemptionCode> list = redemptionCodeMapper.selectList(
                new LambdaQueryWrapper<LotteryRedemptionCode>()
                        .eq(LotteryRedemptionCode::getDrawerUserId, userId)
                        .orderByDesc(LotteryRedemptionCode::getCreatedAt));
        return list.stream().map(this::buildRedemptionCodeVO).collect(Collectors.toList());
    }

    private LotteryDisplayWinnerVO buildDisplayWinnerVO(LotteryDisplayWinner winner) {
        LotteryDisplayWinnerVO vo = new LotteryDisplayWinnerVO();
        vo.setId(winner.getId());
        vo.setNickname(winner.getNickname());
        vo.setAvatarUrl(winner.getAvatarUrl());
        vo.setPrizeName(winner.getPrizeName());
        vo.setWinTime(winner.getWinTime());
        vo.setIsReal(winner.getIsReal());
        return vo;
    }

    private LotteryRedemptionCodeVO buildRedemptionCodeVO(LotteryRedemptionCode code) {
        LotteryRedemptionCodeVO vo = new LotteryRedemptionCodeVO();
        vo.setId(code.getId());
        vo.setCode(code.getCode());
        vo.setTierName("");
        vo.setRewardType(code.getRewardType());
        vo.setStatus(code.getStatus());
        vo.setExpiresAt(code.getExpiresAt());
        vo.setUsedAt(code.getUsedAt());
        return vo;
    }
}
