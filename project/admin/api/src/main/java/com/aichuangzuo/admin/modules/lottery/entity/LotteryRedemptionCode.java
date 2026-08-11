package com.aichuangzuo.admin.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_lottery_redemption_code")
public class LotteryRedemptionCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private Long campaignId;
    private Long tierId;
    private Long drawerUserId;
    private String rewardType;
    private String rewardValueJson;
    private String status;
    private Long usedBy;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private Long tenantId;
    private LocalDateTime createdAt;
}
