package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.auth.dto.request.LoginRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RefreshTokenRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@Transactional
@DirtiesContext
class AuthServiceRefreshAfterResetTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CacheUtil cacheUtil;

    @MockBean
    private EmailCodeService emailCodeService;

    @org.junit.jupiter.api.BeforeEach
    void cleanResetCache() {
        // 清理前序测试残留的 reset-at:{userId} cache 记录（Caffeine 不受 @Transactional 回滚影响）
        var user = userMapper.selectByEmail("refresh_reset@example.com");
        if (user != null) {
            cacheUtil.delete("user:auth:password-reset-at:" + user.getId());
        }
    }

    private AuthTokenVO registerUser(String email, String password) {
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword(password);
        request.setConfirmPassword(password);
        return authService.register(request, "127.0.0.1", "test-agent");
    }

    private void resetPassword(String email, String oldPwd, String newPwd) {
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        ResetPasswordRequest rp = new ResetPasswordRequest();
        rp.setEmail(email);
        rp.setEmailCode("000000");
        rp.setPassword(newPwd);
        rp.setConfirmPassword(newPwd);
        authService.resetPassword(rp, "127.0.0.1");
    }

    private AuthTokenVO login(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return authService.login(req, "127.0.0.1", "test-agent");
    }

    @Test
    void shouldInvalidateOldRefreshTokenAfterPasswordReset() throws InterruptedException {
        AuthTokenVO token = registerUser("refresh_reset@example.com", "OldPass123");
        String oldRefresh = token.getRefreshToken();

        // 确保 reset 与 register 跨秒（避免 JWT iat 秒级精度下无法区分新旧 token）
        Thread.sleep(1100);

        resetPassword("refresh_reset@example.com", "OldPass123", "BrandNew789");

        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(oldRefresh);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken(refreshReq));
        assertEquals(UserAuthErrorCode.REFRESH_TOKEN_INVALID.getCode(), ex.getCode());

        // 新密码登录拿到的 refresh 应能正常使用
        AuthTokenVO newToken = login("refresh_reset@example.com", "BrandNew789");
        RefreshTokenRequest newRefreshReq = new RefreshTokenRequest();
        newRefreshReq.setRefreshToken(newToken.getRefreshToken());
        AuthTokenVO refreshed = authService.refreshToken(newRefreshReq);
        assertNotNull(refreshed.getAccessToken());
    }
}
