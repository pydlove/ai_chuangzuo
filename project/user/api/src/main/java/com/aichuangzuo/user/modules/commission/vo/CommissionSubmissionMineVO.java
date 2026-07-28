package com.aichuangzuo.user.modules.commission.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的投稿列表 VO。
 */
@Data
public class CommissionSubmissionMineVO {

    private Long id;

    private Long taskId;

    /** 任务标题 */
    private String title;

    private String articleTitle;

    private String articleBizNo;

    private Integer wordCount;

    private Integer status;

    private BigDecimal rewardCoin;

    private LocalDateTime createdAt;
}
