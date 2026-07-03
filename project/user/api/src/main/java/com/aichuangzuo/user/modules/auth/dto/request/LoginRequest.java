package com.aichuangzuo.user.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "图形验证码标识不能为空")
    private String captchaKey;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
