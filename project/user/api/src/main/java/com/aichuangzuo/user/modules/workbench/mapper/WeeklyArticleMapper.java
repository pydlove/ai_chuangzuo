package com.aichuangzuo.user.modules.workbench.mapper;

import com.aichuangzuo.user.modules.workbench.entity.WeeklyArticle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户每周文章数据 Mapper。
 */
@Mapper
public interface WeeklyArticleMapper extends BaseMapper<WeeklyArticle> {

    /**
     * 查询指定用户指定周的数据。
     */
    @Select("SELECT id, user_id, week_start_date, title, `reads`, is_deleted, created_at, updated_at, created_by, updated_by " +
            "FROM u_weekly_article " +
            "WHERE user_id = #{userId} AND week_start_date = #{weekStartDate} AND is_deleted = 0 " +
            "ORDER BY id ASC")
    List<WeeklyArticle> selectByUserAndWeek(@Param("userId") Long userId,
                                            @Param("weekStartDate") LocalDate weekStartDate);

    /**
     * 删除指定用户指定周的全部记录。
     */
    @Delete("DELETE FROM u_weekly_article WHERE user_id = #{userId} AND week_start_date = #{weekStartDate}")
    int deleteByUserAndWeek(@Param("userId") Long userId,
                            @Param("weekStartDate") LocalDate weekStartDate);
}
