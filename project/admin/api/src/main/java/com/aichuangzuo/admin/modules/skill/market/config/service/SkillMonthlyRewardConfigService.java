package com.aichuangzuo.admin.modules.skill.market.config.service;

import com.aichuangzuo.admin.modules.skill.market.config.dto.request.SkillMonthlyRewardConfigUpdateRequest;
import com.aichuangzuo.admin.modules.skill.market.config.entity.SkillMonthlyRewardConfig;
import com.aichuangzuo.admin.modules.skill.market.config.mapper.SkillMonthlyRewardConfigMapper;
import com.aichuangzuo.admin.modules.skill.market.config.vo.SkillMonthlyRewardConfigVO;
import com.aichuangzuo.admin.modules.skill.market.enums.AdminSkillMarketErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 提示词市场月度排行榜奖励配置服务。
 *
 * <p>单行配置（id=1），admin 端 GET/PUT 维护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMonthlyRewardConfigService {

    private static final long CONFIG_ID = 1L;

    private final SkillMonthlyRewardConfigMapper mapper;

    public SkillMonthlyRewardConfigVO detail() {
        return toVo(requireById(CONFIG_ID));
    }

    @Transactional
    public SkillMonthlyRewardConfigVO update(SkillMonthlyRewardConfigUpdateRequest req, Long adminUserId) {
        SkillMonthlyRewardConfig exist = requireById(CONFIG_ID);
        exist.setFirstAmount(req.getFirstAmount());
        exist.setSecondAmount(req.getSecondAmount());
        exist.setThirdAmount(req.getThirdAmount());
        exist.setFourthAmount(req.getFourthAmount());
        exist.setFifthAmount(req.getFifthAmount());
        exist.setSettlementCron(req.getSettlementCron());
        exist.setEnabled(req.getEnabled());
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(exist);

        log.info("admin={} 更新提示词市场月度奖励配置 first={} second={} third={} fourth={} fifth={} cron={} enabled={}",
                adminUserId, exist.getFirstAmount(), exist.getSecondAmount(), exist.getThirdAmount(),
                exist.getFourthAmount(), exist.getFifthAmount(), exist.getSettlementCron(), exist.getEnabled());
        return toVo(exist);
    }

    public BigDecimal getPricePerUse() {
        SkillMonthlyRewardConfig config = mapper.selectById(CONFIG_ID);
        if (config == null || config.getPricePerUse() == null) {
            return new BigDecimal("2.00");
        }
        return config.getPricePerUse().compareTo(BigDecimal.ZERO) > 0 ? config.getPricePerUse() : new BigDecimal("2.00");
    }

    @Transactional
    public BigDecimal updatePricePerUse(BigDecimal pricePerUse, Long adminUserId) {
        if (pricePerUse == null || pricePerUse.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(AdminSkillMarketErrorCode.PRICE_INVALID);
        }
        SkillMonthlyRewardConfig exist = requireById(CONFIG_ID);
        exist.setPricePerUse(pricePerUse);
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(exist);

        log.info("admin={} 更新提示词市场单次收益单价 pricePerUse={}", adminUserId, pricePerUse);
        return exist.getPricePerUse();
    }

    /**
     * 取当前生效配置（供 user-api 月结 job 通过内部接口读取）。
     */
    public SkillMonthlyRewardConfig getCurrent() {
        return requireById(CONFIG_ID);
    }

    private SkillMonthlyRewardConfig requireById(Long id) {
        SkillMonthlyRewardConfig c = mapper.selectById(id);
        if (c == null) {
            throw new BusinessException(AdminSkillMarketErrorCode.SKILL_MONTHLY_REWARD_CONFIG_NOT_FOUND);
        }
        return c;
    }

    private SkillMonthlyRewardConfigVO toVo(SkillMonthlyRewardConfig c) {
        SkillMonthlyRewardConfigVO vo = new SkillMonthlyRewardConfigVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
