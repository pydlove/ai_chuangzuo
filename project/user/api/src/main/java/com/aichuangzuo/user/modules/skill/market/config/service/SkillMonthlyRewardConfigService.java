package com.aichuangzuo.user.modules.skill.market.config.service;

import com.aichuangzuo.user.modules.skill.market.config.entity.SkillMonthlyRewardConfig;
import com.aichuangzuo.user.modules.skill.market.config.mapper.SkillMonthlyRewardConfigMapper;
import com.aichuangzuo.user.modules.skill.market.config.vo.SkillMonthlyRewardConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 提示词市场月度排行榜奖励配置查询服务。
 */
@Service
@RequiredArgsConstructor
public class SkillMonthlyRewardConfigService {

    private static final Long CONFIG_ID = 1L;

    private final SkillMonthlyRewardConfigMapper configMapper;

    /**
     * 获取当前启用的月度奖励配置。
     *
     * @return 配置信息；未启用或不存在时返回 null
     */
    public SkillMonthlyRewardConfigVO getEnabledConfig() {
        SkillMonthlyRewardConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            return null;
        }

        SkillMonthlyRewardConfigVO vo = new SkillMonthlyRewardConfigVO();
        vo.setFirstAmount(nonNegative(config.getFirstAmount()));
        vo.setSecondAmount(nonNegative(config.getSecondAmount()));
        vo.setThirdAmount(nonNegative(config.getThirdAmount()));
        vo.setFourthAmount(nonNegative(config.getFourthAmount()));
        vo.setFifthAmount(nonNegative(config.getFifthAmount()));
        vo.setEnabled(config.getEnabled());
        vo.setPricePerUse(config.getPricePerUse() != null && config.getPricePerUse().compareTo(BigDecimal.ZERO) > 0
                ? config.getPricePerUse() : new BigDecimal("2.00"));
        return vo;
    }

    public BigDecimal getPricePerUse() {
        SkillMonthlyRewardConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null || config.getPricePerUse() == null
                || config.getPricePerUse().compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("2.00");
        }
        return config.getPricePerUse();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
