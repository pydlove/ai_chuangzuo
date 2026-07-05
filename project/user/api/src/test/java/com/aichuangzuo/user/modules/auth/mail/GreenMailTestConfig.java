package com.aichuangzuo.user.modules.auth.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 在 @SpringBootTest 类里用 @Import(GreenMailTestConfig.class) 启用。
 * 启动一个监听 127.0.0.1:3025 的 in-memory SMTP server，
 * 让邮件链路 JavaMailSender → SMTP → GreenMail 真正闭环。
 *
 * 注：greenmail-spring 2.0.1 没有 JUnit 5 extension，只有纯 Java GreenMail。
 *     上下文关闭时 @PreDestroy 自动 stop。
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

    @PreDestroy
    public void stop() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }
}
