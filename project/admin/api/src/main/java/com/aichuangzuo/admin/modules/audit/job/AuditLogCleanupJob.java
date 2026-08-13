package com.aichuangzuo.admin.modules.audit.job;

import com.aichuangzuo.admin.modules.audit.client.UserAuditLogClient;
import com.aichuangzuo.admin.modules.audit.event.AuditConfigChangedEvent;
import com.aichuangzuo.admin.modules.audit.service.AuditConfigService;
import com.aichuangzuo.admin.modules.scheduler.annotation.ScheduledTask;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

/**
 * 审计日志清理定时任务。
 * 按 a_audit_config.cleanup_cron 触发，调用 user-api 清理超过 retention_days 的日志。
 */
@Slf4j
@Component
public class AuditLogCleanupJob {

    private final AuditConfigService auditConfigService;
    private final UserAuditLogClient userAuditLogClient;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final ScheduledTaskExecutor scheduledTaskExecutor;

    private volatile ScheduledFuture<?> scheduledFuture;

    public AuditLogCleanupJob(AuditConfigService auditConfigService,
                              UserAuditLogClient userAuditLogClient,
                              ThreadPoolTaskScheduler auditLogTaskScheduler,
                              ScheduledTaskExecutor scheduledTaskExecutor) {
        this.auditConfigService = auditConfigService;
        this.userAuditLogClient = userAuditLogClient;
        this.taskScheduler = auditLogTaskScheduler;
        this.scheduledTaskExecutor = scheduledTaskExecutor;
    }

    @PostConstruct
    public void init() {
        reschedule();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    @EventListener
    public void onConfigChanged(AuditConfigChangedEvent event) {
        log.info("收到审计日志配置变更事件，adminId={}，开始 reschedule", event.getUpdatedBy());
        reschedule();
    }

    public synchronized void reschedule() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        var cfg = auditConfigService.getConfig();
        String cron = cfg.getCleanupCron();
        if (cron == null || cron.isBlank()) {
            cron = "0 0 3 * * ?";
        }
        try {
            new CronTrigger(cron);
            scheduledFuture = taskScheduler.schedule(this::run, new CronTrigger(cron));
            log.info("审计日志清理定时任务已注册，cron={}，retentionDays={}", cron, cfg.getRetentionDays());
        } catch (Exception e) {
            log.warn("审计日志清理 cron 非法，注册失败: {}", cron);
        }
    }

    @ScheduledTask(key = "audit_log_cleanup", name = "用户审计日志清理", description = "按保留天数清理 user-api 的 u_user_audit_log", triggerType = "cron", expression = "0 0 3 * * ?", sortOrder = 50)
    public void run() {
        scheduledTaskExecutor.executeAuto("audit_log_cleanup", () -> {
            try {
                var cfg = auditConfigService.getConfig();
                int retentionDays = cfg.getRetentionDays() == null ? 30 : cfg.getRetentionDays();
                userAuditLogClient.cleanupLogs(retentionDays);
                log.info("审计日志清理完成，retentionDays={}", retentionDays);
            } catch (Exception e) {
                log.error("审计日志清理定时任务异常", e);
            }
        });
    }
}
