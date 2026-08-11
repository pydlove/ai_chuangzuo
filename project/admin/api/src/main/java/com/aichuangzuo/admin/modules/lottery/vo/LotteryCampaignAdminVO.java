package com.aichuangzuo.admin.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryCampaignAdminVO {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String rules;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer freeDrawsPerUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
