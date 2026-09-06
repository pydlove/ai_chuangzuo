package com.aichuangzuo.user.modules.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_daily_active")
public class UserDailyActive {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate activeDate;

    private LocalDateTime createdAt;
}
