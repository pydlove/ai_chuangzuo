package com.aichuangzuo.admin.modules.order.mapper;

import com.aichuangzuo.admin.modules.order.entity.AdminOrderView;
import com.aichuangzuo.admin.modules.order.vo.OrderStatsOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalOverviewVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalTrendVO;
import com.aichuangzuo.admin.modules.order.vo.RenewalUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
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
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate,
                         @Param("adminRemark") String adminRemark,
                         @Param("operatorId") Long operatorId,
                         @Param("now") LocalDateTime now);

    OrderStatsOverviewVO statsOverview();

    List<Map<String, Object>> statsTrend(@Param("days") int days);

    List<Map<String, Object>> statsPlanDistribution();

    List<Map<String, Object>> statsCycleDistribution();

    RenewalOverviewVO selectRenewalOverview();

    List<Map<String, Object>> selectRenewalTrend(@Param("days") int days);

    List<Map<String, Object>> selectRenewalPlanDistribution();

    List<Map<String, Object>> selectRenewalCycleDistribution();

    List<RenewalUserVO> selectRenewalUsers(@Param("keyword") String keyword,
                                            @Param("planKey") String planKey,
                                            @Param("cycle") String cycle,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("renewalOnly") boolean renewalOnly,
                                            @Param("offset") long offset,
                                            @Param("size") long size);

    long countRenewalUsers(@Param("keyword") String keyword,
                          @Param("planKey") String planKey,
                          @Param("cycle") String cycle,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate,
                          @Param("renewalOnly") boolean renewalOnly);

    List<AdminOrderView> selectRenewalOrderPage(@Param("type") String type,
                                                @Param("keyword") String keyword,
                                                @Param("planKey") String planKey,
                                                @Param("cycle") String cycle,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("offset") long offset,
                                                @Param("size") long size);

    long countRenewalOrderPage(@Param("type") String type,
                               @Param("keyword") String keyword,
                               @Param("planKey") String planKey,
                               @Param("cycle") String cycle,
                               @Param("startDate") String startDate,
                               @Param("endDate") String endDate);
}
