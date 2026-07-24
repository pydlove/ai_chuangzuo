package com.aichuangzuo.admin.modules.style.review.service;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.message.entity.MessageAggregate;
import com.aichuangzuo.admin.modules.message.mapper.MessageAggregateMapper;
import com.aichuangzuo.admin.modules.style.entity.UserStyleAggregate;
import com.aichuangzuo.admin.modules.style.market.entity.StyleMarket;
import com.aichuangzuo.admin.modules.style.market.mapper.StyleMarketMapper;
import com.aichuangzuo.admin.modules.style.review.dto.StyleReviewRow;
import com.aichuangzuo.admin.modules.style.review.entity.AuditStatus;
import com.aichuangzuo.admin.modules.style.review.enums.AdminStyleReviewErrorCode;
import com.aichuangzuo.admin.modules.style.review.mapper.StyleReviewAggregateMapper;
import com.aichuangzuo.admin.modules.style.review.mapper.StyleReviewMapper;
import com.aichuangzuo.admin.modules.style.review.service.impl.StyleReviewServiceImpl;
import com.aichuangzuo.admin.modules.style.review.vo.StyleReviewVO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 风格审核服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StyleReviewServiceTest {

    @Mock
    private StyleReviewMapper styleReviewMapper;

    @Mock
    private StyleReviewAggregateMapper aggregateMapper;

    @Mock
    private StyleMarketMapper styleMarketMapper;

    @Mock
    private MessageAggregateMapper messageAggregateMapper;

    @InjectMocks
    private StyleReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityAdminContext.setCurrentAdminUserId(9001L);
    }

    @AfterEach
    void tearDown() {
        SecurityAdminContext.clear();
    }

    // -------- approve --------

    @Test
    void approve_pending_setsApprovedAndCreatesMarketRecord() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.PENDING.getCode(), "old reason");
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.approve("S1");

        assertEquals(AuditStatus.APPROVED.getCode(), s.getAuditStatus());
        assertEquals(9001L, s.getAuditedBy());
        assertNotNull(s.getAuditedAt());
        assertNull(s.getRejectReason());
        verify(styleReviewMapper).updateById((UserStyleAggregate) s);

        org.mockito.ArgumentCaptor<StyleMarket> marketCaptor = org.mockito.ArgumentCaptor.forClass(StyleMarket.class);
        verify(styleMarketMapper).insert(marketCaptor.capture());
        StyleMarket market = marketCaptor.getValue();
        assertEquals("S1", market.getBizNo());
        assertEquals("测试风格", market.getStyleName());
        assertEquals(Long.valueOf(10001L), market.getPublisherUserId());
        assertEquals(Integer.valueOf(1), market.getAuditStatus());
        assertEquals(Integer.valueOf(1), market.getEnableStatus());
        assertEquals(Integer.valueOf(1), market.getSourceType());
        assertEquals(new java.math.BigDecimal("0.20"), market.getPrice());

        org.mockito.ArgumentCaptor<MessageAggregate> msgCaptor = org.mockito.ArgumentCaptor.forClass(MessageAggregate.class);
        verify(messageAggregateMapper).insert(msgCaptor.capture());
        MessageAggregate message = msgCaptor.getValue();
        assertEquals("style", message.getMsgType());
        assertEquals(Integer.valueOf(2), message.getScope());
        assertEquals(Long.valueOf(10001L), message.getTargetUserId());
        assertEquals("风格审核通过", message.getTitle());
        assertEquals("你的风格「测试风格」已通过审核，已上架风格市场。其他用户使用时，你将获得创作币收益。", message.getSummary());
        assertEquals("approved", message.getSubType());
        assertNull(message.getContent());
    }

    @Test
    void approve_alreadyApproved_throws() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.APPROVED.getCode(), null);
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve("S1"));
        assertEquals(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_APPROVED.getCode(),
                ex.getCode());
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void approve_alreadyRejected_throws() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.REJECTED.getCode(), "old");
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve("S1"));
        assertEquals(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_REJECTED.getCode(),
                ex.getCode());
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void approve_notFound_throws() {
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve("NOPE"));
        assertEquals(AdminStyleReviewErrorCode.STYLE_REVIEW_NOT_FOUND.getCode(),
                ex.getCode());
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void approve_existingMarketRecord_updatesRecord() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.PENDING.getCode(), null);
        StyleMarket existing = new StyleMarket();
        existing.setId(999L);
        existing.setBizNo("S1");
        existing.setAuditStatus(0);
        existing.setEnableStatus(0);
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.approve("S1");

        verify(styleMarketMapper, never()).insert((StyleMarket) any());
        verify(styleMarketMapper).updateById(existing);
        assertEquals(Integer.valueOf(1), existing.getAuditStatus());
        assertEquals(Integer.valueOf(1), existing.getEnableStatus());
        assertEquals("测试风格", existing.getStyleName());

        verify(messageAggregateMapper).insert((MessageAggregate) any());
    }

    @Test
    void batchApprove_mixedStatuses_onlyApprovesPendingAndCreatesMarketRecords() {
        UserStyleAggregate pending1 = newStyle("S1", AuditStatus.PENDING.getCode(), null);
        UserStyleAggregate approved = newStyle("S2", AuditStatus.APPROVED.getCode(), null);
        UserStyleAggregate pending2 = newStyle("S3", AuditStatus.PENDING.getCode(), null);
        UserStyleAggregate rejected = newStyle("S4", AuditStatus.REJECTED.getCode(), "prev");
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(pending1, approved, pending2, rejected);
        when(styleMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        int count = service.batchApprove(List.of("S1", "S2", "S3", "S4"));

        assertEquals(2, count);
        assertEquals(AuditStatus.APPROVED.getCode(), pending1.getAuditStatus());
        assertEquals(AuditStatus.APPROVED.getCode(), pending2.getAuditStatus());
        assertEquals(9001L, pending1.getAuditedBy());
        assertEquals(9001L, pending2.getAuditedBy());
        assertNotNull(pending1.getAuditedAt());
        assertNotNull(pending2.getAuditedAt());
        assertNull(pending1.getRejectReason());
        assertNull(pending2.getRejectReason());

        assertEquals(AuditStatus.APPROVED.getCode(), approved.getAuditStatus());
        assertEquals(AuditStatus.REJECTED.getCode(), rejected.getAuditStatus());

        verify(styleReviewMapper, org.mockito.Mockito.times(2)).updateById((UserStyleAggregate) any());
        verify(styleMarketMapper, org.mockito.Mockito.times(2)).insert((StyleMarket) any());
        verify(messageAggregateMapper, org.mockito.Mockito.times(2)).insert((MessageAggregate) any());
    }

    @Test
    void batchApprove_emptyList_returnsZero() {
        int count = service.batchApprove(List.of());

        assertEquals(0, count);
        verify(styleReviewMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void batchApprove_null_returnsZero() {
        int count = service.batchApprove(null);

        assertEquals(0, count);
        verify(styleReviewMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    // -------- reject --------

    @Test
    void reject_blankReason_throws() {
        assertThrows(BusinessException.class, () -> service.reject("S1", "   "));
        verify(styleReviewMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void reject_nullReason_throws() {
        assertThrows(BusinessException.class, () -> service.reject("S1", null));
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void reject_pending_setsRejectedAndAuditFields() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.PENDING.getCode(), null);
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);

        service.reject("S1", "  太宽泛  ");

        assertEquals(AuditStatus.REJECTED.getCode(), s.getAuditStatus());
        assertEquals(9001L, s.getAuditedBy());
        assertNotNull(s.getAuditedAt());
        assertEquals("太宽泛", s.getRejectReason());
        verify(styleReviewMapper).updateById((UserStyleAggregate) s);

        org.mockito.ArgumentCaptor<MessageAggregate> msgCaptor = org.mockito.ArgumentCaptor.forClass(MessageAggregate.class);
        verify(messageAggregateMapper).insert(msgCaptor.capture());
        MessageAggregate message = msgCaptor.getValue();
        assertEquals("style", message.getMsgType());
        assertEquals(Integer.valueOf(2), message.getScope());
        assertEquals(Long.valueOf(10001L), message.getTargetUserId());
        assertEquals("风格审核未通过", message.getTitle());
        assertEquals("你的风格「测试风格」未通过审核，原因：太宽泛", message.getSummary());
        assertEquals("太宽泛", message.getContent());
        assertEquals("rejected", message.getSubType());
    }

    @Test
    void reject_alreadyRejected_throws() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.REJECTED.getCode(), "prev");
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reject("S1", "again"));
        assertEquals(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_REJECTED.getCode(),
                ex.getCode());
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    @Test
    void reject_alreadyApproved_throws() {
        UserStyleAggregate s = newStyle("S1", AuditStatus.APPROVED.getCode(), null);
        when(styleReviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(s);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.reject("S1", "复核后撤回"));
        assertEquals(AdminStyleReviewErrorCode.STYLE_REVIEW_ALREADY_APPROVED.getCode(),
                ex.getCode());
        verify(styleReviewMapper, never()).updateById((UserStyleAggregate) any());
    }

    // -------- page 翻译 --------

    @Test
    void page_translatesIntFieldsToFrontendStrings() {
        StyleReviewRow row = new StyleReviewRow();
        row.setBizNo("S_X1");
        row.setStyleName("温柔治愈");
        row.setSourceType(1);
        row.setAuditStatus(2);
        row.setCreatorName("张三");
        row.setRejectReason("需要补充");
        org.mockito.Mockito.doReturn(List.of(row)).when(aggregateMapper)
                .selectReviewPage(any(), any(), any(), anyLong(), anyLong());
        org.mockito.Mockito.doReturn(1L).when(aggregateMapper)
                .countReviewPage(any(), any(), any());

        var req = new com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest();
        req.setStatus(2);
        req.setKeyword("温柔");
        IPage<StyleReviewVO> page = service.page(req);

        assertEquals(1L, page.getTotal());
        StyleReviewVO vo = page.getRecords().get(0);
        assertEquals("S_X1", vo.getId());
        assertEquals("温柔治愈", vo.getName());
        assertEquals("my", vo.getSourceType());
        assertEquals("rejected", vo.getStatus());
        assertEquals("张三", vo.getCreatorName());
        assertEquals("需要补充", vo.getRejectReason());
    }

    @Test
    void page_reviewedOnly_queriesApprovedAndRejected() {
        org.mockito.Mockito.doReturn(List.of()).when(aggregateMapper)
                .selectReviewPage(org.mockito.ArgumentMatchers.eq(null),
                        org.mockito.ArgumentMatchers.eq(true),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
        org.mockito.Mockito.doReturn(0L).when(aggregateMapper)
                .countReviewPage(org.mockito.ArgumentMatchers.eq(null),
                        org.mockito.ArgumentMatchers.eq(true),
                        org.mockito.ArgumentMatchers.any());

        var req = new com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest();
        req.setReviewed(true);
        service.page(req);

        verify(aggregateMapper).selectReviewPage(null, true, null, 0L, 20L);
        verify(aggregateMapper).countReviewPage(null, true, null);
    }

    @Test
    void page_learnedSourceTypeMapsCorrectly() {
        StyleReviewRow row = new StyleReviewRow();
        row.setBizNo("S_L1");
        row.setSourceType(2);
        row.setAuditStatus(0);
        org.mockito.Mockito.doReturn(List.of(row)).when(aggregateMapper)
                .selectReviewPage(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
        org.mockito.Mockito.doReturn(1L).when(aggregateMapper)
                .countReviewPage(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());

        IPage<StyleReviewVO> page = service.page(new com.aichuangzuo.admin.modules.style.review.dto.request.StyleReviewPageRequest());
        StyleReviewVO vo = page.getRecords().get(0);
        assertEquals("learned", vo.getSourceType());
        assertEquals("pending", vo.getStatus());
    }

    private UserStyleAggregate newStyle(String bizNo, int status, String reason) {
        UserStyleAggregate s = new UserStyleAggregate();
        s.setId(1L);
        s.setBizNo(bizNo);
        s.setUserId(10001L);
        s.setStyleName("测试风格");
        s.setSourceType(1);
        s.setAuditStatus(status);
        s.setRejectReason(reason);
        s.setIsDeleted(0);
        return s;
    }
}