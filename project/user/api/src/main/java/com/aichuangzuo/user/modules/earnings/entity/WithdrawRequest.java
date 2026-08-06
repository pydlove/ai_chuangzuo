package com.aichuangzuo.user.modules.earnings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户提现申请实体，对应表 u_withdraw_request。
 */
@Getter
@Setter
@TableName("u_withdraw_request")
public class WithdrawRequest {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号。 */
    private String bizNo;

    /** 申请人用户ID。 */
    private Long userId;

    /** 提现创作币数量。 */
    private BigDecimal amount;

    /** 收款账号（支付宝）。 */
    private String account;

    /** 收款人真实姓名。 */
    private String name;

    /** 状态：1-审核中，2-已通过，3-已拒绝。 */
    private Integer status;

    /** 租户ID。 */
    private Long tenantId;

    /** 逻辑删除标记。 */
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
