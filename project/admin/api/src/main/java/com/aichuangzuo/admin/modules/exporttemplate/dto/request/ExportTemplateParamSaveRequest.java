package com.aichuangzuo.admin.modules.exporttemplate.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新建 / 更新参数定义请求。
 */
@Data
public class ExportTemplateParamSaveRequest {

    @NotBlank
    private String paramKey;

    @NotBlank
    private String displayLabel;

    /** color / number / text / select / border */
    @NotBlank
    private String fieldType;

    @NotBlank
    private String groupLabel;

    private String defaultValue;

    /** select 类型必填，JSON 数组字符串，如 ["left","center","right"]。 */
    private String optionsJson;

    private Integer sortOrder;
}