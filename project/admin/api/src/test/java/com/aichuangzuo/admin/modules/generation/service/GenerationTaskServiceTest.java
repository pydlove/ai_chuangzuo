package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationTaskServiceTest {

    @Mock
    private GenerationTaskMapper taskMapper;

    @InjectMocks
    private GenerationTaskService taskService;

    @Test
    void claimNext_shouldUpdateClaimedTasksToProcessing() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.QUEUED);

        when(taskMapper.claimBatch(2, "worker-1", 5)).thenReturn(List.of(task));

        List<GenerationTask> claimed = taskService.claimNext(2, "worker-1", 5);

        assertEquals(1, claimed.size());
        assertEquals(GenerationTaskStatus.PROCESSING, task.getStatus());
        assertEquals("worker-1", task.getLockedBy());
        assertNotNull(task.getLeaseUntil());
        verify(taskMapper).updateById(task);
    }

    @Test
    void markCompleted_shouldSetStatusAndCompletedAt() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setFailedReason("old reason");
        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.markCompleted(1L, "ART123");

        assertEquals(GenerationTaskStatus.COMPLETED, task.getStatus());
        assertNull(task.getFailedReason());
        assertNotNull(task.getCompletedAt());
        verify(taskMapper).updateById(task);
    }

    @Test
    void markFailed_shouldAlwaysSetFailedStatus() {
        // 主任务不做自动重试：无论 retryCount 多少都直接置 FAILED
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(0);
        when(taskMapper.selectById(1L)).thenReturn(task);

        GenerationTask after = taskService.markFailed(1L, "network error", false);

        assertEquals(GenerationTaskStatus.FAILED, after.getStatus());
        assertEquals(1, after.getRetryCount());  // 审计计数：第 1 次失败
        assertNotNull(after.getCompletedAt());
        verify(taskMapper).updateById(task);
    }

    @Test
    void markFailed_shouldIncrementRetryCountForAudit() {
        // retryCount 仅作审计计数（已失败几次），不触发任何回 QUEUED 行为
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(3);
        when(taskMapper.selectById(1L)).thenReturn(task);

        GenerationTask after = taskService.markFailed(1L, "final error", false);

        assertEquals(GenerationTaskStatus.FAILED, after.getStatus());
        assertEquals(4, after.getRetryCount());
        assertNotNull(after.getCompletedAt());
        assertEquals("final error", after.getFailedReason());
        verify(taskMapper).updateById(task);
    }

    @Test
    void markFailed_shouldKeepProgressSnapshotForOps() {
        // 失败时保留 progressPct 快照，便于运维排查卡在哪一阶段
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(1);
        task.setProgressPct(30);
        when(taskMapper.selectById(1L)).thenReturn(task);

        GenerationTask after = taskService.markFailed(1L, "stage 4 AI 故障", false);

        assertEquals(GenerationTaskStatus.FAILED, after.getStatus());
        assertEquals(2, after.getRetryCount());
        assertEquals(30, after.getProgressPct());  // 进度快照保留
        verify(taskMapper).updateById(task);
    }

    @Test
    void updateProgress_shouldClampAndPersist() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);

        taskService.updateProgress(1L, 50);

        verify(taskMapper).updateById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(50, captor.getValue().getProgressPct());
        // worker 线程无登录上下文，updatedBy 兜底置 0，否则 NOT NULL 列报错
        assertEquals(0L, captor.getValue().getUpdatedBy());
    }

    @Test
    void updateProgress_shouldPreserveLeaseFields() {
        // 回归：裸实体局部更新曾把 lockedAt/lockedBy/leaseUntil（IGNORED 策略）写成 NULL，
        // 导致 releaseExpiredLeases 永远回收不了 → 任务卡死
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setLockedBy("worker-1");
        LocalDateTime lockedAt = LocalDateTime.now();
        LocalDateTime leaseUntil = lockedAt.plusMinutes(5);
        task.setLockedAt(lockedAt);
        task.setLeaseUntil(leaseUntil);
        task.setUpdatedBy(0L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);

        taskService.updateProgress(1L, 30);

        verify(taskMapper).updateById(captor.capture());
        assertEquals("worker-1", captor.getValue().getLockedBy());
        assertEquals(lockedAt, captor.getValue().getLockedAt());
        assertEquals(leaseUntil, captor.getValue().getLeaseUntil());
    }

    @Test
    void updateProgress_shouldClampOverRange() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);

        taskService.updateProgress(1L, 150);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(100, captor.getValue().getProgressPct());
    }

    @Test
    void updateProgress_shouldClampNegative() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);

        taskService.updateProgress(1L, -10);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getProgressPct());
    }

    @Test
    void updateProgress_shouldNoopWhenTaskIdNull() {
        taskService.updateProgress(null, 50);
        verify(taskMapper, never()).updateById(any(GenerationTask.class));
    }

    @Test
    void updateProgress_shouldSkipWhenTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);
        taskService.updateProgress(99L, 50);
        verify(taskMapper, never()).updateById(any(GenerationTask.class));
    }

    @Test
    void updateProgress_shouldSwallowMapperException() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.updateById(any(GenerationTask.class))).thenThrow(new RuntimeException("db error"));
        // 进度回写失败不应抛出（不影响主流程）
        assertDoesNotThrow(() -> taskService.updateProgress(1L, 50));
    }

    @Test
    void markFailed_shouldTruncateOverlongReason() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        when(taskMapper.selectById(1L)).thenReturn(task);

        String longReason = "AI 返回 JSON 解析失败: " + "x".repeat(2000);
        taskService.markFailed(1L, longReason, false);

        assertNotNull(task.getFailedReason());
        assertEquals(512, task.getFailedReason().length());
        assertTrue(task.getFailedReason().startsWith("AI 返回 JSON 解析失败"));
    }

    @Test
    void releaseExpiredLeases_shouldReturnUpdatedRows() {
        when(taskMapper.releaseExpiredLeases(any(LocalDateTime.class))).thenReturn(2);

        int rows = taskService.releaseExpiredLeases();

        assertEquals(2, rows);
        verify(taskMapper).releaseExpiredLeases(any(LocalDateTime.class));
    }

    @Test
    void renewLease_shouldExtendLeaseUntilByMinutes() {
        when(taskMapper.renewLease(eq(1L), eq("worker-1"), any(LocalDateTime.class))).thenReturn(1);

        LocalDateTime before = LocalDateTime.now();
        taskService.renewLease(1L, "worker-1", 5);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(taskMapper).renewLease(eq(1L), eq("worker-1"), captor.capture());
        // 续约目标时间应在 now+5min 附近（允许秒级误差）
        assertTrue(captor.getValue().isAfter(before.plusMinutes(4)));
        assertTrue(captor.getValue().isBefore(LocalDateTime.now().plusMinutes(6)));
    }

    @Test
    void renewLease_shouldSwallowMapperException() {
        when(taskMapper.renewLease(any(), any(), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("db error"));
        // 心跳失败不应抛出（不影响主流程）
        assertDoesNotThrow(() -> taskService.renewLease(1L, "worker-1", 5));
    }

    @Test
    void renewLease_shouldNoopWhenTaskIdNull() {
        taskService.renewLease(null, "worker-1", 5);
        verify(taskMapper, never()).renewLease(any(), any(), any(LocalDateTime.class));
    }

    @Test
    void renewLease_shouldNoopWhenWorkerIdNull() {
        // workerId 为 null 时 SQL 守卫必然不匹配，直接不调 mapper
        taskService.renewLease(1L, null, 5);
        verify(taskMapper, never()).renewLease(any(), any(), any(LocalDateTime.class));
    }
}
