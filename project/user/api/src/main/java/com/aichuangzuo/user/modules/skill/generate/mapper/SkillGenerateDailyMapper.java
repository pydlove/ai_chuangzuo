package com.aichuangzuo.user.modules.skill.generate.mapper;

import com.aichuangzuo.user.modules.skill.generate.entity.SkillGenerateDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 用户 AI 提示词帮写日次数统计 Mapper。
 */
@Mapper
public interface SkillGenerateDailyMapper extends BaseMapper<SkillGenerateDaily> {

    /**
     * 原子地将当日帮写次数 +1，仅在未超过上限时生效。
     *
     * @param userId 用户ID
     * @param attemptDate 帮写日期
     * @param limit 上限
     * @return 受影响行数；0 表示记录不存在或已达上限
     */
    @Update("UPDATE u_skill_generate_daily SET attempt_count = attempt_count + 1 " +
            "WHERE user_id = #{userId} AND attempt_date = #{attemptDate} AND attempt_count < #{limit}")
    int incrementIfBelowLimit(@Param("userId") Long userId,
                              @Param("attemptDate") LocalDate attemptDate,
                              @Param("limit") int limit);
}
