package com.aichuangzuo.user.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email(message = "邮箱格式错误")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Size(min = 6, max = 6, message = "短信验证码为 6 位")
    private String smsCode;

    @Size(min = 6, max = 6, message = "邮箱验证码为 6 位")
    private String emailCode;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Size(max = 6, message = "邀请码最多 6 位")
    private String inviteCode;

    private Boolean rememberMe;
}
