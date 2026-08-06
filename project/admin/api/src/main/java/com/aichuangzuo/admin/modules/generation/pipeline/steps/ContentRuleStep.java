package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 5 阶段：可配置内容规则处理。
 *
 * <p>该阶段通过 {@code t_prompt_template_stage.stage_key} 决定具体行为，实现同一段位
 * 不同模板可执行不同规则，方便极速 3 阶段等轻量模板插入后处理逻辑而不新增 stage 序号。
 *
 * <ul>
 *   <li>stage_key = {@code content_post_process}：执行内容后处理（如单引号转中文双引号）。</li>
 *   <li>其他（默认 {@code rhythm_detect}）：保持原有韵律检测行为，兼容默认去 AI 味模板。</li>
 * </ul>
 */
@Slf4j
@Component
public class ContentRuleStep implements GenerationStep {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String KEY_CONTENT_POST_PROCESS = "content_post_process";

    @Override
    public int stageIndex() { return 5; }

    @Override
    public String name() { return "content-rule"; }

    @Override
    public StepResult process(GenerationContext ctx) {
        String stageKey = resolveStageKey(ctx);
        if (KEY_CONTENT_POST_PROCESS.equals(stageKey)) {
            return postProcess(ctx);
        }
        // 默认兼容原韵律检测（stage_key = rhythm_detect 或缺省）
        return rhythmDetect(ctx);
    }

    private String resolveStageKey(GenerationContext ctx) {
        PromptTemplateStage stage = ctx.getStages().get(5);
        return stage != null && stage.getStageKey() != null ? stage.getStageKey() : "rhythm_detect";
    }

    // ===== content_post_process 分支 =====

    private StepResult postProcess(GenerationContext ctx) {
        String cfg = ctx.stageRuleConfig(5);
        boolean replaceSingleQuotes = readBool(cfg, "singleQuoteToChineseQuotes", false);
        boolean replaceCornerBrackets = readBool(cfg, "cornerBracketToChineseQuotes", false);
        if (!replaceSingleQuotes && !replaceCornerBrackets) {
            log.info("stage 5 content_post_process 未启用任何规则，跳过");
            return StepResult.CONTINUE;
        }

        String draftJson = ctx.getFinalDraftJson();
        if (draftJson == null || draftJson.isBlank()) {
            draftJson = ctx.getDraftJson();
        }
        if (draftJson == null || draftJson.isBlank()) {
            log.warn("stage 5 content_post_process 无可用 draft，跳过");
            return StepResult.CONTINUE;
        }

        try {
            JsonNode root = MAPPER.readTree(draftJson);
            if (!root.isObject()) {
                log.warn("stage 5 content_post_process draft 不是对象，跳过");
                return StepResult.CONTINUE;
            }
            ObjectNode obj = (ObjectNode) root;
            if (obj.has("draft") && obj.get("draft").isArray()) {
                for (JsonNode para : obj.get("draft")) {
                    if (para instanceof ObjectNode p) {
                        if (p.has("content")) {
                            p.put("content", applyPostProcessRules(p.path("content").asText(""),
                                    replaceSingleQuotes, replaceCornerBrackets));
                        }
                        // 小标题（responsibility）中的引号也要处理
                        if (p.has("responsibility")) {
                            p.put("responsibility", applyPostProcessRules(p.path("responsibility").asText(""),
                                    replaceSingleQuotes, replaceCornerBrackets));
                        }
                    }
                }
            }
            if (obj.has("description") && obj.get("description").isTextual()) {
                String desc = applyPostProcessRules(obj.get("description").asText(""),
                        replaceSingleQuotes, replaceCornerBrackets);
                obj.put("description", desc);
                // DraftStep 可能已经把 description 提到 ctx.publishDescription，这里同步刷新
                ctx.setPublishDescription(desc);
            }
            String processed = MAPPER.writeValueAsString(obj);
            ctx.setFinalDraftJson(processed);
            // 让 draftJson 与 finalDraftJson 保持一致，避免后续 stage 读到旧数据
            if (ctx.getDraftJson() != null && !ctx.getDraftJson().isBlank()) {
                ctx.setDraftJson(processed);
            }
            log.info("stage 5 content_post_process 完成（singleQuoteToChineseQuotes={}, cornerBracketToChineseQuotes={}）",
                    replaceSingleQuotes, replaceCornerBrackets);
        } catch (Exception e) {
            log.warn("stage 5 content_post_process 处理失败，保持原文", e);
        }
        return StepResult.CONTINUE;
    }

