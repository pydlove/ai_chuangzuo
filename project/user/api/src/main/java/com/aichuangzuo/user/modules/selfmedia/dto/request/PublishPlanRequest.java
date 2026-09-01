package com.aichuangzuo.user.modules.selfmedia.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublishPlanRequest {

    @NotBlank(message = "主发平台不能为空")
    private String mainPlatform;
}
