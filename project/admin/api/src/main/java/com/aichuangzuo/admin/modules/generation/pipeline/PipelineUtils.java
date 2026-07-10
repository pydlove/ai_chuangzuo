package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.service.PromptTemplateRenderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线 step 用的工具方法：占位符替换 / JSON 解析 / 错误处理等。
 *
 * <p>所有方法都是 static，方便 step 直接调用。
 */
public final class PipelineUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final PromptTemplateRenderService RENDER = new PromptTemplateRenderService();

    private PipelineUtils() {
    }

    /**
     * 渲染 stage 的 ai_prompt 模板：
     * <ol>
     *   <li>替换 {@code [user_context_block]} 为 stage 1 产出的 block 文本</li>
     *   <li>替换 {@code {{key}}} 占位符（从 ctx 派生 vars）</li>
     * </ol>
     */
    public static String renderAiPrompt(GenerationContext ctx, int stageIndex) {
        String template = ctx.stageAiPrompt(stageIndex);
        if (template == null) return "";
        // 1. [user_context_block] 替换
        String result = template.replace("[user_context_block]",
                ctx.getUserContextBlock() == null ? "" : ctx.getUserContextBlock());
        // 2. {{key}} 替换
        Map<String, Object> vars = buildVars(ctx);
        return RENDER.render(result, vars);
    }

    /** 从 ctx 派生 {{xxx}} 变量表。 */
    public static Map<String, Object> buildVars(GenerationContext ctx) {
        Map<String, Object> vars = new HashMap<>();
        Map<String, Object> in = ctx.getInput() == null ? Map.of() : ctx.getInput();
        vars.put("title", in.get("title"));
        vars.put("description", in.get("description"));
        vars.put("coreViewpoint", in.get("description"));     // alias
        vars.put("targetReader", in.getOrDefault("targetReader", "通用读者"));
        vars.put("platform", in.get("platform"));
        vars.put("wordCount", in.get("wordCount"));
        vars.put("userStylePrompt", in.get("userStylePrompt"));
        vars.put("toneTags", in.get("toneTags"));
        // 阶段产出
        vars.put("userContextBlock", ctx.getUserContextBlock());
        vars.put("outline", ctx.getOutlineJson());
        vars.put("materials", ctx.getMaterialsJson());
        vars.put("draft", ctx.getDraftJson());
        vars.put("rhythmIssues", formatRhythmIssues(ctx.getRhythmIssues()));
        vars.put("draftAfterRhythm", ctx.getDraftAfterRhythmJson());
        vars.put("toxicComments", formatToxicComments(ctx.getToxicComments()));
        vars.put("draftAfterTargeted", ctx.getDraftAfterTargetedJson());
        vars.put("finalDraft", ctx.getFinalDraftJson());
        vars.put("wordStats", formatWordStats(ctx.getWordStats()));
        // 字数目标
        Integer target = ctx.getTask() == null ? null : ctx.getTask().getWordLimitTarget();
        vars.put("targetWordCount", target == null ? 1500 : target);
        // 导出模板 ID
        vars.put("exportTemplateId", extractExportTemplateId(ctx));
        return vars;
    }

    private static String formatRhythmIssues(List<GenerationContext.RhythmIssue> issues) {
        if (issues == null || issues.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < issues.size(); i++) {
            GenerationContext.RhythmIssue r = issues.get(i);
            sb.append("  {")
                    .append("\"type\":\"").append(safe(r.getType())).append("\",")
                    .append("\"paragraphIndex\":").append(r.getParagraphIndex() == null ? -1 : r.getParagraphIndex()).append(",")
                    .append("\"text\":\"").append(safe(r.getText())).append("\",")
                    .append("\"suggestion\":\"").append(safe(r.getSuggestion())).append("\"}");
            if (i < issues.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatToxicComments(List<GenerationContext.ToxicComment> comments) {
        if (comments == null || comments.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < comments.size(); i++) {
            GenerationContext.ToxicComment c = comments.get(i);
            sb.append("  {")
                    .append("\"paragraph\":").append(c.getParagraph() == null ? -1 : c.getParagraph()).append(",")
                    .append("\"sentence\":").append(c.getSentence() == null ? -1 : c.getSentence()).append(",")
                    .append("\"type\":\"").append(safe(c.getType())).append("\",")
                    .append("\"original\":\"").append(safe(c.getOriginal())).append("\",")
                    .append("\"toxicComment\":\"").append(safe(c.getToxicComment())).append("\",")
                    .append("\"severity\":\"").append(safe(c.getSeverity())).append("\"}");
            if (i < comments.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String formatWordStats(GenerationContext.WordStats s) {
        if (s == null) return "{}";
        return "{\"target\":" + s.getTarget() + ",\"actual\":" + s.getActual()
                + ",\"diff\":" + s.getDiff() + ",\"status\":\"" + safe(s.getStatus()) + "\"}";
    }

    private static String extractExportTemplateId(GenerationContext ctx) {
        String cfg = ctx.stageRuleConfig(12);
        if (cfg == null || cfg.isBlank()) return "wechat_default";
        try {
            JsonNode n = MAPPER.readTree(cfg);
            JsonNode t = n.get("templateId");
            return t == null || t.asText().isBlank() ? "wechat_default" : t.asText();
        } catch (Exception e) {
            return "wechat_default";
        }
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * 解析 AI 返回的 JSON 字符串：先去掉 ```json 围栏，再 parse。
     *
     * @return 解析后的 JsonNode
     * @throws RuntimeException 解析失败时
     */
    public static JsonNode parseAiJson(String aiResp) {
        String cleaned = stripCodeFence(aiResp);
        try {
            return MAPPER.readTree(cleaned);
        } catch (Exception e) {
            throw new RuntimeException("AI 返回 JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 AI 返回的顶层字段（必须存在且为字符串）。
     */
    public static String requireString(JsonNode root, String field) {
        JsonNode n = root.path(field);
        if (n.isMissingNode() || n.isNull()) {
            throw new RuntimeException("AI 返回缺少字段: " + field);
        }
        return n.asText();
    }

    private static String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return t.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return t;
    }
}
