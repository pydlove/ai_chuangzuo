package com.aichuangzuo.admin.modules.stats.service;

import com.aichuangzuo.admin.modules.stats.mapper.DashboardGroupRow;
import com.aichuangzuo.admin.modules.stats.mapper.DashboardMapper;
import com.aichuangzuo.admin.modules.stats.vo.DashboardDistributionVO;
import com.aichuangzuo.admin.modules.stats.vo.DashboardOverviewVO;
import com.aichuangzuo.admin.modules.stats.vo.DashboardTrendVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatsService {

    /** 在线窗口：最近 5 分钟有打点即视为在线 */
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    /** 看板整体缓存 60s：12 个计数查询一次算完，管理页读多写少 */
    private final Cache<String, DashboardOverviewVO> dashboardCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(1)
            .build();

    /** 趋势缓存 60s，按天数分 key */
    private final Cache<String, DashboardTrendVO> trendCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(4)
            .build();

    /** 分布缓存 60s */
    private final Cache<String, DashboardDistributionVO> distributionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(1)
            .build();

    private final DashboardMapper dashboardMapper;

    public StatsService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardOverviewVO dashboard() {
        return dashboardCache.get("dashboard", k -> loadDashboard());
    }

    public DashboardTrendVO trend(int days) {
        int normalized = Math.min(Math.max(days, 7), 90);
        return trendCache.get("trend:" + normalized, k -> loadTrend(normalized));
    }

    public DashboardDistributionVO distribution() {
        return distributionCache.get("distribution", k -> loadDistribution());
    }

    private DashboardOverviewVO loadDashboard() {
        return new DashboardOverviewVO(
                dashboardMapper.countOnline(LocalDateTime.now().minus(ONLINE_WINDOW)),
                dashboardMapper.countTodayActive(),
                dashboardMapper.countTotalUsers(),
                dashboardMapper.countTodayNewUsers(),
                dashboardMapper.countValidMembers(),
                dashboardMapper.countTotalArticles(),
                dashboardMapper.countTodayArticles(),
                dashboardMapper.countTodayPaidOrders(),
                dashboardMapper.sumTodayPaidAmount(),
                dashboardMapper.sumTotalPaidAmount(),
                dashboardMapper.countPendingWithdraws()
        );
    }

    private DashboardTrendVO loadTrend(int days) {
        LocalDate fromDate = LocalDate.now().minusDays(days - 1L);
        LocalDateTime from = fromDate.atStartOfDay();
        Map<String, Long> newUserMap = toCountMap(dashboardMapper.countDailyNewUsers(from));
        Map<String, Long> activeMap = toCountMap(dashboardMapper.countDailyActive(fromDate));
        Map<String, Long> articleMap = toCountMap(dashboardMapper.countDailyArticles(from));
        Map<String, DashboardGroupRow> paidMap = dashboardMapper.countDailyPaid(from).stream()
                .collect(Collectors.toMap(DashboardGroupRow::getItemKey, Function.identity(), (a, b) -> a));

        List<DashboardTrendVO.DailyPoint> points = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String date = fromDate.plusDays(i).toString();
            DashboardGroupRow paid = paidMap.get(date);
            points.add(new DashboardTrendVO.DailyPoint(
                    date,
                    newUserMap.getOrDefault(date, 0L),
                    activeMap.getOrDefault(date, 0L),
                    articleMap.getOrDefault(date, 0L),
                    paid != null && paid.getItemCount() != null ? paid.getItemCount() : 0L,
                    paid != null && paid.getItemAmount() != null ? paid.getItemAmount() : BigDecimal.ZERO
            ));
        }
        return new DashboardTrendVO(points);
    }

    private DashboardDistributionVO loadDistribution() {
        List<DashboardDistributionVO.Item> memberPlans = dashboardMapper.countMemberPlanDistribution().stream()
                .map(r -> toItem(r, StatsService::memberPlanName))
                .toList();
        List<DashboardDistributionVO.Item> articlePlatforms = dashboardMapper.countArticlePlatformDistribution().stream()
                .map(r -> toItem(r, StatsService::platformName))
                .toList();
        List<DashboardDistributionVO.Item> paidAmountByPlan = dashboardMapper.sumPaidAmountByPlan().stream()
                .map(r -> toItem(r, StatsService::orderPlanName))
                .toList();
        return new DashboardDistributionVO(memberPlans, articlePlatforms, paidAmountByPlan);
    }

    private Map<String, Long> toCountMap(List<DashboardGroupRow> rows) {
        Map<String, Long> map = new HashMap<>();
        for (DashboardGroupRow row : rows) {
            map.put(row.getItemKey(), row.getItemCount() != null ? row.getItemCount() : 0L);
        }
        return map;
    }

    private DashboardDistributionVO.Item toItem(DashboardGroupRow row, Function<String, String> nameMapper) {
        return new DashboardDistributionVO.Item(
                row.getItemKey(),
                nameMapper.apply(row.getItemKey()),
                row.getItemCount() != null ? row.getItemCount() : 0L,
                row.getItemAmount()
        );
    }

    private static String memberPlanName(String key) {
        return switch (key == null ? "" : key) {
            case "basic" -> "基础版";
            case "pro" -> "专业版";
            case "flagship" -> "旗舰版";
            case "monthly" -> "月度会员";
            case "quarterly" -> "季度会员";
            case "yearly" -> "年度会员";
            case "" -> "其他";
            default -> key;
        };
    }

    private static String orderPlanName(String key) {
        return switch (key == null ? "" : key) {
            case "basic" -> "基础版";
            case "pro" -> "专业版";
            case "flagship" -> "旗舰版";
            case "" -> "其他";
            default -> key;
        };
    }

    private static String platformName(String key) {
        return switch (key == null ? "" : key) {
            case "wechat" -> "微信";
            case "xiaohongshu" -> "小红书";
            case "toutiao" -> "头条";
            case "baijiahao" -> "百家号";
            case "douyin" -> "抖音";
            case "zhihu" -> "知乎";
            case "kuaishou" -> "快手";
            case "bilibili" -> "B站";
            case "general" -> "通用";
            case "other" -> "其他";
            case "" -> "未设置";
            default -> key;
        };
    }
}
