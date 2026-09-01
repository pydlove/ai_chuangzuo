package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineStage;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiDetectStepTest {

    private final AiGateway aiGateway = mock(AiGateway.class);
    private final AiDetectStep step = new AiDetectStep(aiGateway);

    @Test
    void process_shouldParseScoreAndMapQualityLevel() {
        String aiResponse = """
                {
                  "score": 75,
                  "summary": "整体质量不错，但部分句子偏书面。"
                }
                """;
        when(aiGateway.call(any(), any(), any(), any())).thenReturn(aiResponse);

        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(14, "ai_detect", 1));
        ctx.setFinalDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"正文\"}]}");

        step.process(ctx);

        assertNotNull(ctx.getAiDetectReport());
        assertEquals(75, ctx.getAiDetectReport().getScore());
        assertEquals("中", ctx.getAiDetectReport().getQualityLevel());
        assertEquals("整体质量不错，但部分句子偏书面。", ctx.getAiDetectReport().getSummary());
    }

    @Test
    void process_shouldMapAllQualityLevels() {
        assertLevel(45, "低");
        assertLevel(60, "中");
        assertLevel(85, "高");
        assertLevel(95, "极高");
    }

    @Test
    void process_shouldClampOutOfRangeScore() {
        String aiResponse = "{\"score\": 120, \"summary\": \"超范围\"}";
        when(aiGateway.call(any(), any(), any(), any())).thenReturn(aiResponse);

        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(14, "ai_detect", 1));
        ctx.setFinalDraftJson("{\"draft\":[]}");

        step.process(ctx);

        assertEquals(100, ctx.getAiDetectReport().getScore());
        assertEquals("极高", ctx.getAiDetectReport().getQualityLevel());
    }

    @Test
    void process_shouldFallbackFromAiRateForBackwardCompat() {
        String aiResponse = "{\"ai_rate\": 70, \"summary\": \"旧格式\"}";
        when(aiGateway.call(any(), any(), any(), any())).thenReturn(aiResponse);

        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(14, "ai_detect", 1));
        ctx.setFinalDraftJson("{\"draft\":[]}");

        step.process(ctx);

        assertEquals(30, ctx.getAiDetectReport().getScore());
        assertEquals("低", ctx.getAiDetectReport().getQualityLevel());
        assertEquals(70, ctx.getAiDetectReport().getAiRate());
    }

    private void assertLevel(int score, String expectedLevel) {
        String aiResponse = String.format("{\"score\": %d, \"summary\": \"test\"}", score);
        when(aiGateway.call(any(), any(), any(), any())).thenReturn(aiResponse);

        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(14, "ai_detect", 1));
        ctx.setFinalDraftJson("{\"draft\":[]}");

        step.process(ctx);

        assertEquals(expectedLevel, ctx.getAiDetectReport().getQualityLevel(),
                "score=" + score + " 应映射为 " + expectedLevel);
    }

    private GenerationContext makeCtx() {
        GenerationContext ctx = new GenerationContext();
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setTargetUserId(10L);
        ctx.setTask(task);
        return ctx;
    }

    private Map<Integer, PromptTemplateStage> stagesWithKey(int idx, String stageKey, int enabled) {
        Map<Integer, PromptTemplateStage> map = new HashMap<>();
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(idx);
        s.setStageType("ai_prompt");
        s.setStageKey(stageKey);
        s.setEnabled(enabled);
        s.setAiPrompt(PipelineStage.byIndex(idx).defaultAiPrompt);
        map.put(idx, s);
        return map;
    }
}
