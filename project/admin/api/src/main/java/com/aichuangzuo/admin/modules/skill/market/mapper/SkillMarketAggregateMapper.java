package com.aichuangzuo.admin.modules.skill.market.mapper;

import com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 风格市场聚合查询 Mapper。SQL 定义在 {@code resources/mapper/SkillMarketAggregateMapper.xml}。
 */
@Mapper
public interface SkillMarketAggregateMapper {

    /**
     * 分页查询风格市场列表（含启用状态过滤和关键词搜索）。
     */
    List<SkillMarketRow> selectMarketStylePage(@Param("enableStatus") Integer enableStatus,
                                               @Param("keyword") String keyword,
                                               @Param("offset") long offset,
                                               @Param("limit") long limit);

    /**
     * 同条件下的总数。
     */
    long countMarketStylePage(@Param("enableStatus") Integer enableStatus,
                              @Param("keyword") String keyword);
}
