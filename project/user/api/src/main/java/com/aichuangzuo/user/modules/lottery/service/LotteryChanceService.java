package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.lottery.entity.LotteryDrawChance;

public interface LotteryChanceService {

    LotteryDrawChance acquireFreeChance(Long campaignId, Long userId);

    LotteryDrawChance acquireOneAvailableChance(Long campaignId, Long userId);

    int consumeChance(Long chanceId);

    void createInviteChance(Long campaignId, Long userId, Long inviteRelationId);

    int countAvailableChances(Long campaignId, Long userId);

    boolean isFreeChanceUsed(Long campaignId, Long userId);
}
