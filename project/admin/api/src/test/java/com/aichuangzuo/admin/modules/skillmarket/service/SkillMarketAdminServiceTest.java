package com.aichuangzuo.admin.modules.skill.market.service;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.skill.market.dto.request.CreateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SkillMarketPageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.UpdateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.admin.modules.skill.market.enums.AdminSkillMarketErrorCode;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketAggregateMapper;
import com.aichuangzuo.admin.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.admin.modules.skill.market.service.impl.SkillMarketAdminServiceImpl;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO;
import com.aichuangzuo.admin.modules.user.entity.PlatformUser;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 风格市场管理服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SkillMarketAdminServiceTest {

    @Mock
    private SkillMarketMapper skillMarketMapper;

    @Mock
    private SkillMarketAggregateMapper aggregateMapper;

    @Mock
    private PlatformUserMapper platformUserMapper;

    @InjectMocks
    private SkillMarketAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityAdminContext.setCurrentAdminUserId(9001L);
    }

    @AfterEach
    void tearDown() {
        SecurityAdminContext.clear();
    }

    // -------- create --------

    @Test
    void create_setsDefaultPriceAndPlatformSourceTypeAndApprovedAudit() {
        CreateSkillMarketRequest req = new CreateSkillMarketRequest();
        req.setSkillName("爆款情感文");
        req.setDescription("高共鸣情感文风格");
        req.setPromptSummary("语气：共情");
        req.setPrompt("你是一位...");
        req.setScope("公众号");
        req.setPublisherUserId(100L);
        req.setTotalUses(50);
        req.setEnableStatus(1);
        req.setFeatured(0);

        when(platformUserMapper.selectById(100L)).thenReturn(platformUser(100L));
        when(skillMarketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        String bizNo = service.create(req);

        assertNotNull(bizNo);
        assertTrue(bizNo.startsWith("SM"));

        org.mockito.ArgumentCaptor<SkillMarket> captor =
                org.mockito.ArgumentCaptor.forClass(SkillMarket.class);
        verify(skillMarketMapper).insert(captor.capture());
        SkillMarket saved = captor.getValue();
        assertEquals("爆款情感文", saved.getSkillName());
        assertEquals(100L, saved.getPublisherUserId());
        assertEquals(new java.math.BigDecimal("0.20"), saved.getPrice());
        assertEquals(3, saved.getSourceType());
        assertEquals(1, saved.getAuditStatus());
        assertEquals(1, saved.getEnableStatus());
        assertEquals(50, saved.getTotalUses());
        assertEquals(0, saved.getIsDeleted());
        assertEquals(9001L, saved.getCreatedBy());
        assertEquals(9001L, saved.getUpdatedBy());
    }

    @Test
    void create_publisherNotFound_throws() {
        CreateSkillMarketRequest req = new CreateSkillMarketRequest();
        req.setSkillName("x");
        req.setPrompt("p");
        req.setPublisherUserId(999L);
        req.setEnableStatus(1);
        req.setFeatured(0);
        when(platformUserMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminSkillMarketErrorCode.PUBLISHER_NOT_FOUND.getCode(), ex.getCode());
        verify(skillMarketMapper, never()).insert(any(SkillMarket.class));
    }

    @Test
    void create_duplicateName_throws() {
        CreateSkillMarketRequest req = new CreateSkillMarketRequest();
        req.setSkillName("x");
        req.setPrompt("p");
        req.setPublisherUserId(100L);
        req.setEnableStatus(1);
        req.setFeatured(0);
        when(platformUserMapper.selectById(100L)).thenReturn(platformUser(100L));
        when(skillMarketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminSkillMarketErrorCode.SKILL_MARKET_NAME_EXISTS.getCode(), ex.getCode());
        verify(skillMarketMapper, never()).insert(any(SkillMarket.class));
    }

    // -------- update --------

    @Test
    void update_modifiesFieldsAndTotalUses() {
        SkillMarket existing = newSkillMarket("SM0009", "旧名");
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(platformUserMapper.selectById(200L)).thenReturn(platformUser(200L));
        when(skillMarketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        UpdateSkillMarketRequest req = new UpdateSkillMarketRequest();
        req.setSkillName("新名");
        req.setDescription("新描述");
        req.setPromptSummary("新摘要");
        req.setPrompt("新提示词");
        req.setScope("新范围");
        req.setPublisherUserId(200L);
        req.setTotalUses(999);
        req.setEnableStatus(0);
        req.setFeatured(0);

        service.update("SM0009", req);

        assertEquals("新名", existing.getSkillName());
        assertEquals("新描述", existing.getDescription());
        assertEquals("新摘要", existing.getPromptSummary());
        assertEquals("新提示词", existing.getPrompt());
        assertEquals("新范围", existing.getScope());
        assertEquals(200L, existing.getPublisherUserId());
        assertEquals(999, existing.getTotalUses());
        assertEquals(0, existing.getEnableStatus());
        assertEquals(9001L, existing.getUpdatedBy());
        verify(skillMarketMapper).updateById(existing);
    }

    @Test
    void update_invalidEnableStatus_throws() {
        SkillMarket existing = newSkillMarket("SM0009", "测试");
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateSkillMarketRequest req = new UpdateSkillMarketRequest();
        req.setSkillName("测试");
        req.setPrompt("p");
        req.setPublisherUserId(100L);
        req.setEnableStatus(2);
        req.setFeatured(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("SM0009", req));
        assertEquals(AdminSkillMarketErrorCode.ENABLE_STATUS_INVALID.getCode(), ex.getCode());
        verify(skillMarketMapper, never()).updateById(any(SkillMarket.class));
    }

    @Test
    void update_negativeTotalUses_throws() {
        SkillMarket existing = newSkillMarket("SM0009", "测试");
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateSkillMarketRequest req = new UpdateSkillMarketRequest();
        req.setSkillName("测试");
        req.setPrompt("p");
        req.setPublisherUserId(100L);
        req.setTotalUses(-1);
        req.setEnableStatus(1);
        req.setFeatured(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("SM0009", req));
        assertEquals(AdminSkillMarketErrorCode.TOTAL_USES_INVALID.getCode(), ex.getCode());
    }

    @Test
    void update_notFound_throws() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UpdateSkillMarketRequest req = new UpdateSkillMarketRequest();
        req.setSkillName("x");
        req.setPrompt("y");
        req.setPublisherUserId(100L);
        req.setEnableStatus(1);
        req.setFeatured(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("NOPE", req));
        assertEquals(AdminSkillMarketErrorCode.SKILL_MARKET_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- delete --------

    @Test
    void delete_softDeletesBySettingIsDeleted() {
        SkillMarket existing = newSkillMarket("SM0009", "测试");
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.delete("SM0009");

        assertEquals(1, existing.getIsDeleted());
        assertEquals(9001L, existing.getUpdatedBy());
        verify(skillMarketMapper).updateById(existing);
    }

    @Test
    void delete_notFound_throws() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("NOPE"));
        assertEquals(AdminSkillMarketErrorCode.SKILL_MARKET_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- page --------

    @Test
    void page_translatesEnableStatusToFrontendString() {
        var enabledRow = new com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketRow();
        enabledRow.setBizNo("SM0001");
        enabledRow.setSkillName("启用项");
        enabledRow.setEnableStatus(1);
        enabledRow.setPublisherUserId(1L);
        enabledRow.setPublisherName("用户A");
        enabledRow.setPrice(new java.math.BigDecimal("0.20"));
        enabledRow.setTotalUses(10);
        var disabledRow = new com.aichuangzuo.admin.modules.skill.market.dto.SkillMarketRow();
        disabledRow.setBizNo("SM0002");
        disabledRow.setSkillName("禁用项");
        disabledRow.setEnableStatus(0);
        disabledRow.setPublisherUserId(2L);
        disabledRow.setPublisherName("用户B");
        disabledRow.setPrice(new java.math.BigDecimal("0.20"));
        disabledRow.setTotalUses(20);

        when(aggregateMapper.selectMarketStylePage(any(), any(), anyLong(), anyLong()))
                .thenReturn(List.of(enabledRow, disabledRow));
        when(aggregateMapper.countMarketStylePage(any(), any())).thenReturn(2L);

        SkillMarketPageRequest req = new SkillMarketPageRequest();
        IPage<SkillMarketVO> page = service.page(req);

        assertEquals(2L, page.getTotal());
        assertEquals("enabled", page.getRecords().get(0).getStatus());
        assertEquals("disabled", page.getRecords().get(1).getStatus());
        assertEquals("用户A", page.getRecords().get(0).getPublisherName());
        assertEquals("SM0001", page.getRecords().get(0).getId());
    }

    private SkillMarket newSkillMarket(String bizNo, String name) {
        SkillMarket s = new SkillMarket();
        s.setId(1L);
        s.setBizNo(bizNo);
        s.setSkillName(name);
        s.setPublisherUserId(100L);
        s.setPrice(new java.math.BigDecimal("0.20"));
        s.setSourceType(3);
        s.setAuditStatus(1);
        s.setEnableStatus(1);
        s.setIsDeleted(0);
        s.setTotalUses(0);
        return s;
    }

    private PlatformUser platformUser(Long id) {
        PlatformUser u = new PlatformUser();
        u.setId(id);
        u.setNickname("用户" + id);
        u.setEmail("user" + id + "@example.com");
        u.setIsDeleted(0);
        return u;
    }
}
