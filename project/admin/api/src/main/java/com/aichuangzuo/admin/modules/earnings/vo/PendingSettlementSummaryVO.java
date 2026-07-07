package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PendingSettlementSummaryVO {
    private String month;
    private long userCount;
    private long recordCount;
    private BigDecimal totalAmount;
    private List<PendingSettlementUserVO> users;
}
