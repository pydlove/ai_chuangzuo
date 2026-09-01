package com.aichuangzuo.admin.modules.settings.paymentconfig.vo;

import lombok.Data;

/**
 * 支付配置 VO。
 */
@Data
public class PaymentConfigVO {

    private Long id;
    private String provider;
    private String appId;
    private String appSecret;
    private String gatewayUrl;
    private String refundUrl;
    private String notifyUrl;
    private String returnUrl;
    private Integer testMode;
}
