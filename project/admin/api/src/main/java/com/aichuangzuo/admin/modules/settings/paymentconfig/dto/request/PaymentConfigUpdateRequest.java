package com.aichuangzuo.admin.modules.settings.paymentconfig.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付配置更新请求。
 */
@Data
public class PaymentConfigUpdateRequest {

    /** 支付服务商。 */
    @NotBlank(message = "支付服务商不能为空")
    private String provider;

    /** 虎皮椒 App ID。 */
    @NotBlank(message = "App ID 不能为空")
    private String appId;

    /** 虎皮椒 App Secret。留空或全为 * 号表示不修改原密钥。 */
    private String appSecret;

    /** 虎皮椒下单网关地址。 */
    @NotBlank(message = "网关地址不能为空")
    private String gatewayUrl;

    /** 虎皮椒退款网关地址。 */
    private String refundUrl;

    /** 虎皮椒异步通知地址。 */
    private String notifyUrl;

    /** 虎皮椒支付完成回跳地址。 */
    private String returnUrl;

    /** 是否测试模式：0-否，1-是。 */
    @NotNull(message = "测试模式不能为空")
    private Integer testMode;
}
