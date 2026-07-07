package com.aichuangzuo.user.modules.leaderboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 自媒体收入榜 VO。
 */
@Data
public class IncomeLeaderboardVO {

    /** 周期类型：month / year。 */
    private String periodType;

    /** 周期值：YYYY-MM 或 YYYY。 */
    private String periodValue;

    /** TOP 20 列表。 */
    private List<LeaderboardEntryVO> topList;

    /** 当前用户排行信息。 */
    private LeaderboardEntryVO me;
}
