package com.aichuangzuo.user.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 套餐权益值实体，对应表 u_plan_benefit。
 */
@Getter
@Setter
@TableName("u_plan_benefit")
public class PlanBenefit {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐：basic/pro/flagship。 */
    private String planKey;

    /** 权益编码，关联 u_benefit.code。 */
    private String benefitCode;

    /** 权益值：boolean 存 true/false，quota 存数字，tier 存等级标识。 */
    private String benefitValue;

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
