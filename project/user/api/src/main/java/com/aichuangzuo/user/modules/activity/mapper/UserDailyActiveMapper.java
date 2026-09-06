package com.aichuangzuo.user.modules.activity.mapper;

import com.aichuangzuo.user.modules.activity.entity.UserDailyActive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface UserDailyActiveMapper extends BaseMapper<UserDailyActive> {

    /**
     * 幂等记录日活：uk(user_id, active_date) 兜底，重复插入自动忽略。
     */
    void insertIgnore(@Param("userId") Long userId, @Param("activeDate") LocalDate activeDate);
}
