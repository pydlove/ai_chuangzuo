package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceGetTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInviteRelationMapper userInviteRelationMapper;

    @AfterEach
    void clear() {
        SecurityUserContext.clear();
    }

    @Test
    void shouldReturnProfileForCurrentUser() {
        User u = newUser("get-ok@test.com", "Nicky");
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        UserProfileVO vo = userProfileService.getMyProfile();

        assertNotNull(vo);
        assertEquals(u.getBizNo(), vo.getUserId());
        assertEquals("Nicky", vo.getNickname());
        assertEquals("get-ok@test.com", vo.getEmail());
        assertEquals(1, vo.getEmailVerified());
    }

    @Test
    void shouldReturnInviterNicknameWhenBound() {
        User inviter = newUser("inviter-bound@test.com", "Alice");
        User invitee = newUser("invitee-bound@test.com", "Bob");
        userMapper.insert(inviter);
        userMapper.insert(invitee);

        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviter.getInviteCode());
        relation.setSourceType(2);
        relation.setEffectiveStatus(0);
        userInviteRelationMapper.insert(relation);

        SecurityUserContext.setCurrentUserId(invitee.getId());

        UserProfileVO vo = userProfileService.getMyProfile();

        assertNotNull(vo);
        assertEquals(inviter.getId(), vo.getInviterUserId());
        assertEquals("Alice", vo.getInviterNickname());
    }

    @Test
    void shouldReturnInviterEmailWhenNicknameIsBlank() {
        User inviter = newUser("inviter-no-nickname@test.com", null);
        User invitee = newUser("invitee-no-nickname@test.com", "Bob");
        userMapper.insert(inviter);
        userMapper.insert(invitee);

        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviter.getInviteCode());
        relation.setSourceType(2);
        relation.setEffectiveStatus(0);
        userInviteRelationMapper.insert(relation);

        SecurityUserContext.setCurrentUserId(invitee.getId());

        UserProfileVO vo = userProfileService.getMyProfile();

        assertNotNull(vo);
        assertEquals(inviter.getId(), vo.getInviterUserId());
        assertEquals(inviter.getEmail(), vo.getInviterNickname());
    }

    @Test
    void shouldReturnNullInviterWhenNotBound() {
        User u = newUser("no-inviter@test.com", "Solo");
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        UserProfileVO vo = userProfileService.getMyProfile();

        assertNotNull(vo);
        assertNull(vo.getInviterUserId());
        assertNull(vo.getInviterNickname());
    }

    @Test
    void shouldThrowWhenUserMissing() {
        SecurityUserContext.setCurrentUserId(99999999L);
        assertThrows(BusinessException.class, () -> userProfileService.getMyProfile());
    }

    private User newUser(String email, String nickname) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname(nickname);
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(1);
        return u;
    }
}