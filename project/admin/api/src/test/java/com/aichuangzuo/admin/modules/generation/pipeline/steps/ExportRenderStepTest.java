package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ExportRenderStep 行为测试：
 * <ul>
 *   <li>无 finalDraft → 抛异常</li>
 *   <li>正常渲染 → format=markdown / platform 取自 templateId 前缀 / renderedDocument 非空</li>
 *   <li>templateId=xiaohongshu → platform=xiaohongshu 且文本含 #小红书</li>
 *   <li>fallbackToPlainText：draft JSON 不合法时返回原文</li>
 * </ul>
 */
class ExportRenderStepTest {

    private static GenerationContext ctx(String templateId, String finalDraftJson) {
        GenerationContext ctx = new GenerationContext();
        Map<String, Object> input = new HashMap<>();
        input.put("title", "测试标题");
        ctx.setInput(input);
        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        PromptTemplateStage s12 = new PromptTemplateStage();
        s12.setStageIndex(12);
        s12.setRuleConfig("{\"templateId\":\"" + templateId + "\",\"fallbackToPlainText\":true}");
        stages.put(12, s12);
        ctx.setStages(stages);
        ctx.setFinalDraftJson(finalDraftJson);
        return ctx;
    }

    @Test
    void process_shouldThrowWhenFinalDraftNull() {
        ExportRenderStep step = new ExportRenderStep();
        GenerationContext ctx = new GenerationContext();
        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    void process_shouldRenderWechatDefault() {
        ExportRenderStep step = new ExportRenderStep();
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"建立好奇\",\"content\":\"第 1 段内容\"}]}";
        GenerationContext ctx = ctx("wechat_default", draft);

        step.process(ctx);

        GenerationContext.ExportResult r = ctx.getExportResult();
        assertNotNull(r);
        assertEquals("markdown", r.getFormat());
        assertEquals("wechat", r.getPlatform());
        assertTrue(r.getRenderedDocument().contains("第 1 段内容"));
        assertTrue(r.getRenderedDocument().contains("— 完 —"));
    }

    @Test
    void process_shouldRenderResponsibilityAsH2() {
        // responsibility 是文章的小标题结构，应以 `## (N) resp` 形式写进 body，
        // 前端按 markdown H2 渲染（不要按字面 ## 显示，也不要删除）。
        ExportRenderStep step = new ExportRenderStep();
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"建立好奇\",\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("wechat_default", draft);

        step.process(ctx);

        String doc = ctx.getExportResult().getRenderedDocument();
        assertTrue(doc.contains("## (1) 建立好奇"), "body 应含 `## (N) responsibility` 小标题");
        assertTrue(doc.contains("内容"), "body 应含段落正文");
    }

    @Test
    void process_shouldNotEmbedTitleInBody() {
        // body 不能含 title：preview/编辑/卡片页都单独读 article.title，
        // 后端再塞就会出现双标题（preview 页 h1 + body 第一行各渲染一次）。
        ExportRenderStep step = new ExportRenderStep();
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";

        for (String tpl : new String[]{"wechat_default", "xiaohongshu_default",
                "toutiao_default", "zhihu_default", "baijiahao_default",
                "douyin_default", "general_default"}) {
            GenerationContext ctx = ctx(tpl, draft);
            step.process(ctx);
            assertFalse(ctx.getExportResult().getRenderedDocument().contains("测试标题"),
                    "templateId=" + tpl + " 不应在 body 嵌入 title");
        }
    }

    @Test
    void process_shouldRenderXiaohongshuPlatform() {
        ExportRenderStep step = new ExportRenderStep();
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("xiaohongshu_default", draft);

        step.process(ctx);

        assertEquals("xiaohongshu", ctx.getExportResult().getPlatform());
        assertTrue(ctx.getExportResult().getRenderedDocument().contains("#小红书"));
    }

    @Test
    void process_shouldFallbackToRawJsonWhenDraftUnparseable() {
        ExportRenderStep step = new ExportRenderStep();
        GenerationContext ctx = ctx("general_default", "not-a-json");

        step.process(ctx);

        // renderDraft 解析失败 → 返回原文 JSON；wrapByTemplate(general) 不加装饰
        assertTrue(ctx.getExportResult().getRenderedDocument().contains("not-a-json"));
    }
}
