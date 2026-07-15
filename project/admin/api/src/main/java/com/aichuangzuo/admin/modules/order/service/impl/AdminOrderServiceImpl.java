package com.aichuangzuo.admin.modules.order.service.impl;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.entity.AdminMembership;
import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.enums.AdminOrderErrorCode;
import com.aichuangzuo.admin.modules.order.enums.OrderStatus;
import com.aichuangzuo.admin.modules.order.mapper.AdminMembershipMapper;
import com.aichuangzuo.admin.modules.order.mapper.AdminOrderMapper;
import com.aichuangzuo.admin.modules.order.service.AdminOrderService;
import com.aichuangzuo.admin.modules.order.vo.*;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        LocalDateTime now = LocalDateTime.now();
        orderMapper.refund(id, reason, operatorId, now);

        // 回退会员时长
        AdminMembership membership = membershipMapper.selectByUserId(order.getUserId());
        if (membership != null) {
            int days = CYCLE_DAYS.getOrDefault(order.getCycle(), 30);
            LocalDate newExpiresAt = membership.getExpiresAt().minusDays(days);
            membership.setExpiresAt(newExpiresAt);
            membershipMapper.updateMembership(membership);
            syncUserMembershipFields(order.getUserId(), newExpiresAt, order.getCycle());
        }

        log.info("管理员退款 orderId={}, operatorId={}, reason={}", id, operatorId, reason);
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

        syncUserMembershipFields(request.getUserId(), request.getExpiresAt(), null);
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
                request.getCycle(), remark, operatorId, now);
        activateOrExtendMembership(request.getUserId(), request.getPlanKey(), request.getCycle());

        log.info("管理员发放会员 userId={}, planKey={}, cycle={}, operatorId={}",
                request.getUserId(), request.getPlanKey(), request.getCycle(), operatorId);
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

        syncUserMembershipFields(userId, newExpiresAt, cycle);
    }

    private void syncUserMembershipFields(Long userId, LocalDate expiresAt, String cycle) {
        LocalDateTime expireDateTime = expiresAt.atTime(LocalTime.MAX);
        membershipMapper.updateUserMembershipFields(userId, expireDateTime, cycle);
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
        vo.setPaidAt(row.getPaidAt());
        vo.setRefundedAt(row.getRefundedAt());
        vo.setRefundReason(row.getRefundReason());
        vo.setAdminRemark(row.getAdminRemark());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }
}
