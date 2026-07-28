package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        // 兼容 3 阶段快速路径：没有后续打磨阶段时，把 draft 也作为 finalDraft
        ctx.setFinalDraftJson(root.toString());

        // 兼容 3 阶段快速路径：若模型同时返回 description/tags，直接复用（跳过了 stage 13）
        JsonNode descNode = root.path("description");
        if (descNode.isTextual()) {
            ctx.setPublishDescription(descNode.asText().trim());
        }
        JsonNode tagsNode = root.path("tags");
        if (tagsNode.isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode n : tagsNode) {
                if (n.isTextual()) {
                    String t = n.asText().trim();
                    if (!t.isEmpty()) tags.add(t);
                }
            }
            ctx.setPublishTags(tags);
        }
    }
}
