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
        text.setText(buildPlainText(code), "UTF-8");
        mp.addBodyPart(text);

        // text/html 分支
        MimeBodyPart html = new MimeBodyPart();
        html.setContent(buildHtml(code), "text/html;charset=UTF-8");
        mp.addBodyPart(html);

        msg.setContent(mp);
        msg.saveChanges();
    }

    private static String buildPlainText(String code) {
        return "Hi，欢迎加入爱创作！\n\n"
                + "你的验证码是：" + code + "\n"
                + "验证码 5 分钟内有效，请勿泄露给他人。\n\n"
                + "爱创作是一款 AI 自媒体写作助手。只需输入一个写作方向，"
                + "AI 会在 3 分钟内生成一篇结构完整、适配公众号、小红书、今日头条、"
                + "抖音图文、百家号等平台的自媒体文章。\n\n"
                + "核心功能：多平台适配、一键导出 Word\n"
                + "了解会员权益：https://aichuangzuo.com/pricing\n\n"
                + "本邮件由系统自动发出，请勿直接回复。\n"
                + "© 2026 杭州爱启云网络科技有限公司";
    }

    private static String buildHtml(String code) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>你的爱创作验证码</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f8f9fa;font-family:PingFang SC,Microsoft YaHei,Hiragino Sans GB,Helvetica Neue,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr><td align="center" style="padding:32px 16px;">
                      <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:12px;overflow:hidden;">
                        <tr><td style="padding:32px 32px 16px;">
                          <div style="font-size:20px;font-weight:700;color:#1a1a1a;">爱创作</div>
                          <div style="font-size:12px;color:#595959;margin-top:4px;">AI 自媒体写作助手</div>
                        </td></tr>
                        <tr><td style="padding:8px 32px 16px;font-size:16px;color:#262626;">
                          Hi，欢迎加入爱创作
                        </td></tr>
                        <tr><td style="padding:8px 32px 0;font-size:14px;color:#262626;">
                          你的验证码是
                        </td></tr>
                        <tr><td style="padding:16px 32px;">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#fff0f2;border-radius:8px;">
                            <tr><td align="center" style="padding:24px;font-size:40px;font-weight:700;color:#ff2442;letter-spacing:8px;">
                              %s
                            </td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:0 32px 24px;font-size:12px;color:#595959;">
                          验证码 5 分钟内有效，请勿泄露给他人。
                        </td></tr>
                        <tr><td style="padding:0 32px;"><div style="height:1px;background-color:#eeeeee;"></div></td></tr>
                        <tr><td style="padding:24px 32px;">
                          <div style="font-size:14px;font-weight:600;color:#1a1a1a;margin-bottom:8px;">AI 帮你高效创作自媒体内容</div>
                          <div style="font-size:13px;color:#595959;line-height:1.7;margin-bottom:12px;">
                            输入一个写作方向，AI 会在 3 分钟内生成一篇结构完整、适配公众号、小红书、今日头条、抖音图文、百家号等平台的自媒体文章。
                          </div>
                          <div style="margin-bottom:16px;">
                            <span style="display:inline-block;background-color:#fff0f2;color:#ff2442;font-size:12px;padding:4px 10px;border-radius:4px;margin-right:8px;">多平台适配</span>
                            <span style="display:inline-block;background-color:#fff0f2;color:#ff2442;font-size:12px;padding:4px 10px;border-radius:4px;">一键导出 Word</span>
                          </div>
                          <a href="https://aichuangzuo.com/pricing" style="font-size:13px;color:#ff2442;text-decoration:none;">了解会员权益 &rarr;</a>
                        </td></tr>
                        <tr><td style="padding:16px 32px 32px;font-size:11px;color:#8c8c8c;line-height:1.6;border-top:1px solid #eeeeee;">
                          本邮件由系统自动发出，请勿直接回复。<br>
                          &copy; 2026 杭州爱启云网络科技有限公司 &middot; All Rights Reserved
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(code);
    }
}
