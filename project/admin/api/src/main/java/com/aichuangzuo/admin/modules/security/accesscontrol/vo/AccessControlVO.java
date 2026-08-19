package com.aichuangzuo.admin.modules.security.accesscontrol.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccessControlVO {

    private Long id;

    private Integer ruleType;

    private Integer listType;

    private String ruleValue;

    private Integer ruleStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
