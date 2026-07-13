package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.request.CloneTemplateRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateSaveRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateStageSaveItem;
import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateStageMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineStage;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateAdminVO;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateStageVO;
import com.aichuangzuo.shared.creative.CreativeTemplateConstants;
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceStageTest {

    @Mock
    private PromptTemplateMapper templateMapper;

    @Mock
    private PromptTemplateStageMapper stageMapper;

    @Mock
    private PromptTemplateVersionMapper versionMapper;

    @InjectMocks
    private PromptTemplateService service;

    private PromptTemplate sampleTemplate(Long id) {
        PromptTemplate t = new PromptTemplate();
        t.setId(id);
        t.setName("测试模板");
        t.setIsDeleted(0);
        t.setTenantId(0L);
        return t;
    }

    @Test
    void create_shouldAutoInsertTwelveStagesWithDefaults() {
        PromptTemplateSaveRequest req = new PromptTemplateSaveRequest();
        req.setName("新模板");
        req.setRemark("test");
        // 不传 stages，让后端全用默认值

        // mock insert 后回填 id
        org.mockito.Mockito.doAnswer((inv) -> {
            PromptTemplate t = inv.getArgument(0);
            t.setId(10L);
            return 1;
        }).when(templateMapper).insert(any(PromptTemplate.class));

        Long id = service.create(req, 99L);

        assertEquals(Long.valueOf(10L), id);

        // 应该 insert 12 次 stage
        ArgumentCaptor<PromptTemplateStage> captor = ArgumentCaptor.forClass(PromptTemplateStage.class);
        verify(stageMapper, times(12)).insert(captor.capture());
        List<PromptTemplateStage> inserted = captor.getAllValues();

        // 验证顺序是 1-12
        for (int i = 0; i < 12; i++) {
            assertEquals(Integer.valueOf(i + 1), inserted.get(i).getStageIndex());
            assertEquals(Long.valueOf(10L), inserted.get(i).getTemplateId());
            assertEquals(Long.valueOf(99L), inserted.get(i).getCreatedBy());
        }

        // 验证 AI 阶段有默认 prompt；规则阶段有默认 config
        PromptTemplateStage stage4 = inserted.get(3);  // 第 4 阶段
        assertEquals("ai_prompt", stage4.getStageType());
        assertNotNull(stage4.getAiPrompt());
        assertTrue(stage4.getAiPrompt().contains("{{userStylePrompt}}"));

        PromptTemplateStage stage5 = inserted.get(4);  // 第 5 阶段
        assertEquals("rule_config", stage5.getStageType());
        assertNotNull(stage5.getRuleConfig());
        assertTrue(stage5.getRuleConfig().contains("uniformLengthDelta"));
    }

    @Test
    void create_shouldUseUserValuesWhenProvided() {
        PromptTemplateSaveRequest req = new PromptTemplateSaveRequest();
        req.setName("新模板");
        List<PromptTemplateStageSaveItem> items = new ArrayList<>();
        PromptTemplateStageSaveItem it = new PromptTemplateStageSaveItem();
        it.setStageIndex(4);
        it.setAiPrompt("用户自定义的 prompt");
        it.setEnabled(1);
        items.add(it);
        req.setStages(items);

        org.mockito.Mockito.doAnswer((inv) -> {
            PromptTemplate t = inv.getArgument(0);
            t.setId(20L);
            return 1;
        }).when(templateMapper).insert(any(PromptTemplate.class));

        service.create(req, 1L);

        ArgumentCaptor<PromptTemplateStage> captor = ArgumentCaptor.forClass(PromptTemplateStage.class);
        verify(stageMapper, times(12)).insert(captor.capture());
        PromptTemplateStage stage4 = captor.getAllValues().get(3);
        assertEquals("用户自定义的 prompt", stage4.getAiPrompt());
    }

    @Test
    void update_shouldReplaceAllTwelveStages() {
        PromptTemplate exist = sampleTemplate(5L);
        when(templateMapper.selectById(5L)).thenReturn(exist);

        PromptTemplateSaveRequest req = new PromptTemplateSaveRequest();
        req.setName("改名");
        List<PromptTemplateStageSaveItem> items = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            PromptTemplateStageSaveItem it = new PromptTemplateStageSaveItem();
            it.setStageIndex(i);
            it.setAiPrompt("p" + i);
            it.setRuleConfig("{\"k\":1}");
            it.setEnabled(1);
            items.add(it);
        }
        req.setStages(items);

        service.update(5L, req, 1L);

        verify(stageMapper, times(1)).deleteByTemplateId(5L);
        ArgumentCaptor<PromptTemplateStage> captor = ArgumentCaptor.forClass(PromptTemplateStage.class);
        verify(stageMapper, times(12)).insert(captor.capture());
    }

    @Test
    void update_shouldNotTouchStagesWhenEmpty() {
        PromptTemplate exist = sampleTemplate(5L);
        when(templateMapper.selectById(5L)).thenReturn(exist);

        PromptTemplateSaveRequest req = new PromptTemplateSaveRequest();
        req.setName("只改名");
        req.setStages(new ArrayList<>());

        service.update(5L, req, 1L);

        verify(stageMapper, never()).deleteByTemplateId(anyLong());
        verify(stageMapper, never()).insert((PromptTemplateStage) any());
    }

    @Test
    void initStages_shouldInsertTwelveWhenNone() {
        PromptTemplate exist = sampleTemplate(7L);
        when(templateMapper.selectById(7L)).thenReturn(exist);
        when(stageMapper.selectByTemplateId(7L)).thenReturn(new ArrayList<>());

        int inserted = service.initStages(7L, 1L);

        assertEquals(12, inserted);
        verify(stageMapper, times(12)).insert(any(PromptTemplateStage.class));
    }

    @Test
    void initStages_shouldSkipWhenAlreadyHasStages() {
        PromptTemplate exist = sampleTemplate(7L);
        when(templateMapper.selectById(7L)).thenReturn(exist);
        List<PromptTemplateStage> existing = new ArrayList<>();
        existing.add(new PromptTemplateStage());
        when(stageMapper.selectByTemplateId(7L)).thenReturn(existing);

        int inserted = service.initStages(7L, 1L);

        assertEquals(0, inserted);
        verify(stageMapper, never()).insert((PromptTemplateStage) any());
    }

    @Test
    void detail_shouldReturnStagesWithMetadata() {
        PromptTemplate exist = sampleTemplate(8L);
        when(templateMapper.selectById(8L)).thenReturn(exist);

        // 模拟只有 4 阶段（不全） — service 应该用 PipelineStage 默认补齐剩下 8 个
        List<PromptTemplateStage> rows = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            PromptTemplateStage row = new PromptTemplateStage();
            row.setTemplateId(8L);
            row.setStageIndex(i);
            row.setStageKey(PipelineStage.byIndex(i).key);
            row.setStageType(PipelineStage.byIndex(i).type.code);
            row.setEnabled(1);
            row.setAiPrompt("custom" + i);
            rows.add(row);
        }
        when(stageMapper.selectByTemplateId(8L)).thenReturn(rows);

        PromptTemplateAdminVO vo = service.detail(8L);

        assertNotNull(vo);
        assertNotNull(vo.getStages());
        assertEquals(12, vo.getStages().size());
        assertFalse(vo.getStagesInitialized());

        // 第 4 阶段用用户值
        PromptTemplateStageVO stage4 = vo.getStages().get(3);
        assertEquals("custom4", stage4.getAiPrompt());

        // 第 5 阶段用 PipelineStage 默认值
        PromptTemplateStageVO stage5 = vo.getStages().get(4);
        assertNotNull(stage5.getRuleConfig());
        assertNotNull(stage5.getConfigFields());
    }

    @Test
    void detail_shouldMarkBuiltinTemplateAsImmutable() {
        PromptTemplate exist = sampleTemplate(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        when(templateMapper.selectById(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID)).thenReturn(exist);
        when(stageMapper.selectByTemplateId(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID))
                .thenReturn(new ArrayList<>());

        PromptTemplateAdminVO vo = service.detail(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);

        assertNotNull(vo);
        assertTrue(vo.getIsBuiltin(), "id=" + CreativeTemplateConstants.DEFAULT_TEMPLATE_ID + " 应被标为内置");
    }

    @Test
    void detail_shouldMarkCustomTemplateAsNotBuiltin() {
        PromptTemplate exist = sampleTemplate(42L);
        when(templateMapper.selectById(42L)).thenReturn(exist);
        when(stageMapper.selectByTemplateId(42L)).thenReturn(new ArrayList<>());

        PromptTemplateAdminVO vo = service.detail(42L);

        assertNotNull(vo);
        assertFalse(vo.getIsBuiltin(), "id=42 不应被标为内置");
    }

    @Test
    void delete_shouldRefuseBuiltinTemplate() {
        PromptTemplate exist = sampleTemplate(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        when(templateMapper.selectById(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID)).thenReturn(exist);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.delete(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID, 1L));
        assertEquals(AdminGenerationErrorCode.PROMPT_TEMPLATE_BUILTIN_IMMUTABLE.getCode(), ex.getCode());

        verify(templateMapper, never()).deleteById(anyLong());
        verify(stageMapper, never()).deleteByTemplateId(anyLong());
    }

    @Test
    void delete_shouldAllowCustomTemplate() {
        PromptTemplate exist = sampleTemplate(42L);
        when(templateMapper.selectById(42L)).thenReturn(exist);

        service.delete(42L, 1L);

        verify(stageMapper, times(1)).deleteByTemplateId(42L);
        verify(templateMapper, times(1)).deleteById(42L);
    }

    // ===== 阶段 2：发布 / 下线 / 克隆 =====

    @Test
    void publish_shouldCreateVersionAndSetStatus() {
        PromptTemplate exist = sampleTemplate(1L);
        exist.setLatestPublishedVersion(null);
        when(templateMapper.selectById(1L)).thenReturn(exist);
        when(stageMapper.selectByTemplateId(1L)).thenReturn(new ArrayList<>());
        when(versionMapper.selectByTemplateId(1L)).thenReturn(new ArrayList<>());

        Long version = service.publish(1L, "首次发布", 99L);

        assertEquals(Long.valueOf(1L), version);
        ArgumentCaptor<PromptTemplateVersion> captor = ArgumentCaptor.forClass(PromptTemplateVersion.class);
        verify(versionMapper, times(1)).insert(captor.capture());
        PromptTemplateVersion saved = captor.getValue();
        assertEquals(TemplateStatus.PUBLISHED.code, saved.getVersionStatus());
        assertEquals(1, saved.getVersion());
        assertEquals(TemplateStatus.PUBLISHED.code, exist.getTemplateStatus());
        assertEquals(1, exist.getLatestPublishedVersion());
    }

    @Test
    void publish_shouldIncrementVersion() {
        PromptTemplate exist = sampleTemplate(2L);
        exist.setLatestPublishedVersion(3);
        when(templateMapper.selectById(2L)).thenReturn(exist);
        when(stageMapper.selectByTemplateId(2L)).thenReturn(new ArrayList<>());
        when(versionMapper.selectByTemplateId(2L)).thenReturn(new ArrayList<>());

        Long version = service.publish(2L, "第 4 次", 1L);

        assertEquals(Long.valueOf(4L), version);
        ArgumentCaptor<PromptTemplateVersion> captor = ArgumentCaptor.forClass(PromptTemplateVersion.class);
        verify(versionMapper, times(1)).insert(captor.capture());
        assertEquals(4, captor.getValue().getVersion());
        assertEquals(Integer.valueOf(4), exist.getLatestPublishedVersion());
    }

    @Test
    void offline_shouldSetStatusToOffline() {
        PromptTemplate exist = sampleTemplate(1L);
        exist.setTemplateStatus(TemplateStatus.PUBLISHED.code);
        when(templateMapper.selectById(1L)).thenReturn(exist);
        when(versionMapper.selectLatestPublished(1L)).thenReturn(null);

        service.offline(1L, 1L);

        assertEquals(TemplateStatus.OFFLINE.code, exist.getTemplateStatus());
    }

    @Test
    void offline_shouldRejectNonPublishedTemplate() {
        PromptTemplate exist = sampleTemplate(1L);
        exist.setTemplateStatus(TemplateStatus.DRAFT.code);
        when(templateMapper.selectById(1L)).thenReturn(exist);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.offline(1L, 1L));
        assertEquals(AdminGenerationErrorCode.PROMPT_TEMPLATE_INVALID_STATUS.getCode(), ex.getCode());
        verify(versionMapper, never()).updateById(any(PromptTemplateVersion.class));
    }

    @Test
    void clone_shouldCreateDraftWithCopiedStages() {
        PromptTemplate src = sampleTemplate(1L);
        src.setTemplateStatus(TemplateStatus.PUBLISHED.code);
        src.setLatestPublishedVersion(2);
        when(templateMapper.selectById(1L)).thenReturn(src);
        org.mockito.Mockito.doAnswer((inv) -> {
            PromptTemplate t = inv.getArgument(0);
            t.setId(100L);
            return 1;
        }).when(templateMapper).insert(any(PromptTemplate.class));

        List<PromptTemplateStage> srcStages = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            PromptTemplateStage s = new PromptTemplateStage();
            s.setStageIndex(i);
            s.setStageKey("k" + i);
            s.setStageType("ai_prompt");
            s.setAiPrompt("p" + i);
            s.setEnabled(1);
            srcStages.add(s);
        }
        when(stageMapper.selectByTemplateId(1L)).thenReturn(srcStages);

        CloneTemplateRequest req = new CloneTemplateRequest();
        req.setName("默认-副本");
        Long newId = service.clone(1L, req, 99L);

        assertEquals(Long.valueOf(100L), newId);
        ArgumentCaptor<PromptTemplate> tCaptor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(templateMapper, times(1)).insert(tCaptor.capture());
        PromptTemplate inserted = tCaptor.getValue();
        assertEquals("默认-副本", inserted.getName());
        assertEquals(TemplateStatus.DRAFT.code, inserted.getTemplateStatus());
        assertNull(inserted.getLatestPublishedVersion());
        verify(stageMapper, times(3)).insert(any(PromptTemplateStage.class));
    }
}
