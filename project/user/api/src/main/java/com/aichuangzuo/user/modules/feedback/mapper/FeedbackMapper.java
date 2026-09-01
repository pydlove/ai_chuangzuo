package com.aichuangzuo.user.modules.feedback.mapper;

import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("SELECT COUNT(*) FROM u_feedback WHERE user_id = #{userId} AND created_at >= #{since} AND is_deleted = 0")
    long countRecentByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Select("""
            SELECT id, user_id AS userId, type, content,
                   reply_content AS replyContent,
                   reply_admin_id AS replyAdminId,
                   replied_at AS repliedAt,
                   status, created_at AS createdAt
            FROM u_feedback
            WHERE user_id = #{userId} AND is_deleted = 0
              AND (#{status} IS NULL OR status = #{status})
            ORDER BY created_at DESC
            LIMIT #{offset}, #{size}
            """)
    List<Feedback> pageByUser(@Param("userId") Long userId,
                              @Param("status") Integer status,
                              @Param("offset") int offset,
                              @Param("size") int size);

    @Select("""
            SELECT COUNT(*) FROM u_feedback
            WHERE user_id = #{userId} AND is_deleted = 0
              AND (#{status} IS NULL OR status = #{status})
            """)
    long countByUser(@Param("userId") Long userId,
                     @Param("status") Integer status);

    @Select("""
            SELECT id, user_id AS userId, type, content,
                   reply_content AS replyContent,
                   reply_admin_id AS replyAdminId,
                   replied_at AS repliedAt,
                   star_rating AS starRating,
                   status, created_at AS createdAt
            FROM u_feedback
            WHERE user_id = #{userId} AND type = '评价' AND is_deleted = 0
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Feedback findReviewByUser(@Param("userId") Long userId);
}
