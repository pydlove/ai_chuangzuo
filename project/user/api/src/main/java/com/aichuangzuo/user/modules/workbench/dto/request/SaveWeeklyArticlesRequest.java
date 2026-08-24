package com.aichuangzuo.user.modules.workbench.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 保存本周文章数据请求。
 */
@Data
public class SaveWeeklyArticlesRequest {

    @NotEmpty(message = "请至少录入一篇文章")
    @Size(max = 50, message = "每周最多录入 50 篇文章")
    @Valid
    private List<WeeklyArticleItemRequest> articles;
}
