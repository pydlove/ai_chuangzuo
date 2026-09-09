package com.aichuangzuo.user.modules.skill.generate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小爱帮写请求。
 */
@Data
public class GenerateSkillRequest {

    /** 运营方案或提示词描述方向。 */
    @NotBlank(message = "请描述你的运营方案或提示词方向")
    @Size(min = 10, max = 1000, message = "描述长度需为 10-1000 字符")
    private String requirement;
}
