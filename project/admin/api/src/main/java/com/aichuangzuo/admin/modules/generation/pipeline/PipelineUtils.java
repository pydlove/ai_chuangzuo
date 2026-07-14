package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.service.PromptTemplateRenderService;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线 step 用的工具方法：占位符替换 / JSON 解析 / 错误处理等。
 *
 * <p>所有方法都是 static，方便 step 直接调用。
 */
public final class PipelineUtils {

    // ALLOW_UNESCAPED_CONTROL_CHARS：MiniMax-M3 等模型常在字符串值里写字面换行/制表符
    // （未转义控制字符，code 9/10/13），Jackson 严格模式会拒收；开启后读成 \n/\t 原样保留。
    // ALLOW_SINGLE_QUOTES：M3 偶尔用 'foo' 单引号风格定界（Python literal 风），常见于
    // 中文术语字段（如 'AI写作变现'）；严格模式认作 "was expecting double-quote"。
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
            .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
            .build();
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
     * <p>底层 MAPPER 已开启 {@code ALLOW_UNESCAPED_CONTROL_CHARS}（容忍字符串值内
     * 字面换行/制表符）和 {@code ALLOW_SINGLE_QUOTES}（容忍 'foo' 单引号定界），
     * 覆盖 M3 三类常见 JSON 瑕疵：控制字符 / 单引号 / 裸引号（后者由
     * {@link #repairInnerQuotes(String)} 兜底）。
     *
     * <p>解析失败时会尝试 {@link #repairInnerQuotes(String)} 兜底：MiniMax-M3 等中文
     * 模型有时在字符串值里直接写裸 "（如 "30岁" 这种引用），破坏 JSON 结构；这里
     * 用启发式把"看起来像字符串内部的引号"转义掉，再重试一次。
     *
     * @return 解析后的 JsonNode
     * @throws RuntimeException 解析失败时
     */
    public static JsonNode parseAiJson(String aiResp) {
        String cleaned = stripCodeFence(aiResp);
        try {
            return MAPPER.readTree(cleaned);
        } catch (Exception first) {
            String repaired = repairInnerQuotes(cleaned);
            if (repaired.equals(cleaned)) {
                throw new RuntimeException("AI 返回 JSON 解析失败: " + first.getMessage()
                        + truncationHint(first.getMessage()));
            }
            try {
                return MAPPER.readTree(repaired);
            } catch (Exception second) {
                throw new RuntimeException("AI 返回 JSON 解析失败: " + first.getMessage()
                        + truncationHint(first.getMessage()));
            }
        }
    }

    /**
     * Jackson 报 "Unexpected end-of-input" / "end of input within/between" → 输入被截断，
     * 不是 JSON 格式瑕疵（解析器救不了）。给出一句话提示，方便定位是 max_tokens 太小。
     */
    private static String truncationHint(String jacksonMsg) {
        if (jacksonMsg == null) return "";
        String m = jacksonMsg.toLowerCase();
        if (m.contains("end-of-input") || m.contains("end of input")) {
            return "（疑似 AI 输出被 max_tokens 截断，可尝试调大创作设置里的 default_max_tokens）";
        }
        return "";
    }

    /**
     * 启发式修复字符串值内的裸 "：扫描字符流，跟踪 inString 状态；遇到 " 时
     * 向后看一个非空白字符，若不是 JSON 结构符（, } ] :）就判定为内部引号，转义成 \"。
     *
     * <p>局限：内部引号后紧跟结构符（如 "text with ", more"）无法识别，会判定为闭合
     * 引号导致修复失败——这种场景下 {@link #parseAiJson} 仍会抛原始异常。
     *
     * <p>已正确转义的 \" 不会被重复转义：进入 escaped 状态时跳过下一个字符。
     */
    private static String repairInnerQuotes(String json) {
        if (json == null || json.isEmpty()) return json;
        StringBuilder out = new StringBuilder(json.length() + 32);
        boolean inString = false;
        boolean escaped = false;
        boolean changed = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
                continue;
            }
            if (inString && c == '\\') {
                out.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                if (!inString) {
                    inString = true;
                    out.append(c);
                } else {
                    // 向后看一个非空白字符，判断是闭合引号还是内部引号
                    int j = i + 1;
                    while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
                    char next = j < json.length() ? json.charAt(j) : '\0';
                    if (next == ',' || next == '}' || next == ']' || next == ':' || next == '\0') {
                        inString = false;
                        out.append(c);
                    } else {
                        out.append("\\\"");
                        changed = true;
                    }
                }
            } else {
                out.append(c);
            }
        }
        return changed ? out.toString() : json;
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
            if (firstNewline < 0) return t;
            int lastFence = t.lastIndexOf("```");
            if (lastFence > firstNewline) {
                return t.substring(firstNewline + 1, lastFence).trim();
            }
            // 无闭合围栏（模型只写了开头 ```json，或输出被 max_tokens 截断）：
            // 去掉首行围栏再尝试解析，JSON 完整即可恢复
            return t.substring(firstNewline + 1).trim();
        }
        return t;
    }
}
