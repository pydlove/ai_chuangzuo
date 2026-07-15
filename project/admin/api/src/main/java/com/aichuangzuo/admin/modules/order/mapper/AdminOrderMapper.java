package com.aichuangzuo.admin.modules.order.mapper;

import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminOrderMapper {

    List<AdminOrderView> selectPage(@Param("keyword") String keyword,
                                    @Param("planKey") String planKey,
                                    @Param("status") Integer status,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("offset") long offset,
                                    @Param("size") long size);

    long countPage(@Param("keyword") String keyword,
                   @Param("planKey") String planKey,
                   @Param("status") Integer status,
                   @Param("startDate") String startDate,
                   @Param("endDate") String endDate);

    AdminOrderView selectDetailById(@Param("id") Long id);

    int markPaid(@Param("id") Long id,
                 @Param("operatorId") Long operatorId,
                 @Param("now") LocalDateTime now);

    int refund(@Param("id") Long id,
               @Param("reason") String reason,
               @Param("operatorId") Long operatorId,
               @Param("now") LocalDateTime now);

    int cancel(@Param("id") Long id,
               @Param("operatorId") Long operatorId);

    int insertGrantOrder(@Param("orderNo") String orderNo,
                         @Param("userId") Long userId,
                         @Param("planKey") String planKey,
                         @Param("cycle") String cycle,
                         @Param("adminRemark") String adminRemark,
                         @Param("operatorId") Long operatorId,
                         @Param("now") LocalDateTime now);

    OrderStatsOverviewVO statsOverview();

    List<Map<String, Object>> statsTrend(@Param("days") int days);

    List<Map<String, Object>> statsPlanDistribution();

    List<Map<String, Object>> statsCycleDistribution();
}
