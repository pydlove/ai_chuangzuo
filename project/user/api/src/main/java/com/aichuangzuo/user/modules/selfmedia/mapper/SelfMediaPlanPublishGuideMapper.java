package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanPublishGuide;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SelfMediaPlanPublishGuideMapper extends BaseMapper<SelfMediaPlanPublishGuide> {

    @Select("SELECT * FROM u_self_media_plan_publish_guide " +
            "WHERE user_id = #{userId} AND main_platform = #{mainPlatform} " +
            "AND plan_content_hash = #{planContentHash} " +
            "AND is_deleted = 0 LIMIT 1")
    SelfMediaPlanPublishGuide selectByUserPlatformAndHash(@Param("userId") Long userId,
                                                          @Param("mainPlatform") String mainPlatform,
                                                          @Param("planContentHash") String planContentHash);
}
