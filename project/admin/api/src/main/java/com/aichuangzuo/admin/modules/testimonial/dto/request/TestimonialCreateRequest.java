package com.aichuangzuo.admin.modules.testimonial.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TestimonialCreateRequest {

    @Size(max = 512)
    private String avatarUrl;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64)
    private String name;

    @Size(max = 128)
    private String title;

    @NotNull(message = "星级不能为空")
    @Min(value = 1, message = "星级最小为 1")
    @Max(value = 5, message = "星级最大为 5")
    private Integer starRating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 2048)
    private String reviewText;

    @NotNull
    private Integer sort;

    @NotNull
    private Integer isEnabled;
}
