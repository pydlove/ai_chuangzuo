package com.aichuangzuo.user.modules.membership.payment.xunhupay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 虎皮椒下单响应。
 */
@Data
public class XunhupayPaymentResponse {

    /** 历史遗留字段，实际为订单 ID。 */
    private String openid;

    private String url;

    @JsonProperty("url_qrcode")
    private String urlQrcode;

    private Integer errcode;

    private String errmsg;

    private String hash;
}
