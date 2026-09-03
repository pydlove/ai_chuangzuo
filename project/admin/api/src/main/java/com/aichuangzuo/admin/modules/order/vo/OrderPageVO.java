package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderPageVO {
    private List<OrderListVO> list;
    private long total;
    private BigDecimal totalAmount;
}
