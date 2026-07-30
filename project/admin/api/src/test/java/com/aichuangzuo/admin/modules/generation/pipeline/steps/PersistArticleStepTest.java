package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.service.ArticleWriteInternalClient;
import com.aichuangzuo.shared.entity.GenerationTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistArticleStepTest {

    private final ArticleWriteInternalClient articleClient = mock(ArticleWriteInternalClient.class);
    private final PersistArticleStep step = new PersistArticleStep(articleClient);

    @Test
    void process_shouldUseWordStatsWhenAvailable() {
        GenerationContext ctx = makeCtx();
        GenerationContext.WordStats stats = new GenerationContext.WordStats();
        stats.setActual(1234);
        ctx.setWordStats(stats);
        ctx.setFinalDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"这是一段正文。\"}]}");
        ctx.setExportResult(makeExportResult());

        when(articleClient.saveArticle(ArgumentCaptor.forClass(Map.class).capture()))
                .thenReturn("A000000000000001");

        step.process(ctx);

        assertNotNull(ctx.getArticleBizNo());
        Map<String, Object> payload = capturePayload();
        assertEquals(1234, payload.get("wordCount"));
    }

    @Test
    void process_shouldFallbackToCountingFinalDraftWhenWordStatsMissing() {
        GenerationContext ctx = makeCtx();
        // 极速 3 阶段等模板会禁用 stage 10，导致 wordStats 为空
        ctx.setWordStats(null);
        ctx.setFinalDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"这是一段正文。\"}]}");
        ctx.setExportResult(makeExportResult());

        when(articleClient.saveArticle(ArgumentCaptor.forClass(Map.class).capture()))
                .thenReturn("A000000000000002");

        step.process(ctx);

        Map<String, Object> payload = capturePayload();
        // "这是一段正文" 排除标点后 6 个字
        assertEquals(6, payload.get("wordCount"));
    }

    private GenerationContext makeCtx() {
        GenerationContext ctx = new GenerationContext();
        GenerationTask task = new GenerationTask();
        task.setId(1L);
        task.setTargetUserId(10L);
        task.setWordLimitTarget(1500);
        ctx.setTask(task);
        ctx.putExtra("title", "测试标题");
        ctx.putExtra("platform", "wechat");
        return ctx;
    }

    private GenerationContext.ExportResult makeExportResult() {
        GenerationContext.ExportResult result = new GenerationContext.ExportResult();
        result.setRenderedDocument("## 测试\n\n这是一段正文。");
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturePayload() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(articleClient).saveArticle(captor.capture());
        return captor.getValue();
    }
}
