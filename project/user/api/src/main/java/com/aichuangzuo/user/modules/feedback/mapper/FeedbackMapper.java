package com.aichuangzuo.user.modules.feedback.mapper;

import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("SELECT COUNT(*) FROM u_feedback WHERE user_id = #{userId} AND created_at >= #{since} AND is_deleted = 0")
    long countRecentByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
