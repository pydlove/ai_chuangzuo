
package com.aichuangzuo.admin.modules.security.smsconfig.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SmsSendRecordVO {
    private Long id;
    private String phone;
    private String scene;
    private String clientIp;
    private Long userId;
    private Integer sendStatus;
    private String failReason;
    private String responseCode;
    private LocalDateTime createdAt;
}
