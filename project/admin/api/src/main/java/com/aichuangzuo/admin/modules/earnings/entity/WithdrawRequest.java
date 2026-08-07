package com.aichuangzuo.admin.modules.earnings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_withdraw_request")
public class WithdrawRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;

    private Long userId;

    private BigDecimal amount;

    private String account;

    private String name;

    private Integer status;

    private LocalDateTime processedAt;

    private Long processedBy;

    private String resultRemark;

    private Long tenantId;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
