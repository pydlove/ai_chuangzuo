package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现申请记录视图。
 */
@Data
public class WithdrawRequestVO {

    private String bizNo;

    private BigDecimal amount;

    private String account;

    private String name;

    private String status;

    private LocalDateTime createdAt;
}
