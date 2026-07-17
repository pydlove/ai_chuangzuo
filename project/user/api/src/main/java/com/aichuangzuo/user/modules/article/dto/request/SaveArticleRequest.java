package com.aichuangzuo.user.modules.article.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建作品请求（生成完成时调用）。
 */
@Data
public class SaveArticleRequest {

    @NotBlank
    @Size(max = 256)
    private String title;

    @NotBlank
    private String body;

    /**
     * 编辑器内联样式覆盖 JSON 字符串；为空时使用默认空覆盖。
     */
    private String styleOverrides;

    @Size(max = 32)
    private String platform;

    @Size(max = 64)
    private String style;

    @Size(max = 64)
    private String template;

    private Integer wordCount;

    /** 发布描述（pipeline 第 13 阶段 AI 生成）。 */
    @Size(max = 512)
    private String description;

    /** 推荐标签（pipeline 第 13 阶段 AI 生成）。 */
    private List<String> tags;

    private LocalDateTime completedAt;
}