package com.aichuangzuo.user.modules.skill.market.mapper;

import com.aichuangzuo.user.modules.skill.market.dto.MarketSkillRow;
import com.aichuangzuo.user.modules.skill.market.entity.UserMarketFavorite;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户风格市场收藏 Mapper。
 */
@Mapper
public interface UserMarketFavoriteMapper extends BaseMapper<UserMarketFavorite> {

    /**
     * 分页查询用户收藏的市场 skill 详情（含已下架/已删除）。
     *
     * @param page     分页参数
     * @param userId   用户主键
     * @param keyword  关键词，匹配名称、适用范围、提示词或描述；为空时不过滤
     * @return 分页市场 skill 行记录
     */
    IPage<MarketSkillRow> selectFavoriteSkillsPage(IPage<?> page,
                                                    @Param("userId") Long userId,
                                                    @Param("keyword") String keyword);
}
