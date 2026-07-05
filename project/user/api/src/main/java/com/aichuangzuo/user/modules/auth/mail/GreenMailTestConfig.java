package com.aichuangzuo.user.modules.auth.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Properties;

/**
 * 仅在 test profile 下激活：启动一个监听 127.0.0.1:3025 的 in-memory SMTP server，
 * 让邮件链路 JavaMailSender → SMTP → GreenMail 真正闭环。
 *
 * 两类使用场景：
 *  1. JUnit @SpringBootTest：测试类用 @Import(GreenMailTestConfig.class) 拉进来；
 *  2. spring-boot:run 启动后端 (E2E)：test profile 自动激活此 @Configuration，
 *     进程里内嵌 GreenMail，前端 /__test/email-code 端点能拿到真实 6 位验证码。
 *
 * application-test.yml 启用了 mail.smtp.auth=true 并配 test@local / local-dev-no-auth，
 * 这里把同样的凭据通过 setUsers 注册到 GreenMail，握手 235 成功，邮件才进 GreenMail 队列。
 *
 * 上下文关闭时 @PreDestroy 自动 stop。
 */
@Configuration
@Profile("test")
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