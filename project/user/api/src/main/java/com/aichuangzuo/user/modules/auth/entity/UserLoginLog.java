package com.aichuangzuo.user.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_login_log")
public class UserLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer loginType;
    private String clientIp;
    private String userAgent;
    private Integer loginStatus;
    private String failReason;
    private LocalDateTime createdAt;
}
