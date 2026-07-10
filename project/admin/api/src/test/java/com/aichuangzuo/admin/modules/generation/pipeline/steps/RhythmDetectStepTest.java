package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RhythmDetectStepTest {

    private RhythmDetectStep step = new RhythmDetectStep();

    @Test
    void process_shouldDetectUniformLength() {
        GenerationContext ctx = makeCtx();
        // 三句长度都接近（差 ≤ 5）
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"第一句有点长。第二句也有点长。第三句也来长一些。\"}]}");
        ctx.setStages(stagesWithRuleConfig(5, "{\"uniformLengthDelta\": 5, \"breathMaxChars\": 200, \"monotonousStartCount\": 5}"));

        step.process(ctx);
        assertNotNull(ctx.getRhythmIssues());
        // 应该检出句长均匀问题（具体看实现）
        assertEquals(1, ctx.getRhythmIssues().stream()
                .filter(r -> "uniform_length".equals(r.getType()))
                .count());
    }

    @Test
    void process_shouldDetectNoBreath() {
        GenerationContext ctx = makeCtx();
        // 一段超长（>35 字无句末标点）
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"这是一段非常非常非常非常非常非常非常非常非常非常非常非常长的没有任何标点的文字看着都累\"}]}");
        ctx.setStages(stagesWithRuleConfig(5, "{\"uniformLengthDelta\": 0, \"breathMaxChars\": 35, \"monotonousStartCount\": 5}"));

        step.process(ctx);
        assertEquals(1, ctx.getRhythmIssues().stream()
                .filter(r -> "no_breath".equals(r.getType()))
                .count());
    }

    @Test
    void process_shouldUseDefaultThresholdsWhenNoRuleConfig() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"第一句。第二句。第三句。\"}]}");
        // 故意把 stage 5 的 ruleConfig 设为 null
        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        PromptTemplateStage s5 = new PromptTemplateStage();
        s5.setStageIndex(5);
        s5.setStageType("rule_config");
        s5.setRuleConfig(null);
        stages.put(5, s5);
        ctx.setStages(stages);

        // 不应抛异常；用默认阈值
        step.process(ctx);
    }

    @Test
    void process_shouldHandleEmptyDraftGracefully() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson(null);
        step.process(ctx);
        // 不应抛异常；rhythmIssues 保持空
        assertNotNull(ctx.getRhythmIssues());
    }

    private GenerationContext makeCtx() {
        GenerationContext ctx = new GenerationContext();
        GenerationTask t = new GenerationTask();
        t.setId(1L);
        t.setTargetUserId(10L);
        t.setWordLimitTarget(1500);
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
