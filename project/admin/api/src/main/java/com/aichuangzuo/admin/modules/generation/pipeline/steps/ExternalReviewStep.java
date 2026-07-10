package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 第 7 阶段：外部审视（AI）— 毒舌同行
 */
@Component
public class ExternalReviewStep extends AbstractAiStep {

    public ExternalReviewStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 7; }

    @Override
    public String name() { return "external-review"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        // AI 返回 JSON 数组；逐条转成 ToxicComment
        if (!root.isArray()) {
            // 部分模型会包成 {"comments": [...]}，尝试容错
            JsonNode inner = root.path("comments");
            if (inner.isArray()) {
                root = inner;
            } else {
                throw new RuntimeException("第 7 阶段返回应为 JSON 数组（毒舌点评列表）");
            }
        }
        java.util.List<GenerationContext.ToxicComment> list = new java.util.ArrayList<>();
        for (JsonNode item : root) {
            GenerationContext.ToxicComment c = new GenerationContext.ToxicComment();
            c.setParagraph(item.path("paragraph").isInt() ? item.path("paragraph").asInt() : null);
            c.setSentence(item.path("sentence").isInt() ? item.path("sentence").asInt() : null);
            c.setType(textOrNull(item, "type"));
            c.setOriginal(textOrNull(item, "original"));
            c.setToxicComment(textOrNull(item, "toxicComment"));
            c.setSeverity(textOrNull(item, "severity"));
            list.add(c);
        }
        ctx.setToxicComments(list);
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
