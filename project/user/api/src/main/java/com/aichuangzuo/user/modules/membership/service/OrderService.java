package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.dto.request.ListOrderRequest;
import com.aichuangzuo.user.modules.membership.vo.OrderPageVO;

/**
 * 用户订单查询服务。
 */
public interface OrderService {

    /**
     * 查询当前用户的订单列表。
     *
     * @param userId  当前用户ID
     * @param request 查询条件
     * @return 分页订单列表
     */
    OrderPageVO listOrders(Long userId, ListOrderRequest request);
}
