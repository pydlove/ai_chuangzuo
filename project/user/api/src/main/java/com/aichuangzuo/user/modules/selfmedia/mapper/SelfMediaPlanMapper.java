package com.aichuangzuo.user.modules.selfmedia.mapper;

import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SelfMediaPlanMapper extends BaseMapper<SelfMediaPlan> {

    @Select("SELECT * FROM u_self_media_plan WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    SelfMediaPlan selectByUserId(@Param("userId") Long userId);
}
