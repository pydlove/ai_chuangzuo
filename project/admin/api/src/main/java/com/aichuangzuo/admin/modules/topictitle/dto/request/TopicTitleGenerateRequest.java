package com.aichuangzuo.admin.modules.topictitle.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 生成标题请求。
 */
@Data
public class TopicTitleGenerateRequest {

    /** 生成数量：1-100。 */
    @NotNull
    @Min(1)
    @Max(100)
    private Integer count;

    /** 生成方向提示词（可选，如「职场效率类，面向 25-35 岁打工人」）。 */
    private String direction;
}
