package com.aichuangzuo.user.modules.auth.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * 构造爱创作「邮箱验证码」邮件的工厂。
 * 输入一个由 JavaMailSender.createMimeMessage() 创建的 MimeMessage，
 * 在其上填充 From / To / Subject / multipart/alternative 正文。
 */
public final class EmailMessageFactory {

    private EmailMessageFactory() {
    }

    public static void populateCodeEmail(MimeMessage msg, String from, String toEmail, String code)
            throws MessagingException {
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        msg.setSubject("你的爱创作验证码", "UTF-8");

        MimeMultipart mp = new MimeMultipart("alternative");

        // text/plain 分支
        MimeBodyPart text = new MimeBodyPart();
        text.setText("你的验证码是 " + code + ",5 分钟内有效,请勿泄露。", "UTF-8");
        mp.addBodyPart(text);

        // text/html 分支
        MimeBodyPart html = new MimeBodyPart();
        html.setContent(
                "<html><body>"
                        + "<p>你的验证码是 <b>" + code + "</b>,5 分钟内有效,请勿泄露。</p>"
                        + "<p style=\"color:#8c8c8c;font-size:12px;\">"
                        + "本邮件由系统自动发出,请勿直接回复。</p>"
                        + "</body></html>",
                "text/html;charset=UTF-8");
        mp.addBodyPart(html);

        msg.setContent(mp);
        msg.saveChanges();
    }
}
