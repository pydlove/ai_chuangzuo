package com.aichuangzuo.user.modules.style.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 风格分析请求。
 */
@Data
public class AnalyzeStyleRequest {

    @NotBlank(message = "参考文章正文不能为空")
    @Size(min = 200, max = 3000, message = "参考文章正文长度需为 200-3000 字符")
    private String text;
}
