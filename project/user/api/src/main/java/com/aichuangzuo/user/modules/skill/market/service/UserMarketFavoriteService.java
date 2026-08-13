package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 用户风格市场收藏服务。
 */
public interface UserMarketFavoriteService {

    /**
     * 分页查询当前用户收藏的市场 skill 详情列表，可按关键词过滤。
     * <p>返回结果包含已下架/已删除的 skill，方便前端展示收藏但不可用状态。
     *
     * @param userId   用户主键
     * @param keyword  关键词，匹配名称、适用范围、提示词或描述；为空时不过滤
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页市场 skill 视图对象，按收藏时间倒序
     */
    IPage<MarketSkillVO> listFavoriteSkills(Long userId, String keyword, int page, int pageSize);

    /**
     * 收藏市场 skill。
     *
     * @param userId        用户主键
     * @param marketSkillId 市场 skill 业务编号
     */
    void addFavorite(Long userId, String marketSkillId);

    /**
     * 取消收藏市场 skill。
     *
     * @param userId        用户主键
     * @param marketSkillId 市场 skill 业务编号
     */
    void removeFavorite(Long userId, String marketSkillId);
}
