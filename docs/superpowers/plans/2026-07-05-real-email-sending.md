# 真实邮箱验证码发送功能 实施计划 (2026-07-05)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `EmailCodeService.sendEmailCode()` 从「mock 输出到 log」改为「通过 JavaMailSender 真实发送 SMTP 邮件」;test profile 用 GreenMail 嵌入式 SMTP 拦截验证,prod profile 走 126 SMTP(账号密码用 JASYPT 加密 + 运行时环境变量注入)。

**Architecture:** 改造 `EmailCodeServiceImpl`(删 mock 字段、注入 `JavaMailSender` + `@Value spring.mail.username`、构造 MimeMessage 后发送、catch MailException/MessagingException 抛新增 `EMAIL_SEND_FAILED`)。新增 `EmailMessageFactory` 静态工厂封装邮件内容(text + html multipart/alternative)。test profile 接入 GreenMail(`@TestConfiguration` 自动启停)。E2E 通过新加的 `@Profile("test") DebugController` 取真实邮箱验证码。

**Tech Stack:** Spring Boot 3 + Spring Boot Mail(`JavaMailSender` / `MimeMessage` / `MimeMultipart`) + jasypt-spring-boot-starter 3.0.5 + GreenMail 2.0.1(test scope) + Caffeine(已有)+ MySQL 8 + Flyway(已有)。

## Global Constraints

- **不引入新中间件**(Redis / MQ / OSS 等一律不加),仍用 Spring Boot 自带 + Caffeine + MySQL
- **仓库不存任何明文凭证**:`spring.mail.password` 一律 `ENC(...)` 占位;`JASYPT_ENCRYPTOR_PASSWORD` 通过环境变量注入,绝不写进任何 `application*.yml` / shell 脚本 / git 历史
- **完全移除 mock**:`auth.email-code.mock-enabled` / `auth.email-code.mock-code` 两个字段彻底删除,**不保留兼容开关**(旧代码引用全部改成依赖注入的真实发送)
- **同步发送**:不引入 `@EnableAsync` / ThreadPoolTaskExecutor;失败即抛 `EMAIL_SEND_FAILED`
- **Profile 矩阵**:prod 用 `smtp.126.com:465` + 126 授权码;test 强制 `127.0.0.1:3025` + GreenMail(`mailto` 用户 + 任意密码,GreenMail 默认放行);dev 与 test 共用 test profile(本地启动时直接跑 test profile 即可)
- **错误码**:沿用 `UserAuthErrorCode` 体系,新增 `EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试")`
- **E2E 取验证码**:通过 `@Profile("test")` 限定的 `DebugController`(路径 `/__test/email-code?email=...`),生产自动不注册
- **包路径**:核心代码在 `com.aichuangzuo.user.modules.auth.mail.{EmailMessageFactory, ...}`;测试 config 在 `com.aichuangzuo.user.modules.auth.mail` 同包
- **测试隔离**:`GreenMailTestConfig` 只用 `@Import`(不是 `@SpringBootTest classes =`),保证不被生产 classpath 拉起
- **Java**:仍用 `@RequiredArgsConstructor` + Lombok;MailException / MessagingException 用顶级父类 catch,统一抛 111014

## File Structure

**后端新增/修改：**
- 修改：`project/user/api/pom.xml`（+3 个依赖）
- 修改：`project/user/api/src/main/resources/application.yml`（删 mock + 新增 mail 占位）
- 新增：`project/user/api/src/main/resources/application-prod.yml`（prod 126 SMTP 占位）
- 修改：`project/user/api/src/main/resources/application-test.yml`（localhost:3025 GreenMail + 删 mock）
- 修改：`project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`（+1 个枚举值）
- 修改：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactory.java`
- 新增：`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/DebugController.java`
- 新增：`project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/GreenMailTestConfig.java`
- 修改：`project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/EmailCodeServiceTest.java`

**E2E 改造：**
- 修改：`tests/e2e/verify_user_auth.py`
- 修改：`tests/e2e/verify_reset_password.py`

---

### Task 1: pom 追加 3 个依赖

**Files:**
- Modify: `project/user/api/pom.xml:99-102`（在 `spring-boot-starter-test` 之前插入 3 个依赖）

**Consumes:** 无

**Produces:** pom 包含 `spring-boot-starter-mail`、`jasypt-spring-boot-starter`、`greenmail-spring`(test scope)

- [ ] **Step 1: 修改 pom.xml**

打开 `project/user/api/pom.xml`,在 `</dependencies>` 之前、`spring-boot-starter-test` 之前,加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-spring</artifactId>
    <version>2.0.1</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 验证依赖解析 + 编译**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api dependency:resolve -q 2>&1 | tail -20
mvn -pl shared install -DskipTests -q
mvn -pl user/api compile -q
```

