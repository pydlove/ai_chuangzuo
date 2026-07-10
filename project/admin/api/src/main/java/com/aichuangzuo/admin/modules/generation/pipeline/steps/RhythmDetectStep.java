package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineUtils;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 第 5 阶段：韵律检测（rule_config）
 *
 * <p>三个量化指标：
 * <ol>
 *   <li>句长离散度：连续 3 句字数差异在 ±N 字以内 → AI 均匀节律</li>
 *   <li>标点换气间隔：超 N 字仍未出现句末标点 → 长句无气口</li>
 *   <li>首词单调性：连续 5 句中 N 个以上词性相同 → 句首单调</li>
 * </ol>
 *
 * <p>阈值从 stage 5 的 rule_config JSON 读，缺失时用 PipelineStage 默认。
 */
@Slf4j
@Component
public class RhythmDetectStep implements GenerationStep {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int stageIndex() { return 5; }

    @Override
    public String name() { return "rhythm-detect"; }

    @Override
    public StepResult process(GenerationContext ctx) {
        if (ctx.getDraftJson() == null || ctx.getDraftJson().isBlank()) {
            log.warn("draftJson 为空，跳过韵律检测");
            return StepResult.CONTINUE;
        }

        // 1. 读阈值配置
        int uniformDelta = readInt(ctx, "uniformLengthDelta", 5);
        int breathMax = readInt(ctx, "breathMaxChars", 35);
        int monotonousCount = readInt(ctx, "monotonousStartCount", 3);

        // 2. 解析 draft 数组
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

        // 3. 拆句 + 跑指标
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
        // 指标 1：句长离散度（连续 3 句）
        for (int i = 0; i + 2 < all.size(); i++) {
            Sentence a = all.get(i), b = all.get(i + 1), c = all.get(i + 2);
            if (Math.abs(a.len - b.len) <= uniformDelta
                    && Math.abs(b.len - c.len) <= uniformDelta
                    && a.len > 4) {
                issues.add(issue("uniform_length", b.paragraphIndex, b.text,
                        "三句长度接近（差 ≤ " + uniformDelta + " 字），拆短或合并"));
            }
        }
        // 指标 2：标点换气
        for (Sentence s : all) {
            if (s.len > breathMax) {
                issues.add(issue("no_breath", s.paragraphIndex, s.text,
                        "超过 " + breathMax + " 字无句末标点，建议加逗号/分号/破折号"));
            }
        }
        // 指标 3：首词单调（连续 5 句中 3 句以上以"这/那/我们/然而/因此"开头）
        for (int i = 0; i + 4 < all.size(); i++) {
            int sameStart = 0;
            for (int j = i; j < i + 5; j++) {
                String head = firstChar(all.get(j).text);
                if ("这那我们然而因此所以但是不过虽然".contains(head)) sameStart++;
            }
            if (sameStart >= monotonousCount) {
                issues.add(issue("monotonous_start", all.get(i + 1).paragraphIndex,
                        all.get(i + 1).text,
                        "连续 5 句中有 " + sameStart + " 句以「" + monotonousHeadWord(sameStart) + "」开头，建议换 2 个句首"));
            }
        }

        ctx.setRhythmIssues(issues);
        log.info("韵律检测完成：{} 个问题（阈值 uniform±{} breath>{} monotonous≥{}）",
                issues.size(), uniformDelta, breathMax, monotonousCount);
        return StepResult.CONTINUE;
    }

    private static int readInt(GenerationContext ctx, String key, int def) {
        String cfg = ctx.stageRuleConfig(5);
        if (cfg == null || cfg.isBlank()) return def;
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

    /** 中文粗拆句：以 。！？ 切。 */
    private static List<Sentence> splitSentences(String content) {
        List<Sentence> out = new ArrayList<>();
        if (content == null || content.isBlank()) return out;
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
        if (s == null || s.isEmpty()) return "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) return String.valueOf(c);
        }
        return s.substring(0, 1);
    }

    private static String monotonousHeadWord(int n) {
        return "这/那/我们/然而";
    }

    private static class Sentence {
        int paragraphIndex;
        String text;
        int len;
    }
}
