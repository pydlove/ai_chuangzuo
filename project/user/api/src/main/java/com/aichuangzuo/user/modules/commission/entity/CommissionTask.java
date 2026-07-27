package com.aichuangzuo.user.modules.commission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_commission_task")
public class CommissionTask {
    @TableId(type = IdType.AUTO) private Long id;
    private String taskNo;
    private String title;
    private String description;
    private Integer minWordCount;
    private Integer maxWordCount;
    private String styleHint;
    private BigDecimal rewardCoin;
    private Integer neededCount;
    private Integer adoptedCount;
    private Integer status;
    private LocalDateTime deadlineAt;
    private LocalDateTime selectionDeadlineAt;
    private Long publishedBy;
    private LocalDateTime completedAt;
    private Long tenantId;
    @TableLogic private Integer isDeleted;
    private LocalDateTime deletedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT) private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Long updatedBy;
}
