package com.aichuangzuo.admin.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String nickname;
    private String email;
    private String planKey;
    private String planName;
    private String cycle;
    private String cycleName;
    private BigDecimal amount;
    private Integer status;
    private String statusName;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
