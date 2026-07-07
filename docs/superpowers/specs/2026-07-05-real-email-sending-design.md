# 真实邮箱验证码发送 设计 (2026-07-05)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把当前 `EmailCodeServiceImpl.sendEmailCode()` 中的「console 输出验证码」的 mock 实现,替换为通过 SMTP 把验证码邮件真实发送给用户;test 环境用 GreenMail 嵌入式 SMTP server 拦截,生产用 126 SMTP(账号密码走 JASYPT 加密 + 环境变量注入)。

**Architecture:** 单文件改造 `EmailCodeServiceImpl`(删 mock 字段,注入 `JavaMailSender`),新增 1 个静态工厂类 `EmailMessageFactory` 构造 MimeMessage,新增 1 个测试用 `GreenMailTestConfig`,email-* mock 配置字段全删,pom 加 3 个依赖。同步阻塞发送,失败抛新增 `UserAuthErrorCode.EMAIL_SEND_FAILED`。

**Tech Stack:** Spring Boot 3 + Spring Boot Mail(`JavaMailSender` / `MimeMessage`) + GreenMail 2.0(`greenmail-spring` test 依赖) + jasypt-spring-boot-starter 3.0.5。

## Global Constraints

- **不引入新中间件**:仍走既有 Caffeine + MySQL + Spring Boot 自带栈;不接 SendGrid / Mailgun 等第三方服务
- **不在仓库里存任何明文凭证**:`spring.mail.password` 一律用 `ENC(...)`,Master Key 走 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量注入,绝不写入 `application*.yml`
- **Mock 完全移除**:`auth.email-code.mock-enabled` / `auth.email-code.mock-code` 两个字段彻底删除,不留兼容开关
- **同步发送**:不引入 `@EnableAsync` / ThreadPoolTaskExecutor,失败即抛 `EMAIL_SEND_FAILED`
- **生产 SMTP 配置走 `application-prod.yml`**,dev/test 走 `application-test.yml` + GreenMail
- **测试隔离**:`@TestConfiguration GreenMailTestConfig` 只在 `@SpringBootTest` 类里 `@Import`,不影响生产代码路径
- **错误码新增**:沿用 `UserAuthErrorCode` 体系,新增 `EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试")`
- **GlobalConstraints 来自本仓库 architecture 文档**:MySQL 8 + Flyway + JWT + Caffeine,已配置完毕,本次只在邮件栈上扩展

---

## 设计要点

### 1. 架构骨架(总览)

```
POST /api/v1/user/auth/email-codes
   ↓
EmailCodeService.sendEmailCode()
   ├─ captchaService.validate(key, code)
   ├─ checkEmailCodeLimit (24h ≤10 次)
   ├─ code = generateCode()           // 真随机 6 位数字
   ├─ cacheUtil.set(email→code, 5min)
   ├─ incrementEmailCodeCount
   └─ javaMailSender.send(buildCodeEmail(...))
      ├─ prod profile  → smtp.126.com:465
      └─ test profile  → 127.0.0.1:3025 (GreenMail in-memory)
   ↓ 成功   → log.info
   ↓ 失败   → catch (MailException | MessagingException)
            → throw BusinessException(EMAIL_SEND_FAILED)
```

### 2. 文件改动表

| 类型 | 文件 | 改动 |
|------|------|------|
| 修改 | `user/api/pom.xml` | + `spring-boot-starter-mail`(runtime)、+ `jasypt-spring-boot-starter` 3.0.5、+ `greenmail-spring` 2.0.1(test scope) |
| 修改 | `user/api/.../service/impl/EmailCodeServiceImpl.java` | 删 2 个 `@Value mock-*`;注入 `JavaMailSender` + `JavaMailSenderImpl`;`sendEmailCode()` 末尾构造 MimeMessage 并 `mailSender.send(...)`,失败 catch 抛 `EMAIL_SEND_FAILED` |
| 新增 | `user/api/.../mail/EmailMessageFactory.java` | 静态方法 `buildCodeEmail(Session, from, toEmail, code) → MimeMessage`,multipart/alternative(text + html),From 来自 `mailSenderImpl.getUsername()` |
| 修改 | `user/api/src/main/resources/application.yml` | 删除 `auth.email-code.*` 整段;新增 `spring.mail.host: ${SPRING_MAIL_HOST:127.0.0.1}` 等 6 个 key(password 用 `ENC(...)` 占位,真实密文走外部注入) |
| 修改 | `user/api/src/main/resources/application-prod.yml` | 新增 `spring.mail.host=smtp.126.com` / port=465 / username=<126 邮箱> / password=ENC(...)、protocol=smtp、`mail.smtp.ssl.enable=true` `mail.smtp.auth=true` |
| 修改 | `user/api/src/main/resources/application-test.yml` | 新增 `spring.mail.host=127.0.0.1 port=3025 username=test password=test`(被 GreenMail 接收,无需真鉴权);删除 `auth.email-code.mock-*` |
| 修改 | `shared/.../enums/error/UserAuthErrorCode.java` | 新增 `EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试")` |
| 新增 | `user/api/src/test/java/.../mail/GreenMailTestConfig.java` | `@TestConfiguration`:暴露 `GreenMail` bean(`new GreenMail(ServerSetupTest.SMTP)`,监听 3025),Spring 上下文关闭钩子自动 stop |
| 修改 | `user/api/src/test/java/.../service/EmailCodeServiceTest.java` | `@Import GreenMailTestConfig`;新增断言从 `greenMail.getReceivedMessages()` 拿到邮件,验证收件人 / Subject / 正文含 6 位码 |
| 修改 | `tests/e2e/verify_user_auth.py` | `EMAIL_CODE_MOCK = "000000"` 改为:调新增的 `GET /__test/email-code?email=...` 拿到真实验证码(test profile 仅) |
| 修改 | `tests/e2e/verify_reset_password.py` | 同上 |
| 新增 | `user/api/src/main/java/.../controller/DebugController.java` | test profile 下生效;`GET /__test/email-code?email=...` 返回缓存里的验证码(test profile 仅,`@Profile("test")` 注解,生产自动不注册) |

