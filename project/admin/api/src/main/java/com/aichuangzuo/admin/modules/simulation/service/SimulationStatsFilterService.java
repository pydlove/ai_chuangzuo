package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.simulation.entity.SimulationStatsFilter;
import com.aichuangzuo.admin.modules.simulation.mapper.SimulationStatsFilterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 模拟运营-统计数据机器人过滤开关。
 *
 * <p>单行配置（id=1）：includeRobots=1 统计包含机器人；0 时概览/订单统计排除 a_simulation_robot 中的用户。
 * 读取走 10s 缓存，更新即失效，统计侧 60s 缓存自然过期后生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationStatsFilterService {

    private static final long CONFIG_ID = 1L;

    private final SimulationStatsFilterMapper mapper;

    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(1)
            .build();

    /** 统计是否包含机器人数据（默认 true，不排除）。 */
    public boolean includeRobots() {
        return cache.get("includeRobots", k -> loadIncludeRobots());
    }

    /** 统计查询是否需排除机器人：返回 true 时 SQL 需附加过滤条件。 */
    public boolean excludeRobots() {
        return !includeRobots();
    }

    public void update(boolean includeRobots) {
        SimulationStatsFilter config = new SimulationStatsFilter();
        config.setId(CONFIG_ID);
        config.setIncludeRobots(includeRobots ? 1 : 0);
        mapper.updateById(config);
        cache.invalidateAll();
        log.info("模拟运营统计过滤开关更新 includeRobots={}", includeRobots);
    }

    private boolean loadIncludeRobots() {
        SimulationStatsFilter config = mapper.selectOne(
                new LambdaQueryWrapper<SimulationStatsFilter>().eq(SimulationStatsFilter::getId, CONFIG_ID));
        return config == null || config.getIncludeRobots() == null || config.getIncludeRobots() == 1;
    }
}
