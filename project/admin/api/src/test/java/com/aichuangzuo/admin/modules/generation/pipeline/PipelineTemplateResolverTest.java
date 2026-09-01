package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateStageMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper;
import com.aichuangzuo.admin.modules.generation.service.PromptTemplateService;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineTemplateResolverTest {

    @Mock
    private PromptTemplateService templateService;
    @Mock
    private PromptTemplateStageMapper stageMapper;
    @Mock
    private PromptTemplateVersionMapper versionMapper;
    @Mock
    private PromptTemplateMapper templateMapper;

    @InjectMocks
    private PipelineTemplateResolver resolver;

    @Test
    void resolveInto_withLockedVersion_usesTemplateAndSnapshot() {
        PromptTemplate template = new PromptTemplate();
        template.setId(5L);
        template.setName("锁定模板");

        PromptTemplateVersion snapshot = new PromptTemplateVersion();
        snapshot.setVersion(3);
        snapshot.setConfigJson("{\"version\":3}");

        when(templateMapper.selectById(5L)).thenReturn(template);
        when(versionMapper.selectByTemplateId(5L)).thenReturn(List.of(snapshot));
        when(stageMapper.selectByTemplateId(5L)).thenReturn(List.of());

        GenerationContext ctx = new GenerationContext();
        resolver.resolveInto(ctx, 5L, 3);

        assertSame(template, ctx.getTemplate());
        assertEquals("{\"version\":3}", ctx.getConfigJsonSnapshot());
        assertEquals(14, ctx.getStages().size());
        verify(templateMapper).selectById(5L);
        verify(versionMapper).selectByTemplateId(5L);
        verifyNoInteractions(templateService);
    }

    @Test
    void resolveInto_fallbackWhenNull_usesEnabledTemplate() {
        PromptTemplate enabled = new PromptTemplate();
        enabled.setId(1L);
        enabled.setName("默认模板");

        when(templateService.findPublished()).thenReturn(Optional.of(enabled));
        when(stageMapper.selectByTemplateId(1L)).thenReturn(List.of());

        GenerationContext ctx = new GenerationContext();
        resolver.resolveInto(ctx, null, null);

        assertSame(enabled, ctx.getTemplate());
        assertNull(ctx.getConfigJsonSnapshot());
        assertEquals(14, ctx.getStages().size());
        verify(templateService).findPublished();
        verifyNoInteractions(templateMapper);
        verifyNoInteractions(versionMapper);
    }

    @Test
    void resolveInto_fillsMissingStagesWithDefaults() {
        PromptTemplate template = new PromptTemplate();
        template.setId(2L);

        PromptTemplateStage stage2 = new PromptTemplateStage();
        stage2.setTemplateId(2L);
        stage2.setStageIndex(2);
        stage2.setStageType(StageType.AI_PROMPT.code);
        stage2.setStageKey("outline");
        stage2.setAiPrompt("自定义大纲 prompt");
        stage2.setEnabled(1);

        when(templateMapper.selectById(2L)).thenReturn(template);
        when(versionMapper.selectByTemplateId(2L)).thenReturn(List.of());
        when(stageMapper.selectByTemplateId(2L)).thenReturn(List.of(stage2));

        GenerationContext ctx = new GenerationContext();
        resolver.resolveInto(ctx, 2L, 1);

        Map<Integer, PromptTemplateStage> stages = ctx.getStages();
        assertEquals(14, stages.size());
        assertEquals("自定义大纲 prompt", stages.get(2).getAiPrompt());

        // 缺失的 stage 用默认值补齐
        PromptTemplateStage stage1 = stages.get(1);
        assertEquals("intent_anchor", stage1.getStageKey());
        assertEquals(StageType.PASSTHROUGH.code, stage1.getStageType());
        assertEquals(1, stage1.getEnabled());

        PromptTemplateStage stage5 = stages.get(5);
        assertEquals("rhythm_detect", stage5.getStageKey());
        assertEquals(StageType.RULE_CONFIG.code, stage5.getStageType());
        assertNotNull(stage5.getRuleConfig());
        assertTrue(stage5.getRuleConfig().contains("uniformLengthDelta"));
    }

    @Test
    void resolveInto_backfillsNullPromptForExistingRows() {
        PromptTemplate template = new PromptTemplate();
        template.setId(2L);

        // stage 14 行存在，但 ai_prompt 为 null（模拟 V2.0.0_103 迁移插入的数据）
        PromptTemplateStage stage14 = new PromptTemplateStage();
        stage14.setTemplateId(2L);
        stage14.setStageIndex(14);
        stage14.setStageType(StageType.AI_PROMPT.code);
        stage14.setStageKey("ai_detect");
        stage14.setAiPrompt(null);
        stage14.setEnabled(1);

        // stage 5 行存在，但 rule_config 为 null
        PromptTemplateStage stage5 = new PromptTemplateStage();
        stage5.setTemplateId(2L);
        stage5.setStageIndex(5);
        stage5.setStageType(StageType.RULE_CONFIG.code);
        stage5.setStageKey("rhythm_detect");
        stage5.setRuleConfig(null);
        stage5.setEnabled(1);

        when(templateMapper.selectById(2L)).thenReturn(template);
        when(versionMapper.selectByTemplateId(2L)).thenReturn(List.of());
        when(stageMapper.selectByTemplateId(2L)).thenReturn(List.of(stage14, stage5));

        GenerationContext ctx = new GenerationContext();
        resolver.resolveInto(ctx, 2L, 1);

        Map<Integer, PromptTemplateStage> stages = ctx.getStages();
        assertEquals(14, stages.size());

        PromptTemplateStage resolved14 = stages.get(14);
        assertNotNull(resolved14.getAiPrompt());
        assertTrue(resolved14.getAiPrompt().contains("质量评估专家"));

        PromptTemplateStage resolved5 = stages.get(5);
        assertNotNull(resolved5.getRuleConfig());
        assertTrue(resolved5.getRuleConfig().contains("uniformLengthDelta"));
    }

    @Test
    void resolveInto_doesNotOverrideCustomPromptForExistingRows() {
        PromptTemplate template = new PromptTemplate();
        template.setId(2L);

        PromptTemplateStage stage14 = new PromptTemplateStage();
        stage14.setTemplateId(2L);
        stage14.setStageIndex(14);
        stage14.setStageType(StageType.AI_PROMPT.code);
        stage14.setStageKey("ai_detect");
        stage14.setAiPrompt("自定义质量检测 prompt");
        stage14.setEnabled(1);

        when(templateMapper.selectById(2L)).thenReturn(template);
        when(versionMapper.selectByTemplateId(2L)).thenReturn(List.of());
        when(stageMapper.selectByTemplateId(2L)).thenReturn(List.of(stage14));

        GenerationContext ctx = new GenerationContext();
        resolver.resolveInto(ctx, 2L, 1);

        assertEquals("自定义质量检测 prompt", ctx.getStages().get(14).getAiPrompt());
    }

    @Test
    void resolveInto_lockedTemplateNotFound_throws() {
        when(templateMapper.selectById(99L)).thenReturn(null);

        GenerationContext ctx = new GenerationContext();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveInto(ctx, 99L, 1));

        assertEquals(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void resolveInto_fallbackNoEnabled_throws() {
        when(templateService.findPublished()).thenReturn(Optional.empty());

        GenerationContext ctx = new GenerationContext();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveInto(ctx));

        assertEquals(AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_PUBLISHED.getCode(), ex.getCode());
    }
}
