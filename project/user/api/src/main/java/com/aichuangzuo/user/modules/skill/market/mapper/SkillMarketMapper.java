package com.aichuangzuo.user.modules.skill.market.mapper;

import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

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

    /**
     * 原子性增加市场提示词的使用次数与收益。
     *
     * @param skillId 市场 skill 主键
     * @param price   单次使用收益
     * @return 受影响行数；通常为 1
     */
    @Update("UPDATE u_skill_market SET total_uses = total_uses + 1, weekly_uses = weekly_uses + 1, " +
            "weekly_earnings = weekly_earnings + #{price}, updated_at = NOW(3) " +
            "WHERE id = #{skillId} AND is_deleted = 0")
    int incrementUsageStats(@Param("skillId") Long skillId, @Param("price") BigDecimal price);
}
