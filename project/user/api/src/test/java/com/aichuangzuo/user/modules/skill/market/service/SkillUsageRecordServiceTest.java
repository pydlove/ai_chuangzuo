package com.aichuangzuo.user.modules.skill.market.service;

import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillUsageRecordServiceTest {

    @Mock
    private SkillMarketMapper skillMarketMapper;
    @Mock
    private UserSkillMapper userSkillMapper;
    @Mock
    private SkillMarketUsageService skillMarketUsageService;
    @Mock
    private UserSkillService userSkillService;

    @InjectMocks
    private SkillUsageRecordService service;

    @Test
    void record_shouldCountMarketSkillAndEarnings() {
        SkillMarket market = new SkillMarket();
        market.setBizNo("SK123");
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(market);

        service.record("SK123", 1L);

        verify(skillMarketUsageService).recordUsage("SK123", 1L);
        verify(userSkillMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void record_shouldCountUserSkillOnly() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        UserSkill skill = new UserSkill();
        skill.setSkillName("轻松");
        skill.setSourceType(1);
        when(userSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        service.record("轻松", 1L);

        verify(skillMarketUsageService, never()).recordUsage(any(), any());
        verify(userSkillService).incrementUseCount(1L, "轻松");
    }

    @Test
    void record_shouldNoopForSystemPreset() {
        when(skillMarketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        UserSkill skill = new UserSkill();
        skill.setSkillName("正式");
        skill.setSourceType(3);
        when(userSkillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(skill);

        service.record("正式", 1L);

        verify(skillMarketUsageService, never()).recordUsage(any(), any());
        verify(userSkillService, never()).incrementUseCount(any(), any());
    }

    @Test
    void record_shouldNoopWhenSkillRefBlank() {
        service.record("", 1L);
        verify(skillMarketMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }
}