Expected: 三个 BUILD SUCCESS，没有 unresolved artifact。如果 greenmail-spring:2.0.1 在 Maven Central 不可见，临时换 1.6.x（兼容性退路，不在本任务范围内）。

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/pom.xml
git commit -m "$(cat <<'EOF'
feat(user-api): pom 加 spring-boot-starter-mail / jasypt / greenmail

为后续真实邮箱发送 + GreenMail 拦截 + JASYPT 解密做依赖准备。

- spring-boot-starter-mail: JavaMailSender 真实发送
- jasypt-spring-boot-starter 3.0.5: ENC(...) 自动解密
- greenmail-spring 2.0.1 (test): test profile 嵌入式 SMTP

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: config 改造（application*.yml）

**Files:**
- Modify: `project/user/api/src/main/resources/application.yml`
- Create: `project/user/api/src/main/resources/application-prod.yml`
- Modify: `project/user/api/src/main/resources/application-test.yml`

**Consumes:** Task 1 完成的依赖

**Produces:** 默认 profile 没有 mock，prod profile 有 126 SMTP 占位，test profile 强制 127.0.0.1:3025 + GreenMail

- [ ] **Step 1: 修改 application.yml**

打开 `project/user/api/src/main/resources/application.yml`，**删除文件末尾**的 `auth:` / `register:` / `jwt:` 这几行（保留 `auth:` 字段以备后用，只删 `email-code` 部分），并**新增** `spring.mail.*` 段：

```yaml
server:
  port: 25050

spring:
  application:
    name: user-api
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/aichuangzuo?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  mail:
    host: ${SPRING_MAIL_HOST:127.0.0.1}
    port: ${SPRING_MAIL_PORT:3025}
    username: ${SPRING_MAIL_USERNAME:test@local}
    password: ${SPRING_MAIL_PASSWORD:local-dev-no-auth}
    protocol: smtp
    default-encoding: UTF-8
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
      mail.smtp.ssl.enable: false
      mail.smtp.connectiontimeout: 5000
      mail.smtp.timeout: 5000
      mail.smtp.writetimeout: 5000

auth:
  jwt:
    access-secret: ${JWT_ACCESS_SECRET:please-change-this-access-secret-at-least-256-bits-long}
    refresh-secret: ${JWT_REFRESH_SECRET:please-change-this-refresh-secret-at-least-256-bits-long}
    access-expiration: 7200
    refresh-expiration: 604800
  register:
    max-per-ip: 10
```

注意：**完全不包含 `auth.email-code.*` 字段**（mock 字段已彻底移除）。默认 profile mail.* 用 localhost:3025 + no-auth 跑通，prod/test profile 各自 override。

- [ ] **Step 2: 新增 application-prod.yml**

创建 `project/user/api/src/main/resources/application-prod.yml`：

```yaml
spring:
  mail:
    host: smtp.126.com
    port: 465
    username: aiocloud@126.com
    # 占位：真实密文由用户在本地用 jasypt CLI 加密后替换此处
    # 命令：java -cp '~/.m2/repository/org/jasypt/jasypt/1.9.3/jasypt-1.9.3.jar' \
    #       org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
    #       input="<明文授权码>" password="MySecretKey2026!" \
    #       algorithm=PBEWithMD5AndDES
    # 然后把输出粘贴进 password 的 ENC(...) 占位内
    password: ENC(REPLACE_WITH_LOCAL_ENCRYPTED_126_AUTH_CODE)
    protocol: smtp
    default-encoding: UTF-8
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: false
      mail.smtp.ssl.enable: true
      mail.smtp.connectiontimeout: 10000
      mail.smtp.timeout: 10000
      mail.smtp.writetimeout: 10000

logging:
  level:
    com.aichuangzuo: INFO
```

注意：`username: aiocloud@126.com` 是脱敏后的明文（地址本身算半公开），但 `password: ENC(...)` 是占位 — 真实密文由用户本地 CLI 加密后填入，不进入对话/聊天框。

- [ ] **Step 3: 修改 application-test.yml**

把现有 `project/user/api/src/main/resources/application-test.yml`：

```yaml
auth:
  captcha:
    mock-enabled: true
    mock-code: "TEST12"
  email-code:
    mock-enabled: true
    mock-code: "000000"

logging:
  level:
    com.aichuangzuo: INFO
```

改成：

```yaml
spring:
  mail:
    host: 127.0.0.1
    port: 3025
    username: test@local
    password: local-dev-no-auth
    protocol: smtp
    properties:
      mail.smtp.auth: false
      mail.smtp.ssl.enable: false

logging:
  level:
    com.aichuangzuo: INFO
```

