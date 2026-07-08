package com.aichuangzuo.admin.modules.style.preset.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预设风格 SQL 直出行。Mapper XML 填充，Service 翻译成 {@link com.aichuangzuo.admin.modules.style.preset.vo.GlobalStyleVO}。
 */
@Data
public class SystemStyleRow {

    private String bizNo;
    private String styleName;
    private String description;
    private String promptSummary;
    private String prompt;
    private String scope;
    private Integer sourceType;
    private Integer enableStatus;
    private Integer auditStatus;
    private LocalDateTime createdAt;
}