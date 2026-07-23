package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServicePasswordTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @AfterEach
    void clear() { SecurityUserContext.clear(); }

    @Test
    void shouldChangePasswordSuccessfully() {
        User u = newUserWithHash("pwd-ok@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("new123456");
        req.setConfirmPassword("new123456");
        userProfileService.changePassword(req);

        User refreshed = userMapper.selectById(u.getId());
        assertTrue(passwordEncoder.matches("new123456", refreshed.getPasswordHash()));
    }

    @Test
    void shouldThrowWhenAccountDisabled() {
        User u = newUserWithHash("pwd-disabled@test.com", passwordEncoder.encode("old123"));
        u.setUserStatus(0);
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("new123456");
        req.setConfirmPassword("new123456");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    @Test
    void shouldThrowWhenOldPasswordWrong() {
        User u = newUserWithHash("pwd-wrong@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("WRONG");
        req.setNewPassword("new123456");
        req.setConfirmPassword("new123456");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    @Test
    void shouldThrowWhenNewPasswordTooWeak() {
        User u = newUserWithHash("pwd-weak@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("123");
        req.setConfirmPassword("123");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    @Test
    void shouldThrowWhenConfirmMismatch() {
        User u = newUserWithHash("pwd-mm@test.com", passwordEncoder.encode("old123"));
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old123");
        req.setNewPassword("new123456");
        req.setConfirmPassword("different");
        assertThrows(BusinessException.class, () -> userProfileService.changePassword(req));
    }

    private User newUserWithHash(String email, String hash) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname("u");
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(1);
        return u;
    }
}
