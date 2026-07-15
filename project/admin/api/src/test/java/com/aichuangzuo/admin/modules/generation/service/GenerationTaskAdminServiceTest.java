package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.GenerationTaskListRow;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminPageVO;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationTaskAdminServiceTest {

    @Mock
    private GenerationTaskMapper taskMapper;

    @Mock
    private GenerationCallLogMapper callLogMapper;

    @InjectMocks
    private GenerationTaskAdminService service;

    private GenerationTaskListRow sampleRow() {
        GenerationTaskListRow r = new GenerationTaskListRow();
        r.setId(1L);
        r.setBizNo("GA1");
        r.setUserId(7L);
        r.setUserNickname("小王");
        r.setStatus(1);
        r.setWordLimitTarget(1500);
        r.setRetryCount(0);
        return r;
    }

    @Test
    void list_shouldDelegateToMapperAndWrapInVo() {
        GenerationTaskQueryRequest req = new GenerationTaskQueryRequest();
        req.setStatus(1);
        req.setKeyword("  小王  ");
        req.setPage(1);
        req.setPageSize(20);

        when(taskMapper.selectAdminList(eq(1), eq("小王"), eq(0L), eq(20)))
                .thenReturn(List.of(sampleRow()));
        when(taskMapper.countAdminList(eq(1), eq("小王"))).thenReturn(1L);
        when(callLogMapper.sumTokensByTaskIds(any())).thenReturn(List.of());

        GenerationTaskAdminPageVO vo = service.list(req);

        assertNotNull(vo);
        assertEquals(1, vo.getList().size());
        assertEquals(1L, vo.getTotal());
        assertEquals(1L, vo.getPage());
        assertEquals(20L, vo.getPageSize());
        assertEquals("小王", vo.getList().get(0).getUserNickname());
    }

    @Test
    void list_shouldPassNullKeywordWhenBlank() {
        GenerationTaskQueryRequest req = new GenerationTaskQueryRequest();
        req.setStatus(0);
        req.setKeyword("   ");

        when(taskMapper.selectAdminList(eq(0), eq(null), anyLong(), anyInt())).thenReturn(List.of());
        when(taskMapper.countAdminList(eq(0), eq(null))).thenReturn(0L);

        service.list(req);

        verify(taskMapper).selectAdminList(eq(0), eq(null), anyLong(), anyInt());
    }

    @Test
    void stopTask_shouldThrowWhenTaskIsCompleted() {
        GenerationTask task = new GenerationTask();
        task.setId(20L);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        when(taskMapper.selectById(20L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.stopTask(20L));
        verify(taskMapper, never()).updateById((GenerationTask) any());
    }

    @Test
    void stopTask_shouldThrowWhenTaskIsFailed() {
        GenerationTask task = new GenerationTask();
        task.setId(22L);
        task.setStatus(GenerationTaskStatus.FAILED);
        when(taskMapper.selectById(22L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.stopTask(22L));
        verify(taskMapper, never()).updateById((GenerationTask) any());
    }

    @Test
    void stopTask_shouldMarkFailedWhenProcessing() {
        GenerationTask task = new GenerationTask();
        task.setId(21L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setRetryCount(0);
        task.setLockedBy("worker-1");
        task.setLeaseUntil(java.time.LocalDateTime.now());
        when(taskMapper.selectById(21L)).thenReturn(task);

        service.stopTask(21L);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        GenerationTask updated = captor.getValue();
        assertEquals(GenerationTaskStatus.FAILED, updated.getStatus());
        assertEquals("管理员手动停止", updated.getFailedReason());
        assertNull(updated.getLockedBy());
        assertNull(updated.getLeaseUntil());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void stopTask_shouldMarkFailedWhenQueued() {
        GenerationTask task = new GenerationTask();
        task.setId(23L);
        task.setStatus(GenerationTaskStatus.QUEUED);
        when(taskMapper.selectById(23L)).thenReturn(task);

        service.stopTask(23L);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        GenerationTask updated = captor.getValue();
        assertEquals(GenerationTaskStatus.FAILED, updated.getStatus());
        assertEquals("管理员手动停止", updated.getFailedReason());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void manualMarkFailed_shouldSetFailedAndCompletedAt() {
        GenerationTask task = new GenerationTask();
        task.setId(30L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        task.setLockedBy("worker-1");
        when(taskMapper.selectById(30L)).thenReturn(task);

        service.manualMarkFailed(30L, "  bad request  ");

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        GenerationTask updated = captor.getValue();
        assertEquals(GenerationTaskStatus.FAILED, updated.getStatus());
        assertEquals("  bad request  ", updated.getFailedReason());
        assertNotNull(updated.getCompletedAt());
    }
}
