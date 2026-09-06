package com.aichuangzuo.admin.modules.testimonial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserReviewStatusRequest {

    @NotNull(message = "展示状态不能为空")
    private Integer isShowOnHomepage;
}
