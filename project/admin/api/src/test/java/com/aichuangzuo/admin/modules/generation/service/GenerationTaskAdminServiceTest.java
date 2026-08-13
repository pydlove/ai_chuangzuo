package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.GenerationTaskListRow;
import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.admin.modules.generation.vo.BatchStopGenerationTaskResultVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminPageVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminVO;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationTaskAdminServiceTest {

    @Mock
    private GenerationTaskMapper taskMapper;

    @Mock
    private GenerationCallLogMapper callLogMapper;

    @Mock
    private QuotaRefundInternalClient refundClient;

    @Mock
    private ArticleReadInternalClient articleReadClient;

    @Mock
    private ObjectMapper objectMapper;

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
        verify(refundClient, never()).refund(anyLong(), anyLong());
    }

    @Test
    void stopTask_shouldThrowWhenTaskIsFailed() {
        GenerationTask task = new GenerationTask();
        task.setId(22L);
        task.setStatus(GenerationTaskStatus.FAILED);
        when(taskMapper.selectById(22L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.stopTask(22L));
        verify(taskMapper, never()).updateById((GenerationTask) any());
        verify(refundClient, never()).refund(anyLong(), anyLong());
    }

    @Test
    void stopTask_shouldMarkFailedAndRefundWhenProcessing() {
        GenerationTask task = new GenerationTask();
        task.setId(21L);
        task.setTargetUserId(7L);
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
        verify(refundClient).refund(21L, 7L);
    }

    @Test
    void stopTask_shouldMarkFailedAndRefundWhenQueued() {
        GenerationTask task = new GenerationTask();
        task.setId(23L);
        task.setTargetUserId(8L);
        task.setStatus(GenerationTaskStatus.QUEUED);
        when(taskMapper.selectById(23L)).thenReturn(task);

        service.stopTask(23L);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        GenerationTask updated = captor.getValue();
        assertEquals(GenerationTaskStatus.FAILED, updated.getStatus());
        assertEquals("管理员手动停止", updated.getFailedReason());
        assertNotNull(updated.getCompletedAt());
        verify(refundClient).refund(23L, 8L);
    }

    @Test
    void stopTask_shouldStillStopWhenRefundFails() {
        GenerationTask task = new GenerationTask();
        task.setId(24L);
        task.setTargetUserId(9L);
        task.setStatus(GenerationTaskStatus.QUEUED);
        when(taskMapper.selectById(24L)).thenReturn(task);
        org.mockito.Mockito.doThrow(new RuntimeException("user-api down"))
                .when(refundClient).refund(24L, 9L);

        // 退款失败不影响停止本身
        service.stopTask(24L);

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(GenerationTaskStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void previewArticle_shouldReturnArticleWhenTaskCompleted() {
        GenerationTask task = new GenerationTask();
        task.setId(30L);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        task.setArticleBizNo("A123");
        when(taskMapper.selectById(30L)).thenReturn(task);

        com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO article =
                new com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO();
        article.setBizNo("A123");
        article.setTitle("测试标题");
        article.setBody("正文内容");
        when(articleReadClient.getArticle("A123")).thenReturn(article);

        com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO result = service.previewArticle(30L);

        assertEquals("测试标题", result.getTitle());
        assertEquals("正文内容", result.getBody());
    }

    @Test
    void previewArticle_shouldThrowWhenTaskNotCompleted() {
        GenerationTask task = new GenerationTask();
        task.setId(31L);
        task.setStatus(GenerationTaskStatus.PROCESSING);
        when(taskMapper.selectById(31L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.previewArticle(31L));
        verify(articleReadClient, never()).getArticle(any());
    }

    @Test
    void previewArticle_shouldThrowWhenArticleBizNoMissing() {
        GenerationTask task = new GenerationTask();
        task.setId(32L);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        when(taskMapper.selectById(32L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.previewArticle(32L));
        verify(articleReadClient, never()).getArticle(any());
    }

    @Test
    void toVo_shouldParseInputParamFields() throws Exception {
        String json = "{\"title\":\"测试标题\",\"description\":\"要求\",\"platform\":\"wechat\",\"skillRef\":\"skill-1\",\"template\":\"t1\",\"userSkillPrompt\":\"prompt\"}";
        GenerationTaskListRow r = sampleRow();
        r.setInputParam(json);

        java.util.Map<String, Object> parsed = new java.util.HashMap<>();
        parsed.put("title", "测试标题");
        parsed.put("description", "要求");
        parsed.put("platform", "wechat");
        parsed.put("skillRef", "skill-1");
        parsed.put("template", "t1");
        parsed.put("userSkillPrompt", "prompt");
        when(objectMapper.readValue(eq(json), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(parsed);

        GenerationTaskAdminVO vo = service.toVo(r, LocalDateTime.now(), 0L);

        assertEquals("测试标题", vo.getTitle());
        assertEquals("要求", vo.getDescription());
        assertEquals("wechat", vo.getPlatform());
        assertEquals("skill-1", vo.getSkillRef());
        assertEquals("t1", vo.getTemplate());
        assertEquals("prompt", vo.getUserSkillPrompt());
    }

    @Test
    void toVo_withModelConfig_shouldBuildDisplay() {
        GenerationTaskListRow r = sampleRow();
        r.setModelConfigId(5L);
        r.setModelConfigName("默认key");
        r.setProviderType("minimax");

        GenerationTaskAdminVO vo = service.toVo(r, LocalDateTime.now(), 0L);

        assertEquals("默认key/MiniMax", vo.getModelConfigDisplay());
    }

    @Test
    void toVo_withoutModelConfig_shouldShowDash() {
        GenerationTaskListRow r = sampleRow();
        r.setModelConfigId(null);
        r.setModelConfigName(null);
        r.setProviderType(null);

        GenerationTaskAdminVO vo = service.toVo(r, LocalDateTime.now(), 0L);

        assertEquals("-", vo.getModelConfigDisplay());
    }

    @Test
    void toVo_unknownProvider_shouldUseProviderType() {
        GenerationTaskListRow r = sampleRow();
        r.setModelConfigName("测试key");
        r.setProviderType("openai");

        GenerationTaskAdminVO vo = service.toVo(r, LocalDateTime.now(), 0L);

        assertEquals("测试key/openai", vo.getModelConfigDisplay());
    }

    @Test
    void toVo_completedTask_shouldUseCompletedAtAsEndTime() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0, 0);
        LocalDateTime createdAt = now.minusHours(2);
        LocalDateTime completedAt = createdAt.plusMinutes(30);

        GenerationTaskListRow r = sampleRow();
        r.setStatus(2);
        r.setCreatedAt(createdAt);
        r.setCompletedAt(completedAt);

        GenerationTaskAdminVO vo = service.toVo(r, now, 0L);

        assertEquals(30 * 60, vo.getWaitingSeconds());
    }

    @Test
    void toVo_failedTask_shouldUseCompletedAtAsEndTime() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0, 0);
        LocalDateTime createdAt = now.minusHours(2);
        LocalDateTime completedAt = createdAt.plusMinutes(15);

        GenerationTaskListRow r = sampleRow();
        r.setStatus(3);
        r.setCreatedAt(createdAt);
        r.setCompletedAt(completedAt);

        GenerationTaskAdminVO vo = service.toVo(r, now, 0L);

        assertEquals(15 * 60, vo.getWaitingSeconds());
        assertTrue(vo.getFailedSecondsAgo() > 0);
    }

    @Test
    void toVo_processingTask_shouldUseNowAsEndTime() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0, 0);
        LocalDateTime createdAt = now.minusMinutes(5);

        GenerationTaskListRow r = sampleRow();
        r.setStatus(1);
        r.setCreatedAt(createdAt);
        r.setCompletedAt(null);

        GenerationTaskAdminVO vo = service.toVo(r, now, 0L);

        assertEquals(5 * 60, vo.getWaitingSeconds());
    }

    @Test
    void toVo_queuedTask_shouldUseNowAsEndTime() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0, 0);
        LocalDateTime createdAt = now.minusSeconds(45);

        GenerationTaskListRow r = sampleRow();
        r.setStatus(0);
        r.setCreatedAt(createdAt);
        r.setCompletedAt(null);

        GenerationTaskAdminVO vo = service.toVo(r, now, 0L);

        assertEquals(45, vo.getWaitingSeconds());
    }

    @Test
    void toVo_completedTaskWithoutCompletedAt_shouldFallbackToNow() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0, 0);
        LocalDateTime createdAt = now.minusMinutes(10);

        GenerationTaskListRow r = sampleRow();
        r.setStatus(2);
        r.setCreatedAt(createdAt);
        r.setCompletedAt(null);

        GenerationTaskAdminVO vo = service.toVo(r, now, 0L);

        assertEquals(10 * 60, vo.getWaitingSeconds());
    }

    @Test
    void downloadArticle_shouldReturnMarkdownBytes() {
        GenerationTask task = new GenerationTask();
        task.setId(40L);
        task.setBizNo("GA20260701");
        task.setStatus(GenerationTaskStatus.COMPLETED);
        task.setArticleBizNo("A456");
        when(taskMapper.selectById(40L)).thenReturn(task);

        com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO article =
                new com.aichuangzuo.admin.modules.generation.vo.GeneratedArticleVO();
        article.setTitle("标题");
        article.setBody("正文");
        article.setDescription("描述");
        when(articleReadClient.getArticle("A456")).thenReturn(article);

        ArticleDownload download = service.downloadArticle(40L);

        assertEquals("GA20260701.md", download.getFilename());
        String content = new String(download.getContent(), java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(content);
        assertEquals("# 标题\n\n> 描述\n\n正文", content);
    }

    @Test
    void batchStop_shouldThrowWhenIdsEmpty() {
        assertThrows(BusinessException.class, () -> service.batchStop(List.of()));
    }

    @Test
    void batchStop_shouldThrowWhenIdsExceedLimit() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 101; i++) {
            ids.add(i);
        }
        assertThrows(BusinessException.class, () -> service.batchStop(ids));
    }

    @Test
    void batchStop_shouldStopOnlyQueuedAndProcessingTasks() {
        GenerationTask queued = new GenerationTask();
        queued.setId(1L);
        queued.setTargetUserId(10L);
        queued.setStatus(GenerationTaskStatus.QUEUED);

        GenerationTask processing = new GenerationTask();
        processing.setId(2L);
        processing.setTargetUserId(11L);
        processing.setStatus(GenerationTaskStatus.PROCESSING);

        GenerationTask completed = new GenerationTask();
        completed.setId(3L);
        completed.setStatus(GenerationTaskStatus.COMPLETED);

        when(taskMapper.selectById(1L)).thenReturn(queued);
        when(taskMapper.selectById(2L)).thenReturn(processing);
        when(taskMapper.selectById(3L)).thenReturn(completed);
        when(taskMapper.selectById(4L)).thenReturn(null);

        BatchStopGenerationTaskResultVO result = service.batchStop(List.of(1L, 2L, 3L, 4L));

        assertEquals(4, result.getTotal());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getInvalidIds().size());
        assertTrue(result.getInvalidIds().contains(3L));
        assertEquals(1, result.getMissingIds().size());
        assertTrue(result.getMissingIds().contains(4L));
        assertTrue(result.getFailedIds().isEmpty());

        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper, times(2)).updateById(captor.capture());
        List<GenerationTask> updated = captor.getAllValues();
        assertEquals(2, updated.size());
        assertTrue(updated.stream().allMatch(t -> t.getStatus() == GenerationTaskStatus.FAILED));
        verify(refundClient).refund(1L, 10L);
        verify(refundClient).refund(2L, 11L);
    }

    @Test
    void batchStop_shouldRecordFailedIdsWhenUpdateThrows() {
        GenerationTask task1 = new GenerationTask();
        task1.setId(1L);
        task1.setTargetUserId(10L);
        task1.setStatus(GenerationTaskStatus.QUEUED);

        GenerationTask task2 = new GenerationTask();
        task2.setId(2L);
        task2.setTargetUserId(11L);
        task2.setStatus(GenerationTaskStatus.QUEUED);

        when(taskMapper.selectById(1L)).thenReturn(task1);
        when(taskMapper.selectById(2L)).thenReturn(task2);
        doThrow(new RuntimeException("db error")).when(taskMapper).updateById(task1);

        BatchStopGenerationTaskResultVO result = service.batchStop(List.of(1L, 2L));

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedIds().size());
        assertTrue(result.getFailedIds().contains(1L));
        verify(refundClient).refund(2L, 11L);
    }

    @Test
    void batchStop_shouldStillStopWhenRefundFails() {
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setTargetUserId(10L);
        task.setStatus(GenerationTaskStatus.QUEUED);
        when(taskMapper.selectById(1L)).thenReturn(task);
        doThrow(new RuntimeException("user-api down")).when(refundClient).refund(1L, 10L);

        BatchStopGenerationTaskResultVO result = service.batchStop(List.of(1L));

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertTrue(result.getFailedIds().isEmpty());
        ArgumentCaptor<GenerationTask> captor = ArgumentCaptor.forClass(GenerationTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertEquals(GenerationTaskStatus.FAILED, captor.getValue().getStatus());
    }
}
