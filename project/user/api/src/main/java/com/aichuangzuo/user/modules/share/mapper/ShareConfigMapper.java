package com.aichuangzuo.user.modules.share.mapper;

import com.aichuangzuo.shared.entity.ShareConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShareConfigMapper extends BaseMapper<ShareConfig> {

    @Select("SELECT * FROM u_share_config WHERE scene_key = #{sceneKey} AND enabled = 1 AND is_deleted = 0 LIMIT 1")
    ShareConfig selectEnabledBySceneKey(@Param("sceneKey") String sceneKey);
}
