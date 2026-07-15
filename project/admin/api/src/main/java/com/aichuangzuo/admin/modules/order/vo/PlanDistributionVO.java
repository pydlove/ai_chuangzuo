package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanDistributionVO {
    private List<PlanItem> plans;
    private List<CycleItem> cycles;

    @Data
    public static class PlanItem {
        private String planKey;
        private String planName;
        private Long count;
        private BigDecimal revenue;
    }

    @Data
    public static class CycleItem {
        private String cycle;
        private String cycleName;
        private Long count;
    }
}
