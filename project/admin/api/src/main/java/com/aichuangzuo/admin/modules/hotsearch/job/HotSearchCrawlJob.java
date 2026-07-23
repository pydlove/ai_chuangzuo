package com.aichuangzuo.admin.modules.hotsearch.job;

import com.aichuangzuo.admin.modules.hotsearch.crawler.HotSearchFetcher;
import com.aichuangzuo.admin.modules.hotsearch.crawler.HotSearchItem;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchDaily;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;
import com.aichuangzuo.admin.modules.hotsearch.enums.AdminHotSearchErrorCode;
import com.aichuangzuo.admin.modules.hotsearch.event.HotSearchConfigChangedEvent;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchDailyMapper;
import com.aichuangzuo.admin.modules.hotsearch.mapper.HotSearchPlatformMapper;
import com.aichuangzuo.admin.modules.hotsearch.service.HotSearchConfigService;
import com.aichuangzuo.admin.modules.hotsearch.vo.CrawlResultVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.LastRunVO;
import com.aichuangzuo.admin.modules.hotsearch.vo.PlatformCrawlResultVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 每日热搜定时抓取任务。
 * 使用 ThreadPoolTaskScheduler 动态重建 Trigger，cron 表达式来自 DB 配置。
 */
@Slf4j
@Component
public class HotSearchCrawlJob {

    private final HotSearchPlatformMapper platformMapper;
    private final HotSearchDailyMapper dailyMapper;
    private final List<HotSearchFetcher> fetchers;
    private final HotSearchConfigService configService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile LastRunVO lastRun = new LastRunVO();

    public HotSearchCrawlJob(HotSearchPlatformMapper platformMapper,
                             HotSearchDailyMapper dailyMapper,
                             List<HotSearchFetcher> fetchers,
                             HotSearchConfigService configService,
                             @Qualifier("hotSearchTaskScheduler") ThreadPoolTaskScheduler hotSearchTaskScheduler) {
        this.platformMapper = platformMapper;
        this.dailyMapper = dailyMapper;
        this.fetchers = fetchers;
        this.configService = configService;
        this.taskScheduler = hotSearchTaskScheduler;
    }

    /**
     * 监听配置变更事件，事务提交后再 reschedule，避免读到未提交的旧配置。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfigChanged(HotSearchConfigChangedEvent event) {
        log.info("收到配置变更事件，adminId={}，开始 reschedule", event.adminId());
        reschedule();
    }

    @PostConstruct
    public void init() {
        configService.syncFromProperties();
        reschedule();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
    }

    /**
     * 根据当前 DB 配置重建定时 Trigger。
     */
    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        var cfg = configService.getConfig();
        if (cfg.getEnabled() != null && cfg.getEnabled() == 1) {
            try {
                ZoneId zone = ZoneId.of("Asia/Shanghai");
                new CronTrigger(cfg.getCron(), zone); // 校验
                scheduledFuture = taskScheduler.schedule(this::crawl, new CronTrigger(cfg.getCron(), zone));
                log.info("热搜定时抓取已注册，cron={}，时区={}", cfg.getCron(), zone);
            } catch (Exception e) {
                log.warn("热搜 cron 表达式非法，注册失败: {}", cfg.getCron(), e);
            }
        } else {
            log.info("热搜定时抓取已停用");
        }
    }

    /**
     * 定时任务入口。
     */
    public void crawl() {
        log.info("热搜定时抓取任务开始执行");
        try {
            crawlAll();
        } catch (Exception e) {
            log.error("定时抓取异常", e);
        }
        log.info("热搜定时抓取任务执行结束");
    }

    /**
     * 同步抓取所有启用平台，返回每平台结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public CrawlResultVO crawlAll() {
        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<HotSearchPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotSearchPlatform::getEnabled, 1)
                .orderByAsc(HotSearchPlatform::getSortOrder);
        List<HotSearchPlatform> platforms = platformMapper.selectList(wrapper);

        List<PlatformCrawlResultVO> results = new ArrayList<>();
        for (HotSearchPlatform platform : platforms) {
            results.add(crawlPlatform(platform, today));
        }

        CrawlResultVO result = new CrawlResultVO();
        result.setResults(results);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());

        updateLastRun(results);
        log.info("热搜抓取完成，平台数={}", results.size());
        return result;
    }

    /**
     * 重抓指定平台当日。
     */
    @Transactional(rollbackFor = Exception.class)
    public CrawlResultVO recrawlPlatform(String platformCode) {
        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();
        HotSearchPlatform platform = platformMapper.selectOne(
                new LambdaQueryWrapper<HotSearchPlatform>().eq(HotSearchPlatform::getCode, platformCode));
        if (platform == null) {
            throw new BusinessException(AdminHotSearchErrorCode.PLATFORM_NOT_FOUND);
        }
        List<PlatformCrawlResultVO> results = List.of(crawlPlatform(platform, today));
        CrawlResultVO result = new CrawlResultVO();
        result.setResults(results);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        updateLastRun(results);
        return result;
    }

    public LastRunVO getLastRun() {
        return lastRun;
    }

    private PlatformCrawlResultVO crawlPlatform(HotSearchPlatform platform, LocalDate date) {
        PlatformCrawlResultVO vo = new PlatformCrawlResultVO();
        vo.setPlatformCode(platform.getCode());
        vo.setPlatformName(platform.getName());

        HotSearchFetcher fetcher = fetchers.stream()
                .filter(f -> f.supports(platform))
                .findFirst()
                .orElse(null);
        if (fetcher == null) {
            vo.setSuccess(false);
            vo.setFetched(0);
            vo.setError("无抓取器");
            log.warn("平台 [{}] 无可用抓取器", platform.getCode());
            return vo;
        }

        try {
            List<HotSearchItem> items = fetcher.fetch(platform);
            if (items == null || items.isEmpty()) {
                vo.setSuccess(false);
                vo.setFetched(0);
                vo.setError("未抓取到数据");
                log.warn("平台 [{}] 未抓取到数据", platform.getCode());
                return vo;
            }
            // 删除旧数据
            dailyMapper.delete(new LambdaQueryWrapper<HotSearchDaily>()
                    .eq(HotSearchDaily::getPlatformCode, platform.getCode())
                    .eq(HotSearchDaily::getSnapshotDate, date));
            // 写入新数据
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
            vo.setSuccess(true);
            vo.setFetched(items.size());
            log.info("平台 [{}] 抓取完成，写入 {} 条", platform.getCode(), items.size());
            return vo;
        } catch (Exception e) {
            vo.setSuccess(false);
            vo.setFetched(0);
            vo.setError(e.getMessage());
            log.warn("平台 [{}] 抓取失败: {}", platform.getCode(), e.getMessage());
            return vo;
        }
    }

    private void updateLastRun(List<PlatformCrawlResultVO> results) {
        LastRunVO run = new LastRunVO();
        run.setLastRunAt(Instant.now());
        run.setResults(results);
        run.setSuccessCount((int) results.stream().filter(PlatformCrawlResultVO::isSuccess).count());
        run.setFailCount(results.size() - run.getSuccessCount());
        run.setTotalFetched(results.stream().mapToInt(PlatformCrawlResultVO::getFetched).sum());
        this.lastRun = run;
    }
}
