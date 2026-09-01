package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.service.PromptTemplateRenderService;
import com.aichuangzuo.shared.utils.LlmJsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    // ALLOW_UNQUOTED_FIELD_NAMES：兜底 `{a:1}` 这种 JS/Python 风格的裸 key。
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
            .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true)
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
        vars.put("userSkillPrompt", in.get("userSkillPrompt"));
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
     * 字面换行/制表符）、{@code ALLOW_SINGLE_QUOTES}（容忍 'foo' 单引号定界）和
     * {@code ALLOW_UNQUOTED_FIELD_NAMES}（容忍 `{a:1}` 裸 key），覆盖 M3 多类常见
     * JSON 瑕疵；裸引号场景另外由 {@link #repairInnerQuotes(String)} 兜底。
     *
     * <p>解析失败时会尝试 {@link #repairInnerQuotes(String)} 兜底：MiniMax-M3 等中文
     * 模型有时在字符串值里直接写裸 "（如 "30岁" 这种引用），破坏 JSON 结构；这里
     * 用启发式把"看起来像字符串内部的引号"转义掉，再重试一次。
     *
     * @return 解析后的 JsonNode
     * @throws RuntimeException 解析失败时
     */
    /**
     * 解析 AI 返回的 JSON 字符串：先去掉 ```json 围栏，再用 LlmJsonParser 解析。
     *
     * <p>底层 MAPPER 已开启 {@code ALLOW_UNESCAPED_CONTROL_CHARS}（容忍字符串值内
     * 字面换行/制表符）、{@code ALLOW_SINGLE_QUOTES}（容忍 'foo' 单引号定界）和
     * {@code ALLOW_UNQUOTED_FIELD_NAMES}（容忍 {@code {a:1}} 裸 key），覆盖 M3 多类常见
     * JSON 瑕疵；LlmJsonParser 在此基础上再提供：
     * <ul>
     *   <li>多 JSON 块时取最后一个能解析成功的对象/数组（避免模型复述 prompt 示例）</li>
     *   <li>字符串包裹的 JSON 自动 unwrap</li>
     *   <li>字符串值内未转义英文双引号兜底转义</li>
     *   <li>trailing comma 容忍</li>
     * </ul>
     *
     * @return 解析后的 JsonNode
     * @throws RuntimeException 解析失败时
     */
    public static JsonNode parseAiJson(String aiResp) {
        try {
            return LlmJsonParser.parseLenient(MAPPER, aiResp);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 返回 JSON 解析失败: " + e.getMessage()
                    + truncationHint(e.getMessage()));
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

    // removed: repairInnerQuotes / stripCodeFence — now handled by LlmJsonParser

    /**
     * 把 AI 正文里的中文单引号 ‘’ 统一换成中文双引号 “”。
     *
     * <p>模型写引用/强调时习惯用单引号（JSON 里 ASCII 双引号要转义，单引号省事），
     * 但中文排版规范外层引用应使用双引号。ASCII 单引号 ' 不动——可能是英文撇号
     * （Don't / it's），无法和引号用法区分。
     */
    public static String normalizeQuotes(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.replace('‘', '“').replace('’', '”');
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
}
