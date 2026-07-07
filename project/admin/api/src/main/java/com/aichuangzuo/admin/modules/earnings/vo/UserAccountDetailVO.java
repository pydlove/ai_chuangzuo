package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserAccountDetailVO {
    private Long userId;
    private String nickname;
    private String email;
    private LocalDateTime registeredAt;
    private BigDecimal totalEarnings;
    private BigDecimal settledEarnings;
    private BigDecimal unsettledEarnings;
    private BigDecimal coinBalance;
    private BigDecimal totalCoinIncome;
    private BigDecimal totalCoinExpense;
    private Integer rewardCount;
    private List<EarningsRecordVO> recentEarnings;
    private List<UserCoinRecordVO> recentCoinRecords;
    private List<RewardRecordVO> recentRewards;
}
