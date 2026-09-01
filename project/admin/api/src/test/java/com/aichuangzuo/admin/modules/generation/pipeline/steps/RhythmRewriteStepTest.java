package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RhythmRewriteStepTest {

    private final AiGateway aiGateway = mock(AiGateway.class);
    private final RhythmRewriteStep step = new RhythmRewriteStep(aiGateway);

    @Test
    void enabled_shouldReturnTrueWhenStageKeyIsRhythmRewriteAndEnabled() {
        GenerationContext ctx = new GenerationContext();
        ctx.setStages(stagesWithKey(6, "rhythm_rewrite", 1));
        assertTrue(step.enabled(ctx));
    }

    @Test
    void enabled_shouldReturnFalseWhenStageKeyIsHumanizeRewrite() {
        GenerationContext ctx = new GenerationContext();
        ctx.setStages(stagesWithKey(6, "humanize_rewrite", 1));
        assertFalse(step.enabled(ctx));
    }

    @Test
    void enabled_shouldReturnFalseWhenStageDisabled() {
        GenerationContext ctx = new GenerationContext();
        ctx.setStages(stagesWithKey(6, "rhythm_rewrite", 0));
        assertFalse(step.enabled(ctx));
    }

    @Test
    void parseAndStore_shouldStoreDraftAfterRhythmJson() {
        String aiResponse = "{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"改写后\"}]}";
        when(aiGateway.call(any(), any(), any(), any())).thenReturn(aiResponse);

        GenerationContext ctx = new GenerationContext();
        ctx.setStages(stagesWithKey(6, "rhythm_rewrite", 1));
        ctx.setDraftJson("{\"draft\":[]}");

        // AbstractAiStep.process 会调 renderAiPrompt，需要 ai_prompt 非空
        ctx.getStages().get(6).setAiPrompt("根据 {{rhythmIssues}} 改写 {{draft}}");
        ctx.setRhythmIssues(new java.util.ArrayList<>());

        step.process(ctx);

        assertTrue(ctx.getDraftAfterRhythmJson().contains("改写后"));
    }

    @Test
    void parseAndStore_shouldThrowWhenResponseLacksDraftArray() {
        when(aiGateway.call(any(), any(), any(), any())).thenReturn("{\"error\":\"bad\"}");

        GenerationContext ctx = new GenerationContext();
        ctx.setStages(stagesWithKey(6, "rhythm_rewrite", 1));
        ctx.setDraftJson("{\"draft\":[]}");
        ctx.getStages().get(6).setAiPrompt("改写");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> step.process(ctx));
        assertTrue(ex.getMessage().contains("draft"));
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