### 3. 核心组件契约

#### 3.1 接口(不变)

```java
public interface EmailCodeService {
    void sendEmailCode(String email, String captchaKey, String captchaCode);
    boolean validateEmailCode(String email, String emailCode);
}
```

签名不动,只动实现。

#### 3.2 `EmailMessageFactory`(新)

```java
package com.aichuangzuo.user.modules.auth.mail;

public final class EmailMessageFactory {
    private EmailMessageFactory() {}

    /** 在由 mailSender.createMimeMessage() 创建的空 MimeMessage 上填充 From/To/Subject/正文。 */
    public static void populateCodeEmail(
            MimeMessage msg,
            String from,
            String toEmail,
            String code) throws MessagingException {
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, toEmail);
        msg.setSubject("你的爱创作验证码", "UTF-8");
        Multipart mp = new MimeMultipart("alternative");
        // text/plain 分支
        MimeBodyPart text = new MimeBodyPart();
        text.setText("你的验证码是 " + code + ",5 分钟内有效,请勿泄露。", "UTF-8");
        mp.addBodyPart(text);
        // text/html 分支
        MimeBodyPart html = new MimeBodyPart();
        html.setContent(
            "<p>你的验证码是 <b>" + code + "</b>,5 分钟内有效,请勿泄露。</p>",
            "text/html;charset=UTF-8");
        mp.addBodyPart(html);
        msg.setContent(mp);
    }
}
```

调用方在 `EmailCodeServiceImpl` 里 `mailSender.createMimeMessage()` 拿到带 session 的 MimeMessage,再交给工厂填充内容。`from` 通过 `@Value("${spring.mail.username}")` 注入(避免额外注入 `JavaMailSenderImpl`)。

#### 3.3 `EmailCodeServiceImpl` 改造

```java
@Slf4j @Service @RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private final CaptchaService captchaService;
    private final CacheUtil cacheUtil;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;   // 直接从配置读,避免注入 JavaMailSenderImpl

    // 删除原 @Value("${auth.email-code.mock-enabled:false}") 与 mockCode 整块

    @Override
    public void sendEmailCode(String email, String captchaKey, String captchaCode) {
        if (!captchaService.validateCaptcha(captchaKey, captchaCode)) {
            throw new BusinessException(UserAuthErrorCode.CAPTCHA_ERROR);
        }
        checkEmailCodeLimit(email);
        String code = generateCode();
        cacheUtil.set(EMAIL_CODE_PREFIX + email, code, EMAIL_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        incrementEmailCodeCount(email);

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            EmailMessageFactory.populateCodeEmail(msg, mailFrom, email, code);
            mailSender.send(msg);
        } catch (MailException | MessagingException ex) {
            log.warn("邮箱发送失败 email={}, reason={}", email, ex.getMessage());
            throw new BusinessException(UserAuthErrorCode.EMAIL_SEND_FAILED);
        }
        log.info("邮箱验证码已发送 email={}, code={}", email, code);
    }

    // validateEmailCode / checkEmailCodeLimit / incrementEmailCodeCount / generateCode 全部不变
}
```

#### 3.4 错误码新增

`shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`:

```java
EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试"),
```

#### 3.5 `GreenMailTestConfig`(test only)

```java
@TestConfiguration
public class GreenMailTestConfig {
    private GreenMail greenMail;

    @Bean
    public GreenMail greenMail() {
        greenMail = new GreenMail(ServerSetupTest.SMTP); // 127.0.0.1:3025
        greenMail.start();
        return greenMail;
    }

    @PreDestroy
    public void stop() {
        if (greenMail != null) greenMail.stop();
    }
}
```

### 4. 数据流(用户视角)

```
用户输入邮箱 → 点"获取验证码"
   ↓
拖滑块 → POST /api/v1/user/auth/email-codes
   ↓
AuthController → AuthServiceImpl → EmailCodeService.sendEmailCode
   ↓
6 步: captcha / 限频 / 生成 / 缓存 / 计数 / SMTP send
   ↓ 成功 → {code:0, data:null}
   ↓ 失败 → {code:111014, message:"邮件发送失败,请稍后重试"}
```

