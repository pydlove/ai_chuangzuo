package com.aichuangzuo.admin.modules.settings.paymentconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 支付配置，对应表 {@code a_payment_config}。
 *
 * <p>单行配置（id=1），由 admin 端系统设置-支付设置维护。
 * AppSecret 落库为 Jasypt 加密后的密文。
 */
@Getter
@Setter
@TableName("a_payment_config")
public class PaymentConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付服务商：当前仅 xunhupay。 */
    private String provider;

    /** 虎皮椒 App ID。 */
    private String appId;

    /** Jasypt 加密后的虎皮椒 App Secret。 */
    private String appSecret;

    /** 虎皮椒下单网关地址。 */
    private String gatewayUrl;

    /** 虎皮椒退款网关地址。 */
    private String refundUrl;

    /** 虎皮椒异步通知地址。 */
    private String notifyUrl;

    /** 虎皮椒支付完成回跳地址。 */
    private String returnUrl;

    /** 是否启用支付：0-否，1-是。 */
    private Integer enabled;

    /** 是否测试模式：0-否，1-是。 */
    private Integer testMode;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
