package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptRenderService;
import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HumanizeRewriteStepTest {

    private final AiGateway aiGateway = mock(AiGateway.class);
    private final AiPromptRenderService renderService = mock(AiPromptRenderService.class);
    private final HumanizeRewriteStep step = new HumanizeRewriteStep(aiGateway, renderService);

    @Test
    void enabled_shouldReturnTrueWhenStageKeyIsHumanizeRewriteAndEnabled() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 1));
        assertTrue(step.enabled(ctx));
    }

    @Test
    void enabled_shouldReturnFalseWhenStageKeyIsRhythmRewrite() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "rhythm_rewrite", 1));
        assertFalse(step.enabled(ctx));
    }

    @Test
    void enabled_shouldReturnFalseWhenStageDisabled() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 0));
        assertFalse(step.enabled(ctx));
    }

    @Test
    void enabled_shouldReturnFalseWhenStageMissing() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(new HashMap<>());
        assertFalse(step.enabled(ctx));
    }

    @Test
    void process_shouldRenderPromptAndCallGatewayAndStoreFinalDraft() {
        String draftJson = "{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"原文。\"}]}";
        String aiResponse = "{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"改写后。\"}]}";

        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 1));
        ctx.setDraftJson(draftJson);
        ctx.setFinalDraftJson(draftJson);

        when(renderService.render(eq(HumanizeRewriteStep.PROMPT_CODE), any()))
                .thenReturn(new AiPromptRendered("你是编辑", "改写：{{draft}}"));
        when(aiGateway.call(eq(ctx), eq("你是编辑"), eq("改写：{{draft}}"), any()))
                .thenReturn(aiResponse);

        StepResult result = step.process(ctx);

        assertEquals(StepResult.CONTINUE, result);
        assertTrue(ctx.getFinalDraftJson().contains("改写后。"));
        assertEquals(6, ctx.getExtra("__currentStageIndex"));
        assertEquals("humanize-rewrite", ctx.getExtra("__currentStepName"));

        ArgumentCaptor<Map<String, Object>> varCaptor = ArgumentCaptor.forClass(Map.class);
        verify(renderService, times(1)).render(eq(HumanizeRewriteStep.PROMPT_CODE), varCaptor.capture());
        assertEquals(draftJson, varCaptor.getValue().get("draft"));
    }

    @Test
    void process_shouldPreferFinalDraftJsonOverDraftJson() {
        String draftJson = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"初稿\"}]}";
        String finalDraftJson = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"已后处理\"}]}";
        String aiResponse = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"改写后\"}]}";

        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 1));
        ctx.setDraftJson(draftJson);
        ctx.setFinalDraftJson(finalDraftJson);

        when(renderService.render(eq(HumanizeRewriteStep.PROMPT_CODE), any()))
                .thenReturn(new AiPromptRendered("你是编辑", "改写"));
        when(aiGateway.call(any(), any(), any(), any())).thenReturn(aiResponse);

        step.process(ctx);

        ArgumentCaptor<Map<String, Object>> varCaptor = ArgumentCaptor.forClass(Map.class);
        verify(renderService).render(eq(HumanizeRewriteStep.PROMPT_CODE), varCaptor.capture());
        assertEquals(finalDraftJson, varCaptor.getValue().get("draft"));
    }

    @Test
    void process_shouldSkipWhenDraftIsEmpty() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 1));

        StepResult result = step.process(ctx);

        assertEquals(StepResult.CONTINUE, result);
        verify(renderService, never()).render(any(), any());
        verify(aiGateway, never()).call(any(), any(), any(), any());
    }

    @Test
    void process_shouldThrowWhenResponseLacksDraftArray() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 1));
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"x\"}]}");

        when(renderService.render(eq(HumanizeRewriteStep.PROMPT_CODE), any()))
                .thenReturn(new AiPromptRendered("你是编辑", "改写"));
        when(aiGateway.call(any(), any(), any(), any())).thenReturn("{\"error\":\"bad\"}");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> step.process(ctx));
        assertTrue(ex.getMessage().contains("draft"));
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
        map.put(idx, s);
        return map;
    }
}
