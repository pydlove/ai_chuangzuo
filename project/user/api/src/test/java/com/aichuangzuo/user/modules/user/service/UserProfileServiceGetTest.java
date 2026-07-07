package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceGetTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMapper userMapper;

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