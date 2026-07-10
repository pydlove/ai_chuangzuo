package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 端-生成任务服务：worker 抢占 / 标记状态 / 释放 lease。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskService {

    private final GenerationTaskMapper mapper;

    /**
     * 抢占一批任务（FOR UPDATE SKIP LOCKED）。
     * 事务内 SELECT + 立即 UPDATE status=PROCESSING + 设置 lease_until，避免被其他 worker 重提。
     *
     * @param limit         抢占数量上限
     * @param workerId      worker 实例 ID
     * @param leaseMinutes  lease 持续分钟（5）
     */
    @Transactional
    public List<GenerationTask> claimNext(int limit, String workerId, int leaseMinutes) {
        LocalDateTime now = LocalDateTime.now();
        List<GenerationTask> tasks = mapper.claimBatch(limit, workerId, leaseMinutes);
        if (tasks.isEmpty()) return tasks;

        LocalDateTime leaseUntil = now.plusMinutes(leaseMinutes);
        for (GenerationTask task : tasks) {
            task.setStatus(GenerationTaskStatus.PROCESSING);
            task.setLockedAt(now);
            task.setLockedBy(workerId);
            task.setLeaseUntil(leaseUntil);
            mapper.updateById(task);
        }
        log.debug("worker={} claimed {} tasks", workerId, tasks.size());
        return tasks;
    }

    /**
     * 标记任务完成。articleBizNo 关联刚生成的 u_article.biz_no。
     */
    @Transactional
    public void markCompleted(Long taskId, String articleBizNo) {
        GenerationTask task = requireById(taskId);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setFailedReason(null);
        mapper.updateById(task);
        log.info("task={} completed, articleBizNo={}", taskId, articleBizNo);
    }

    /**
     * 标记任务失败。若 retry_count < max_retry，回 queued 让 worker 再试；否则置 FAILED。
     *
     * @param refundRequired  最终失败时是否需要退额度（completed-or-failed 时由调用方决定）
     */
    @Transactional
    public GenerationTask markFailed(Long taskId, String reason, boolean refundRequired) {
        GenerationTask task = requireById(taskId);
        task.setFailedReason(reason);

        int nextRetry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        task.setRetryCount(nextRetry);

        int max = task.getMaxRetry() == null ? 3 : task.getMaxRetry();
        if (nextRetry <= max) {
            // 回 queued，释放 lease
            task.setStatus(GenerationTaskStatus.QUEUED);
            task.setLockedAt(null);
            task.setLockedBy(null);
            task.setLeaseUntil(null);
            mapper.updateById(task);
            log.info("task={} retry {}/{}, queued back, reason={}", taskId, nextRetry, max, reason);
        } else {
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            mapper.updateById(task);
            log.warn("task={} 最终失败 retry={}/{}, reason={}", taskId, nextRetry, max, reason);
        }
        return task;
    }

    /**
     * 释放 lease 已过期的 processing 任务回 queued。
     * 返回受影响行数（监控用）。
     */
    @Transactional
    public int releaseExpiredLeases() {
        int rows = mapper.releaseExpiredLeases(LocalDateTime.now());
        if (rows > 0) log.info("回收 lease 过期任务 {} 行，回 queued", rows);
        return rows;
    }

    private GenerationTask requireById(Long id) {
        GenerationTask task = mapper.selectById(id);
        if (task == null) throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_NOT_FOUND);
        return task;
    }
}
