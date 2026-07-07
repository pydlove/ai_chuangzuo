package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EarningsRecordVO {
    private Long id;
    private String type;
    private String title;
    private BigDecimal amount;
    private Integer status;
    private String settlementMonth;
    private LocalDateTime createdAt;
}
