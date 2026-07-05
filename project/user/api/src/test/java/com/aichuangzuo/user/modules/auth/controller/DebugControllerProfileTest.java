package com.aichuangzuo.user.modules.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 DebugController(@Profile("test")) 只在 test profile 下注册。
 * default profile 启动跑 JUnit 时 DebugController 不在容器里，
 * 不会暴露 /__test/email-code 给生产或未知环境。
 *
 * 不能直接用 @ActiveProfiles("prod") 验证(prod 需要真实的
 * JASYPT master key 才能解密 application-prod.yml 里的 ENC(...) 密码)。
 * @Profile("test") 是 Spring 标准行为,默认 profile 不匹配即不注册,
 * 这里跑一次确认 context loader 实际行为与注解一致。
 */
@SpringBootTest
@ActiveProfiles("default")
class DebugControllerProfileTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void debugControllerShouldNotBeRegisteredInDefaultProfile() {
        org.junit.jupiter.api.Assertions.assertNull(
                ctx.getBeanProvider(DebugController.class).getIfAvailable(),
                "DebugController 必须在非 test profile 下不注册,否则会泄露到生产");
    }
}