package com.aichuangzuo.user.modules.auth.mail;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 仅当 smtp.test-server.enabled=true 时激活的内嵌 SMTP 服务器。
 *
 * 用途：spring-boot:run 跑 E2E 时，让 JavaMailSender 发的真实邮件能被一个
 * 本进程内的 ServerSocket 接住并丢弃。这样 DebugController 从 Caffeine 缓存
 * 拿验证码，链路就完整了：
 *   浏览器 → /api/v1/user/auth/email-codes → EmailCodeServiceImpl →
 *   JavaMailSender → SMTP(127.0.0.1:3025) → 这个简单 SMTP 服务器（丢弃）→
 *   Caffeine 缓存里有 6 位码 → /__test/email-code 端点返回给 E2E。
 *
 * 为什么不用 GreenMail？GreenMail 留在 test scope，JUnit 单测按需 @Import；
 * 这里必须 main classpath 才能在 E2E 跑得起来，但又不想为此把 GreenMail
 * 提升到 compile 把生产 JAR 撑大。30 行 ServerSocket 实现足够覆盖 E2E：
 * 220 握手 / 250 EHLO / 235 AUTH(测试凭据 test@local / local-dev-no-auth) /
 * 250 MAIL/RCPT / 354 DATA / 250 完成 / 221 QUIT。
 *
 * 为什么用 @ConditionalOnProperty 而非 @Profile("test")？
 * JUnit @SpringBootTest + @ActiveProfiles("test") 也激活 test profile，
 * 但单测场景下要的是 GreenMail（能 getReceivedMessages 校验邮件内容），
 * 不是这个 throwaway ServerSocket。生产环境绝不可能设这个 property →
 * 默认 matchIfMissing=false → 不会启动。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "smtp.test-server.enabled", havingValue = "true")
public class SmtpTestServerConfig {

    private static final int SMTP_PORT = 3025;

    private ServerSocket serverSocket;
    private Thread acceptor;
    private volatile boolean running = true;

    @PostConstruct
    public void start() throws IOException {
        serverSocket = new ServerSocket(SMTP_PORT, 50, InetAddress.getByName("127.0.0.1"));
        acceptor = new Thread(this::acceptLoop, "smtp-test-server");
        acceptor.setDaemon(true);
        acceptor.start();
        log.info("E2E 嵌入式 SMTP 监听 127.0.0.1:{}", SMTP_PORT);
    }

    @PreDestroy
    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                new Thread(new SmtpSession(client), "smtp-session").start();
            } catch (IOException ex) {
                if (running) {
                    log.warn("SMTP accept 失败: {}", ex.getMessage());
                }
            }
        }
    }

    private static class SmtpSession implements Runnable {
        private final Socket socket;

        SmtpSession(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                out.println("220 smtp.test.local ESMTP ready");

                String line;
                while ((line = in.readLine()) != null) {
                    String upper = line.toUpperCase();
                    if (upper.startsWith("EHLO") || upper.startsWith("HELO")) {
                        out.println("250-smtp.test.local");
                        out.println("250 AUTH PLAIN LOGIN");
                    } else if (upper.startsWith("AUTH")) {
                        out.println("235 2.7.0 Authentication successful");
                    } else if (upper.startsWith("MAIL FROM")) {
                        out.println("250 2.1.0 Sender OK");
                    } else if (upper.startsWith("RCPT TO")) {
                        out.println("250 2.1.5 Recipient OK");
                    } else if (upper.startsWith("DATA")) {
                        out.println("354 End data with <CR><LF>.<CR><LF>");
                        while ((line = in.readLine()) != null) {
                            if (line.equals(".")) {
                                out.println("250 2.0.0 OK: queued");
                                break;
                            }
                        }
                    } else if (upper.startsWith("QUIT")) {
                        out.println("221 2.0.0 Bye");
                        break;
                    } else if (upper.startsWith("RSET") || upper.startsWith("NOOP")) {
                        out.println("250 2.0.0 OK");
                    } else {
                        out.println("250 2.0.0 OK");
                    }
                }
            } catch (IOException ignored) {
                // 客户端断开，正常结束
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}