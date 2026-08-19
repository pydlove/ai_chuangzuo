package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlanNiche;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SelfMediaPlanNicheMapper extends BaseMapper<SelfMediaPlanNiche> {

    @Select("SELECT * FROM u_self_media_plan_niche " +
            "WHERE user_id = #{userId} AND platform_key = #{platformKey} " +
            "AND answer_snapshot_hash = #{hash} AND is_deleted = 0 " +
            "ORDER BY id ASC")
    List<SelfMediaPlanNiche> selectByUserPlatformAndHash(@Param("userId") Long userId,
                                                         @Param("platformKey") String platformKey,
                                                         @Param("hash") String hash);
}
