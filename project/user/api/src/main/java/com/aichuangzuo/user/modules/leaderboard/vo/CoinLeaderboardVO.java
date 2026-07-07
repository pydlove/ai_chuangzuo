package com.aichuangzuo.user.modules.leaderboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 创作币榜 VO。
 */
@Data
public class CoinLeaderboardVO {

    /** 榜单月份 YYYY-MM。 */
    private String month;

    /** TOP 20 列表。 */
    private List<LeaderboardEntryVO> topList;

    /** 当前用户排行信息（未上榜也有）。 */
    private LeaderboardEntryVO me;
}
