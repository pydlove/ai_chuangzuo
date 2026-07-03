package com.aichuangzuo.user.modules.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "auth.captcha.mock-enabled=true",
    "auth.captcha.mock-code=TEST12"
})
class EmailCodeServiceTest {

    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private CaptchaService captchaService;

    @Test
    void shouldSendEmailCodeAfterValidCaptcha() {
        var captcha = captchaService.generateCaptcha();
        assertDoesNotThrow(() ->
            emailCodeService.sendEmailCode("test@example.com", captcha.getCaptchaKey(), "TEST12")
        );
    }

    @Test
    void shouldRejectInvalidCaptcha() {
        assertThrows(RuntimeException.class, () ->
            emailCodeService.sendEmailCode("test@example.com", "invalid-key", "invalid-code")
        );
    }
}
