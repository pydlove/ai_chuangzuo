package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;

/**
 * 自由创作模拟-随机提示词候选行。
 */
@Data
public class SkillPickRowDTO {

    private String bizNo;
    private String skillName;
    private Long publisherUserId;
    private String publisherNickname;
}
