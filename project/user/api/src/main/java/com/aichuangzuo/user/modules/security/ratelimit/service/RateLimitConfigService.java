package com.aichuangzuo.user.modules.security.ratelimit.service;

import com.aichuangzuo.user.modules.security.ratelimit.entity.RateLimitConfig;
import com.aichuangzuo.user.modules.security.ratelimit.mapper.RateLimitConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 登录限流配置读取服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitConfigService {

    private static final long CONFIG_ID = 1L;
    private static final int DEFAULT_ENABLED = 1;

    private final RateLimitConfigMapper configMapper;

    /**
     * 是否启用登录限流，默认启用。
     */
    @Cacheable(cacheNames = "rateLimitConfig", key = "'single'")
    public boolean isLoginRateLimitEnabled() {
        RateLimitConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null || config.getIsLoginRateLimitEnabled() == null) {
            return DEFAULT_ENABLED == 1;
        }
        return config.getIsLoginRateLimitEnabled() == 1;
    }
}
