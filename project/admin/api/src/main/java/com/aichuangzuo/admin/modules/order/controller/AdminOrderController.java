package com.aichuangzuo.admin.modules.order.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.dto.request.OrderRefundRequest;
import com.aichuangzuo.admin.modules.order.service.AdminOrderService;
import com.aichuangzuo.admin.modules.order.vo.OrderDetailVO;
import com.aichuangzuo.admin.modules.order.vo.OrderPageVO;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.OrderTrendVO;
import com.aichuangzuo.admin.modules.order.vo.PlanDistributionVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端订单管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService orderService;

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public Result<OrderPageVO> listOrders(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "planKey", required = false) String planKey,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return Result.success(orderService.listOrders(keyword, planKey, status, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable(name = "id") Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @Operation(summary = "标记已支付")
    @PostMapping("/orders/{id}/mark-paid")
    public Result<Void> markPaid(@PathVariable(name = "id") Long id) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.markPaid(id, adminId);
        return Result.success();
    }

    @Operation(summary = "退款")
    @PostMapping("/orders/{id}/refund")
    public Result<Void> refund(@PathVariable(name = "id") Long id,
                               @Valid @RequestBody OrderRefundRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.refund(id, request.getReason(), adminId);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable(name = "id") Long id) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.cancel(id, adminId);
        return Result.success();
    }

    @Operation(summary = "手动调整会员")
    @PostMapping("/membership/adjust")
    public Result<Void> adjustMembership(@Valid @RequestBody MembershipAdjustRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.adjustMembership(request, adminId);
        return Result.success();
    }

    @Operation(summary = "手动发放会员")
    @PostMapping("/membership/grant")
    public Result<Void> grantMembership(@Valid @RequestBody MembershipGrantRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        orderService.grantMembership(request, adminId);
        return Result.success();
    }

    @Operation(summary = "统计概览")
    @GetMapping("/orders/stats/overview")
    public Result<OrderStatsOverviewVO> statsOverview() {
        return Result.success(orderService.getStatsOverview());
    }

    @Operation(summary = "收入趋势")
    @GetMapping("/orders/stats/trend")
    public Result<OrderTrendVO> statsTrend(@RequestParam(name = "days", defaultValue = "7") int days) {
        return Result.success(orderService.getStatsTrend(days));
    }

    @Operation(summary = "套餐分布")
    @GetMapping("/orders/stats/plan-distribution")
    public Result<PlanDistributionVO> planDistribution() {
        return Result.success(orderService.getPlanDistribution());
    }
}
