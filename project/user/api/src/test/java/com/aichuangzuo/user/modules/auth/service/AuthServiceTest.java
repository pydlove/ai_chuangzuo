package com.aichuangzuo.user.modules.auth.service;

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
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private EmailCodeService emailCodeService;

    @Test
    void shouldRegisterNewUserSuccessfully() {
        String email = "register_test@example.com";
        when(emailCodeService.validateEmailCode(anyString(), anyString())).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setEmailCode("000000");
        request.setPassword("123456");
        request.setConfirmPassword("123456");

        AuthTokenVO token = authService.register(request, "127.0.0.1", "test-agent");
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertEquals(7200, token.getExpiresIn());
        assertNotNull(token.getUser());
    }
}
