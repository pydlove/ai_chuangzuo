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

    /** failed_reason 列长 VARCHAR(512)；错误消息常嵌 AI 原文，超长需截断否则 update 直接失败。 */
    private static final int MAX_FAILED_REASON_LEN = 512;

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
        task.setFailedReason(truncateReason(reason));

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
     * <p>必须先 selectById 再 updateById：lockedAt/lockedBy/leaseUntil 是
     * {@code updateStrategy=IGNORED}（永远进 SET 子句），用裸实体局部更新会把
     * lease 三个字段写成 NULL，导致 releaseExpiredLeases 永远回收不了该任务。
     *
     * <p>失败时仅记录日志（进度丢失不影响任务最终成功 / 失败判定）。
     *
     * @param taskId 任务 ID
     * @param pct    0-100 累计进度
     */
    @Transactional
    public void updateProgress(Long taskId, int pct) {
        if (taskId == null) return;
        int clamped = Math.max(0, Math.min(100, pct));
        try {
            GenerationTask task = mapper.selectById(taskId);
            if (task == null) {
                log.warn("task={} 进度回写跳过：任务不存在", taskId);
                return;
            }
            task.setProgressPct(clamped);
            // worker 线程无登录上下文；库里老数据 updatedBy 不会为 null，这里仅兜底
            if (task.getUpdatedBy() == null) task.setUpdatedBy(0L);
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

    /**
     * 续约 lease（心跳）：worker 每完成一个 stage 调一次，把 lease_until 顺延
     * {@code leaseMinutes} 分钟。慢模型（MiniMax-M3）跑全流程可能超过单个 lease
     * 周期，心跳让活跃 worker 始终持有任务，避免被同池其他 worker 误判卡死回收、
     * 重复处理。
     *
     * <p>底层 SQL 带 {@code status=1 AND locked_by=workerId} 守卫：任务已被回收 /
     * 易主时影响 0 行，不会复活。失败仅记日志（续约失败不影响主流程）。
     *
     * @param taskId       任务 ID
     * @param workerId     持有该任务的 worker（取 task.lockedBy，非当前配置）
     * @param leaseMinutes 顺延分钟数
     */
    public void renewLease(Long taskId, String workerId, int leaseMinutes) {
        if (taskId == null || workerId == null) return;
        try {
            int rows = mapper.renewLease(taskId, workerId,
                    LocalDateTime.now().plusMinutes(leaseMinutes));
            if (rows == 0) {
                log.debug("task={} 续约跳过：非 processing 或非 {} 持有", taskId, workerId);
            }
        } catch (Exception e) {
            log.warn("task={} 续约失败: {}", taskId, e.getMessage());
        }
    }

    private GenerationTask requireById(Long id) {
        GenerationTask task = mapper.selectById(id);
        if (task == null) throw new BusinessException(AdminGenerationErrorCode.GENERATION_TASK_NOT_FOUND);
        return task;
    }

    private static String truncateReason(String reason) {
        if (reason == null || reason.length() <= MAX_FAILED_REASON_LEN) return reason;
        return reason.substring(0, MAX_FAILED_REASON_LEN);
    }
}
