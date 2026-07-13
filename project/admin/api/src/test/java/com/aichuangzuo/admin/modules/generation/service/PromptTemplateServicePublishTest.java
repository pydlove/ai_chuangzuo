package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateStageMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper;
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServicePublishTest {

    @Mock
    private PromptTemplateMapper mapper;

    @Mock
    private PromptTemplateStageMapper stageMapper;

    @Mock
    private PromptTemplateVersionMapper versionMapper;

    @InjectMocks
    private PromptTemplateService service;

    /** 纯 Mockito 环境没有 SqlSessionFactory，手动初始化实体 TableInfo 供 lambda wrapper 解析列名。 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, PromptTemplate.class);
        TableInfoHelper.initTableInfo(assistant, PromptTemplateVersion.class);
    }

    @Test
    void publish_shouldOfflineOtherPublishedTemplatesAndVersions() {
        PromptTemplate t = new PromptTemplate();
        t.setId(2L);
        t.setTemplateStatus(TemplateStatus.DRAFT.code);
        when(mapper.selectById(2L)).thenReturn(t);
        when(stageMapper.selectByTemplateId(2L)).thenReturn(List.of());
        when(versionMapper.selectByTemplateId(2L)).thenReturn(List.of());

        Long version = service.publish(2L, "note", 1L);

        assertEquals(1L, version);
        // 先下线其他已发布模板 + 其他模板的已发布版本（update(null, wrapper) 各一次）
        verify(mapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(versionMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        // 目标模板置为已发布 v1
        ArgumentCaptor<PromptTemplate> captor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(TemplateStatus.PUBLISHED.code, captor.getValue().getTemplateStatus());
        assertEquals(1, captor.getValue().getLatestPublishedVersion());
    }
}
