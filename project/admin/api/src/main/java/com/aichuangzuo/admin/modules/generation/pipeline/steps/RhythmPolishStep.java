package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 第 9 阶段：节奏打磨（AI）— 风格主保护区，最终调气
 */
@Component
public class RhythmPolishStep extends AbstractAiStep {

    public RhythmPolishStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 9; }

    @Override
    public String name() { return "rhythm-polish"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        if (!root.path("draft").isArray()) {
            throw new RuntimeException("第 9 阶段返回缺少 draft 数组");
        }
        ctx.setFinalDraftJson(root.toString());
    }
}
