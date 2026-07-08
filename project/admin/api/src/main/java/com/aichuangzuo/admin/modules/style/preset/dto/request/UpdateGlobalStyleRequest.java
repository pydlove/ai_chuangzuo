package com.aichuangzuo.admin.modules.style.preset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新预设风格请求体（全量字段）。
 */
@Data
public class UpdateGlobalStyleRequest {

    @NotBlank(message = "风格名称不能为空")
    @Size(max = 64, message = "风格名称最多 64 字")
    private String styleName;

    @Size(max = 256, message = "描述最多 256 字")
    private String description;

    @Size(max = 512, message = "提示词摘要最多 512 字")
    private String promptSummary;

    @NotBlank(message = "提示词不能为空")
    private String prompt;

    @Size(max = 256, message = "适用范围最多 256 字")
    private String scope;

    @NotNull(message = "启用状态不能为空")
    private Integer enableStatus;
}