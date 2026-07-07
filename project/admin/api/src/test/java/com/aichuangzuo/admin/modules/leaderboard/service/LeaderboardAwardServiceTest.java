package com.aichuangzuo.admin.modules.leaderboard.service;

import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import com.aichuangzuo.admin.modules.leaderboard.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.admin.modules.leaderboard.mapper.RewardRecordMapper;
import com.aichuangzuo.admin.modules.leaderboard.service.impl.LeaderboardAwardServiceImpl;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 发奖服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class LeaderboardAwardServiceTest {

    @Mock
    private LeaderboardAggregateMapper aggregateMapper;

    @Mock
    private RewardRecordMapper rewardRecordMapper;

    @Mock
    private UserApiClient userApiClient;

    @InjectMocks
    private LeaderboardAwardServiceImpl awardService;

    @Test
    void grant_shouldSkipAlreadyAwardedUsers() {
        when(aggregateMapper.selectCoinRankingMonth(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(mockTop10());
        when(rewardRecordMapper.exists(anyInt(), anyString(), anyLong())).thenReturn(true);

        LeaderboardGrantResultVO result = awardService.grant(1, "2026-06", 1L);

        assertEquals(0, result.getGranted());
        assertEquals(2, result.getSkipped());
    }

    @Test
    void grant_shouldAwardTopUsersWhenNoneAwarded() {
        when(aggregateMapper.selectIncomeRankingMonth(anyString(), anyInt()))
                .thenReturn(mockTop10());
        when(rewardRecordMapper.exists(anyInt(), anyString(), anyLong())).thenReturn(false);
        when(userApiClient.grantCoin(anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn("UC123");

        LeaderboardGrantResultVO result = awardService.grant(2, "2026-06", 1L);

        assertEquals(2, result.getGranted());
        assertEquals(0, result.getSkipped());
    }

    private List<LeaderboardTop10VO> mockTop10() {
        LeaderboardTop10VO item1 = new LeaderboardTop10VO();
        item1.setUserId(1L);
        item1.setAmount(new BigDecimal("100.00"));

        LeaderboardTop10VO item2 = new LeaderboardTop10VO();
        item2.setUserId(2L);
        item2.setAmount(new BigDecimal("80.00"));

        return List.of(item1, item2);
    }
}
