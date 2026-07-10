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
     * <p>回 queued 路径会重置 progress_pct=0：让 worker 下一次运行时从头累加进度，
     * 避免 user 端看到「卡在上次失败的 30%」后又被重置。
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
            // 回 queued，释放 lease，重置进度（让下一轮 worker 从头累计）
            task.setStatus(GenerationTaskStatus.QUEUED);
            task.setLockedAt(null);
            task.setLockedBy(null);
            task.setLeaseUntil(null);
            task.setProgressPct(0);
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
     * 实时回写任务进度（worker 每个 stage 完成后回调）。
     *
     * <p>不重试 / 不改 status：纯字段更新，避免和 status 状态机冲突。
     * 失败时仅记录日志（进度丢失不影响任务最终成功 / 失败判定）。
     *
     * @param taskId 任务 ID
     * @param pct    0-100 累计进度
     */
    @Transactional
    public void updateProgress(Long taskId, int pct) {
        if (taskId == null) return;
        int clamped = Math.max(0, Math.min(100, pct));
        try {
            GenerationTask task = new GenerationTask();
            task.setId(taskId);
            task.setProgressPct(clamped);
            mapper.updateById(task);
            log.debug("task={} 进度回写 progressPct={}", taskId, clamped);
        } catch (Exception e) {
            log.warn("task={} 进度回写失败 pct={}: {}", taskId, clamped, e.getMessage());
        }
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
