package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 第 6 阶段：韵律改写（AI）— 针对第 5 阶段问题清单定向改写
 */
@Component
public class RhythmRewriteStep extends AbstractAiStep {

    public RhythmRewriteStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 6; }

    @Override
    public String name() { return "rhythm-rewrite"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        if (!root.path("draft").isArray()) {
            throw new RuntimeException("第 6 阶段返回缺少 draft 数组");
        }
        ctx.setDraftAfterRhythmJson(root.toString());
    }
}
