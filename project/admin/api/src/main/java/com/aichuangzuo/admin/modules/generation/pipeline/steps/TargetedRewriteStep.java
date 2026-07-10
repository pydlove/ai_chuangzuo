package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 第 8 阶段：定向改写（AI）— 根据毒舌点评改写
 */
@Component
public class TargetedRewriteStep extends AbstractAiStep {

    public TargetedRewriteStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 8; }

    @Override
    public String name() { return "targeted-rewrite"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        if (!root.path("draft").isArray()) {
            throw new RuntimeException("第 8 阶段返回缺少 draft 数组");
        }
        ctx.setDraftAfterTargetedJson(root.toString());
    }
}
