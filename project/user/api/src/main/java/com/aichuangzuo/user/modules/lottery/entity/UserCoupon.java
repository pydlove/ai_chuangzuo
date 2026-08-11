package com.aichuangzuo.user.modules.lottery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String couponCode;
    private String couponType;
    private BigDecimal discountValue;
    private String applicableCycle;
    private String applicablePlan;
    private String status;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Long usedOrderId;

    private Long tenantId;

    private LocalDateTime createdAt;
}
