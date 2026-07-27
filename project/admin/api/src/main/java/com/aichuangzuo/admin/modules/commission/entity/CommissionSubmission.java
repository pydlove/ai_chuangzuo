package com.aichuangzuo.admin.modules.commission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_commission_submission")
public class CommissionSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long submitterId;
    private String articleBizNo;
    private String articleTitle;
    private String articleBody;
    private Integer wordCount;
    private Integer status;
    private BigDecimal rewardCoin;
    private String coinRecordBizNo;
    private LocalDateTime adoptedAt;
    private LocalDateTime withdrawnAt;
    private Long tenantId;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
