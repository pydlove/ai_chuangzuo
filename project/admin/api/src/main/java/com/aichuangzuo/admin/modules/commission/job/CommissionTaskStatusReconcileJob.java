package com.aichuangzuo.admin.modules.commission.job;

import com.aichuangzuo.admin.modules.commission.service.AdminCommissionService;
import com.aichuangzuo.admin.modules.scheduler.annotation.ScheduledTask;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionTaskStatusReconcileJob {

    private final AdminCommissionService commissionService;
    private final ScheduledTaskExecutor scheduledTaskExecutor;

    @ScheduledTask(key = "commission_task_status_reconcile", name = "约稿任务状态校正", description = "每天凌晨把过期的 SUBMISSION/REVIEW 约稿任务状态推进到下一状态", triggerType = "cron", expression = "0 10 3 * * *", sortOrder = 30)
    @Scheduled(cron = "0 10 3 * * *")
    public void run() {
        scheduledTaskExecutor.executeAuto("commission_task_status_reconcile", () -> {
            int changed = commissionService.reconcileTaskStatus();
            if (changed > 0) {
                log.info("约稿任务状态校正完成，变更 {} 条", changed);
            }
        });
    }
}
