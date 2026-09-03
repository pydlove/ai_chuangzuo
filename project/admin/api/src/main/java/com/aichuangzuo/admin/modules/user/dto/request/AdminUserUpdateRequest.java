package com.aichuangzuo.admin.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管理端编辑用户全部可改信息。
 */
@Data
public class AdminUserUpdateRequest {
    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

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

    /** 套餐 key（如 basic / pro / flagship），null 表示无套餐 */
    private String membershipPlan;

    /** 头像 URL（上传后返回的访问路径） */
    @Length(max = 512, message = "头像 URL 长度不能超过 512 字符")
    private String avatarUrl;

    /** 当月创作币收益（覆盖管理员设置值，null 表示不修改） */
    @DecimalMin(value = "0", inclusive = true, message = "当月创作币收益必须大于或等于 0")
    private BigDecimal monthlyCoinEarnings;
}
