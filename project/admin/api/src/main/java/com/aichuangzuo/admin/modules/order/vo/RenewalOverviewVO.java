package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RenewalOverviewVO {
    private Long totalPaidUsers;
    private Long renewalUsers;
    private BigDecimal renewalRate;
    private Long renewalOrders;
    private BigDecimal renewalRevenue;
    private Long firstOrders;
    private BigDecimal firstRevenue;
}
