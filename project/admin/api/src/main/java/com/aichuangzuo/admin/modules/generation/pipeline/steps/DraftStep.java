package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 第 4 阶段：分块初稿（AI）— 风格主战场
 */
@Component
public class DraftStep extends AbstractAiStep {

    public DraftStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 4; }

    @Override
    public String name() { return "draft"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        if (!root.path("draft").isArray()) {
            throw new RuntimeException("第 4 阶段返回缺少 draft 数组");
        }
        ctx.setDraftJson(root.toString());
    }
}
