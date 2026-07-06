package com.aichuangzuo.admin.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_admin_login_log")
public class AdminLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminUserId;

    private Integer loginType;

    private String clientIp;

    private String userAgent;

    private Integer loginStatus;

    private String failReason;

    private LocalDateTime createdAt;
}
