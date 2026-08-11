package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryRedemptionCodeVO {

    private Long id;
    private String code;
    private String tierName;
    private String rewardType;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
}
