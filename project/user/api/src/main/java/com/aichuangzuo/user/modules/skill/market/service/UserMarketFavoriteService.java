package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;

import java.util.List;

/**
 * 用户风格市场收藏服务。
 */
public interface UserMarketFavoriteService {

    /**
     * 查询当前用户收藏的市场 skill 详情列表。
     * <p>返回结果包含已下架/已删除的 skill，方便前端展示收藏但不可用状态。
     *
     * @param userId 用户主键
     * @return 市场 skill 视图对象列表，按收藏时间倒序
     */
    List<MarketSkillVO> listFavoriteSkills(Long userId);

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
