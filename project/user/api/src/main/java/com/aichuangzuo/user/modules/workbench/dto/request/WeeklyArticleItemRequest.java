package com.aichuangzuo.user.modules.workbench.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单篇文章数据项。
 */
@Data
public class WeeklyArticleItemRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 256, message = "文章标题不能超过 256 个字符")
    private String title;

    @NotNull(message = "阅读量不能为空")
    @Min(value = 0, message = "阅读量不能小于 0")
    @Max(value = 999999999, message = "阅读量不能超过 999999999")
    private Integer reads;
}
