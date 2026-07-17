package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.admin.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 第 12 阶段：导出模板渲染
 *
 * <p>把 {@code finalDraft}（结构化 JSON）展开成平台无关的 markdown 规范文，
 * 再根据用户选择的导出模板（task input 中的 {@code template} key）从
 * {@code a_export_template} 表读取签名文本和位置，追加到 body 头部或尾部。
 *
 * <p>视觉样式（颜色/字号/边框）不落 body，由前端按 {@code article.template}
 * 从 {@code a_export_template.visual_style_json} 读取后渲染。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportRenderStep implements GenerationStep {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ExportTemplateMapper exportTemplateMapper;

    @Override
    public int stageIndex() { return 12; }

    @Override
    public String name() { return "export-render"; }

    @Override
    public StepResult process(GenerationContext ctx) {
        if (ctx.getFinalDraftJson() == null) {
            throw new RuntimeException("finalDraftJson 为空，无法渲染导出模板");
        }

        String body = renderDraft(ctx);
        String templateKey = resolveTemplateKey(ctx);
        String rendered = applySignature(templateKey, body);

        GenerationContext.ExportResult result = new GenerationContext.ExportResult();
        result.setFormat("markdown");
        result.setPlatform(resolvePlatform(ctx, templateKey));
        result.setRenderedDocument(rendered);
        result.setSourceDraftJson(ctx.getFinalDraftJson());
        ctx.setExportResult(result);
        log.info("导出模板渲染完成 platform={} format=markdown templateKey={}",
                result.getPlatform(), templateKey);
        return StepResult.CONTINUE;
    }

    /**
     * 把 finalDraft JSON 展开成平台无关 markdown：
     * <ul>
     *   <li>body 不含 title——preview/编辑/卡片页都单独读 article.title，塞进来会双标题</li>
     *   <li>responsibility 作为二级小标题（`## (N) resp`），是文章结构的一部分，前端按 markdown H2 渲染</li>
     *   <li>每段 content 拼成段落，按 paragraph_index 顺序，段间空一行</li>
     * </ul>
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

    /** 从 task input 读 template key，缺省 "wechat"。 */
    private String resolveTemplateKey(GenerationContext ctx) {
        if (ctx.getInput() != null) {
            Object t = ctx.getInput().get("template");
            if (t != null && !t.toString().isBlank()) {
                return t.toString();
            }
        }
        return "wechat";
    }

    /** 从 a_export_template 查签名，追加到 body。 */
    private String applySignature(String templateKey, String body) {
        ExportTemplate tpl = exportTemplateMapper.selectByKey(templateKey);
        if (tpl == null || tpl.getSignatureText() == null || tpl.getSignatureText().isBlank()) {
            return body;
        }
        if ("start".equalsIgnoreCase(tpl.getSignaturePosition())) {
            return tpl.getSignatureText() + "\n\n" + body;
        }
        return body + "\n\n" + tpl.getSignatureText();
    }

    /** 优先从 input.platform 取平台，其次从 templateKey 前缀推断。 */
    private String resolvePlatform(GenerationContext ctx, String templateKey) {
        if (ctx.getInput() != null) {
            Object p = ctx.getInput().get("platform");
            if (p != null && !p.toString().isBlank()) {
                return p.toString();
            }
        }
        int idx = templateKey.indexOf('-');
        return idx > 0 ? templateKey.substring(0, idx) : templateKey;
    }
}
