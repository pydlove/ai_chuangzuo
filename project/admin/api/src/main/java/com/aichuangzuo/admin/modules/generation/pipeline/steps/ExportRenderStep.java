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
 * <p><b>双层导出架构（设计文档 §8）：</b>
 * <ol>
 *   <li><b>后端（本 step）：</b>把 {@code finalDraft}（结构化 JSON：每段 paragraph_index / responsibility /
 *       content）展开成<b>平台无关的 markdown 规范文</b>，写入 {@code ctx.exportResult.renderedDocument}。
 *       markdown 是唯一权威的「内容真源」，后续所有展示形式都从它派生，避免多处维护同一份内容。</li>
 *   <li><b>前端 templatePresets：</b>用户在预览页选平台（微信公众号 / 小红书 / 知乎 ...），前端按
 *       {@code ctx.exportResult.platform}（来自 {@code rule_config.templateId} 前缀）从
 *       {@code shared.js::templatePresets} 取对应样式模板，套到 markdown 上做可视化（卡片图 / 字号 / 标签）。</li>
 * </ol>
 *
 * <p><b>为什么不让后端直接把 HTML 写死：</b>
 * <ul>
 *   <li>平台模板样式频繁迭代（小红书要加话题标签 / 知乎要加参考链接位），放在前端 JS 可以热更新；后端写死要重启。</li>
 *   <li>用户在预览页切换平台是高频操作，前端本地切换零延迟；如果每次换平台都打后端会慢且费额度。</li>
 *   <li>后端只负责「内容对不对」，前端负责「好不好看」——关注点分离。</li>
 * </ul>
 *
 * <p><b>渲染失败回退：</b>{@code rule_config.fallbackToPlainText=true}（默认）时，
 * 若 finalDraft 解析失败，{@link #renderDraft} 会把原文 JSON 当字符串返回，保证用户至少看到文字。
 *
 * <p><b>本期实现范围：</b>后端 markdown wrapper 仅加平台特定的开头/结尾装饰（emoji / 话题标签），
 * 作为「后端也做了一点样式」的最小示意；真正的排版细节由前端 templatePresets 接管。
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

        // 后端层：把 draft 数组的 content 拼成平台无关 markdown，前后套模板 wrapper
        // （前端 layer 会按 platform 取 templatePresets 进一步装饰）
        String body = renderDraft(ctx);
        String rendered = wrapByTemplate(templateId, body);

        GenerationContext.ExportResult result = new GenerationContext.ExportResult();
        result.setFormat("markdown");
        result.setPlatform(extractPlatform(templateId));
        result.setRenderedDocument(rendered);
        result.setSourceDraftJson(ctx.getFinalDraftJson());
        ctx.setExportResult(result);
        log.info("导出模板渲染完成 platform={} format=markdown templateId={}",
                result.getPlatform(), templateId);
        return StepResult.CONTINUE;
    }

    /**
     * 把 finalDraft JSON 展开成平台无关 markdown：
     * <ul>
     *   <li>body 不含 title——preview/编辑/卡片页都单独读 article.title，塞进来会双标题</li>
     *   <li>responsibility 作为二级小标题（`## (N) resp`），是文章结构的一部分，前端按 markdown H2 渲染</li>
     *   <li>每段 content 拼成段落，按 paragraph_index 顺序，段间空一行</li>
     * </ul>
     *
     * <p>解析失败时回退到 finalDraftJson 原文（fallback 策略由外层 fallbackToPlainText 配置）。
     */
    private String renderDraft(GenerationContext ctx) {
        try {
            JsonNode root = MAPPER.readTree(ctx.getFinalDraftJson());
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (JsonNode para : root.path("draft")) {
                int idx = para.path("paragraph_index").asInt(0);
                String resp = para.path("responsibility").asText("");
                String content = para.path("content").asText("");
                if (!first) sb.append("\n\n");
                if (!resp.isBlank()) sb.append("## (").append(idx).append(") ").append(resp).append("\n\n");
                sb.append(content);
                first = false;
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("finalDraft 解析失败，返回原文", e);
            return ctx.getFinalDraftJson();
        }
    }

    private String wrapByTemplate(String templateId, String body) {
        // 各平台 wrapper 不再叠 title（title 已在 article.title 里，叠加会双标题）。
        // 平台特定的尾部签名保留，作为「后端做一点样式」的最小示意。
        switch (templateId) {
            case "xiaohongshu_default":
                return body + "\n\n#小红书 #爱创作";
            case "toutiao_default":
                return body + "\n\n（本文由爱创作生成）";
            case "zhihu_default":
                return "> 本文由爱创作 AI 生成。\n\n" + body;
            case "baijiahao_default":
                return body + "\n\n—— 来自爱创作 ——";
            case "douyin_default":
                return body;
            case "general_default":
                return body;
            case "wechat_default":
            default:
                return body + "\n\n— 完 —";
        }
    }

    private String extractPlatform(String templateId) {
        int idx = templateId.indexOf('_');
        return idx > 0 ? templateId.substring(0, idx) : "wechat";
    }
}
