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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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

        // 模拟 pipeline 跑 3 个 stage，每个 stage 完成后触发回调
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
        }).when(pipeline).runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class));

        invokeProcessOne(task);

        // 3 次 stage 回调 + 1 次 finally 兜底
        verify(callLogService, times(4)).persistAll(any(GenerationContext.class));
        verify(taskService).markCompleted(100L, "ART-100", "worker-1");
        verify(taskService, never()).markFailed(anyLong(), anyString(), anyBoolean(), anyString());
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
        }).when(pipeline).runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class));

        // 第一次增量 persist 抛异常，主流程应继续
        doThrow(new RuntimeException("db timeout"))
                .doReturn(0)
                .when(callLogService).persistAll(any(GenerationContext.class));

        invokeProcessOne(task);

        verify(taskService).markCompleted(101L, "ART-101", "worker-1");
        verify(callLogService, times(2)).persistAll(any(GenerationContext.class));
    }

    @Test
    void processOne_shouldPersistInFinallyWhenPipelineFailsEarly() throws Exception {
        GenerationTask task = makeTask(102L);
        GenerationTask failedTask = new GenerationTask();
        failedTask.setId(102L);
        failedTask.setStatus(GenerationTaskStatus.FAILED);
        failedTask.setTargetUserId(10L);

        when(pipeline.runInto(any(GenerationContext.class), eq(task), any(BiConsumer.class)))
                .thenThrow(new RuntimeException("stage 2 failed"));
        when(taskService.markFailed(eq(102L), anyString(), eq(false), anyString())).thenReturn(failedTask);

        invokeProcessOne(task);

        // 回调未触发，finally 兜底一次
        verify(callLogService, times(1)).persistAll(any(GenerationContext.class));
        verify(taskService).markFailed(102L, "stage 2 failed", false, "worker-1");
        verify(refundClient).refund(102L, 10L);
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
        }).when(pipeline).runInto(ctxCaptor.capture(), eq(task), any(BiConsumer.class));

        invokeProcessOne(task);

        GenerationContext captured = ctxCaptor.getValue();
        assertNotNull(captured);
        assertEquals("ART-103", captured.getArticleBizNo());
    }
}
