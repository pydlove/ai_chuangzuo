package com.aichuangzuo.admin.modules.style.market.service;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.style.market.dto.request.CreateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.StyleMarketPageRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.UpdateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.entity.StyleMarket;
import com.aichuangzuo.admin.modules.style.market.enums.AdminStyleMarketErrorCode;
import com.aichuangzuo.admin.modules.style.market.mapper.StyleMarketAggregateMapper;
import com.aichuangzuo.admin.modules.style.market.mapper.StyleMarketMapper;
import com.aichuangzuo.admin.modules.style.market.service.impl.StyleMarketAdminServiceImpl;
import com.aichuangzuo.admin.modules.style.market.vo.StyleMarketVO;
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
class StyleMarketAdminServiceTest {

    @Mock
    private StyleMarketMapper styleMarketMapper;

    @Mock
    private StyleMarketAggregateMapper aggregateMapper;

    @Mock
    private PlatformUserMapper platformUserMapper;

    @InjectMocks
    private StyleMarketAdminServiceImpl service;

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
        CreateStyleMarketRequest req = new CreateStyleMarketRequest();
        req.setStyleName("爆款情感文");
        req.setDescription("高共鸣情感文风格");
        req.setPromptSummary("语气：共情");
        req.setPrompt("你是一位...");
        req.setScope("公众号");
        req.setPublisherUserId(100L);
        req.setTotalUses(50);
        req.setEnableStatus(1);

        when(platformUserMapper.selectById(100L)).thenReturn(platformUser(100L));
        when(styleMarketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        String bizNo = service.create(req);

        assertNotNull(bizNo);
        assertTrue(bizNo.startsWith("SM"));

        org.mockito.ArgumentCaptor<StyleMarket> captor =
                org.mockito.ArgumentCaptor.forClass(StyleMarket.class);
        verify(styleMarketMapper).insert(captor.capture());
        StyleMarket saved = captor.getValue();
        assertEquals("爆款情感文", saved.getStyleName());
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
        CreateStyleMarketRequest req = new CreateStyleMarketRequest();
        req.setStyleName("x");
        req.setPrompt("p");
        req.setPublisherUserId(999L);
        req.setEnableStatus(1);
        when(platformUserMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminStyleMarketErrorCode.PUBLISHER_NOT_FOUND.getCode(), ex.getCode());
        verify(styleMarketMapper, never()).insert(any(StyleMarket.class));
    }

