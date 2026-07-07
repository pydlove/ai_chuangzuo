package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.user.modules.auth.mail.EmbeddedSmtpTestConfig;
import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实链路测试：JavaMailSender 默认走 127.0.0.1:3025 → GreenMail。
 * Spring profile=test 自动激活 EmbeddedSmtpTestConfig 启嵌入式 SMTP，
 * Spring 自动注册 GreenMail bean，省去 @Import。
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailCodeServiceTest {

    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private GreenMail greenMail;

    @Test
    void shouldSendEmailAndGreenMailReceives() throws Exception {
        String toEmail = "greenmail-recv@example.com";

        emailCodeService.sendEmailCode(toEmail);

        // 等一小段时间确保 GreenMail 收齐
        Thread.sleep(200);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length, "GreenMail 应该收到一封邮件");

        MimeMessage msg = received[0];
        assertEquals(toEmail, msg.getAllRecipients()[0].toString(), "收件人匹配");
        assertEquals("你的爱创作验证码", msg.getSubject(), "主题匹配");

        Object content = msg.getContent();
        assertNotNull(content, "邮件正文不应为空");
        assertTrue(content instanceof MimeMultipart,
                "正文应该是 multipart/alternative, 实际: " + content.getClass());
        MimeMultipart mp = (MimeMultipart) content;
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart part = mp.getBodyPart(i);
            body.append(part.getContent().toString());
        }
        assertTrue(body.toString().matches("(?s).*\\d{6}.*"),
                "正文应该包含 6 位数字验证码,实际: " + body);
    }
}
