package com.aichuangzuo.admin.modules.earnings.service;

import com.aichuangzuo.admin.modules.earnings.client.UserCoinRecordClient;
import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardGrantRequest;
import com.aichuangzuo.admin.modules.earnings.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.admin.modules.earnings.mapper.RewardRecordMapper;
import com.aichuangzuo.admin.modules.earnings.service.impl.LeaderboardAwardServiceImpl;
import com.aichuangzuo.admin.modules.earnings.entity.RewardRecord;
import com.aichuangzuo.admin.modules.earnings.vo.LeaderboardTop10VO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardAwardServiceTest {

    @Mock
    private RewardRecordMapper rewardRecordMapper;

    @Mock
    private LeaderboardAggregateMapper leaderboardAggregateMapper;

    @Mock
    private UserCoinRecordClient userCoinRecordClient;

    @InjectMocks
    private LeaderboardAwardServiceImpl leaderboardAwardService;

    @Test
    void grant_skipsAlreadyGranted() {
        LeaderboardTop10VO vo = new LeaderboardTop10VO();
        vo.setRank(1);
        vo.setUserId(1L);
        vo.setAmount(new BigDecimal("100"));
        vo.setGranted(true);

        RewardRecord grantedRecord = new RewardRecord();
        grantedRecord.setUserId(1L);

        when(leaderboardAggregateMapper.selectCoinTop10("2026-06")).thenReturn(List.of(vo));
        when(rewardRecordMapper.selectList(any())).thenReturn(List.of(grantedRecord));

        LeaderboardGrantRequest request = new LeaderboardGrantRequest();
        request.setLeaderboardType(1);
        request.setPeriodMonth("2026-06");

        int granted = leaderboardAwardService.grant(request);
        assertEquals(0, granted);
        verify(userCoinRecordClient, never()).internalGrant(any(), any(), any(), any(), any());
    }
}
