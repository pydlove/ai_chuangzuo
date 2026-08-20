package com.aichuangzuo.user.modules.recommendedcreation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateAnglesRequest {

    @NotBlank(message = "选题ID不能为空")
    private String topicId;
}
