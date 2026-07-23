package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称请求体。
 *
 * <p>校验规则：trim 后 1-20 字符。空字符串和超长由 Bean Validation 拦截，
 * 业务层还会再 trim 一次（防御 setNickname(" ") 的边缘情况）。
 */
@Data
public class UpdateNicknameRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 20, message = "昵称长度需在 1-20 个字符之间")
    private String nickname;
}