# 邮箱验证码邮件模板 redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `EmailMessageFactory.populateCodeEmail()` 的简陋邮件正文升级为带品牌视觉、产品价值、功能亮点和会员引导的营销型邮件，并保持 multipart/alternative 与主流邮件客户端兼容。

**Architecture:** 在现有工厂类中用字符串模板（Java 17 text block + `replace`）拼接 HTML / 纯文本邮件正文；新增单元测试覆盖邮件主题、收件人、HTML 品牌元素、纯文本验证码；最后跑通现有 GreenMail 集成测试与 E2E，确保验证码提取正则仍命中。

**Tech Stack:** Spring Boot Mail (`MimeMessage` / `MimeMultipart`)、inline CSS + table 布局、JUnit 5、GreenMail。

## Global Constraints

- **不引入新中间件/模板引擎**: 继续使用字符串拼接构造 HTML，不引入 Thymeleaf / Freemarker / Velocity 等模板依赖。
- **保持 multipart/alternative**: 必须同时提供 `text/plain` 和 `text/html`，确保不支持 HTML 的客户端也能读取验证码。
- **邮件客户端兼容优先**: HTML 使用 table 布局 + inline style；避免 flexbox、grid、自定义字体、CSS 变量、RGBA 半透明阴影。
- **不添加外部图片**: 为避免被邮件客户端屏蔽/显示红叉，本次不使用远程图片 Logo，只用文字品牌名作为品牌头。
- **营销文案先硬编码**: 若后续频繁变更再抽成配置；本次以视觉和内容升级为主。
- **不改动发邮件流程、缓存、限频、错误码**。

---

## File Structure

| 文件 | 职责 |
|------|------|
| `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactory.java` | 构造验证码邮件：设置 From/To/Subject，生成 text/plain + text/html 正文 |
| `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactoryTest.java` | 单元测试：验证邮件主题、收件人、HTML 包含品牌/验证码/营销内容、纯文本包含验证码 |
| `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/EmailCodeServiceTest.java` | 现有集成测试：验证 GreenMail 能收到邮件，正文含 6 位数字 |
| `tests/e2e/verify_user_auth.py` | 现有 E2E：通过正则从 GreenMail 邮件中提取 6 位验证码 |
| `tests/e2e/verify_reset_password.py` | 现有 E2E：同上 |

---

### Task 1: 编写 EmailMessageFactory 单元测试

**Files:**
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactoryTest.java`

**Interfaces:**
- Consumes: `EmailMessageFactory.populateCodeEmail(MimeMessage msg, String from, String toEmail, String code)`
- Produces: JUnit 断言覆盖邮件主题、收件人、HTML 品牌元素、纯文本验证码

- [ ] **Step 1: 编写失败测试**

```java
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
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
cd project/user/api
mvn test -Dtest=EmailMessageFactoryTest -q
```

Expected: 编译通过，测试 FAIL（`EmailMessageFactory.populateCodeEmail` 当前生成的 HTML 不含新品牌元素）。

- [ ] **Step 3: Commit 测试文件**

```bash
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactoryTest.java
git commit -m "test(email): add EmailMessageFactory branded template test"
```

---

### Task 2: 实现品牌化邮件模板

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactory.java`

**Interfaces:**
- Consumes: `EmailCodeServiceImpl.populateCodeEmail(msg, mailFrom, email, code)`
- Produces: `MimeMessage` 包含 branded `text/plain` + `text/html` 正文

- [ ] **Step 1: 重写 EmailMessageFactory**

完整替换 `populateCodeEmail` 方法：

```java
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
```

注意：
- HTML 模板里的 `%%` 是为了在 text block 中输出单个 `%`，因为 `String.formatted(code)` 会把 `%s` 当作占位符。
- `&rarr;` 和 `&middot;` 是 HTML 实体，避免邮件客户端对特殊字符处理不一致。

- [ ] **Step 2: 运行单元测试**

Run:
```bash
cd project/user/api
mvn test -Dtest=EmailMessageFactoryTest -q
```

Expected: PASS。

- [ ] **Step 3: 运行现有集成测试**

Run:
```bash
cd project/user/api
mvn test -Dtest=EmailCodeServiceTest,EmailCodeServiceSmtpFailTest -q
```

