package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SMTP 失败路径测试：mock 掉 JavaMailSender 让 send() 抛 MailSendException，
 * 验证 EmailCodeServiceImpl 捕获并转抛 EMAIL_SEND_FAILED(111014)。
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailCodeServiceSmtpFailTest {

    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private CaptchaService captchaService;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setupMock() {
        Session session = Session.getInstance(new Properties());
        Mockito.when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(session));
        Mockito.doThrow(new MailSendException("smtp connection refused"))
                .when(mailSender).send(Mockito.<MimeMessage>any());
    }

    @Test
    void shouldThrowEmailSendFailedWhenSmtpFails() {
        var captcha = captchaService.generateCaptcha();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.sendEmailCode("fail@example.com",
                        captcha.getCaptchaKey(), "TEST12"));
        assertEquals(UserAuthErrorCode.EMAIL_SEND_FAILED.getCode(), ex.getCode());
    }
}
