package com.aichuangzuo.user.modules.style.market.mapper;

import com.aichuangzuo.user.modules.style.market.dto.MarketStyleRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户端 - 风格市场聚合查询 Mapper。SQL 定义在 {@code resources/mapper/StyleMarketAggregateMapper.xml}。
 */
@Mapper
public interface StyleMarketAggregateMapper {

    /**
     * 查询已上架的风格市场列表。
     */
    List<MarketStyleRow> selectEnabledMarketStyles();
}
