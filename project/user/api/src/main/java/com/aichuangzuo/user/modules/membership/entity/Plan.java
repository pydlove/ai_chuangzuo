package com.aichuangzuo.user.modules.membership.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐元数据实体，对应表 u_plan。
 */
@Getter
@Setter
@TableName("u_plan")
public class Plan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐 key：basic/pro/flagship。 */
    private String planKey;

    /** 套餐显示名。 */
    private String displayName;

    /** 排序号。 */
    private Integer sortOrder;

    /** 是否推荐套餐。 */
    private Integer recommended;

    private BigDecimal priceMonthly;
    private BigDecimal priceQuarter;
    private BigDecimal priceYear;

    private BigDecimal originalMonthly;
    private BigDecimal originalQuarter;
    private BigDecimal originalYear;

    private String articlesMonthly;
    private String articlesQuarter;
    private String articlesYear;

    private BigDecimal savingsYear;

    /** 状态：0-停用，1-启用。 */
    private Integer status;

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