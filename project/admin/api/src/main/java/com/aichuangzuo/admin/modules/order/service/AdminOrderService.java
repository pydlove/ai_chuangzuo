package com.aichuangzuo.admin.modules.order.service;

import com.aichuangzuo.admin.modules.order.dto.request.MembershipAdjustRequest;
import com.aichuangzuo.admin.modules.order.dto.request.MembershipGrantRequest;
import com.aichuangzuo.admin.modules.order.vo.OrderDetailVO;
import com.aichuangzuo.admin.modules.order.vo.OrderPageVO;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.OrderTrendVO;
import com.aichuangzuo.admin.modules.order.vo.PlanDistributionVO;

public interface AdminOrderService {

    OrderPageVO listOrders(String keyword, String planKey, Integer status,
                           String startDate, String endDate, int page, int pageSize);

    OrderDetailVO getOrderDetail(Long id);

    void markPaid(Long id, Long operatorId);

    void refund(Long id, String reason, Long operatorId);

    void cancel(Long id, Long operatorId);

    void adjustMembership(MembershipAdjustRequest request, Long operatorId);

    void grantMembership(MembershipGrantRequest request, Long operatorId);

    OrderStatsOverviewVO getStatsOverview();

    OrderTrendVO getStatsTrend(int days);

    PlanDistributionVO getPlanDistribution();
}
