package com.aichuangzuo.admin.modules.leaderboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 榜单 TOP10 单项 VO（管理端发奖用）。
 */
@Data
public class LeaderboardTop10VO {

    /** 用户ID。 */
    private Long userId;

    /** 用户昵称。 */
    private String nickname;

    /** 榜单统计金额。 */
    private BigDecimal amount;

    /** 排名。 */
    private Integer rank;

    /** 奖励金额（创作币）。 */
    private BigDecimal rewardAmount;
}
