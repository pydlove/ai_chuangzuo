package com.aichuangzuo.user.modules.user.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@Rollback
class UserProfileServiceEmailTest {

    @Autowired private UserProfileService userProfileService;
    @Autowired private UserMapper userMapper;
    @MockBean private EmailCodeService emailCodeService;

    @AfterEach
    void clear() { SecurityUserContext.clear(); }

    @Test
    void shouldUpdateEmailSuccessfully() {
        User u = newUser("email-old@test.com", 1); // emailVerified=1
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());
        when(emailCodeService.validateEmailCode("email-new@test.com", "000000")).thenReturn(true);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("email-new@test.com");
        req.setEmailCode("000000");
        UserProfileVO vo = userProfileService.updateEmail(req);

        assertEquals("email-new@test.com", vo.getEmail());
        assertEquals(1, vo.getEmailVerified());
        User refreshed = userMapper.selectById(u.getId());
        assertEquals("email-new@test.com", refreshed.getEmail());
    }

    @Test
    void shouldThrowWhenCodeInvalid() {
        User u = newUser("email-a@test.com", 1);
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(false);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("email-b@test.com");
        req.setEmailCode("111111");
        assertThrows(BusinessException.class, () -> userProfileService.updateEmail(req));
    }

    @Test
    void shouldThrowWhenNewEmailAlreadyTaken() {
        User me = newUser("me@test.com", 1);
        User other = newUser("taken@test.com", 1);
        userMapper.insert(me);
        userMapper.insert(other);
        SecurityUserContext.setCurrentUserId(me.getId());
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("taken@test.com");
        req.setEmailCode("000000");
        assertThrows(BusinessException.class, () -> userProfileService.updateEmail(req));
    }

    @Test
    void shouldThrowWhenNewEmailSameAsOld() {
        User u = newUser("same@test.com", 1);
        userMapper.insert(u);
        SecurityUserContext.setCurrentUserId(u.getId());
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        UpdateEmailRequest req = new UpdateEmailRequest();
        req.setNewEmail("same@test.com");
        req.setEmailCode("000000");
        assertThrows(BusinessException.class, () -> userProfileService.updateEmail(req));
    }

    private User newUser(String email, int verified) {
        User u = new User();
        u.setBizNo("B" + System.nanoTime());
        u.setNickname("u");
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime() % 1000000);
        u.setUserStatus(1);
        u.setEmailVerified(verified);
        return u;
    }
}