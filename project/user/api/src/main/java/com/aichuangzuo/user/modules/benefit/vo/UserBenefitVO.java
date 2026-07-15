package com.aichuangzuo.user.modules.benefit.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 当前用户权益视图。
 */
@Getter
@Setter
public class UserBenefitVO {

    /** 套餐 key；无会员时为 free。 */
    private String planKey;

    /** 套餐名称；无会员时为 免费版。 */
    private String planName;

    /** 会员到期日期（yyyy-MM-dd）；无会员时为 null。 */
    private String expiresAt;

    /** 权益列表。 */
    private List<BenefitItem> benefits;

    /**
     * 单项权益。
     */
    @Getter
    @Setter
    public static class BenefitItem {

        /** 权益编码。 */
        private String code;

        /** 权益名称。 */
        private String name;

        /** 类型：boolean/quota/tier。 */
        private String type;

        /** 权益值。 */
        private String value;

        /** 已用量（仅 quota 类型有值）。 */
        private Integer used;

        /** 剩余额度（仅 quota 类型有值）。 */
        private Integer remaining;
    }
}
