package com.aichuangzuo.admin.modules.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardTrendVO {

    /** 近 N 天每日数据点，按日期升序，缺失日期补零 */
    private final List<DailyPoint> points;

    @Getter
    @AllArgsConstructor
    public static class DailyPoint {

        /** 日期，yyyy-MM-dd */
        private final String date;

        /** 新增用户 */
        private final long newUsers;

        /** 日活 */
        private final long activeUsers;

        /** 生成文章数 */
        private final long articles;

        /** 支付订单数 */
        private final long paidOrders;

        /** 支付金额（元） */
        private final BigDecimal paidAmount;
    }
}
