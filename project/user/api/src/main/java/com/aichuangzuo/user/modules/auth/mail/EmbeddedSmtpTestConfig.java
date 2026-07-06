package com.aichuangzuo.user.modules.auth.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Properties;

/**
 * 仅 test profile 启用的嵌入式 SMTP，让 JavaMailSender(127.0.0.1:3025)
 * → GreenMail 全闭环。生产环境 @Profile("test") 不激活，不会启动。
 *
 * 和测试用的 GreenMailTestConfig 整合：测试场景仍可通过
 * @Import(EmbeddedSmtpTestConfig.class) 在 surefire 自己的 JVM 里复用 bean。
 *
 * 凭据 test@local / local-dev-no-auth 与 application-test.yml 的
 * mail.smtp.auth + username/password 配对，握手 235 才能进队列。
 */
@Configuration
@Profile("test")
public class EmbeddedSmtpTestConfig {

    private GreenMail greenMail;

    @Bean
    public GreenMail embeddedSmtpServer() {
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
