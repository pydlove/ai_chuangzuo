package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MembershipGrantRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "套餐不能为空")
    private String planKey;
    @NotBlank(message = "周期不能为空")
    private String cycle;
    private String remark;
}
