package com.aichuangzuo.admin.modules.leaderboard.config.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收益排行榜奖励规则配置 VO。
 */
@Data
public class LeaderboardRewardConfigVO {

    /** 可发放奖励的名次上限，如 TOP 3。 */
    private Integer topLimit;

    /** 每名奖励的创作币数量。 */
    private BigDecimal rewardAmount;
}
