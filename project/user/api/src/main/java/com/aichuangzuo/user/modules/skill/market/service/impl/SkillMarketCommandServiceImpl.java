package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.skill.enums.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketCommandService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户端 - 风格市场写操作服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMarketCommandServiceImpl implements SkillMarketCommandService {

    private static final String BENEFIT_CODE_SKILL_MARKET_PUBLISH = "skill_market_publish";

    private final SkillMarketMapper skillMarketMapper;
    private final UserSkillMapper userSkillMapper;
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
        skillMarketMapper.deleteById(market.getId());

        // 同步重置 u_user_skill 的审核状态，避免前端刷新后仍从用户 skill 表读到“已通过”
        UserSkill userSkill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getBizNo, bizNo)
                        .eq(UserSkill::getUserId, userId));
        if (userSkill != null) {
            userSkill.setAuditStatus(0);
            userSkill.setUpdatedAt(LocalDateTime.now());
            userSkillMapper.updateById(userSkill);
        }

        benefitService.refund(userId, BENEFIT_CODE_SKILL_MARKET_PUBLISH);
        log.info("用户下架市场 skill userId={}, bizNo={}", userId, bizNo);
    }
}
