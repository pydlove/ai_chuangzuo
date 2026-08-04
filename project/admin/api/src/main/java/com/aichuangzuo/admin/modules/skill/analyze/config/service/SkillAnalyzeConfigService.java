package com.aichuangzuo.admin.modules.skill.analyze.config.service;

import com.aichuangzuo.admin.modules.skill.analyze.config.dto.request.SkillAnalyzeConfigUpdateRequest;
import com.aichuangzuo.admin.modules.skill.analyze.config.entity.SkillAnalyzeConfig;
import com.aichuangzuo.admin.modules.skill.analyze.config.mapper.SkillAnalyzeConfigMapper;
import com.aichuangzuo.admin.modules.skill.analyze.config.vo.SkillAnalyzeConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 提示词分析安全配置服务。
 *
 * <p>单行配置（id=1），admin 端 GET/PUT 维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAnalyzeConfigService {

    private static final long CONFIG_ID = 1L;
    private static final int DEFAULT_DAILY_LIMIT = 5;

    private final SkillAnalyzeConfigMapper mapper;

    public SkillAnalyzeConfigVO detail() {
        SkillAnalyzeConfig config = mapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new SkillAnalyzeConfig();
            config.setId(CONFIG_ID);
            config.setDailyAttemptLimit(DEFAULT_DAILY_LIMIT);
        }
        return toVo(config);
    }

    @Transactional
    public SkillAnalyzeConfigVO update(SkillAnalyzeConfigUpdateRequest req, Long adminUserId) {
        SkillAnalyzeConfig exist = mapper.selectById(CONFIG_ID);
        boolean isNew = exist == null;
        if (isNew) {
            exist = new SkillAnalyzeConfig();
            exist.setId(CONFIG_ID);
        }
        exist.setDailyAttemptLimit(req.getDailyAttemptLimit());
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        if (isNew) {
            mapper.insert(exist);
        } else {
            mapper.updateById(exist);
        }

        log.info("admin={} 更新 AI 提示词分析安全配置 dailyAttemptLimit={}", adminUserId, exist.getDailyAttemptLimit());
        return toVo(exist);
    }

    private SkillAnalyzeConfigVO toVo(SkillAnalyzeConfig c) {
        SkillAnalyzeConfigVO vo = new SkillAnalyzeConfigVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
