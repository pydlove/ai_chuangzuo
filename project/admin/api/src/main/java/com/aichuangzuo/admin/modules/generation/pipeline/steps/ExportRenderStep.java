package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 第 12 阶段：导出模板渲染（rule_config）
 *
 * <p>按用户选的平台模板把 finalDraft 渲染成对应平台的可发布格式。
 * 本期先实现「按 templateId 套样式」的简化版：套一个平台特定的 markdown wrapper；
 * 真正完整的渲染逻辑（卡片图 / 排版 / 标签）可在后续迭代。
 */
@Slf4j
@Component
public class ExportRenderStep implements GenerationStep {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int stageIndex() { return 12; }

    @Override
    public String name() { return "export-render"; }

    @Override
    public StepResult process(GenerationContext ctx) {
        if (ctx.getFinalDraftJson() == null) {
            throw new RuntimeException("finalDraftJson 为空，无法渲染导出模板");
        }
        String cfg = ctx.stageRuleConfig(12);
        Map<String, Object> config = new HashMap<>();
        if (cfg != null && !cfg.isBlank()) {
            try {
                JsonNode n = MAPPER.readTree(cfg);
                n.fields().forEachRemaining(e -> config.put(e.getKey(), e.getValue().asText()));
            } catch (Exception ignore) {
            }
        }
        String templateId = String.valueOf(config.getOrDefault("templateId", "wechat_default"));
        String title = ctx.getInput() == null ? "" : String.valueOf(ctx.getInput().getOrDefault("title", ""));

        // 简化渲染：把 draft 数组的 content 拼成 markdown，前后套模板 wrapper
        String body = renderDraft(ctx, title);
        String rendered = wrapByTemplate(templateId, title, body);

        GenerationContext.ExportResult result = new GenerationContext.ExportResult();
        result.setFormat("markdown");
        result.setPlatform(extractPlatform(templateId));
        result.setRenderedDocument(rendered);
        result.setSourceDraftJson(ctx.getFinalDraftJson());
        ctx.setExportResult(result);
        log.info("导出模板渲染完成 platform={} format=markdown", result.getPlatform());
        return StepResult.CONTINUE;
    }

    private String renderDraft(GenerationContext ctx, String title) {
        try {
            JsonNode root = MAPPER.readTree(ctx.getFinalDraftJson());
            StringBuilder sb = new StringBuilder();
            if (title != null && !title.isBlank()) {
                sb.append("# ").append(title).append("\n\n");
            }
            for (JsonNode para : root.path("draft")) {
                int idx = para.path("paragraph_index").asInt(0);
                String resp = para.path("responsibility").asText("");
                String content = para.path("content").asText("");
                if (idx > 0) sb.append("\n\n");
                if (!resp.isBlank()) sb.append("## (").append(idx).append(") ").append(resp).append("\n\n");
                sb.append(content);
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("finalDraft 解析失败，返回原文", e);
            return ctx.getFinalDraftJson();
        }
    }

    private String wrapByTemplate(String templateId, String title, String body) {
        // 简化：每平台只加不同的开头/结尾注释
        switch (templateId) {
            case "xiaohongshu_default":
                return "🌟 " + title + " 🌟\n\n" + body + "\n\n#小红书 #爱创作";
            case "toutiao_default":
                return "【" + title + "】\n\n" + body + "\n\n（本文由爱创作生成）";
            case "zhihu_default":
                return "# " + title + "\n\n> 本文由爱创作 AI 生成。\n\n" + body;
            case "baijiahao_default":
                return body + "\n\n—— 来自爱创作 ——";
            case "douyin_default":
                return title + "｜" + body.substring(0, Math.min(80, body.length())) + "...";
            case "general_default":
                return body;
            case "wechat_default":
            default:
                return title + "\n\n" + body + "\n\n— 完 —";
        }
    }

    private String extractPlatform(String templateId) {
        int idx = templateId.indexOf('_');
        return idx > 0 ? templateId.substring(0, idx) : "wechat";
    }
}
