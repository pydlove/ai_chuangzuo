package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

/**
 * 支付配置公开信息。
 */
@Data
public class PaymentConfigVO {

    /** 是否启用支付：0-否，1-是。 */
    private Integer enabled;

    /** 是否测试模式：0-否，1-是。 */
    private Integer testMode;
}
