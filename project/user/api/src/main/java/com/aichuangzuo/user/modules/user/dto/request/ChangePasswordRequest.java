package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求体（已登录状态）。
 *
 * <p>与公开的 reset-password 不同：这里必须提供原密码。
 *
 * <p>长度校验统一放在 service 层（≥6 位且 ≤20 位），不在注解里写死，
 * 方便后续调整密码强度策略时只改 service。
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}