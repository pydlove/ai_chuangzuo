package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembershipGrantRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "套餐不能为空")
    private String planKey;
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
    private String remark;
}
