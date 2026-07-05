package com.aichuangzuo.user.modules.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 DebugController(@Profile("test")) 在 test profile 下注册,
 * 供 E2E 通过 /__test/email-code 拿验证码。
 */
@SpringBootTest
@ActiveProfiles("test")
class DebugControllerTestProfileTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void debugControllerShouldBeRegisteredInTestProfile() {
        assertNotNull(ctx.getBeanProvider(DebugController.class).getIfAvailable(),
                "DebugController 必须在 test profile 下注册");
    }
}