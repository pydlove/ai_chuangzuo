package com.aichuangzuo.user.modules.commission.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 约稿中心顶部统计 VO。
 */
@Data
public class CommissionStatsVO {

    /** 进行中（投递中）的任务总数。 */
    private Long activeTaskCount;

    /** 我的投稿总数（含所有状态）。 */
    private Long mySubmissionCount;

    /** 已获得创作币总额（仅已采纳）。 */
    private BigDecimal earnedCoinTotal;
}
