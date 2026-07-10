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
    void markFailed_shouldQueueBackWhenRetryUnderMax() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(0);
        task.setMaxRetry(3);
        when(taskMapper.selectById(1L)).thenReturn(task);

        GenerationTask after = taskService.markFailed(1L, "network error", false);

        assertEquals(GenerationTaskStatus.QUEUED, after.getStatus());
        assertEquals(1, after.getRetryCount());
        assertNull(after.getLeaseUntil());
        assertNull(after.getLockedBy());
        verify(taskMapper).updateById(task);
    }

    @Test
    void markFailed_shouldSetFailedWhenRetryExceedsMax() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(3);
        task.setMaxRetry(3);
        when(taskMapper.selectById(1L)).thenReturn(task);

        GenerationTask after = taskService.markFailed(1L, "final error", false);

        assertEquals(GenerationTaskStatus.FAILED, after.getStatus());
        assertEquals(4, after.getRetryCount());
        assertNotNull(after.getCompletedAt());
        assertEquals("final error", after.getFailedReason());
        verify(taskMapper).updateById(task);
    }

    @Test
    void releaseExpiredLeases_shouldReturnUpdatedRows() {
        when(taskMapper.releaseExpiredLeases(any(LocalDateTime.class))).thenReturn(2);

        int rows = taskService.releaseExpiredLeases();

        assertEquals(2, rows);
        verify(taskMapper).releaseExpiredLeases(any(LocalDateTime.class));
    }
}
