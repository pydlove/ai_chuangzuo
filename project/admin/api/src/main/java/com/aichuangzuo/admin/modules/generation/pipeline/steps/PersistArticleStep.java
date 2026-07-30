package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineUtils;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.aichuangzuo.admin.modules.generation.service.ArticleWriteInternalClient;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * pipeline 收尾 step：把 exportResult（如果存在）或 finalDraft 写到 article。
 *
 * <p>走 user-api 内部接口（与原 {@code ArticleWriteInternalClient} 兼容），拿到 articleBizNo 写回 ctx。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersistArticleStep implements GenerationStep {

    private final ArticleWriteInternalClient articleClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int stageIndex() { return 100; }

    @Override
    public String name() { return "persist-article"; }

    @Override
    public boolean enabled(GenerationContext ctx) {
        // 没 finalDraftJson 就不跑（前面某步失败）
        return ctx.getFinalDraftJson() != null && !ctx.getFinalDraftJson().isBlank();
    }

    @Override
    public StepResult process(GenerationContext ctx) {
        GenerationTask task = ctx.getTask();
        if (task == null) {
            throw new RuntimeException("ctx.task 为空，无法持久化 article");
        }
        Map<String, Object> in = ctx.getInput() == null ? Map.of() : ctx.getInput();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("userId", task.getTargetUserId());
        payload.put("title", in.get("title"));
        payload.put("body", ctx.getExportResult() == null ? null
                : PipelineUtils.normalizeQuotes(ctx.getExportResult().getRenderedDocument()));
        payload.put("summary", in.get("description"));
        payload.put("wordCount", resolveWordCount(ctx));
        payload.put("platform", in.get("platform"));
        payload.put("skill", in.get("skillRef"));
        payload.put("template", in.get("template"));
        payload.put("description", PipelineUtils.normalizeQuotes(ctx.getPublishDescription()));
        payload.put("tags", ctx.getPublishTags());
        payload.put("inputParam", task.getInputParam());
        payload.put("wordLimitTarget", task.getWordLimitTarget() == null ? 1500 : task.getWordLimitTarget());

        String articleBizNo = articleClient.saveArticle(payload);
        ctx.setArticleBizNo(articleBizNo);
        log.info("article 持久化完成 task={} articleBizNo={}", task.getId(), articleBizNo);

        return StepResult.CONTINUE;
    }

    /**
     * 解析最终字数：优先用 stage 10 统计结果，未统计（如极速 3 阶段模板禁用 stage 10）
     * 时从 finalDraftJson 兜底计算，避免 article.word_count 落库为 0。
     */
    private int resolveWordCount(GenerationContext ctx) {
        if (ctx.getWordStats() != null) {
            return Math.max(0, ctx.getWordStats().getActual());
        }
        return countWordsFromFinalDraft(ctx.getFinalDraftJson());
    }

    private int countWordsFromFinalDraft(String finalDraftJson) {
        if (finalDraftJson == null || finalDraftJson.isBlank()) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(finalDraftJson);
            int total = 0;
            for (JsonNode para : root.path("draft")) {
                total += countChars(para.path("content").asText(""));
            }
            log.debug("finalDraftJson 兜底字数统计 actual={}", total);
            return total;
        } catch (Exception e) {
            log.warn("finalDraftJson 兜底字数统计失败: {}", e.getMessage());
            return 0;
        }
    }

    private int countChars(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isPunctuation(c)) {
                continue;
            }
            n++;
        }
        return n;
    }

    private boolean isPunctuation(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || "，。！？、；：\"'\"'（）《》【】「」【】…—～·".indexOf(c) >= 0;
    }
}
