package com.aichuangzuo.user.modules.membership.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 查询我的订单请求。
 */
@Data
public class ListOrderRequest {

    /**
     * 订单状态：0-待支付，1-已支付；不传表示全部。
     */
    @Min(value = 0, message = "订单状态非法")
    @Max(value = 1, message = "订单状态非法")
    private Integer status;

    @Min(value = 1, message = "页码至少为 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer pageSize = 20;
}
