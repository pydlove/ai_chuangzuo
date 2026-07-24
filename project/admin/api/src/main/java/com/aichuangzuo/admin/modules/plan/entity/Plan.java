package com.aichuangzuo.admin.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐元数据（对应 u_plan，跨端共享）。
 */
@Getter
@Setter
@TableName("u_plan")
public class Plan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planKey;
    private String displayName;
    private Integer sortOrder;
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