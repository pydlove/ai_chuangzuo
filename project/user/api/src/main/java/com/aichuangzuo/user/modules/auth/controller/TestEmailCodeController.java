package com.aichuangzuo.user.modules.auth.controller;

import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 仅 test profile 注册的端点，供 E2E 从 GreenMail 抓最近一封目标邮箱的 6 位验证码。
 * 替代方案过去由 DebugController 做：从 Caffeine 缓存读验证码。
 * 这里改走真实邮件链路 (JavaMailSender → GreenMail) 让 E2E 校验更贴近生产。
 *
 * 生产环境绝不可用——@Profile("test") 限定，prod 启动时不注册这个 Controller。
 */
@RestController
@RequestMapping("/__test")
@Profile("test")
@RequiredArgsConstructor
public class TestEmailCodeController {

    private static final Pattern SIX_DIGITS = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");

    private final GreenMail greenMail;

    @GetMapping("/email-code")
    public Map<String, Object> emailCode(@RequestParam("email") String email) {
        if (greenMail == null) {
            return Map.of("found", false, "email", email);
        }
        try {
            MimeMessage[] messages = greenMail.getReceivedMessages();
            for (int i = messages.length - 1; i >= 0; i--) {
                MimeMessage msg = messages[i];
                if (msg.getAllRecipients() == null) continue;
                for (var recipient : msg.getAllRecipients()) {
                    if (email.equalsIgnoreCase(recipient.toString())) {
                        String body = extractText(msg);
                        Matcher m = SIX_DIGITS.matcher(body);
                        if (m.find()) {
                            return Map.of("found", true, "email", email, "code", m.group());
                        }
                        return Map.of("found", false, "email", email);
                    }
                }
            }
            return Map.of("found", false, "email", email);
        } catch (Exception ex) {
            return Map.of("found", false, "email", email, "error", ex.getClass().getSimpleName());
        }
    }

    private String extractText(MimeMessage msg) throws Exception {
        Object content = msg.getContent();
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof MimeMultipart mp) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart part = mp.getBodyPart(i);
                Object partContent = part.getContent();
                if (partContent instanceof String s) {
                    sb.append(s);
                }
            }
            return sb.toString();
        }
        return "";
    }
}
