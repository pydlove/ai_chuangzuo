package com.aichuangzuo.admin.modules.stats.service;

import com.aichuangzuo.admin.modules.stats.mapper.UserActivityStatsMapper;
import com.aichuangzuo.admin.modules.stats.vo.StatsOverviewVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class StatsService {

    /** 在线窗口：最近 5 分钟有打点即视为在线 */
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final Cache<String, Long> onlineCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(10)
            .build();

    private final Cache<String, Long> dailyCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10)
            .build();

    private final UserActivityStatsMapper userActivityStatsMapper;

    public StatsService(UserActivityStatsMapper userActivityStatsMapper) {
        this.userActivityStatsMapper = userActivityStatsMapper;
    }

    public StatsOverviewVO overview() {
        long online = onlineCache.get("online", k ->
                userActivityStatsMapper.countOnline(LocalDateTime.now().minus(ONLINE_WINDOW)));
        long today = dailyCache.get("today", k ->
                userActivityStatsMapper.countDailyActive(LocalDate.now()));
        return new StatsOverviewVO(online, today);
    }
}
