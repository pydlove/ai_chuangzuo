package com.aichuangzuo.user.modules.style.market.service;

import com.aichuangzuo.user.modules.style.market.vo.MarketStyleVO;

import java.util.List;

/**
 * 用户端 - 风格市场查询服务。
 */
public interface StyleMarketQueryService {

    /**
     * 获取已上架的风格市场列表。
     */
    List<MarketStyleVO> listEnabled();
}
