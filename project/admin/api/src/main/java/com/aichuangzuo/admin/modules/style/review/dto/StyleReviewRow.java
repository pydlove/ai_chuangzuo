package com.aichuangzuo.admin.modules.style.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风格审核 SQL 直出记录。Mapper XML 填充，业务层翻译成 {@link com.aichuangzuo.admin.modules.style.review.vo.StyleReviewVO}。
 *
 * <p>数字字段保留 int / Integer，避免 MyBatis 把 int 列自动转 String 时的歧义。
 */
@Data
public class StyleReviewRow {

    private String bizNo;
    private Long userId;
    private String creatorName;
    private String styleName;
    private String prompt;
    private String scope;
    private Integer sourceType;
    private Integer auditStatus;
    private String rejectReason;
    private LocalDateTime createdAt;
}