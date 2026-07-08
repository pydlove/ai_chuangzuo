package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按月聚合的结算视图。
 */
@Data
public class MonthlySettlementVO {

    private String month;

    private Integer count;

    private BigDecimal total;

    private BigDecimal settled;

    private BigDecimal unsettled;
}
