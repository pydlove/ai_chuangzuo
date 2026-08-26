package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.user.modules.membership.dto.request.ListOrderRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.service.OrderService;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.aichuangzuo.user.modules.membership.vo.OrderPageVO;
import com.aichuangzuo.user.modules.membership.vo.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户订单查询服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final PlanLookupService planLookupService;

    @Override
    public OrderPageVO listOrders(Long userId, ListOrderRequest request) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (request.getStatus() != null) {
            wrapper.eq(Order::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> page = new Page<>(request.getPage(), request.getPageSize());
        page = orderMapper.selectPage(page, wrapper);

        List<OrderVO> list = page.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList());

        OrderPageVO vo = new OrderPageVO();
        vo.setList(list);
        vo.setTotal(page.getTotal());
        vo.setPage(page.getCurrent());
        vo.setPageSize(page.getSize());
        return vo;
    }

    private OrderVO toVo(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlanKey(order.getPlanKey());
        vo.setPlanName(planLookupService.getDisplayName(order.getPlanKey()));
        vo.setCycle(order.getCycle());
        vo.setCycleName(cycleName(order.getCycle()));
        vo.setAmount(order.getAmount());
        vo.setCoinAmount(order.getCoinAmount());
        vo.setCoinDiscount(order.getCoinDiscount());
        vo.setCouponDiscount(order.getCouponDiscount());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setPaidAt(order.getPaidAt());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private String cycleName(String cycle) {
        MembershipCycle mc = MembershipCycle.of(cycle);
        if (mc != null) {
            return switch (mc) {
                case MONTH -> "月卡";
                case QUARTER -> "季卡";
                case YEAR -> "年卡";
            };
        }
        return cycle;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            default -> "未知";
        };
    }
}
