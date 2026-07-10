package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创作提交限流：每用户每分钟最多 N 次（Caffeine 进程内计数）。
 *
 * <p>计数器按用户 key，1 分钟过期；超阈值抛 RATE_LIMIT。
 */
@Slf4j
@Service
public class GenerationRateLimiter {

    private final Cache<Long, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    /** 自增计数；超 quota 直接抛业务异常。 */
    public void check(Long userId, int quotaPerMinute) {
        AtomicInteger c = counters.get(userId, k -> new AtomicInteger(0));
        int now = c.incrementAndGet();
        if (now > quotaPerMinute) {
            log.info("user={} 限流触发 {}/min", userId, quotaPerMinute);
            throw new BusinessException(UserGenerationErrorCode.GENERATION_RATE_LIMIT);
        }
    }
}
