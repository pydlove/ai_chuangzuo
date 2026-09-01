package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.security.ratelimit.service.RateLimitConfigService;
import com.aichuangzuo.shared.enums.error.SelfMediaPlanErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 平台账号检测（昵称检测/推荐）每日次数限制器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameCheckRateLimiter {

    private static final String CACHE_KEY_PREFIX = "user:nickname-check:daily:";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CacheUtil cacheUtil;
    private final RateLimitConfigService rateLimitConfigService;

    /**
     * 检查并递增当日次数。超过上限时抛出业务异常。
     */
    public void checkAndIncrement(Long userId) {
        int limit = rateLimitConfigService.getNicknameCheckDailyLimit();
        String key = buildKey(userId);
        AtomicInteger counter = cacheUtil.get(key);
        if (counter == null) {
            counter = new AtomicInteger(0);
            long secondsUntilMidnight = secondsUntilMidnight();
            cacheUtil.set(key, counter, secondsUntilMidnight, TimeUnit.SECONDS);
        }
        int count = counter.incrementAndGet();
        log.info("账号检测次数校验, userId={}, count={}, limit={}", userId, count, limit);
        if (count > limit) {
            log.warn("账号检测日限次超限, userId={}, count={}, limit={}", userId, count, limit);
            throw new BusinessException(SelfMediaPlanErrorCode.NICKNAME_CHECK_DAILY_LIMIT_EXCEEDED);
        }
    }

    private String buildKey(Long userId) {
        return CACHE_KEY_PREFIX + LocalDate.now(ZONE) + ":" + userId;
    }

    private long secondsUntilMidnight() {
        LocalDate today = LocalDate.now(ZONE);
        long nowMillis = System.currentTimeMillis();
        long midnightMillis = today.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
        return Math.max(1L, (midnightMillis - nowMillis) / 1000);
    }
}
