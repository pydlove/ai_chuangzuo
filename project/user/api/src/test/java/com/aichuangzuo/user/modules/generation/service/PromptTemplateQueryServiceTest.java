package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.creative.CreativeTemplateConstants;
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.generation.mapper.UserPromptTemplateMapper;
import com.aichuangzuo.user.modules.generation.vo.PromptTemplatePublicVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptTemplateQueryServiceTest {

    @Mock
    private UserPromptTemplateMapper mapper;

    @InjectMocks
    private PromptTemplateQueryService service;

    @Test
    void listPublished_returnsVos() {
        PromptTemplate t1 = publishedTemplate(1L, "默认模板");
        PromptTemplate t2 = publishedTemplate(2L, "风格 A");
        t2.setLatestPublishedVersion(3);

        when(mapper.selectPublished()).thenReturn(List.of(t1, t2));

        List<PromptTemplatePublicVO> result = service.listPublished();

        assertEquals(2, result.size());
        assertEquals("默认模板", result.get(0).getName());
        assertTrue(result.get(0).getIsBuiltin());
        assertEquals("风格 A", result.get(1).getName());
        assertFalse(result.get(1).getIsBuiltin());
        assertEquals(3, result.get(1).getLatestPublishedVersion());
        verify(mapper).selectPublished();
    }

    @Test
    void detail_published_returnsVo() {
        PromptTemplate t = publishedTemplate(2L, "已发布模板");
        t.setLatestPublishedVersion(2);

        when(mapper.selectById(2L)).thenReturn(t);

        PromptTemplatePublicVO vo = service.detail(2L);

        assertEquals(2L, vo.getId());
        assertEquals("已发布模板", vo.getName());
        assertEquals(2, vo.getLatestPublishedVersion());
        verify(mapper).selectById(2L);
    }

    @Test
    void detail_notPublished_throws() {
        PromptTemplate t = new PromptTemplate();
        t.setId(3L);
        t.setTemplateStatus(TemplateStatus.DRAFT.code);

        when(mapper.selectById(3L)).thenReturn(t);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(3L));
        assertEquals(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void detail_notFound_throws() {
        when(mapper.selectById(4L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(4L));
        assertEquals(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED.getCode(), ex.getCode());
    }

    private PromptTemplate publishedTemplate(Long id, String name) {
        PromptTemplate t = new PromptTemplate();
        t.setId(id);
        t.setName(name);
        t.setTemplateStatus(TemplateStatus.PUBLISHED.code);
        t.setEnabled(1);
        if (id.equals(CreativeTemplateConstants.DEFAULT_TEMPLATE_ID)) {
            t.setLatestPublishedVersion(1);
        }
        return t;
    }
}
