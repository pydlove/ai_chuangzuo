package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.user.modules.leaderboard.vo.CoinLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeLeaderboardVO;

/**
 * 榜单聚合服务。
 */
public interface LeaderboardService {

    CoinLeaderboardVO getCoinLeaderboard(Long currentUserId, String month);

    IncomeLeaderboardVO getIncomeLeaderboard(Long currentUserId, String periodType, String periodValue);
}
