package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 第 10 阶段：字数统计（rule_config）
 *
 * <p>纯统计：算出 finalDraft 总字数（可配置排除标点 / 空白），跟 target 比较得 status。
 */
@Slf4j
@Component
public class WordCountStep implements GenerationStep {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int stageIndex() { return 10; }

    @Override
    public String name() { return "word-count"; }

    @Override
    public StepResult process(GenerationContext ctx) {
        int target = ctx.getTask() == null ? 1500
                : (ctx.getTask().getWordLimitTarget() == null ? 1500 : ctx.getTask().getWordLimitTarget());
        if (ctx.getFinalDraftJson() == null || ctx.getFinalDraftJson().isBlank()) {
            log.warn("finalDraftJson 为空，字数计 0");
            GenerationContext.WordStats s = new GenerationContext.WordStats();
            s.setTarget(target);
            s.setActual(0);
            s.setDiff(-target);
            s.setStatus("under");
            ctx.setWordStats(s);
            return StepResult.CONTINUE;
        }
        boolean excludePunct = readBool(ctx, "excludePunctuation", true);
        boolean excludeSpaces = readBool(ctx, "excludeSpaces", true);

        JsonNode root;
        try {
            root = MAPPER.readTree(ctx.getFinalDraftJson());
        } catch (Exception e) {
            log.warn("finalDraftJson 解析失败", e);
            return StepResult.CONTINUE;
        }
        int total = 0;
        for (JsonNode para : root.path("draft")) {
            String c = para.path("content").asText("");
            total += countChars(c, excludePunct, excludeSpaces);
        }
        int diff = total - target;
        String status = diff > 0 ? "over" : (diff < 0 ? "under" : "ok");
        GenerationContext.WordStats s = new GenerationContext.WordStats();
        s.setTarget(target);
        s.setActual(total);
        s.setDiff(diff);
        s.setStatus(status);
        ctx.setWordStats(s);
        log.info("字数统计 actual={} target={} status={}", total, target, status);
        return StepResult.CONTINUE;
    }

    private static boolean readBool(GenerationContext ctx, String key, boolean def) {
        String cfg = ctx.stageRuleConfig(10);
        if (cfg == null || cfg.isBlank()) return def;
        try {
            JsonNode n = MAPPER.readTree(cfg);
            return n.path(key).asBoolean(def);
        } catch (Exception e) {
            return def;
        }
    }

    private static int countChars(String s, boolean excludePunct, boolean excludeSpaces) {
        if (s == null || s.isEmpty()) return 0;
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (excludeSpaces && Character.isWhitespace(c)) continue;
            if (excludePunct && isPunctuation(c)) continue;
            n++;
        }
        return n;
    }

    private static boolean isPunctuation(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || "，。！？、；：\"'\"'（）《》【】「」【】…—～·".indexOf(c) >= 0;
    }
}