    private String applyPostProcessRules(String text, boolean replaceSingleQuotes, boolean replaceCornerBrackets) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        if (replaceSingleQuotes) {
            result = replaceSingleQuotes(result);
        }
        if (replaceCornerBrackets) {
            result = replaceCornerBrackets(result);
        }
        return result;
    }

    /**
     * 将成对单引号 {@code '} 交替替换为中文双引号 {@code “} / {@code ”}。
     * <p>例如：{@code 他说'你好'。} → {@code 他说“你好”。}
     */
    private String replaceSingleQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        boolean open = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                sb.append(open ? '“' : '”');
                open = !open;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将日文角引号 {@code 「} / {@code 」} 替换为中文双引号 {@code “} / {@code ”}。
     * <p>例如：{@code 他说「你好」。} → {@code 他说“你好”。}
     */
    private String replaceCornerBrackets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '「') {
                sb.append('“');
            } else if (c == '」') {
                sb.append('”');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean readBool(String cfg, String key, boolean def) {
        if (cfg == null || cfg.isBlank()) {
            return def;
        }
        try {
            JsonNode n = MAPPER.readTree(cfg);
            return n.path(key).asBoolean(def);
        } catch (Exception e) {
            return def;
        }
    }

    // ===== rhythm_detect 分支（兼容默认模板） =====

    private StepResult rhythmDetect(GenerationContext ctx) {
        if (ctx.getDraftJson() == null || ctx.getDraftJson().isBlank()) {
            log.warn("draftJson 为空，跳过韵律检测");
            return StepResult.CONTINUE;
        }

        int uniformDelta = readInt(ctx, "uniformLengthDelta", 5);
        int breathMax = readInt(ctx, "breathMaxChars", 35);
        int monotonousCount = readInt(ctx, "monotonousStartCount", 3);

        JsonNode root;
        try {
            root = MAPPER.readTree(ctx.getDraftJson());
        } catch (Exception e) {
            log.warn("draftJson 解析失败，跳过韵律检测", e);
            return StepResult.CONTINUE;
        }
        JsonNode drafts = root.path("draft");
        if (!drafts.isArray()) {
            log.warn("draft 不是数组，跳过韵律检测");
            return StepResult.CONTINUE;
        }

        List<Sentence> all = new ArrayList<>();
        for (JsonNode para : drafts) {
            int pIdx = para.path("paragraph_index").asInt(0);
            String content = para.path("content").asText("");
            splitSentences(content).forEach(s -> {
                s.paragraphIndex = pIdx;
                all.add(s);
            });
        }

        List<GenerationContext.RhythmIssue> issues = new ArrayList<>();
        for (int i = 0; i + 2 < all.size(); i++) {
            Sentence a = all.get(i), b = all.get(i + 1), c = all.get(i + 2);
            if (Math.abs(a.len - b.len) <= uniformDelta
                    && Math.abs(b.len - c.len) <= uniformDelta
                    && a.len > 4) {
                issues.add(issue("uniform_length", b.paragraphIndex, b.text,
                        "三句长度接近（差 ≤ " + uniformDelta + " 字），拆短或合并"));
            }
        }
        for (Sentence s : all) {
            if (s.len > breathMax) {
                issues.add(issue("no_breath", s.paragraphIndex, s.text,
                        "超过 " + breathMax + " 字无句末标点，建议加逗号/分号/破折号"));
            }
        }
        for (int i = 0; i + 4 < all.size(); i++) {
            int sameStart = 0;
            for (int j = i; j < i + 5; j++) {
                String head = firstChar(all.get(j).text);
                if ("这那我们然而因此所以但是不过虽然".contains(head)) {
                    sameStart++;
                }
            }
            if (sameStart >= monotonousCount) {
                issues.add(issue("monotonous_start", all.get(i + 1).paragraphIndex,
                        all.get(i + 1).text,
                        "连续 5 句中有 " + sameStart + " 句以常见词开头，建议换 2 个句首"));
            }
        }

        ctx.setRhythmIssues(issues);
        log.info("韵律检测完成：{} 个问题（阈值 uniform±{} breath>{} monotonous≥{}）",
                issues.size(), uniformDelta, breathMax, monotonousCount);
        return StepResult.CONTINUE;
    }

    private static int readInt(GenerationContext ctx, String key, int def) {
        String cfg = ctx.stageRuleConfig(5);
        if (cfg == null || cfg.isBlank()) {
            return def;
        }
        try {
            JsonNode n = MAPPER.readTree(cfg);
            return n.path(key).asInt(def);
        } catch (Exception e) {
            return def;
        }
    }

    private static GenerationContext.RhythmIssue issue(String type, Integer pIdx, String text, String suggestion) {
        GenerationContext.RhythmIssue r = new GenerationContext.RhythmIssue();
        r.setType(type);
        r.setParagraphIndex(pIdx);
        r.setText(text);
        r.setSuggestion(suggestion);
        return r;
    }

    private static List<Sentence> splitSentences(String content) {
        List<Sentence> out = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return out;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            buf.append(c);
            if (c == '。' || c == '！' || c == '？' || c == '\n') {
                String s = buf.toString().trim();
                if (!s.isEmpty()) {
                    Sentence sent = new Sentence();
                    sent.text = s;
                    sent.len = s.replaceAll("\\s", "").length();
                    out.add(sent);
                }
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            String s = buf.toString().trim();
            if (!s.isEmpty()) {
                Sentence sent = new Sentence();
                sent.text = s;
                sent.len = s.replaceAll("\\s", "").length();
                out.add(sent);
            }
        }
        return out;
    }

    private static String firstChar(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return String.valueOf(c);
            }
        }
        return s.substring(0, 1);
    }

    private static class Sentence {
        int paragraphIndex;
        String text;
        int len;
    }
}
