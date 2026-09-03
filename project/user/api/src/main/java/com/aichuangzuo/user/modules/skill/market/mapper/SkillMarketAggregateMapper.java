package com.aichuangzuo.user.modules.skill.market.mapper;

import com.aichuangzuo.user.modules.skill.market.dto.MarketSkillRow;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 根据关键词推荐已上架的市场提示词。
     *
     * @param userId   当前用户ID，用于排除自己发布的提示词
     * @param keywords 关键词列表
     * @param limit    返回条数上限
     */
    List<MarketSkillRow> selectRecommendedMarketSkills(@Param("userId") Long userId,
                                                         @Param("keywords") List<String> keywords,
                                                         @Param("limit") int limit);
}
