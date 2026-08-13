package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RenewalUserVO {
    private Long userId;
    private String nickname;
    private String email;
    private LocalDateTime firstPaidAt;
    private LocalDateTime lastRenewalAt;
    private Integer renewalCount;
    private BigDecimal renewalAmount;
    private String currentLevel;
    private LocalDate expiresAt;
}
