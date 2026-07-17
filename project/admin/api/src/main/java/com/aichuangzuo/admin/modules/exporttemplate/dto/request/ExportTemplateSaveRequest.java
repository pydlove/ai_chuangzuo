package com.aichuangzuo.admin.modules.exporttemplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExportTemplateSaveRequest {

    @NotBlank(message = "模板标识不能为空")
    @Size(max = 64)
    private String templateKey;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128)
    private String name;

    @Size(max = 32)
    private String platform;

    @Size(max = 256)
    private String description;

    @Size(max = 16)
    private String bgColor;

    @Size(max = 16)
    private String textColor;

    /** 视觉预设 JSON 字符串 */
    private String visualStyleJson;

    @Size(max = 128)
    private String signatureText;

    @Size(max = 8)
    private String signaturePosition;

    private Integer sortOrder;
    private Integer status;
}
