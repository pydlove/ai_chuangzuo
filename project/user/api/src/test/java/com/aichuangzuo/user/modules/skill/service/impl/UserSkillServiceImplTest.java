package com.aichuangzuo.user.modules.skill.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.shared.enums.error.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.shared.enums.error.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Mock
    private BenefitService benefitService;

    @Mock
    private SkillMarketMapper skillMarketMapper;

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

        when(userSkillMapper.selectOne(any())).thenReturn(existing, (UserSkill) null);

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

    @Test
    void publishSkill_firstPublish_consumesQuotaAndCreatesMarketRecord() {
        UserSkill existing = new UserSkill();
        existing.setId(1L);
        existing.setBizNo("S123");
        existing.setUserId(10001L);
        existing.setSkillName("测试风格");
        existing.setPrompt("测试提示词");
        existing.setSourceType(1);

        when(userSkillMapper.selectOne(any())).thenReturn(existing);
        when(skillMarketMapper.selectByBizNoIncludeDeleted(any())).thenReturn(null);

        userSkillService.publishSkill("S123");

        verify(benefitService).consume(10001L, "skill_market_publish");
        verify(userSkillMapper).updateById(existing);
        ArgumentCaptor<SkillMarket> captor = ArgumentCaptor.forClass(SkillMarket.class);
        verify(skillMarketMapper).insert(captor.capture());
        SkillMarket market = captor.getValue();
        assertEquals("S123", market.getBizNo());
        assertEquals("测试风格", market.getSkillName());
        assertEquals(Integer.valueOf(0), market.getEnableStatus());
        assertEquals(Integer.valueOf(0), market.getAuditStatus());
    }

    @Test
    void publishSkill_quotaExhausted_throwsSkillMarketPublishQuotaExceeded() {
        UserSkill existing = new UserSkill();
        existing.setId(1L);
        existing.setBizNo("S123");
        existing.setUserId(10001L);
        existing.setSkillName("测试风格");
        existing.setPrompt("测试提示词");

        when(userSkillMapper.selectOne(any())).thenReturn(existing);
        doThrow(new BusinessException(BenefitErrorCode.QUOTA_EXHAUSTED))
                .when(benefitService).consume(eq(10001L), eq("skill_market_publish"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.publishSkill("S123"));
        assertEquals(SkillErrorCode.SKILL_MARKET_PUBLISH_QUOTA_EXCEEDED.getCode(), ex.getCode());
        verify(userSkillMapper, never()).updateById((UserSkill) any());
        verify(skillMarketMapper, never()).insert((SkillMarket) any());
    }

    @Test
    void incrementUseCount_shouldIncreaseUseCount() {
        Long userId = 1L;
        String skillName = "轻松";
        UserSkill skill = new UserSkill();
        skill.setId(10L);
        skill.setUserId(userId);
        skill.setSkillName(skillName);
        skill.setUseCount(5);

        when(userSkillMapper.selectOne(any())).thenReturn(skill);

        userSkillService.incrementUseCount(userId, skillName);

        ArgumentCaptor<UpdateWrapper<UserSkill>> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(userSkillMapper).update(isNull(), captor.capture());
        String sql = captor.getValue().getSqlSet();
        assertTrue(sql.contains("use_count = use_count + 1"));
    }

    @Test
    void incrementUseCount_shouldNoopWhenSkillNotFound() {
        when(userSkillMapper.selectOne(any())).thenReturn(null);

        userSkillService.incrementUseCount(1L, "不存在");

        verify(userSkillMapper, never()).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void createSkill_duplicateNameGlobally_throwsSkillNameExists() {
        UserSkill existing = new UserSkill();
        existing.setId(1L);
        existing.setUserId(10002L);
        existing.setSkillName("重复名称");
        existing.setSourceType(1);

        when(userSkillMapper.selectOne(any())).thenReturn(existing);

        CreateSkillRequest request = new CreateSkillRequest();
        request.setSkillName("重复名称");
        request.setPrompt("prompt");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.createSkill(request));
        assertEquals(SkillErrorCode.SKILL_NAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void updateSkill_duplicateNameOwnedByAnotherUser_throwsSkillNameExists() {
        UserSkill existing = new UserSkill();
        existing.setId(1L);
        existing.setBizNo("S123");
        existing.setUserId(10001L);
        existing.setSkillName("旧名称");
        existing.setPrompt("旧提示词");
        existing.setScope("旧标签");

        UserSkill duplicate = new UserSkill();
        duplicate.setId(2L);
        duplicate.setUserId(10002L);
        duplicate.setSkillName("新名称");
        duplicate.setSourceType(1);

        when(userSkillMapper.selectOne(any())).thenReturn(existing, duplicate);

        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setSkillName("新名称");
        request.setPrompt("新提示词");
        request.setScope("新标签");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.updateSkill("S123", request));
        assertEquals(SkillErrorCode.SKILL_NAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void incrementUseCount_shouldNoopWhenSkillNameBlank() {
        userSkillService.incrementUseCount(1L, "");
        verify(userSkillMapper, never()).selectOne(any());
    }
}
