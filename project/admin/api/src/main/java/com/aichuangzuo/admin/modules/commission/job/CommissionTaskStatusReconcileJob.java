package com.aichuangzuo.admin.modules.commission.job;

import com.aichuangzuo.admin.modules.commission.service.AdminCommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionTaskStatusReconcileJob {

    private final AdminCommissionService commissionService;

    @Scheduled(cron = "0 10 3 * * *")
    public void run() {
        int changed = commissionService.reconcileTaskStatus();
        if (changed > 0) {
            log.info("约稿任务状态校正完成，变更 {} 条", changed);
        }
    }
}
