package com.aichuangzuo.user.modules.lottery.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.lottery.dto.request.LotteryDrawRequest;
import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.user.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryChanceService;
import com.aichuangzuo.user.modules.lottery.service.LotteryDrawService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryCampaignVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryChancesVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDrawResultVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryPrizeTierVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "用户端-抽奖活动")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryDrawService lotteryDrawService;
    private final LotteryChanceService lotteryChanceService;
    private final LotteryCampaignMapper campaignMapper;
    private final LotteryPrizeTierMapper prizeTierMapper;

    @Operation(summary = "当前进行中的活动")
    @GetMapping("/campaigns/current")
    public Result<LotteryCampaignVO> currentCampaign() {
        LotteryCampaign campaign = campaignMapper.selectOne(
                new LambdaQueryWrapper<LotteryCampaign>()
                        .eq(LotteryCampaign::getStatus, 1)
                        .le(LotteryCampaign::getStartTime, LocalDateTime.now())
                        .ge(LotteryCampaign::getEndTime, LocalDateTime.now())
                        .orderByDesc(LotteryCampaign::getStartTime)
                        .last("LIMIT 1"));
        if (campaign == null) {
            return Result.success(null);
        }
        return Result.success(buildCampaignVO(campaign));
    }

    @Operation(summary = "我的剩余抽奖次数")
    @GetMapping("/chances")
    public Result<LotteryChancesVO> chances(@RequestParam Long campaignId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        LotteryCampaign campaign = campaignMapper.selectById(campaignId);
        if (campaign != null && campaign.getFreeDrawsPerUser() != null && campaign.getFreeDrawsPerUser() > 0) {
            lotteryChanceService.acquireFreeChance(campaignId, userId);
        }
        int available = lotteryChanceService.countAvailableChances(campaignId, userId);
        boolean freeUsed = lotteryChanceService.isFreeChanceUsed(campaignId, userId);
        return Result.success(new LotteryChancesVO(available, !freeUsed));
    }

    @Operation(summary = "抽奖")
    @PostMapping("/draw")
    public Result<LotteryDrawResultVO> draw(@RequestBody @Valid LotteryDrawRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("用户抽奖 userId={}, campaignId={}", userId, request.getCampaignId());
        return Result.success(lotteryDrawService.draw(userId, request.getCampaignId()));
    }

    private LotteryCampaignVO buildCampaignVO(LotteryCampaign campaign) {
        LotteryCampaignVO vo = new LotteryCampaignVO();
        vo.setId(campaign.getId());
        vo.setName(campaign.getName());
        vo.setDescription(campaign.getDescription());
        vo.setImageUrl(campaign.getImageUrl());
        vo.setRules(campaign.getRules());
        vo.setStartTime(campaign.getStartTime());
        vo.setEndTime(campaign.getEndTime());

        List<LotteryPrizeTier> tiers = prizeTierMapper.selectList(
                new LambdaQueryWrapper<LotteryPrizeTier>()
                        .eq(LotteryPrizeTier::getCampaignId, campaign.getId())
                        .eq(LotteryPrizeTier::getStatus, 1)
                        .orderByAsc(LotteryPrizeTier::getSortOrder));
        vo.setTiers(tiers.stream().map(this::buildTierVO).collect(Collectors.toList()));
        return vo;
    }

    private LotteryPrizeTierVO buildTierVO(LotteryPrizeTier tier) {
        LotteryPrizeTierVO vo = new LotteryPrizeTierVO();
        vo.setId(tier.getId());
        vo.setTierKey(tier.getTierKey());
        vo.setTierName(tier.getTierName());
        vo.setRewardType(tier.getRewardType());
        vo.setRewardValueJson(tier.getRewardValueJson());
        vo.setSortOrder(tier.getSortOrder());
        return vo;
    }
}
