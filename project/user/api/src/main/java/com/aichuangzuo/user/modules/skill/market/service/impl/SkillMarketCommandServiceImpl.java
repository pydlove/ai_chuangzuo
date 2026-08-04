package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketCommandService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户端 - 风格市场写操作服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMarketCommandServiceImpl implements SkillMarketCommandService {

    private static final String BENEFIT_CODE_SKILL_MARKET_PUBLISH = "skill_market_publish";

    private final SkillMarketMapper skillMarketMapper;
    private final BenefitService benefitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnMarketSkill(String bizNo, Long userId) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, bizNo)
                .eq(SkillMarket::getIsDeleted, 0);
        SkillMarket market = skillMarketMapper.selectOne(wrapper);
        if (market == null) {
            throw new BusinessException(SkillErrorCode.SKILL_NOT_FOUND);
        }
        if (!market.getPublisherUserId().equals(userId)) {
            throw new BusinessException(SkillErrorCode.SKILL_MARKET_NOT_OWNER);
        }
        market.setIsDeleted(1);
        skillMarketMapper.updateById(market);
        benefitService.refund(userId, BENEFIT_CODE_SKILL_MARKET_PUBLISH);
        log.info("用户下架市场 skill userId={}, bizNo={}", userId, bizNo);
    }
}
