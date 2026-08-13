package com.aichuangzuo.user.modules.skill.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建风格请求。
 */
@Data
public class CreateSkillRequest {

    @NotBlank(message = "风格名称不能为空")
    @Size(min = 1, max = 20, message = "风格名称长度需为 1-20 字符")
    private String skillName;

    @NotBlank(message = "skill 提示词不能为空")
    @Size(min = 1, max = 1200, message = "skill 提示词长度需为 1-1200 字符")
    private String prompt;

    /** 原文提示词示例片段1（学习的提示词用）。 */
    @Size(max = 255, message = "示例片段1 过长")
    private String excerpt1;

    /** 原文提示词示例片段2（学习的提示词用）。 */
    @Size(max = 255, message = "示例片段2 过长")
    private String excerpt2;

    @Size(max = 256, message = "适用范围过长")
    private String scope;

    /** 结构化提示词 JSON（templateBased/role/audience/requirements/tone/restrictions）。 */
    @Size(max = 4000, message = "结构化提示词过长")
    private String promptExtra;

    /** 简短描述，一句话方便创作者快速了解该提示词。 */
    @Size(max = 100, message = "简短描述不能超过 100 字符")
    private String description;

    /** 来源类型：1-自定义（默认），2-学习；系统预设（3）不允许用户创建。 */
    @Min(value = 1, message = "来源类型不合法")
    @Max(value = 2, message = "来源类型不合法")
    private Integer sourceType;
}
