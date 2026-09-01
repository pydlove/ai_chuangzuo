package com.aichuangzuo.user.modules.membership.payment.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 支付配置，对应管理端表 {@code a_payment_config}。
 *
 * <p>用户端只读使用，配置由 admin 端维护。
 */
@Getter
@Setter
@TableName("a_payment_config")
public class PaymentConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String provider;
    private String appId;
    private String appSecret;
    private String gatewayUrl;
    private String refundUrl;
    private String notifyUrl;
    private String returnUrl;
    private Integer enabled;
    private Integer testMode;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
