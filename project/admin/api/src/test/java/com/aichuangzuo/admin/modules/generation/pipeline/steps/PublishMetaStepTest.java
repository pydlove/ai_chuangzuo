package com.aichuangzuo.admin.modules.generation.pipeline.steps;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.pipeline.AiGateway;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PublishMetaStep 行为测试：
 * <ul>
 *   <li>description + tags 解析后写入 ctx</li>
 *   <li>缺 description → 抛异常</li>
 *   <li>tags 缺失 / 空数组 / 全空白 → 抛异常</li>
 *   <li>tags 中空白项被过滤</li>
 * </ul>
 */
class PublishMetaStepTest {

    private AiGateway aiGateway;
    private PublishMetaStep step;

    @BeforeEach
    void setUp() {
        aiGateway = mock(AiGateway.class);
        step = new PublishMetaStep(aiGateway);
    }

    private static GenerationContext ctx() {
        GenerationContext ctx = new GenerationContext();
        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        PromptTemplateStage s = new PromptTemplateStage();
        s.setStageIndex(13);
        s.setAiPrompt("标题：{{title}} 最终稿：{{finalDraft}}");
        s.setEnabled(1);
        stages.put(13, s);
        ctx.setStages(stages);
        Map<String, Object> input = new HashMap<>();
        input.put("title", "测试标题");
        ctx.setInput(input);
        ctx.setFinalDraftJson("{\"draft\":[{\"paragraph_index\":1,\"content\":\"内容\"}]}");
        return ctx;
    }

    @Test
    void process_shouldStoreDescriptionAndTags() {
        when(aiGateway.call(any(), any(), any(), any()))
                .thenReturn("{\"description\":\"这是一段发布描述\",\"tags\":[\"时间管理\",\"效率提升\",\"自我管理\",\"职场成长\"]}");
        GenerationContext ctx = ctx();

        step.process(ctx);

        assertEquals("这是一段发布描述", ctx.getPublishDescription());
        assertEquals(4, ctx.getPublishTags().size());
        assertEquals("时间管理", ctx.getPublishTags().get(0));
    }

    @Test
    void process_shouldThrowWhenDescriptionMissing() {
        when(aiGateway.call(any(), any(), any(), any()))
                .thenReturn("{\"tags\":[\"标签1\"]}");
        GenerationContext ctx = ctx();

        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    void process_shouldThrowWhenTagsMissing() {
        when(aiGateway.call(any(), any(), any(), any()))
                .thenReturn("{\"description\":\"只有描述\"}");
        GenerationContext ctx = ctx();

        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    void process_shouldThrowWhenTagsEmpty() {
        when(aiGateway.call(any(), any(), any(), any()))
                .thenReturn("{\"description\":\"只有描述\",\"tags\":[]}");
        GenerationContext ctx = ctx();

        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    void process_shouldThrowWhenAllTagsBlank() {
        when(aiGateway.call(any(), any(), any(), any()))
                .thenReturn("{\"description\":\"只有描述\",\"tags\":[\"  \",\"\"]}");
        GenerationContext ctx = ctx();

        assertThrows(RuntimeException.class, () -> step.process(ctx));
    }

    @Test
    void process_shouldFilterBlankTags() {
        when(aiGateway.call(any(), any(), any(), any()))
                .thenReturn("{\"description\":\"描述\",\"tags\":[\"有效标签\",\"  \",\"另一个\"]}");
        GenerationContext ctx = ctx();

        step.process(ctx);

        assertEquals(2, ctx.getPublishTags().size());
        assertEquals("有效标签", ctx.getPublishTags().get(0));
        assertEquals("另一个", ctx.getPublishTags().get(1));
    }
}
