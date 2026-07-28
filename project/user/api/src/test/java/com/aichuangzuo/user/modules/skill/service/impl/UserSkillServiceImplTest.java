package com.aichuangzuo.user.modules.skill.service.impl;

import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户风格服务单元测试（不依赖数据库）。
 */
@ExtendWith(MockitoExtension.class)
class UserSkillServiceImplTest {

    @Mock
    private UserSkillMapper userSkillMapper;

    @Mock
    private UserMembershipMapper userMembershipMapper;

    @Mock
    private PlanBenefitMapper planBenefitMapper;

    @InjectMocks
    private UserSkillServiceImpl userSkillService;

    @BeforeEach
    void setUp() {
        SecurityUserContext.setCurrentUserId(10001L);
    }

    @AfterEach
    void tearDown() {
        SecurityUserContext.clear();
    }

    @Test
    void updateSkill_resetsRejectedSkillToPendingAndClearsRejectReason() {
        UserSkill existing = new UserSkill();
        existing.setId(1L);
        existing.setBizNo("S123");
        existing.setUserId(10001L);
        existing.setSkillName("旧名称");
        existing.setPrompt("旧提示词");
        existing.setScope("旧标签");
        existing.setAuditStatus(2);
        existing.setRejectReason("过于宽泛");

        when(userSkillMapper.selectOne(any())).thenReturn(existing);

        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setSkillName("新名称");
        request.setPrompt("新提示词");
        request.setScope("新标签");

        UserSkillVO updated = userSkillService.updateSkill("S123", request);

        assertEquals("新名称", updated.getSkillName());
        assertEquals("新提示词", updated.getPrompt());
        assertEquals("新标签", updated.getScope());
        assertEquals(Integer.valueOf(0), updated.getAuditStatus());

        ArgumentCaptor<UserSkill> captor = ArgumentCaptor.forClass(UserSkill.class);
        verify(userSkillMapper).updateById(captor.capture());
        UserSkill saved = captor.getValue();
        assertEquals(Integer.valueOf(0), saved.getAuditStatus());
        assertNull(saved.getRejectReason());
    }
}
