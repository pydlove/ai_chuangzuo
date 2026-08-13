package com.aichuangzuo.admin.modules.skill.preset.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除预设风格请求。
 */
@Data
public class BatchDeleteGlobalSkillRequest {

    @NotEmpty(message = "请选择要删除的预设提示词")
    private List<String> bizNos;
}
