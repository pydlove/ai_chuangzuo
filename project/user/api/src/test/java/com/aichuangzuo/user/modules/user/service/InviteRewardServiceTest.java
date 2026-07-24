package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.CoinDirection;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.message.entity.Message;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.mapper.MessageMapper;
import com.aichuangzuo.user.modules.user.vo.InviteStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class InviteRewardServiceTest {

    @Autowired
    private InviteRewardService inviteRewardService;

    @Autowired
    private CoinRecordService coinRecordService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    void rewardAfterRegister_shouldGrantFiveCoinsToInviteeAndCreateEffectiveRelation() {
        User inviter = createUser("reward-inviter@test.com");
        User invitee = createUser("reward-invitee@test.com");

        inviteRewardService.rewardAfterRegister(invitee, inviter.getInviteCode());

        User inviteeDb = userMapper.selectById(invitee.getId());
        assertEquals(0, inviteeDb.getCoinBalance().compareTo(new BigDecimal("5")));

        UserCoinRecord record = userCoinRecordMapper.selectOne(
                new LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getUserId, invitee.getId())
                        .eq(UserCoinRecord::getBizType, "invite_register_reward"));
        assertNotNull(record);
        assertEquals(CoinDirection.INCOME.getCode(), record.getDirection());
        assertEquals(0, record.getAmount().compareTo(new BigDecimal("5")));

        List<UserInviteRelation> relations = userInviteRelationMapper.selectList(
                new LambdaQueryWrapper<UserInviteRelation>()
                        .eq(UserInviteRelation::getInviterId, inviter.getId()));
        assertEquals(1, relations.size());
        assertEquals(1, relations.get(0).getEffectiveStatus());
    }

    @Test
    void rewardAfterRegister_shouldExtendMembershipAtThirdFifthAndBeyond() {
        User inviter = createUser("ladder-inviter@test.com");

        for (int i = 1; i <= 6; i++) {
            User invitee = createUser("ladder-invitee" + i + "@test.com");
            inviteRewardService.rewardAfterRegister(invitee, inviter.getInviteCode());
        }

        UserMembership membership = userMembershipMapper.selectByUserId(inviter.getId());
        assertNotNull(membership);
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(3 + 5 + 2), membership.getExpiresAt());

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getTargetUserId, inviter.getId())
                        .eq(Message::getMsgType, "membership")
                        .eq(Message::getSubType, MessageSubType.INVITE_REWARD.getCode())
                        .orderByAsc(Message::getCreatedAt));
        assertEquals(3, messages.size());
        assertTrue(messages.get(0).getSummary().contains("3 天专业版会员"));
        assertTrue(messages.get(1).getSummary().contains("5 天专业版会员"));
        assertTrue(messages.get(2).getSummary().contains("2 天专业版会员"));
    }

    @Test
    void getInviteStats_shouldReturnCorrectAggregatesAndFriends() {
        User inviter = createUser("stats-inviter@test.com");
        User invitee = createUser("stats-invitee@test.com");
        inviteRewardService.rewardAfterRegister(invitee, inviter.getInviteCode());

        coinRecordService.grant(inviter.getId(), "invite_reward", new BigDecimal("12.50"),
                null, "好友下单返利");

        InviteStatsVO stats = inviteRewardService.getInviteStats(inviter.getId());

        assertEquals(inviter.getInviteCode(), stats.getInviteCode());
        assertEquals(1, stats.getInvitedCount());
        assertEquals(0, stats.getMembershipDaysEarned());
        assertEquals(0, stats.getCoinEarned().compareTo(new BigDecimal("12.50")));
        assertNotNull(stats.getFriends());
        assertEquals(1, stats.getFriends().size());
        assertEquals(invitee.getEmail(), stats.getFriends().get(0).getEmail());
        assertEquals("registered", stats.getFriends().get(0).getStatus());
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
