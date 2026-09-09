package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillMarketUsageServiceTest {

    @Mock
    private SkillMarketMapper skillMarketMapper;
    @Mock
    private EarningsRecordMapper earningsRecordMapper;
    @Mock
    private EarningsService earningsService;
    @Mock
    private CoinRecordService coinRecordService;

    @InjectMocks
    private SkillMarketUsageService service;

    @Test
    void recordUsage_shouldGrantCoinAndInsertEarnings() {
        SkillMarket skill = new SkillMarket();
        skill.setId(1L);
        skill.setBizNo("SK123");
        skill.setSkillName("爆款标题");
        skill.setPublisherUserId(100L);
        skill.setAuditStatus(1);
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);
        when(earningsService.nextBizNo()).thenReturn("ER123");

        service.recordUsage("SK123", 2L);

        verify(coinRecordService).grant(100L, "skill_market_usage", new BigDecimal("2.00"), "ER123", "提示词使用收益：爆款标题");
        verify(skillMarketMapper).incrementUsageStats(1L, new BigDecimal("2.00"));

        ArgumentCaptor<EarningsRecord> captor = ArgumentCaptor.forClass(EarningsRecord.class);
        verify(earningsRecordMapper).insert(captor.capture());
        EarningsRecord record = captor.getValue();
        assertEquals(100L, record.getUserId());
        assertEquals(EarningsType.USAGE.getCode(), record.getType());
        assertEquals("skill_market", record.getSourceType());
        assertEquals("SK123", record.getSourceId());
        assertEquals(new BigDecimal("2.00"), record.getAmount());
        assertEquals("ER123", record.getBizNo());
    }

    @Test
    void recordUsage_shouldGrantCoinOnEveryUsageOfSameSkill() {
        SkillMarket skill = new SkillMarket();
        skill.setId(1L);
        skill.setBizNo("SK123");
        skill.setSkillName("爆款标题");
        skill.setPublisherUserId(100L);
        skill.setAuditStatus(1);
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);
        when(earningsService.nextBizNo()).thenReturn("ER1", "ER2");

        service.recordUsage("SK123", 2L);
        service.recordUsage("SK123", 3L);

        verify(coinRecordService).grant(100L, "skill_market_usage", new BigDecimal("2.00"), "ER1", "提示词使用收益：爆款标题");
        verify(coinRecordService).grant(100L, "skill_market_usage", new BigDecimal("2.00"), "ER2", "提示词使用收益：爆款标题");
        verify(earningsRecordMapper, org.mockito.Mockito.times(2)).insert(any(EarningsRecord.class));
    }

    @Test
    void recordUsage_shouldNoopWhenSkillNotFound() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.recordUsage("SK999", 2L);

        verify(coinRecordService, never()).grant(any(), any(), any(), any(), any());
        verify(earningsRecordMapper, never()).insert(any(EarningsRecord.class));
        verify(skillMarketMapper, never()).incrementUsageStats(any(), any());
    }

    @Test
    void recordUsage_shouldNoopWhenSkillNotAudited() {
        SkillMarket skill = new SkillMarket();
        skill.setId(1L);
        skill.setBizNo("SK123");
        skill.setAuditStatus(0);
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        service.recordUsage("SK123", 2L);

        verify(coinRecordService, never()).grant(any(), any(), any(), any(), any());
        verify(earningsRecordMapper, never()).insert(any(EarningsRecord.class));
        verify(skillMarketMapper, never()).incrementUsageStats(any(), any());
    }
}
