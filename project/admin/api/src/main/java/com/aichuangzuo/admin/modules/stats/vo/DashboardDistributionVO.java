package com.aichuangzuo.admin.modules.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardDistributionVO {

    /** 有效会员套餐分布 */
    private final List<Item> memberPlans;

    /** 作品平台分布 */
    private final List<Item> articlePlatforms;

    /** 累计支付金额按套餐分布 */
    private final List<Item> paidAmountByPlan;

    @Getter
    @AllArgsConstructor
    public static class Item {

        /** 分组键（套餐 key / 平台 key） */
        private final String key;

        /** 中文名称 */
        private final String name;

        /** 数量 */
        private final long count;

        /** 金额（元，无金额场景为 null） */
        private final BigDecimal amount;
    }
}
