package com.aichuangzuo.admin.modules.experience.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExperienceTokenAdminVO {

    private Long id;
    private String batchId;
    private String token;
    private String planKey;
    private Integer membershipDays;
    private Integer status;
    private Long usedByUserId;
    private String usedByUserName;
    private String usedByUserEmail;
    private String usedByUserPhone;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
