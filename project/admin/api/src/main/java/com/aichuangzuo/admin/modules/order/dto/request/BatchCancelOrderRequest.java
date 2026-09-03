package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量取消订单请求。
 */
@Data
public class BatchCancelOrderRequest {

    @NotEmpty(message = "请选择要取消的订单")
    private List<Long> ids;
}
