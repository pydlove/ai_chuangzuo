package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.user.modules.skill.market.dto.MarketSkillRow;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketAggregateMapper;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketQueryService;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillOverviewVO;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.aichuangzuo.user.modules.skill.market.vo.TopCreatorVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端 - 风格市场查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class SkillMarketQueryServiceImpl implements SkillMarketQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int FEATURED_LIMIT = 6;
    private static final int CREATOR_LIMIT = 20;

    private final SkillMarketAggregateMapper aggregateMapper;

    @Override
    public List<MarketSkillVO> listEnabled() {
        List<MarketSkillRow> rows = aggregateMapper.selectEnabledMarketSkills(new Page<>(1, Integer.MAX_VALUE), null, "all").getRecords();
        return rows.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<MarketSkillVO> pageEnabled(int page, int pageSize, String keyword, String sortType) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String safeSortType = StringUtils.hasText(sortType) ? sortType : "all";

        Page<MarketSkillRow> rowPage = new Page<>(safePage, safeSize);
        IPage<MarketSkillRow> result = aggregateMapper.selectEnabledMarketSkills(rowPage, safeKeyword, safeSortType);

        List<MarketSkillVO> records = result.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        Page<MarketSkillVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public MarketSkillOverviewVO getOverview() {
        List<MarketSkillRow> allApproved = aggregateMapper.selectEnabledMarketSkills(new Page<>(1, Integer.MAX_VALUE), null, "all").getRecords();

        MarketSkillOverviewVO overview = new MarketSkillOverviewVO();
        overview.setApprovedCount((long) allApproved.size());
        overview.setTotalUses(allApproved.stream()
                .mapToLong(r -> r.getTotalUses() == null ? 0L : r.getTotalUses().longValue())
                .sum());
        overview.setTotalEarnings(allApproved.stream()
                .map(r -> r.getTotalUses() == null ? BigDecimal.ZERO
                        : BigDecimal.valueOf(r.getTotalUses().longValue() * 2L))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 当前表结构无 featured 字段，官方精选默认返回空列表
        List<MarketSkillVO> featured = java.util.Collections.emptyList();
        overview.setFeaturedSkills(featured);

        overview.setTopCreators(buildTopCreators(allApproved));
        return overview;
    }

    private List<TopCreatorVO> buildTopCreators(List<MarketSkillRow> rows) {
        Map<Long, List<MarketSkillRow>> byCreator = rows.stream()
                .filter(r -> r.getPublisherUserId() != null)
                .collect(Collectors.groupingBy(MarketSkillRow::getPublisherUserId));

        return byCreator.entrySet().stream()
                .map(e -> {
                    List<MarketSkillRow> list = e.getValue();
                    MarketSkillRow best = list.stream()
                            .max(Comparator.comparing(MarketSkillRow::getTotalUses, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);
                    TopCreatorVO vo = new TopCreatorVO();
                    vo.setCreatorId(e.getKey());
                    vo.setCreatorName(list.get(0).getPublisherName());
                    vo.setWeeklyEarnings(list.stream()
                            .map(MarketSkillRow::getWeeklyEarnings)
                            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b == null ? BigDecimal.ZERO : b)));
                    vo.setWeeklyUses(list.stream()
                            .mapToInt(r -> r.getWeeklyUses() == null ? 0 : r.getWeeklyUses())
                            .sum());
                    vo.setBestSkill(best == null ? null : toVo(best));
                    vo.setTotalEarnings(list.stream()
                            .map(r -> r.getTotalUses() == null ? BigDecimal.ZERO
                                    : BigDecimal.valueOf(r.getTotalUses().longValue() * 2L))
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    return vo;
                })
                .sorted(Comparator.comparing(TopCreatorVO::getWeeklyEarnings, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(CREATOR_LIMIT)
                .collect(Collectors.toList());
    }

    private MarketSkillVO toVo(MarketSkillRow row) {
        MarketSkillVO vo = new MarketSkillVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
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
        vo.setFeatured(false);
        vo.setLastSettlementAt(row.getLastSettlementAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
