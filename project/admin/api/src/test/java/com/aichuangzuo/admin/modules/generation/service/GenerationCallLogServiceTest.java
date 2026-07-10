package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.entity.GenerationCallLog;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationCallLogMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.AiCallRecord;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogVO;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationCallLogServiceTest {

    @Mock
    private GenerationCallLogMapper mapper;

    @InjectMocks
    private GenerationCallLogService service;

    private GenerationContext makeCtx(Long taskId) {
        GenerationContext ctx = new GenerationContext();
        GenerationTask t = new GenerationTask();
        t.setId(taskId);
        t.setTargetUserId(10L);
        ctx.setTask(t);
        return ctx;
    }

    private AiCallRecord rec(int stageIdx, String stepName, int attempt, boolean success, String error, long ms) {
        AiCallRecord r = new AiCallRecord();
        r.setStageIndex(stageIdx);
        r.setStepName(stepName);
        r.setAttempt(attempt);
        r.setSuccess(success);
        r.setError(error);
        r.setDurationMs(ms);
        r.setCalledAt(LocalDateTime.now());
        return r;
    }

    @Test
    void persistAll_shouldBatchInsertAllHistoryRecords() {
        GenerationContext ctx = makeCtx(100L);
        List<AiCallRecord> history = new ArrayList<>();
        history.add(rec(2, "outline", 1, true, null, 1234));
        history.add(rec(4, "draft", 1, false, "timeout", 5000));
        history.add(rec(4, "draft", 2, true, null, 1100));
        ctx.setAiCallHistory(history);
        ctx.setAiCallUsed(3);
        ctx.setAiCallFailed(1);
        ctx.setAiCallRetried(1);

        when(mapper.batchInsert(any())).thenReturn(3);

        int n = service.persistAll(ctx);

        assertEquals(3, n);
        ArgumentCaptor<List<GenerationCallLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).batchInsert(captor.capture());
        List<GenerationCallLog> rows = captor.getValue();
        assertEquals(3, rows.size());
        assertEquals(Long.valueOf(100L), rows.get(0).getTaskId());
        assertEquals(Integer.valueOf(2), rows.get(0).getStageIndex());
        assertEquals("outline", rows.get(0).getStageName());
        assertEquals(Integer.valueOf(1), rows.get(0).getSuccess());
        // 第 2 行是失败的
        assertEquals(Integer.valueOf(0), rows.get(1).getSuccess());
        assertEquals("timeout", rows.get(1).getError());
        // 第 3 行是 retry 成功的
        assertEquals(Integer.valueOf(2), rows.get(2).getAttempt());
    }

    @Test
    void persistAll_shouldReturnZeroWhenNoHistory() {
        GenerationContext ctx = makeCtx(100L);
        ctx.setAiCallHistory(new ArrayList<>());
        int n = service.persistAll(ctx);
        assertEquals(0, n);
        verify(mapper, never()).batchInsert(any());
    }

    @Test
    void persistAll_shouldReturnZeroWhenTaskIsNull() {
        GenerationContext ctx = new GenerationContext();
        ctx.setAiCallHistory(List.of(rec(2, "outline", 1, true, null, 100)));
        int n = service.persistAll(ctx);
        assertEquals(0, n);
        verify(mapper, never()).batchInsert(any());
    }

    @Test
    void queryByTaskId_shouldConvertSuccessFlagToBoolean() {
        GenerationCallLog row1 = new GenerationCallLog();
        row1.setId(1L); row1.setTaskId(100L);
        row1.setStageIndex(2); row1.setStageName("outline");
        row1.setAttempt(1); row1.setSuccess(1); row1.setDurationMs(1000);
        row1.setCalledAt(LocalDateTime.now());

        GenerationCallLog row2 = new GenerationCallLog();
        row2.setId(2L); row2.setTaskId(100L);
        row2.setStageIndex(4); row2.setStageName("draft");
        row2.setAttempt(1); row2.setSuccess(0); row2.setError("err");
        row2.setDurationMs(5000);
        row2.setCalledAt(LocalDateTime.now());

        when(mapper.selectByTaskId(100L)).thenReturn(List.of(row1, row2));

        List<GenerationCallLogVO> vos = service.queryByTaskId(100L);

        assertEquals(2, vos.size());
        assertTrue(vos.get(0).getSuccess());
        assertFalse(vos.get(1).getSuccess());
        assertEquals("err", vos.get(1).getError());
    }

    @Test
    void queryByTaskIdGrouped_shouldBucketByStageIndex() {
        GenerationCallLog r1 = new GenerationCallLog();
        r1.setTaskId(100L); r1.setStageIndex(2); r1.setStageName("outline");
        r1.setSuccess(1); r1.setDurationMs(1000); r1.setCalledAt(LocalDateTime.now());

        GenerationCallLog r2 = new GenerationCallLog();
        r2.setTaskId(100L); r2.setStageIndex(4); r2.setStageName("draft");
        r2.setSuccess(0); r2.setDurationMs(5000); r2.setCalledAt(LocalDateTime.now());

        GenerationCallLog r3 = new GenerationCallLog();
        r3.setTaskId(100L); r3.setStageIndex(4); r3.setStageName("draft");
        r3.setAttempt(2); r3.setSuccess(1); r3.setDurationMs(1100);
        r3.setCalledAt(LocalDateTime.now());

        when(mapper.selectByTaskId(100L)).thenReturn(List.of(r1, r2, r3));

        Map<Integer, List<GenerationCallLogVO>> grouped = service.queryByTaskIdGrouped(100L);

        assertNotNull(grouped);
        assertEquals(2, grouped.size());  // 2 个 stage
        assertEquals(1, grouped.get(2).size());
        assertEquals(2, grouped.get(4).size());  // stage 4 调了 2 次
        // stage 4 第二次重试成功
        assertTrue(grouped.get(4).get(1).getSuccess());
    }
}
