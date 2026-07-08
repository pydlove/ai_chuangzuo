package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算上月结果。
 */
@Data
public class SettleLastMonthResultVO {

    private String month;

    private Integer settledCount;

    private BigDecimal settledAmount;
}
