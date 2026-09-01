package com.aichuangzuo.user.modules.membership.payment.xunhupay.dto;

import lombok.Data;

/**
 * 虎皮椒异步通知参数。
 *
 * <p>参数名与官方文档保持一致。
 */
@Data
public class XunhupayNotifyParams {

    private String appid;

    /** 商户网站订单号。 */
    private String tradeOrderId;

    /** 订单支付金额。 */
    private String totalFee;

    /** 交易号。 */
    private String transactionId;

    /** 虎皮椒内部订单号。 */
    private String openOrderId;

    /** 订单标题。 */
    private String orderTitle;

    /** 订单状态：OD-已支付，CD-已退款，RD-退款中，UD-退款失败。 */
    private String status;

    private String plugins;

    /** 备注，回调原样返回。 */
    private String attach;

    private String time;
    private String nonceStr;
    private String hash;
}
