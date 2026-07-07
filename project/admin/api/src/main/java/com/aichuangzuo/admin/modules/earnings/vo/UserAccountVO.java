package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserAccountVO {
    private Long userId;
    private String nickname;
    private String email;
    private LocalDateTime registeredAt;
    private BigDecimal totalEarnings;
    private BigDecimal unsettledEarnings;
    private BigDecimal coinBalance;
    private Integer coinRankThisMonth;
    private Integer incomeRankThisMonth;
}
