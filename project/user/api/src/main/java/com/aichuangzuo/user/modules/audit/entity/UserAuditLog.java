package com.aichuangzuo.user.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user_audit_log")
public class UserAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String actionType;

    private String module;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String requestBody;

    private String clientIp;

    private String userAgent;

    private Integer statusCode;

    private String errorMsg;

    private Integer durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
