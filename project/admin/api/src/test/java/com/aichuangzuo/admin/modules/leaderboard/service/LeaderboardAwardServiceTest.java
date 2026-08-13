package com.aichuangzuo.admin.modules.leaderboard.service;

import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import com.aichuangzuo.admin.modules.leaderboard.config.entity.LeaderboardRewardConfig;
import com.aichuangzuo.admin.modules.leaderboard.config.service.LeaderboardRewardConfigService;
import com.aichuangzuo.admin.modules.leaderboard.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.admin.modules.leaderboard.mapper.RewardRecordMapper;
import com.aichuangzuo.admin.modules.leaderboard.service.impl.LeaderboardAwardServiceImpl;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.leaderboard.entity.RewardRecord;
import com.aichuangzuo.admin.modules.leaderboard.vo.RewardRecordAdminVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Mock
    private LeaderboardRewardConfigService rewardConfigService;

    @InjectMocks
    private LeaderboardAwardServiceImpl awardService;

    private LeaderboardRewardConfig defaultConfig() {
        LeaderboardRewardConfig config = new LeaderboardRewardConfig();
        config.setId(1L);
        config.setRewardTopLimit(10);
        config.setRewardAmount(new BigDecimal("100.0000"));
        return config;
    }

    @Test
    void grant_shouldSkipAlreadyAwardedUsers() {
        when(rewardConfigService.getEffectiveConfig()).thenReturn(defaultConfig());
        when(aggregateMapper.selectCoinRankingMonth(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(mockTop10());
        when(rewardRecordMapper.exists(anyInt(), anyString(), anyLong())).thenReturn(true);

        LeaderboardGrantResultVO result = awardService.grant(1, "2026-06", 1L);

        assertEquals(0, result.getGranted());
        assertEquals(2, result.getSkipped());
    }

    @Test
    void grant_shouldAwardTopUsersWhenNoneAwarded() {
        when(rewardConfigService.getEffectiveConfig()).thenReturn(defaultConfig());
        when(aggregateMapper.selectIncomeRankingMonth(anyString(), anyInt()))
                .thenReturn(mockTop10());
        when(rewardRecordMapper.exists(anyInt(), anyString(), anyLong())).thenReturn(false);
        when(userApiClient.grantCoin(anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn("UC123");

        LeaderboardGrantResultVO result = awardService.grant(2, "2026-06", 1L);

        assertEquals(2, result.getGranted());
        assertEquals(0, result.getSkipped());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rewardHistory_shouldReturnPageResultWithItems() {
        when(rewardRecordMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<RewardRecord> page = invocation.getArgument(0);
            RewardRecord record = new RewardRecord();
            record.setId(1L);
            record.setLeaderboardType(1);
            record.setPeriodMonth("2026-08");
            record.setRankNo(1);
            record.setUserId(100L);
            record.setAmount(new BigDecimal("100.00"));
            record.setGrantedAt(LocalDateTime.now());
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });

        PageResult<RewardRecordAdminVO> result = awardService.rewardHistory(1, "2026-08", 1, 20);

        assertEquals(1, result.total());
        assertEquals(1, result.items().size());
        assertEquals(100L, result.items().get(0).getUserId());
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
