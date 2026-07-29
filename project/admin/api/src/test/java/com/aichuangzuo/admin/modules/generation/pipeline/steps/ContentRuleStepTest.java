package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentRuleStepTest {

    private final ContentRuleStep step = new ContentRuleStep();

    // ===== content_post_process 分支 =====

    @Test
    void postProcess_shouldReplaceSingleQuotesWithChineseDoubleQuotes() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKeyAndConfig(5, "content_post_process",
                "{\"singleQuoteToChineseQuotes\": true}"));
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"他说'你好'，我说'再见'。\"}]}");
        ctx.setFinalDraftJson(ctx.getDraftJson());

        step.process(ctx);

        assertTrue(ctx.getFinalDraftJson().contains("他说“你好”，我说“再见”。"),
                "成对单引号应被替换为中文双引号");
        assertTrue(ctx.getDraftJson().contains("他说“你好”，我说“再见”。"),
                "draftJson 也应同步更新");
    }

    @Test
    void postProcess_shouldReplaceQuotesInDescription() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKeyAndConfig(5, "content_post_process",
                "{\"singleQuoteToChineseQuotes\": true}"));
        ctx.setDraftJson("{\"draft\":[],\"description\":\"他说'很重要'。\"}");
        ctx.setFinalDraftJson(ctx.getDraftJson());

        step.process(ctx);

        assertTrue(ctx.getFinalDraftJson().contains("他说“很重要”。"),
                "description 中的单引号也应被替换");
        assertEquals("他说“很重要”。", ctx.getPublishDescription(),
                "ctx.publishDescription 也应同步刷新");
    }

    @Test
    void postProcess_shouldSkipWhenConfigDisabled() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKeyAndConfig(5, "content_post_process",
                "{\"singleQuoteToChineseQuotes\": false}"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"他说'你好'。\"}]}";
        ctx.setDraftJson(draft);
        ctx.setFinalDraftJson(draft);

        step.process(ctx);

        assertEquals(draft, ctx.getFinalDraftJson(), "未启用时应保持原样");
    }

    @Test
    void postProcess_shouldSkipWhenDraftEmpty() {
        GenerationContext ctx = makeCtx();
        ctx.setStages(stagesWithKeyAndConfig(5, "content_post_process",
                "{\"singleQuoteToChineseQuotes\": true}"));

        step.process(ctx);

        assertEquals(null, ctx.getFinalDraftJson(), "draft 为空时不应报错");
    }

    // ===== rhythm_detect 分支（默认兼容） =====

    @Test
    void rhythmDetect_shouldDetectUniformLength() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"第一句有点长。第二句也有点长。第三句也来长一些。\"}]}");
        ctx.setStages(stagesWithKeyAndConfig(5, "rhythm_detect",
                "{\"uniformLengthDelta\": 5, \"breathMaxChars\": 200, \"monotonousStartCount\": 5}"));

        step.process(ctx);
        assertNotNull(ctx.getRhythmIssues());
        assertEquals(1, ctx.getRhythmIssues().stream()
                .filter(r -> "uniform_length".equals(r.getType()))
                .count());
    }

    @Test
    void rhythmDetect_shouldDetectNoBreath() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"这是一段非常非常非常非常非常非常非常非常非常非常非常非常长的没有任何标点的文字看着都累\"}]}");
        ctx.setStages(stagesWithKeyAndConfig(5, "rhythm_detect",
                "{\"uniformLengthDelta\": 0, \"breathMaxChars\": 35, \"monotonousStartCount\": 5}"));

        step.process(ctx);
        assertEquals(1, ctx.getRhythmIssues().stream()
                .filter(r -> "no_breath".equals(r.getType()))
                .count());
    }

    @Test
    void rhythmDetect_shouldUseDefaultThresholdsWhenNoRuleConfig() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"第一句。第二句。第三句。\"}]}");
        ctx.setStages(stagesWithKeyAndConfig(5, "rhythm_detect", null));

        step.process(ctx);
    }

    @Test
    void rhythmDetect_shouldHandleEmptyDraftGracefully() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson(null);
        step.process(ctx);
        assertNotNull(ctx.getRhythmIssues());
    }

    @Test
    void rhythmDetect_shouldBeDefaultWhenStageKeyMissing() {
        GenerationContext ctx = makeCtx();
        ctx.setDraftJson("{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"x\",\"content\":\"第一句。第二句。第三句。\"}]}");
        ctx.setStages(stagesWithKeyAndConfig(5, null, null));

        step.process(ctx);
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

    private Map<Integer, PromptTemplateStage> stagesWithKeyAndConfig(int idx, String stageKey, String ruleConfig) {
        Map<Integer, PromptTemplateStage> map = new HashMap<>();
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(idx);
        s.setStageType("rule_config");
        s.setStageKey(stageKey);
        s.setRuleConfig(ruleConfig);
        map.put(idx, s);
        return map;
    }
}
