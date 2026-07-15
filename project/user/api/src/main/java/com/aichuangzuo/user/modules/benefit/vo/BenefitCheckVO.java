package com.aichuangzuo.user.modules.benefit.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 权益校验/消费结果视图。
 */
@Getter
@Setter
public class BenefitCheckVO {

    /** 是否放行。 */
    private Boolean allowed;

    /** 权益编码。 */
    private String code;

    /** 类型：boolean/quota/tier。 */
    private String type;

    /** 权益值。 */
    private String value;

    /** 已用量（仅 quota 类型有值）。 */
    private Integer used;

    /** 剩余额度（仅 quota 类型有值）。 */
    private Integer remaining;

    /** 提示信息（不放行时给出原因）。 */
    private String message;
}
