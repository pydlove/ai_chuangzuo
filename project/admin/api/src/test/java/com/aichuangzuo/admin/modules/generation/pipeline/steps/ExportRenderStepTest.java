package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.admin.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ExportRenderStep 行为测试：
 * <ul>
 *   <li>无 finalDraft → 抛异常</li>
 *   <li>从 task input 读 template key，查库拿签名</li>
 *   <li>签名在尾部（end）或头部（start）</li>
 *   <li>模板无签名 → body 不追加</li>
 *   <li>body 不含 title</li>
 *   <li>responsibility 渲染为 ## (N) resp 小标题</li>
 *   <li>fallbackToPlainText：draft JSON 不合法时返回原文</li>
 * </ul>
 */
class ExportRenderStepTest {

    private ExportTemplateMapper templateMapper;
    private ExportRenderStep step;

    @BeforeEach
    void setUp() {
        templateMapper = mock(ExportTemplateMapper.class);
        step = new ExportRenderStep(templateMapper);
    }

    private static GenerationContext ctx(String templateKey, String finalDraftJson) {
        GenerationContext ctx = new GenerationContext();
        Map<String, Object> input = new HashMap<>();
        input.put("title", "测试标题");
        input.put("template", templateKey);
        input.put("platform", "wechat");
        ctx.setInput(input);
        ctx.setFinalDraftJson(finalDraftJson);
        return ctx;
    }

    private ExportTemplate tpl(String key, String sigText, String sigPos) {
        ExportTemplate t = new ExportTemplate();
        t.setTemplateKey(key);
        t.setSignatureText(sigText);
        t.setSignaturePosition(sigPos);
        return t;
    }

    @Test
    void process_shouldThrowWhenFinalDraftNull() {
        GenerationContext ctx = new GenerationContext();
        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    void process_shouldAppendEndSignature() {
        when(templateMapper.selectByKey("wechat")).thenReturn(tpl("wechat", "— 完 —", "end"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("wechat", draft);

        step.process(ctx);

        String doc = ctx.getExportResult().getRenderedDocument();
        assertTrue(doc.endsWith("— 完 —"), "尾部应有微信签名");
        assertTrue(doc.contains("内容"));
    }

    @Test
    void process_shouldPrependStartSignature() {
        when(templateMapper.selectByKey("zhihu-answer")).thenReturn(tpl("zhihu-answer", "> 本文由爱创作 AI 生成。", "start"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("zhihu-answer", draft);

        step.process(ctx);

        String doc = ctx.getExportResult().getRenderedDocument();
        assertTrue(doc.startsWith("> 本文由爱创作 AI 生成。"), "头部应有知乎签名");
    }

    @Test
    void process_shouldAppendXiaohongshuSignature() {
        when(templateMapper.selectByKey("xiaohongshu")).thenReturn(tpl("xiaohongshu", "#小红书 #爱创作", "end"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("xiaohongshu", draft);

        step.process(ctx);

        assertTrue(ctx.getExportResult().getRenderedDocument().endsWith("#小红书 #爱创作"));
    }

    @Test
    void process_shouldNotAppendWhenSignatureNull() {
        when(templateMapper.selectByKey("dark")).thenReturn(tpl("dark", null, "end"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("dark", draft);

        step.process(ctx);

        assertEquals("内容", ctx.getExportResult().getRenderedDocument().trim());
    }

    @Test
    void process_shouldNotAppendWhenTemplateNotFound() {
        when(templateMapper.selectByKey(anyString())).thenReturn(null);
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("nonexistent", draft);

        step.process(ctx);

        assertEquals("内容", ctx.getExportResult().getRenderedDocument().trim());
    }

    @Test
    void process_shouldRenderResponsibilityAsH2() {
        when(templateMapper.selectByKey(anyString())).thenReturn(tpl("wechat", null, "end"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"responsibility\":\"建立好奇\",\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("wechat", draft);

        step.process(ctx);

        String doc = ctx.getExportResult().getRenderedDocument();
        assertTrue(doc.contains("## (1) 建立好奇"), "body 应含 `## (N) responsibility` 小标题");
        assertTrue(doc.contains("内容"), "body 应含段落正文");
    }

    @Test
    void process_shouldNotEmbedTitleInBody() {
        when(templateMapper.selectByKey(anyString())).thenReturn(tpl("wechat", "— 完 —", "end"));
        String draft = "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}";
        GenerationContext ctx = ctx("wechat", draft);

        step.process(ctx);

        assertFalse(ctx.getExportResult().getRenderedDocument().contains("测试标题"),
                "body 不应嵌入 title");
    }

    @Test
    void process_shouldFallbackToRawJsonWhenDraftUnparseable() {
        when(templateMapper.selectByKey(anyString())).thenReturn(tpl("wechat", null, "end"));
        GenerationContext ctx = ctx("wechat", "not-a-json");

        step.process(ctx);

        assertTrue(ctx.getExportResult().getRenderedDocument().contains("not-a-json"));
    }

    @Test
    void process_shouldUseInputPlatform() {
        when(templateMapper.selectByKey(anyString())).thenReturn(tpl("wechat", null, "end"));
        GenerationContext ctx = ctx("wechat", "{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}");
        ctx.getInput().put("platform", "xiaohongshu");

        step.process(ctx);

        assertEquals("xiaohongshu", ctx.getExportResult().getPlatform());
    }
}
