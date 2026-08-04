package com.aichuangzuo.user.modules.skill.analyze.config.service;

import com.aichuangzuo.user.modules.skill.analyze.config.entity.SkillAnalyzeConfig;
import com.aichuangzuo.user.modules.skill.analyze.config.mapper.SkillAnalyzeConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * AI 提示词分析安全配置读取服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAnalyzeConfigService {

    private static final long CONFIG_ID = 1L;
    private static final int DEFAULT_DAILY_LIMIT = 5;

    private final SkillAnalyzeConfigMapper configMapper;

    /**
     * 获取每日分析次数上限，默认 5 次。
     */
    @Cacheable(cacheNames = "skillAnalyzeConfig", key = "'single'")
    public int getDailyAttemptLimit() {
        SkillAnalyzeConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null || config.getDailyAttemptLimit() == null || config.getDailyAttemptLimit() <= 0) {
            return DEFAULT_DAILY_LIMIT;
        }
        return config.getDailyAttemptLimit();
    }
}
