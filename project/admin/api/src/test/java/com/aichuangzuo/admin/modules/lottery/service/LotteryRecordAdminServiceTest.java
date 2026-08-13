package com.aichuangzuo.admin.modules.lottery.service;

import com.aichuangzuo.admin.modules.lottery.entity.LotteryDrawChance;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDrawChanceMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryDrawRecordMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryRedemptionCodeMapper;
import com.aichuangzuo.admin.modules.lottery.service.impl.LotteryRecordAdminServiceImpl;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LotteryRecordAdminServiceTest {

    @Mock
    private LotteryRedemptionCodeMapper redemptionCodeMapper;

    @Mock
    private LotteryDrawRecordMapper drawRecordMapper;

    @Mock
    private LotteryPrizeTierMapper prizeTierMapper;

    @Mock
    private LotteryCampaignMapper campaignMapper;

    @Mock
    private LotteryDrawChanceMapper drawChanceMapper;

    @Mock
    private PlatformUserMapper platformUserMapper;

    @InjectMocks
    private LotteryRecordAdminServiceImpl recordAdminService;

    @Test
    void resetDrawChance_shouldUpdateUsedToAvailable() {
        when(drawChanceMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> recordAdminService.resetDrawChance(1L, 102L));

        verify(drawChanceMapper).update(eq(null), any(UpdateWrapper.class));
        verify(drawChanceMapper, never()).insert(any(LotteryDrawChance.class));
    }

    @Test
    void resetDrawChance_shouldNotThrowWhenNoUsedRecord() {
        when(drawChanceMapper.update(eq(null), any(UpdateWrapper.class))).thenReturn(0);

        assertDoesNotThrow(() -> recordAdminService.resetDrawChance(1L, 102L));

        verify(drawChanceMapper).update(eq(null), any(UpdateWrapper.class));
        verify(drawChanceMapper, never()).insert(any(LotteryDrawChance.class));
    }
}
