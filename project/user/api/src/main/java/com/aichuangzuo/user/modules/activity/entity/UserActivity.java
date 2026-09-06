package com.aichuangzuo.user.modules.activity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_activity")
public class UserActivity {

    @TableId
    private Long userId;

    private LocalDateTime lastActiveTime;
}
