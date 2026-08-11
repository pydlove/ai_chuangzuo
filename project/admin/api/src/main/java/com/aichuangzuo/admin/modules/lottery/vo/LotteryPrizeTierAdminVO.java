package com.aichuangzuo.admin.modules.lottery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LotteryPrizeTierAdminVO {

    private Long id;
    private Long campaignId;
    private String tierKey;
    private String tierName;
    private BigDecimal probability;
    private Integer maxWinCount;
    private Integer remainingWinCount;
    private String rewardType;
    private String rewardValueJson;
    private String codePrefix;
    private Integer codeLength;
    private Integer codeValidityDays;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
