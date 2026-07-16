package com.aichuangzuo.user.modules.topic.mapper;

import com.aichuangzuo.shared.entity.TopicTitle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 选题标题库 Mapper（用户端）：随机拉取 + 使用计数。
 */
@Mapper
public interface TopicTitleMapper extends BaseMapper<TopicTitle> {

    /**
     * 随机拉取 N 条标题：排除「该用户已用过 + 已删除」。
     * 只查 id/title/summary 三列，其余字段为 null。
     */
    @Select("SELECT t.id, t.title, t.summary FROM u_topic_title t " +
            "WHERE t.is_deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM u_topic_title_usage u WHERE u.title_id = t.id AND u.user_id = #{userId}) " +
            "ORDER BY RAND() LIMIT #{count}")
    List<TopicTitle> selectRandomExcludeUsed(@Param("userId") Long userId, @Param("count") int count);

    /**
     * 使用次数 +1（仅当 usage 行实际插入成功后才调用，重复 use 不膨胀计数）。
     */
    @Update("UPDATE u_topic_title SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);
}