    @Test
    void create_duplicateName_throws() {
        CreateStyleMarketRequest req = new CreateStyleMarketRequest();
        req.setStyleName("x");
        req.setPrompt("p");
        req.setPublisherUserId(100L);
        req.setEnableStatus(1);
        when(platformUserMapper.selectById(100L)).thenReturn(platformUser(100L));
        when(styleMarketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals(AdminStyleMarketErrorCode.STYLE_MARKET_NAME_EXISTS.getCode(), ex.getCode());
        verify(styleMarketMapper, never()).insert(any(StyleMarket.class));
    }

    // -------- update --------

    @Test
    void update_modifiesFieldsAndTotalUses() {
        StyleMarket existing = newStyleMarket("SM0009", "旧名");
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(platformUserMapper.selectById(200L)).thenReturn(platformUser(200L));
        when(styleMarketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        UpdateStyleMarketRequest req = new UpdateStyleMarketRequest();
        req.setStyleName("新名");
        req.setDescription("新描述");
        req.setPromptSummary("新摘要");
        req.setPrompt("新提示词");
        req.setScope("新范围");
        req.setPublisherUserId(200L);
        req.setTotalUses(999);
        req.setEnableStatus(0);

        service.update("SM0009", req);

        assertEquals("新名", existing.getStyleName());
        assertEquals("新描述", existing.getDescription());
        assertEquals("新摘要", existing.getPromptSummary());
        assertEquals("新提示词", existing.getPrompt());
        assertEquals("新范围", existing.getScope());
        assertEquals(200L, existing.getPublisherUserId());
        assertEquals(999, existing.getTotalUses());
        assertEquals(0, existing.getEnableStatus());
        assertEquals(9001L, existing.getUpdatedBy());
        verify(styleMarketMapper).updateById(existing);
    }

    @Test
    void update_invalidEnableStatus_throws() {
        StyleMarket existing = newStyleMarket("SM0009", "测试");
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateStyleMarketRequest req = new UpdateStyleMarketRequest();
        req.setStyleName("测试");
        req.setPrompt("p");
        req.setPublisherUserId(100L);
        req.setEnableStatus(2);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("SM0009", req));
        assertEquals(AdminStyleMarketErrorCode.ENABLE_STATUS_INVALID.getCode(), ex.getCode());
        verify(styleMarketMapper, never()).updateById(any(StyleMarket.class));
    }

    @Test
    void update_negativeTotalUses_throws() {
        StyleMarket existing = newStyleMarket("SM0009", "测试");
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        UpdateStyleMarketRequest req = new UpdateStyleMarketRequest();
        req.setStyleName("测试");
        req.setPrompt("p");
        req.setPublisherUserId(100L);
        req.setTotalUses(-1);
        req.setEnableStatus(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("SM0009", req));
        assertEquals(AdminStyleMarketErrorCode.TOTAL_USES_INVALID.getCode(), ex.getCode());
    }

    @Test
    void update_notFound_throws() {
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        UpdateStyleMarketRequest req = new UpdateStyleMarketRequest();
        req.setStyleName("x");
        req.setPrompt("y");
        req.setPublisherUserId(100L);
        req.setEnableStatus(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update("NOPE", req));
        assertEquals(AdminStyleMarketErrorCode.STYLE_MARKET_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- delete --------

    @Test
    void delete_softDeletesBySettingIsDeleted() {
        StyleMarket existing = newStyleMarket("SM0009", "测试");
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.delete("SM0009");

        assertEquals(1, existing.getIsDeleted());
        assertEquals(9001L, existing.getUpdatedBy());
        verify(styleMarketMapper).updateById(existing);
    }

    @Test
    void delete_notFound_throws() {
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("NOPE"));
        assertEquals(AdminStyleMarketErrorCode.STYLE_MARKET_NOT_FOUND.getCode(), ex.getCode());
    }

    // -------- page --------

    @Test
    void page_translatesEnableStatusToFrontendString() {
        var enabledRow = new com.aichuangzuo.admin.modules.style.market.dto.StyleMarketRow();
        enabledRow.setBizNo("SM0001");
        enabledRow.setStyleName("启用项");
        enabledRow.setEnableStatus(1);
        enabledRow.setPublisherUserId(1L);
        enabledRow.setPublisherName("用户A");
        enabledRow.setPrice(new java.math.BigDecimal("0.20"));
        enabledRow.setTotalUses(10);
        var disabledRow = new com.aichuangzuo.admin.modules.style.market.dto.StyleMarketRow();
        disabledRow.setBizNo("SM0002");
        disabledRow.setStyleName("禁用项");
        disabledRow.setEnableStatus(0);
        disabledRow.setPublisherUserId(2L);
        disabledRow.setPublisherName("用户B");
        disabledRow.setPrice(new java.math.BigDecimal("0.20"));
        disabledRow.setTotalUses(20);

        when(aggregateMapper.selectMarketStylePage(any(), any(), anyLong(), anyLong()))
                .thenReturn(List.of(enabledRow, disabledRow));
        when(aggregateMapper.countMarketStylePage(any(), any())).thenReturn(2L);

        StyleMarketPageRequest req = new StyleMarketPageRequest();
        IPage<StyleMarketVO> page = service.page(req);

        assertEquals(2L, page.getTotal());
        assertEquals("enabled", page.getRecords().get(0).getStatus());
        assertEquals("disabled", page.getRecords().get(1).getStatus());
        assertEquals("用户A", page.getRecords().get(0).getPublisherName());
        assertEquals("SM0001", page.getRecords().get(0).getId());
    }

    private StyleMarket newStyleMarket(String bizNo, String name) {
        StyleMarket s = new StyleMarket();
        s.setId(1L);
        s.setBizNo(bizNo);
        s.setStyleName(name);
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
