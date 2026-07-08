package com.aichuangzuo.user.modules.article.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

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

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}