Expected: PASS。`EmailCodeServiceTest` 中的正则 `.*\d{6}.*` 仍能命中新模板里的验证码。

- [ ] **Step 4: Commit 实现**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactory.java
git commit -m "feat(email): redesign verification email with branding and marketing content"
```

---

### Task 3: 手动邮件渲染验证

**Files:**
- 无（纯手动验证）

**Interfaces:**
- Consumes: 后端以 `test` profile 启动后调用 `POST /api/v1/user/auth/email-codes`
- Produces: 在真实邮件客户端中检查渲染效果

- [ ] **Step 1: 启动后端并发送测试邮件**

1. 确保本地 MySQL 与 GreenMail 环境可用：
   ```bash
   cd project/user/api
   SPRING_PROFILES_ACTIVE=test ./mvnw spring-boot:run
   ```
2. 调用接口发送验证码（可用 curl 或前端登录页点击「获取验证码」）：
   ```bash
   curl -X POST 'http://localhost:25050/api/v1/user/auth/email-codes' \
     -H 'Content-Type: application/json' \
     -d '{"email":"your-test-email@example.com"}'
   ```
   注意：test profile 走 GreenMail，不会发到真实邮箱；如要测真实客户端，需临时改 `application-dev.yml` 或 `application-prod.yml` 指向真实 SMTP。

- [ ] **Step 2: 检查常见客户端渲染**

打开邮件检查：
- Gmail Web / Gmail App：无错位、圆角/背景色正常
- Outlook Windows：table 不崩、验证码可见
- Apple Mail：完全支持
- QQ 邮箱 / 企业邮箱：无乱码、链接可点

- [ ] **Step 3: 记录结果**

如渲染有严重问题，回到 Task 2 调整 HTML；如仅圆角失效等可接受降级，在 commit message 或本 plan 的 Task 2 中注明 fallback 行为。

---

### Task 4: 跑 E2E 回归

**Files:**
- 无需修改：`tests/e2e/verify_user_auth.py`、`tests/e2e/verify_reset_password.py`

**Interfaces:**
- Consumes: 后端 `test` profile + 前端 dev server
- Produces: E2E 通过

- [ ] **Step 1: 启动全栈**

```bash
./scripts/local/user-full-stack/start.sh
```

- [ ] **Step 2: 跑用户认证 E2E**

```bash
python3 tests/e2e/verify_user_auth.py
```

Expected: 通过。`fetch_email_code()` 仍能从 GreenMail 邮件正文正则提取 6 位验证码。

- [ ] **Step 3: 跑重置密码 E2E**

```bash
python3 tests/e2e/verify_reset_password.py
```

Expected: 通过。

- [ ] **Step 4: Commit（如无需改动可跳过）**

若 E2E 无需修改，本 task 不产生新 commit。

---

## Self-Review

### 1. Spec Coverage

| Spec 要求 | 对应 Task |
|-----------|-----------|
| 品牌卡片布局、浅灰背景、白色卡片、圆角 12px | Task 2 |
| 品牌色 `#ff2442`、字体栈、字号字重 | Task 2 |
| 验证码大号 40px、淡色背景框 | Task 2 |
| 产品价值简介 + 功能标签 + 会员引导 | Task 2 |
| text/plain 同步更新 | Task 2 |
| multipart/alternative 保持 | Task 2 |
| table 布局 + inline CSS 兼容邮件客户端 | Task 2 |
| 不引入模板引擎、不添加外部图片 | Task 2（Global Constraints） |
| 单元测试覆盖 HTML/纯文本内容 | Task 1 + Task 2 |
| 现有集成测试与 E2E 回归 | Task 3 + Task 4 |

无遗漏。

### 2. Placeholder Scan

- 无 `TBD` / `TODO` / "implement later" / "fill in details"。
- 每个 Step 都包含完整代码或具体命令。
- 没有未定义的函数或类型。

### 3. Type Consistency

- `EmailMessageFactory.populateCodeEmail(MimeMessage, String, String, String)` 签名保持不变。
- `EmailCodeServiceImpl` 调用方式不变：`EmailMessageFactory.populateCodeEmail(msg, mailFrom, email, code)`。
- 测试中断言的文案与实现中文案一致。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-06-email-verification-template.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
