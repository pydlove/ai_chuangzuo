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
        assertEquals(12, ctx.getStages().size());
        verify(templateMapper).selectById(5L);
        verify(versionMapper).selectByTemplateId(5L);
        verifyNoInteractions(templateService);
    }

    @Test
    void resolveInto_fallbackWhenNull_usesEnabledTemplate() {
        PromptTemplate enabled = new PromptTemplate();
        enabled.setId(1L);
        enabled.setName("默认模板");

        when(templateService.findEnabled()).thenReturn(Optional.of(enabled));
        when(stageMapper.selectByTemplateId(1L)).thenReturn(List.of());

        GenerationContext ctx = new GenerationContext();
        resolver.resolveInto(ctx, null, null);

        assertSame(enabled, ctx.getTemplate());
        assertNull(ctx.getConfigJsonSnapshot());
        assertEquals(12, ctx.getStages().size());
        verify(templateService).findEnabled();
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
        assertEquals(12, stages.size());
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
    void resolveInto_lockedTemplateNotFound_throws() {
        when(templateMapper.selectById(99L)).thenReturn(null);

        GenerationContext ctx = new GenerationContext();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveInto(ctx, 99L, 1));

        assertEquals(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void resolveInto_fallbackNoEnabled_throws() {
        when(templateService.findEnabled()).thenReturn(Optional.empty());

        GenerationContext ctx = new GenerationContext();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resolver.resolveInto(ctx));

        assertEquals(AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_ENABLED.getCode(), ex.getCode());
    }
}
