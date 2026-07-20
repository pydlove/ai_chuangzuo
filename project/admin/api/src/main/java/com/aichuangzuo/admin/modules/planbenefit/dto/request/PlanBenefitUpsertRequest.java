package com.aichuangzuo.admin.modules.planbenefit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanBenefitUpsertRequest {

    @NotBlank(message = "套餐 key 不能为空")
    private String planKey;

    @NotBlank(message = "权益编码不能为空")
    private String benefitCode;

    /** boolean 存 true/false；quota 存整数字符串；tier 存等级标识。 */
    @NotBlank(message = "权益值不能为空")
    private String benefitValue;
}