package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AbstractAiStep 行为测试：
 * <ul>
 *   <li>process() 调 4-arg aiGateway.call，把 ctx.modelParams 透传</li>
 *   <li>AI 返回 JSON → parseAndStore 被调用</li>
 *   <li>ctx.putExtra 标记了当前 stageIndex / stepName</li>
 * </ul>
 */
class AbstractAiStepTest {

    @Test
    void process_shouldCallFourArgGatewayWithModelParamsFromCtx() {
        AiGateway gw = mock(AiGateway.class);
        when(gw.call(any(), any(), any(), any())).thenReturn("{\"k\":1}");

        PromptTemplateStage stage = makeStage(1, "hello world");

        GenerationContext ctx = new GenerationContext();
        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        stages.put(1, stage);
        ctx.setStages(stages);
        Map<String, Object> params = Map.of("temperature", 0.4, "max_tokens", 1024);
        ctx.setModelParams(params);

        DummyAiStep step = new DummyAiStep(gw);
        step.process(ctx);

        // 验证调用了 4 参版本
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(gw, times(1)).call(eq(ctx), eq(""), any(), captor.capture());
        assertEquals(0.4, captor.getValue().get("temperature"));
        assertEquals(1024, captor.getValue().get("max_tokens"));

        // parseAndStore 被调用（ctx 上有标记）
        assertTrue(step.parsedCtx);
    }

    @Test
    void process_shouldPassNullModelParams() {
        AiGateway gw = mock(AiGateway.class);
        when(gw.call(any(), any(), any(), isNull())).thenReturn("{\"k\":2}");

        PromptTemplateStage stage = makeStage(1, "hi");

        GenerationContext ctx = new GenerationContext();
        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        stages.put(1, stage);
        ctx.setStages(stages);
        // 不设 modelParams → null

        DummyAiStep step = new DummyAiStep(gw);
        step.process(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(gw, times(1)).call(eq(ctx), eq(""), any(), captor.capture());
        assertEquals(null, captor.getValue());
    }

    @Test
    void process_shouldMarkCurrentStageAndStepInCtx() {
        AiGateway gw = mock(AiGateway.class);
        when(gw.call(any(), any(), any(), any())).thenReturn("{\"k\":3}");

        PromptTemplateStage stage = makeStage(1, "draft prompt");

        GenerationContext ctx = new GenerationContext();
        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        stages.put(1, stage);
        ctx.setStages(stages);

        DummyAiStep step = new DummyAiStep(gw);
        step.process(ctx);

        // 留痕用的 stage/step（DummyAiStep 固定 stageIndex=1）
        assertEquals(1, ctx.getExtra("__currentStageIndex"));
        assertEquals("dummy-ai", ctx.getExtra("__currentStepName"));
    }

    private static PromptTemplateStage makeStage(int idx, String aiPrompt) {
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(idx);
        s.setAiPrompt(aiPrompt);
        s.setEnabled(1);
        return s;
    }

    /**
     * 一个最小可用的 AI step：固定 stageIndex=1, name="dummy-ai"；
     * parseAndStore 标记 parsedCtx=true 用于断言是否被调用。
     */
    static class DummyAiStep extends AbstractAiStep {
        boolean parsedCtx = false;

        DummyAiStep(AiGateway gw) {
            super(gw);
        }

        @Override
        public int stageIndex() { return 1; }

        @Override
        public String name() { return "dummy-ai"; }

        @Override
        protected void parseAndStore(JsonNode root, GenerationContext ctx) {
            parsedCtx = true;
            assertNotNull(root);
        }
    }
}
