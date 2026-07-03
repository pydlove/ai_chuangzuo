package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.user.modules.auth.vo.CaptchaVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CaptchaServiceTest {

    @Autowired
    private CaptchaService captchaService;

    @Test
    void shouldGenerateCaptchaWithKeyAndImage() {
        CaptchaVO captcha = captchaService.generateCaptcha();
        assertNotNull(captcha.getCaptchaKey());
        assertNotNull(captcha.getCaptchaImage());
        assertTrue(captcha.getCaptchaImage().startsWith("data:image/png;base64,"));
    }
}