`auth.captcha.*` mock 字段**保留**（图形码 mock 与本次任务无关），但 `auth.email-code.*` 已从 application.yml 全局删除 — 不需要在 test profile 里 override。

- [ ] **Step 4: 启动 backend 验证 yml 正确**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl shared install -DskipTests -q
nohup mvn -pl user/api spring-boot:run -Dspring-boot.run.profiles=test \
  > /tmp/user-api-test.log 2>&1 &
MVN_PID=$!
echo "MVN_PID=$MVN_PID"

# 等启动
for i in $(seq 1 60); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:25050/api/v1/user/auth/captcha)
  if [ "$STATUS" = "200" ]; then
    echo "ready in ${i}s"; break
  fi
  sleep 1
done

# 验证 EmailCodeServiceImpl 启动时不再依赖 mock 字段
grep -i "ACTIVE\|profile\|started" /tmp/user-api-test.log | tail -5
```

Expected: profile=test 启动成功，控制台日志输出 `Started UserApiApplication`，没有 `Could not resolve placeholder 'auth.email-code.mock-enabled'` 之类错误（之前 mock 字段已经全删）。

如果看到 `Unable to resolve placeholder 'auth.email-code.mock-*'` 错误，说明删除位置有残留，回头检查 application.yml。

- [ ] **Step 5: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/resources/
git commit -m "$(cat <<'EOF'
feat(user-api): yml 迁移到 spring.mail.*,移除 email-code mock 字段

- application.yml: 删 auth.email-code.*,新增 spring.mail.* 默认占位
- application-prod.yml: 新增,prod 走 smtp.126.com:465,password 用 ENC 占位
- application-test.yml: 改 localhost:3025,提供 GreenMail 友好的 SMTP 配置

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: UserAuthErrorCode 新增 EMAIL_SEND_FAILED

**Files:**
- Modify: `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`

**Consumes:** 无

**Produces:** `UserAuthErrorCode.EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试")`

- [ ] **Step 1: 在 USER_NOT_FOUND 之后插入新枚举值**

打开 `project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java`，在 `USER_NOT_FOUND(111013, "用户不存在"),` 之后插入：

```java
EMAIL_SEND_FAILED(111014, "邮件发送失败,请稍后重试"),
```

- [ ] **Step 2: 重新安装 shared + 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl shared install -DskipTests -q
mvn -pl user/api compile -q 2>&1 | tail -5
```

Expected: 编译成功，UserAuthErrorCode.EMAIL_SEND_FAILED 可被引用。

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/shared/src/main/java/com/aichuangzuo/shared/enums/error/UserAuthErrorCode.java
git commit -m "$(cat <<'EOF'
feat(shared): UserAuthErrorCode 新增 EMAIL_SEND_FAILED(111014)

邮件发送失败时统一抛该错误码。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: EmailMessageFactory 新建

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactory.java`

**Consumes:** 无

**Produces:** `EmailMessageFactory.populateCodeEmail(MimeMessage, String from, String toEmail, String code)` 静态方法

- [ ] **Step 1: 创建包目录 + 工厂文件**

```bash
mkdir -p /Users/panyong/aio_project/ai_chuangzuo/project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail
```

写入 `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/EmailMessageFactory.java`：

```java
package com.aichuangzuo.user.modules.auth.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Message;
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
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS（依赖 jakarta.mail.* 由 spring-boot-starter-mail 自动传递引入）。

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/mail/
git commit -m "$(cat <<'EOF'
feat(user-api): 新增 EmailMessageFactory 构造验证码邮件 multipart

工厂只关心内容填充,From/To/Subject 均通过参数传入,
由调用方在 JavaMailSender 提供的带 Session 的 MimeMessage 上调用。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: EmailCodeServiceImpl 改造(核心)

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java`

**Consumes:** Task 3（错误码）+ Task 4（工厂）

**Produces:** `EmailCodeServiceImpl.sendEmailCode()` 真实通过 JavaMailSender 发邮件；catch MailException/MessagingException 抛 EMAIL_SEND_FAILED

- [ ] **Step 1: 重写 EmailCodeServiceImpl**

完整替换文件 `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java` 为：

