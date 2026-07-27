package com.aichuangzuo.user.modules.commission.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommissionSubmitRequest {
    @NotBlank
    @Size(max = 64)
    private String articleBizNo;
}
