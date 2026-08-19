package com.aichuangzuo.admin.modules.security.accesscontrol.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccessControlCreateRequest {

    @NotNull(message = "规则类型不能为空")
    @Min(value = 1, message = "规则类型非法")
    @Max(value = 2, message = "规则类型非法")
    private Integer ruleType;

    @NotNull(message = "名单类型不能为空")
    @Min(value = 1, message = "名单类型非法")
    @Max(value = 2, message = "名单类型非法")
    private Integer listType;

    @NotBlank(message = "规则值不能为空")
    @Size(max = 128, message = "规则值不能超过128字符")
    private String ruleValue;

    @Size(max = 256, message = "备注不能超过256字符")
    private String remark;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    private Integer ruleStatus;
}
