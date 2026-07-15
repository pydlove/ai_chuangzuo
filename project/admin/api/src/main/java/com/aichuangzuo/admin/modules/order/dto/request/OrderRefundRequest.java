package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderRefundRequest {
    @NotBlank(message = "退款原因不能为空")
    private String reason;
}
