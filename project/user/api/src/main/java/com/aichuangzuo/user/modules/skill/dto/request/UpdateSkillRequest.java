package com.aichuangzuo.user.modules.skill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改风格请求。
 */
@Data
public class UpdateSkillRequest {

    @NotBlank(message = "风格名称不能为空")
    @Size(min = 1, max = 20, message = "风格名称长度需为 1-20 字符")
    private String skillName;

    @NotBlank(message = "skill 提示词不能为空")
    @Size(min = 1, max = 1200, message = "skill 提示词长度需为 1-1200 字符")
    private String prompt;

    @Size(max = 256, message = "适用范围过长")
    private String scope;
}
