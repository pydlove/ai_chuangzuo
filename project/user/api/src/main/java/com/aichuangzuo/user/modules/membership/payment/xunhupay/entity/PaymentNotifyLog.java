package com.aichuangzuo.user.modules.membership.payment.xunhupay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 支付异步通知日志，对应表 {@code u_payment_notify_log}。
 */
@Getter
@Setter
@TableName("u_payment_notify_log")
public class PaymentNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private String nonceStr;
    private String tradeOrderId;
    private String rawBody;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createdAt;
}
