package com.aichuangzuo.user.modules.earnings.service.impl;

import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.message.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

        verify(messageService, times(1)).pushPersonal(any(), any(), any(), any(), any(), any(), any());
    }
}
