package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanPersona;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SelfMediaPlanPersonaMapper extends BaseMapper<SelfMediaPlanPersona> {

    @Select("SELECT * FROM u_self_media_plan_persona " +
            "WHERE user_id = #{userId} AND platform_key = #{platformKey} " +
            "AND answer_snapshot_hash = #{hash} AND niche_key = #{nicheKey} AND is_deleted = 0 " +
            "ORDER BY id ASC")
    List<SelfMediaPlanPersona> selectByUserPlatformHashAndNiche(@Param("userId") Long userId,
                                                                @Param("platformKey") String platformKey,
                                                                @Param("hash") String hash,
                                                                @Param("nicheKey") String nicheKey);
}
