package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.dto.request.BindInviteCodeRequest;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class UserInviteBindingServiceTest {

    @Autowired
    private UserInviteBindingService userInviteBindingService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @Autowired
    private UserCoinRecordMapper userCoinRecordMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @AfterEach
    void clear() {
        SecurityUserContext.clear();
    }

    @Test
    void shouldBindInviteCodeSuccessfullyAndGrantRewards() {
        User inviter = newUser("inviter-ok@test.com", "INVOK");
        User invitee = newUser("invitee-ok@test.com", "INVOE");
        userMapper.insert(inviter);
        userMapper.insert(invitee);
        SecurityUserContext.setCurrentUserId(invitee.getId());

        BindInviteCodeRequest request = new BindInviteCodeRequest();
        request.setInviteCode(inviter.getInviteCode());

        userInviteBindingService.bindInviteCode(request);

        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(invitee.getId());
        assertNotNull(relation);
        assertEquals(inviter.getId(), relation.getInviterId());
        assertEquals(invitee.getId(), relation.getInviteeId());
        assertEquals(inviter.getInviteCode(), relation.getInviteCode());
        assertEquals(2, relation.getSourceType());
        assertEquals(1, relation.getEffectiveStatus());

        User inviteeDb = userMapper.selectById(invitee.getId());
        assertEquals(0, inviteeDb.getCoinBalance().compareTo(new BigDecimal("5")));

        assertEquals(1L, userCoinRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord>()
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getUserId, invitee.getId())
                        .eq(com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord::getBizType, "invite_register_reward")));
    }

    @Test
    void shouldRejectWhenAlreadyBound() {
        User inviter = newUser("inviter-already@test.com", "ALRIN");
        User invitee = newUser("invitee-already@test.com", "ALRIN2");
        userMapper.insert(inviter);
        userMapper.insert(invitee);

        UserInviteRelation existing = new UserInviteRelation();
        existing.setInviterId(inviter.getId());
        existing.setInviteeId(invitee.getId());
        existing.setInviteCode(inviter.getInviteCode());
        existing.setSourceType(2);
        existing.setEffectiveStatus(0);
        userInviteRelationMapper.insert(existing);

        SecurityUserContext.setCurrentUserId(invitee.getId());
        BindInviteCodeRequest request = new BindInviteCodeRequest();
        request.setInviteCode("NEWCODE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userInviteBindingService.bindInviteCode(request));
        assertEquals(UserAuthErrorCode.INVITE_ALREADY_BOUND.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectWhenBindingWindowExpired() {
        User inviter = newUser("inviter-expired@test.com", "EXPIN");
        User invitee = newUser("invitee-expired@test.com", "EXPIN2");
        invitee.setCreatedAt(LocalDateTime.now().minusDays(8));
        userMapper.insert(inviter);
        userMapper.insert(invitee);
        SecurityUserContext.setCurrentUserId(invitee.getId());

        BindInviteCodeRequest request = new BindInviteCodeRequest();
        request.setInviteCode(inviter.getInviteCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userInviteBindingService.bindInviteCode(request));
        assertEquals(UserAuthErrorCode.INVITE_BINDING_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectWhenInviteCodeInvalid() {
        User invitee = newUser("invitee-invalid@test.com", "INVAL");
        userMapper.insert(invitee);
        SecurityUserContext.setCurrentUserId(invitee.getId());

        BindInviteCodeRequest request = new BindInviteCodeRequest();
        request.setInviteCode("NOPE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userInviteBindingService.bindInviteCode(request));
        assertEquals(UserAuthErrorCode.INVITE_CODE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectWhenBindingSelf() {
        User user = newUser("self-bind@test.com", "SELF1");
        userMapper.insert(user);
        SecurityUserContext.setCurrentUserId(user.getId());

        BindInviteCodeRequest request = new BindInviteCodeRequest();
        request.setInviteCode(user.getInviteCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userInviteBindingService.bindInviteCode(request));
        assertEquals(UserAuthErrorCode.INVITE_SELF_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectWhenCircularRelation() {
        User a = newUser("circular-a@test.com", "CIRCA");
        User b = newUser("circular-b@test.com", "CIRCB");
        userMapper.insert(a);
        userMapper.insert(b);

        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(a.getId());
        relation.setInviteeId(b.getId());
        relation.setInviteCode(a.getInviteCode());
        relation.setSourceType(1);
        relation.setEffectiveStatus(0);
        userInviteRelationMapper.insert(relation);

        SecurityUserContext.setCurrentUserId(a.getId());
        BindInviteCodeRequest request = new BindInviteCodeRequest();
        request.setInviteCode(b.getInviteCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userInviteBindingService.bindInviteCode(request));
        assertEquals(UserAuthErrorCode.INVITE_CIRCULAR_NOT_ALLOWED.getCode(), ex.getCode());
    }

    private User newUser(String email, String inviteCode) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode(inviteCode);
        u.setUserStatus(1);
        u.setEmailVerified(1);
        u.setCoinBalance(BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }
}
