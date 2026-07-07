package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettlementResultVO {
    private String month;
    private int settledUserCount;
    private int settledRecordCount;
    private BigDecimal settledAmount;
}
