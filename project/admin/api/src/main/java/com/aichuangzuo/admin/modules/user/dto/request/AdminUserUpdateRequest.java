package com.aichuangzuo.admin.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * 管理端编辑用户全部可改信息。
 */
@Data
public class AdminUserUpdateRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "昵称不能为空")
    @Length(min = 1, max = 64, message = "昵称长度 1-64 字符")
    private String nickname;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "enabled|disabled", message = "状态只能是 enabled 或 disabled")
    private String status;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    /** monthly / quarterly / yearly，null 表示无套餐 */
    private String membershipPlan;
}
