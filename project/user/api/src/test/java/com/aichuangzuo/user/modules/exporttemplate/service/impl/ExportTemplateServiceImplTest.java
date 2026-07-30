package com.aichuangzuo.user.modules.exporttemplate.service.impl;

import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.user.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.user.modules.exporttemplate.vo.ExportTemplateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExportTemplateServiceImpl 纯单测：验证 accessible 标记按 plan template_access 权益下发。
 */
class ExportTemplateServiceImplTest {

    private ExportTemplateMapper exportTemplateMapper;
    private BenefitService benefitService;
    private ExportTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        exportTemplateMapper = mock(ExportTemplateMapper.class);
        benefitService = mock(BenefitService.class);
        service = new ExportTemplateServiceImpl(exportTemplateMapper, benefitService);
    }

    @Test
    void listEnabled_noUser_returnsAllLocked() {
        when(exportTemplateMapper.selectList(any())).thenReturn(rows(
                template("wechat", "basic"),
                template("magazine", "pro"),
                template("xiaohongshu-emotion", "flagship")
        ));

        List<ExportTemplateVO> list = service.listEnabled(null);

        assertEquals(3, list.size());
        assertFalse(list.stream().allMatch(ExportTemplateVO::getAccessible));
        verify(benefitService, never()).getPlanBenefitValue(any(), any(), any());
    }

    @Test
    void listEnabled_freeUserWithEmptyValue_locksAll() {
        when(exportTemplateMapper.selectList(any())).thenReturn(rows(
                template("wechat", "basic"),
                template("magazine", "pro")
        ));
        when(benefitService.getPlanBenefitValue(7L, "template_access", "")).thenReturn("");

        List<ExportTemplateVO> list = service.listEnabled(7L);

        assertTrue(list.stream().noneMatch(ExportTemplateVO::getAccessible));
    }

    @Test
    void listEnabled_userWithExplicitKeys_marksEachAccessible() {
        when(exportTemplateMapper.selectList(any())).thenReturn(rows(
                template("wechat", "basic"),
                template("business", "basic"),
                template("magazine", "pro"),
                template("xiaohongshu-emotion", "flagship")
        ));
        when(benefitService.getPlanBenefitValue(7L, "template_access", ""))
                .thenReturn("wechat,business");

        List<ExportTemplateVO> list = service.listEnabled(7L);

        ExportTemplateVO wechat = findByKey(list, "wechat");
        ExportTemplateVO business = findByKey(list, "business");
        ExportTemplateVO magazine = findByKey(list, "magazine");
        ExportTemplateVO flagship = findByKey(list, "xiaohongshu-emotion");

        assertTrue(wechat.getAccessible());
        assertTrue(business.getAccessible());
        assertFalse(magazine.getAccessible());
        assertFalse(flagship.getAccessible());
    }

    @Test
    void listEnabled_valueWithSpaces_trimsAndMatches() {
        when(exportTemplateMapper.selectList(any())).thenReturn(rows(
                template("wechat", "basic"),
                template("magazine", "pro")
        ));
        when(benefitService.getPlanBenefitValue(7L, "template_access", ""))
                .thenReturn(" wechat , magazine ");

        List<ExportTemplateVO> list = service.listEnabled(7L);

        assertTrue(findByKey(list, "wechat").getAccessible());
        assertTrue(findByKey(list, "magazine").getAccessible());
    }

    @Test
    void listEnabled_dropsEmptyEntries() {
        when(exportTemplateMapper.selectList(any())).thenReturn(rows(
                template("wechat", "basic")
        ));
        // 末尾逗号 + 空白：视为只有 wechat
        when(benefitService.getPlanBenefitValue(7L, "template_access", ""))
                .thenReturn("wechat,, ,");

        List<ExportTemplateVO> list = service.listEnabled(7L);

        assertTrue(findByKey(list, "wechat").getAccessible());
        verify(benefitService).getPlanBenefitValue(eq(7L), eq("template_access"), eq(""));
    }

    private ExportTemplateVO findByKey(List<ExportTemplateVO> list, String key) {
        return list.stream()
                .filter(t -> key.equals(t.getTemplateKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 " + key));
    }

    private List<ExportTemplate> rows(ExportTemplate... arr) {
        return List.of(arr);
    }

    private ExportTemplate template(String key, String tier) {
        ExportTemplate t = new ExportTemplate();
        t.setTemplateKey(key);
        t.setName(key);
        t.setPlatform("general");
        t.setTier(tier);
        t.setStatus(1);
        t.setSortOrder(1);
        return t;
    }
}
