package com.aichuangzuo.admin.modules.modelconfig.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelConfigActiveRequest {

    @NotNull
    private Integer isActive;
}
