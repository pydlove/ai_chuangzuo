package com.aichuangzuo.user.modules.membership.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订单实体，对应表 u_order。
 */
@Getter
@Setter
@TableName("u_order")
public class Order {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号：SUB{yyMMdd}{6位随机}。 */
    private String orderNo;

    /** 下单用户ID。 */
    private Long userId;

    /** 套餐：basic/pro/flagship。 */
    private String planKey;

    /** 周期：month/quarter/year。 */
    private String cycle;

    /** 订单金额。 */
    private BigDecimal amount;

    /** 状态：0-待支付，1-已支付。 */
    private Integer status;

    /** 支付时间。 */
    private LocalDateTime paidAt;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
