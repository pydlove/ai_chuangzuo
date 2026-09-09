package com.aichuangzuo.admin.modules.stats.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DashboardOverviewVO {

    /** 当前在线（最近 5 分钟活跃用户数） */
    private final long onlineCount;

    /** 今日日活 */
    private final long todayActive;

    /** 注册用户总数 */
    private final long totalUsers;

    /** 今日新增用户 */
    private final long todayNewUsers;

    /** 有效会员数 */
    private final long validMembers;

    /** 累计生成文章数 */
    private final long totalArticles;

    /** 今日生成文章数 */
    private final long todayArticles;

    /** 今日支付订单数 */
    private final long todayPaidOrders;

    /** 今日支付金额 */
    private final BigDecimal todayPaidAmount;

    /** 累计支付金额 */
    private final BigDecimal totalPaidAmount;

    /** 待审核提现申请数 */
    private final long pendingWithdraws;
}
