package com.aichuangzuo.user.modules.membership.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.membership.dto.request.ListOrderRequest;
import com.aichuangzuo.user.modules.membership.service.OrderService;
import com.aichuangzuo.user.modules.membership.vo.OrderPageVO;
import com.aichuangzuo.user.modules.membership.vo.OrderStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端订单接口。
 */
@Tag(name = "用户订单")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<OrderPageVO> listOrders(@Valid ListOrderRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("List user orders, userId={}, status={}, page={}, pageSize={}",
                userId, request.getStatus(), request.getPage(), request.getPageSize());
        return Result.success(orderService.listOrders(userId, request));
    }

    @Operation(summary = "查询单笔订单状态")
    @GetMapping("/{orderNo}/status")
    public Result<OrderStatusVO> getOrderStatus(@PathVariable String orderNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("Query order status, userId={}, orderNo={}", userId, orderNo);
        OrderStatusVO vo = orderService.getOrderStatus(userId, orderNo);
        if (vo == null) {
            return Result.success();
        }
        return Result.success(vo);
    }
}
