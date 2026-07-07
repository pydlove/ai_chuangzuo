package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PendingSettlementUserVO {
    private Long userId;
    private String nickname;
    private Integer recordCount;
    private BigDecimal unsettledAmount;
}
