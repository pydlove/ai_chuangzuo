package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 第 11 阶段：字数调整（AI）— 质量优先：超过才删，欠字数不补
 */
@Slf4j
@Component
public class WordAdjustStep extends AbstractAiStep {

    public WordAdjustStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 11; }

    @Override
    public String name() { return "word-adjust"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        String action = root.path("action").asText("");
        if (action.isBlank()) {
            action = "keep";
        }
        GenerationContext.WordAdjustResult r = new GenerationContext.WordAdjustResult();
        r.setAction(action);
        r.setReason(root.path("reason").asText(""));
        r.setEstimatedFinalCount(root.path("estimated_final_count").asInt(0));
        ctx.setWordAdjustResult(r);
        if ("cut".equals(action)) {
            log.info("字数调整建议 cut 预估 {} 字", r.getEstimatedFinalCount());
        } else {
            log.info("字数调整 keep：{}（actual vs target）", r.getReason());
        }
    }
}
