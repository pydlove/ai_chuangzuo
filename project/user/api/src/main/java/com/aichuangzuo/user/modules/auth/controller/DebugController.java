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
 *      从这里读取缓存里的 6 位验证码，模拟「用户在邮箱里看到码」的场景。
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
    public Map<String, Object> emailCode(@RequestParam("email") String email) {
        String code = cacheUtil.get(EMAIL_CODE_PREFIX + email);
        if (code == null) {
            return Map.of("found", false, "email", email);
        }
        return Map.of("found", true, "email", email, "code", code);
    }
}
