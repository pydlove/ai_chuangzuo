package com.aichuangzuo.user.modules.skill.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户风格视图对象。
 */
@Data
public class UserSkillVO {

    private String bizNo;
    private String skillName;
    private String prompt;
    private String description;
    private String promptSummary;
    private String scope;
    private Integer enableStatus;
    private Integer sourceType;
    private Integer useCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 审核状态：0-待审核，1-已通过，2-已拒绝。 */
    private Integer auditStatus;
}
