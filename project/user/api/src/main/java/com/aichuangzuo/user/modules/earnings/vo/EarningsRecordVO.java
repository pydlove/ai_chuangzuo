package com.aichuangzuo.user.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单条收益记录视图。
 */
@Data
public class EarningsRecordVO {

    private Long id;

    private String type;

    private String typeLabel;

    private String title;

    private String description;

    private BigDecimal amount;

    private Integer status;

    private String statusLabel;

    private String settlementMonth;

    private LocalDateTime createdAt;
}
