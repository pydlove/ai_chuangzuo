package com.aichuangzuo.admin.modules.order.service.impl;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.dto.request.RenewalUserQueryRequest;
import com.aichuangzuo.admin.modules.order.dto.request.RenewalOrderDetailQueryRequest;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.enums.AdminOrderErrorCode;
import com.aichuangzuo.admin.modules.order.enums.OrderStatus;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.order.mapper.AdminOrderMapper;
import com.aichuangzuo.admin.modules.order.payment.xunhupay.client.XunhupayRefundClient;
import com.aichuangzuo.admin.modules.order.payment.xunhupay.dto.XunhupayRefundResponse;
import com.aichuangzuo.admin.modules.order.service.AdminOrderService;
import com.aichuangzuo.admin.modules.order.vo.*;
import com.aichuangzuo.admin.modules.settings.paymentconfig.entity.PaymentConfig;
import com.aichuangzuo.admin.modules.settings.paymentconfig.service.PaymentConfigService;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final Map<String, String> PLAN_NAMES = Map.of(
            "basic", "基础版", "pro", "专业版", "flagship", "旗舰版");
    private static final Map<String, String> CYCLE_NAMES = Map.of(
            "month", "月付", "quarter", "季付", "year", "年付");
    private static final Map<String, Integer> CYCLE_DAYS = Map.of(
            "month", 30, "quarter", 90, "year", 365);

    private final AdminOrderMapper orderMapper;
    private final AdminMembershipMapper membershipMapper;
    private final PaymentConfigService paymentConfigService;
    private final XunhupayRefundClient xunhupayRefundClient;

    private static final long PAYMENT_CONFIG_ID = 1L;
    private static final String PAYMENT_METHOD_XUNHUPAY = "xunhupay";
    private static final String REFUND_STATUS_COMPLETED = "CD";
    private static final String REFUND_STATUS_PROCESSING = "RD";

    @Override
    public OrderPageVO listOrders(String keyword, String planKey, Integer status,
                                  String startDate, String endDate, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        List<AdminOrderView> rows = orderMapper.selectPage(keyword, planKey, status, startDate, endDate, offset, pageSize);
        long total = orderMapper.countPage(keyword, planKey, status, startDate, endDate);

        List<OrderListVO> list = rows.stream().map(this::toListVO).toList();
        OrderPageVO vo = new OrderPageVO();
        vo.setList(list);
        vo.setTotal(total);
        return vo;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        AdminOrderView row = orderMapper.selectDetailById(id);
        if (row == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        return toDetailVO(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long id, Long operatorId) {
        AdminOrderView order = orderMapper.selectDetailById(id);
        if (order == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        orderMapper.markPaid(id, operatorId, now);
        activateOrExtendMembership(order.getUserId(), order.getPlanKey(), order.getCycle());

        log.info("管理员标记订单已支付 orderId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id, String reason, Long operatorId) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(AdminOrderErrorCode.REFUND_REASON_REQUIRED);
        }
        AdminOrderView order = orderMapper.selectDetailById(id);
        if (order == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        String thirdPartyRefundId = null;
        BigDecimal refundAmount = order.getAmount();

        // 真实虎皮椒支付订单（有第三方交易号）需先调用网关退款
        if (PAYMENT_METHOD_XUNHUPAY.equals(order.getPaymentMethod()) && StringUtils.hasText(order.getThirdPartyTradeId())) {
            PaymentConfig config = paymentConfigService.getConfig(PAYMENT_CONFIG_ID);
            if (config == null || !StringUtils.hasText(config.getAppId()) || !StringUtils.hasText(config.getAppSecret())) {
                throw new BusinessException(AdminOrderErrorCode.PAYMENT_CONFIG_NOT_FOUND);
            }

            XunhupayRefundResponse response = xunhupayRefundClient.refund(config, order.getOrderNo(), reason);
            if (response.getErrcode() == null || response.getErrcode() != 0) {
                log.error("虎皮椒退款失败 orderId={}, orderNo={}, errcode={}, errmsg={}",
                        id, order.getOrderNo(), response.getErrcode(), response.getErrmsg());
                String msg = response.getErrmsg() != null ? response.getErrmsg() : "退款失败";
                throw new BusinessException(AdminOrderErrorCode.REFUND_FAILED.getCode(), msg);
            }

            String refundStatus = response.getRefundStatus();
            if (!REFUND_STATUS_COMPLETED.equals(refundStatus) && !REFUND_STATUS_PROCESSING.equals(refundStatus)) {
                log.error("虎皮椒退款状态异常 orderId={}, orderNo={}, refundStatus={}",
                        id, order.getOrderNo(), refundStatus);
                String msg = "退款状态异常: " + (refundStatus == null ? "未知" : refundStatus);
                throw new BusinessException(AdminOrderErrorCode.REFUND_FAILED.getCode(), msg);
            }

            thirdPartyRefundId = response.getOutRefundNo();
            if (response.getRefundFee() != null) {
                refundAmount = response.getRefundFee();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        orderMapper.refund(id, reason, operatorId, now, thirdPartyRefundId, refundAmount);

        // 退款直接取消会员：expires_at 设为昨天
        AdminMembership membership = membershipMapper.selectByUserId(order.getUserId());
        if (membership != null) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            membership.setExpiresAt(yesterday);
            membershipMapper.updateMembership(membership);
            syncUserMembershipFields(order.getUserId(), yesterday, order.getCycle());
        }

        log.info("管理员退款 orderId={}, operatorId={}, reason={}, thirdPartyRefundId={}, refundAmount={}",
                id, operatorId, reason, thirdPartyRefundId, refundAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, Long operatorId) {
        AdminOrderView order = orderMapper.selectDetailById(id);
        if (order == null) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(AdminOrderErrorCode.ORDER_STATUS_NOT_ALLOWED);
        }

        orderMapper.cancel(id, operatorId);
        log.info("管理员取消订单 orderId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustMembership(MembershipAdjustRequest request, Long operatorId) {
        if (membershipMapper.userExists(request.getUserId()) == 0) {
            throw new BusinessException(AdminOrderErrorCode.USER_NOT_FOUND);
        }

        AdminMembership membership = membershipMapper.selectByUserId(request.getUserId());
        if (membership == null) {
            membership = new AdminMembership();
            membership.setUserId(request.getUserId());
            membership.setLevel(request.getLevel());
            membership.setStartedAt(LocalDate.now());
            membership.setExpiresAt(request.getExpiresAt());
            membershipMapper.insertMembership(membership);
        } else {
            membership.setLevel(request.getLevel());
            membership.setStartedAt(LocalDate.now());
            membership.setExpiresAt(request.getExpiresAt());
            membershipMapper.updateMembership(membership);
        }

        syncUserMembershipFields(request.getUserId(), request.getExpiresAt(), request.getLevel());
        log.info("管理员调整会员 userId={}, level={}, expiresAt={}, operatorId={}",
                request.getUserId(), request.getLevel(), request.getExpiresAt(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMembership(MembershipGrantRequest request, Long operatorId) {
        if (membershipMapper.userExists(request.getUserId()) == 0) {
            throw new BusinessException(AdminOrderErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        String remark = request.getRemark() != null ? "手动发放：" + request.getRemark() : "手动发放";
        String orderNo = generateOrderNo();

        orderMapper.insertGrantOrder(orderNo, request.getUserId(), request.getPlanKey(),
                request.getStartDate(), request.getEndDate(), remark, operatorId, now);
        activateOrExtendMembership(request.getUserId(), request.getPlanKey(),
                request.getStartDate(), request.getEndDate());

        log.info("管理员发放会员 userId={}, planKey={}, startDate={}, endDate={}, operatorId={}",
                request.getUserId(), request.getPlanKey(), request.getStartDate(), request.getEndDate(), operatorId);
    }

    @Override
    public OrderStatsOverviewVO getStatsOverview() {
        return orderMapper.statsOverview();
    }

    @Override
    public OrderTrendVO getStatsTrend(int days) {
        if (days != 7 && days != 30) {
            days = 7;
        }
        List<Map<String, Object>> rows = orderMapper.statsTrend(days);
        OrderTrendVO vo = new OrderTrendVO();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            dates.add((String) row.get("dateLabel"));
            revenues.add((BigDecimal) row.get("revenue"));
            orderCounts.add(((Number) row.get("orderCount")).longValue());
        }
        vo.setDates(dates);
        vo.setRevenues(revenues);
        vo.setOrderCounts(orderCounts);
        return vo;
    }

    @Override
    public RenewalOverviewVO getRenewalOverview() {
        RenewalOverviewVO vo = orderMapper.selectRenewalOverview();
        if (vo.getTotalPaidUsers() != null && vo.getTotalPaidUsers() > 0) {
            BigDecimal rate = BigDecimal.valueOf(vo.getRenewalUsers())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(vo.getTotalPaidUsers()), 2, RoundingMode.HALF_UP);
            vo.setRenewalRate(rate);
        } else {
            vo.setRenewalRate(BigDecimal.ZERO);
        }
        return vo;
    }

    @Override
    public RenewalTrendVO getRenewalTrend(int days) {
        if (days != 7 && days != 30 && days != 90) {
            days = 7;
        }
        List<Map<String, Object>> rows = orderMapper.selectRenewalTrend(days);
        RenewalTrendVO vo = new RenewalTrendVO();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Long> userCounts = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            dates.add((String) row.get("dateLabel"));
            revenues.add((BigDecimal) row.get("revenue"));
            orderCounts.add(((Number) row.get("orderCount")).longValue());
            userCounts.add(((Number) row.get("userCount")).longValue());
        }
        vo.setDates(dates);
        vo.setRevenues(revenues);
        vo.setOrderCounts(orderCounts);
        vo.setUserCounts(userCounts);
        return vo;
    }

    @Override
    public RenewalDistributionVO getRenewalDistribution() {
        List<Map<String, Object>> planRows = orderMapper.selectRenewalPlanDistribution();
        List<Map<String, Object>> cycleRows = orderMapper.selectRenewalCycleDistribution();

        RenewalDistributionVO vo = new RenewalDistributionVO();
        List<RenewalDistributionVO.PlanItem> plans = new ArrayList<>();
        for (Map<String, Object> row : planRows) {
            RenewalDistributionVO.PlanItem item = new RenewalDistributionVO.PlanItem();
            String key = (String) row.get("planKey");
            item.setPlanKey(key);
            item.setPlanName(PLAN_NAMES.getOrDefault(key, key));
            item.setCount(((Number) row.get("count")).longValue());
            item.setRevenue((BigDecimal) row.get("revenue"));
            plans.add(item);
        }
        vo.setPlans(plans);

        List<RenewalDistributionVO.CycleItem> cycles = new ArrayList<>();
        for (Map<String, Object> row : cycleRows) {
            RenewalDistributionVO.CycleItem item = new RenewalDistributionVO.CycleItem();
            String code = (String) row.get("cycle");
            item.setCycle(code);
            item.setCycleName(CYCLE_NAMES.getOrDefault(code, code));
            item.setCount(((Number) row.get("count")).longValue());
            item.setRevenue((BigDecimal) row.get("revenue"));
            cycles.add(item);
        }
        vo.setCycles(cycles);
        return vo;
    }

    @Override
    public RenewalUserPageVO listRenewalUsers(RenewalUserQueryRequest request) {
        long offset = (long) (request.getPage() - 1) * request.getPageSize();
        boolean renewalOnly = request.getRenewalOnly() != null ? request.getRenewalOnly() : true;
        List<RenewalUserVO> rows = orderMapper.selectRenewalUsers(
                request.getKeyword(), request.getPlanKey(), request.getCycle(),
                request.getStartDate(), request.getEndDate(), renewalOnly, offset, request.getPageSize());
        long total = orderMapper.countRenewalUsers(
                request.getKeyword(), request.getPlanKey(), request.getCycle(),
                request.getStartDate(), request.getEndDate(), renewalOnly);
        for (RenewalUserVO user : rows) {
            user.setCurrentLevel(PLAN_NAMES.getOrDefault(user.getCurrentLevel(), user.getCurrentLevel()));
        }
        RenewalUserPageVO page = new RenewalUserPageVO();
        page.setList(rows);
        page.setTotal(total);
        return page;
    }

    @Override
    public OrderPageVO listRenewalOrderDetails(RenewalOrderDetailQueryRequest request) {
        long offset = (long) (request.getPage() - 1) * request.getPageSize();
        String type = request.getType();
        if (!"first".equals(type) && !"renewal".equals(type)) {
            type = "renewal";
        }
        List<AdminOrderView> rows = orderMapper.selectRenewalOrderPage(
                type, request.getKeyword(), request.getPlanKey(), request.getCycle(),
                request.getStartDate(), request.getEndDate(), offset, request.getPageSize());
        long total = orderMapper.countRenewalOrderPage(
                type, request.getKeyword(), request.getPlanKey(), request.getCycle(),
                request.getStartDate(), request.getEndDate());
        List<OrderListVO> list = rows.stream().map(this::toListVO).toList();
        OrderPageVO vo = new OrderPageVO();
        vo.setList(list);
        vo.setTotal(total);
        return vo;
    }

    @Override
    public PlanDistributionVO getPlanDistribution() {
        List<Map<String, Object>> planRows = orderMapper.statsPlanDistribution();
        List<Map<String, Object>> cycleRows = orderMapper.statsCycleDistribution();

        PlanDistributionVO vo = new PlanDistributionVO();

        List<PlanDistributionVO.PlanItem> plans = new ArrayList<>();
        for (Map<String, Object> row : planRows) {
            PlanDistributionVO.PlanItem item = new PlanDistributionVO.PlanItem();
            String key = (String) row.get("planKey");
            item.setPlanKey(key);
            item.setPlanName(PLAN_NAMES.getOrDefault(key, key));
            item.setCount(((Number) row.get("count")).longValue());
            item.setRevenue((BigDecimal) row.get("revenue"));
            plans.add(item);
        }
        vo.setPlans(plans);

        List<PlanDistributionVO.CycleItem> cycles = new ArrayList<>();
        for (Map<String, Object> row : cycleRows) {
            PlanDistributionVO.CycleItem item = new PlanDistributionVO.CycleItem();
            String code = (String) row.get("cycle");
            item.setCycle(code);
            item.setCycleName(CYCLE_NAMES.getOrDefault(code, code));
            item.setCount(((Number) row.get("count")).longValue());
            cycles.add(item);
        }
        vo.setCycles(cycles);

        return vo;
    }

    // ── private helpers ──

    private void activateOrExtendMembership(Long userId, String planKey, String cycle) {
        AdminMembership membership = membershipMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        int days = CYCLE_DAYS.getOrDefault(cycle, 30);

        LocalDate baseDate = today;
        if (membership != null && membership.getExpiresAt().isAfter(today.minusDays(1))) {
            baseDate = membership.getExpiresAt();
        }
        LocalDate newExpiresAt = baseDate.plusDays(days);

        if (membership == null) {
            membership = new AdminMembership();
            membership.setUserId(userId);
            membership.setLevel(planKey);
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membershipMapper.insertMembership(membership);
        } else {
            membership.setLevel(planKey);
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membershipMapper.updateMembership(membership);
        }

        syncUserMembershipFields(userId, newExpiresAt, planKey);
    }

    private void activateOrExtendMembership(Long userId, String planKey, LocalDate startDate, LocalDate endDate) {
        AdminMembership membership = membershipMapper.selectByUserId(userId);

        if (membership == null) {
            membership = new AdminMembership();
            membership.setUserId(userId);
            membership.setLevel(planKey);
            membership.setStartedAt(startDate);
            membership.setExpiresAt(endDate);
            membershipMapper.insertMembership(membership);
        } else {
            membership.setLevel(planKey);
            membership.setStartedAt(startDate);
            membership.setExpiresAt(endDate);
            membershipMapper.updateMembership(membership);
        }

        syncUserMembershipFields(userId, endDate, planKey);
    }

    private void syncUserMembershipFields(Long userId, LocalDate expiresAt, String planKey) {
        LocalDateTime expireDateTime = expiresAt.atTime(LocalTime.MAX);
        String plan = expiresAt.isBefore(LocalDate.now()) ? null : planKey;
        membershipMapper.updateUserMembershipFields(userId, expireDateTime, plan);
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return "SUB" + date + random;
    }

    private OrderListVO toListVO(AdminOrderView row) {
        OrderListVO vo = new OrderListVO();
        vo.setId(row.getId());
        vo.setOrderNo(row.getOrderNo());
        vo.setUserId(row.getUserId());
        vo.setNickname(row.getNickname());
        vo.setEmail(row.getEmail());
        vo.setPlanKey(row.getPlanKey());
        vo.setPlanName(PLAN_NAMES.getOrDefault(row.getPlanKey(), row.getPlanKey()));
        vo.setCycle(row.getCycle());
        vo.setCycleName(CYCLE_NAMES.getOrDefault(row.getCycle(), row.getCycle()));
        vo.setAmount(row.getAmount());
        vo.setStatus(row.getStatus());
        OrderStatus os = OrderStatus.of(row.getStatus());
        vo.setStatusName(os != null ? os.getDisplayName() : "未知");
        vo.setPaymentMethod(row.getPaymentMethod());
        vo.setThirdPartyTradeId(row.getThirdPartyTradeId());
        vo.setPaidAt(row.getPaidAt());
        vo.setRefundedAt(row.getRefundedAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private OrderDetailVO toDetailVO(AdminOrderView row) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(row.getId());
        vo.setOrderNo(row.getOrderNo());
        vo.setUserId(row.getUserId());
        vo.setNickname(row.getNickname());
        vo.setEmail(row.getEmail());
        vo.setPlanKey(row.getPlanKey());
        vo.setPlanName(PLAN_NAMES.getOrDefault(row.getPlanKey(), row.getPlanKey()));
        vo.setCycle(row.getCycle());
        vo.setCycleName(CYCLE_NAMES.getOrDefault(row.getCycle(), row.getCycle()));
        vo.setAmount(row.getAmount());
        vo.setStatus(row.getStatus());
        OrderStatus os = OrderStatus.of(row.getStatus());
        vo.setStatusName(os != null ? os.getDisplayName() : "未知");
        vo.setPaymentMethod(row.getPaymentMethod());
        vo.setThirdPartyTradeId(row.getThirdPartyTradeId());
        vo.setThirdPartyRefundId(row.getThirdPartyRefundId());
        vo.setRefundAmount(row.getRefundAmount());
        vo.setPaidAt(row.getPaidAt());
        vo.setRefundedAt(row.getRefundedAt());
        vo.setRefundReason(row.getRefundReason());
        vo.setAdminRemark(row.getAdminRemark());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
