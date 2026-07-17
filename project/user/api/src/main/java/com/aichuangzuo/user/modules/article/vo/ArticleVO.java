package com.aichuangzuo.user.modules.article.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品详情 VO。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleVO {

    private String bizNo;

    private String title;

    private String body;

    /**
     * 编辑器内联样式覆盖；前端可解析为 {blocks, inlines}。
     */
    private Object styleOverrides;

    private String platform;

    private String style;

    private String template;

    private Integer wordCount;

    /** 发布描述（pipeline 第 13 阶段 AI 生成）。 */
    private String description;

    /** 推荐标签（pipeline 第 13 阶段 AI 生成）。 */
    private List<String> tags;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}