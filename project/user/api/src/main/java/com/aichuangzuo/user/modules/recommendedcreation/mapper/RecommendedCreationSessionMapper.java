package com.aichuangzuo.user.modules.recommendedcreation.mapper;

import com.aichuangzuo.user.modules.recommendedcreation.entity.RecommendedCreationSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecommendedCreationSessionMapper extends BaseMapper<RecommendedCreationSession> {

    @Select("SELECT * FROM u_recommended_creation_session WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    RecommendedCreationSession selectByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM u_recommended_creation_session WHERE id = #{id}")
    int deleteByIdPhysically(@Param("id") Long id);
}
