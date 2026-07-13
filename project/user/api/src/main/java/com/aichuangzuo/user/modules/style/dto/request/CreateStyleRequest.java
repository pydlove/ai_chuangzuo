package com.aichuangzuo.user.modules.style.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建风格请求。
 */
@Data
public class CreateStyleRequest {

    @NotBlank(message = "风格名称不能为空")
    @Size(min = 1, max = 20, message = "风格名称长度需为 1-20 字符")
    private String styleName;

    @NotBlank(message = "风格提示词不能为空")
    @Size(min = 1, max = 1000, message = "风格提示词长度需为 1-1000 字符")
    private String prompt;

    @Size(max = 256, message = "适用范围过长")
    private String scope;

    /** 来源类型：1-自定义（默认），2-学习；系统预设（3）不允许用户创建。 */
    @Min(value = 1, message = "来源类型不合法")
    @Max(value = 2, message = "来源类型不合法")
    private Integer sourceType;
}
