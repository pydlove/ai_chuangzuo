package com.aichuangzuo.admin.modules.leaderboard.service;

import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.leaderboard.vo.RewardRecordAdminVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

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

    /**
     * 分页查询奖励发放记录。
     */
    IPage<RewardRecordAdminVO> rewardHistory(Integer leaderboardType, String periodMonth, IPage<RewardRecordAdminVO> pageParam);
}
