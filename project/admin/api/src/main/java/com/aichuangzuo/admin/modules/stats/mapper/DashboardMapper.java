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

    /** 机器人排除条件：u_user 主键为 id，其余表为 user_id */
    String ROBOT_USER_FILTER = " AND id NOT IN (SELECT user_id FROM a_simulation_robot WHERE is_deleted = 0) ";
    String ROBOT_REF_FILTER = " AND user_id NOT IN (SELECT user_id FROM a_simulation_robot WHERE is_deleted = 0) ";

    /** 当前在线：最近 N 分钟内有活跃打点的用户数 */
    @Select("<script>SELECT COUNT(*) FROM u_user_activity WHERE last_active_time > #{since}" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    long countOnline(@Param("since") LocalDateTime since, @Param("excludeRobots") boolean excludeRobots);

    /** 今日日活 */
    @Select("<script>SELECT COUNT(*) FROM u_daily_active WHERE active_date = CURDATE()" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    long countTodayActive(@Param("excludeRobots") boolean excludeRobots);

    /** 注册用户总数 */
    @Select("<script>SELECT COUNT(*) FROM u_user WHERE is_deleted = 0" +
            "<if test='excludeRobots'>" + ROBOT_USER_FILTER + "</if></script>")
    long countTotalUsers(@Param("excludeRobots") boolean excludeRobots);

    /** 今日新增用户 */
    @Select("<script>SELECT COUNT(*) FROM u_user WHERE is_deleted = 0 AND created_at >= CURDATE()" +
            "<if test='excludeRobots'>" + ROBOT_USER_FILTER + "</if></script>")
    long countTodayNewUsers(@Param("excludeRobots") boolean excludeRobots);

    /** 有效会员数（会员未过期） */
    @Select("<script>SELECT COUNT(*) FROM u_user WHERE is_deleted = 0 AND membership_expire_at > NOW()" +
            "<if test='excludeRobots'>" + ROBOT_USER_FILTER + "</if></script>")
    long countValidMembers(@Param("excludeRobots") boolean excludeRobots);

    /** 累计生成文章数 */
    @Select("<script>SELECT COUNT(*) FROM u_article WHERE is_deleted = 0" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    long countTotalArticles(@Param("excludeRobots") boolean excludeRobots);

    /** 今日生成文章数 */
    @Select("<script>SELECT COUNT(*) FROM u_article WHERE is_deleted = 0 AND created_at >= CURDATE()" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    long countTodayArticles(@Param("excludeRobots") boolean excludeRobots);

    /** 今日支付订单数 */
    @Select("<script>SELECT COUNT(*) FROM u_order WHERE status = 1 AND paid_at >= CURDATE()" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    long countTodayPaidOrders(@Param("excludeRobots") boolean excludeRobots);

    /** 今日支付金额 */
    @Select("<script>SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1 AND paid_at >= CURDATE()" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    BigDecimal sumTodayPaidAmount(@Param("excludeRobots") boolean excludeRobots);

    /** 累计支付金额 */
    @Select("<script>SELECT COALESCE(SUM(amount), 0) FROM u_order WHERE status = 1" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if></script>")
    BigDecimal sumTotalPaidAmount(@Param("excludeRobots") boolean excludeRobots);

    /** 待审核提现申请数 */
    @Select("SELECT COUNT(*) FROM u_withdraw_request WHERE status = 1 AND is_deleted = 0")
    long countPendingWithdraws();

    /** 近 N 天每日新增用户 */
    @Select("<script>SELECT DATE(created_at) AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_user WHERE is_deleted = 0 AND created_at >= #{from}" +
            "<if test='excludeRobots'>" + ROBOT_USER_FILTER + "</if>" +
            " GROUP BY DATE(created_at)</script>")
    List<DashboardGroupRow> countDailyNewUsers(@Param("from") LocalDateTime from,
                                               @Param("excludeRobots") boolean excludeRobots);

    /** 近 N 天每日活跃（每用户每日一行） */
    @Select("<script>SELECT active_date AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_daily_active WHERE active_date >= #{from}" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if>" +
            " GROUP BY active_date</script>")
    List<DashboardGroupRow> countDailyActive(@Param("from") LocalDate from,
                                             @Param("excludeRobots") boolean excludeRobots);

    /** 近 N 天每日生成文章 */
    @Select("<script>SELECT DATE(created_at) AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_article WHERE is_deleted = 0 AND created_at >= #{from}" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if>" +
            " GROUP BY DATE(created_at)</script>")
    List<DashboardGroupRow> countDailyArticles(@Param("from") LocalDateTime from,
                                               @Param("excludeRobots") boolean excludeRobots);

    /** 近 N 天每日支付订单数与金额 */
    @Select("<script>SELECT DATE(paid_at) AS item_key, COUNT(*) AS item_count, COALESCE(SUM(amount), 0) AS item_amount " +
            "FROM u_order WHERE status = 1 AND paid_at >= #{from}" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if>" +
            " GROUP BY DATE(paid_at)</script>")
    List<DashboardGroupRow> countDailyPaid(@Param("from") LocalDateTime from,
                                           @Param("excludeRobots") boolean excludeRobots);

    /** 有效会员套餐分布 */
    @Select("<script>SELECT membership_plan AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_user WHERE is_deleted = 0 AND membership_expire_at > NOW()" +
            "<if test='excludeRobots'>" + ROBOT_USER_FILTER + "</if>" +
            " GROUP BY membership_plan</script>")
    List<DashboardGroupRow> countMemberPlanDistribution(@Param("excludeRobots") boolean excludeRobots);

    /** 作品平台分布：非标准平台 key（含 NULL，如通用模板 marketing/story 产生的历史数据）归入 other */
    @Select("<script>SELECT CASE WHEN platform IN ('wechat','xiaohongshu','toutiao','baijiahao','douyin','zhihu','bilibili','kuaishou','general') " +
            "THEN platform ELSE 'other' END AS item_key, COUNT(*) AS item_count, NULL AS item_amount " +
            "FROM u_article WHERE is_deleted = 0" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if>" +
            " GROUP BY 1 ORDER BY item_count DESC LIMIT 10</script>")
    List<DashboardGroupRow> countArticlePlatformDistribution(@Param("excludeRobots") boolean excludeRobots);

    /** 累计支付金额按套餐分布 */
    @Select("<script>SELECT plan_key AS item_key, COUNT(*) AS item_count, COALESCE(SUM(amount), 0) AS item_amount " +
            "FROM u_order WHERE status = 1" +
            "<if test='excludeRobots'>" + ROBOT_REF_FILTER + "</if>" +
            " GROUP BY plan_key ORDER BY item_amount DESC</script>")
    List<DashboardGroupRow> sumPaidAmountByPlan(@Param("excludeRobots") boolean excludeRobots);
}
