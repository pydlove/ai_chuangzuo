package com.aichuangzuo.user.modules.skill.generate.vo;

import lombok.Data;

import java.util.List;

/**
 * 小爱帮写结果。
 */
@Data
public class SkillGenerateVO {

    /** 提示词名称。 */
    private String skillName;

    /** 简短描述。 */
    private String description;

    /** 角色。 */
    private String role;

    /** 受众。 */
    private String audience;

    /** 写作要求。 */
    private String requirements;

    /** 语气。 */
    private String tone;

    /** 禁区。 */
    private String restrictions;

    /** 示例。 */
    private String example;

    /** 适用范围标签。 */
    private List<String> scopeTags;

    /** 今日剩余帮写次数。 */
    private Integer remainingToday;
}
