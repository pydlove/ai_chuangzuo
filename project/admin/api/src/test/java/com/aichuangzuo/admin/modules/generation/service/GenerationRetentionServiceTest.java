package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.GenerationHistoryMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.shared.entity.GenerationHistory;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationRetentionServiceTest {

    @Mock
    private GenerationTaskMapper taskMapper;

    @Mock
    private GenerationHistoryMapper historyMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationRetentionService retentionService;

    @Test
    void archiveBatch_shouldInsertHistoryAndDeleteTasks() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setBizNo("GA123");
        task.setTargetUserId(2L);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        task.setRetryCount(0);
        task.setWordLimitTarget(1500);
        task.setModelConfigId(3L);
        task.setPromptTemplateId(4L);
        task.setInputParam("{\"title\":\"测试标题\"}");
        task.setCreatedAt(LocalDateTime.now().minusDays(31));
        task.setCompletedAt(LocalDateTime.now().minusDays(30));

        when(taskMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(task));

        int archived = retentionService.archiveBatch(List.of(1L));

        assertEquals(1, archived);
        ArgumentCaptor<GenerationHistory> captor = ArgumentCaptor.forClass(GenerationHistory.class);
        verify(historyMapper).insert(captor.capture());
        verify(taskMapper).deleteBatchIds(List.of(1L));

        GenerationHistory history = captor.getValue();
        assertEquals(Long.valueOf(1L), history.getTaskId());
        assertEquals("GA123", history.getBizNo());
        assertEquals(2, history.getTargetUserId());
        assertEquals(2, history.getStatus());
        assertEquals("测试标题", history.getTitle());
        assertNotNull(history.getArchivedAt());
    }

    @Test
    void archiveBatch_shouldReturnZeroForEmptyIds() {
        assertEquals(0, retentionService.archiveBatch(List.of()));
        assertEquals(0, retentionService.archiveBatch(null));
        verifyNoInteractions(taskMapper, historyMapper);
    }
}
