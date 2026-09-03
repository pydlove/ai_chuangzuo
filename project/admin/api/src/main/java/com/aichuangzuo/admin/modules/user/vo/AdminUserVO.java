package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String account;
    private String email;
    private String phone;
    private String nickname;
    private String status;
    private String userType;
    private String avatarUrl;
    private String inviteCode;
    private Integer invitedCount;
    private Long inviterId;
    private String inviterEmail;
    private String inviterNickname;
    private LocalDateTime membershipExpireAt;
    private String membershipPlan;
    private Integer remainingArticleQuota;
    private BigDecimal monthlyCoinEarnings;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
