package com.aichuangzuo.user.modules.activity.mapper;

import com.aichuangzuo.user.modules.activity.entity.UserActivity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface UserActivityMapper extends BaseMapper<UserActivity> {

    /**
     * 记录用户活跃：首次插入，之后仅更新 last_active_time（单行 upsert，避免先查后写的并发问题）。
     */
    void upsert(@Param("userId") Long userId, @Param("activeTime") LocalDateTime activeTime);
}
