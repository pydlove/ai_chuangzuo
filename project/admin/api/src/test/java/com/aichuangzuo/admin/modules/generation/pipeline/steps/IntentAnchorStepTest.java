package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentAnchorStepTest {

    @Test
    void process_shouldAssembleUserContextBlockFromInput() {
        IntentAnchorStep step = new IntentAnchorStep();
        Map<String, Object> input = new HashMap<>();
        input.put("title", "AI 写作入门");
        input.put("description", "从零开始学 AI 创作");
        input.put("userStylePrompt", "轻松活泼");

        GenerationContext ctx = new GenerationContext();
        ctx.setInput(input);

        step.process(ctx);

        String block = ctx.getUserContextBlock();
        assertNotNull(block);
        assertTrue(block.contains("标题：AI 写作入门"));
        assertTrue(block.contains("核心观点：从零开始学 AI 创作"));
        assertTrue(block.contains("目标读者：通用读者"));  // 默认值
        assertTrue(block.contains("风格：轻松活泼"));
    }

    @Test
    void process_shouldUseCustomTargetReader() {
        IntentAnchorStep step = new IntentAnchorStep();
        Map<String, Object> input = new HashMap<>();
        input.put("title", "T");
        input.put("description", "D");
        input.put("targetReader", "宝妈");
        input.put("userStylePrompt", "S");

        GenerationContext ctx = new GenerationContext();
        ctx.setInput(input);
        step.process(ctx);

        assertTrue(ctx.getUserContextBlock().contains("目标读者：宝妈"));
    }

    @Test
    void process_shouldHandleMissingFields() {
        IntentAnchorStep step = new IntentAnchorStep();
        GenerationContext ctx = new GenerationContext();
        ctx.setInput(new HashMap<>());

        step.process(ctx);

        // 不应抛异常；缺失字段填空串
        assertNotNull(ctx.getUserContextBlock());
        assertTrue(ctx.getUserContextBlock().startsWith("标题："));
    }
}
