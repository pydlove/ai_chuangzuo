package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanPublishGuide;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SelfMediaPlanPublishGuideMapper extends BaseMapper<SelfMediaPlanPublishGuide> {

    @Select("SELECT * FROM u_self_media_plan_publish_guide " +
            "WHERE user_id = #{userId} AND article_title = #{articleTitle} AND main_platform = #{mainPlatform} " +
            "AND is_deleted = 0 LIMIT 1")
    SelfMediaPlanPublishGuide selectByUserTitleAndPlatform(@Param("userId") Long userId,
                                                           @Param("articleTitle") String articleTitle,
                                                           @Param("mainPlatform") String mainPlatform);
}
