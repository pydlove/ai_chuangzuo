package com.aichuangzuo.user.modules.auth.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailMessageFactoryTest {

    @Test
    void shouldPopulateBrandedCodeEmail() throws Exception {
        String from = "noreply@aichuangzuo.com";
        String toEmail = "user@example.com";
        String code = "123456";

        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
        EmailMessageFactory.populateCodeEmail(msg, from, toEmail, code);

        assertEquals(from, msg.getFrom()[0].toString(), "发件人匹配");
        assertEquals(toEmail, msg.getAllRecipients()[0].toString(), "收件人匹配");
        assertEquals("你的爱创作验证码", msg.getSubject(), "主题匹配");

        Object content = msg.getContent();
        assertNotNull(content);
        assertTrue(content instanceof MimeMultipart, "应为 multipart/alternative");

        MimeMultipart mp = (MimeMultipart) content;
        assertEquals(2, mp.getCount(), "应包含 text/plain 和 text/html 两个部分");

        String plain = mp.getBodyPart(0).getContent().toString();
        String html = mp.getBodyPart(1).getContent().toString();

        assertTrue(plain.contains(code), "纯文本应包含验证码");
        assertTrue(plain.contains("5 分钟内有效"), "纯文本应包含有效期");
        assertTrue(plain.contains("爱创作"), "纯文本应包含品牌名");
        assertTrue(plain.contains("https://aichuangzuo.com/pricing"), "纯文本应包含会员链接");

        assertTrue(html.contains(code), "HTML 应包含验证码");
        assertTrue(html.contains("爱创作"), "HTML 应包含品牌名");
        assertTrue(html.contains("AI 自媒体写作助手"), "HTML 应包含 slogan");
        assertTrue(html.contains("多平台适配"), "HTML 应包含功能标签");
        assertTrue(html.contains("一键导出 Word"), "HTML 应包含功能标签");
        assertTrue(html.contains("了解会员权益"), "HTML 应包含会员引导");
        assertTrue(html.contains("https://aichuangzuo.com/pricing"), "HTML 应包含会员链接");
    }
}
