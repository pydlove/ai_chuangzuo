package com.aichuangzuo.user.modules.recommendedcreation.mapper;

import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationTopicHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小爱推荐创作选题历史记录 Mapper。
 */
@Mapper
public interface RecommendedCreationTopicHistoryMapper extends BaseMapper<RecommendedCreationTopicHistory> {

    /**
     * 查询指定用户在指定时间之后已推荐的选题标题（按时间倒序）。
     *
     * @param userId 用户ID
     * @param since  起始时间
     * @return 标题列表
     */
    @Select("SELECT title FROM u_recommended_creation_topic_history " +
            "WHERE user_id = #{userId} AND is_deleted = 0 AND created_at >= #{since} " +
            "ORDER BY created_at DESC")
    List<String> selectTitlesByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
