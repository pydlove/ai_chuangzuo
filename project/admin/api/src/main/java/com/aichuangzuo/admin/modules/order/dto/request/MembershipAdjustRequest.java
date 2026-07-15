package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MembershipAdjustRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "会员等级不能为空")
    private String level;
    @NotNull(message = "到期时间不能为空")
    private LocalDate expiresAt;
    private String remark;
}
