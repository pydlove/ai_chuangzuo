package com.aichuangzuo.user.modules.style.dto.request;

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
}
