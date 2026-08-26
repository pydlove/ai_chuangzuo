package com.aichuangzuo.admin.modules.testimonial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestimonialStatusRequest {

    @NotNull(message = "启用状态不能为空")
    private Integer isEnabled;
}
