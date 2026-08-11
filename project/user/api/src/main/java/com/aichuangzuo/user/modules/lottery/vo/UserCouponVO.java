package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private String couponCode;
    private String couponType;
    private BigDecimal discountValue;
    private String applicableCycle;
    private String applicablePlan;
    private String status;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Long usedOrderId;
}
