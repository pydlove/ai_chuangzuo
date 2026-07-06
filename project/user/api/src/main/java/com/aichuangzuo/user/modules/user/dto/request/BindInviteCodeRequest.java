package com.aichuangzuo.user.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 绑定邀请码请求。
 *
 * <p>仅支持 6 位邀请码，与注册时 {@code RegisterRequest#inviteCode} 保持一致。
 */
@Data
public class BindInviteCodeRequest {

    /** 邀请码，6 位字符。 */
    @NotBlank(message = "邀请码不能为空")
    @Size(max = 6, message = "邀请码最多 6 位")
    private String inviteCode;
}
