package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceNicknameTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;

    @AfterEach
    void clear() { SecurityUserContext.clear(); }

    @Test
    void shouldUpdateNicknameSuccessfully() {
        User u = newUser("nick-ok@test.com", "old");
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("  new name  ");
        UserProfileVO vo = userProfileService.updateNickname(req);

        assertEquals("new name", vo.getNickname()); // trim 后存储
        User refreshed = userMapper.selectById(u.getId());
        assertEquals("new name", refreshed.getNickname());
    }

    @Test
    void shouldThrowWhenUserMissing() {
        SecurityUserContext.setCurrentUserId(99999999L);
        UpdateNicknameRequest req = new UpdateNicknameRequest();
        req.setNickname("x");
        assertThrows(BusinessException.class, () -> userProfileService.updateNickname(req));
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