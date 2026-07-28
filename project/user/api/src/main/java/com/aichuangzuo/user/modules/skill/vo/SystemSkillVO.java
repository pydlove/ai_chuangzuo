package com.aichuangzuo.user.modules.skill.vo;

import lombok.Data;

/**
 * 系统预设风格视图对象。
 */
@Data
public class SystemSkillVO {

    private String bizNo;
    private String name;
    private String description;
    private String promptSummary;
    private String prompt;
    private String scope;
}