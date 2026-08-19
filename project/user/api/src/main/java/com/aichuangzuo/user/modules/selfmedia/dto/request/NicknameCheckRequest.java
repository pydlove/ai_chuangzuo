package com.aichuangzuo.user.modules.selfmedia.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NicknameCheckRequest {

    @NotBlank(message = "请输入账号昵称")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    /** 平台显示名，如「小红书」。后端优先使用该值；为空时从当前自媒体方案读取。 */
    @Size(max = 64, message = "平台名长度不能超过 64 个字符")
    private String platform;

    /** 自媒体定位摘要，后端优先使用该值；为空时从当前自媒体方案自动拼装。 */
    @Size(max = 1000, message = "定位摘要长度不能超过 1000 个字符")
    private String positioning;
}

