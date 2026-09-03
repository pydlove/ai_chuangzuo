package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除订单请求。
 */
@Data
public class BatchDeleteOrderRequest {

    @NotEmpty(message = "请选择要删除的订单")
    private List<Long> ids;
}
