package com.aichuangzuo.user.modules.membership.payment.xunhupay.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 虎皮椒下单请求参数。
 */
@Data
public class XunhupayPaymentRequest {

    private String version = "1.1";
    private String appid;
    private String tradeOrderId;
    private BigDecimal totalFee;
    private String title;
    private Integer time;
    private String callbackUrl;
    private String notifyUrl;
    private String returnUrl;
    private String plugins;
    private String nonceStr;
    private String hash;
}
