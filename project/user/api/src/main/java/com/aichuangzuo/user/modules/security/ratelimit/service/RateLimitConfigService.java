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
    private static final int DEFAULT_NICKNAME_CHECK_DAILY_LIMIT = 10;

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

    /**
     * 平台账号检测每日次数上限，默认 10。
     */
    @Cacheable(cacheNames = "rateLimitConfig", key = "'nickname-check-daily-limit'")
    public int getNicknameCheckDailyLimit() {
        RateLimitConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null || config.getNicknameCheckDailyLimit() == null) {
            return DEFAULT_NICKNAME_CHECK_DAILY_LIMIT;
        }
        return config.getNicknameCheckDailyLimit();
    }
}
