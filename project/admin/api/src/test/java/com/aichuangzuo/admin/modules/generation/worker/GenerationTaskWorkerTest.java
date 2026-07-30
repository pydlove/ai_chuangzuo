package com.aichuangzuo.admin.modules.generation.worker;

import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationPipeline;
import com.aichuangzuo.admin.modules.generation.service.GenerationCallLogService;
import com.aichuangzuo.admin.modules.generation.service.GenerationConfigService;
import com.aichuangzuo.admin.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.admin.modules.generation.service.QuotaRefundInternalClient;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationTaskWorkerTest {

    @Mock
    private GenerationTaskService taskService;
    @Mock
    private GenerationPipeline pipeline;
    @Mock
    private GenerationConfigService configService;
    @Mock
    private QuotaRefundInternalClient refundClient;
    @Mock
    private GenerationCallLogService callLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationTaskWorker worker;

    private GenerationConfig defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = new GenerationConfig();
        defaultConfig.setPoolSize(1);
        defaultConfig.setClaimBatchSize(1);
        defaultConfig.setLeaseMinutes(5);
        defaultConfig.setPollIntervalMs(500);
        defaultConfig.setWorkerId("worker-1");
        when(configService.getCurrent()).thenReturn(defaultConfig);
    }

    private GenerationTask makeTask(Long id) {
        GenerationTask t = new GenerationTask();
        t.setId(id);
        t.setLockedBy("worker-1");
        t.setTargetUserId(10L);
        return t;
    }

    private void invokeProcessOne(GenerationTask task) throws Exception {
        Method m = GenerationTaskWorker.class.getDeclaredMethod("processOne", GenerationTask.class);
        m.setAccessible(true);
        m.invoke(worker, task);
    }

    @Test
    void processOne_shouldPersistCallLogsAfterEachStageAndFinally() throws Exception {
        GenerationTask task = makeTask(100L);

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            GenerationTask t = inv.getArgument(1);
            @SuppressWarnings("unchecked")
            BiConsumer<Long, Integer> callback = inv.getArgument(2);
            ctx.setTask(t);
            callback.accept(t.getId(), 10);
            callback.accept(t.getId(), 30);
            callback.accept(t.getId(), 60);
            ctx.setArticleBizNo("ART-100");
            return ctx;
        }).when(pipeline).runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class));

        invokeProcessOne(task);

        verify(callLogService, times(4)).persistAll(any(GenerationContext.class));
        verify(taskService).markCompleted(eq(100L), eq("ART-100"), eq("worker-1"), anyMap());
        verify(taskService, never()).markFailed(anyLong(), anyString(), anyBoolean(), anyString(), anyMap());
    }

    @Test
    void processOne_shouldNotBreakPipelineWhenPersistFailsInCallback() throws Exception {
        GenerationTask task = makeTask(101L);

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            GenerationTask t = inv.getArgument(1);
            @SuppressWarnings("unchecked")
            BiConsumer<Long, Integer> callback = inv.getArgument(2);
            ctx.setTask(t);
            callback.accept(t.getId(), 10);
            ctx.setArticleBizNo("ART-101");
            return ctx;
        }).when(pipeline).runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class));

        doThrow(new RuntimeException("db timeout"))
                .doReturn(0)
                .when(callLogService).persistAll(any(GenerationContext.class));

        invokeProcessOne(task);

        verify(taskService).markCompleted(eq(101L), eq("ART-101"), eq("worker-1"), anyMap());
        verify(callLogService, times(2)).persistAll(any(GenerationContext.class));
    }

    @Test
    void processOne_shouldPersistInFinallyWhenPipelineFailsEarly() throws Exception {
        GenerationTask task = makeTask(102L);
        GenerationTask failedTask = new GenerationTask();
        failedTask.setId(102L);
        failedTask.setStatus(GenerationTaskStatus.FAILED);
        failedTask.setTargetUserId(10L);

        when(pipeline.runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class)))
                .thenThrow(new RuntimeException("stage 2 failed"));
        when(taskService.markFailed(eq(102L), anyString(), eq(false), anyString(), anyMap()))
                .thenReturn(failedTask);

        invokeProcessOne(task);

        verify(callLogService, times(1)).persistAll(any(GenerationContext.class));
        verify(taskService).markFailed(eq(102L), eq("stage 2 failed"), eq(false), eq("worker-1"), anyMap());
        verify(refundClient).refund(102L, 10L);
    }

    @Test
    void processOne_completed_payloadContainsArticleTitleAndBizNo() throws Exception {
        GenerationTask task = makeTask(200L);
        task.setInputParam("{\"title\":\"我的爆款标题\"}");

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            GenerationTask t = inv.getArgument(1);
            ctx.setTask(t);
            ctx.setArticleBizNo("ART-200");
            return ctx;
        }).when(pipeline).runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class));

        invokeProcessOne(task);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).markCompleted(eq(200L), eq("ART-200"), eq("worker-1"), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(200L, payload.get("taskId"));
        assertEquals(10L, payload.get("userId"));
        assertEquals("completed", payload.get("status"));
        assertEquals("ART-200", payload.get("articleBizNo"));
        assertEquals("我的爆款标题", payload.get("articleTitle"));
    }

    @Test
    void processOne_completed_omitsTitleWhenInputParamMalformed() throws Exception {
        GenerationTask task = makeTask(201L);
        task.setInputParam("{malformed");

        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            GenerationTask t = inv.getArgument(1);
            ctx.setTask(t);
            ctx.setArticleBizNo("ART-201");
            return ctx;
        }).when(pipeline).runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class));

        invokeProcessOne(task);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).markCompleted(eq(201L), eq("ART-201"), eq("worker-1"), payloadCaptor.capture());
        assertNull(payloadCaptor.getValue().get("articleTitle"));
    }

    @Test
    void processOne_failed_payloadContainsFailReason() throws Exception {
        GenerationTask task = makeTask(203L);
        GenerationTask failedTask = new GenerationTask();
        failedTask.setId(203L);
        failedTask.setStatus(GenerationTaskStatus.FAILED);
        failedTask.setTargetUserId(10L);

        when(pipeline.runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class)))
                .thenThrow(new RuntimeException("stage failed"));
        when(taskService.markFailed(eq(203L), anyString(), eq(false), anyString(), anyMap()))
                .thenReturn(failedTask);

        invokeProcessOne(task);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).markFailed(eq(203L), eq("stage failed"), eq(false), eq("worker-1"), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals(203L, payload.get("taskId"));
        assertEquals(10L, payload.get("userId"));
        assertEquals("failed", payload.get("status"));
        assertEquals("stage failed", payload.get("failReason"));
    }

    @Test
    void processOne_shouldPassCurrentContextToPipeline() throws Exception {
        GenerationTask task = makeTask(103L);

        ArgumentCaptor<GenerationContext> ctxCaptor = ArgumentCaptor.forClass(GenerationContext.class);
        doAnswer(inv -> {
            GenerationContext ctx = inv.getArgument(0);
            GenerationTask t = inv.getArgument(1);
            ctx.setTask(t);
            ctx.setArticleBizNo("ART-103");
            return ctx;
        }).when(pipeline).runInto(ctxCaptor.capture(), eq(task), any(BiConsumer.class), any(BooleanSupplier.class));

        invokeProcessOne(task);

        GenerationContext captured = ctxCaptor.getValue();
        assertNotNull(captured);
        assertEquals("ART-103", captured.getArticleBizNo());
    }

    @Test
    void processOne_shouldNotMarkFailedWhenPipelineAborted() throws Exception {
        GenerationTask task = makeTask(104L);

        when(pipeline.runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class), any(BooleanSupplier.class)))
                .thenThrow(new GenerationPipeline.TaskAbortedException("task=104 已被外部停止"));

        invokeProcessOne(task);

        verify(taskService, never()).markFailed(anyLong(), anyString(), anyBoolean(), anyString(), anyMap());
        verify(taskService, never()).markCompleted(anyLong(), anyString(), anyString(), anyMap());
        verify(refundClient, never()).refund(anyLong(), anyLong());
        verify(callLogService, times(1)).persistAll(any(GenerationContext.class));
    }
}
