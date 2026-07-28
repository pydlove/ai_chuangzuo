package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillOverviewVO;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 用户端 - 风格市场查询服务。
 */
public interface SkillMarketQueryService {

    /**
     * 获取全部已上架的风格市场列表（兼容旧逻辑）。
     */
    List<MarketSkillVO> listEnabled();

    /**
     * 分页查询已上架的风格市场列表。
     *
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数
     * @param keyword  关键词（匹配风格名或适用范围）
     * @param sortType 排序类型：all / week-hot / all-hot / new / featured
     */
    IPage<MarketSkillVO> pageEnabled(int page, int pageSize, String keyword, String sortType);

    /**
     * 获取风格市场概览（统计、官方精选、收益潜力榜）。
     */
    MarketSkillOverviewVO getOverview();
}
