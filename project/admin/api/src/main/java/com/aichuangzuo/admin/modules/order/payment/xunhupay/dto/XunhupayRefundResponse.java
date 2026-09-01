package com.aichuangzuo.admin.modules.order.payment.xunhupay.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 虎皮椒退款响应。
 */
@Data
public class XunhupayRefundResponse {

    /** 商户网站订单号。 */
    private String tradeOrderId;

    /** 交易号。 */
    private String transactionId;

    /** 退款单号。 */
    private String outRefundNo;

    /** 退款金额。 */
    private BigDecimal refundFee;

    /** 退款原因。 */
    private String reason;

    /** 退款状态：OD-已支付，CD-已退款，RD-退款中，UD-退款失败。 */
    private String refundStatus;

    /** 退款时间。 */
    private String refundTime;

    /** 错误码。 */
    private Integer errcode;

    /** 错误信息。 */
    private String errmsg;

    /** 数据签名。 */
    private String hash;
}
