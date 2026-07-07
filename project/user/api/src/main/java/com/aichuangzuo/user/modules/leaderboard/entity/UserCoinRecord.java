package com.aichuangzuo.user.modules.leaderboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_coin_record")
public class UserCoinRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;
    private Long userId;
    private String bizType;
    private Integer direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String refId;
    private String remark;
    private LocalDateTime bizTime;
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
