package com.aichuangzuo.admin.modules.stats.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {

    /** 当前在线：最近 N 分钟内有活跃打点的用户数 */
    @Select("SELECT COUNT(*) FROM u_user_activity WHERE last_active_time > #{since}")
    long countOnline(@Param("since") LocalDateTime since);

    /** 今日日活 */
    @Select("SELECT COUNT(*) FROM u_daily_active WHERE active_date = CURDATE()")
    long countTodayActive();

    /** 注册用户总数 */
    @Select("SELECT COUNT(*) FROM u_user WHERE is_deleted = 0")
    long countTotalUsers();

    /** 今日新增用户 */
    @Select("SELECT COUNT(*) FROM u_user WHERE is_deleted = 0 AND created_at >= CURDATE()")
    long countTodayNewUsers();

    /** 有效会员数（会员未过期） */
    @Select("SELECT COUNT(*) FROM u_user WHERE is_deleted = 0 AND membership_expire_at > NOW()")
    long countValidMembers();

    /** 累计生成文章数 */
    @Select("SELECT COUNT(*) FROM u_article WHERE is_deleted = 0")
    long countTotalArticles();

    /** 今日生成文章数 */
    @Select("SELECT COUNT(*) FROM u_article WHERE is_deleted = 0 AND created_at >= CURDATE()")
    long countTodayArticles();

    /** 今日支付订单数 */
    @Select("SELECT COUNT(*) FROM u_order WHERE status = 1 AND paid_at >= CURDATE()")
    long countTodayPaidOrders();

    /** 今日支付金额 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1 AND paid_at >= CURDATE()")
    BigDecimal sumTodayPaidAmount();

    /** 累计支付金额 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1")
    BigDecimal sumTotalPaidAmount();

    /** 待审核提现申请数 */
    @Select("SELECT COUNT(*) FROM u_withdraw_request WHERE status = 1 AND is_deleted = 0")
    long countPendingWithdraws();

    /** 近 N 天每日新增用户 */
    @Select("SELECT DATE(created_at) AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_user WHERE is_deleted = 0 AND created_at >= #{from} GROUP BY DATE(created_at)")
    List<DashboardGroupRow> countDailyNewUsers(@Param("from") LocalDateTime from);

    /** 近 N 天每日活跃（每用户每日一行） */
    @Select("SELECT active_date AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_daily_active WHERE active_date >= #{from} GROUP BY active_date")
    List<DashboardGroupRow> countDailyActive(@Param("from") LocalDate from);

    /** 近 N 天每日生成文章 */
    @Select("SELECT DATE(created_at) AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_article WHERE is_deleted = 0 AND created_at >= #{from} GROUP BY DATE(created_at)")
    List<DashboardGroupRow> countDailyArticles(@Param("from") LocalDateTime from);

    /** 近 N 天每日支付订单数与金额 */
    @Select("SELECT DATE(paid_at) AS item_key, COUNT(*) AS item_count, COALESCE(SUM(amount), 0) AS item_amount " +
            "FROM u_order WHERE status = 1 AND paid_at >= #{from} GROUP BY DATE(paid_at)")
    List<DashboardGroupRow> countDailyPaid(@Param("from") LocalDateTime from);

    /** 有效会员套餐分布 */
    @Select("SELECT membership_plan AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_user WHERE is_deleted = 0 AND membership_expire_at > NOW() GROUP BY membership_plan")
    List<DashboardGroupRow> countMemberPlanDistribution();

    /** 作品平台分布：非标准平台 key（含 NULL，如通用模板 marketing/story 产生的历史数据）归入 other */
    @Select("SELECT CASE WHEN platform IN ('wechat','xiaohongshu','toutiao','baijiahao','douyin','zhihu','bilibili','kuaishou','general') " +
            "THEN platform ELSE 'other' END AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_article WHERE is_deleted = 0 GROUP BY 1 ORDER BY item_count DESC LIMIT 10")
    List<DashboardGroupRow> countArticlePlatformDistribution();

    /** 累计支付金额按套餐分布 */
    @Select("SELECT plan_key AS item_key, COUNT(*) AS item_count, COALESCE(SUM(amount), 0) AS item_amount " +
            "FROM u_order WHERE status = 1 GROUP BY plan_key ORDER BY item_amount DESC")
    List<DashboardGroupRow> sumPaidAmountByPlan();
}
