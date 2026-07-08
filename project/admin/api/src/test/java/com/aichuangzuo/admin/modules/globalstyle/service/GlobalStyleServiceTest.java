package com.aichuangzuo.admin.modules.style.preset.service;

import com.aichuangzuo.admin.modules.style.entity.UserStyleAggregate;
import com.aichuangzuo.admin.modules.style.preset.dto.SystemStyleRow;
import com.aichuangzuo.admin.modules.style.preset.dto.request.CreateGlobalStyleRequest;
import com.aichuangzuo.admin.modules.style.preset.dto.request.UpdateGlobalStyleRequest;
import com.aichuangzuo.admin.modules.style.preset.enums.AdminGlobalStyleErrorCode;
import com.aichuangzuo.admin.modules.style.preset.mapper.GlobalStyleAggregateMapper;
import com.aichuangzuo.admin.modules.style.preset.mapper.GlobalStyleMapper;
import com.aichuangzuo.admin.modules.style.preset.service.impl.GlobalStyleServiceImpl;
import com.aichuangzuo.admin.modules.style.preset.vo.GlobalStyleVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预设风格服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalStyleServiceTest {

    @Mock
    private GlobalStyleMapper globalStyleMapper;

    @Mock
    private GlobalStyleAggregateMapper aggregateMapper;

    @InjectMocks
    private GlobalStyleServiceImpl service;

    // -------- create --------

    @Test
    void create_setsSystemSourceTypeAndApprovedAudit() {
        CreateGlobalStyleRequest req = new CreateGlobalStyleRequest();
        req.setStyleName("测试预设");
        req.setDescription("描述");
        req.setPromptSummary("摘要");
        req.setPrompt("提示词");
        req.setScope("");
        when(globalStyleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        String bizNo = service.create(req);

        org.junit.jupiter.api.Assertions.assertNotNull(bizNo);
        org.junit.jupiter.api.Assertions.assertTrue(bizNo.startsWith("GS"));

        org.mockito.ArgumentCaptor<UserStyleAggregate> captor =
                org.mockito.ArgumentCaptor.forClass(UserStyleAggregate.class);
        verify(globalStyleMapper).insert(captor.capture());
        UserStyleAggregate saved = captor.getValue();
        assertEquals(0L, saved.getUserId());
        assertEquals(3, saved.getSourceType());
        assertEquals(1, saved.getAuditStatus());
        assertEquals(1, saved.getEnableStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(0, saved.getUseCount());
    }

    @Test
    void create_duplicateName_throws() {
        CreateGlobalStyleRequest req = new CreateGlobalStyleRequest();
        req.setStyleName("年度总结");
        req.setPrompt("提示词");
        when(globalStyleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NAME_EXISTS.getCode(), ex.getCode());
        verify(globalStyleMapper, never()).insert((UserStyleAggregate) any());
    }

    // -------- update --------

    @Test
    void update_modifiesFieldsAndNameChangeTriggersDuplicateCheck() {
        UserStyleAggregate existing = newStyle("GS0009", "旧名");
        when(globalStyleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(globalStyleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        UpdateGlobalStyleRequest req = new UpdateGlobalStyleRequest();
        req.setStyleName("新名");
        req.setDescription("新描述");
        req.setPromptSummary("新摘要");
        req.setPrompt("新提示词");
        req.setScope("新范围");
        req.setEnableStatus(0);

        service.update("GS0009", req);

        assertEquals("新名", existing.getStyleName());
        assertEquals("新描述", existing.getDescription());
        assertEquals("新摘要", existing.getPromptSummary());
        assertEquals("新提示词", existing.getPrompt());
        assertEquals("新范围", existing.getScope());
        assertEquals(0, existing.getEnableStatus());
        verify(globalStyleMapper).updateById((UserStyleAggregate) existing);
    }

    @Test
    void update_sameName_shouldNotThrowDuplicate() {
        UserStyleAggregate existing = newStyle("GS0001", "年度总结");
        when(globalStyleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateGlobalStyleRequest req = new UpdateGlobalStyleRequest();
        req.setStyleName("年度总结");
        req.setDescription("新描述");
        req.setPromptSummary("新摘要");
        req.setPrompt("新提示词");
        req.setScope("新范围");
        req.setEnableStatus(0);

        service.update("GS0001", req);

        assertEquals("年度总结", existing.getStyleName());
        assertEquals("新描述", existing.getDescription());
        assertEquals(0, existing.getEnableStatus());
        verify(globalStyleMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(globalStyleMapper).updateById((UserStyleAggregate) existing);
    }

    @Test
    void update_invalidEnableStatus_throws() {
        UserStyleAggregate existing = newStyle("GS0009", "测试");
        when(globalStyleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateGlobalStyleRequest req = new UpdateGlobalStyleRequest();
        req.setStyleName("测试");
        req.setPrompt("p");
        req.setEnableStatus(2);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("GS0009", req));
        assertEquals(AdminGlobalStyleErrorCode.ENABLE_STATUS_INVALID.getCode(), ex.getCode());
        verify(globalStyleMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void update_notFound_throws() {
        when(globalStyleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UpdateGlobalStyleRequest req = new UpdateGlobalStyleRequest();
        req.setStyleName("x");
        req.setPrompt("y");
        req.setEnableStatus(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("NOPE", req));
        assertEquals(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- delete --------

    @Test
    void delete_softDeletesBySettingIsDeleted() {
        UserStyleAggregate existing = newStyle("GS0009", "测试");
        when(globalStyleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.delete("GS0009");

        assertEquals(1, existing.getIsDeleted());
        verify(globalStyleMapper).updateById((UserStyleAggregate) existing);
    }

    @Test
    void delete_notFound_throws() {
        when(globalStyleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("NOPE"));
        assertEquals(AdminGlobalStyleErrorCode.GLOBAL_STYLE_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- page 翻译 --------

    @Test
    void page_translatesEnableStatusToFrontendString() {
        SystemStyleRow enabledRow = new SystemStyleRow();
        enabledRow.setBizNo("GS0001");
        enabledRow.setStyleName("启用项");
        enabledRow.setEnableStatus(1);
        SystemStyleRow disabledRow = new SystemStyleRow();
        disabledRow.setBizNo("GS0002");
        disabledRow.setStyleName("禁用项");
        disabledRow.setEnableStatus(0);

        org.mockito.Mockito.doReturn(List.of(enabledRow, disabledRow)).when(aggregateMapper)
                .selectGlobalStylePage(any(), any(), anyLong(), anyLong());
        org.mockito.Mockito.doReturn(2L).when(aggregateMapper)
                .countGlobalStylePage(any(), any());

        var req = new com.aichuangzuo.admin.modules.style.preset.dto.request.GlobalStylePageRequest();
        IPage<GlobalStyleVO> page = service.page(req);

        assertEquals(2L, page.getTotal());
        assertEquals("enabled", page.getRecords().get(0).getStatus());
        assertEquals("disabled", page.getRecords().get(1).getStatus());
        assertEquals("系统", page.getRecords().get(0).getCreatorName());
        assertEquals("GS0001", page.getRecords().get(0).getId());
    }

    private UserStyleAggregate newStyle(String bizNo, String name) {
        UserStyleAggregate s = new UserStyleAggregate();
        s.setId(1L);
        s.setBizNo(bizNo);
        s.setUserId(0L);
        s.setStyleName(name);
        s.setSourceType(3);
        s.setAuditStatus(1);
        s.setEnableStatus(1);
        s.setIsDeleted(0);
        return s;
    }
}