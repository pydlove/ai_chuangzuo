package com.aichuangzuo.admin.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryRedemptionCodeAdminVO {

    private Long id;
    private String code;
    private Long campaignId;
    private String campaignName;
    private Long tierId;
    private String tierName;
    private Long drawerUserId;
    private String userDisplay;
    private String rewardType;
    private String rewardValueJson;
    private String rewardContent;
    private String status;
    private Long usedBy;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
