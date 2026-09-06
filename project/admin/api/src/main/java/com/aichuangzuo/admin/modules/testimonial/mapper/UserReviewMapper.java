package com.aichuangzuo.admin.modules.testimonial.mapper;

import com.aichuangzuo.admin.modules.testimonial.entity.UserReviewEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserReviewMapper extends BaseMapper<UserReviewEntity> {

    @Select("""
            SELECT COUNT(*) FROM u_feedback
            WHERE type = '评价' AND is_deleted = 0
              AND (#{keyword} IS NULL OR content LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countReviews(@Param("keyword") String keyword);

    @Select("""
            SELECT id, user_id AS userId, type, content,
                   star_rating AS starRating,
                   reply_content AS replyContent,
                   reply_admin_id AS replyAdminId,
                   replied_at AS repliedAt,
                   status, is_show_on_homepage AS isShowOnHomepage,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM u_feedback
            WHERE type = '评价' AND is_deleted = 0
              AND (#{keyword} IS NULL OR content LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY created_at DESC
            LIMIT #{offset}, #{size}
            """)
    List<UserReviewEntity> pageReviews(@Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    @Update("""
            UPDATE u_feedback
            SET is_show_on_homepage = #{isShowOnHomepage},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id} AND type = '评价' AND is_deleted = 0
            """)
    int updateShowOnHomepage(@Param("id") Long id,
                             @Param("isShowOnHomepage") Integer isShowOnHomepage);
}
