package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户账户收益汇总。
 */
@Data
public class AccountSummaryVO {

    private BigDecimal coinBalance;

    private BigDecimal totalEarnings;

    private BigDecimal settledEarnings;

    private BigDecimal unsettledEarnings;
}
