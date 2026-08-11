package com.aichuangzuo.admin.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDisplayWinnerAdminVO {

    private Long id;
    private Long campaignId;
    private String campaignName;
    private Long tierId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String prizeName;
    private LocalDateTime winTime;
    private Integer isReal;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
