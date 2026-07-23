package com.aichuangzuo.admin.modules.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserCreateRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 字符")
    private String email;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 64, message = "昵称长度需在 1-64 字符之间")
    private String nickname;

    /** 留空则使用默认密码；非空时由 Service 校验长度 6-32 字符 */
    private String password;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;
}
