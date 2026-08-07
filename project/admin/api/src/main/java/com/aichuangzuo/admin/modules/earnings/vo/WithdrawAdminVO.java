package com.aichuangzuo.admin.modules.earnings.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawAdminVO {

    private Long id;

    private String bizNo;

    private Long userId;

    private String nickname;

    private String email;

    private BigDecimal amount;

    private String account;

    private String name;

    private Integer status;

    private String statusText;

    private LocalDateTime processedAt;

    private Long processedBy;

    private String resultRemark;

    private LocalDateTime createdAt;
}
