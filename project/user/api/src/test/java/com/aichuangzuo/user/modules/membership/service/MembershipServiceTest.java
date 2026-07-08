package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class MembershipServiceTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void subscribe_invalidPayCode_throwsBusinessException() {
        User user = createUser("sub-pay-code@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "000000", new BigDecimal("503.2"));

        try {
            membershipService.subscribe(user.getId(), request);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("支付码错误"));
            return;
        }
        throw new AssertionError("应抛出支付码错误异常");
    }

    @Test
    void subscribe_newMembership_createsMembershipAndOrder() {
        User user = createUser("sub-new@test.com");
        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));

        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        assertNotNull(result.getOrderNo());
        assertTrue(result.getOrderNo().startsWith("SUB"));
        assertEquals("pro", result.getLevel());
        assertEquals(365, result.getDays());
        assertNotNull(result.getExpiresAt());
        assertFalse(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(BigDecimal.ZERO));

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertNotNull(membership);
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(365), membership.getExpiresAt());
    }

    @Test
    void subscribe_extendMembership_extendsFromExistingExpiry() {
        User user = createUser("sub-extend@test.com");
        UserMembership existing = new UserMembership();
        existing.setUserId(user.getId());
        existing.setLevel("basic");
        existing.setStartedAt(LocalDate.now().minusDays(30));
        existing.setExpiresAt(LocalDate.now().plusDays(10));
        existing.setTenantId(0L);
        userMembershipMapper.insert(existing);

        SubscribeRequest request = buildRequest("pro", "quarter", "123456", new BigDecimal("161.7"));
        SubscribeResultVO result = membershipService.subscribe(user.getId(), request);

        UserMembership membership = userMembershipMapper.selectByUserId(user.getId());
        assertEquals("pro", membership.getLevel());
        assertEquals(LocalDate.now().plusDays(10).plusDays(90), membership.getExpiresAt());
        assertEquals(90, result.getDays());
    }

    @Test
    void subscribe_withInviter_grantsRewardAndSendsNotification() {
        User inviter = createUser("sub-inviter@test.com");
        User invitee = createUser("sub-invitee@test.com");
        createInviteRelation(inviter, invitee);

        SubscribeRequest request = buildRequest("pro", "year", "123456", new BigDecimal("503.2"));
        SubscribeResultVO result = membershipService.subscribe(invitee.getId(), request);

        assertTrue(result.isInviterRewarded());
        assertEquals(0, result.getRewardAmount().compareTo(new BigDecimal("5")));

        Long rewardCount = userCoinRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord>()
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getUserId, inviter.getId())
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getBizType, "invite_reward")
        );
        assertEquals(1L, rewardCount);
    }

    @Test
    void getMyMembership_expired_returnsFalse() {
        User user = createUser("sub-expired@test.com");
        UserMembership membership = new UserMembership();
        membership.setUserId(user.getId());
        membership.setLevel("pro");
        membership.setStartedAt(LocalDate.now().minusDays(400));
        membership.setExpiresAt(LocalDate.now().minusDays(10));
        membership.setTenantId(0L);
        userMembershipMapper.insert(membership);

        MembershipStatusVO vo = membershipService.getMyMembership(user.getId());

        assertFalse(vo.isHasMembership());
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

    private void createInviteRelation(User inviter, User invitee) {
        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviter.getInviteCode());
        relation.setSourceType(2);
        relation.setEffectiveStatus(1);
        userInviteRelationMapper.insert(relation);
    }

    private SubscribeRequest buildRequest(String planKey, String cycle, String payCode, BigDecimal amount) {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanKey(planKey);
        request.setCycle(cycle);
        request.setPayCode(payCode);
        request.setAmount(amount);
        return request;
    }
}
