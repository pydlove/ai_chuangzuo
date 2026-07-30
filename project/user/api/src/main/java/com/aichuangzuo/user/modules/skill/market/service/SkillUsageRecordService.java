package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 提示词使用统一计数服务。
 *
 * <p>按 skillRef 判定来源：市场提示词产生收益，个人提示词只计 use_count，系统预设不计。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillUsageRecordService {

    private static final int SOURCE_TYPE_SYSTEM_PRESET = 3;

    private final SkillMarketMapper skillMarketMapper;
    private final UserSkillMapper userSkillMapper;
    private final SkillMarketUsageService skillMarketUsageService;
    private final UserSkillService userSkillService;

    /**
     * 记录一次提示词使用。
     *
     * @param skillRef 提示词标识：市场为 biz_no，个人/系统预设为 skill_name
     * @param userId   使用者用户ID
     */
    public void record(String skillRef, Long userId) {
        if (skillRef == null || skillRef.isBlank() || userId == null) {
            return;
        }

        // 1. 优先判定市场提示词（biz_no 匹配）
        SkillMarket market = skillMarketMapper.selectOne(
                new LambdaQueryWrapper<SkillMarket>()
                        .eq(SkillMarket::getBizNo, skillRef)
                        .eq(SkillMarket::getAuditStatus, 1)
                        .eq(SkillMarket::getEnableStatus, 1)
                        .eq(SkillMarket::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (market != null) {
            skillMarketUsageService.recordUsage(skillRef, userId);
            return;
        }

        // 2. 再判定个人提示词（用户自己的自定义或学习风格）
        UserSkill userSkill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillName, skillRef)
                        .eq(UserSkill::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (userSkill != null) {
            if (SOURCE_TYPE_SYSTEM_PRESET == userSkill.getSourceType()) {
                log.info("系统预设风格不计数 userId={} skillName={}", userId, skillRef);
                return;
            }
            userSkillService.incrementUseCount(userId, skillRef);
            return;
        }

        log.warn("未找到对应提示词来源，不计数 userId={} skillRef={}", userId, skillRef);
    }
}
