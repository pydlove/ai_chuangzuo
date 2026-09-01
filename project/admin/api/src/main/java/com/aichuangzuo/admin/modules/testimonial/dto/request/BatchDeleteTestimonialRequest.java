package com.aichuangzuo.admin.modules.testimonial.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除首页评价请求。
 */
@Data
public class BatchDeleteTestimonialRequest {

    @NotEmpty(message = "请选择要删除的评价")
    private List<Long> ids;
}
