package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.user.modules.skill.market.config.entity.SkillMonthlyRewardConfig;
import com.aichuangzuo.user.modules.skill.market.config.mapper.SkillMonthlyRewardConfigMapper;
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
    private static final int CREATOR_LIMIT = 5;
    private static final long CONFIG_ID = 1L;
    private static final BigDecimal DEFAULT_PRICE_PER_USE = new BigDecimal("2.00");

    private final SkillMarketAggregateMapper aggregateMapper;
    private final SkillMonthlyRewardConfigMapper configMapper;

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
        BigDecimal pricePerUse = resolvePricePerUse();

        MarketSkillOverviewVO overview = new MarketSkillOverviewVO();
        overview.setApprovedCount((long) allApproved.size());
        overview.setTotalUses(allApproved.stream()
                .mapToLong(r -> r.getTotalUses() == null ? 0L : r.getTotalUses().longValue())
                .sum());
        overview.setTotalEarnings(allApproved.stream()
                .map(r -> {
                    int totalUses = r.getTotalUses() == null ? 0 : r.getTotalUses();
                    return pricePerUse.multiply(BigDecimal.valueOf(totalUses));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 官方精选：取 featured=1 的已上架提示词，按本月使用倒序，限制 FEATURED_LIMIT 条
        List<MarketSkillVO> featured = allApproved.stream()
                .filter(r -> r.getFeatured() != null && r.getFeatured() == 1)
                .sorted(Comparator.comparing(MarketSkillRow::getMonthlyUses, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketSkillRow::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(FEATURED_LIMIT)
                .map(this::toVo)
                .collect(Collectors.toList());
        overview.setFeaturedSkills(featured);

        overview.setTopCreators(buildTopCreators(allApproved));
        return overview;
    }

    private List<TopCreatorVO> buildTopCreators(List<MarketSkillRow> rows) {
        BigDecimal pricePerUse = resolvePricePerUse();
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
                    vo.setMonthlyEarnings(list.stream()
                            .map(MarketSkillRow::getMonthlyEarnings)
                            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b == null ? BigDecimal.ZERO : b)));
                    vo.setMonthlyUses(list.stream()
                            .mapToInt(r -> r.getMonthlyUses() == null ? 0 : r.getMonthlyUses())
                            .sum());
                    vo.setBestSkill(best == null ? null : toVo(best));
                    vo.setTotalEarnings(list.stream()
                            .map(r -> {
                                int totalUses = r.getTotalUses() == null ? 0 : r.getTotalUses();
                                return pricePerUse.multiply(BigDecimal.valueOf(totalUses));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    return vo;
                })
                .filter(vo -> vo.getMonthlyEarnings() != null && vo.getMonthlyEarnings().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TopCreatorVO::getMonthlyEarnings, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(CREATOR_LIMIT)
                .collect(Collectors.toList());
    }

    private MarketSkillVO toVo(MarketSkillRow row) {
        MarketSkillVO vo = new MarketSkillVO();
        vo.setId(row.getBizNo());
        vo.setName(row.getSkillName());
        vo.setDescription(row.getDescription());
        vo.setSourceType("admin");
        vo.setCreatorId(row.getPublisherUserId());
        vo.setCreatorName(row.getPublisherName());
        vo.setPrompt(row.getPrompt());
        vo.setScope(row.getScope());
        vo.setExcerpt1(null);
        vo.setExcerpt2(null);
        vo.setStatus("approved");
        vo.setPrice(resolvePricePerUse());
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setMonthlyUses(row.getMonthlyUses());
        vo.setMonthlyEarnings(row.getMonthlyEarnings());
        vo.setLeaderboardReward(row.getLeaderboardReward());
        vo.setFeatured(row.getFeatured() != null && row.getFeatured() == 1);
        vo.setLastSettlementAt(row.getLastSettlementAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private BigDecimal resolvePricePerUse() {
        SkillMonthlyRewardConfig config = configMapper.selectById(CONFIG_ID);
        if (config == null || config.getPricePerUse() == null
                || config.getPricePerUse().compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_PRICE_PER_USE;
        }
        return config.getPricePerUse();
    }
}
