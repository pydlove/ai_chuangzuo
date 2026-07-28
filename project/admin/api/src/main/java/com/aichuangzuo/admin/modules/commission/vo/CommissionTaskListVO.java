package com.aichuangzuo.admin.modules.commission.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTaskListVO {

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
    private LocalDateTime completedAt;
    private Long publishedBy;
    private LocalDateTime createdAt;

    private Long submissionCount;
    private Long manualCount;
}
