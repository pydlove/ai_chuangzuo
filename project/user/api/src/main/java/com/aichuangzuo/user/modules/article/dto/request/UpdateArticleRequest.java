package com.aichuangzuo.user.modules.article.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改作品请求（编辑保存时调用）。
 */
@Data
public class UpdateArticleRequest {

    @Size(max = 256)
    private String title;

    private String body;

    /**
     * 编辑器内联样式覆盖 JSON 字符串。
     */
    private String styleOverrides;
}