### 5. 错误处理矩阵

| 场景 | 检测点 | 行为 |
|------|--------|------|
| 滑块 captcha 错 | `captchaService.validate()` | 抛 `CAPTCHA_ERROR`(已有) |
| 24h 超限频 | `checkEmailCodeLimit` | 抛 `OPERATION_TOO_FREQUENT`(已有) |
| 126 SMTP 4xx/5xx/超时 | `catch MailException` 在 sendEmailCode 末尾 | 抛新增 `EMAIL_SEND_FAILED` |
| 126 SMTP 鉴权失败 | 同上 | 同上(用户不区分,antd 提示"发送失败请重试") |
| 验证码 5min 过期 | `validateEmailCode` cache miss | false(已有) |
| 验证码错误 | equalsIgnoreCase | false(已有) |

**SMTP 重试**:不做。用户重发即可,不需要补发。

### 6. 测试矩阵

| 层 | 用例 | 方法 |
|----|------|------|
| 单测 | GreenMail 收到邮件,验收件人 = toEmail、Subject 含"验证码"、正文含 6 位数字 | `@Import GreenMailTestConfig`,`greenMail.getReceivedMessages()` |
| 单测 | SMTP 失败 → `EMAIL_SEND_FAILED` | `@MockBean JavaMailSender` 抛 `MailSendException` |
| 单测 | captcha 错误、限频超限 | 沿用现有 2 条断言 |
| E2E | `verify_user_auth.py` 真实验证码 | `GET /__test/email-code?email=...`(test profile only) |
| E2E | `verify_reset_password.py` 同上 | 同上 |
| 集成 | 生产 126 联通 | 手动跑 prod profile + 真邮箱;不自动化 |

### 7. 配置矩阵

| profile | SMTP host | credentials | 邮件 sender |
|---------|-----------|-------------|-------------|
| `test`  | `127.0.0.1:3025` | 无认证 | GreenMail |
| `prod`  | `smtp.126.com:465` | 126 授权码(ENC) | 126 SMTP |
| `dev`   | `127.0.0.1:3025` | 无认证 | GreenMail(本地起 server) |

dev 与 test 走同一份 Spring Boot Mail 配置(都连本地 3025),E2E + 集成测试都能复用 GreenMail。

### 8. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 凭证被贴到对话/明文提交到仓库 | 已要求 126 重置授权码 + 替换 JASYPT master key;新密文由用户本地加密后,只把 `ENC(...)` 占位 + `JASYPT_ENCRYPTOR_PASSWORD` 环境变量两件事写进 git,master key 本身永不落仓库 |
| 126 SMTP 网络抖动 → 用户感知慢 | 同步发送,前端按钮 loading + 5s 超时提示;失败 throw `EMAIL_SEND_FAILED`,前端 message.error 渲染 |
| MailException 类型多(认证 / 编码 / 连接 / 收件人格式) | catch 顶级父类 `MailException` + `MessagingException`,统一抛一个 `EMAIL_SEND_FAILED`,日志里用 log.warn 打 reason,用户层面不区分 |
| GreenMail 与 SMTP 真实端口在 126 有差异(SSL / 编码) | test profile 关 SSL,不验证书;prod profile 显式 `mail.smtp.ssl.enable=true` + `mail.smtp.auth=true` |
| e2e 拿验证码需要测试辅助端点 → 不能放进生产 | `DebugController` 用 `@Profile("test")` 限定,生产 profile 不注册,无安全风险 |
| Caffeine cache 重启丢失 → E2E 测试 setup 阶段拿不到之前发过的验证码 | DebugController 直接读 `CacheUtil.get(EMAIL_CODE_PREFIX + email)`;无需持久化 |

### 9. 实施任务分解(供 writing-plans 阶段展开)

1. pom 加 3 个依赖 + 验证 `mvn dependency:tree` 无冲突
2. `application.yml` 删 mock 字段 + 增 mail 占位(ENC)
3. `application-prod.yml` 增 prod 真实 126 配置(只用占位 + ENC,密文由用户离线提供)
4. `application-test.yml` 改 localhost:3025 + 删 mock
5. `UserAuthErrorCode` 新增 `EMAIL_SEND_FAILED`
6. `EmailMessageFactory` 新建(纯静态)
7. `EmailCodeServiceImpl` 改造(删 mock、注入 sender、加 try/catch)
8. `GreenMailTestConfig` 新建(`@TestConfiguration`)
9. `EmailCodeServiceTest` 改造(`@Import` + GreenMail 断言 + SMTP 失败 mock 用例)
10. `DebugController` 新建(`@Profile("test")`)
11. `tests/e2e/verify_user_auth.py` 适配(从 mock 改走 DebugController)
12. `tests/e2e/verify_reset_password.py` 适配
13. 全量测试 + E2E 全跑通

(每步对应 writing-plans 阶段的 1 个 task,共 13 个)
