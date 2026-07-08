package com.aichuangzuo.admin.modules.reminder.job;

import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.event.ReminderConfigChangedEvent;
import com.aichuangzuo.admin.modules.reminder.service.ExpireReminderService;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.admin.modules.reminder.vo.ExpiringUserVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

/**
 * 到期提醒定时任务。
 * 使用 ThreadPoolTaskScheduler 动态重建 Trigger，cron 由配置 notify_hour 拼出。
 * 配置变更通过事件触发 reschedule。
 */
@Slf4j
@Component
public class ExpireReminderJob {

    private static final int SCAN_PAGE_SIZE = 200;

    private final ReminderConfigService configService;
    private final ExpireReminderService reminderService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private volatile ScheduledFuture<?> scheduledFuture;

    public ExpireReminderJob(ReminderConfigService configService,
                             ExpireReminderService reminderService,
                             ThreadPoolTaskScheduler reminderTaskScheduler) {
        this.configService = configService;
        this.reminderService = reminderService;
        this.taskScheduler = reminderTaskScheduler;
    }

    @EventListener
    public void onConfigChanged(ReminderConfigChangedEvent event) {
        log.info("收到提醒配置变更事件，adminId={}，开始 reschedule", event.adminId());
        reschedule();
    }

    @PostConstruct
    public void init() {
        configService.syncFromProperties();
        reschedule();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        var cfg = configService.getConfig();
        if (cfg.getEnabled() == null || cfg.getEnabled() == 0) {
            log.info("到期提醒定时任务已停用");
            return;
        }
        int hour = cfg.getNotifyHour() == null ? 9 : cfg.getNotifyHour();
        String cron = String.format("0 0 %d * * ?", hour);
        try {
            new CronTrigger(cron);
            scheduledFuture = taskScheduler.schedule(this::run, new CronTrigger(cron));
            log.info("到期提醒定时任务已注册，cron={}", cron);
        } catch (Exception e) {
            log.warn("到期提醒 cron 非法，注册失败: {}", cron);
        }
    }

    /**
     * 定时任务入口：分页扫描命中用户，逐个调 remindUser("auto")。
     */
    public void run() {
        try {
            int advanceDays = configService.getConfig().getAdvanceDays();
            long page = 1;
            while (true) {
                ExpiringUserPageQuery query = new ExpiringUserPageQuery();
                query.setAdvanceDays(advanceDays);
                query.setPage(page);
                query.setSize((long) SCAN_PAGE_SIZE);
                ExpireReminderService.PageResult pr = reminderService.pageExpiringUsers(query);
                if (pr.items().isEmpty()) break;
                for (ExpiringUserVO vo : pr.items()) {
                    try {
                        reminderService.remindUser(vo.getUserId(), "auto");
                    } catch (Exception e) {
                        log.warn("自动提醒失败 userId={}, reason={}", vo.getUserId(), e.getMessage());
                    }
                }
                if (pr.items().size() < SCAN_PAGE_SIZE) break;
                page++;
                if (page > 100) {
                    log.warn("到期提醒扫描超过 100 页，强制结束");
                    break;
                }
            }
            log.info("到期提醒定时扫描完成，advanceDays={}", advanceDays);
        } catch (Exception e) {
            log.error("到期提醒定时任务异常", e);
        }
    }
}
