package com.aichuangzuo.admin.modules.testimonial.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 首页评价列表查询参数。
 */
@Data
public class TestimonialPageRequest {

    /** 搜索关键词（按姓名或评价内容模糊匹配）。 */
    private String keyword;

    @Min(value = 1, message = "pageNum 必须 ≥ 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize 必须 ≥ 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 20;
}
