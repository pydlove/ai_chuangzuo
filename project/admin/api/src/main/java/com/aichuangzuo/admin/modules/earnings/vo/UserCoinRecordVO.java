package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCoinRecordVO {
    private Long id;
    private String bizType;
    private Integer direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime bizTime;
}
