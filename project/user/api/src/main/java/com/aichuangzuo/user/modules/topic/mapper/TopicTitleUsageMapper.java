package com.aichuangzuo.user.modules.topic.mapper;

import com.aichuangzuo.user.modules.topic.entity.TopicTitleUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户标题使用记录 Mapper。
 */
@Mapper
public interface TopicTitleUsageMapper extends BaseMapper<TopicTitleUsage> {

    /**
     * 幂等插入使用记录：uk(user_id, title_id) 冲突时忽略。
     *
     * @return 受影响行数；1=新插入，0=已存在（重复 use）
     */
    @Insert("INSERT IGNORE INTO u_topic_title_usage (user_id, title_id) VALUES (#{userId}, #{titleId})")
    int insertIgnore(@Param("userId") Long userId, @Param("titleId") Long titleId);
}
