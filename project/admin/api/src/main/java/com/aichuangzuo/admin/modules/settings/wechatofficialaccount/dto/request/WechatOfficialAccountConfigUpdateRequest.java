package com.aichuangzuo.admin.modules.settings.wechatofficialaccount.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 微信公众号配置更新请求。
 */
@Data
public class WechatOfficialAccountConfigUpdateRequest {

    /** 公众号 AppID。 */
    @NotBlank(message = "AppID 不能为空")
    private String appId;

    /** 公众号 AppSecret。留空或全为 * 号表示不修改原密钥。 */
    private String appSecret;

    /** 服务器配置 Token。 */
    @NotBlank(message = "Token 不能为空")
    private String token;

    /** 是否明文模式：0-否，1-是。 */
    @NotNull(message = "明文模式不能为空")
    private Integer plaintextMode;

    /** 是否启用：0-否，1-是。 */
    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
}