```java
package com.aichuangzuo.user.modules.auth.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.auth.mail.EmailMessageFactory;
import com.aichuangzuo.user.modules.auth.service.CaptchaService;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private final CaptchaService captchaService;
    private final CacheUtil cacheUtil;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    private static final String EMAIL_CODE_PREFIX = "user:auth:email-code:";
    private static final String EMAIL_CODE_COUNT_PREFIX = "user:auth:email-code-count:";
    private static final long EMAIL_CODE_TTL_MINUTES = 5;
    private static final long EMAIL_CODE_COUNT_TTL_HOURS = 24;
    private static final int MAX_EMAIL_CODE_PER_EMAIL = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

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

    @Override
    public boolean validateEmailCode(String email, String emailCode) {
        String key = EMAIL_CODE_PREFIX + email;
        String cachedCode = cacheUtil.get(key);
        if (cachedCode == null) {
            return false;
        }
        cacheUtil.delete(key);
        return cachedCode.equalsIgnoreCase(emailCode);
    }

    private void checkEmailCodeLimit(String email) {
        AtomicInteger count = cacheUtil.get(EMAIL_CODE_COUNT_PREFIX + email);
        if (count != null && count.get() >= MAX_EMAIL_CODE_PER_EMAIL) {
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }
    }

    private void incrementEmailCodeCount(String email) {
        String key = EMAIL_CODE_COUNT_PREFIX + email;
        AtomicInteger count = cacheUtil.get(key);
        if (count == null) {
            count = new AtomicInteger(0);
            cacheUtil.set(key, count, EMAIL_CODE_COUNT_TTL_HOURS, TimeUnit.HOURS);
        }
        count.incrementAndGet();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
```

要点说明：
- 删除原 `@Value("${auth.email-code.mock-enabled:false}")` 和 `@Value("${auth.email-code.mock-code:}")` 两个字段
- 原 `String code = mockEnabled ? mockCode : generateCode();` 改为统一 `String code = generateCode();`
- 注入 `JavaMailSender`(Spring Boot Mail 自动配置)
- `@Value("${spring.mail.username}")` 拿 From
- `mailSender.createMimeMessage()` 已用 Spring 配置的 Session
- catch 顶级父类 `MailException` + `MessagingException`,统一抛 `EMAIL_SEND_FAILED`

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api compile -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS。如果出现 `Could not resolve 'spring.mail.username'` placeholder 错误，确认 application.yml / test profile yml 里都有 `spring.mail.username`。

- [ ] **Step 3: 启动 backend 验证连通**

```bash
# 杀掉 Task 2 step 4 的 backend
pkill -f 'spring-boot:run.*user/api' 2>/dev/null
pkill -f 'UserApiApplication' 2>/dev/null
sleep 2

cd /Users/panyong/aio_project/ai_chuangzuo/project
nohup mvn -pl user/api spring-boot:run -Dspring-boot.run.profiles=test \
  > /tmp/user-api-test.log 2>&1 &

# 等启动
for i in $(seq 1 90); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:25050/api/v1/user/auth/captcha)
  if [ "$STATUS" = "200" ]; then
    echo "ready in ${i}s"; break
  fi
  sleep 1
done

# 触发一次 sendEmailCode(没有 GreenMail,这里只是验证 service 装配成功,发邮件会失败 → 抛 EMAIL_SEND_FAILED)
TEST_EMAIL="tf-connect-$(date +%s)@example.com"
CAP=$(curl -s http://localhost:25050/api/v1/user/auth/captcha)
CAPKEY=$(echo "$CAP" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["captchaKey"])')
curl -s -X POST http://localhost:25050/api/v1/user/auth/email-codes \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"captchaKey\":\"$CAPKEY\",\"captchaCode\":\"TEST12\"}"
echo ""

# 期望返回 {"code":111014, "message":"邮件发送失败,请稍后重试"} 或成功（取决于 GreenMail 是否启动）
# 关键断言：响应里 code 字段是数字，且不是 5xx
echo "--- log tail ---"
grep -E "WARN|ERROR|Started UserApiApplication" /tmp/user-api-test.log | tail -10
```

Expected:
- backend 启动日志里有 `Started UserApiApplication`
- 接口返回 code 是数字(可能 0 也可能 111014),不是 500 Internal Server Error
- 如果 code=111014,说明抛了 BusinessException 走正常错误流 → service 装配成功
- 如果 code=0,说明 GreenMail 已经在某处拉起(意外),也是 OK 的

- [ ] **Step 4: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/service/impl/EmailCodeServiceImpl.java
git commit -m "$(cat <<'EOF'
feat(user-api): EmailCodeServiceImpl 真实发送 SMTP 邮件

- 删 auth.email-code.mock-* 字段(完全移除 mock)
- 注入 JavaMailSender + @Value spring.mail.username
- 通过 mailSender.createMimeMessage() + EmailMessageFactory 构造 multipart 邮件
- catch MailException/MessagingException 抛新增 EMAIL_SEND_FAILED(111014)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: DebugController 新建(test profile only)

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/DebugController.java`

**Consumes:** EmailCodeServiceImpl 的 `EMAIL_CODE_PREFIX` cache key 前缀(读取最近的验证码)

**Produces:** `GET /__test/email-code?email=...` — 返回刚发出的验证码字符串(仅 test profile 注册)

- [ ] **Step 1: 创建 controller**

`project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/DebugController.java`：

```java
package com.aichuangzuo.user.modules.auth.controller;

