package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderTrendVO {
    private List<String> dates;
    private List<BigDecimal> revenues;
    private List<Long> orderCounts;
}
