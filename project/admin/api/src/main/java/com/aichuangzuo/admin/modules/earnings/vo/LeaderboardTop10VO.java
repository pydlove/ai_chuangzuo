package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaderboardTop10VO {
    private Integer rank;
    private Long userId;
    private String nickname;
    private BigDecimal amount;
    private Boolean granted;
}
