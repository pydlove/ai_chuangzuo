package com.aichuangzuo.admin.modules.leaderboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发奖结果 VO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardGrantResultVO {

    /** 本次发放人数。 */
    private int granted;

    /** 已发放跳过人数。 */
    private int skipped;
}
