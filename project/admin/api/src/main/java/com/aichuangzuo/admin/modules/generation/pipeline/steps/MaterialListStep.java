package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 第 3 阶段：素材清单（AI）— 诚实标注 已知/推断/未知/待补
 */
@Component
public class MaterialListStep extends AbstractAiStep {

    public MaterialListStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 3; }

    @Override
    public String name() { return "material-list"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        if (!root.path("materials").isArray()) {
            throw new RuntimeException("第 3 阶段返回缺少 materials 数组");
        }
        ctx.setMaterialsJson(root.toString());
    }
}
