package com.aichuangzuo.user.modules.skill.analyze.mapper;

import com.aichuangzuo.user.modules.skill.analyze.entity.SkillAnalyzeDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 用户 AI 提示词分析日次数统计 Mapper。
 */
@Mapper
public interface SkillAnalyzeDailyMapper extends BaseMapper<SkillAnalyzeDaily> {

    /**
     * 原子地将当日分析次数 +1，仅在未超过上限时生效。
     *
     * @param userId 用户ID
     * @param attemptDate 分析日期
     * @param limit 上限
     * @return 受影响行数；0 表示记录不存在或已达上限
     */
    @Update("UPDATE u_skill_analyze_daily SET attempt_count = attempt_count + 1 " +
            "WHERE user_id = #{userId} AND attempt_date = #{attemptDate} AND attempt_count < #{limit}")
    int incrementIfBelowLimit(@Param("userId") Long userId,
                              @Param("attemptDate") LocalDate attemptDate,
                              @Param("limit") int limit);
}
