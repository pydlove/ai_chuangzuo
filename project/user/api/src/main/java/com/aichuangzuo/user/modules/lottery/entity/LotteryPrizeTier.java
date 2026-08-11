package com.aichuangzuo.user.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_prize_tier")
public class LotteryPrizeTier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long campaignId;
    private String tierKey;
    private String tierName;
    private Integer prizeLevel;
    private BigDecimal probability;
    private Integer maxWinCount;
    private Integer remainingWinCount;
    private Integer displayRemaining;
    private Integer displayRemainingCount;
    private String rewardType;
    private String rewardValueJson;
    private String codePrefix;
    private Integer codeLength;
    private Integer codeValidityDays;
    private Integer sortOrder;
    private Integer status;

    private Long tenantId;
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
