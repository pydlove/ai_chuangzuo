package com.aichuangzuo.admin.modules.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {

    private Long id;
    private Long userId;
    private String nickname;
    private String email;
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
    private LocalDateTime createdAt;
}
