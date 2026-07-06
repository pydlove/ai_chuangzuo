package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.user.modules.auth.dto.request.RefreshTokenRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AuthServiceTokenTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private EmailCodeService emailCodeService;

    private AuthTokenVO registerUser() {
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("token_test@example.com");
        request.setEmailCode("000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        return authService.register(request, "127.0.0.1", "test-agent");
    }

    @Test
    void shouldRefreshToken() {
        AuthTokenVO token = registerUser();
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token.getRefreshToken());

        AuthTokenVO newToken = authService.refreshToken(request);
        assertNotNull(newToken.getAccessToken());
        assertNotNull(newToken.getRefreshToken());
        assertEquals(7200, newToken.getExpiresIn());
    }

    @Test
    void shouldLogoutSuccessfully() {
        AuthTokenVO token = registerUser();
        assertDoesNotThrow(() -> authService.logout(token.getAccessToken()));
    }
}