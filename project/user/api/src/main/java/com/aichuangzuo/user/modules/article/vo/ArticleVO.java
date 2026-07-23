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

    /**
     * 风格可读名称；市场风格（SM 开头）会解析为 u_style_market.style_name，
     * 用户/系统预设风格直接回显 style 字段本身。
     */
    private String styleName;

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