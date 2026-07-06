package com.aichuangzuo.admin.modules.hotsearch.job;

import com.aichuangzuo.admin.modules.hotsearch.crawler.HotSearchFetcher;
import com.aichuangzuo.admin.modules.hotsearch.crawler.HotSearchItem;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.properties.HotSearchProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日热搜定时抓取任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotSearchCrawlJob {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;
    private final List<HotSearchFetcher> fetchers;
    private final HotSearchProperties properties;

    /**
     * 每天凌晨 2 点执行；cron 可通过配置覆盖。
     */
    @Scheduled(cron = "${hot-search.cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void crawl() {
        if (!properties.isCrawlEnabled()) {
            log.info("热搜定时抓取已关闭");
            return;
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<HotSearchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotSearchPlatform::getEnabled, 1)
                .orderByAsc(HotSearchPlatform::getSortOrder);
        List<HotSearchPlatform> platforms = platformMapper.selectList(wrapper);

        for (HotSearchPlatform platform : platforms) {
            if (!properties.getPlatforms().isEmpty()
                    && !properties.getPlatforms().contains(platform.getCode())) {
                continue;
            }
            crawlPlatform(platform, today);
        }

        log.info("热搜定时抓取完成，日期={}", today);
    }

    private void crawlPlatform(HotSearchPlatform platform, LocalDate date) {
        HotSearchFetcher fetcher = fetchers.stream()
                .filter(f -> f.supports(platform))
                .findFirst()
                .orElse(null);
        if (fetcher == null) {
            log.warn("平台 [{}] 无可用抓取器", platform.getCode());
            return;
        }

        List<HotSearchItem> items = fetcher.fetch(platform);
        if (items == null || items.isEmpty()) {
            log.warn("平台 [{}] 未抓取到数据", platform.getCode());
            return;
        }

        LambdaQueryWrapper<HotSearchDaily> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(HotSearchDaily::getPlatformCode, platform.getCode())
                .eq(HotSearchDaily::getSnapshotDate, date);
        dailyMapper.delete(deleteWrapper);

        for (HotSearchItem item : items) {
            HotSearchDaily daily = new HotSearchDaily();
            daily.setPlatformCode(platform.getCode());
            daily.setRankNum(item.getRank());
            daily.setTitle(item.getTitle());
            daily.setHotValue(item.getHotValue());
            daily.setUrl(item.getUrl());
            daily.setSearchCount(item.getSearchCount());
            daily.setSnapshotDate(date);
            dailyMapper.insert(daily);
        }

        log.info("平台 [{}] 抓取完成，写入 {} 条", platform.getCode(), items.size());
    }
}
