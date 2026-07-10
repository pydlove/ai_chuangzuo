package com.aichuangzuo.admin.modules.generation.job;

import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.service.GenerationConfigService;
import com.aichuangzuo.admin.modules.generation.service.GenerationRetentionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 创作任务归档定时任务：每天把已过保留期的 completed/failed 任务迁移到 a_generation_history，
 * 并从 a_generation_task 删除，减少主表体积。
 *
 * <p>cron 表达式从 {@link GenerationConfigService} 读取；改后需重启 admin-api 生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationRetentionJob {

    private static final int BATCH_SIZE = 200;
    private static final String DEFAULT_CRON = "0 0 3 * * ?";

    private final GenerationTaskMapper taskMapper;
    private final GenerationRetentionService retentionService;
    private final GenerationConfigService configService;
    private final ThreadPoolTaskScheduler generationTaskScheduler;

    private volatile ScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void init() {
        GenerationConfig cfg = configService.getCurrent();
        String cron = cfg.getRetentionCron() == null || cfg.getRetentionCron().isBlank()
                ? DEFAULT_CRON
                : cfg.getRetentionCron();
        try {
            new CronTrigger(cron);
            scheduledFuture = generationTaskScheduler.schedule(this::run, new CronTrigger(cron));
            log.info("创作任务归档定时任务已注册，cron={}", cron);
        } catch (Exception e) {
            log.warn("创作任务归档 cron 非法，使用默认 {}", DEFAULT_CRON, e);
            scheduledFuture = generationTaskScheduler.schedule(this::run, new CronTrigger(DEFAULT_CRON));
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    public void run() {
        log.info("创作任务归档扫描开始");
        int totalArchived = 0;
        int totalBatches = 0;
        try {
            while (totalBatches < 1000) {
                List<Long> ids = taskMapper.selectExpiredTaskIds(LocalDateTime.now(), BATCH_SIZE);
                if (ids == null || ids.isEmpty()) {
                    break;
                }
                int archived = retentionService.archiveBatch(ids);
                totalArchived += archived;
                totalBatches++;
                if (archived < ids.size()) {
                    log.warn("归档批次异常：期望 {} 条，实际 {} 条", ids.size(), archived);
                    break;
                }
            }
            log.info("创作任务归档扫描完成，共归档 {} 条，批次 {}", totalArchived, totalBatches);
        } catch (Exception e) {
            log.error("创作任务归档扫描异常", e);
        }
    }
}
