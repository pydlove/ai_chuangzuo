package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 13 阶段：发布描述 + 推荐标签（AI）
 *
 * <p>根据标题和最终稿（{@code finalDraftJson}）生成发布页用的描述摘要和推荐标签，
 * 结果存 {@code ctx.publishDescription} / {@code ctx.publishTags}，
 * 由 {@code PersistArticleStep} 随文章一起落库。
 */
@Component
public class PublishMetaStep extends AbstractAiStep {

    public PublishMetaStep(AiGateway aiGateway) {
        super(aiGateway);
    }

    @Override
    public int stageIndex() { return 13; }

    @Override
    public String name() { return "publish-meta"; }

    @Override
    protected void parseAndStore(JsonNode root, GenerationContext ctx) {
        String description = root.path("description").asText("").trim();
        if (description.isEmpty()) {
            throw new RuntimeException("第 13 阶段返回缺少 description");
        }
        ctx.setPublishDescription(description);

        // 未开通 SEO 关键词建议权益时，只保留描述，不生成推荐标签
        if (!isSeoKeywordsEnabled(ctx)) {
            return;
        }

        if (!root.path("tags").isArray() || root.path("tags").isEmpty()) {
            throw new RuntimeException("第 13 阶段返回缺少 tags 数组");
        }
        List<String> tags = new ArrayList<>();
        for (JsonNode tag : root.path("tags")) {
            String t = tag.asText("").trim();
            if (!t.isEmpty()) tags.add(t);
        }
        if (tags.isEmpty()) {
            throw new RuntimeException("第 13 阶段 tags 数组无有效标签");
        }
        ctx.setPublishTags(tags);
    }

    private static boolean isSeoKeywordsEnabled(GenerationContext ctx) {
        Object value = ctx.getInput() == null ? null : ctx.getInput().get("seoKeywords");
        if (value instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
