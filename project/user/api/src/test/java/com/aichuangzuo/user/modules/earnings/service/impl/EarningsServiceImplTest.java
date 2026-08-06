package com.aichuangzuo.user.modules.earnings.service.impl;

import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordVO;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户收益服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class EarningsServiceImplTest {

    @Mock
    private EarningsRecordMapper earningsRecordMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private EarningsServiceImpl earningsService;

    @Test
    void recordEarnings_shouldAcceptCommissionRewardType() {
        Long userId = 1L;
        String type = EarningsType.COMMISSION_REWARD.getCode();
        String sourceType = "commission";
        String sourceId = "commission:100";
        String title = "约稿采纳奖励";
        String description = "测试描述";
        BigDecimal amount = new BigDecimal("30.00");
        String settlementMonth = "2026-08";

        earningsService.recordEarnings(userId, type, sourceType, sourceId, title, description, amount, settlementMonth);

        ArgumentCaptor<EarningsRecord> captor = ArgumentCaptor.forClass(EarningsRecord.class);
        verify(earningsRecordMapper, times(1)).insert(captor.capture());

        EarningsRecord record = captor.getValue();
        assertEquals(userId, record.getUserId());
        assertEquals(type, record.getType());
        assertEquals(sourceType, record.getSourceType());
        assertEquals(sourceId, record.getSourceId());
        assertEquals(title, record.getTitle());
        assertEquals(description, record.getDescription());
        assertEquals(amount, record.getAmount());
        assertEquals(settlementMonth, record.getSettlementMonth());
        assertNotNull(record.getBizNo());
        assertTrue(record.getBizNo().startsWith("ER"));
        assertEquals(18, record.getBizNo().length());

        verify(messageService, times(1)).pushPersonal(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordCoinDiscountEarnings_shouldRecordNegativeAmount() {
        Long userId = 1L;
        String sourceId = "order:100";
        String planKey = "pro";
        String planName = "专业版";
        String cycle = "month";
        BigDecimal coinAmount = new BigDecimal("200");

        earningsService.recordCoinDiscountEarnings(userId, sourceId, planKey, planName, cycle, coinAmount);

        ArgumentCaptor<EarningsRecord> captor = ArgumentCaptor.forClass(EarningsRecord.class);
        verify(earningsRecordMapper, times(1)).insert(captor.capture());

        EarningsRecord record = captor.getValue();
        assertEquals(userId, record.getUserId());
        assertEquals(EarningsType.COIN_DEDUCTION.getCode(), record.getType());
        assertEquals("subscribe_coin_discount", record.getSourceType());
        assertEquals(sourceId, record.getSourceId());
        assertEquals(planKey, record.getPlanKey());
        assertEquals(planName, record.getPlanName());
        assertEquals(cycle, record.getCycle());
        assertEquals("订阅抵扣", record.getTitle());
        assertTrue(record.getDescription().contains("200"));
        assertTrue(record.getDescription().contains("专业版"));
        assertEquals(0, new BigDecimal("-200").compareTo(record.getAmount()));
        assertNotNull(record.getSettlementMonth());
        assertNotNull(record.getBizNo());
        assertTrue(record.getBizNo().startsWith("ER"));
    }

    @Test
    void nextBizNo_shouldGenerateUniqueNumbers() {
        Set<String> numbers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String bizNo = earningsService.nextBizNo();
            assertNotNull(bizNo);
            assertTrue(bizNo.startsWith("ER"));
            assertEquals(18, bizNo.length());
            assertTrue(numbers.add(bizNo), "生成的流水号应唯一");
        }
    }

    @Test
    void listEarnings_shouldMapSkillMarketFlagAndBizNo() {
        EarningsRecord record = new EarningsRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setType(EarningsType.USAGE.getCode());
        record.setSourceType("skill_market");
        record.setBizNo("ER1234567890ABCDEF");
        record.setTitle("测试提示词被使用");
        record.setAmount(new BigDecimal("2.00"));
        record.setSettlementMonth("2026-08");

        Page<EarningsRecord> page = new Page<>(1, 20);
        page.setRecords(List.of(record));
        page.setTotal(1);
        when(earningsRecordMapper.selectPage(any(Page.class), any())).thenReturn(page);

        ListEarningsRequest request = new ListEarningsRequest();
        EarningsRecordPageVO result = earningsService.listEarnings(1L, request);

        assertEquals(1, result.getList().size());
        EarningsRecordVO vo = result.getList().get(0);
        assertEquals("ER1234567890ABCDEF", vo.getBizNo());
        assertTrue(vo.getFromSkillMarket());
    }
}
