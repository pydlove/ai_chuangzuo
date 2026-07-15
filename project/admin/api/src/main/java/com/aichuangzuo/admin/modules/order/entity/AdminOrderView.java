package com.aichuangzuo.admin.modules.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表/详情视图，JOIN u_user 结果。
 */
@Data
public class AdminOrderView {
    private Long id;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String email;
    private String planKey;
    private String cycle;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private String adminRemark;
    private Long operatorId;
    private LocalDateTime createdAt;
}
