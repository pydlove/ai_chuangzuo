package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改邮箱请求体。
 *
 * <p>改邮箱需要新邮箱收到验证码，因此除了邮箱格式还要求 6 位验证码。
 * 新邮箱与旧邮箱相同 / 邮箱已被他人注册 / 验证码错误都由 service 层校验。
 */
@Data
public class UpdateEmailRequest {

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String newEmail;

    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码为 6 位")
    private String emailCode;
}