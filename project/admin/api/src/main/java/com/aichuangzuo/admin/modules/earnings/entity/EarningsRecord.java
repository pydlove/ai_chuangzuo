package com.aichuangzuo.admin.modules.earnings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
