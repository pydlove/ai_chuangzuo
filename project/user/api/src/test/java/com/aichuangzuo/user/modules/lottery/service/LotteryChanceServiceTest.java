package com.aichuangzuo.user.modules.lottery.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.user.modules.lottery.entity.LotteryDrawChance;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryDrawChanceMapper;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Rollback
class LotteryChanceServiceTest {

    @Autowired
    private InviteRewardService inviteRewardService;

    @Autowired
    private LotteryCampaignMapper lotteryCampaignMapper;

    @Autowired
    private LotteryDrawChanceMapper lotteryDrawChanceMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void createInviteChance_shouldAllowMultipleChancesForSameInviter() {
        LotteryCampaign campaign = createActiveCampaign("邀请得次数活动");

        User inviter = createUser("chance-inviter@test.com");
        User invitee1 = createUser("chance-invitee1@test.com");
        User invitee2 = createUser("chance-invitee2@test.com");

        inviteRewardService.rewardAfterRegister(invitee1, inviter.getInviteCode());
        inviteRewardService.rewardAfterRegister(invitee2, inviter.getInviteCode());

        List<LotteryDrawChance> chances = lotteryDrawChanceMapper.selectList(
                new LambdaQueryWrapper<LotteryDrawChance>()
                        .eq(LotteryDrawChance::getUserId, inviter.getId())
                        .eq(LotteryDrawChance::getChanceType, "invite"));

        assertEquals(2, chances.size());
    }

    private LotteryCampaign createActiveCampaign(String name) {
        LotteryCampaign campaign = new LotteryCampaign();
        campaign.setName(name);
        campaign.setStartTime(LocalDateTime.now().minusHours(1));
        campaign.setEndTime(LocalDateTime.now().plusDays(1));
        campaign.setStatus(1);
        campaign.setFreeDrawsPerUser(1);
        campaign.setTenantId(0L);
        campaign.setIsDeleted(0);
        lotteryCampaignMapper.insert(campaign);
        return campaign;
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        user.setCoinBalance(BigDecimal.ZERO);
        userMapper.insert(user);
        return user;
    }
}
