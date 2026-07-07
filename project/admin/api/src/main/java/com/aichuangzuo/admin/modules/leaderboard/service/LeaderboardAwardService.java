package com.aichuangzuo.admin.modules.leaderboard.service;

import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;

import java.util.List;

/**
 * 收益排行榜发奖服务。
 */
public interface LeaderboardAwardService {

    /**
     * 预览某月某榜单 TOP10。
     */
    List<LeaderboardTop10VO> previewTop10(Integer leaderboardType, String periodMonth);

    /**
     * 发放某月某榜单 TOP10 奖励。
     */
    LeaderboardGrantResultVO grant(Integer leaderboardType, String periodMonth, Long adminUserId);
}
