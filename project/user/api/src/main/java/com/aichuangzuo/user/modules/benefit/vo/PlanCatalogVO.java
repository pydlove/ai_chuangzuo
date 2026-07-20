package com.aichuangzuo.user.modules.benefit.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 公开查询：定价页完整目录（套餐 + 卡片特性 + 对比表）。
 */
@Getter
@Setter
public class PlanCatalogVO {

    private List<PlanVO> plans;
    private List<CompareRowVO> compareRows;

    @Getter
    @Setter
    public static class PlanVO {
        private String key;
        private String name;
        private boolean recommended;
        private PriceBlock monthly;
        private PriceBlock quarter;
        private PriceBlock year;
        private List<FeatureVO> features;
    }

    @Getter
    @Setter
    public static class PriceBlock {
        private BigDecimal original;
        private BigDecimal current;
        private String articles;
        /** 年付立省金额（仅 year block 有值）。 */
        private BigDecimal savings;
    }

    @Getter
    @Setter
    public static class FeatureVO {
        /** 权益 code（用于客户端按 code 反查/翻译）。 */
        private String code;
        /** 卡片上展示的特性文案。 */
        private String text;
        /** 当前套餐是否包含（false 时灰显）。 */
        private boolean included;
    }

    @Getter
    @Setter
    public static class CompareRowVO {
        /** 权益 code。 */
        private String code;
        /** 对比表行标签。 */
        private String label;
        /** 各套餐的单元格值；true=包含（显示 ✓），false=不包含（显示 ✗），其他字符串原样展示。 */
        private CompareCell basic;
        private CompareCell pro;
        private CompareCell flagship;
    }

    /** 接受 true / false / String 三态。 */
    public static class CompareCell {
        private final Object value;

        public CompareCell(Object value) { this.value = value; }
        public Object getValue() { return value; }
        public boolean isBoolean() { return value instanceof Boolean; }
    }
}