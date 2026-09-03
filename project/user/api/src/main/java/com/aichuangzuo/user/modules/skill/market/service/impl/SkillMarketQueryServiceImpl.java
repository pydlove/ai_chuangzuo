package com.aichuangzuo.user.modules.skill.market.service.impl;

import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.aichuangzuo.user.modules.selfmedia.vo.SelfMediaPlanVO;
import com.aichuangzuo.user.modules.skill.market.dto.MarketSkillRow;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketAggregateMapper;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketQueryService;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillOverviewVO;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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
    private static final BigDecimal DEFAULT_PRICE_PER_USE = new BigDecimal("2.00");

    private final SkillMarketAggregateMapper aggregateMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final SelfMediaPlanService selfMediaPlanService;

    private static final Pattern KEYWORD_SPLIT_PATTERN = Pattern.compile("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");

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
                .map(r -> {
                    int totalUses = r.getTotalUses() == null ? 0 : r.getTotalUses();
                    return DEFAULT_PRICE_PER_USE.multiply(BigDecimal.valueOf(totalUses));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 官方精选：取 featured=1 的已上架提示词，按本月使用倒序，限制 FEATURED_LIMIT 条
        List<MarketSkillVO> featured = allApproved.stream()
                .filter(r -> r.getFeatured() != null && r.getFeatured() == 1)
                .sorted(Comparator.comparing(MarketSkillRow::getTotalUses, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketSkillRow::getApprovedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketSkillRow::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(FEATURED_LIMIT)
                .map(this::toVo)
                .collect(Collectors.toList());
        overview.setFeaturedSkills(featured);

        return overview;
    }

    @Override
    public List<MarketSkillVO> listMySubmissions(Long userId) {
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getPublisherUserId, userId)
                .eq(SkillMarket::getIsDeleted, 0)
                .orderByDesc(SkillMarket::getCreatedAt);
        return skillMarketMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketSkillVO> recommendSkills(Long userId, String title, int size) {
        int safeSize = Math.min(Math.max(1, size), 10);
        List<String> keywords = buildRecommendationKeywords(userId, title);
        if (keywords.isEmpty()) {
            return listFeaturedFallback(userId, safeSize);
        }
        List<MarketSkillRow> rows = aggregateMapper.selectRecommendedMarketSkills(userId, keywords, safeSize);
        if (rows.isEmpty()) {
            return listFeaturedFallback(userId, safeSize);
        }
        return rows.stream().map(this::toVo).collect(Collectors.toList());
    }

    private List<String> buildRecommendationKeywords(Long userId, String title) {
        Set<String> keywords = new LinkedHashSet<>();
        if (StringUtils.hasText(title)) {
            for (String part : KEYWORD_SPLIT_PATTERN.split(title.trim())) {
                if (part.length() >= 2) {
                    keywords.add(part);
                }
            }
        }
        SelfMediaPlanVO plan = selfMediaPlanService.getCurrentPlan(userId);
        if (plan != null) {
            addKeyword(keywords, plan.getPlatformName());
            addKeyword(keywords, plan.getNicheName());
            addKeyword(keywords, plan.getPersonaName());
            List<PillarVO> pillars = plan.getPillars();
            if (pillars != null) {
                for (PillarVO pillar : pillars) {
                    if (pillar != null) {
                        addKeyword(keywords, pillar.getName());
                    }
                }
            }
        }
        return new ArrayList<>(keywords);
    }

    private void addKeyword(Set<String> keywords, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            keywords.add(trimmed);
        }
    }

    private List<MarketSkillVO> listFeaturedFallback(Long userId, int size) {
        List<MarketSkillRow> allApproved = aggregateMapper.selectEnabledMarketSkills(
                new Page<>(1, Integer.MAX_VALUE), null, "all").getRecords();
        return allApproved.stream()
                .filter(r -> !userId.equals(r.getPublisherUserId()))
                .filter(r -> r.getFeatured() != null && r.getFeatured() == 1)
                .sorted(Comparator.comparing(MarketSkillRow::getWeeklyUses, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketSkillRow::getTotalUses, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketSkillRow::getApprovedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(size)
                .map(this::toVo)
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
        vo.setPrice(DEFAULT_PRICE_PER_USE);
        vo.setWeeklyUses(row.getWeeklyUses());
        vo.setTotalUses(row.getTotalUses());
        vo.setWeeklyEarnings(row.getWeeklyEarnings());
        vo.setMilestoneBonus(row.getMilestoneBonus());
        vo.setFeatured(row.getFeatured() != null && row.getFeatured() == 1);
        vo.setLastSettlementAt(row.getLastSettlementAt());
        vo.setApprovedAt(row.getApprovedAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private MarketSkillVO toVo(SkillMarket market) {
        MarketSkillVO vo = new MarketSkillVO();
        vo.setId(market.getBizNo());
        vo.setName(market.getSkillName());
        vo.setDescription(market.getDescription());
        vo.setSourceType(toSourceTypeString(market.getSourceType()));
        vo.setCreatorId(market.getPublisherUserId());
        vo.setCreatorName(null);
        vo.setPrompt(market.getPrompt());
        vo.setScope(market.getScope());
        vo.setExcerpt1(null);
        vo.setExcerpt2(null);
        vo.setStatus(toStatusString(market.getAuditStatus()));
        vo.setPrice(market.getPrice());
        vo.setWeeklyUses(market.getWeeklyUses());
        vo.setTotalUses(market.getTotalUses());
        vo.setWeeklyEarnings(market.getWeeklyEarnings());
        vo.setMilestoneBonus(market.getMilestoneBonus());
        vo.setFeatured(Boolean.FALSE);
        vo.setLastSettlementAt(market.getLastSettlementAt());
        vo.setApprovedAt(market.getApprovedAt());
        vo.setCreatedAt(market.getCreatedAt());
        return vo;
    }

    private String toSourceTypeString(Integer code) {
        return code != null && code == 1 ? "my" : "learned";
    }

    private String toStatusString(Integer code) {
        if (code == null) {
            return "pending";
        }
        return switch (code) {
            case 1 -> "approved";
            case 2 -> "rejected";
            default -> "pending";
        };
    }
}
