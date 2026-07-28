package com.aichuangzuo.admin.modules.skill.preset.service;

import com.aichuangzuo.admin.modules.skill.entity.UserSkillAggregate;
import com.aichuangzuo.admin.modules.skill.preset.dto.SystemSkillRow;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.CreateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.UpdateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.enums.AdminGlobalSkillErrorCode;
import com.aichuangzuo.admin.modules.skill.preset.mapper.GlobalSkillAggregateMapper;
import com.aichuangzuo.admin.modules.skill.preset.mapper.GlobalSkillMapper;
import com.aichuangzuo.admin.modules.skill.preset.service.impl.GlobalSkillServiceImpl;
import com.aichuangzuo.admin.modules.skill.preset.vo.GlobalSkillVO;
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
class GlobalSkillServiceTest {

    @Mock
    private GlobalSkillMapper globalSkillMapper;

    @Mock
    private GlobalSkillAggregateMapper aggregateMapper;

    @InjectMocks
    private GlobalSkillServiceImpl service;

    // -------- create --------

    @Test
    void create_setsSystemSourceTypeAndApprovedAudit() {
        CreateGlobalSkillRequest req = new CreateGlobalSkillRequest();
        req.setSkillName("测试预设");
        req.setDescription("描述");
        req.setPromptSummary("摘要");
        req.setPrompt("提示词");
        req.setScope("");
        when(globalSkillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        String bizNo = service.create(req);

        org.junit.jupiter.api.Assertions.assertNotNull(bizNo);
        org.junit.jupiter.api.Assertions.assertTrue(bizNo.startsWith("GS"));

        org.mockito.ArgumentCaptor<UserSkillAggregate> captor =
                org.mockito.ArgumentCaptor.forClass(UserSkillAggregate.class);
        verify(globalSkillMapper).insert(captor.capture());
        UserSkillAggregate saved = captor.getValue();
        assertEquals(0L, saved.getUserId());
        assertEquals(3, saved.getSourceType());
        assertEquals(1, saved.getAuditStatus());
        assertEquals(1, saved.getEnableStatus());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(0, saved.getUseCount());
    }

    @Test
    void create_duplicateName_throws() {
        CreateGlobalSkillRequest req = new CreateGlobalSkillRequest();
        req.setSkillName("年度总结");
        req.setPrompt("提示词");
        when(globalSkillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NAME_EXISTS.getCode(), ex.getCode());
        verify(globalSkillMapper, never()).insert((UserSkillAggregate) any());
    }

    // -------- update --------

    @Test
    void update_modifiesFieldsAndNameChangeTriggersDuplicateCheck() {
        UserSkillAggregate existing = newStyle("GS0009", "旧名");
        when(globalSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(globalSkillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        UpdateGlobalSkillRequest req = new UpdateGlobalSkillRequest();
        req.setSkillName("新名");
        req.setDescription("新描述");
        req.setPromptSummary("新摘要");
        req.setPrompt("新提示词");
        req.setScope("新范围");
        req.setEnableStatus(0);

        service.update("GS0009", req);

        assertEquals("新名", existing.getSkillName());
        assertEquals("新描述", existing.getDescription());
        assertEquals("新摘要", existing.getPromptSummary());
        assertEquals("新提示词", existing.getPrompt());
        assertEquals("新范围", existing.getScope());
        assertEquals(0, existing.getEnableStatus());
        verify(globalSkillMapper).updateById((UserSkillAggregate) existing);
    }

    @Test
    void update_sameName_shouldNotThrowDuplicate() {
        UserSkillAggregate existing = newStyle("GS0001", "年度总结");
        when(globalSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateGlobalSkillRequest req = new UpdateGlobalSkillRequest();
        req.setSkillName("年度总结");
        req.setDescription("新描述");
        req.setPromptSummary("新摘要");
        req.setPrompt("新提示词");
        req.setScope("新范围");
        req.setEnableStatus(0);

        service.update("GS0001", req);

        assertEquals("年度总结", existing.getSkillName());
        assertEquals("新描述", existing.getDescription());
        assertEquals(0, existing.getEnableStatus());
        verify(globalSkillMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(globalSkillMapper).updateById((UserSkillAggregate) existing);
    }

    @Test
    void update_invalidEnableStatus_throws() {
        UserSkillAggregate existing = newStyle("GS0009", "测试");
        when(globalSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateGlobalSkillRequest req = new UpdateGlobalSkillRequest();
        req.setSkillName("测试");
        req.setPrompt("p");
        req.setEnableStatus(2);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("GS0009", req));
        assertEquals(AdminGlobalSkillErrorCode.ENABLE_STATUS_INVALID.getCode(), ex.getCode());
        verify(globalSkillMapper, never()).updateById((UserSkillAggregate) any());
    }

    @Test
    void update_notFound_throws() {
        when(globalSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UpdateGlobalSkillRequest req = new UpdateGlobalSkillRequest();
        req.setSkillName("x");
        req.setPrompt("y");
        req.setEnableStatus(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("NOPE", req));
        assertEquals(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- delete --------

    @Test
    void delete_softDeletesBySettingIsDeleted() {
        UserSkillAggregate existing = newStyle("GS0009", "测试");
        when(globalSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.delete("GS0009");

        assertEquals(1, existing.getIsDeleted());
        verify(globalSkillMapper).updateById((UserSkillAggregate) existing);
    }

    @Test
    void delete_notFound_throws() {
        when(globalSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("NOPE"));
        assertEquals(AdminGlobalSkillErrorCode.GLOBAL_SKILL_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- page 翻译 --------

    @Test
    void page_translatesEnableStatusToFrontendString() {
        SystemSkillRow enabledRow = new SystemSkillRow();
        enabledRow.setBizNo("GS0001");
        enabledRow.setSkillName("启用项");
        enabledRow.setEnableStatus(1);
        SystemSkillRow disabledRow = new SystemSkillRow();
        disabledRow.setBizNo("GS0002");
        disabledRow.setSkillName("禁用项");
        disabledRow.setEnableStatus(0);

        org.mockito.Mockito.doReturn(List.of(enabledRow, disabledRow)).when(aggregateMapper)
                .selectGlobalSkillPage(any(), any(), anyLong(), anyLong());
        org.mockito.Mockito.doReturn(2L).when(aggregateMapper)
                .countGlobalSkillPage(any(), any());

        var req = new com.aichuangzuo.admin.modules.skill.preset.dto.request.GlobalSkillPageRequest();
        IPage<GlobalSkillVO> page = service.page(req);

        assertEquals(2L, page.getTotal());
        assertEquals("enabled", page.getRecords().get(0).getStatus());
        assertEquals("disabled", page.getRecords().get(1).getStatus());
        assertEquals("系统", page.getRecords().get(0).getCreatorName());
        assertEquals("GS0001", page.getRecords().get(0).getId());
    }

    private UserSkillAggregate newStyle(String bizNo, String name) {
        UserSkillAggregate s = new UserSkillAggregate();
        s.setId(1L);
        s.setBizNo(bizNo);
        s.setUserId(0L);
        s.setSkillName(name);
        s.setSourceType(3);
        s.setAuditStatus(1);
        s.setEnableStatus(1);
        s.setIsDeleted(0);
        return s;
    }
}