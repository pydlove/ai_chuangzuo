package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SelfMediaPlanQuestionMapper extends BaseMapper<SelfMediaPlanQuestion> {

    @Select("SELECT * FROM u_self_media_plan_question " +
            "WHERE user_id = #{userId} AND platform_key = #{platformKey} AND is_deleted = 0 " +
            "ORDER BY sort_order ASC, id ASC")
    List<SelfMediaPlanQuestion> selectByUserAndPlatform(@Param("userId") Long userId,
                                                        @Param("platformKey") String platformKey);

    @Delete("DELETE FROM u_self_media_plan_question WHERE user_id = #{userId} AND platform_key = #{platformKey}")
    int deleteByUserAndPlatform(@Param("userId") Long userId, @Param("platformKey") String platformKey);
}
