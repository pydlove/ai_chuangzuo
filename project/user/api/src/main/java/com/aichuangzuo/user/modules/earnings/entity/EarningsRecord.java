package com.aichuangzuo.user.modules.earnings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户收益记录，对应表 u_earnings_record。
 */
@Getter
@Setter
@TableName("u_earnings_record")
public class EarningsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private String sourceType;

    private String sourceId;

    private String planKey;

    private String planName;

    private String cycle;

    private BigDecimal orderAmount;

    private BigDecimal commissionRate;

    private Integer isFirstPurchase;

    private String title;

    private String description;

    private BigDecimal amount;

    private Integer status;

    private String settlementMonth;

    private LocalDateTime settledAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
