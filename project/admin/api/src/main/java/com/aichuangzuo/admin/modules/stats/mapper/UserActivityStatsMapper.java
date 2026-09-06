package com.aichuangzuo.admin.modules.stats.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface UserActivityStatsMapper {

    /** 在线人数：最近 N 分钟内有活跃打点的用户数 */
    @Select("SELECT COUNT(*) FROM u_user_activity WHERE last_active_time > #{since}")
    long countOnline(@Param("since") LocalDateTime since);

    /** 指定日期的日活 */
    @Select("SELECT COUNT(*) FROM u_daily_active WHERE active_date = #{date}")
    long countDailyActive(@Param("date") LocalDate date);
}