import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仅在 test profile 下注册的调试端点。
 * 用途：E2E 测试用真实邮箱触发 sendEmailCode 后，
 *       从这里读取缓存里的 6 位验证码，模拟「用户在邮箱里看到码」的场景。
 *
 * 生产环境绝不可用——@Profile("test") 限定 Spring Boot 启动时不注册这个 Controller。
 */
@RestController
@RequestMapping("/__test")
@Profile("test")
@RequiredArgsConstructor
public class DebugController {

    private final CacheUtil cacheUtil;

    private static final String EMAIL_CODE_PREFIX = "user:auth:email-code:";

    @GetMapping("/email-code")
    public Map<String, Object> emailCode(@RequestParam String email) {
        String code = cacheUtil.get(EMAIL_CODE_PREFIX + email);
        if (code == null) {
            return Map.of("found", false, "email", email);
        }
        return Map.of("found", true, "email", email, "code", code);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api compile -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: 端到端验证(test profile 启动后可访问,prod 不可见)**

由于 backend 当前正以 test profile 运行（Task 5 step 3 启动的），先复用：

```bash
# 等服务就绪
for i in $(seq 1 30); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:25050/api/v1/user/auth/captcha)
  if [ "$STATUS" = "200" ]; then
    echo "ready"; break
  fi
  sleep 1
done

# 触发一次 sendEmailCode（这里没有 GreenMail 接收方，会抛 EMAIL_SEND_FAILED 但验证码已经写入了 cache）
TEST_EMAIL="tf-debug-$(date +%s)@example.com"
CAP=$(curl -s http://localhost:25050/api/v1/user/auth/captcha)
CAPKEY=$(echo "$CAP" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["captchaKey"])')
curl -s -X POST http://localhost:25050/api/v1/user/auth/email-codes \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"captchaKey\":\"$CAPKEY\",\"captchaCode\":\"TEST12\"}" > /dev/null

# 读 cache
sleep 1
curl -s "http://localhost:25050/__test/email-code?email=$TEST_EMAIL"
echo ""
```

Expected：返回 `{"found":true,"email":"tf-debug-...@example.com","code":"123456"}`（码为 6 位随机数字）。

- [ ] **Step 4: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/auth/controller/DebugController.java
git commit -m "$(cat <<'EOF'
feat(user-api): DebugController @Profile(test) 暴露邮箱验证码给 E2E

GET /__test/email-code?email=... 直接读 CacheUtil 里的验证码。
@Profile("test") 限定生产 profile 不注册此 Controller。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: GreenMailTestConfig 新建(test @TestConfiguration)

**Files:**
- Create: `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/GreenMailTestConfig.java`

**Consumes:** greenmail-spring 2.0.1 (Task 1 装入)

**Produces:** `@TestConfiguration` 暴露 `GreenMail` bean(SMTP 监听 127.0.0.1:3025)

- [ ] **Step 1: 创建测试目录和 config 文件**

```bash
mkdir -p /Users/panyong/aio_project/ai_chuangzuo/project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail
```

写入 `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/GreenMailTestConfig.java`：

```java
package com.aichuangzuo.user.modules.auth.mail;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 在 @SpringBootTest 类里用 @Import(GreenMailTestConfig.class) 启用。
 * 启动一个监听 127.0.0.1:3025 的 in-memory SMTP server，
 * 让邮件链路 JavaMailSender → SMTP → GreenMail 真正闭环。
 */
@TestConfiguration
public class GreenMailTestConfig {

    private GreenMail greenMail;

    @Bean
    public GreenMail greenMail() {
        // ServerSetupTest.SMTP = 127.0.0.1:3025
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        return greenMail;
    }

    @Bean
    public GreenMailExtension greenMailExtension(GreenMail greenMail) {
        // 仅用于兼容旧 API 调用；本身已经通过 @Bean GreenMail 装配
        return new GreenMailExtension(ServerSetupTest.SMTP);
    }

    @PreDestroy
    public void stop() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }
}
```

注意：`ServerSetupTest` 是 `com.icegreen.greenmail.util.ServerSetupTest`（greenmail-spring 2.0.1 提供）。常量 `ServerSetupTest.SMTP` 就是 127.0.0.1:3025 的 `ServerSetup`。

- [ ] **Step 2: 编译测试代码**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api test-compile -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS（test 编译）。

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/mail/
git commit -m "$(cat <<'EOF'
test(user-api): 新增 GreenMailTestConfig 监听 127.0.0.1:3025

在 @SpringBootTest 类里 @Import 启用,内嵌 in-memory SMTP server,
让 EmailCodeService → JavaMailSender → GreenMail 完整链路闭环。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: EmailCodeServiceTest 改造

**Files:**
- Modify: `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/EmailCodeServiceTest.java`

**Consumes:** Task 5（真实 impl）+ Task 7（GreenMail）

**Produces:** 3 条单测：(1) GreenMail 收到邮件断言收件人/Subject/正文含验证码；(2) captcha 错误；(3) SMTP 失败抛 EMAIL_SEND_FAILED

- [ ] **Step 1: 重写测试文件**

完整替换 `project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/EmailCodeServiceTest.java`：

```java
package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.mail.GreenMailTestConfig;
import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(GreenMailTestConfig.class)
class EmailCodeServiceTest {

    @Autowired
    private EmailCodeService emailCodeService;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private GreenMail greenMail;

    /** 替换 EmailCodeService 内部的 JavaMailSender。让 send() 抛 MailException。 */
    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void shouldSendEmailAndGreenMailReceives() throws Exception {
        var captcha = captchaService.generateCaptcha();
        String toEmail = "greenmail-recv@example.com";

        emailCodeService.sendEmailCode(toEmail, captcha.getCaptchaKey(), "TEST12");

        // 等待一帧确认 GreenMail 已经收齐了邮件
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length, "GreenMail 应该收到一封邮件");

        MimeMessage msg = received[0];
        assertEquals(toEmail, msg.getAllRecipients()[0].toString(), "收件人匹配");
        assertEquals("你的爱创作验证码", msg.getSubject(), "主题匹配");
        String content = (String) msg.getContent();
        assertTrue(content.matches("(?s).*\\d{6}.*"), "正文应该包含 6 位数字验证码");
    }

    @Test
    void shouldThrowEmailSendFailedWhenSmtpFails() {
        // 让 mock 出来的 JavaMailSender.send() 抛 MailSendException
        org.mockito.Mockito.doThrow(new MailSendException("smtp connection refused"))
                .when(mailSender).send((MimeMessage) org.mockito.ArgumentMatchers.any());

        var captcha = captchaService.generateCaptcha();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.sendEmailCode("fail@example.com", captcha.getCaptchaKey(), "TEST12"));
        assertEquals(UserAuthErrorCode.EMAIL_SEND_FAILED.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectInvalidCaptcha() {
        assertThrows(BusinessException.class, () ->
                emailCodeService.sendEmailCode("test@example.com", "invalid-key", "invalid-code"));
    }
}
```

要点：
- `@SpringBootTest` + `@Import(GreenMailTestConfig.class)`
- 用 `@MockitoBean`（Spring Boot 3.x 提供的 `@MockBean` 替代）覆盖 `JavaMailSender`，让 SMTP 失败 mock 用例生效
- 第 1 条用例跑真实 GreenMail 链路，断言收件人/主题/正文
- 第 2 条用例 mock JavaMailSender 抛 MailSendException
- 第 3 条是原有用例沿用

> ⚠️ 注：如果项目 Spring Boot 版本早于 3.4，需要把 `@MockitoBean` 换成 `@MockBean`，两者功能等价（3.4 之前是 `@MockBean`，3.4 引入新注解 `@MockitoBean`）。

- [ ] **Step 2: 跑测试**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api test -Dtest='EmailCodeServiceTest' -DfailIfNoTests=false \
  -Dargline="-Djava.awt.headless=true" 2>&1 | grep -E "(Tests run|BUILD|FAIL)" | tail -10
```

Expected：3 tests run, 0 failures, BUILD SUCCESS。`shouldSendEmailAndGreenMailReceives` 应该验证 GreenMail 收到邮件。

如果 `shouldSendEmailAndGreenMailReceives` FAIL（GreenMail 没收到邮件）：
- 检查 GreenMail 端口 3025 没被占用：`lsof -ti:3025`
- 看 surefire 日志：`target/surefire-reports/com.aichuangzuo...EmailCodeServiceTest-output.txt`

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/test/java/com/aichuangzuo/user/modules/auth/service/EmailCodeServiceTest.java
git commit -m "$(cat <<'EOF'
test(user-api): EmailCodeServiceTest 改造为 GreenMail 验证

- @Import GreenMailTestConfig 启用嵌入式 SMTP
- @MockitoBean 覆盖 JavaMailSender 模拟 SMTP 失败
- 断言:GreenMail 收到邮件、收件人/主题/正文正确;SMTP 失败抛 EMAIL_SEND_FAILED

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 9: verify_user_auth.py E2E 适配

**Files:**
- Modify: `tests/e2e/verify_user_auth.py`

**Consumes:** Task 6（`/__test/email-code` 端点）

**Produces:** E2E 跑 register 流程,从 `/__test/email-code` 拿真实验证码,而不是写死 000000

- [ ] **Step 1: 修改 EMAIL_CODE_MOCK 与 register 流程**

打开 `tests/e2e/verify_user_auth.py`，找到 `EMAIL_CODE_MOCK = "000000"` 的引用处（多处），把 register 段里的：

```python
EMAIL_CODE_MOCK = "000000"
```

改为：

```python
EMAIL_CODE_MOCK = None  # 由测试代码通过 /__test/email-code 实时拉取

def fetch_email_code(email):
    """从 backend test profile 暴露的调试端点拿真实的 6 位邮箱验证码"""
    import time
    for _ in range(10):
        resp = requests.get(f"{API_URL.replace('/api/v1/user','')}/__test/email-code", params={"email": email}, timeout=5)
        body = resp.json()
        if body.get("found"):
            return body["code"]
        time.sleep(0.5)
    raise RuntimeError(f"5 秒内未拿到 email={email} 的验证码")
```

然后在原本填 `EMAIL_CODE_MOCK` 的位置（通常是 `register` API 调用或注册页的 email code 输入框），改成调用 `fetch_email_code(email)`：

```python
# register API 调用示例
import requests
real_code = fetch_email_code(email)
resp = requests.post(f"{API_URL}/auth/register", json={
    "email": email,
    "emailCode": real_code,
    "password": password,
    "confirmPassword": password,
    "captchaKey": cap2["data"]["captchaKey"],
    "captchaCode": "TEST12",
})
```

注意 `API_URL` 当前是 `http://localhost:25050/api/v1/user`,所以 `API_URL.replace('/api/v1/user','')` 拿到 `http://localhost:25050`,与新加的 `DebugController` 路径 `/__test/email-code` 拼接。

- [ ] **Step 2: 跑 E2E**

```bash
# 确保 backend 跑 SPRING_PROFILES_ACTIVE=test
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_user_auth.py 2>&1 | tail -30
```

Expected：所有 PASS。register 流程通过 `/__test/email-code` 拿到真实 6 位验证码。

如果 register 失败显示 `EMAIL_CODE_ERROR`:检查 `fetch_email_code` 返回的码是否正确传给 register（验证码是一次性的，`fetch_email_code` 跑通后 register 内部 `validateEmailCode` 会消费这个码，所以中间不能再掉一次）。

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add tests/e2e/verify_user_auth.py
git commit -m "$(cat <<'EOF'
test(e2e): verify_user_auth 改走 /__test/email-code 拿真实验证码

从 hardcoded "000000" 改为通过 backend test profile 的
DebugController 实时拉取真实 6 位验证码,完整覆盖 sendEmailCode
链路(JavaMailSender → GreenMail → DebugController → E2E)。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 10: verify_reset_password.py E2E 适配

**Files:**
- Modify: `tests/e2e/verify_reset_password.py`

**Consumes:** Task 6 + Task 9（API 接口已存在）+ UI 走 forgot 页面输入 6 位码

**Produces:** E2E 走完整 UI 流程,验证码从 `/__test/email-code` 拉真实值

- [ ] **Step 1: 把 EMAIL_CODE_MOCK 由 hardcode "000000" 改成 fetch**

打开 `tests/e2e/verify_reset_password.py`,把 `EMAIL_CODE_MOCK = "000000"` 改成:

```python
EMAIL_CODE_MOCK = None
```

然后找到 UI 输入码的 `page.fill("input[placeholder='输入 6 位验证码']:visible", EMAIL_CODE_MOCK)` 之前的几行,加一个 polling 等待 + 拉码:

```python
# 等 GreenMail 收到邮件 + DebugController 可读 + 输入框可见
real_code = None
for _ in range(20):
    resp = requests.get(f"{API_URL.replace('/api/v1/user','')}/__test/email-code",
                        params={"email": email}, timeout=5)
    if resp.json().get("found"):
        real_code = resp.json()["code"]
        break
    page.wait_for_timeout(300)
assert real_code, f"未能从 /__test/email-code 拿到 email={email} 的验证码"

page.fill("input[placeholder='输入 6 位验证码']:visible", real_code)
```

放在第 143 行 `page.fill("input[placeholder='输入 6 位验证码']:visible", EMAIL_CODE_MOCK)` 之前。

- [ ] **Step 2: 跑 E2E**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_reset_password.py 2>&1 | tail -30
```

Expected:11 个 PASS 全部通过。

- [ ] **Step 3: commit**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add tests/e2e/verify_reset_password.py
git commit -m "$(cat <<'EOF'
test(e2e): verify_reset_password 改走 /__test/email-code 拿真实验证码

与 verify_user_auth 一致,UI 输入的验证码改为实时从 DebugController 拉取,
覆盖 sendEmailCode 真实发送链路。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 11: 全量 verification

**Files:** 无新增/修改（仅跑测试 + 检查状态）

**Consumes:** 所有前置 task

**Produces:** 全量测试 PASS 的状态 + 一次总览 commit（如有遗留）

- [ ] **Step 1: 跑全部 backend 单测**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api test -DfailIfNoTests=false 2>&1 | grep -E "Tests run|BUILD" | tail -20
```

Expected：所有测试 PASS，包括之前的 `AuthServiceLoginTest` / `AuthServiceRefreshAfterResetTest` / `AuthServiceResetPasswordTest` / `EmailCodeServiceTest` 等无回归。

- [ ] **Step 2: 跑两个 E2E**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_user_auth.py 2>&1 | tail -15
python3 tests/e2e/verify_reset_password.py 2>&1 | tail -15
```

Expected：两个 E2E 都全 PASS。

- [ ] **Step 3: prod profile 验证 `DebugController` 不注册**

```bash
# 杀掉 test profile 的 backend
pkill -f 'spring-boot:run.*user/api' 2>/dev/null
pkill -f 'UserApiApplication' 2>/dev/null
sleep 2

cd /Users/panyong/aio_project/ai_chuangzuo/project
nohup mvn -pl user/api spring-boot:run -Dspring-boot.run.profiles=prod \
  > /tmp/user-api-prod.log 2>&1 &
sleep 15  # 等启动

# DebugController 应该返回 404（路径不存在），证明 prod profile 下未注册
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:25050/__test/email-code?email=any@example.com")
echo "prod /__test/email-code status = $STATUS"

if [ "$STATUS" = "404" ]; then
  echo "✓ DebugController 在 prod profile 未注册"
else
  echo "✗ FAIL: DebugController 在 prod profile 仍然可见"
  exit 1
fi
```

注意：prod profile 启动会因为 `JASYPT_ENCRYPTOR_PASSWORD` 没注入而报错（密码解密失败）。**这是预期行为**，且 `DebugController` 不会被注册。完整 prod 启动连通测试需要用户线下配 master key 后单独跑，不在本任务范围。

- [ ] **Step 4: 恢复 test profile backend 供后续开发**

```bash
pkill -f 'spring-boot:run.*user/api' 2>/dev/null
pkill -f 'UserApiApplication' 2>/dev/null
sleep 2

cd /Users/panyong/aio_project/ai_chuangzuo/project
nohup mvn -pl user/api spring-boot:run -Dspring-boot.run.profiles=test \
  > /tmp/user-api-test.log 2>&1 &
echo "test profile backend 重启中..."
for i in $(seq 1 60); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:25050/api/v1/user/auth/captcha)
  if [ "$STATUS" = "200" ]; then
    echo "ready in ${i}s"; break
  fi
  sleep 1
done
```

- [ ] **Step 5: 更新 progress.md**

打开 `.superpowers/sdd/progress.md`,在最新一条记录下加一段：

```markdown
## 真实邮箱验证码发送 (2026-07-05)

**Plan**: docs/superpowers/plans/2026-07-05-real-email-sending.md

完成情况：11 个 task 全 pass。
- pom +3 dep, yml 迁移到 spring.mail.* (mock 全删)
- EmailCodeServiceImpl 真实发送,失败抛 EMAIL_SEND_FAILED(111014)
- EmailMessageFactory 工厂构造 multipart
- GreenMail @TestConfig + 改写单测
- E2E 改走 /__test/email-code 取真实验证码

**未完成 / 待用户线下完成**：
- 本地生成 `ENC(126 授权码)` 密文替换 `application-prod.yml` 的占位（用户在 master key 轮换时一起完成）
- 启动 prod profile 跑一次连通测试验证 126 真实发邮件
```

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add .superpowers/sdd/progress.md
git commit -m "$(cat <<'EOF'
docs(sdd): 记录真实邮箱发送功能实施完成

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review 速查

**Spec coverage**: 上述 task 覆盖 spec 节 2 文件改动表的所有 11 个文件;节 3-7 全部映射到 task 4-8;E2E 适配覆盖 spec 节 6 测试矩阵。

**Placeholder scan**: 无 TBD/TODO/fill in。每个 step 的代码块都是可粘贴执行的完整代码。

**Type consistency**:
- `EmailCodeService` 接口签名不变（task 5）
- `EmailMessageFactory.populateCodeEmail(msg, from, toEmail, code)` 在 task 4 定义,task 5 引用,两者签名一致
- `EMAIL_CODE_PREFIX = "user:auth:email-code:"` 在 EmailCodeServiceImpl 与 DebugController 一致
- 错误码 `EMAIL_SEND_FAILED(111014)` 在 task 3 定义,task 5 引用,task 8 单测断言

**Scope**: 单一 plan,完整覆盖 spec。9-10 的两个 E2E 任务独立 review 合理。
