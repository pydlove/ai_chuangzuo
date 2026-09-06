package com.aichuangzuo.user.modules.activity.service;

import com.aichuangzuo.user.modules.activity.mapper.UserActivityMapper;
import com.aichuangzuo.user.modules.activity.mapper.UserDailyActiveMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户活跃打点：每用户 60 秒节流，upsert 活跃时间 + 幂等记录日活。
 *
 * <p>打点失败只记 warn，绝不影响主请求。
 */
@Slf4j
@Service
public class UserActivityService {

    /** 同一用户 60 秒内重复请求只写一次库 */
    private static final Duration THROTTLE_TTL = Duration.ofSeconds(60);

    private final Cache<Long, Boolean> throttle = Caffeine.newBuilder()
            .expireAfterWrite(THROTTLE_TTL)
            .maximumSize(100_000)
            .build();

    private final UserActivityMapper userActivityMapper;
    private final UserDailyActiveMapper userDailyActiveMapper;

    public UserActivityService(UserActivityMapper userActivityMapper,
                               UserDailyActiveMapper userDailyActiveMapper) {
        this.userActivityMapper = userActivityMapper;
        this.userDailyActiveMapper = userDailyActiveMapper;
    }

    public void track(Long userId) {
        if (userId == null || throttle.getIfPresent(userId) != null) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            userActivityMapper.upsert(userId, now);
            userDailyActiveMapper.insertIgnore(userId, LocalDate.now());
            throttle.put(userId, Boolean.TRUE);
        } catch (Exception e) {
            log.warn("用户活跃打点失败 userId={}: {}", userId, e.getMessage());
        }
    }
}
