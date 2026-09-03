package com.aichuangzuo.admin.modules.order.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.order.dto.request.BatchCancelOrderRequest;
import com.aichuangzuo.admin.modules.order.dto.request.BatchDeleteOrderRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.dto.request.OrderRefundRequest;
import com.aichuangzuo.admin.modules.order.dto.request.RenewalUserQueryRequest;
import com.aichuangzuo.admin.modules.order.dto.request.RenewalOrderDetailQueryRequest;
import com.aichuangzuo.admin.modules.order.service.AdminOrderService;
import com.aichuangzuo.admin.modules.order.vo.OrderDetailVO;
import com.aichuangzuo.admin.modules.order.vo.OrderPageVO;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.OrderTrendVO;
import com.aichuangzuo.admin.modules.order.vo.PlanDistributionVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalDistributionVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalTrendVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalUserPageVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端订单管理")
@Slf4j
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
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询订单列表, adminUserId={}, keyword={}, planKey={}, status={}, startDate={}, endDate={}, page={}, pageSize={}",
                adminUserId, keyword, planKey, status, startDate, endDate, page, pageSize);
        return Result.success(orderService.listOrders(keyword, planKey, status, startDate, endDate, page, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable(name = "id") Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询订单详情, adminUserId={}, orderId={}", adminUserId, id);
        return Result.success(orderService.getOrderDetail(id));
    }

    @Operation(summary = "标记已支付")
    @PostMapping("/orders/{id}/mark-paid")
    public Result<Void> markPaid(@PathVariable(name = "id") Long id) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员标记订单已支付, adminUserId={}, orderId={}", adminId, id);
        orderService.markPaid(id, adminId);
        return Result.success();
    }

    @Operation(summary = "退款")
    @PostMapping("/orders/{id}/refund")
    public Result<Void> refund(@PathVariable(name = "id") Long id,
                               @Valid @RequestBody OrderRefundRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员退款订单, adminUserId={}, orderId={}, reason={}", adminId, id, request.getReason());
        orderService.refund(id, request.getReason(), adminId);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable(name = "id") Long id) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员取消订单, adminUserId={}, orderId={}", adminId, id);
        orderService.cancel(id, adminId);
        return Result.success();
    }

    @Operation(summary = "批量取消订单")
    @PostMapping("/orders/batch/cancel")
    public Result<Void> batchCancel(@Valid @RequestBody BatchCancelOrderRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量取消订单, adminUserId={}, count={}, ids={}", adminId, request.getIds().size(), request.getIds());
        orderService.batchCancel(request.getIds(), adminId);
        return Result.success();
    }

    @Operation(summary = "批量删除订单")
    @PostMapping("/orders/batch/delete")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteOrderRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量删除订单, adminUserId={}, count={}, ids={}", adminId, request.getIds().size(), request.getIds());
        orderService.batchDelete(request.getIds(), adminId);
        return Result.success();
    }

    @Operation(summary = "手动调整会员")
    @PostMapping("/membership/adjust")
    public Result<Void> adjustMembership(@Valid @RequestBody MembershipAdjustRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员手动调整会员, adminUserId={}, userId={}, level={}, expiresAt={}",
                adminId, request.getUserId(), request.getLevel(), request.getExpiresAt());
        orderService.adjustMembership(request, adminId);
        return Result.success();
    }

    @Operation(summary = "手动发放会员")
    @PostMapping("/membership/grant")
    public Result<Void> grantMembership(@Valid @RequestBody MembershipGrantRequest request) {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员手动发放会员, adminUserId={}, userId={}, planKey={}, startDate={}, endDate={}",
                adminId, request.getUserId(), request.getPlanKey(), request.getStartDate(), request.getEndDate());
        orderService.grantMembership(request, adminId);
        return Result.success();
    }

    @Operation(summary = "统计概览")
    @GetMapping("/orders/stats/overview")
    public Result<OrderStatsOverviewVO> statsOverview() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询订单统计概览, adminUserId={}", adminUserId);
        return Result.success(orderService.getStatsOverview());
    }

    @Operation(summary = "收入趋势")
    @GetMapping("/orders/stats/trend")
    public Result<OrderTrendVO> statsTrend(@RequestParam(name = "days", defaultValue = "7") int days) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询订单收入趋势, adminUserId={}, days={}", adminUserId, days);
        return Result.success(orderService.getStatsTrend(days));
    }

    @Operation(summary = "套餐分布")
    @GetMapping("/orders/stats/plan-distribution")
    public Result<PlanDistributionVO> planDistribution() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询订单套餐分布, adminUserId={}", adminUserId);
        return Result.success(orderService.getPlanDistribution());
    }

    @Operation(summary = "续费统计概览")
    @GetMapping("/orders/stats/renewal/overview")
    public Result<RenewalOverviewVO> renewalOverview() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询续费统计概览, adminUserId={}", adminUserId);
        return Result.success(orderService.getRenewalOverview());
    }

    @Operation(summary = "续费趋势")
    @GetMapping("/orders/stats/renewal/trend")
    public Result<RenewalTrendVO> renewalTrend(
            @RequestParam(name = "days", defaultValue = "7") int days) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询续费趋势, adminUserId={}, days={}", adminUserId, days);
        return Result.success(orderService.getRenewalTrend(days));
    }

    @Operation(summary = "续费分布")
    @GetMapping("/orders/stats/renewal/distribution")
    public Result<RenewalDistributionVO> renewalDistribution() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询续费分布, adminUserId={}", adminUserId);
        return Result.success(orderService.getRenewalDistribution());
    }

    @Operation(summary = "续费用户明细")
    @GetMapping("/orders/stats/renewal/users")
    public Result<RenewalUserPageVO> renewalUsers(
            @Valid @ModelAttribute RenewalUserQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询续费用户明细, adminUserId={}, keyword={}, planKey={}, cycle={}, page={}, renewalOnly={}",
                adminUserId, request.getKeyword(), request.getPlanKey(), request.getCycle(), request.getPage(),
                request.getRenewalOnly());
        return Result.success(orderService.listRenewalUsers(request));
    }

    @Operation(summary = "累计付费用户明细")
    @GetMapping("/orders/stats/renewal/paid-users")
    public Result<RenewalUserPageVO> paidUsers(
            @Valid @ModelAttribute RenewalUserQueryRequest request) {
        request.setRenewalOnly(false);
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询累计付费用户明细, adminUserId={}, keyword={}, planKey={}, cycle={}, page={}",
                adminUserId, request.getKeyword(), request.getPlanKey(), request.getCycle(), request.getPage());
        return Result.success(orderService.listRenewalUsers(request));
    }

    @Operation(summary = "续费/新购订单明细")
    @GetMapping("/orders/stats/renewal/orders")
    public Result<OrderPageVO> renewalOrders(
            @Valid @ModelAttribute RenewalOrderDetailQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询续费/新购订单明细, adminUserId={}, type={}, page={}",
                adminUserId, request.getType(), request.getPage());
        return Result.success(orderService.listRenewalOrderDetails(request));
    }
}
