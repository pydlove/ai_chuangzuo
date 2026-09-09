
package com.aichuangzuo.user.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendSmsCodeRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    /** 发送场景：register-注册，reset_password-忘记密码，bind_phone-绑定/换绑手机号。 */
    @Pattern(regexp = "^(register|reset_password|bind_phone)$", message = "发送场景不合法")
    private String scene;
}
