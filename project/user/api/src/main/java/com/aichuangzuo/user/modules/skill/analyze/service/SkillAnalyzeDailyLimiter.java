package com.aichuangzuo.user.modules.skill.analyze.service;

import com.aichuangzuo.user.modules.skill.analyze.entity.SkillAnalyzeDaily;
import com.aichuangzuo.user.modules.skill.analyze.mapper.SkillAnalyzeDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 用户 AI 提示词分析日次数限制器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAnalyzeDailyLimiter {

    private final SkillAnalyzeDailyMapper dailyMapper;

    /**
     * 检查并递增当日分析次数。
     *
     * @param userId 用户ID
     * @param limit  每日上限
     * @return 递增后的次数
     */
    public int checkAndIncrement(Long userId, int limit) {
        LocalDate today = LocalDate.now();

        int updated = dailyMapper.incrementIfBelowLimit(userId, today, limit);
        if (updated > 0) {
            return currentCount(userId, today);
        }

        SkillAnalyzeDaily record = new SkillAnalyzeDaily();
        record.setUserId(userId);
        record.setAttemptDate(today);
        record.setAttemptCount(1);
        record.setCreatedBy(0L);
        record.setUpdatedBy(0L);
        record.setTenantId(0L);
        try {
            dailyMapper.insert(record);
            return 1;
        } catch (DuplicateKeyException e) {
            updated = dailyMapper.incrementIfBelowLimit(userId, today, limit);
            if (updated == 0) {
                log.warn("用户 AI 提示词分析日次数已达上限 userId={}, date={}", userId, today);
                throw new com.aichuangzuo.shared.exception.BusinessException(
                        com.aichuangzuo.shared.enums.error.SkillErrorCode.SKILL_ANALYZE_DAILY_LIMIT_EXCEEDED);
            }
            return currentCount(userId, today);
        }
    }

    private int currentCount(Long userId, LocalDate date) {
        SkillAnalyzeDaily record = dailyMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillAnalyzeDaily>()
                        .eq(SkillAnalyzeDaily::getUserId, userId)
                        .eq(SkillAnalyzeDaily::getAttemptDate, date));
        return record == null ? 0 : record.getAttemptCount();
    }
}
