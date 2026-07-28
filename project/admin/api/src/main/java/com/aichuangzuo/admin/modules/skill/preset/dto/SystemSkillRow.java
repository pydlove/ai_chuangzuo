package com.aichuangzuo.admin.modules.skill.preset.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预设风格 SQL 直出行。Mapper XML 填充，Service 翻译成 {@link com.aichuangzuo.admin.modules.skill.preset.vo.GlobalSkillVO}。
 */
@Data
public class SystemSkillRow {

    private String bizNo;
    private String skillName;
    private String description;
    private String promptSummary;
    private String prompt;
    private String scope;
    private Integer sourceType;
    private Integer enableStatus;
    private Integer auditStatus;
    private LocalDateTime createdAt;
}