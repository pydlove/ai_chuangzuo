package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import org.springframework.stereotype.Component;

/**
 * 第 2 阶段：结构骨架（AI）— 职责式大纲
 */
@Component
public class OutlineStep extends AbstractAiStep {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public OutlineStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 2; }

    @Override
    public String name() { return "outline"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        if (!root.path("paragraphs").isArray()) {
            throw new RuntimeException("第 2 阶段返回缺少 paragraphs 数组");
        }
        // 直接把 root 转成 JSON 字符串存 ctx（方便后续阶段引用）
        ctx.setOutlineJson(root.toString());
    }
}
