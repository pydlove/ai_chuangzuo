package com.aichuangzuo.user.modules.style.market.service.impl;

import com.aichuangzuo.user.modules.style.market.dto.MarketStyleRow;
import com.aichuangzuo.user.modules.style.market.mapper.StyleMarketAggregateMapper;
import com.aichuangzuo.user.modules.style.market.service.StyleMarketQueryService;
import com.aichuangzuo.user.modules.style.market.vo.MarketStyleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端 - 风格市场查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class StyleMarketQueryServiceImpl implements StyleMarketQueryService {

    private final StyleMarketAggregateMapper aggregateMapper;

    @Override
    public List<MarketStyleVO> listEnabled() {
        List<MarketStyleRow> rows = aggregateMapper.selectEnabledMarketStyles();
        return rows.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    private MarketStyleVO toVo(MarketStyleRow row) {
        MarketStyleVO vo = new MarketStyleVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getStyleName());
        vo.setSourceType("admin");
        vo.setCreatorId(row.getPublisherUserId());
        vo.setCreatorName(row.getPublisherName());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setExcerpt1(null);
        vo.setExcerpt2(null);
        vo.setStatus("approved");
        vo.setPrice(row.getPrice());
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setLastSettlementAt(row.getLastSettlementAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
