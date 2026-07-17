package com.aichuangzuo.user.modules.article.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 草稿详情 VO。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftVO {

    private String bizNo;

    private String customTitle;

    private String customRequirement;

    private String platform;

    private Integer wordCount;

    private String style;

    private String template;

    private String createMode;

    private LocalDateTime savedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}