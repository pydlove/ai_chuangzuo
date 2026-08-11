package com.aichuangzuo.admin.modules.lottery.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryRedemptionCodeQueryRequest {

    private Long campaignId;
    private Long tierId;
    private String status;
    private String userKeyword;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long page = 1L;
    private Long size = 20L;
}
