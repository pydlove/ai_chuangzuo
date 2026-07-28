package com.aichuangzuo.user.modules.commission.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 约稿任务列表项 VO。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommissionTaskVO {

    private Long id;

    private String taskNo;

    private String title;

    private String description;

    private Integer minWordCount;

    private Integer maxWordCount;

    private String skillHint;

    private BigDecimal rewardCoin;

    private Integer neededCount;

    private Integer adoptedCount;

    private Integer status;

    private LocalDateTime deadlineAt;

    private LocalDateTime selectionDeadlineAt;

    private LocalDateTime createdAt;

    /** 当前有效投稿人数（不含已撤回）。 */
    private Long submissionCount;
}
