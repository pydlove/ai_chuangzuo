package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatsOverviewVO {
    private Long todayOrderCount;
    private BigDecimal todayRevenue;
    private Long monthOrderCount;
    private BigDecimal monthRevenue;
    private Long totalOrderCount;
    private BigDecimal totalRevenue;
}
