package com.aichuangzuo.admin.modules.reminder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:test@local}")
    private String mailFrom;

    /**
     * 发送纯文本邮件。失败抛出 RuntimeException，由调用方记 send_log。
     */
    public void send(String toEmail, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(toEmail);
        msg.setSubject(subject);
        msg.setText(text);
        try {
            mailSender.send(msg);
        } catch (MailException ex) {
            log.warn("提醒邮件发送失败 to={}, reason={}", toEmail, ex.getMessage());
            throw ex;
        }
    }
}