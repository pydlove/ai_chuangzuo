package com.aichuangzuo.user.modules.lottery.service.impl;

import com.aichuangzuo.user.modules.lottery.entity.LotteryDrawChance;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryDrawChanceMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryChanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryChanceServiceImpl implements LotteryChanceService {

    private final LotteryDrawChanceMapper drawChanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryDrawChance acquireFreeChance(Long campaignId, Long userId) {
        LotteryDrawChance chance = drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getChanceType, "free"));
        if (chance != null) {
            return chance;
        }
        chance = new LotteryDrawChance();
        chance.setCampaignId(campaignId);
        chance.setUserId(userId);
        chance.setChanceType("free");
        chance.setStatus("available");
        chance.setTenantId(0L);
        try {
            drawChanceMapper.insert(chance);
        } catch (DuplicateKeyException e) {
            log.warn("免费抽奖次数并发创建，回查 userId={}, campaignId={}", userId, campaignId);
            return drawChanceMapper.selectOne(
                    new LambdaQueryWrapper<LotteryDrawChance>()
                            .eq(LotteryDrawChance::getCampaignId, campaignId)
                            .eq(LotteryDrawChance::getUserId, userId)
                            .eq(LotteryDrawChance::getChanceType, "free"));
        }
        return chance;
    }

    @Override
    public LotteryDrawChance acquireOneAvailableChance(Long campaignId, Long userId) {
        return drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getStatus, "available")
                        .orderByAsc(LotteryDrawChance::getCreatedAt)
                        .last("LIMIT 1"));
    }

    @Override
    public int consumeChance(Long chanceId) {
        return drawChanceMapper.update(null,
                new LambdaUpdateWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getId, chanceId)
                        .eq(LotteryDrawChance::getStatus, "available")
                        .set(LotteryDrawChance::getStatus, "used")
                        .set(LotteryDrawChance::getUsedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInviteChance(Long campaignId, Long userId, Long inviteRelationId) {
        LotteryDrawChance existing = drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getChanceType, "invite")
                        .eq(LotteryDrawChance::getSourceInviteRelationId, inviteRelationId));
        if (existing != null) {
            log.warn("邀请抽奖次数已存在，跳过创建 campaignId={}, userId={}, inviteRelationId={}",
                    campaignId, userId, inviteRelationId);
            return;
        }
        LotteryDrawChance chance = new LotteryDrawChance();
        chance.setCampaignId(campaignId);
        chance.setUserId(userId);
        chance.setChanceType("invite");
        chance.setSourceInviteRelationId(inviteRelationId);
        chance.setStatus("available");
        chance.setTenantId(0L);
        drawChanceMapper.insert(chance);
        log.info("创建邀请抽奖次数 campaignId={}, userId={}, inviteRelationId={}", campaignId, userId, inviteRelationId);
    }

    @Override
    public int countAvailableChances(Long campaignId, Long userId) {
        Long count = drawChanceMapper.selectCount(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getStatus, "available"));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public boolean isFreeChanceUsed(Long campaignId, Long userId) {
        LotteryDrawChance free = drawChanceMapper.selectOne(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getCampaignId, campaignId)
                        .eq(LotteryDrawChance::getUserId, userId)
                        .eq(LotteryDrawChance::getChanceType, "free"));
        return free == null || "used".equals(free.getStatus());
    }
}
