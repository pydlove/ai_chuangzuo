package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创作提交限流：每用户每分钟最多 N 次（Caffeine 进程内计数）。
 *
 * <p>计数器按用户 key，1 分钟过期；超阈值抛 RATE_LIMIT，并提示剩余等待秒数。
 */
@Slf4j
@Service
public class GenerationRateLimiter {

    private final Cache<Long, Counter> counters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    /** 自增计数；超 quota 直接抛业务异常。 */
    public void check(Long userId, int quotaPerMinute) {
        Counter counter = counters.get(userId, k -> new Counter(Instant.now()));
        int now = counter.count.incrementAndGet();
        if (now > quotaPerMinute) {
            long remainingSeconds = Duration.between(Instant.now(), counter.windowStart.plusSeconds(60)).getSeconds();
            if (remainingSeconds < 1) {
                remainingSeconds = 1;
            }
            log.info("user={} 限流触发 {}/min, remainingSeconds={}", userId, quotaPerMinute, remainingSeconds);
            String msg = String.format("操作过于频繁，请 %d 秒后再试", remainingSeconds);
            throw new BusinessException(UserGenerationErrorCode.GENERATION_RATE_LIMIT.getCode(), msg);
        }
    }

    private static class Counter {
        private final Instant windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        Counter(Instant windowStart) {
            this.windowStart = windowStart;
        }
    }
}
