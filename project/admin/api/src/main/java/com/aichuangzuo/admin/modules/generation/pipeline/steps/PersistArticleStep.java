package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationStep;
import com.aichuangzuo.admin.modules.generation.pipeline.StepResult;
import com.aichuangzuo.admin.modules.generation.service.ArticleWriteInternalClient;
import com.aichuangzuo.shared.entity.GenerationTask;
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
        payload.put("body", ctx.getExportResult() == null ? null : ctx.getExportResult().getRenderedDocument());
        payload.put("summary", in.get("description"));
        payload.put("wordCount", ctx.getWordStats() == null ? 0 : ctx.getWordStats().getActual());
        payload.put("platform", in.get("platform"));
        payload.put("style", in.get("styleRef"));
        payload.put("inputParam", task.getInputParam());
        payload.put("wordLimitTarget", task.getWordLimitTarget() == null ? 1500 : task.getWordLimitTarget());

        String articleBizNo = articleClient.saveArticle(payload);
        ctx.setArticleBizNo(articleBizNo);
        log.info("article 持久化完成 task={} articleBizNo={}", task.getId(), articleBizNo);
        return StepResult.CONTINUE;
    }
}
