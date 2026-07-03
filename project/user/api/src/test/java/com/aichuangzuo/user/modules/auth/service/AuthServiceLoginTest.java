package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.dto.request.LoginRequest;
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
class AuthServiceLoginTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private EmailCodeService emailCodeService;

    @MockBean
    private CaptchaService captchaService;

    @Test
    void shouldLoginWithValidCredentialsAndCaptcha() {
        String email = "login_test@example.com";
        String password = "123456";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setEmailCode("000000");
        registerRequest.setPassword(password);
        registerRequest.setConfirmPassword(password);
        authService.register(registerRequest, "127.0.0.1", "test-agent");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        loginRequest.setCaptchaKey("mock-key");
        loginRequest.setCaptchaCode("mock-code");

        AuthTokenVO token = authService.login(loginRequest, "127.0.0.1", "test-agent");
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertEquals(7200, token.getExpiresIn());
        assertNotNull(token.getUser());
        assertNotNull(token.getUser().getEmail());
        assertTrue(token.getUser().getEmail().contains("@example.com"));
    }

    @Test
    void shouldRejectInvalidCaptcha() {
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(false);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("any@example.com");
        loginRequest.setPassword("123456");
        loginRequest.setCaptchaKey("mock-key");
        loginRequest.setCaptchaCode("wrong-code");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest, "127.0.0.1", "test-agent"));
        assertEquals(UserAuthErrorCode.CAPTCHA_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectWrongPassword() {
        String email = "login_wrong@example.com";
        String password = "123456";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);
        when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setEmailCode("000000");
        registerRequest.setPassword(password);
        registerRequest.setConfirmPassword(password);
        authService.register(registerRequest, "127.0.0.1", "test-agent");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("wrong-password");
        loginRequest.setCaptchaKey("mock-key");
        loginRequest.setCaptchaCode("mock-code");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest, "127.0.0.1", "test-agent"));
        assertEquals(UserAuthErrorCode.ACCOUNT_OR_PASSWORD_ERROR.getCode(), exception.getCode());
    }
}
