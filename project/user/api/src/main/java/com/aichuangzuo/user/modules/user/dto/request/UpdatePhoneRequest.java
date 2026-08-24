package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改手机号请求体。
 *
 * <p>改手机号需要新手机号收到短信验证码，因此除了手机号格式还要求 6 位验证码。
 * 新手机号与旧手机号相同 / 手机号已被他人注册 / 验证码错误都由 service 层校验。
 */
@Data
public class UpdatePhoneRequest {

    @NotBlank(message = "新手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String newPhone;

    @NotBlank(message = "短信验证码不能为空")
    @Size(min = 6, max = 6, message = "短信验证码为 6 位")
    private String smsCode;
}
