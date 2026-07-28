package com.aichuangzuo.user.modules.skill.market.mapper;

import com.aichuangzuo.user.modules.skill.market.dto.MarketSkillRow;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户端 - 风格市场聚合查询 Mapper。SQL 定义在 {@code resources/mapper/SkillMarketAggregateMapper.xml}。
 */
@Mapper
public interface SkillMarketAggregateMapper {

    /**
     * 分页查询已上架的风格市场列表。
     *
     * @param page     分页对象
     * @param keyword  关键词（匹配风格名或适用范围）
     * @param sortType 排序类型：all / week-hot / all-hot / new / featured
     */
    IPage<MarketSkillRow> selectEnabledMarketSkills(IPage<?> page,
                                                    @Param("keyword") String keyword,
                                                    @Param("sortType") String sortType);
}
