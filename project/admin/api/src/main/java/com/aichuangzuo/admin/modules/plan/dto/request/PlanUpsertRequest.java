package com.aichuangzuo.admin.modules.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanUpsertRequest {

    @NotBlank(message = "套餐 key 不能为空")
    private String planKey;

    @NotBlank(message = "套餐显示名不能为空")
    private String displayName;

    /** 套餐英文显示名（如 Plus/Max/Ultra），可空。 */
    private String displayNameEn;

    private Integer sortOrder;

    @NotNull(message = "推荐位不能为空")
    private Integer recommended;

    @NotNull(message = "月度价格不能为空")
    private BigDecimal priceMonthly;

    /** 首月价格（仅月付周期首次购买生效），可空表示不启用。 */
    private BigDecimal firstMonthPrice;

    @NotNull(message = "季度价格不能为空")
    private BigDecimal priceQuarter;

    @NotNull(message = "年度价格不能为空")
    private BigDecimal priceYear;

    private BigDecimal originalMonthly;
    private BigDecimal originalQuarter;
    private BigDecimal originalYear;

    private String articlesMonthly;
    private String articlesQuarter;
    private String articlesYear;

    private BigDecimal savingsYear;

    @NotNull(message = "状态不能为空")
    private Integer status;
}