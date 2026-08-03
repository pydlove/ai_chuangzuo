package com.aichuangzuo.user.modules.skill.market.mapper;

import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户端 - 风格市场 BaseMapper。
 */
@Mapper
public interface SkillMarketMapper extends BaseMapper<SkillMarket> {

    /**
     * 按业务编号查询市场 skill，包含已逻辑删除的行（用于重新发布时复活旧记录）。
     */
    @Select("SELECT * FROM u_skill_market WHERE biz_no = #{bizNo} LIMIT 1")
    SkillMarket selectByBizNoIncludeDeleted(@Param("bizNo") String bizNo);
}
