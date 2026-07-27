package com.aichuangzuo.admin.modules.commission.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSubmissionVO {

    private Long id;
    private Long taskId;
    private Long submitterId;
    private String submitterNickname;
    private String submitterEmail;
    private String articleBizNo;
    private String articleTitle;
    private String articleBody;
    private Integer wordCount;
    private Integer status;
    private BigDecimal rewardCoin;
    private LocalDateTime adoptedAt;
    private LocalDateTime createdAt;
}
