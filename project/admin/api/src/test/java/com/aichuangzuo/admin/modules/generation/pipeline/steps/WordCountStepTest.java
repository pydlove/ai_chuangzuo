package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WordCountStepTest {

    private WordCountStep step = new WordCountStep();

    @Test
    void process_shouldCountCharsAndCompareWithTarget() {
        GenerationContext ctx = makeCtx(1500);
        ctx.setFinalDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"这是一段正文。\"}]}");
        ctx.setStages(stagesWithRuleConfig(10, "{\"excludePunctuation\": true, \"excludeSpaces\": true}"));

        step.process(ctx);

        assertNotNull(ctx.getWordStats());
        // "这是一段正文" 6 个字（排除标点 .）
        assertEquals(6, ctx.getWordStats().getActual());
        assertEquals(1500, ctx.getWordStats().getTarget());
        assertEquals("under", ctx.getWordStats().getStatus());
    }

    @Test
    void process_shouldReportOverWhenExceedsTarget() {
        GenerationContext ctx = makeCtx(20);
        // 30+ 字超 20 字目标
        ctx.setFinalDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"这是一段超过目标的测试文字用来验证超额状态判断逻辑是否符合预期哈哈\"}]}");
        ctx.setStages(stagesWithRuleConfig(10, "{\"excludePunctuation\": true, \"excludeSpaces\": true}"));

        step.process(ctx);

        assertEquals("over", ctx.getWordStats().getStatus());
    }

    @Test
    void process_shouldHandleEmptyDraft() {
        GenerationContext ctx = makeCtx(1500);
        ctx.setFinalDraftJson(null);
        step.process(ctx);

        assertEquals(0, ctx.getWordStats().getActual());
        assertEquals(1500, ctx.getWordStats().getTarget());
        assertEquals("under", ctx.getWordStats().getStatus());
    }

    private GenerationContext makeCtx(int target) {
        GenerationContext ctx = new GenerationContext();
        GenerationTask t = new GenerationTask();
        t.setId(1L);
        t.setTargetUserId(10L);
        t.setWordLimitTarget(target);
        ctx.setTask(t);
        return ctx;
    }

    private Map<Integer, PromptTemplateStage> stagesWithRuleConfig(int idx, String ruleConfig) {
        Map<Integer, PromptTemplateStage> map = new HashMap<>();
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(idx);
        s.setStageType("rule_config");
        s.setRuleConfig(ruleConfig);
        map.put(idx, s);
        return map;
    }
}
