package com.aichuangzuo.user.modules.membership.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态查询结果。
 */
@Data
public class OrderStatusVO {

    /** 订单编号。 */
    private String orderNo;

    /** 订单状态：0-待支付，1-已支付。 */
    private Integer status;

    /** 支付时间。 */
    private LocalDateTime paidAt;
}
