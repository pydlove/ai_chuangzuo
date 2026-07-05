package com.aichuangzuo.user.modules.auth.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

/**
 * JUnit 单测用 @TestConfiguration：测试类用 @Import(GreenMailTestConfig.class) 拉进来，
 * 启动一个监听 127.0.0.1:3025 的 in-memory SMTP server，让邮件链路
 * JavaMailSender → SMTP → GreenMail 真正闭环。
 *
 * 注：greenmail-spring 2.0.1 没有 JUnit 5 extension，只有纯 Java GreenMail。
 *     上下文关闭时 @PreDestroy 自动 stop。
 *
 *     测试 SMTP 会话走真实的 AUTH 流程：application-test.yml 启用了
 *     mail.smtp.auth=true 并配了 test@local / local-dev-no-auth 凭据；
 *     这里把同样的凭据通过 setUsers 注册到 GreenMail，握手 235 成功，
 *     邮件正文才能顺利进 GreenMail 的接收队列。
 *
 * 和 src/main 的 SmtpTestServerConfig 不冲突：JUnit 单测在 surefire 自己的 JVM 里跑，
 * E2E 在 spring-boot:run 的进程里跑，两者不会同时启动。GreenMail 留 test scope
 * 也能避免 JAR 撑大。
 */
@TestConfiguration
public class GreenMailTestConfig {

    private GreenMail greenMail;

    @Bean
    public GreenMail greenMail() {
        ServerSetup setup = new ServerSetup(3025, "127.0.0.1", "smtp");
        greenMail = new GreenMail(setup);
        Properties users = new Properties();
        users.setProperty("test@local", "local-dev-no-auth");
        greenMail.setUsers(users);
        greenMail.start();
        return greenMail;
    }

    @PreDestroy
    public void stop() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }
}