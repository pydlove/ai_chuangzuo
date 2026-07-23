package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AuthServiceResetPasswordTest {

    private static final String RESET_AT_PREFIX = "user:auth:password-reset-at:";

    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CacheUtil cacheUtil;
    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private EmailCodeService emailCodeService;

    private AuthTokenVO registerUser(String email, String password) {
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword(password);
        request.setConfirmPassword(password);
        return authService.register(request, "127.0.0.1", "test-agent");
    }

    private ResetPasswordRequest buildRequest(String email, String pwd) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(email);
        req.setEmailCode("000000");
        req.setPassword(pwd);
        req.setConfirmPassword(pwd);
        return req;
    }

    private Long jwtUserId(AuthTokenVO token) {
        JwtUtil jwtUtil = applicationContext.getBean(JwtUtil.class);
        return jwtUtil.parseAccessToken(token.getAccessToken());
    }

    @Test
    void shouldRejectWhenPasswordMismatch() {
        registerUser("reset_mismatch@example.com", "OldPass123");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("reset_mismatch@example.com");
        req.setEmailCode("000000");
        req.setPassword("NewPass456");
        req.setConfirmPassword("Different789");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(req, "127.0.0.1"));
        assertEquals(UserAuthErrorCode.PASSWORD_NOT_MATCH.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectWhenUserNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(buildRequest("no_such@example.com", "NewPass456"),
                        "127.0.0.1"));
        assertEquals(UserAuthErrorCode.RESET_PASSWORD_FAILED.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectWhenEmailCodeWrong() {
        registerUser("reset_code@example.com", "OldPass123");
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(buildRequest("reset_code@example.com", "NewPass456"),
                        "127.0.0.1"));
        assertEquals(UserAuthErrorCode.EMAIL_CODE_ERROR.getCode(), ex.getCode());
    }

    @Test
    void shouldUpdatePasswordAndWriteResetTimestamp() {
        AuthTokenVO token = registerUser("reset_ok@example.com", "OldPass123");
        Long userId = jwtUserId(token);

        authService.resetPassword(buildRequest("reset_ok@example.com", "BrandNew789"), "127.0.0.1");

        var user = userMapper.selectByEmail("reset_ok@example.com");
        assertNotNull(user);
        assertTrue(passwordEncoder.matches("BrandNew789", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPass123", user.getPasswordHash()));

        Date resetAt = cacheUtil.get(RESET_AT_PREFIX + userId);
        assertNotNull(resetAt);
        assertTrue(resetAt.getTime() <= System.currentTimeMillis());
    }
}
