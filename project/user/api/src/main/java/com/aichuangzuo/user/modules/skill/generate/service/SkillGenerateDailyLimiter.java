package com.aichuangzuo.user.modules.skill.generate.service;

import com.aichuangzuo.shared.enums.error.SkillErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.skill.generate.entity.SkillGenerateDaily;
import com.aichuangzuo.user.modules.skill.generate.mapper.SkillGenerateDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 用户 AI 提示词帮写日次数限制器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillGenerateDailyLimiter {

    private final SkillGenerateDailyMapper dailyMapper;

    /**
     * 检查并递增当日帮写次数。
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

        SkillGenerateDaily record = new SkillGenerateDaily();
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
                log.warn("用户 AI 提示词帮写日次数已达上限 userId={}, date={}", userId, today);
                throw new BusinessException(SkillErrorCode.SKILL_GENERATE_DAILY_LIMIT_EXCEEDED);
            }
            return currentCount(userId, today);
        }
    }

    /**
     * 查询当日已帮写次数（未生成过返回 0）。
     */
    public int currentCount(Long userId, LocalDate date) {
        SkillGenerateDaily record = dailyMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillGenerateDaily>()
                        .eq(SkillGenerateDaily::getUserId, userId)
                        .eq(SkillGenerateDaily::getAttemptDate, date));
        return record == null ? 0 : record.getAttemptCount();
    }
}
